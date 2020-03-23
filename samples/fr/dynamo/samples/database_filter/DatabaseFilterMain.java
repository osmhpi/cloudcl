package fr.dynamo.samples.database_filter;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.logging.Logger;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class DatabaseFilterMain {

  public static void main(String[] args) throws InterruptedException, IOException {
    if (args.length != 2) {
      System.out.println("Usage: DatabaseFilterMain size tiles");
      System.exit(1);
    }

    final String tpchLineItemFile = args[0];
    final int tiles = Integer.parseInt(args[1]);

    ThreadFinishedNotifyable finishedNotifyable = thread -> {
      DatabaseFilterKernel kernel = (DatabaseFilterKernel) thread.getKernel();
      kernel.get(kernel.resultSumQtyHi);
      kernel.get(kernel.resultSumQtyLo);
      kernel.get(kernel.resultSumBasePriceHi);
      kernel.get(kernel.resultSumBasePriceLo);
      kernel.get(kernel.resultSumDiscPriceHi);
      kernel.get(kernel.resultSumDiscPriceLo);
      kernel.get(kernel.resultSumChargeHi);
      kernel.get(kernel.resultSumChargeLo);
      kernel.get(kernel.resultSumDiscountHi);
      kernel.get(kernel.resultSumDiscountLo);
      kernel.get(kernel.resultCountOrder);
    };

    DynamoJob job = new DatabaseFilterJob(tpchLineItemFile, tiles, DevicePreference.GPU_ONLY, finishedNotifyable);
    DynamoExecutor.instance().submit(job);

    job.awaitTermination(1, TimeUnit.DAYS);

    long[] overallSumQty = new long[6];
    long[] overallSumBasePrice = new long[6];
    long[] overallSumDiscPrice = new long[6];
    long[] overallSumCharge = new long[6];
    long[] overallSumDiscount = new long[6];
    int[] overallCountOrder = new int[6];
    for(DynamoKernel k:job.getFinishedKernels()){
      DatabaseFilterKernel kernel = (DatabaseFilterKernel)k;
      for (int i = 0; i < 6; i++) {
        overallSumQty[i] += (((long) kernel.resultSumQtyHi[i]) << 32) | (kernel.resultSumQtyLo[i] & 0xffffffffL);
        overallSumBasePrice[i] += (((long) kernel.resultSumBasePriceHi[i]) << 32) | (kernel.resultSumBasePriceLo[i] & 0xffffffffL);
        overallSumDiscPrice[i] += (((long) kernel.resultSumDiscPriceHi[i]) << 32) | (kernel.resultSumDiscPriceLo[i] & 0xffffffffL);
        overallSumCharge[i] += (((long) kernel.resultSumChargeHi[i]) << 32) | (kernel.resultSumChargeLo[i] & 0xffffffffL);
        overallSumDiscount[i] += (((long) kernel.resultSumDiscountHi[i]) << 32) | (kernel.resultSumDiscountLo[i] & 0xffffffffL);
        overallCountOrder[i] += kernel.resultCountOrder[i];
      }
    }

    // TODO: Summarize stats (averages), etc.
    System.out.println("overallSumQty: " + Arrays.toString(overallSumQty));
    System.out.println("overallSumBasePrice: " + Arrays.toString(overallSumBasePrice));
    System.out.println("overallSumDiscPrice: " + Arrays.toString(overallSumDiscPrice));
    System.out.println("overallSumCharge: " + Arrays.toString(overallSumCharge));
    System.out.println("overallSumDiscount: " + Arrays.toString(overallSumDiscount));
    System.out.println("overallCountOrder: " + Arrays.toString(overallCountOrder));

    PerformanceCache.getInstance().printStatistics(job);
    Logger.instance().info(job);

    job.cleanUp();
  }

}