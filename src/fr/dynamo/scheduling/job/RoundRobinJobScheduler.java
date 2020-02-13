package fr.dynamo.scheduling.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import fr.dynamo.scheduling.device.KernelDevicePairing;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class RoundRobinJobScheduler implements JobScheduler{

  private int roundRobinCounter;

  @Override
  public List<DynamoKernel> schedule(List<DynamoJob> jobs) {

    if(roundRobinCounter >= jobs.size()) roundRobinCounter = 0;

    List<DynamoKernel> kernels = new LinkedList<>();
    int maxKernelCount = 0;
    for(DynamoJob job:jobs){
      maxKernelCount = Math.max(maxKernelCount, job.remaining());
    }

    Object[] front;
    Object[] back;
    if(roundRobinCounter > 0){
      back = Arrays.copyOfRange(jobs.toArray(), 0, roundRobinCounter);
      front = Arrays.copyOfRange(jobs.toArray(), roundRobinCounter, jobs.size());
    }else{
      front = Arrays.copyOfRange(jobs.toArray(), 0, jobs.size());
      back = new Object[0];
    }

    List<DynamoJob> reorderedJobs = new ArrayList<>();

    for(Object o:front){
      reorderedJobs.add((DynamoJob) o);
    }

    for(Object o:back){
      reorderedJobs.add((DynamoJob) o);
    }

    for(int i=0; i<maxKernelCount; i++){
      for(DynamoJob job:reorderedJobs){
        if(i<job.getKernelsToRun().size()){
          kernels.add(job.getKernelsToRun().get(i));
        }
      }
    }

    roundRobinCounter = roundRobinCounter + 1;
    return kernels;
  }

  @Override
  public void registerSchedulingDecisions(List<KernelDevicePairing> scheduledKernels) {
  }

}
