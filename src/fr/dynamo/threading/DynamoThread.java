package fr.dynamo.threading;
import java.util.LinkedHashSet;

import com.amd.aparapi.Kernel.EXECUTION_MODE;
import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.Device.TYPE;
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
    try{
      LinkedHashSet<Device> preferences = new LinkedHashSet<Device>();
      preferences.add(device);
      KernelManager.instance().setPreferredDevices(kernel, preferences);

      if(device.getType() == TYPE.GPU){
        kernel.setExecutionModeWithoutFallback(EXECUTION_MODE.GPU);
      }else if(device.getType() == TYPE.CPU){
        kernel.setExecutionModeWithoutFallback(EXECUTION_MODE.CPU);
      }

      System.out.println("Execute Thread " + this.getId() + " on " + device.getShortDescription() + " " + device.getDeviceId());
      try{
        kernel.execute();
      }catch(Error e){
        System.out.println("Thread " + this.getId() + " has failed.");
        kernel.reduceRemainingTries();
        throw e;
      }

      System.out.println("Execution of Thread " + this.getId() + " finished after " + kernel.getExecutionTime() + " ms");
      kernel.setRemainingTries(0);
      dispose();
    }finally{
      notifyable.notifyListener(this);
    }
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
