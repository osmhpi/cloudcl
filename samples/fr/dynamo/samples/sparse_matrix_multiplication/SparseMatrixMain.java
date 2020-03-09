package fr.dynamo.samples.sparse_matrix_multiplication;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.concurrent.TimeUnit;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.logging.Logger;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;
import fr.dynamo.threading.DynamoThread;

public class SparseMatrixMain {

  public static void main(String[] args) throws InterruptedException, IOException {
    if (args.length != 3) {
      System.out.println("Usage: SparseMatrixMain size sparsity tiles");
      System.exit(1);
    }

    final int size = Integer.parseInt(args[0]);
    final float sparsity = Float.parseFloat(args[1]);
    final int tiles = Integer.parseInt(args[2]);

    ThreadFinishedNotifyable matrixNotifyable = new ThreadFinishedNotifyable() {
      @Override
      public void notifyListener(DynamoThread thread) {
        SparseMatrixKernel kernel = (SparseMatrixKernel) thread.getKernel();
        kernel.get(kernel.overallSum);
      }
    };

    DynamoJob job = new SparseMatrixJob(size, sparsity, tiles, DevicePreference.NONE, matrixNotifyable);
    DynamoExecutor.instance().submit(job);

    job.awaitTermination(1, TimeUnit.DAYS);

    float overallSum = 0;
    for(DynamoKernel k:job.getFinishedKernels()){
      SparseMatrixKernel matrixKernel = (SparseMatrixKernel)k;
      overallSum += matrixKernel.overallSum[0];
    }

    // Since all numbers in the input matrices are either zeroes or random values
    // between -1 and 1, we expect with high probability that the final overall
    // sum is close to zero, so check this with some heuristic bounds
    float overallSumLowerBound = -(float)Math.pow((float)size, 1.5f);
    float overallSumHigherBound = (float)Math.pow((float)size, 1.5f);

    System.out.println("Got overall sum " + overallSum + ", expecting between " + overallSumLowerBound + " and " + overallSumHigherBound);

    if(overallSum < overallSumLowerBound || overallSum > overallSumHigherBound){
      throw new UnexpectedException("Checksum error when checking the resulting matrix.");
    }

    PerformanceCache.getInstance().printStatistics(job);
    Logger.instance().info(job);

    job.cleanUp();
  }

}