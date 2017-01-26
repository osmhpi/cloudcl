package fr.dynamo.scheduling.job;

import java.util.LinkedList;
import java.util.List;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class RoundRobinJobScheduler implements JobScheduler{

  private int roundRobinCounter;

  @Override
  public List<DynamoKernel> schedule(List<DynamoJob> jobs) {
    if(roundRobinCounter >= jobs.size()) roundRobinCounter = 0;

    List<DynamoKernel> kernels = new LinkedList<DynamoKernel>();
    int maxKernelCount = 0;
    for(DynamoJob job:jobs){
      maxKernelCount = Math.max(maxKernelCount, job.remaining());
    }

    for(int i=0; i<maxKernelCount; i++){
      if(roundRobinCounter >= jobs.size()) roundRobinCounter = 0;

      while(roundRobinCounter < jobs.size()){
        DynamoJob job = jobs.get(roundRobinCounter);
        if(i<job.getKernelsToRun().size()){
          kernels.add(job.getKernelsToRun().get(i));
        }
        roundRobinCounter++;
      }
    }

    return kernels;
  }

}
