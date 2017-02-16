package fr.dynamo.test;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.amd.aparapi.Range;

import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.threading.DynamoJob;
//import static org.junit.Assert.assertEquals;
import fr.dynamo.threading.DynamoKernel;
import junit.framework.Assert;


public class DynamoExecutorTest {


  @Test
  public void testFailedJobsFinish() throws InterruptedException {
    DynamoJob job = new DynamoJob("Fail");

    DynamoKernel kernel = new DynamoKernel(job, Range.create(1)) {
      @Override
      public void run() {
        System.out.println("FAIL");
      }
    };
    job.addKernel(kernel);

    DynamoKernel kernel2 = new DynamoKernel(job, Range.create(1)) {
      @Override
      public void run() {
        System.out.println("FAIL");
      }
    };
    job.addKernel(kernel2);

    DynamoKernel kernel3 = new DynamoKernel(job, Range.create(1)) {
      @Override
      public void run() {
        System.out.println("FAIL");
      }
    };
    job.addKernel(kernel3);

    DynamoExecutor.instance().submit(job);

    job.awaitTermination(10, TimeUnit.SECONDS);

    Assert.assertTrue(job.isTerminated());
  }

  @Test
  public void testRetries() throws InterruptedException {
    DynamoJob job = new DynamoJob("Fail");

    DynamoKernel kernel = new DynamoKernel(job, Range.create(1)) {
      @Override
      public void run() {
        System.out.println("FAIL");
      }
    };
    kernel.setRemainingTries(2);
    job.addKernel(kernel);

    DynamoExecutor.instance().submit(job);

    job.awaitTermination(10, TimeUnit.SECONDS);

    Assert.assertTrue(job.isTerminated());
  }
}
