import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;

public class DeviceManager {

  Set<TYPE> usableTypes = new HashSet<TYPE>();

  public DeviceManager(Set<TYPE> usableTypes) {
    super();
    this.usableTypes = usableTypes;
  }

  public Set<OpenCLDevice> getDevices(){
    Set<OpenCLDevice> devices = new HashSet<OpenCLDevice>();
    for(TYPE type:usableTypes){
      devices.addAll(OpenCLDevice.listDevices(type));
    }
    return devices;
  }

  public Set<OpenCLDevice> getUnusedDevices(List<DynamoThread> threads){
    Set<OpenCLDevice> unusedDevices = new HashSet<OpenCLDevice>();
    Set<OpenCLDevice> devices = getDevices();
    Set<Long> usedDeviceIds = getUsedDeviceIds(threads);
    for(OpenCLDevice device:devices){
      if(!usedDeviceIds.contains(device.getDeviceId())){
        unusedDevices.add(device);
      }
    }
    return unusedDevices;
  }

  public Set<Long> getUsedDeviceIds(List<DynamoThread> threads){
    Set<Long> usedDeviceIds = new HashSet<Long>();
    for(DynamoThread t:threads){
      usedDeviceIds.add(t.device.getDeviceId());
    }
    return usedDeviceIds;
  }

}
