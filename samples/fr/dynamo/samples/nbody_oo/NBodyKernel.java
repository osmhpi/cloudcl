package fr.dynamo.samples.nbody_oo;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class NBodyKernel extends DynamoKernel{

  protected final float delT = .005f;
  protected final float espSqr = 1.0f;

  public final Body[] bodies;
  public final int bodyCount;

  public NBodyKernel(DynamoJob job, Body[] bodies, DevicePreference preference) {
    super(job, Range.create(bodies.length), preference);
    this.bodies = bodies;
    this.bodyCount = bodies.length;
  }

  @Override public void run() {
    final int body = getGlobalId();

    float accx = 0.f;
    float accy = 0.f;
    float accz = 0.f;

    float myPosx = bodies[body].getX();
    float myPosy = bodies[body].getY();
    float myPosz = bodies[body].getZ();

    for (int i = 0; i < bodyCount; i++) {

      final float dx = bodies[i].getX() - myPosx;
      final float dy = bodies[i].getY() - myPosy;
      final float dz = bodies[i].getZ() - myPosz;
      final float invDist = rsqrt((dx * dx) + (dy * dy) + (dz * dz) + espSqr);
      final float s = bodies[i].getM() * invDist * invDist * invDist;
      accx = accx + (s * dx);
      accy = accy + (s * dy);
      accz = accz + (s * dz);
    }

    accx = accx * delT;
    accy = accy * delT;
    accz = accz * delT;
    bodies[body].setX(myPosx + (bodies[body].getVx() * delT) + (accx * .5f * delT));
    bodies[body].setY(myPosy + (bodies[body].getVy() * delT) + (accy * .5f * delT));
    bodies[body].setZ(myPosz + (bodies[body].getVz() * delT) + (accz * .5f * delT));

    bodies[body].setVx(bodies[body].getVx() + accx);
    bodies[body].setVy(bodies[body].getVy() + accy);
    bodies[body].setVz(bodies[body].getVz() + accz);
  }

}

