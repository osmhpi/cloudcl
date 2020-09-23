package fr.dynamo.execution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelRunner;

import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.logging.Logger;
import fr.dynamo.scheduling.device.AbstractDeviceScheduler;
import fr.dynamo.scheduling.device.KernelDevicePairing;
import fr.dynamo.scheduling.device.SimpleDeviceScheduler;
import fr.dynamo.scheduling.job.JobScheduler;
import fr.dynamo.scheduling.job.RoundRobinJobScheduler;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;
import fr.dynamo.threading.DynamoThread;

public class DynamoExecutor implements ThreadFinishedNotifyable{

  private final Set<DynamoJob> jobs = Collections.synchronizedSet(new HashSet<>());
  private JobScheduler jobScheduler = new RoundRobinJobScheduler();
  private AbstractDeviceScheduler deviceScheduler = new SimpleDeviceScheduler();
  private static DynamoExecutor instance;
  private boolean stopped = false;

  private DynamoExecutor() {
    super();
    KernelRunner.BINARY_CACHING_DISABLED = true;
  }

  public static DynamoExecutor instance(){
    if(instance == null) instance = new DynamoExecutor();
    return instance;
  }

  public void submit(DynamoJob job){
    Logger.instance().info("Enqueueing job " + job.getName() + " with " + job.total() + " kernels to run.");
    jobs.add(job);
    assignKernels();
  }

  public void triggerAssignment(){
    Logger.instance().debug("Assignment of kernels to devices has been triggered externally.");
    assignKernels();
  }

  private synchronized void assignKernels(){
    if(stopped) return;
    List<DynamoThread> builtThreads = buildThreads();
    start(builtThreads);
  }

  private synchronized List<DynamoThread> buildThreads(){
    Logger.instance().debug("Building Threads called.");
    List<DynamoThread> newThreads = new ArrayList<>();
    List<OpenCLDevice> unusedDevices = AbstractDeviceScheduler.getUnusedDevices(allThreads());
    if(unusedDevices.size() == 0){
      Logger.instance().info("No Devices available at this time. Waiting for another task to finish.");
      return newThreads;
    }

    List<DynamoKernel> scheduledKernels = jobScheduler.schedule(getUnfinishedJobs());
    Logger.instance().info(scheduledKernels.size() + " kernels and " + unusedDevices.size() + " devices available for disposition.");

    List<KernelDevicePairing> pairings = deviceScheduler.scheduleDevices(scheduledKernels, unusedDevices);
    Logger.instance().debug(pairings.size() + " pairings found.");

    for(KernelDevicePairing pairing:pairings){
      newThreads.add(pairing.kernel.buildThread(pairing.device, this));
    }

    jobScheduler.registerSchedulingDecisions(pairings);
    return newThreads;
  }

  private synchronized List<DynamoJob> getUnfinishedJobs(){
    List<DynamoJob> unfinishedJobs = new ArrayList<>();
    for(DynamoJob job:jobs){
      if(!job.isTerminated() && job.getKernelsToRun().size() > 0){
        unfinishedJobs.add(job);
      }
    }
    return unfinishedJobs;
  }

  private synchronized List<DynamoThread> allThreads(){
    List<DynamoThread> threads = new ArrayList<>();
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

  public Set<DynamoJob> getJobs() {
    return jobs;
  }

  public void setJobScheduler(JobScheduler scheduler) {
    this.jobScheduler = scheduler;
  }

  public void setDeviceScheduler(AbstractDeviceScheduler deviceScheduler) {
    this.deviceScheduler = deviceScheduler;
  }

  public boolean isStopped() {
    return stopped;
  }

  public void stop() {
    this.stopped = true;
  }

  public void start() {
    this.stopped = false;
    assignKernels();
  }

  @Override
  public void notifyListener(DynamoThread thread) {
    DynamoKernel kernel = thread.getKernel();

    if(kernel.getRemainingTries() > 0 && thread.isFailed()){
      kernel.getJob().requeue(thread);
    }else{
      kernel.getJob().finish(thread);
    }

    assignKernels();
  }

}
