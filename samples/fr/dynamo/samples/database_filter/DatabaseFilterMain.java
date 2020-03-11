package fr.dynamo.samples.database_filter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.logging.Logger;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;
import fr.dynamo.threading.DynamoThread;


import java.util.concurrent.TimeUnit;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class DatabaseFilterMain {

  public static void main(String[] args) throws InterruptedException, IOException {
    if (args.length != 2) {
      System.out.println("Usage: DatabaseFilterMain size tiles");
      System.exit(1);
    }

    final String tpchLineItemFile = args[0];
    final int tiles = Integer.parseInt(args[1]);

    ThreadFinishedNotifyable finishedNotifyable = new ThreadFinishedNotifyable() {
      @Override
      public void notifyListener(DynamoThread thread) {
        DatabaseFilterKernel kernel = (DatabaseFilterKernel) thread.getKernel();
        kernel.get(kernel.resultSumQty);
        kernel.get(kernel.resultSumBasePrice);
        kernel.get(kernel.resultSumDiscPrice);
        kernel.get(kernel.resultSumCharge);
        kernel.get(kernel.resultSumDiscount);
        kernel.get(kernel.resultCountOrder);
      }
    };

    DynamoJob job = new DatabaseFilterJob(tpchLineItemFile, tiles, DevicePreference.GPU_ONLY, finishedNotifyable);
    DynamoExecutor.instance().submit(job);

    job.awaitTermination(1, TimeUnit.DAYS);

    int[] overallResultSumQty = new int[6];
    int[] overallResultSumBasePrice = new int[6];
    int[] overallResultSumDiscPrice = new int[6];
    int[] overallResultSumCharge = new int[6];
    int[] overallResultSumDiscount = new int[6];
    int[] overallResultCountOrder = new int[6];
    for(DynamoKernel k:job.getFinishedKernels()){
      DatabaseFilterKernel kernel = (DatabaseFilterKernel)k;
      for (int i = 0; i < 6; i++) {
        overallResultSumQty[i] += kernel.resultSumQty[i];
        overallResultSumBasePrice[i] += kernel.resultSumBasePrice[i];
        overallResultSumDiscPrice[i] += kernel.resultSumDiscPrice[i];
        overallResultSumCharge[i] += kernel.resultSumCharge[i];
        overallResultSumDiscount[i] += kernel.resultSumDiscount[i];
        overallResultCountOrder[i] += kernel.resultCountOrder[i];
      }
    }

    // TODO: Summarize stats (averages), etc.
    System.out.println("overallResultSumQty: " + Arrays.toString(overallResultSumQty));
    System.out.println("overallResultSumBasePrice: " + Arrays.toString(overallResultSumBasePrice));
    System.out.println("overallResultSumDiscPrice: " + Arrays.toString(overallResultSumDiscPrice));
    System.out.println("overallResultSumCharge: " + Arrays.toString(overallResultSumCharge));
    System.out.println("overallResultSumDiscount: " + Arrays.toString(overallResultSumDiscount));
    System.out.println("overallResultCountOrder: " + Arrays.toString(overallResultCountOrder));

    PerformanceCache.getInstance().printStatistics(job);
    Logger.instance().info(job);

    job.cleanUp();
  }

}