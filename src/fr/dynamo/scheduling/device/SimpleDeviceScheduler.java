package fr.dynamo.scheduling.device;

import java.util.ArrayList;
import java.util.List;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.DevicePreference;
import fr.dynamo.threading.DynamoKernel;

public class SimpleDeviceScheduler extends AbstractDeviceScheduler{

  @Override
  public List<KernelDevicePairing> scheduleDevices(List<DynamoKernel> kernels, List<OpenCLDevice> unusedDevices) {
    List<KernelDevicePairing> pairings = new ArrayList<KernelDevicePairing>();
        
    for(DynamoKernel kernel:kernels){
      DevicePreference preference = kernel.getDevicePreference();
      OpenCLDevice chosenDevice = null;
      
      if(preference == DevicePreference.CPU_ONLY){
        chosenDevice = getFirstDeviceOfType(unusedDevices, TYPE.CPU);
      }else if(preference == DevicePreference.GPU_ONLY){
        chosenDevice = getFirstDeviceOfType(unusedDevices, TYPE.GPU);
      }else if(preference == DevicePreference.CPU_PREFERRED){
        chosenDevice = getFirstDeviceOfType(unusedDevices, TYPE.CPU);
        if(chosenDevice == null) chosenDevice = getFirstDeviceOfType(unusedDevices, TYPE.GPU);
      }else if(preference == DevicePreference.GPU_PREFERRED){
        chosenDevice = getFirstDeviceOfType(unusedDevices, TYPE.GPU);
        if(chosenDevice == null) chosenDevice = getFirstDeviceOfType(unusedDevices, TYPE.CPU);
      }else if(preference == DevicePreference.NONE){
        chosenDevice = getFirstDeviceOfType(unusedDevices, TYPE.CPU);
        if(chosenDevice == null) chosenDevice = getFirstDeviceOfType(unusedDevices, TYPE.GPU);
      }
      
      if(chosenDevice != null){
        unusedDevices.remove(chosenDevice);
        pairings.add(new KernelDevicePairing(kernel, chosenDevice));
      }
    }
    
    return pairings;
  }


}
