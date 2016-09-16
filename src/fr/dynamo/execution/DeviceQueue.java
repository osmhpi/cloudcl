package fr.dynamo.execution;

import java.util.LinkedList;
import java.util.Queue;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.DevicePreference;

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

  public OpenCLDevice findFittingDevice(DevicePreference preference){
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
