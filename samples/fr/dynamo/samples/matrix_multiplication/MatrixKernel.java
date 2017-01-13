package fr.dynamo.samples.matrix_multiplication;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class MatrixKernel extends DynamoKernel{

  final float[] a;
  final float[] b;
  final float[] result;

  final int size;

  public MatrixKernel(DynamoJob job, Range range, float[] a, float[] b, float[] result, int size) {
    super(job, range);
    this.a = a;
    this.b = b;
    this.result = result;
    this.size = size;
  }

  @Override
  public void run() {
    int y = getGlobalId(0);
    int x = getGlobalId(1);

    float sum = 0;
    for(int i=0; i<size; i++){
      sum += a[y * size + i] * b[x * size + i];
    }
    result[y*size+x] = sum;
  }
}
