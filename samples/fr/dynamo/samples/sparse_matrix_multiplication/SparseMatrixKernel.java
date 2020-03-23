package fr.dynamo.samples.sparse_matrix_multiplication;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class SparseMatrixKernel extends DynamoKernel{

  private final float[] a;
  private final float[] b;
  public int[] overallSum;

  private final int sizeM;

  public SparseMatrixKernel(DynamoJob job, Range range, float[] a, float[] b, int sizeM) {
    super(job, range);
    this.a = a;
    this.b = b;
    this.overallSum = new int[] { 0 };
    this.sizeM = sizeM;
  }

  @Override
  public void run() {
    int y = getGlobalId(0);
    int x = getGlobalId(1);

    float sum = 0;
    for(int i=0; i<sizeM; i++){
      sum += a[y * sizeM + i] * b[x * sizeM + i];
    }
    // Unfortunately can't atomicAdd float, but that's good enough
    atomicAdd(overallSum, 0, (int)sum);
  }
}
