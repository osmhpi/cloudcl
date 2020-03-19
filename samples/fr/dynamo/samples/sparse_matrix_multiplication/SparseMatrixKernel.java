package fr.dynamo.samples.sparse_matrix_multiplication;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class SparseMatrixKernel extends DynamoKernel{

  final float[] a;
  final float[] b;
  public int[] overallSum;

  final int sizeN;
  final int sizeM;
  final int sizeP;

  public SparseMatrixKernel(DynamoJob job, Range range, float[] a, float[] b,
    int sizeN, int sizeM, int sizeP) {
    super(job, range);
    this.a = a;
    this.b = b;
    this.overallSum = new int[] { 0 };
    this.sizeN = sizeN;
    this.sizeM = sizeM;
    this.sizeP = sizeP;
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
