package fr.dynamo.test;

import static org.junit.Assert.assertEquals;

import java.util.*;

import fr.dynamo.ec2.DynamoInstance;
import fr.dynamo.ec2.NodeListBase;
import org.junit.Before;
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
  final AbstractDeviceScheduler scheduler;

  final OpenCLPlatform platform = new OpenCLPlatform();
  final List<OpenCLDevice> unusedDevices = new ArrayList<>();

  final OpenCLDevice localCpu = new OpenCLDevice(platform, 0, TYPE.CPU);
  final OpenCLDevice cloudCpu = new OpenCLDevice(platform, 1, TYPE.CPU);


  {
    localCpu.setName("local");
    localCpu.setMaxComputeUnits(1);

    cloudCpu.setName("cloud");
    cloudCpu.setMaxComputeUnits(2);

    scheduler = new NetworkAwareDeviceScheduler(new NodeListBase() {
      @Override
      public void addNode(DynamoInstance node) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void removeNode(DynamoInstance node) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Set<DynamoInstance> getNodes() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Set<OpenCLDevice> getCloudDevices() {
        return new HashSet<>(Collections.singletonList(cloudCpu));
      }

      @Override
      public Set<OpenCLDevice> getAllDevices() {
        throw new UnsupportedOperationException();
      }
    });
  }

  private final DynamoKernel bigKernel = new DynamoKernel(new DynamoJob("bigJob"), Range.create(0)) {

    int[] bigData = new int[1000];
    @Override
    public void run() {}
  };

  private final DynamoKernel smallKernel = new DynamoKernel(new DynamoJob("smallJob"), Range.create(0)) {
    int[] smallData = new int[10];

    @Override
    public void run() {}
  };

  private final DynamoKernel hugeKernel = new DynamoKernel(new DynamoJob("hugeJob"), Range.create(0)) {

    int[] bigData = new int[10000];
    @Override
    public void run() {}
  };

  private final DynamoKernel tinyKernel = new DynamoKernel(new DynamoJob("tinyJob"), Range.create(0)) {

    int[] tinyData = new int[1];
    @Override
    public void run() {}
  };

  final List<DynamoKernel> kernels = new ArrayList<>();

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
