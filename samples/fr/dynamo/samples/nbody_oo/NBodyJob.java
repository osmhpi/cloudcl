package fr.dynamo.samples.nbody_oo;

import java.util.concurrent.TimeUnit;

import fr.dynamo.DevicePreference;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.threading.DynamoJob;

public class NBodyJob extends DynamoJob{

  public NBodyJob(int bodyCount, int steps, DevicePreference preference) throws InterruptedException {
    super("NBody");
    Body[] bodies = createBodies(bodyCount);

    NBodyKernel kernel = new NBodyKernel(this, bodies.clone(), preference);
    addKernel(kernel);

    for(int i = 0; i<steps; i++){
      reset();
      DynamoExecutor.instance().submit(this);
      awaitTermination(1, TimeUnit.DAYS);
    }
  }


  public static Body[] createBodies(int count){
    Body[] bodies = new Body[count];

    final float maxDist = 20f;
    for (int body = 0; body < count; body++) {
      final float theta = (float) (Math.random() * Math.PI * 2);
      final float phi = (float) (Math.random() * Math.PI * 2);
      final float radius = (float) (Math.random() * maxDist);

      float x = (float) (radius * Math.cos(theta) * Math.sin(phi));
      float y = (float) (radius * Math.sin(theta) * Math.sin(phi));
      float z = (float) (radius * Math.cos(phi));

      if ((body % 2) == 0) {
        x += maxDist * 1.5;
      } else {
        x -= maxDist * 1.5;
      }
      bodies[body] = new Body(x, y, z, 5f);
    }
    return bodies;
  }

}
