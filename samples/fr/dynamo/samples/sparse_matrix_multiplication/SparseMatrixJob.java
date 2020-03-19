package fr.dynamo.samples.sparse_matrix_multiplication;

import java.util.Arrays;
import java.util.Random;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.threading.DynamoJob;

public class SparseMatrixJob extends DynamoJob{

  public SparseMatrixJob(int sizeN, int sizeM, int sizeP, float sparsity, int tiles, DevicePreference preference, ThreadFinishedNotifyable notifyable) {
    super("SparseMatrix", notifyable);

    Random random = new Random();
    random.setSeed(1000);

    final float[] a = new float[sizeN*sizeM];
    final float[] b = new float[sizeM*sizeP];
    for (int i = 0; i < a.length; i++) {
      a[i] = random.nextFloat() >= sparsity ? (random.nextFloat() * 2 - 1) : 0;
    }
    for (int i = 0; i < b.length; i++) {
      b[i] = random.nextFloat() >= sparsity ? (random.nextFloat() * 2 - 1) : 0;
    }

    int tileHeight = sizeN/tiles;
    for(int tile=0; tile<tiles; tile++){
      float[] aSplit = Arrays.copyOfRange(a, tile*tileHeight*sizeM, (tile+1)*tileHeight*sizeM);

      Range range = Range.create2D(tileHeight, sizeP, 100, 1);
      SparseMatrixKernel kernel = new SparseMatrixKernel(this, range, aSplit, b, sizeN, sizeM, sizeP);
      kernel.setDevicePreference(preference);
      kernel.setExplicit(true);
      // IMPORTANT: The initial values for the kernel data should *not* be uploaded (put) here,
      //            because Aparapi already does this on its own on the first kernel run
      //            In fact, doing a 'put' here results in the data being uploaded twice!
      //kernel.put(aSplit);
      //kernel.put(b);
      addKernel(kernel);
    }
  }

}
