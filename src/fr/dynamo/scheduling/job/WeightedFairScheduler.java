package fr.dynamo.scheduling.job;

import java.util.List;

import fr.dynamo.scheduling.device.KernelDevicePairing;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class WeightedFairScheduler implements JobScheduler{

  @Override
  public List<DynamoKernel> schedule(List<DynamoJob> jobs) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void registerSchedulingDecisions(List<KernelDevicePairing> scheduledKernels) {

  }

}
