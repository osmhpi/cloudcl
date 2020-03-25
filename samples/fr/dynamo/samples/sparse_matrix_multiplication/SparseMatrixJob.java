package fr.dynamo.samples.sparse_matrix_multiplication;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.threading.DynamoJob;

public class SparseMatrixJob extends DynamoJob{

  public SparseMatrixJob(int sizeN, int sizeM, int sizeP, float sparsity, int tiles, DevicePreference preference, ThreadFinishedNotifyable notifyable) {
    super("SparseMatrix", notifyable);

    final float[] a = new float[sizeN*sizeM];
    final float[] b = new float[sizeM*sizeP];

    int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    IntStream.range(0, NUM_THREADS).parallel().forEach(ps -> {
      Random random = new Random(1000 + ps);
      for (int i = ps * (a.length / NUM_THREADS) + Math.min(ps, a.length % NUM_THREADS);
           i < (ps + 1) * (a.length / NUM_THREADS) + Math.min(ps + 1, a.length % NUM_THREADS); i++) {
        a[i] = random.nextFloat() >= sparsity ? (random.nextFloat() * 2 - 1) : 0;
      }
      for (int i = ps * (b.length / NUM_THREADS) + Math.min(ps, b.length % NUM_THREADS);
           i < (ps + 1) * (b.length / NUM_THREADS) + Math.min(ps + 1, b.length % NUM_THREADS); i++) {
        b[i] = random.nextFloat() >= sparsity ? (random.nextFloat() * 2 - 1) : 0;
      }
    });

    int tileHeight = sizeN/tiles;
    for(int tile=0; tile<tiles; tile++){
      float[] aSplit = Arrays.copyOfRange(a, tile*tileHeight*sizeM, (tile+1)*tileHeight*sizeM);

      Range range = Range.create2D(tileHeight, sizeP, 100, 1);
      SparseMatrixKernel kernel = new SparseMatrixKernel(this, range, aSplit, b, sizeM);
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
