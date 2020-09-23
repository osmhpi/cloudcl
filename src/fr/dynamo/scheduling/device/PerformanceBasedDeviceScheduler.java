package fr.dynamo.scheduling.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.DevicePreference;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.performance.PerformanceMeasurement;
import fr.dynamo.threading.DynamoKernel;

public class PerformanceBasedDeviceScheduler extends AbstractDeviceScheduler {

  @Override
  public List<KernelDevicePairing> scheduleDevices(List<DynamoKernel> kernels, List<OpenCLDevice> unusedDevices) {
    List<KernelDevicePairing> pairings = new ArrayList<>();


    for(DynamoKernel kernel:kernels){
      PerformanceMeasurement measurement = PerformanceCache.getInstance().getPerformanceMeasurement(kernel.getJob());
      List<String> rankedDeviceNames = new ArrayList<>();
      for(Entry<String, Long> entry : measurement.getDeviceRanking()){
        rankedDeviceNames.add(entry.getKey());
      }

      OpenCLDevice device = findDeviceWithoutMeasurements(rankedDeviceNames, unusedDevices, kernel.getDevicePreference());

      if(device == null){
        device = findDeviceWithBestRanking(rankedDeviceNames, unusedDevices);
      }

      if(device != null){
        unusedDevices.remove(device);
        pairings.add(new KernelDevicePairing(kernel, device));
      }
    }

    return pairings;
  }


  private OpenCLDevice findDeviceWithoutMeasurements(List<String> rankedNames, List<OpenCLDevice> devices, DevicePreference preference){
    for(OpenCLDevice device:devices){
      if(preference.fitsPreference(device) && !rankedNames.contains(device.getPerformanceIdentifier())){
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
