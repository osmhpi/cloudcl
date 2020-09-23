package fr.dynamo.test;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

import fr.dynamo.performance.NetworkEstimator;
import fr.dynamo.performance.NetworkSpeed;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;


public class NetworkEstimatorTest {

  DynamoKernel kernel;
  final int[] x = new int[1000000];
  final int[][][] z = new int[100][1000][10];

  @Before
  public void prepare(){
    OpenCLDevice device = new OpenCLDevice(new OpenCLPlatform(), 1, TYPE.CPU);

    Range range = new Range(device, 1);



    kernel = new DynamoKernel(new DynamoJob("Test"), range) {
      int[] x_input = x;
      int[][][] z_input = z;
      @Override
      public void run() {
      }
    };
  }

  @Test
  public void testGetSize() {
    assertEquals(8000000, kernel.getDataSize());
    assertEquals(6400, NetworkEstimator.calculateTranferTime(kernel, NetworkSpeed.MBIT10));
    assertEquals(6, NetworkEstimator.calculateTranferTime(kernel, NetworkSpeed.GBIT10));
  }

  @Test
  public void testTransferredDataSize() {
    kernel.setExplicit(true);
    kernel.put(x);
    assertEquals(4000000, kernel.getTransferredDataSize());
    kernel.get(x);
    assertEquals(8000000, kernel.getTransferredDataSize());
  }
}