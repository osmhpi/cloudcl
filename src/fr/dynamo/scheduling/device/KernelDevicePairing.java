package fr.dynamo.scheduling.device;

import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.threading.DynamoKernel;

public class KernelDevicePairing {
  public final DynamoKernel kernel;
  public final OpenCLDevice device;
  
  public KernelDevicePairing(DynamoKernel kernel, OpenCLDevice device) {
    super();
    this.kernel = kernel;
    this.device = device;
  }
}
