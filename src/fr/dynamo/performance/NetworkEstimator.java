package fr.dynamo.performance;

import fr.dynamo.threading.DynamoKernel;

public class NetworkEstimator {

  public static long calculateTranferTime(DynamoKernel kernel, NetworkSpeed speed){
    return Math.round(kernel.getTransferSize() / (double)speed.bytesPerSecond * 1000);
  }

}
