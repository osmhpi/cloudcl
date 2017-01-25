package fr.dynamo.test;
import static org.junit.Assert.assertEquals;

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

  @Test
  public void testGetSize() {
    OpenCLDevice device = new OpenCLDevice(new OpenCLPlatform(), 1, TYPE.CPU);
    Range range = new Range(device, 1);
    DynamoKernel kernel = new DynamoKernel(new DynamoJob("Test"), range) {

      private int[] x = new int[1000000];
      private int[][][] z = new int[100][1000][10];

      @Override
      public void run() {
      }
    };

    assertEquals(8000000, kernel.getDataSize());
    assertEquals(6400, NetworkEstimator.calculateTranferTime(kernel, NetworkSpeed.MBIT10));
    assertEquals(6, NetworkEstimator.calculateTranferTime(kernel, NetworkSpeed.GBIT10));

  }
}