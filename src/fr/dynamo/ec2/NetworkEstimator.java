package fr.dynamo.ec2;

import fr.dynamo.threading.TileKernel;

public class NetworkEstimator {

  public static long calculateTranferTime(TileKernel kernel, NetworkSpeed speed){
    return Math.round(kernel.getTransferSize() / (double)speed.bytesPerSecond * 1000);
  }

}
