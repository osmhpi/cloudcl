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

    int tileHeight = size/tiles;

    Random random = new Random();
    random.setSeed(1000);

    final float[] a = new float[size*size];
    final float[] b = new float[size*size];
    for (int i = 0; i < a.length; i++) {
      a[i] = random.nextFloat() * 3;
    }
    for (int i = 0; i < b.length; i++) {
      b[i] = random.nextFloat() * 3;
    }

    for(int tile=0; tile<tiles; tile++){

      final float[] result = new float[tileHeight*size];

      float[] aSplit = Arrays.copyOfRange(a, tile*tileHeight*size, (tile+1)*tileHeight*size);

      Range range = Range.create2D(tileHeight, size, 100, 1);

      MatrixKernel kernel = new MatrixKernel(this, range, aSplit, b, result, size);
      kernel.setDevicePreference(preference);
      kernel.setExplicit(true);
      kernel.put(aSplit);
      kernel.put(b);
      addKernel(kernel);
    }
  }

}
