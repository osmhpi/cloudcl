package fr.dynamo.samples.kmeans;

import fr.dynamo.DevicePreference;

public class KMeansMain {

  public static void main(String[] args) throws InterruptedException {
    int pointCountPerKernel = Integer.parseInt(args[0]);
    int clusterCount = Integer.parseInt(args[1]);

    KMeansJob job = new KMeansJob(clusterCount, pointCountPerKernel, 100, DevicePreference.NONE, null);
  }

}
