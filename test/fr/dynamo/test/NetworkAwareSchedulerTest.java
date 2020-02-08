package fr.dynamo.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

import fr.dynamo.scheduling.device.AbstractDeviceScheduler;
import fr.dynamo.scheduling.device.KernelDevicePairing;
import fr.dynamo.scheduling.device.NetworkAwareDeviceScheduler;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class NetworkAwareSchedulerTest {

  AbstractDeviceScheduler scheduler = new NetworkAwareDeviceScheduler();

  OpenCLPlatform platform = new OpenCLPlatform();
  List<OpenCLDevice> unusedDevices = new ArrayList<OpenCLDevice>();

  OpenCLDevice localCpu = new OpenCLDevice(platform, 0, TYPE.CPU);
  OpenCLDevice cloudCpu = new OpenCLDevice(platform, 1, TYPE.CPU);


  {
    localCpu.setName("local");
    localCpu.setMaxComputeUnits(1);
    //localCpu.setCloudDevice(false);

    cloudCpu.setName("cloud");
    cloudCpu.setMaxComputeUnits(2);
    //cloudCpu.setCloudDevice(true);
  }

  private DynamoKernel bigKernel = new DynamoKernel(new DynamoJob("bigJob"), Range.create(0)) {

    int[] bigData = new int[1000];
    @Override
    public void run() {}
  };

  private DynamoKernel smallKernel = new DynamoKernel(new DynamoJob("smallJob"), Range.create(0)) {
    int[] smallData = new int[10];

    @Override
    public void run() {}
  };

  private DynamoKernel hugeKernel = new DynamoKernel(new DynamoJob("hugeJob"), Range.create(0)) {

    int[] bigData = new int[10000];
    @Override
    public void run() {}
  };

  private DynamoKernel tinyKernel = new DynamoKernel(new DynamoJob("tinyJob"), Range.create(0)) {

    int[] tinyData = new int[1];
    @Override
    public void run() {}
  };

  List<DynamoKernel> kernels = new ArrayList<DynamoKernel>();

  @Before
  public void prepare(){
    kernels.clear();
    kernels.add(bigKernel);
    kernels.add(smallKernel);
    kernels.add(hugeKernel);
    kernels.add(tinyKernel);

    unusedDevices.clear();
    unusedDevices.add(localCpu);
    unusedDevices.add(cloudCpu);
  }

  @Test
  // TODOXXX: I am pretty sure that this test either is broken, or requires a pretty complex manual setup step
  //          (having a working dOpenCL environment running), or probably both
  //          It seems the easiest way to fix this would be to have a mock NodeList returning what the test wants
  //          and then making sure the rest of the test works correctly
  @Ignore
  public void testScheduling() {
    List<KernelDevicePairing> pairings = scheduler.scheduleDevices(kernels, unusedDevices);
    for(KernelDevicePairing p : pairings){
      System.out.println(p.device.getName() + " " + p.kernel.getJob().getName());
    }
    assertEquals("hugeJob", pairings.get(0).kernel.getJob().getName());
    //assertEquals(false, pairings.get(0).device.isCloudDevice());

    assertEquals("tinyJob", pairings.get(1).kernel.getJob().getName());
    //assertEquals(true, pairings.get(1).device.isCloudDevice());

  }

}
