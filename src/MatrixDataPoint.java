
public final class MatrixDataPoint {
  
  final float a;
  final float b;
  float result;
  



  public MatrixDataPoint(float a, float b, float result) {
    super();
    this.a = a;
    this.b = b;
    this.result = result;
  }


  public float getA() {
    return a;
  }


  public float getB() {
    return b;
  }


  public float getResult() {
    return result;
  }

  
  public void setResult(float result) {
    this.result = result;
  }
  
}
