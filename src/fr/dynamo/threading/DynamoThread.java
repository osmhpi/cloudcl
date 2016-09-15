package fr.dynamo.threading;
import java.util.LinkedHashSet;

import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelManager;

import fr.dynamo.Notifyable;

public class DynamoThread extends Thread{

  private TileKernel kernel;
  private OpenCLDevice device;
  private Notifyable notifyable;

  public DynamoThread(TileKernel kernel, OpenCLDevice device, Notifyable notifyable) {
    super();
    this.kernel = kernel;
    this.device = device;
    this.notifyable = notifyable;
  }

  @Override
  public void run() {
    LinkedHashSet<Device> preferences = new LinkedHashSet<Device>();
    preferences.add(device);
    KernelManager.instance().setPreferredDevices(kernel, preferences);
    System.out.println("Execute Thread " + this.getId() + " on " + device.getShortDescription() + " " + device.getDeviceId());

    kernel.execute();
    dispose();
    notifyable.notifyListener(this);
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
