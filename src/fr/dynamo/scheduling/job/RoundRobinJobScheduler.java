package fr.dynamo.scheduling.job;

import java.util.LinkedList;
import java.util.List;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class RoundRobinJobScheduler implements JobScheduler{

  private int roundRobinCounter;
  
  @Override
  public List<DynamoKernel> schedule(List<DynamoJob> jobs) {
    if(roundRobinCounter >= jobs.size()){
      roundRobinCounter = 0;
    }
    
    List<DynamoKernel> kernels = new LinkedList<DynamoKernel>();
    int maxKernelCount = 0;
    for(DynamoJob job:jobs){
      maxKernelCount = Math.max(maxKernelCount, job.remaining());
    }

    for(int i=0; i<maxKernelCount; i++){
      
      for(int j = roundRobinCounter; j < jobs.size(); j++){
        DynamoJob job = jobs.get(j);
        if(i<job.getKernelsToRun().size()){
          kernels.add(job.getKernelsToRun().get(i));
        }
      }
    }
    
    roundRobinCounter++;
    return kernels;
  }

}
