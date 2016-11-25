package fr.dynamo.samples.mandelbrot;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.threading.DynamoJob;

public class MandelbrotMain {


  public static void main(String[] args) throws InterruptedException {
    DynamoJob job = new DynamoJob("Mandelbrot");

    int fullWidth = Integer.parseInt(args[0]);
    int fullHeight = Integer.parseInt(args[0]);
    int iterations = Integer.parseInt(args[1]);
    System.out.println("Size: " + fullWidth + "; Iterations: " + iterations);

    final int kernelCount = Integer.parseInt(args[2]);;
    int stripWidth = fullWidth / kernelCount;

    for(int i = 0; i<kernelCount; i++){
      Range range = Range.create2D(stripWidth, fullHeight, 100, 1);
      MandelbrotKernel k = new MandelbrotKernel(job, range, fullWidth, fullHeight, stripWidth, stripWidth * i, iterations);
      k.setDevicePreference(DevicePreference.CPU_ONLY);
      job.addKernel(k);
    }

    DynamoExecutor.instance().submit(job);
    job.awaitTermination(1, TimeUnit.DAYS);

    BufferedImage fullImage = new BufferedImage(fullWidth, fullHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = fullImage.createGraphics();
    for(int i = 0; i < job.getFinishedKernels().size(); i++){
      MandelbrotKernel kernel = (MandelbrotKernel) job.getFinishedKernels().get(i);
      BufferedImage image = paintPicture(kernel.result, stripWidth, fullHeight);
      try {
        ImageIO.write(image, "png", new File("TILE_"+i+".png"));
      } catch (IOException e) {
        e.printStackTrace();
      }
      graphics.drawImage(image, stripWidth*i, 0, null);
    }

    File outputfile = new File("result.png");
    System.out.println(outputfile.getAbsolutePath());
    try {
      ImageIO.write(fullImage, "png", outputfile);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  private static BufferedImage paintPicture(boolean[] results, int width, int height){
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    int c = new Color(0, 0, 0).getRGB();

    for(int y=0;y<height;y++){
      for(int x=0;x<width;x++){
        int index = y*width+x;
        if (results[index]) {
          img.setRGB(x, y, c);
        }
      }
    }

    return img;

  }
}
