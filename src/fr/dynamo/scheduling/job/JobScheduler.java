package fr.dynamo.scheduling.job;

import java.util.List;

import fr.dynamo.scheduling.device.KernelDevicePairing;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public interface JobScheduler {
  List<DynamoKernel> schedule(List<DynamoJob> jobs);
  void registerSchedulingDecisions(List<KernelDevicePairing> scheduledKernels);
}
