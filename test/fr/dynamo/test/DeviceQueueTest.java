package fr.dynamo.test;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

import fr.dynamo.DevicePreference;
import fr.dynamo.execution.DeviceQueue;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.threading.DynamoKernel;

public class DeviceQueueTest {

  private DeviceQueue deviceQueue = new DeviceQueue();

  OpenCLPlatform platform = new OpenCLPlatform();

  OpenCLDevice cpu1 = new OpenCLDevice(platform, 0, TYPE.CPU);
  OpenCLDevice cpu2 = new OpenCLDevice(platform, 1, TYPE.CPU);
  OpenCLDevice gpu1 = new OpenCLDevice(platform, 2, TYPE.GPU);
  OpenCLDevice gpu2 = new OpenCLDevice(platform, 3, TYPE.GPU);

  {
    cpu1.setName("CPU1");
    cpu1.setMaxComputeUnits(1);

    cpu2.setName("CPU2");
    cpu2.setMaxComputeUnits(2);

    gpu1.setName("GPU1");
    gpu1.setMaxComputeUnits(1);

    gpu2.setName("GPU2");
    gpu2.setMaxComputeUnits(2);
  }

  private DynamoKernel kernel = new DynamoKernel("Test", Range.create(0)) {
    @Override
    public void run() {
    }
  };

  @Before
  public void prepare(){
    deviceQueue.add(cpu1);
    deviceQueue.add(cpu2);
    deviceQueue.add(gpu1);
    deviceQueue.add(gpu2);
    PerformanceCache.getInstance().clear();
  }

  @Test
  public void testSizes() {
    assertEquals(4, deviceQueue.size());
    deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(3, deviceQueue.size());
    deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(2, deviceQueue.size());
    deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(1, deviceQueue.size());
    deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(0, deviceQueue.size());
    deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(0, deviceQueue.size());
  }

  @Test
  public void testAddition() {
    deviceQueue = new DeviceQueue();
    assertEquals(0, deviceQueue.size());
    deviceQueue.add(new OpenCLDevice(new OpenCLPlatform(), 0, TYPE.CPU));
    assertEquals(1, deviceQueue.size());
  }


  @Test
  public void testExclusivePreferences() {
    OpenCLDevice device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_ONLY);
    assertEquals(TYPE.CPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.GPU_ONLY);
    assertEquals(TYPE.GPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_ONLY);
    assertEquals(TYPE.CPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_ONLY);
    assertEquals(null, device);

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.GPU_ONLY);
    assertEquals(TYPE.GPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.GPU_ONLY);
    assertEquals(null, device);

  }

  @Test
  public void testCpuPreferences() {
    OpenCLDevice device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_PREFERRED);
    assertEquals(TYPE.CPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_PREFERRED);
    assertEquals(TYPE.CPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_PREFERRED);
    assertEquals(TYPE.GPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_PREFERRED);
    assertEquals(TYPE.GPU, device.getType());
  }

  @Test
  public void testGpuPreferences() {
    OpenCLDevice device = deviceQueue.findFittingDevice(kernel, DevicePreference.GPU_PREFERRED);
    assertEquals(TYPE.GPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.GPU_PREFERRED);
    assertEquals(TYPE.GPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.GPU_PREFERRED);
    assertEquals(TYPE.CPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.GPU_PREFERRED);
    assertEquals(TYPE.CPU, device.getType());
  }

  @Test
  public void testVaguePreferences() {
    OpenCLDevice device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_ONLY);

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(TYPE.GPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(TYPE.CPU, device.getType());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(TYPE.GPU, device.getType());
  }


  @Test
  public void testPreferDeviceWithoutMeasurement() {
    PerformanceCache.getInstance().addPerformanceMeasurement(kernel, cpu1, 10);
    OpenCLDevice device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_PREFERRED);
    assertEquals(1, device.getDeviceId());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.CPU_PREFERRED);
    assertEquals(2, device.getDeviceId());
  }

  @Test
  public void testPreferFasterDevice() {
    PerformanceCache.getInstance().addPerformanceMeasurement(kernel, cpu1, 10);
    PerformanceCache.getInstance().addPerformanceMeasurement(kernel, cpu2, 20);
    PerformanceCache.getInstance().addPerformanceMeasurement(kernel, gpu1, 5);
    PerformanceCache.getInstance().addPerformanceMeasurement(kernel, gpu2, 8);

    OpenCLDevice device = deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(2, device.getDeviceId());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(3, device.getDeviceId());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(0, device.getDeviceId());

    device = deviceQueue.findFittingDevice(kernel, DevicePreference.NONE);
    assertEquals(1, device.getDeviceId());
  }
}
