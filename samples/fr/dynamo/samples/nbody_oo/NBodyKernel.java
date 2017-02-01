package fr.dynamo.samples.nbody_oo;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class NBodyKernel extends DynamoKernel{

  protected final float delT = .005f;

  protected final float espSqr = 1.0f;

  protected final float mass = 5f;


  public Body[] bodies;

  public NBodyKernel(DynamoJob job, Range _range, DevicePreference preference) {
    super(job, _range, preference);

    bodies = new Body[range.getGlobalSize(0)];

    final float maxDist = 20f;
    for (int body = 0; body < range.getGlobalSize(0); body++) {
      final float theta = (float) (Math.random() * Math.PI * 2);
      final float phi = (float) (Math.random() * Math.PI * 2);
      final float radius = (float) (Math.random() * maxDist);

      // get the 3D dimensional coordinates
      float x = (float) (radius * Math.cos(theta) * Math.sin(phi));
      float y = (float) (radius * Math.sin(theta) * Math.sin(phi));
      float z = (float) (radius * Math.cos(phi));

      // divide into two 'spheres of bodies' by adjusting x
      if ((body % 2) == 0) {
        x += maxDist * 1.5;
      } else {
        x -= maxDist * 1.5;
      }
      bodies[body] = new Body(x, y, z, 5f);
    }

    Body.allBodies = bodies;
  }

  /**
   * Here is the kernel entrypoint. Here is where we calculate the position of each body
   */
  @Override public void run() {
    final int body = getGlobalId();

    float accx = 0.f;
    float accy = 0.f;
    float accz = 0.f;

    float myPosx = bodies[body].getX();
    float myPosy = bodies[body].getY();
    float myPosz = bodies[body].getZ();

    for (int i = 0; i < getGlobalSize(0); i++) {

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

