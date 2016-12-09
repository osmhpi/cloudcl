package fr.dynamo.test;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

import fr.dynamo.DevicePreference;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.scheduling.device.AbstractDeviceScheduler;
import fr.dynamo.scheduling.device.KernelDevicePairing;
import fr.dynamo.scheduling.device.SimpleDeviceScheduler;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class SimpleSchedulerTest {

  AbstractDeviceScheduler scheduler = new SimpleDeviceScheduler();
  OpenCLPlatform platform = new OpenCLPlatform();

  List<OpenCLDevice> unusedDevices = new ArrayList<OpenCLDevice>();
  
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

  private DynamoKernel kernel = new DynamoKernel(new DynamoJob("Test"), Range.create(0)) {
    @Override
    public void run() {
    }
  };

  @Before
  public void prepare(){
    unusedDevices.clear();
    unusedDevices.add(cpu1);
    unusedDevices.add(gpu1);
    unusedDevices.add(cpu2);
    unusedDevices.add(gpu2);

    PerformanceCache.getInstance().clear();
  }

  @Test
  public void testExclusivePreferences() {
    List<KernelDevicePairing> pairings = null;
    List<DynamoKernel> kernels = new ArrayList<DynamoKernel>();
    kernels.add(kernel);
    
    kernel.setDevicePreference(DevicePreference.CPU_ONLY);
     pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.CPU, pairings.get(0).device.getType());

    kernel.setDevicePreference(DevicePreference.CPU_ONLY);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.CPU, pairings.get(0).device.getType());

    kernel.setDevicePreference(DevicePreference.CPU_ONLY);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(0, pairings.size());

    kernel.setDevicePreference(DevicePreference.GPU_ONLY);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.GPU, pairings.get(0).device.getType());

    kernel.setDevicePreference(DevicePreference.GPU_ONLY);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.GPU, pairings.get(0).device.getType());
    
    kernel.setDevicePreference(DevicePreference.GPU_ONLY);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(0, pairings.size());
  }

  @Test
  public void testCpuPreferences() {
    
    List<KernelDevicePairing> pairings = null;
    List<DynamoKernel> kernels = new ArrayList<DynamoKernel>();
    kernels.add(kernel);
    
    kernel.setDevicePreference(DevicePreference.CPU_PREFERRED);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.CPU, pairings.get(0).device.getType());
    
    kernel.setDevicePreference(DevicePreference.CPU_PREFERRED);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.CPU, pairings.get(0).device.getType());
     
    kernel.setDevicePreference(DevicePreference.CPU_PREFERRED);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.GPU, pairings.get(0).device.getType());
  
    kernel.setDevicePreference(DevicePreference.CPU_PREFERRED);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.GPU, pairings.get(0).device.getType());

  }

  @Test
  public void testGpuPreferences() {
    List<KernelDevicePairing> pairings = null;
    List<DynamoKernel> kernels = new ArrayList<DynamoKernel>();
    kernels.add(kernel);
    
    kernel.setDevicePreference(DevicePreference.GPU_PREFERRED);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.GPU, pairings.get(0).device.getType());
    
    kernel.setDevicePreference(DevicePreference.GPU_PREFERRED);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.GPU, pairings.get(0).device.getType());
     
    kernel.setDevicePreference(DevicePreference.GPU_PREFERRED);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.CPU, pairings.get(0).device.getType());
  
    kernel.setDevicePreference(DevicePreference.GPU_PREFERRED);
    pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    assertEquals(TYPE.CPU, pairings.get(0).device.getType());

  }


}
