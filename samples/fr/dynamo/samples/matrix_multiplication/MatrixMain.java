package fr.dynamo.samples.matrix_multiplication;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifier;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class MatrixMain {

  public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
    final int size = Integer.parseInt(args[0]);
    final int tiles = Integer.parseInt(args[1]);

    System.out.println("Width/Height: " + size);
    int tileHeight = size/tiles;
    System.out.println("Height per Tile: " + tileHeight);

    Random random = new Random();
    random.setSeed(1000);

    DynamoJob job = new DynamoJob("Matrix", new ThreadFinishedNotifier() {
      @Override
      public void notifyListener(DynamoKernel kernel) {
        System.out.println("Callback for Kernel: " + kernel.hashCode());
      }
    });

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

      MatrixKernel kernel = new MatrixKernel(job, range, aSplit, b, result, size);
      kernel.setDevicePreference(DevicePreference.CPU_ONLY);
      job.addKernel(kernel);
    }

    DynamoExecutor.instance().submit(job);

    job.awaitTermination(1, TimeUnit.DAYS);

    int zeroes = 0;
    for(DynamoKernel k:job.getFinishedKernels()){
      MatrixKernel matrixKernel = (MatrixKernel)k;

      for(float f : matrixKernel.result){
        if(f == 0){
          zeroes++;
        }
      }
    }

    if(zeroes > 0){
      throw new UnexpectedException("Checksum error when checking the resulting matrix.");
    }

    PerformanceCache.getInstance().printStatistics(job);
    System.out.println(job);

    job.cleanUp();
  }

}