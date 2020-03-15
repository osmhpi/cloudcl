package fr.dynamo.samples.matrix_multiplication;

import java.util.Arrays;
import java.util.Random;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.threading.DynamoJob;

public class MatrixJob extends DynamoJob{

  public MatrixJob(int size, int tiles, DevicePreference preference, ThreadFinishedNotifyable notifyable) {
    super("Matrix", notifyable);

    Random random = new Random();
    random.setSeed(1000);

    final float[] a = new float[size*size];
    final float[] b = new float[size*size];
    for (int i = 0; i < a.length; i++) {
      a[i] = 0.001f + random.nextFloat() * 3;
    }
    for (int i = 0; i < b.length; i++) {
      b[i] = 0.001f + random.nextFloat() * 3;
    }

    int tileHeight = size/tiles;
    for(int tile=0; tile<tiles; tile++){

      final float[] result = new float[tileHeight*size];

      float[] aSplit = Arrays.copyOfRange(a, tile*tileHeight*size, (tile+1)*tileHeight*size);

      Range range = Range.create2D(tileHeight, size, 100, 1);

      MatrixKernel kernel = new MatrixKernel(this, range, aSplit, b, result, size);
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
