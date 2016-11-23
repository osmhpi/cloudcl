package fr.dynamo.samples.nbody;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.amd.aparapi.Range;

import fr.dynamo.execution.KernelExecutor;

public class NBodyDynamo {

  public final static int PICTURESIZE = 2000;
  public final static float UNIVERSE_RADIUS = 1e18f;
  public static final float SOLARMASS = 1.98892e30f;
  public static final float DT_$constant$ = 1e14f;
  public static void main(String[] args) throws InterruptedException {

    KernelExecutor executor = new KernelExecutor();

    int particleCount = Integer.parseInt(args[0]);
    float[] x = new float[particleCount];
    float[] y = new float[particleCount];
    float[] mass = new float[particleCount];

    Random random = new Random(1000);

    //Create the sun
    x[0] = UNIVERSE_RADIUS / 2;
    y[0] = UNIVERSE_RADIUS / 2;
    mass[0] = SOLARMASS;

    for(int i = 1; i<particleCount; i++){
      x[i] = (float) Math.abs(random.nextDouble() * UNIVERSE_RADIUS);
      y[i] = (float) Math.abs(random.nextDouble() * UNIVERSE_RADIUS);
      mass[i] = (float) Math.abs(random.nextDouble() * SOLARMASS / 100.0);

    }

    NBodyKernel kernel = new NBodyKernel(Range.create(particleCount, 100), x, y, mass);

    long before = System.currentTimeMillis();
    executor.execute(kernel);
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.DAYS);


    for (int i = 0; i < particleCount; i++) {
      kernel.vx[i] += DT_$constant$ * kernel.fx[i] / kernel.mass[i];
      kernel.vy[i] += DT_$constant$ * kernel.fy[i] / kernel.mass[i];
      kernel.x[i] += DT_$constant$ * kernel.vx[i];
      kernel.y[i] += DT_$constant$ * kernel.vy[i];
     }
    long after = System.currentTimeMillis();

    System.out.println(after - before + " ms");
  }

  public static void update(int i, double dt) {

  }

}
