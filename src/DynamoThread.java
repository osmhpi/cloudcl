import java.util.LinkedHashSet;

import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelManager;

public class DynamoThread extends Thread{

  TileKernel kernel;
  OpenCLDevice device;

  public DynamoThread(TileKernel kernel, OpenCLDevice device) {
    super();
    this.kernel = kernel;
    this.device = device;
  }

  @Override
  public void run() {
    LinkedHashSet<Device> preferences = new LinkedHashSet<Device>();
    preferences.add(device);
    KernelManager.instance().setPreferredDevices(kernel, preferences);
    System.out.println("Execute Thread" + this.getId() + " on " + device.getShortDescription() + " " + device.getDeviceId());

    kernel.execute();
  }

  public TileKernel getKernel() {
    return kernel;
  }

  public OpenCLDevice getDevice() {
    return device;
  }

  public void dispose(){
    System.out.println("Thread " + this.getId() + " being disposed.");
    kernel.dispose();
  }


}
