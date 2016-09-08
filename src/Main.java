import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.amd.aparapi.Range;

public class Main {

  public static void main(String[] args) throws InterruptedException {

    final int size = Integer.parseInt(args[0]);
    final int tiles = Integer.parseInt(args[1]);

    System.out.println("Width/Height: " + size);
    int tileWidth = size/ tiles;
    System.out.println("Width per Tile: " + tileWidth);

    Random random = new Random();
    random.setSeed(1000);

    List<MyKernel> kernels = new ArrayList<MyKernel>();
    for(int tile=0; tile<tiles; tile++){
      final MatrixDataPoint[] dataPoints = new MatrixDataPoint[tileWidth*size];
      System.out.println("Datapoint for tile: " + dataPoints.length);
      float value = random.nextFloat() * 3;
      for (int i = 0; i < dataPoints.length; i++) {
        dataPoints[i] = new MatrixDataPoint(value, value, 0);
      }

      Range range = Range.create2D(size, tileWidth);

      MyKernel kernel = new MyKernel(range, dataPoints, tileWidth);
      kernels.add(kernel);
    }

    KernelExecutor<MyKernel> executor = new KernelExecutor<MyKernel>(kernels);
    executor.execute();

    int zeroes = 0;
    for(MyKernel k: kernels){
      for(MatrixDataPoint point : k.data){
        float f = point.getResult();
        if(f == 0){
          zeroes++;
        }
      }
    }

    System.out.println("Time: " + executor.executionTime() + " ms");
    System.out.println("Zeroes: " + zeroes);
  }

}