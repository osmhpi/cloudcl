package fr.dynamo.samples.nbody_oo;

import fr.dynamo.DevicePreference;

public class NBodyMain{

  public static void main(String[] args) throws InterruptedException {
    if (args.length != 2) {
      System.out.println("Usage: NBodyMain bodyCount steps");
      System.exit(1);
    }

    final int bodyCount = Integer.parseInt(args[0]);
    final int steps = Integer.parseInt(args[1]);

    new NBodyJob(bodyCount, steps, DevicePreference.NONE);
  }
}