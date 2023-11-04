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

package org.apache.celeborn.service.deploy.master;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import scala.Tuple2;

import org.junit.Assert;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.meta.DiskInfo;
import org.apache.celeborn.common.meta.WorkerInfo;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.protocol.StorageInfo;

public class SlotsAllocatorSuiteJ {
  private List<WorkerInfo> prepareWorkers(boolean hasDisks) {
    long assumedPartitionSize = 64 * 1024 * 1024;

    Random random = new Random();
    Map<String, DiskInfo> disks1 = new HashMap<>();
    DiskInfo diskInfo1 =
        new DiskInfo(
            "/mnt/disk1",
            random.nextInt() + 100 * 1024 * 1024 * 1024L,
            random.nextInt(1000),
            random.nextInt(1000),
            0);
    DiskInfo diskInfo2 =
        new DiskInfo(
            "/mnt/disk2",
            random.nextInt() + 95 * 1024 * 1024 * 1024L,
            random.nextInt(1000),
            random.nextInt(1000),
            0);
    DiskInfo diskInfo3 =
        new DiskInfo(
            "/mnt/disk3",
            random.nextInt() + 90 * 1024 * 1024 * 1024L,
            random.nextInt(1000),
            random.nextInt(1000),
            0);
    diskInfo1.maxSlots_$eq(diskInfo1.actualUsableSpace() / assumedPartitionSize);
    diskInfo2.maxSlots_$eq(diskInfo2.actualUsableSpace() / assumedPartitionSize);
    diskInfo3.maxSlots_$eq(diskInfo3.actualUsableSpace() / assumedPartitionSize);
    if (hasDisks) {
      disks1.put("/mnt/disk1", diskInfo1);
      disks1.put("/mnt/disk2", diskInfo2);
      disks1.put("/mnt/disk3", diskInfo3);
    }

    Map<String, DiskInfo> disks2 = new HashMap<>();
    DiskInfo diskInfo4 =
        new DiskInfo(
            "/mnt/disk1",
            random.nextInt() + 100 * 1024 * 1024 * 1024L,
            random.nextInt(1000),
            random.nextInt(1000),
            0);
    DiskInfo diskInfo5 =
        new DiskInfo(
            "/mnt/disk2",
            random.nextInt() + 95 * 1024 * 1024 * 1024L,
            random.nextInt(1000),
            random.nextInt(1000),
            0);
    DiskInfo diskInfo6 =
        new DiskInfo(
            "/mnt/disk3",
            random.nextInt() + 90 * 1024 * 1024 * 1024L,
            random.nextInt(1000),
            random.nextInt(1000),
            0);
    diskInfo4.maxSlots_$eq(diskInfo4.actualUsableSpace() / assumedPartitionSize);
    diskInfo5.maxSlots_$eq(diskInfo5.actualUsableSpace() / assumedPartitionSize);
    diskInfo6.maxSlots_$eq(diskInfo6.actualUsableSpace() / assumedPartitionSize);
    if (hasDisks) {
      disks2.put("/mnt/disk1", diskInfo4);
      disks2.put("/mnt/disk2", diskInfo5);
      disks2.put("/mnt/disk3", diskInfo6);
    }

    Map<String, DiskInfo> disks3 = new HashMap<>();
    DiskInfo diskInfo7 =
        new DiskInfo(
            "/mnt/disk1",
            random.nextInt() + 100 * 1024 * 1024 * 1024L,
            random.nextInt(1000),
            random.nextInt(1000),
            0);
    DiskInfo diskInfo8 =
        new DiskInfo(
            "/mnt/disk2",
            random.nextInt() + 95 * 1024 * 1024 * 1024L,
            random.nextInt(1000),
            random.nextInt(1000),
            0);
    DiskInfo diskInfo9 =
        new DiskInfo(
            "/mnt/disk3",
            random.nextInt() + 90 * 1024 * 1024 * 1024L,
            random.nextInt(1000),
            random.nextInt(1000),
            0);
    diskInfo7.maxSlots_$eq(diskInfo7.actualUsableSpace() / assumedPartitionSize);
    diskInfo8.maxSlots_$eq(diskInfo8.actualUsableSpace() / assumedPartitionSize);
    diskInfo9.maxSlots_$eq(diskInfo9.actualUsableSpace() / assumedPartitionSize);
    if (hasDisks) {
      disks3.put("/mnt/disk2", diskInfo8);
      disks3.put("/mnt/disk1", diskInfo7);
      disks3.put("/mnt/disk3", diskInfo9);
    }

    ArrayList<WorkerInfo> workers = new ArrayList<>(3);
    workers.add(new WorkerInfo("host1", 9, 10, 110, 113, disks1, null));
    workers.add(new WorkerInfo("host2", 9, 11, 111, 114, disks2, null));
    workers.add(new WorkerInfo("host3", 9, 12, 112, 115, disks3, null));
    return workers;
  }

  @Test
  public void testAllocateSlotsForEmptyPartitionId() {
    final List<WorkerInfo> workers = prepareWorkers(true);
    final List<Integer> partitionIds = Collections.emptyList();
    final boolean shouldReplicate = true;

    check(workers, partitionIds, shouldReplicate, true);
  }

  @Test
  public void testAllocateSlotsForSinglePartitionId() {
    final List<WorkerInfo> workers = prepareWorkers(true);
    final List<Integer> partitionIds = Collections.singletonList(0);
    final boolean shouldReplicate = true;

    check(workers, partitionIds, shouldReplicate, true);
  }

  @Test
  public void testAllocateSlotsForSinglePartitionIdWithoutReplicate() {
    final List<WorkerInfo> workers = prepareWorkers(true);
    final List<Integer> partitionIds = Collections.singletonList(0);
    final boolean shouldReplicate = false;

    check(workers, partitionIds, shouldReplicate, true);
  }

  @Test
  public void testAllocateSlotsForTwoPartitionIdsWithoutReplicate() {
    final List<WorkerInfo> workers = prepareWorkers(true);
    final List<Integer> partitionIds = Arrays.asList(0, 1);
    final boolean shouldReplicate = false;

    check(workers, partitionIds, shouldReplicate, true);
  }

  @Test
  public void testAllocateSlotsForThreePartitionIdsWithoutReplicate() {
    final List<WorkerInfo> workers = prepareWorkers(true);
    final List<Integer> partitionIds = Arrays.asList(0, 1, 2);
    final boolean shouldReplicate = false;

    check(workers, partitionIds, shouldReplicate, true);
  }

  @Test
  public void testAllocateSlotsForThreeReduceIdsWithReplicate() {
    final List<WorkerInfo> workers = prepareWorkers(true);
    final List<Integer> partitionIds = Arrays.asList(0, 1, 2);
    final boolean shouldReplicate = true;

    check(workers, partitionIds, shouldReplicate, true);
  }

  @Test
  public void testAllocate3000ReduceIdsWithReplicate() {
    final List<WorkerInfo> workers = prepareWorkers(true);
    final List<Integer> partitionIds = new ArrayList<>();
    for (int i = 0; i < 3000; i++) {
      partitionIds.add(i);
    }
    final boolean shouldReplicate = true;

    check(workers, partitionIds, shouldReplicate, true);
  }

  @Test
  public void testAllocate3000ReduceIdsWithoutReplicate() {
    final List<WorkerInfo> workers = prepareWorkers(true);
    final List<Integer> partitionIds = new ArrayList<>();
    for (int i = 0; i < 3000; i++) {
      partitionIds.add(i);
    }
    final boolean shouldReplicate = true;

    check(workers, partitionIds, shouldReplicate, true);
  }

  private void check(
      List<WorkerInfo> workers,
      List<Integer> partitionIds,
      boolean shouldReplicate,
      boolean expectSuccess) {
    String shuffleKey = "appId-1";
    CelebornConf conf = new CelebornConf();
    conf.set(CelebornConf.MASTER_SLOT_ASSIGN_LOADAWARE_DISKGROUP_NUM(), 2);
    conf.set(CelebornConf.MASTER_SLOT_ASSIGN_LOADAWARE_DISKGROUP_GRADIENT(), 1.0);
    conf.set(CelebornConf.WORKER_DISK_RESERVE_SIZE(), 10 * 1024 * 1024 * 1024L);
    conf.set(CelebornConf.WORKER_DISK_RESERVE_RATIO(), 0.1);
    Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>> slots =
        SlotsAllocator.offerSlotsLoadAware(
            workers,
            partitionIds,
            shouldReplicate,
            false,
            conf,
            StorageInfo.ALL_TYPES_AVAILABLE_MASK);
    if (expectSuccess) {
      if (shouldReplicate) {
        slots.forEach(
            (k, v) -> {
              Set<String> locationDuplicationSet = new HashSet<>();
              v._1.forEach(
                  i -> {
                    String uniqueId = i.getUniqueId();
                    assert !locationDuplicationSet.contains(uniqueId);
                    locationDuplicationSet.add(uniqueId);
                    for (PartitionLocation location : v._1) {
                      assert location.getHost().equals(k.host())
                          && location.getRpcPort() == k.rpcPort()
                          && location.getPushPort() == k.pushPort()
                          && location.getFetchPort() == k.fetchPort();
                    }
                  });
            });
      }
      Map<WorkerInfo, Map<String, Integer>> workerToAllocatedSlots =
          SlotsAllocator.slotsToDiskAllocations(slots);
      int unknownDiskSlots = 0;
      for (Map.Entry<WorkerInfo, Map<String, Integer>> entry : workerToAllocatedSlots.entrySet()) {
        WorkerInfo worker = entry.getKey();
        Map<String, Integer> allocationMap = entry.getValue();
        worker.allocateSlots(shuffleKey, allocationMap);
        if (allocationMap.containsKey("UNKNOWN_DISK")) {
          unknownDiskSlots += allocationMap.get("UNKNOWN_DISK");
        }
      }
      int allocateToDiskSlots = 0;
      for (WorkerInfo worker : workers) {
        allocateToDiskSlots += worker.usedSlots();
      }
      if (shouldReplicate) {
        Assert.assertTrue(partitionIds.size() * 2 >= unknownDiskSlots + allocateToDiskSlots);
      } else {
        Assert.assertTrue(partitionIds.size() >= unknownDiskSlots + allocateToDiskSlots);
      }
      Assert.assertEquals(0, unknownDiskSlots);
    } else {
      assert slots.isEmpty()
          : "Expect to fail to offer slots, but return " + slots.size() + " slots.";
    }
  }

  private void checkSlotsOnHDFS(
      List<WorkerInfo> workers,
      List<Integer> partitionIds,
      boolean shouldReplicate,
      boolean expectSuccess) {
    String shuffleKey = "appId-1";
    CelebornConf conf = new CelebornConf();
    conf.set("celeborn.active.storage.levels", "HDFS");
    Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>> slots =
        SlotsAllocator.offerSlotsRoundRobin(
            workers, partitionIds, shouldReplicate, false, StorageInfo.ALL_TYPES_AVAILABLE_MASK);

    int allocatedPartitionCount = 0;

    for (Map.Entry<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>>
        workerToPartitions : slots.entrySet()) {
      WorkerInfo workerInfo = workerToPartitions.getKey();
      List<PartitionLocation> primaryLocs = workerToPartitions.getValue()._1;
      List<PartitionLocation> replicaLocs = workerToPartitions.getValue()._2();
      allocatedPartitionCount += primaryLocs.size();
      allocatedPartitionCount += replicaLocs.size();
    }
    if (expectSuccess) {
      Assert.assertEquals(slots.isEmpty(), false);
    } else {
      Assert.assertEquals(slots.isEmpty(), true);
    }
    if (shouldReplicate) {
      Assert.assertEquals(allocatedPartitionCount, partitionIds.size() * 2);
    } else {
      Assert.assertEquals(allocatedPartitionCount, partitionIds.size());
    }
  }

  @Test
  public void testHDFSOnly() {
    final List<WorkerInfo> workers = prepareWorkers(false);
    final List<Integer> partitionIds = new ArrayList<>();
    for (int i = 0; i < 3000; i++) {
      partitionIds.add(i);
    }
    final boolean shouldReplicate = true;
    checkSlotsOnHDFS(workers, partitionIds, shouldReplicate, true);
  }
}
