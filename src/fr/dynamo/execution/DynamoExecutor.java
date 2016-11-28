package fr.dynamo.execution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelRunner;

import fr.dynamo.Notifyable;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;
import fr.dynamo.threading.DynamoThread;

public class DynamoExecutor implements Notifyable{

  private DeviceManager deviceManager = new DeviceManager();
  private Set<DynamoJob> jobs = Collections.synchronizedSet(new HashSet<DynamoJob>());

  private static DynamoExecutor instance;

  private DynamoExecutor() {
    super();
    KernelRunner.BINARY_CACHING_DISABLED = true;
  }

  public static DynamoExecutor instance(){
    if(instance == null) instance = new DynamoExecutor();
    return instance;
  }

  public void submit(DynamoJob job){
    System.out.println("Enqueueing job " + job.getName() + " with " + job.total() + " kernels to run.");
    jobs.add(job);
    assignKernels();
  }

  private synchronized void assignKernels(){
    List<DynamoThread> builtThreads = buildThreads();
    start(builtThreads);
  }

  private synchronized List<DynamoThread> buildThreads(){
    List<DynamoThread> newThreads = new ArrayList<DynamoThread>();
    DeviceQueue unusedDevices = deviceManager.getUnusedDevices(allThreads());
    if(unusedDevices.size() == 0){
      System.out.println("No Devices available at this time. Waiting for another task to finish.");
      return newThreads;
    }

    List<DynamoKernel> scheduledKernels = buildScheduledList();
    System.out.println(scheduledKernels.size() + " kernels and " + unusedDevices.size() + " devices available for disposition.");

    for(DynamoKernel kernel:scheduledKernels){
      if(unusedDevices.size() == 0) break;

      OpenCLDevice device = unusedDevices.findFittingDevice(kernel, kernel.getDevicePreference());
      if(device == null){
        continue;
      }

      newThreads.add(kernel.buildThread(device, this));
    }

    return newThreads;
  }

  private synchronized List<DynamoKernel> buildScheduledList(){
    List<DynamoKernel> kernels = new LinkedList<DynamoKernel>();
    int maxKernelCount = 0;
    for(DynamoJob job:jobs){
      maxKernelCount = Math.max(maxKernelCount, job.remaining());
    }

    for(int i=0; i<maxKernelCount; i++){
      for(DynamoJob job:jobs){
        if(i<job.getKernelsToRun().size()){
          kernels.add(job.getKernelsToRun().get(i));
        }
      }
    }

    return kernels;
  }

  private synchronized List<DynamoThread> allThreads(){
    List<DynamoThread> threads = new ArrayList<DynamoThread>();
    for(DynamoJob job:jobs){
      threads.addAll(job.getThreads());
    }
    return threads;
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

  @Override
  public synchronized void notifyListener(Object notifier) {
    DynamoThread thread = (DynamoThread) notifier;
    DynamoKernel kernel = thread.getKernel();

    if(kernel.getRemainingTries() == 0){
      kernel.getJob().finish(thread);
    }else{
      kernel.getJob().requeue(thread);
    }

    assignKernels();
  }

  public Set<DynamoJob> getJobs() {
    return jobs;
  }

}