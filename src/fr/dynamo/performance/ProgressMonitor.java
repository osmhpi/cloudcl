package fr.dynamo.performance;

import java.util.HashMap;
import java.util.Map;

public class ProgressMonitor {

  private Map<String, Progress> stats = new HashMap<String, Progress>();

  private static ProgressMonitor instance;

  private ProgressMonitor(){}

  public static ProgressMonitor instance(){
    if(instance == null) instance = new ProgressMonitor();
    return instance;
  }

  public void printStatus(){
    for(String key:stats.keySet()){
      Progress p = stats.get(key);
      System.out.println(p);
    }
  }

  public void incrementTotal(String jobId){
    createIfNonExistant(jobId);
    stats.get(jobId).incrementTotal();
  }

  public void incrementFinished(String jobId){
    createIfNonExistant(jobId);
    stats.get(jobId).incrementFinished();
  }

  public void incrementRunning(String jobId){
    createIfNonExistant(jobId);
    stats.get(jobId).incrementRunning();
  }

  public void decrementRunning(String jobId){
    createIfNonExistant(jobId);
    stats.get(jobId).decrementRunning();
  }

  private void createIfNonExistant(String jobId){
    if(!stats.containsKey(jobId)){
      stats.put(jobId, new Progress(jobId));
    }
  }
}
