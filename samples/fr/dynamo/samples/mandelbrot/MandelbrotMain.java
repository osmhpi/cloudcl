package fr.dynamo.samples.mandelbrot;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import fr.dynamo.DevicePreference;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.threading.DynamoJob;

public class MandelbrotMain {


  public static void main(String[] args) throws InterruptedException {
    int size = Integer.parseInt(args[0]);
    int iterations = Integer.parseInt(args[1]);
    final int tileCount = Integer.parseInt(args[2]);;
    boolean outputPicture = Integer.parseInt(args[3]) == 1;
    int stripWidth = size / tileCount;

    DynamoJob job = new MandelbrotJob(size, tileCount, iterations, DevicePreference.NONE);

    DynamoExecutor.instance().submit(job);
    job.awaitTermination(1, TimeUnit.DAYS);

    if(outputPicture){
      BufferedImage fullImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = fullImage.createGraphics();
      for(int i = 0; i < job.getFinishedKernels().size(); i++){
        MandelbrotKernel kernel = (MandelbrotKernel) job.getFinishedKernels().get(i);
        BufferedImage image = paintPicture(kernel.result, stripWidth, size);
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

    job.cleanUp();
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
