/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.client.read;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.buffer.ByteBuf;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.compress.Decompressor;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.exception.CelebornIOException;
import org.apache.celeborn.common.network.client.TransportClientFactory;
import org.apache.celeborn.common.network.util.TransportConf;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.protocol.StorageInfo;
import org.apache.celeborn.common.protocol.TransportModuleConstants;
import org.apache.celeborn.common.unsafe.Platform;
import org.apache.celeborn.common.util.Utils;

public abstract class RssInputStream extends InputStream {
  private static final Logger logger = LoggerFactory.getLogger(RssInputStream.class);

  public static RssInputStream create(
      CelebornConf conf,
      TransportClientFactory clientFactory,
      String shuffleKey,
      PartitionLocation[] locations,
      int[] attempts,
      int attemptNumber,
      int startMapIndex,
      int endMapIndex,
      ConcurrentHashMap<String, Long> fetchExcludedWorkers)
      throws IOException {
    if (locations == null || locations.length == 0) {
      return emptyInputStream;
    } else {
      return new RssInputStreamImpl(
          conf,
          clientFactory,
          shuffleKey,
          locations,
          attempts,
          attemptNumber,
          startMapIndex,
          endMapIndex,
          fetchExcludedWorkers);
    }
  }

  public static RssInputStream empty() {
    return emptyInputStream;
  }

  public abstract void setCallback(MetricsCallback callback);

  private static final RssInputStream emptyInputStream =
      new RssInputStream() {
        @Override
        public int read() throws IOException {
          return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
          return -1;
        }

        @Override
        public void setCallback(MetricsCallback callback) {}
      };

  private static final class RssInputStreamImpl extends RssInputStream {
    private static final Random RAND = new Random();

    private final CelebornConf conf;
    private final TransportClientFactory clientFactory;
    private final String shuffleKey;
    private final PartitionLocation[] locations;
    private final int[] attempts;
    private final int attemptNumber;
    private final int startMapIndex;
    private final int endMapIndex;

    private final Map<Integer, Set<Integer>> batchesRead = new HashMap<>();

    private byte[] compressedBuf;
    private byte[] decompressedBuf;
    private final Decompressor decompressor;

    private ByteBuf currentChunk;
    private PartitionReader currentReader;
    private final int fetchChunkMaxRetry;
    private int fetchChunkRetryCnt = 0;
    int retryWaitMs;
    private int fileIndex;
    private int position;
    private int limit;

    private MetricsCallback callback;

    // mapId, attemptId, batchId, size
    private final int BATCH_HEADER_SIZE = 4 * 4;
    private final byte[] sizeBuf = new byte[BATCH_HEADER_SIZE];
    private LongAdder skipCount = new LongAdder();
    private final boolean rangeReadFilter;

    private boolean fetchBlacklistEnabled;
    private long fetchExcludedWorkerExpireTimeout;
    private final ConcurrentHashMap<String, Long> fetchExcludedWorkers;

    RssInputStreamImpl(
        CelebornConf conf,
        TransportClientFactory clientFactory,
        String shuffleKey,
        PartitionLocation[] locations,
        int[] attempts,
        int attemptNumber,
        int startMapIndex,
        int endMapIndex,
        ConcurrentHashMap<String, Long> fetchExcludedWorkers)
        throws IOException {
      this.conf = conf;
      this.clientFactory = clientFactory;
      this.shuffleKey = shuffleKey;
      this.locations = (PartitionLocation[]) Utils.randomizeInPlace(locations, RAND);
      this.attempts = attempts;
      this.attemptNumber = attemptNumber;
      this.startMapIndex = startMapIndex;
      this.endMapIndex = endMapIndex;
      this.rangeReadFilter = conf.shuffleRangeReadFilterEnabled();
      this.fetchBlacklistEnabled = conf.clientFetchExcludeWorkerOnFailureEnabled();
      this.fetchExcludedWorkerExpireTimeout = conf.clientFetchExcludedWorkerExpireTimeout();
      this.fetchExcludedWorkers = fetchExcludedWorkers;

      int headerLen = Decompressor.getCompressionHeaderLength(conf);
      int blockSize = conf.clientPushBufferMaxSize() + headerLen;
      compressedBuf = new byte[blockSize];
      decompressedBuf = new byte[blockSize];

      decompressor = Decompressor.getDecompressor(conf);

      if (conf.clientPushReplicateEnabled()) {
        fetchChunkMaxRetry = conf.clientFetchMaxRetriesForEachReplica() * 2;
      } else {
        fetchChunkMaxRetry = conf.clientFetchMaxRetriesForEachReplica();
      }
      TransportConf transportConf =
          Utils.fromCelebornConf(conf, TransportModuleConstants.DATA_MODULE, 0);
      retryWaitMs = transportConf.ioRetryWaitTimeMs();
      moveToNextReader();
    }

    private boolean skipLocation(int startMapIndex, int endMapIndex, PartitionLocation location) {
      if (!rangeReadFilter) {
        return false;
      }
      if (endMapIndex == Integer.MAX_VALUE) {
        return false;
      }
      RoaringBitmap bitmap = location.getMapIdBitMap();
      if (bitmap == null && location.getPeer() != null) {
        bitmap = location.getPeer().getMapIdBitMap();
      }
      for (int i = startMapIndex; i < endMapIndex; i++) {
        if (bitmap.contains(i)) {
          return false;
        }
      }
      return true;
    }

    private PartitionLocation nextReadableLocation() {
      int locationCount = locations.length;
      if (fileIndex >= locationCount) {
        return null;
      }
      PartitionLocation currentLocation = locations[fileIndex];
      while (skipLocation(startMapIndex, endMapIndex, currentLocation)) {
        skipCount.increment();
        fileIndex++;
        if (fileIndex == locationCount) {
          return null;
        }
        currentLocation = locations[fileIndex];
      }

      fetchChunkRetryCnt = 0;

      return currentLocation;
    }

    private void moveToNextReader() throws IOException {
      if (currentReader != null) {
        currentReader.close();
        currentReader = null;
      }
      PartitionLocation currentLocation = nextReadableLocation();
      if (currentLocation == null) {
        return;
      }
      currentReader = createReaderWithRetry(currentLocation);
      fileIndex++;
      while (!currentReader.hasNext()) {
        currentReader.close();
        currentReader = null;
        currentLocation = nextReadableLocation();
        if (currentLocation == null) {
          return;
        }
        currentReader = createReaderWithRetry(currentLocation);
        fileIndex++;
      }
      currentChunk = getNextChunk();
    }

    private void excludeFailedLocation(PartitionLocation location, Exception e) {
      if (conf.clientPushReplicateEnabled() && fetchBlacklistEnabled && isCriticalCause(e)) {
        fetchExcludedWorkers.put(location.hostAndFetchPort(), System.currentTimeMillis());
      }
    }

    private boolean isExcluded(PartitionLocation location) {
      Long timestamp = fetchExcludedWorkers.get(location.hostAndFetchPort());
      if (timestamp == null) {
        return false;
      } else if (System.currentTimeMillis() - timestamp > fetchExcludedWorkerExpireTimeout) {
        fetchExcludedWorkers.remove(location.hostAndFetchPort());
        return false;
      } else if (location.getPeer() != null) {
        Long peerTimestamp = fetchExcludedWorkers.get(location.getPeer().hostAndFetchPort());
        // To avoid both replicate locations is excluded, if peer add to excluded list earlier,
        // change to try peer.
        if (peerTimestamp == null || peerTimestamp < timestamp) {
          return true;
        } else {
          return false;
        }
      } else {
        return true;
      }
    }

    private boolean isCriticalCause(Exception e) {
      boolean isConnectTimeout =
          e instanceof IOException
              && e.getMessage() != null
              && e.getMessage().startsWith("Connecting to")
              && e.getMessage().contains("timed out");
      boolean rpcTimeout =
          e instanceof IOException
              && e.getCause() != null
              && e.getCause() instanceof TimeoutException;
      boolean fetchChunkTimeout =
          e instanceof CelebornIOException
              && e.getCause() != null
              && e.getCause() instanceof IOException;
      return isConnectTimeout || rpcTimeout || fetchChunkTimeout;
    }

    private PartitionReader createReaderWithRetry(PartitionLocation location) throws IOException {
      while (fetchChunkRetryCnt < fetchChunkMaxRetry) {
        try {
          if (isExcluded(location)) {
            throw new CelebornIOException("Fetch data from blacklisted location! " + location);
          }
          return createReader(location, fetchChunkRetryCnt, fetchChunkMaxRetry);
        } catch (Exception e) {
          excludeFailedLocation(location, e);
          fetchChunkRetryCnt++;
          if (location.getPeer() != null) {
            // fetchChunkRetryCnt % 2 == 0 means both replicas have been tried,
            // so sleep before next try.
            if (fetchChunkRetryCnt % 2 == 0) {
              Uninterruptibles.sleepUninterruptibly(retryWaitMs, TimeUnit.MILLISECONDS);
            }
            location = location.getPeer();
            logger.warn(
                "CreatePartitionReader failed {}/{} times for location {}, change to peer",
                fetchChunkRetryCnt,
                fetchChunkMaxRetry,
                location,
                e);
          } else {
            logger.warn(
                "CreatePartitionReader failed {}/{} times for location {}, retry the same location",
                fetchChunkRetryCnt,
                fetchChunkMaxRetry,
                location,
                e);
            Uninterruptibles.sleepUninterruptibly(retryWaitMs, TimeUnit.MILLISECONDS);
          }
        }
      }
      throw new CelebornIOException("createPartitionReader failed! " + location);
    }

    private ByteBuf getNextChunk() throws IOException {
      while (fetchChunkRetryCnt < fetchChunkMaxRetry) {
        try {
          if (isExcluded(currentReader.getLocation())) {
            throw new CelebornIOException(
                "Fetch data from blacklisted location! " + currentReader.getLocation());
          }
          return currentReader.next();
        } catch (Exception e) {
          excludeFailedLocation(currentReader.getLocation(), e);
          fetchChunkRetryCnt++;
          currentReader.close();
          if (fetchChunkRetryCnt == fetchChunkMaxRetry) {
            logger.warn("Fetch chunk fail exceeds max retry {}", fetchChunkRetryCnt, e);
            throw new CelebornIOException(
                "Fetch chunk failed for "
                    + fetchChunkRetryCnt
                    + " times for location "
                    + currentReader.getLocation(),
                e);
          } else {
            if (currentReader.getLocation().getPeer() != null) {
              logger.warn(
                  "Fetch chunk failed {}/{} times for location {}, change to peer",
                  fetchChunkRetryCnt,
                  fetchChunkMaxRetry,
                  currentReader.getLocation(),
                  e);
              // fetchChunkRetryCnt % 2 == 0 means both replicas have been tried,
              // so sleep before next try.
              if (fetchChunkRetryCnt % 2 == 0) {
                Uninterruptibles.sleepUninterruptibly(retryWaitMs, TimeUnit.MILLISECONDS);
              }
              currentReader = createReaderWithRetry(currentReader.getLocation().getPeer());
            } else {
              logger.warn(
                  "Fetch chunk failed {}/{} times for location {}",
                  fetchChunkRetryCnt,
                  fetchChunkMaxRetry,
                  currentReader.getLocation(),
                  e);
              Uninterruptibles.sleepUninterruptibly(retryWaitMs, TimeUnit.MILLISECONDS);
              currentReader = createReaderWithRetry(currentReader.getLocation());
            }
          }
        }
      }
      throw new CelebornIOException("Fetch chunk failed! " + currentReader.getLocation());
    }

    private PartitionReader createReader(
        PartitionLocation location, int fetchChunkRetryCnt, int fetchChunkMaxRetry)
        throws IOException, InterruptedException {
      if (location.getPeer() == null) {
        logger.debug("Partition {} has only one partition replica.", location);
      }
      if (location.getPeer() != null && attemptNumber % 2 == 1) {
        location = location.getPeer();
        logger.debug("Read peer {} for attempt {}.", location, attemptNumber);
      }

      logger.debug("create reader for location {}", location);

      StorageInfo storageInfo = location.getStorageInfo();
      if (storageInfo.getType() == StorageInfo.Type.HDD
          || storageInfo.getType() == StorageInfo.Type.SSD) {
        return new WorkerPartitionReader(
            conf,
            shuffleKey,
            location,
            clientFactory,
            startMapIndex,
            endMapIndex,
            fetchChunkRetryCnt,
            fetchChunkMaxRetry);
      }
      if (storageInfo.getType() == StorageInfo.Type.HDFS) {
        return new DfsPartitionReader(
            conf, shuffleKey, location, clientFactory, startMapIndex, endMapIndex);
      }

      throw new CelebornIOException(
          "Unknown storage info " + storageInfo + " to read location " + location);
    }

    public void setCallback(MetricsCallback callback) {
      // callback must set before read()
      this.callback = callback;
    }

    @Override
    public int read() throws IOException {
      if (position < limit) {
        int b = decompressedBuf[position];
        position++;
        return b & 0xFF;
      }

      if (!fillBuffer()) {
        return -1;
      }

      if (position >= limit) {
        return read();
      } else {
        int b = decompressedBuf[position];
        position++;
        return b & 0xFF;
      }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (b == null) {
        throw new NullPointerException();
      } else if (off < 0 || len < 0 || len > b.length - off) {
        throw new IndexOutOfBoundsException();
      } else if (len == 0) {
        return 0;
      }

      int readBytes = 0;
      while (readBytes < len) {
        while (position >= limit) {
          if (!fillBuffer()) {
            return readBytes > 0 ? readBytes : -1;
          }
        }

        int bytesToRead = Math.min(limit - position, len - readBytes);
        System.arraycopy(decompressedBuf, position, b, off + readBytes, bytesToRead);
        position += bytesToRead;
        readBytes += bytesToRead;
      }

      return readBytes;
    }

    @Override
    public void close() {
      int locationsCount = locations.length;
      logger.debug(
          "total location count {} read {} skip {}",
          locationsCount,
          locationsCount - skipCount.sum(),
          skipCount.sum());
      if (currentChunk != null) {
        logger.debug("Release chunk {}", currentChunk);
        currentChunk.release();
        currentChunk = null;
      }
      if (currentReader != null) {
        logger.debug("Closing reader");
        currentReader.close();
        currentReader = null;
      }
    }

    private boolean moveToNextChunk() throws IOException {
      if (currentChunk != null) {
        currentChunk.release();
      }
      currentChunk = null;
      if (currentReader.hasNext()) {
        currentChunk = getNextChunk();
        return true;
      } else if (fileIndex < locations.length) {
        moveToNextReader();
        return currentReader != null;
      }
      if (currentReader != null) {
        currentReader.close();
        currentReader = null;
      }
      return false;
    }

    private boolean fillBuffer() throws IOException {
      if (currentChunk == null) {
        return false;
      }

      long startTime = System.currentTimeMillis();

      boolean hasData = false;
      while (currentChunk.isReadable() || moveToNextChunk()) {
        currentChunk.readBytes(sizeBuf);
        int mapId = Platform.getInt(sizeBuf, Platform.BYTE_ARRAY_OFFSET);
        int attemptId = Platform.getInt(sizeBuf, Platform.BYTE_ARRAY_OFFSET + 4);
        int batchId = Platform.getInt(sizeBuf, Platform.BYTE_ARRAY_OFFSET + 8);
        int size = Platform.getInt(sizeBuf, Platform.BYTE_ARRAY_OFFSET + 12);
        if (size > compressedBuf.length) {
          compressedBuf = new byte[size];
        }

        currentChunk.readBytes(compressedBuf, 0, size);

        // de-duplicate
        if (attemptId == attempts[mapId]) {
          if (!batchesRead.containsKey(mapId)) {
            Set<Integer> batchSet = new HashSet<>();
            batchesRead.put(mapId, batchSet);
          }
          Set<Integer> batchSet = batchesRead.get(mapId);
          if (!batchSet.contains(batchId)) {
            batchSet.add(batchId);
            if (callback != null) {
              callback.incBytesRead(BATCH_HEADER_SIZE + size);
            }
            // decompress data
            int originalLength = decompressor.getOriginalLen(compressedBuf);
            if (decompressedBuf.length < originalLength) {
              decompressedBuf = new byte[originalLength];
            }
            limit = decompressor.decompress(compressedBuf, decompressedBuf, 0);
            position = 0;
            hasData = true;
            break;
          } else {
            logger.debug(
                "Skip duplicated batch: mapId {}, attemptId {}, batchId {}.",
                mapId,
                attemptId,
                batchId);
          }
        }
      }

      if (callback != null) {
        callback.incReadTime(System.currentTimeMillis() - startTime);
      }
      return hasData;
    }
  }
}
