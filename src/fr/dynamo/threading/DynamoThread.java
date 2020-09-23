package fr.dynamo.threading;
import java.util.LinkedHashSet;

import com.amd.aparapi.Kernel.EXECUTION_MODE;
import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelManager;

import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.logging.Logger;

public class DynamoThread extends Thread{

  private final DynamoKernel kernel;
  private final OpenCLDevice device;
  private final ThreadFinishedNotifyable notifyable;
  private long transferredDataSize;
  private boolean failed;

  public DynamoThread(DynamoKernel kernel, OpenCLDevice device, ThreadFinishedNotifyable notifyable) {
    super();
    this.kernel = kernel;
    this.device = device;
    this.notifyable = notifyable;
  }

  @Override
  public void run() {
    failed = false;
    try{
      getKernel().getJob().submitThread(this);
      LinkedHashSet<Device> preferences = new LinkedHashSet<>();
      preferences.add(device);
      KernelManager.instance().setPreferredDevices(kernel, preferences);

      if(device.getType() == TYPE.GPU){
        kernel.setExecutionModeWithoutFallback(EXECUTION_MODE.GPU);
      }else if(device.getType() == TYPE.CPU){
        kernel.setExecutionModeWithoutFallback(EXECUTION_MODE.CPU);
      }

      Logger.instance().info(getKernel().getJob().getName() + ": execute Thread " + this.getId() + " on " + device.getName() + " " + device.getDeviceId());
      try{
        kernel.execute();
      }catch(Error e){
        Logger.instance().error("Thread " + this.getId() + " has failed.");
        failed = true;
        kernel.reduceRemainingTries();
        throw e;
      }

      Logger.instance().info(getKernel().getJob().getName() + ": execution of Thread " + this.getId() + " on " + device.getName() + " " + " finished after " + kernel.getExecutionTime() + " ms");
      kernel.setRemainingTries(0);
    }finally{
      notifyable.notifyListener(this);
      dispose();
    }
  }

  public DynamoKernel getKernel() {
    return kernel;
  }

  public OpenCLDevice getDevice() {
    return device;
  }

  public long getTransferredDataSize() {
    return transferredDataSize;
  }

  public boolean isFailed() {
    return failed;
  }

  public void dispose(){
    transferredDataSize = kernel.getTransferredDataSize();
    kernel.dispose();
  }

}
