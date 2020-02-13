package fr.dynamo.samples.nbody_oo;

import fr.dynamo.DevicePreference;

public class NBodyMain{

  public static void main(String[] args) throws InterruptedException {
   final int bodyCount = Integer.parseInt(args[0]);
   final int steps = Integer.parseInt(args[1]);

   NBodyJob job = new NBodyJob(bodyCount, steps, DevicePreference.NONE);
  }



}