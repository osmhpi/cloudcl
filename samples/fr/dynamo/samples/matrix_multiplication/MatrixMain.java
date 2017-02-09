package fr.dynamo.samples.matrix_multiplication;
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

public class MatrixMain {

  public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
    final int size = Integer.parseInt(args[0]);
    final int tiles = Integer.parseInt(args[1]);

    ThreadFinishedNotifyable matrixNotifyable = new ThreadFinishedNotifyable() {
      @Override
      public void notifyListener(DynamoThread thread) {
        MatrixKernel kernel = (MatrixKernel) thread.getKernel();
        kernel.get(kernel.result);
      }
    };

    DynamoJob job = new MatrixJob(size, tiles, DevicePreference.NONE, matrixNotifyable);
    DynamoExecutor.instance().submit(job);

    job.awaitTermination(1, TimeUnit.DAYS);

    int zeroes = 0;
    for(DynamoKernel k:job.getFinishedKernels()){
      MatrixKernel matrixKernel = (MatrixKernel)k;

      for(float f : matrixKernel.result){
        if(f == 0){
          zeroes++;
        }
      }
    }

    if(zeroes > 0){
      throw new UnexpectedException("Checksum error when checking the resulting matrix.");
    }

    PerformanceCache.getInstance().printStatistics(job);
    Logger.instance().info(job);

    job.cleanUp();
  }

}