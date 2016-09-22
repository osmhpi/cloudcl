import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

import fr.dynamo.ec2.NetworkEstimator;
import fr.dynamo.ec2.NetworkSpeed;
import fr.dynamo.threading.TileKernel;


public class NetworkEstimatorTest {

  @Test
  public void testGetSize() {
    OpenCLDevice device = new OpenCLDevice(new OpenCLPlatform(), 1, TYPE.CPU);
    Range range = new Range(device, 1);
    TileKernel kernel = new TileKernel(range) {

      private int[] x = new int[1000000];

      @Override
      public void run() {
      }
    };

    assertEquals(4000000, kernel.getTransferSize());
    assertEquals(3200.0, NetworkEstimator.calculateTranferTime(kernel, NetworkSpeed.MBIT10), 0);
    assertEquals(3.0, NetworkEstimator.calculateTranferTime(kernel, NetworkSpeed.GBIT10), 0);

  }
}