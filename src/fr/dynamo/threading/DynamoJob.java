package fr.dynamo.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.performance.PerformanceCache;
import fr.dynamo.performance.PerformanceMeasurement;

public class DynamoJob {

  private final String jobName;
  private final String id;
  private final List<DynamoKernel> kernelsToRun = Collections.synchronizedList(new ArrayList<>());
  private final List<DynamoThread> runningThreads = Collections.synchronizedList(new ArrayList<>());
  private final List<DynamoKernel> finishedKernels = Collections.synchronizedList(new ArrayList<>());
  private boolean terminated = false;
  private long start = -1;
  private long end = -1;
  private int iteration = 1;
  private final Date submissionTime;
  private ThreadFinishedNotifyable finishedKernelNotifier;
  private int priority = 1;

  public DynamoJob(String jobName) {
    super();

    this.jobName = jobName;
    this.id = UUID.randomUUID().toString();
    submissionTime = new Date();
  }

  public DynamoJob(String jobName, ThreadFinishedNotifyable finishedKernelNotifier) {
    this(jobName);
    this.finishedKernelNotifier = finishedKernelNotifier;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return jobName;
  }

  public void addKernel(DynamoKernel kernel){
    kernelsToRun.add(kernel);
  }

  public void requeue(DynamoThread thread){
    runningThreads.remove(thread);
    kernelsToRun.add(thread.getKernel());
  }

  public synchronized void finish(DynamoThread thread){
    PerformanceCache.getInstance().addPerformanceMeasurement(thread.getKernel().getJob(), thread.getDevice(), (long)thread.getKernel().getExecutionTime());
    finishedKernels.add(thread.getKernel());

    runningThreads.remove(thread);

    end = System.currentTimeMillis();
    if(finishedKernelNotifier != null){
      finishedKernelNotifier.notifyListener(thread);
    }
    terminated = kernelsToRun.isEmpty() && runningThreads.isEmpty();
  }

  public List<DynamoKernel> getFinishedKernels() {
    return finishedKernels;
  }

  public List<DynamoKernel> getKernelsToRun() {
    return kernelsToRun;
  }

  public List<DynamoThread> getThreads() {
    return runningThreads;
  }

  public List<OpenCLDevice> getActiveDevices() {
    List<OpenCLDevice> devices = new ArrayList<>();
    for(DynamoThread thread:runningThreads){
      devices.add(thread.getDevice());
    }
    return devices;
  }

  public PerformanceMeasurement getDevicePerformance(){
    return PerformanceCache.getInstance().getPerformanceMeasurement(this);
  }

  public List<Entry<String, Long>> getPerformanceRanking(){
    return PerformanceCache.getInstance().getPerformanceMeasurement(this).getDeviceRanking();
  }

  public void submitThread(DynamoThread thread){
    if(start == -1) start = System.currentTimeMillis();
    runningThreads.add(thread);
  }

  public void reset(){
    if(isTerminated()){
      kernelsToRun.addAll(finishedKernels);
      finishedKernels.clear();

      iteration++;
      end = -1;

      terminated = kernelsToRun.isEmpty() && runningThreads.isEmpty();
    }
  }

  public int total(){
    return running() + remaining() + finished();
  }

  public int running(){
    return runningThreads.size();
  }

  public int remaining(){
    return kernelsToRun.size();
  }

  public int getIteration() {
    return iteration;
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

  public int finished(){
    return finishedKernels.size();
  }

  public boolean isTerminated() {
    return terminated;
  }

  public float getProgress(){
    if(total() == 0) return 0;

    return (float)finished() / total();
  }

  public Date getSubmissionTime() {
    return submissionTime;
  }

  public long getExecutionTime(){
    if(end == -1){
      return System.currentTimeMillis() - start;
    }
    return end - start;
  }

  public long estimatedRemainingRuntime(){
    if(finished() == 0) return -1;

    long timePerItem = getExecutionTime() / finished();

    return (remaining() + running() / 2) * timePerItem;
  }

  public void setFinishedKernelNotifier(ThreadFinishedNotifyable finishedKernelNotifier) {
    this.finishedKernelNotifier = finishedKernelNotifier;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    if(priority < 1){
      throw new IllegalArgumentException("Priority has to be a minimum of 1.");
    }
    this.priority = priority;
  }

  public void cleanUp(){
    for(DynamoKernel k:kernelsToRun){
      k.dispose();
    }

    for(DynamoKernel k:finishedKernels){
      k.dispose();
    }

    for(Thread t:runningThreads){
      if(t.isAlive()){
       t.interrupt();
      }
    }

    kernelsToRun.clear();
    runningThreads.clear();
    finishedKernels.clear();
  }

  @Override
  public String toString() {
    return "Job: " + getName() + " (" + getId() +") at " + Math.round(getProgress() * 100) + "% (" + finished() + "/" +
            total() + " " + running() + " running) after " + getExecutionTime() + "ms (" + estimatedRemainingRuntime() + "ms remaining estimated).";
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof DynamoJob){
      return getId().equals(((DynamoJob)obj).getId());
    }
    return false;
  }
}
