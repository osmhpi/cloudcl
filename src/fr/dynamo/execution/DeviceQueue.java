package fr.dynamo.execution;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.DevicePreference;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.performance.PerformanceMeasurement;
import fr.dynamo.threading.TileKernel;

public class DeviceQueue {

  private Queue<OpenCLDevice> gpus = new LinkedList<OpenCLDevice>();
  private Queue<OpenCLDevice> cpus = new LinkedList<OpenCLDevice>();


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

  public OpenCLDevice findFittingDevice(TileKernel kernel, DevicePreference preference){

    //Choose the device based on previous performance.
    PerformanceMeasurement measurement = PerformanceCache.getInstance().getPerformanceMeasurement(kernel);
    if(measurement != null){
      List<OpenCLDevice> devices = new ArrayList<OpenCLDevice>();
      devices.addAll(cpus);
      devices.addAll(gpus);

      List<Entry<String, Long>> ranking = measurement.getDeviceRanking();
      for(Entry<String, Long> entry:ranking){
        for(OpenCLDevice device:devices){
          if(entry.getKey().equals(device.getPerformanceIdentifier())){
            return device;
          }
        }
      }
    }

    //If none of the previously used devices is available, choose based on kernel preferences.

    if(preference == DevicePreference.CPU_ONLY){
      return cpus.poll();
    }

    if(preference == DevicePreference.GPU_ONLY){
      return gpus.poll();
    }

    if(preference == DevicePreference.CPU_PREFERRED){
      return cpus.isEmpty() ? gpus.poll() : cpus.poll();
    }

    if(preference == DevicePreference.GPU_PREFERRED){
      return gpus.isEmpty() ? cpus.poll() : gpus.poll();
    }

    if(preference == DevicePreference.NONE){
      return cpus.size() >= gpus.size() ? cpus.poll() : gpus.poll();
    }

    return null;
  }
}
