package fr.dynamo.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import fr.dynamo.performance.PerformanceCache;

public class DynamoJob {

  private final String jobName;
  private final String id;
  private final List< DynamoKernel> kernelsToRun = Collections.synchronizedList(new ArrayList<DynamoKernel>());
  private final List<DynamoThread> runningThreads = Collections.synchronizedList(new ArrayList<DynamoThread>());
  private final List<DynamoKernel> finishedKernels = Collections.synchronizedList(new ArrayList<DynamoKernel>());
  private boolean terminated = false;
  private long start = System.currentTimeMillis();
  private long end = -1;

  public DynamoJob(String jobId) {
    super();

    this.jobName = jobId;
    this.id = UUID.randomUUID().toString();
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
    kernelsToRun.add(thread.getKernel());
  }

  public void finish(DynamoThread thread){
    PerformanceCache.getInstance().addPerformanceMeasurement(thread.getKernel().getJob(), thread.getDevice(), (long)thread.getKernel().getExecutionTime());
    finishedKernels.add(thread.getKernel());
    runningThreads.remove(thread);

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

  public void submitThread(DynamoThread thread){
    runningThreads.add(thread);
  }

  public void reset(){
    if(isTerminated()){
      kernelsToRun.addAll(finishedKernels);
      finishedKernels.clear();
    }

    terminated = kernelsToRun.isEmpty() && runningThreads.isEmpty();
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

  @Override
  public String toString() {
    return "Job: " + getName() + " (" + getId() +") at " + Math.round(getProgress() * 100) + "% (" + finished() + "/" +
            total() + " " + running() + " running) after " + getExecutionTime() + "ms (" + estimatedRemainingRuntime() + "ms remaining estimated).";
  }
}
