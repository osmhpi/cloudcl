package fr.dynamo.samples.matrix_multiplication;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.amd.aparapi.Range;

import fr.dynamo.execution.KernelExecutor;

public class Main {

  public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
    final int size = Integer.parseInt(args[0]);
    final int tiles = Integer.parseInt(args[1]);

    System.out.println("Width/Height: " + size);
    int tileWidth = size/ tiles;
    System.out.println("Width per Tile: " + tileWidth);

    Random random = new Random();
    random.setSeed(1000);

    KernelExecutor executor = new KernelExecutor();

    List<MatrixKernel> kernels = new ArrayList<MatrixKernel>();
    for(int tile=0; tile<tiles; tile++){
      final float[] a = new float[tileWidth*size];
      final float[] b = new float[tileWidth*size];
      final float[] result = new float[tileWidth*size];

      float value = random.nextFloat() * 3;

      for (int i = 0; i < a.length; i++) {
        a[i] = value;
        b[i] = value;
      }

      Range range = Range.create2D(size, tileWidth, 200, 1);

      MatrixKernel kernel = new MatrixKernel(range, a, b, result, tileWidth);
      kernels.add(kernel);
    }

    long before = System.currentTimeMillis();
    for(MatrixKernel k:kernels){
      executor.execute(k);
    }

    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.DAYS);
    long after = System.currentTimeMillis();

    int zeroes = 0;
    for(MatrixKernel k: kernels){
      for(float f : k.result){
        if(f == 0){
          zeroes++;
        }
      }
    }

    if(zeroes > 0){
      throw new UnexpectedException("Checksum error when checking the resulting matrix.");
    }

    System.out.println("Execution took " + (after-before) + " ms.");

  }

}