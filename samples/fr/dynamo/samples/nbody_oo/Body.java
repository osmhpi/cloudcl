package fr.dynamo.samples.nbody_oo;

public final class Body {
  private float x, y, z, m, vx, vy, vz;

  public Body(float _x, float _y, float _z, float _m) {
     x = _x;
     y = _y;
     z = _z;
     m = _m;
  }

  public float getX() {
     return x;
  }

  public float getY() {
     return y;
  }

  public float getZ() {
     return z;
  }

  public float getVx() {
     return vx;
  }

  public float getVy() {
     return vy;
  }

  public float getVz() {
     return vz;
  }

  public float getM() {
     return m;
  }

  public void setX(float _x) {
     x = _x;
  }

  public void setY(float _y) {
     y = _y;
  }

  public void setZ(float _z) {
     z = _z;
  }

  public void setVx(float _vx) {
     vx = _vx;
  }

  public void setVy(float _vy) {
     vy = _vy;
  }

  public void setVz(float _vz) {
     vz = _vz;
  }
}