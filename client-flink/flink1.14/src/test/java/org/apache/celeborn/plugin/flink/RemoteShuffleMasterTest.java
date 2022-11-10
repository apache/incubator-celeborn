package org.apache.celeborn.plugin.flink;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.clusterframework.types.ResourceID;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;
import org.apache.flink.runtime.jobgraph.IntermediateResultPartitionID;
import org.apache.flink.runtime.shuffle.JobShuffleContext;
import org.apache.flink.runtime.shuffle.PartitionDescriptor;
import org.apache.flink.runtime.shuffle.ProducerDescriptor;
import org.apache.flink.runtime.shuffle.ShuffleMasterContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.util.PackedPartitionId;

public class RemoteShuffleMasterTest {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteShuffleMasterTest.class);
  private RemoteShuffleMaster remoteShuffleMaster;

  @Before
  public void setUp() {
    Configuration configuration = new Configuration();
    remoteShuffleMaster = createShuffleMaster(configuration);
  }

  @Test
  public void testRegisterJob() {
    JobShuffleContext jobShuffleContext = createJobShuffleContext(JobID.generate());
    remoteShuffleMaster.registerJob(jobShuffleContext);

    // reRunRegister job
    try {
      remoteShuffleMaster.registerJob(jobShuffleContext);
    } catch (Exception e) {
      Assert.assertTrue(true);
    }

    // unRegister job
    remoteShuffleMaster.unregisterJob(jobShuffleContext.getJobId());
  }

  @Test
  public void testRegisterPartitionWithProducer()
      throws UnknownHostException, ExecutionException, InterruptedException {
    JobID jobID = JobID.generate();
    JobShuffleContext jobShuffleContext = createJobShuffleContext(jobID);
    remoteShuffleMaster.registerJob(jobShuffleContext);

    IntermediateDataSetID intermediateDataSetID = new IntermediateDataSetID();
    PartitionDescriptor partitionDescriptor = createPartitionDescriptor(intermediateDataSetID, 0);
    ProducerDescriptor producerDescriptor = createProducerDescriptor();
    RemoteShuffleDescriptor remoteShuffleDescriptor =
        remoteShuffleMaster
            .registerPartitionWithProducer(jobID, partitionDescriptor, producerDescriptor)
            .get();
    ShuffleResource shuffleResource = remoteShuffleDescriptor.getShuffleResource();
    ShuffleResourceDescriptor mapPartitionShuffleDescriptor =
        shuffleResource.getMapPartitionShuffleDescriptor();
    System.out.println(mapPartitionShuffleDescriptor.toString());
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getShuffleId());
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getPartitionId());
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getAttemptId());
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getMapId());

    // use same dataset id
    partitionDescriptor = createPartitionDescriptor(intermediateDataSetID, 1);
    remoteShuffleDescriptor =
        remoteShuffleMaster
            .registerPartitionWithProducer(jobID, partitionDescriptor, producerDescriptor)
            .get();
    mapPartitionShuffleDescriptor =
        remoteShuffleDescriptor.getShuffleResource().getMapPartitionShuffleDescriptor();
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getShuffleId());
    Assert.assertEquals(1, mapPartitionShuffleDescriptor.getMapId());

    // use another attemptId
    producerDescriptor = createProducerDescriptor();
    remoteShuffleDescriptor =
        remoteShuffleMaster
            .registerPartitionWithProducer(jobID, partitionDescriptor, producerDescriptor)
            .get();
    mapPartitionShuffleDescriptor =
        remoteShuffleDescriptor.getShuffleResource().getMapPartitionShuffleDescriptor();
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getShuffleId());
    Assert.assertEquals(
        PackedPartitionId.packedPartitionId(1, 1), mapPartitionShuffleDescriptor.getPartitionId());
    Assert.assertEquals(1, mapPartitionShuffleDescriptor.getAttemptId());
    Assert.assertEquals(1, mapPartitionShuffleDescriptor.getMapId());
  }

  @After
  public void tearDown() {
    if (remoteShuffleMaster != null) {
      try {
        remoteShuffleMaster.close();
      } catch (Exception e) {
        LOG.warn(e.getMessage(), e);
      }
    }
  }

  public RemoteShuffleMaster createShuffleMaster(Configuration configuration) {
    remoteShuffleMaster =
        new RemoteShuffleMaster(
            new ShuffleMasterContext() {
              @Override
              public Configuration getConfiguration() {
                return configuration;
              }

              @Override
              public void onFatalError(Throwable throwable) {
                System.exit(-1);
              }
            });

    return remoteShuffleMaster;
  }

  public JobShuffleContext createJobShuffleContext(JobID jobId) {
    return new JobShuffleContext() {
      @Override
      public org.apache.flink.api.common.JobID getJobId() {
        return jobId;
      }

      @Override
      public CompletableFuture<?> stopTrackingAndReleasePartitions(
          Collection<ResultPartitionID> collection) {
        return CompletableFuture.completedFuture(null);
      }
    };
  }

  public PartitionDescriptor createPartitionDescriptor(
      IntermediateDataSetID intermediateDataSetId, int partitionNum) {
    IntermediateResultPartitionID intermediateResultPartitionId =
        new IntermediateResultPartitionID(intermediateDataSetId, partitionNum);
    return new PartitionDescriptor(
        intermediateDataSetId,
        10,
        intermediateResultPartitionId,
        ResultPartitionType.BLOCKING,
        5,
        1);
  }

  public ProducerDescriptor createProducerDescriptor() throws UnknownHostException {
    ExecutionAttemptID executionAttemptId = new ExecutionAttemptID();
    return new ProducerDescriptor(
        ResourceID.generate(), executionAttemptId, InetAddress.getLocalHost(), 100);
  }
}
