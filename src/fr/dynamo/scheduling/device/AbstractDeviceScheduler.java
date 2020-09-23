package fr.dynamo.scheduling.device;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.threading.DynamoKernel;
import fr.dynamo.threading.DynamoThread;

public abstract class AbstractDeviceScheduler {
  
  private static TYPE[] usableTypes = new TYPE[]{TYPE.CPU, TYPE.GPU};

  public abstract List<KernelDevicePairing> scheduleDevices(List<DynamoKernel> kernels, List<OpenCLDevice> unusedDevices);

  public static List<OpenCLDevice> getUnusedDevices(List<DynamoThread> threads){
    List<OpenCLDevice> unusedDevices = new ArrayList<>();
    List<OpenCLDevice> devices = getDevices();
    Set<Long> usedDeviceIds = getUsedDeviceIds(threads);
    for(OpenCLDevice device:devices){
      if(!usedDeviceIds.contains(device.getDeviceId())){
        unusedDevices.add(device);
      }
    }
    return unusedDevices;
  }

  public static Set<Long> getUsedDeviceIds(List<DynamoThread> threads){
    Set<Long> usedDeviceIds = new HashSet<>();
    for(DynamoThread t:threads){
      usedDeviceIds.add(t.getDevice().getDeviceId());
    }
    return usedDeviceIds;
  }

  public static List<OpenCLDevice> getDevices(){
    return OpenCLDevice.listDevices(null);
  }
  
  protected OpenCLDevice getFirstDeviceOfType(List<OpenCLDevice> devices, TYPE type){
    for(OpenCLDevice device:devices){
      if(device.getType() == type) return device;
    }
    return null;
  }

}
