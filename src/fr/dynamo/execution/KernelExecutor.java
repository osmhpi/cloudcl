package fr.dynamo.execution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelRunner;

import fr.dynamo.Notifyable;
import fr.dynamo.threading.DynamoThread;
import fr.dynamo.threading.TileKernel;

public class KernelExecutor<T extends TileKernel> implements Notifyable{
  private ConcurrentLinkedQueue<T> kernelsToRun = new ConcurrentLinkedQueue<T>();
  private List<DynamoThread> threads = Collections.synchronizedList(new ArrayList<DynamoThread>());

  private DeviceManager deviceManager;


  public KernelExecutor(List<T> kernels, Set<TYPE> deviceTypesToUse) {
    super();
    KernelRunner.BINARY_CACHING_DISABLED = true;
    deviceManager = new DeviceManager(deviceTypesToUse);
    kernelsToRun.addAll(kernels);
  }

  public void execute(){
    assignKernels();
    while(hasWork()){
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      processDiedThreads();
    }
  }

  private synchronized void assignKernels(){
    List<DynamoThread> builtThreads = buildThreads();
    threads.addAll(builtThreads);
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
      newThreads.add(thread);
    }
    return newThreads;
  }

  private synchronized void start(List<DynamoThread> threads){
    /* Threads have to be started in succession and await their conversion because
       Aparapi doesn't allow parallel conversions by Kernels of the same class. */
    for(DynamoThread t:threads){
      t.start();
      awaitConversion(t);
    }
  }

  private void awaitConversion(DynamoThread t){
    // The only way to know if a Kernel has been converted is by identifying whether the respective ConversionTime value has been set.
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
