package fr.dynamo.samples.sparse_matrix_multiplication;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class SparseMatrixKernel extends DynamoKernel{

  final float[] a;
  final float[] b;
  public int[] overallSum;

  final int size;

  public SparseMatrixKernel(DynamoJob job, Range range, float[] a, float[] b, int size) {
    super(job, range);
    this.a = a;
    this.b = b;
    this.overallSum = new int[] { 0 };
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
    // Unfortunately can't atomicAdd float, but that's good enough
    atomicAdd(overallSum, 0, (int)sum);
  }
}
