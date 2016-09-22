package fr.dynamo.test;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

import fr.dynamo.threading.TileKernel;


public class TileKernelTest {

  @Test
  public void testGetSize() {
    OpenCLDevice device = new OpenCLDevice(new OpenCLPlatform(), 1, TYPE.CPU);
    Range range = new Range(device, 1);
    TileKernel kernel = new TileKernel(range) {

      private int[] x = new int[]{1,2,3,4,5,6,7,8,9,10};

      @Override
      public void run() {
      }
    };

    assertEquals(40, kernel.getTransferSize());
  }


  @Test
  public void testGetSizeComplex() {
    OpenCLDevice device = new OpenCLDevice(new OpenCLPlatform(), 1, TYPE.CPU);
    Range range = new Range(device, 1);
    TileKernel kernel = new TileKernel(range) {

      private int[] ints = new int[]{1,2,3,4,5,6,7,8,9,10};
      private double[] doubles = new double[]{1,2,3,4,5,6,7,8,9,10};
      private boolean[] booleans = new boolean[]{false,true,true,false,false,true,true,true,true,false};

      @Override
      public void run() {
      }
    };

    assertEquals(160, kernel.getTransferSize());

    assertEquals(0.00000014901161, kernel.getTransferrableGigabytes(), 0.001);

  }


  @Test
  public void testGetSizeNestedObject() {
    OpenCLDevice device = new OpenCLDevice(new OpenCLPlatform(), 1, TYPE.CPU);
    Range range = new Range(device, 1);
    TileKernel kernel = new TileKernel(range) {

      private SimpleBean[] objects = new SimpleBean[]{new SimpleBean(1, 1),new SimpleBean(1, 1),new SimpleBean(1, 1),new SimpleBean(1, 1),new SimpleBean(1, 1)};

      @Override
      public void run() {
      }
    };

    assertEquals(40, kernel.getTransferSize());

  }
}