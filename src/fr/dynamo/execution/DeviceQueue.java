package fr.dynamo.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.DevicePreference;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.performance.PerformanceMeasurement;
import fr.dynamo.threading.DynamoKernel;

public class DeviceQueue {

  private List<OpenCLDevice> gpus = new ArrayList<OpenCLDevice>();
  private List<OpenCLDevice> cpus = new ArrayList<OpenCLDevice>();


  public DeviceQueue(){
  }

  public void add(OpenCLDevice device){
    if(device.getType() == TYPE.CPU){
      cpus.add(device);
    }else if(device.getType() == TYPE.GPU){
      gpus.add(device);
    }
  }

  public int size(){
    return cpus.size() + gpus.size();
  }

  public OpenCLDevice findFittingDevice(DynamoKernel kernel, DevicePreference preference){
    PerformanceMeasurement measurement = PerformanceCache.getInstance().getPerformanceMeasurement(kernel.getJob());
    List<String> rankedDeviceNames = new ArrayList<String>();
    for(Entry<String, Long> entry : measurement.getDeviceRanking()){
      rankedDeviceNames.add(entry.getKey());
    }

    List<OpenCLDevice> eligibleDevices = buildEligibleDevices(preference);

    OpenCLDevice device = findDeviceWithoutMeasurements(rankedDeviceNames, eligibleDevices);

    if(device == null){
      device = findDeviceWithBestRanking(rankedDeviceNames, eligibleDevices);
    }

    if(device != null){
      if(device.getType() == TYPE.CPU){
        cpus.remove(device);
      }else{
        gpus.remove(device);
      }
    }

    return device;
  }

  private List<OpenCLDevice> buildEligibleDevices(DevicePreference preference){
    List<OpenCLDevice> eligibleDevices = new ArrayList<OpenCLDevice>();

    if(preference == DevicePreference.CPU_ONLY){
      eligibleDevices.addAll(cpus);
    }

    if(preference == DevicePreference.GPU_ONLY){
      eligibleDevices.addAll(gpus);
    }

    if(preference == DevicePreference.CPU_PREFERRED){
      eligibleDevices.addAll(cpus);
      eligibleDevices.addAll(gpus);
    }

    if(preference == DevicePreference.GPU_PREFERRED){
      eligibleDevices.addAll(gpus);
      eligibleDevices.addAll(cpus);
      }

    if(preference == DevicePreference.NONE){
      if(cpus.size() >= gpus.size()){
        eligibleDevices.addAll(cpus);
        eligibleDevices.addAll(gpus);
      }else{
        eligibleDevices.addAll(gpus);
        eligibleDevices.addAll(cpus);
      }
    }
    return eligibleDevices;
  }

  private OpenCLDevice findDeviceWithoutMeasurements(List<String> rankedNames, List<OpenCLDevice> devices){
    for(OpenCLDevice device:devices){
      if(!rankedNames.contains(device.getPerformanceIdentifier())){
        return device;
      }
    }
    return null;
  }

  private OpenCLDevice findDeviceWithBestRanking(List<String> rankedNames, List<OpenCLDevice> devices){
    for(String name:rankedNames){
      for(OpenCLDevice device:devices){
        if(name.equals(device.getPerformanceIdentifier())){
          return device;
        }
      }
    }
    return null;
  }
}
