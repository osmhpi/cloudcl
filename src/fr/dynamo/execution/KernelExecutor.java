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
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.performance.ProgressMonitor;
import fr.dynamo.threading.DynamoKernel;
import fr.dynamo.threading.DynamoThread;

public class KernelExecutor implements Executor, Notifyable{

  private final KernelQueue kernelsToRun = new KernelQueue();
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
    ProgressMonitor.instance().printStatus();
    List<DynamoThread> newThreads = new ArrayList<DynamoThread>();
    DeviceQueue unusedDevices = deviceManager.getUnusedDevices(threads);
    if(unusedDevices.size() == 0){
      System.out.println("No Devices available at this time. Waiting for another task to finish.");
      return newThreads;
    }

    List<DynamoKernel> scheduledKernels = kernelsToRun.buildScheduledList();
    System.out.println(scheduledKernels.size() + " kernels and " + unusedDevices.size() + " devices available for disposition.");

    for(DynamoKernel kernel:scheduledKernels){
      if(unusedDevices.size() == 0) kernelsToRun.reject(kernel);

      OpenCLDevice device = unusedDevices.findFittingDevice(kernel, kernel.getDevicePreference());
      if(device == null){
        kernelsToRun.reject(kernel);
        continue;
      }

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
      ProgressMonitor.instance().incrementRunning(t.getKernel().getJobId());
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

    if(thread.getKernel().getRemainingTries() == 0){
      PerformanceCache.getInstance().addPerformanceMeasurement(thread.getKernel(), thread.getDevice(), (long)thread.getKernel().getExecutionTime());
      ProgressMonitor.instance().incrementFinished(thread.getKernel().getJobId());
    }else{
      kernelsToRun.add(thread.getKernel());
    }

    ProgressMonitor.instance().decrementRunning(thread.getKernel().getJobId());
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
    List<Runnable> waitingTasks = new ArrayList<>(kernelsToRun.buildScheduledList());
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
    DynamoKernel tileKernel = (DynamoKernel)kernel;
    enqueue(tileKernel);
  }

  private void enqueue(DynamoKernel kernel){
    System.out.println("Enqueueing Kernel " + kernel.getClass().getName() + " " +kernel.hashCode() + ".");
    kernelsToRun.add(kernel);
    ProgressMonitor.instance().incrementTotal(kernel.getJobId());
    assignKernels();
  }

  private boolean isAcceptedInstance(Runnable r){
    return r instanceof DynamoKernel;
  }

  @Override
  protected void finalize() {
    shutdown();
  }

}
