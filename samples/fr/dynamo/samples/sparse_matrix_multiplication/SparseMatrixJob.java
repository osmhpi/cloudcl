package fr.dynamo.samples.sparse_matrix_multiplication;

import java.util.Arrays;
import java.util.Random;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.threading.DynamoJob;

public class SparseMatrixJob extends DynamoJob{

  public SparseMatrixJob(int size, float sparsity, int tiles, DevicePreference preference, ThreadFinishedNotifyable notifyable) {
    super("SparseMatrix", notifyable);

    int tileHeight = size/tiles;

    Random random = new Random();
    random.setSeed(1000);

    final float[] a = new float[size*size];
    final float[] b = new float[size*size];
    for (int i = 0; i < a.length; i++) {
      a[i] = random.nextFloat() >= sparsity ? (random.nextFloat() * 2 - 1) : 0;
    }
    for (int i = 0; i < b.length; i++) {
      b[i] = random.nextFloat() >= sparsity ? (random.nextFloat() * 2 - 1) : 0;
    }

    for(int tile=0; tile<tiles; tile++){
      float[] aSplit = Arrays.copyOfRange(a, tile*tileHeight*size, (tile+1)*tileHeight*size);

      Range range = Range.create2D(tileHeight, size, 100, 1);

      SparseMatrixKernel kernel = new SparseMatrixKernel(this, range, aSplit, b, size);
      kernel.setDevicePreference(preference);
      kernel.setExplicit(true);
      kernel.put(aSplit);
      kernel.put(b);
      addKernel(kernel);
    }
  }

}
