import java.util.Random;

import com.amd.aparapi.Range;

public class Main {

  public static void main(String[] args) throws InterruptedException {
    
    final int size_$constant$ = Integer.parseInt(args[0]);
   
    System.out.println(size_$constant$);
    Random random = new Random();
    random.setSeed(1000);
    
    final MatrixDataPoint[] dataPoints = new MatrixDataPoint[size_$constant$*size_$constant$];

    
    float value = random.nextFloat() * 3;
    for (int i = 0; i < size_$constant$*size_$constant$; i++) {
      dataPoints[i] = new MatrixDataPoint(value, value, 0);
    }
    
    
    TileKernel kernel = new TileKernel() {
      @Override
      public void run() {
        int y = getGlobalId(0);
        int x = getGlobalId(1);

        float sum = 0;
        for(int i=0; i<size_$constant$; i++){
          sum += dataPoints[y * size_$constant$ + i].getA() * dataPoints[x * size_$constant$ + i].getB();
        }
        dataPoints[y*size_$constant$+x].setResult(sum);
      }
    };
    
    Range range = Range.create2D(size_$constant$, size_$constant$);

    kernel.execute(range);
    
    int zeroes = 0;
    for(MatrixDataPoint point : dataPoints){
      float f = point.getResult();
      System.out.println(f);
      if(f == 0) zeroes++;
    }
    
    System.out.println("Zeroes: " + zeroes);
  }
  
}