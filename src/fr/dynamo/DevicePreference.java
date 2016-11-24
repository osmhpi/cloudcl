package fr.dynamo;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;

public enum DevicePreference {
  NONE, CPU_ONLY, GPU_ONLY, CPU_PREFERRED, GPU_PREFERRED;

  public boolean fitsPreference(OpenCLDevice device){
    if((this == CPU_ONLY || this == CPU_PREFERRED) && device.getType() == TYPE.CPU){
      return true;
    }

    if((this == GPU_ONLY || this == GPU_PREFERRED) && device.getType() == TYPE.GPU){
      return true;
    }

    return this == NONE;
  }
}
