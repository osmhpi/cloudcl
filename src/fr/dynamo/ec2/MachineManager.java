package fr.dynamo.ec2;

import java.util.List;

import fr.dynamo.execution.DynamoExecutor;

public abstract class MachineManager {
  protected abstract List<DynamoInstance> bookInstance(int count, String type);
  protected abstract void terminateInstances(List<DynamoInstance> instances);
  protected abstract List<DynamoInstance> discoverExistingInstances();
  protected abstract void collectInstanceInformation(List<DynamoInstance> instances);

  public MachineManager(){
    System.out.println("Discover already running instances.");
    discoverExistingInstances();
  }

  public List<DynamoInstance> book(int count, String type){
    List<DynamoInstance> instances = bookInstance(count, type);
    initializeInstances(instances);
    DynamoExecutor.instance().triggerAssignment();
    return instances;
  }

  public void release(List<DynamoInstance> instances){
    System.out.println("Terminating instances.");
    terminateInstances(instances);
    for(DynamoInstance instance:instances){
      NodeList.getInstance().removeNode(instance);
    }
    System.out.println("Instances terminated.");
  }

  private void initializeInstances(List<DynamoInstance> dynamoInstances){
    System.out.println("Launching " + dynamoInstances.size() + " instances.");
    collectInstanceInformation(dynamoInstances);
    blockUntilReachable(dynamoInstances, 120000);
    System.out.println(dynamoInstances.size() + " instances available now.");
  }

  private boolean blockUntilReachable(List<DynamoInstance> instances, long timeout) {
    System.out.println("Waiting for Instances to be reachable via SSH.");
    for(DynamoInstance instance:instances){
      if(!instance.blockUntilReachable(timeout)) return false;
      NodeList.getInstance().addNode(instance);
      System.out.println(NodeList.getInstance().getAllDevices().size() + " devices in the cluster now.");
    }
    System.out.println("Instances are reachable via SSH.");
    return true;
  }


}
