package fr.dynamo.performance;

import java.util.concurrent.atomic.AtomicInteger;

public class Progress {

  private String jobId;
  private AtomicInteger total = new AtomicInteger(0);
  private AtomicInteger finished = new AtomicInteger(0);;
  private AtomicInteger running = new AtomicInteger(0);;

  private long start;
  private long end = -1;

  public Progress(String jobId) {
    super();
    this.jobId = jobId;
    start = System.currentTimeMillis();
  }

  public int incrementTotal(){
    return total.incrementAndGet();
  }

  public int incrementFinished(){
    int finishedCount = finished.incrementAndGet();
    if(total.get() == finishedCount){
      end = System.currentTimeMillis();
    }
    return finishedCount;
  }

  public int incrementRunning(){
    return running.incrementAndGet();
  }

  public int decrementRunning(){
    return running.decrementAndGet();
  }

  public float getProgress(){
    if(total.get() == 0) return 0;

    return (float)finished.get() / total.get();
  }

  public String getJobId() {
    return jobId;
  }
  public int getTotal() {
    return total.get();
  }
  public int getFinished() {
    return finished.get();
  }
  public int getRunning() {
    return running.get();
  }

  public long getExecutionTime(){
    if(end == -1){
      return System.currentTimeMillis() - start;
    }
    return end - start;
  }

  public long estimatedRemainingRuntime(){
    if(getFinished() == 0) return -1;

    int remainingItems = getTotal() - getFinished() - getRunning();

    long timePerItem = getExecutionTime() / getFinished();

    return (remainingItems + getRunning() / 2) * timePerItem;
  }

  @Override
  public String toString() {
    return "Job: " + getJobId() + " at " + Math.round(getProgress() * 100) + "% (" + getFinished() + "/" +
            getTotal() + " " + getRunning() + " running) after " + getExecutionTime() + "ms (" + estimatedRemainingRuntime() + "ms remaining estimated).";
  }
}
