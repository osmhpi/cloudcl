package fr.dynamo.samples.kmeans;

import fr.dynamo.DevicePreference;

public class KMeansMain {

  public static void main(String[] args) throws InterruptedException {
    if (args.length != 2) {
      System.out.println("Usage: KMeansMain pointCountPerKernel clusterCount");
      System.exit(1);
    }

    int pointCountPerKernel = Integer.parseInt(args[0]);
    int clusterCount = Integer.parseInt(args[1]);

    new KMeansJob(clusterCount, pointCountPerKernel, 100, DevicePreference.NONE, null);
  }

}
