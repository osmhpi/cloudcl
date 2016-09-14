import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelRunner;

public class KernelExecutor<T extends TileKernel> {
  Stack<T> kernelsToRun;
  List<DynamoThread> threads = new ArrayList<DynamoThread>();

  Set<TYPE> usableTypes;

  long startTime = -1;
  long endTime = -1;

  public KernelExecutor(List<T> kernels, Set<TYPE> deviceTypesToUse) {
    super();
    KernelRunner.BINARY_CACHING_DISABLED = true;

    this.kernelsToRun = new Stack<T>();
    kernelsToRun.addAll(kernels);

    usableTypes = deviceTypesToUse;
  }

  public void execute(){
    startTime = System.currentTimeMillis();

    while(!(kernelsToRun.isEmpty() && threads.isEmpty())){
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      List<DynamoThread> finishedThreads = getFinishedThreads();
      if(!threads.isEmpty() && finishedThreads.isEmpty()) continue;

      removeThreads(finishedThreads);
      if(kernelsToRun.isEmpty()) continue;

      List<DynamoThread> builtThreads = buildThreads();
      start(builtThreads);
    }

    endTime = System.currentTimeMillis();
  }

  public long executionTime(){
    if(startTime == -1 || endTime == -1){
      return -1;
    }
    return endTime - startTime;
  }

  private List<DynamoThread> buildThreads(){
    List<DynamoThread> newThreads = new ArrayList<DynamoThread>();
    Set<OpenCLDevice> devices = unusedDevices();
    System.out.println(devices.size() + " devices available for disposition.");

    for(OpenCLDevice device:unusedDevices()){
      if(kernelsToRun.isEmpty()) return newThreads;

      TileKernel kernel = kernelsToRun.pop();
      DynamoThread thread = new DynamoThread(kernel, device);
      threads.add(thread);
      newThreads.add(thread);
    }
    return newThreads;
  }

  private void start(List<DynamoThread> threads){
    for(DynamoThread t:threads){
      t.start();
      awaitConversion(t);
    }
  }

  private void awaitConversion(DynamoThread t){
    while(true && t.isAlive()){
      boolean converted = !String.valueOf(t.getKernel().getConversionTime()).equals("NaN");
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if(converted) break;
    }
  }

  private List<DynamoThread> getFinishedThreads(){
    List<DynamoThread> finishedThreads = new ArrayList<DynamoThread>();
    for(DynamoThread t:threads){
      if(!t.isAlive()){
        finishedThreads.add(t);
      }
    }
    return finishedThreads;
  }

  private void removeThreads(List<DynamoThread> removableThreads){
    for(DynamoThread t:removableThreads){
      threads.remove(t);
    }
  }

  private Set<OpenCLDevice> getDevices(){
    Set<OpenCLDevice> devices = new HashSet<OpenCLDevice>();
    for(TYPE type:usableTypes){
      devices.addAll(OpenCLDevice.listDevices(type));
    }
    return devices;
  }

  private Set<OpenCLDevice> unusedDevices(){
    Set<OpenCLDevice> unusedDevices = new HashSet<OpenCLDevice>();
    Set<OpenCLDevice> devices = getDevices();
    Set<Long> usedDeviceIds = getUsedDeviceIds();
    for(OpenCLDevice device:devices){
      if(!usedDeviceIds.contains(device.getDeviceId())){
        unusedDevices.add(device);
      }
    }
    return unusedDevices;
  }

  private Set<Long> getUsedDeviceIds(){
    Set<Long> usedDeviceIds = new HashSet<Long>();
    for(DynamoThread t:threads){
      usedDeviceIds.add(t.device.getDeviceId());
    }
    return usedDeviceIds;
  }
}
