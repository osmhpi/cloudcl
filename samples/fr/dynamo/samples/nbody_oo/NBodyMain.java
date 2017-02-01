package fr.dynamo.samples.nbody_oo;

import java.util.concurrent.TimeUnit;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.threading.DynamoJob;

public class NBodyMain{

  public static void main(String _args[]) throws InterruptedException {
     final int bodyCount = 32000;

     DynamoJob job = new DynamoJob("NBody");

     NBodyKernel kernel = new NBodyKernel(job, Range.create(bodyCount), DevicePreference.GPU_ONLY);
     job.addKernel(kernel);

     System.out.println(kernel.bodies[0].x + " " + kernel.bodies[0].y);

     for(int i = 0; i<10; i++){
       job.reset();
       DynamoExecutor.instance().submit(job);
       job.awaitTermination(1, TimeUnit.DAYS);
       System.out.println(kernel.bodies[0].x + " " + kernel.bodies[0].y);
     }
  }

}