package fr.dynamo.samples.mandelbrot;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class MandelbrotKernel extends DynamoKernel{

  final int fullWidth;
  final int fullHeight;
  final int width;
  final int maxIterations;
  final int offsetX;
  final boolean[] result;

  public MandelbrotKernel(DynamoJob job, Range range, int fullWidth, int fullHeight, int width, int offsetX, int maxIterations){
    super(job, range);
    this.fullHeight = fullHeight;
    this.fullWidth = fullWidth;
    this.width = width;
    this.offsetX = offsetX;
    this.maxIterations = maxIterations;
    this.result = new boolean[width*fullHeight];
  }

  @Override
  public void run() {
    int col = offsetX + getGlobalId(0);
    int row = getGlobalId(1);

    double c_re = ((col - fullWidth/2.0)*4.0/fullWidth);
    double c_im = ((row - fullHeight/2.0)*4.0/fullHeight);
    double x = 0, y = 0;
    int iteration = 0;
    while (x*x+y*y <= 4 && iteration < maxIterations) {
        double x_new = x*x - y*y + c_re;
        y = 2*x*y + c_im;
        x = x_new;
        iteration++;
    }

    int index = row* width + (col - offsetX);
    if (iteration >= maxIterations){
      result[index] = true;
    }
  }

  @Override
  public String toString() {
    return range  + ";" +  fullHeight + ";" + fullWidth + ";" + width + ";" + offsetX  + ";" + maxIterations;
  }

}
