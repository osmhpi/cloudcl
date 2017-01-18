package fr.dynamo.ec2;

import java.util.List;

public interface MachineManager {
  public abstract List<DynamoInstance> bookInstance(int count, String type);
  public abstract void terminateInstances(List<DynamoInstance> instances);
}
