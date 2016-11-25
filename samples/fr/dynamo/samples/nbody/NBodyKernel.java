package fr.dynamo.samples.nbody;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class NBodyKernel extends DynamoKernel{

  public static final float G_$constant$ = 6.673e-11f;   // gravitational constant
  public static final float EPS_$constant$ = 3E4f;

  public float[] x;
  public float[] y;
  public float[] vx;
  public float[] vy;
  public float[] fx;
  public float[] fy;
  public float[] mass;

  public NBodyKernel(DynamoJob job, Range range, float[] x, float[] y, float[] mass) {
    super(job, range);
    this.x = x;
    this.y = y;
    this.mass = mass;
    this.vx = new float[x.length];
    this.vy = new float[x.length];
    this.fx = new float[x.length];
    this.fy = new float[x.length];
  }

  @Override
  public void run() {
    int i = getGlobalId(0);

    fx[i] = 0;
    fy[i] = 0;

    for (int j = 0; j < getGlobalSize(); j++) {
      float dx = x[i] - x[j];
      float dy = y[i] - y[j];
      float dist = (float) Math.sqrt(dx*dx + dy*dy);

      float F = (G_$constant$ * mass[i] * mass[j]) / (dist*dist + EPS_$constant$*EPS_$constant$);
      fx[i] += F * dx / dist;
      fy[i] += F * dy / dist;
    }
  }

}