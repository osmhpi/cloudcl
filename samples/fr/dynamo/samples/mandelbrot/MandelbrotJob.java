package fr.dynamo.samples.mandelbrot;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.threading.DynamoJob;

public class MandelbrotJob extends DynamoJob{

  public MandelbrotJob(int size, int tileCount, int iterations, DevicePreference preference) {
    super("Mandelbrot");

    int stripWidth = size / tileCount;

    for(int i = 0; i<tileCount; i++){
      Range range = Range.create2D(stripWidth, size, 100, 1);
      MandelbrotKernel k = new MandelbrotKernel(this, range, size, size, stripWidth, stripWidth * i, iterations);
      k.setDevicePreference(preference);
      addKernel(k);
    }
  }

}
