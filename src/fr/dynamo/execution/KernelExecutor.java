package fr.dynamo.execution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelRunner;

import fr.dynamo.Notifyable;
import fr.dynamo.threading.DynamoThread;
import fr.dynamo.threading.TileKernel;

public class KernelExecutor implements Executor, Notifyable{

  private final List<TileKernel> kernelsToRun = Collections.synchronizedList(new ArrayList<TileKernel>());
  private final List<DynamoThread> threads = Collections.synchronizedList(new ArrayList<DynamoThread>());
  private DeviceManager deviceManager = new DeviceManager();
  private boolean isShutdown = false;


  public KernelExecutor() {
    super();
    KernelRunner.BINARY_CACHING_DISABLED = true;
  }

  private synchronized void assignKernels(){
    List<DynamoThread> builtThreads = buildThreads();
    threads.addAll(builtThreads);
    start(builtThreads);
  }

  private synchronized List<DynamoThread> buildThreads(){
    List<DynamoThread> newThreads = new ArrayList<DynamoThread>();
    DeviceQueue unusedDevices = deviceManager.getUnusedDevices(threads);
    System.out.println(unusedDevices.size() + " devices available for disposition.");

    for(int i = 0; i< kernelsToRun.size(); i++){
      TileKernel kernel = kernelsToRun.get(i);
      OpenCLDevice device = unusedDevices.findFittingDevice(kernel.getDevicePreference());
      if(device == null) continue;

      DynamoThread thread = new DynamoThread(kernel, device, this);
      newThreads.add(thread);
      kernelsToRun.remove(i);
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
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if(converted) break;
    }
  }

  private boolean hasFinishedWork(){
    return kernelsToRun.isEmpty() && threads.isEmpty();
  }

  @Override
  public synchronized void notifyListener(Object notifier) {
    DynamoThread thread = (DynamoThread) notifier;
    threads.remove(thread);
    assignKernels();
  }


  public void shutdown() {
    isShutdown = true;
  }

  public List<Runnable> shutdownNow() {
    for(DynamoThread t:threads){
      t.interrupt();
    }
    threads.clear();
    List<Runnable> waitingTasks =  new ArrayList<>(kernelsToRun);
    kernelsToRun.clear();
    isShutdown = true;
    return waitingTasks;
  }

  public boolean isShutdown() {
    return isShutdown;
  }

  public boolean isTerminated() {
    return isShutdown() && hasFinishedWork();
  }

  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    final int interval = 10;
    long bound = unit.toMillis(timeout);
    while(!isTerminated()){
      bound -= interval;
      if(bound <= 0) return false;
      Thread.sleep(interval);
    }

    return true;
  }

  @Override
  public void execute(Runnable kernel){
    if(!isAcceptedInstance(kernel)) throw new RejectedExecutionException("KernelExecutor only accepts instances of type TileKernel.");
    TileKernel tileKernel = (TileKernel)kernel;
    enqueue(tileKernel);
  }

  private void enqueue(TileKernel kernel){
    System.out.println("Enqueueing Kernel " + kernel.getClass().getName() + " " +kernel.hashCode() + ".");
    kernelsToRun.add(kernel);
    assignKernels();
  }

  private boolean isAcceptedInstance(Runnable r){
    return r instanceof TileKernel;
  }

  @Override
  protected void finalize() {
    shutdown();
  }

}
