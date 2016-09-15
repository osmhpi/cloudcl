import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelRunner;

public class KernelExecutor<T extends TileKernel> implements Notifyable{
  private ConcurrentLinkedQueue<T> kernelsToRun = new ConcurrentLinkedQueue<T>();
  private List<DynamoThread> threads = Collections.synchronizedList(new ArrayList<DynamoThread>());

  private DeviceManager deviceManager;

  private long startTime = -1;
  private long endTime = -1;

  public KernelExecutor(List<T> kernels, Set<TYPE> deviceTypesToUse) {
    super();
    KernelRunner.BINARY_CACHING_DISABLED = true;
    deviceManager = new DeviceManager(deviceTypesToUse);
    kernelsToRun.addAll(kernels);
  }

  public void execute(){
    startTime = System.currentTimeMillis();
    assignKernels();
    while(hasWork()){
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      processDiedThreads();
    }

    endTime = System.currentTimeMillis();
  }

  public long executionTime(){
    if(startTime == -1 || endTime == -1){
      return -1;
    }
    return endTime - startTime;
  }

  private synchronized void assignKernels(){
    List<DynamoThread> builtThreads = buildThreads();
    start(builtThreads);
  }

  private synchronized List<DynamoThread> buildThreads(){
    List<DynamoThread> newThreads = new ArrayList<DynamoThread>();
    Set<OpenCLDevice> unusedDevices = deviceManager.getUnusedDevices(threads);
    System.out.println(unusedDevices.size() + " devices available for disposition.");

    for(OpenCLDevice device:unusedDevices){
      TileKernel kernel = kernelsToRun.poll();
      if(kernel == null) return newThreads;
      DynamoThread thread = new DynamoThread(kernel, device, this);
      threads.add(thread);
      newThreads.add(thread);
    }
    return newThreads;
  }

  private synchronized void start(List<DynamoThread> threads){
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

  private void processDiedThreads(){
    for(DynamoThread t:threads){
      if(!t.isAlive()){
        notifyListener(t);
      }
    }
  }

  private boolean hasWork(){
    return !(kernelsToRun.isEmpty() && threads.isEmpty());
  }

  @Override
  public synchronized void notifyListener(Object notifier) {
    DynamoThread thread = (DynamoThread) notifier;
    threads.remove(thread);
    assignKernels();
  }
}
