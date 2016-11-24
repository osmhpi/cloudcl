package fr.dynamo.samples.matrix_multiplication;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoKernel;

public class MatrixKernel extends DynamoKernel{

  final float[] a;
  final float[] b;
  final float[] result;

  final int size_$constant$;

  public MatrixKernel(Range range, float[] a, float[] b, float[] result, int size) {
    super("Matrix", range);
    this.a = a;
    this.b = b;
    this.result = result;

    this.size_$constant$ = size;
  }

  @Override
  public void run() {
    int y = getGlobalId(0);
    int x = getGlobalId(1);

    float sum = 0;
    for(int i=0; i<size_$constant$; i++){
      sum += a[y * size_$constant$ + i] * b[x * size_$constant$ + i];
    }
    result[y*size_$constant$+x] = sum;
  }
}
