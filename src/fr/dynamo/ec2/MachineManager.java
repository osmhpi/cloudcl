package fr.dynamo.ec2;

import java.util.List;

import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.logging.Logger;

public abstract class MachineManager {
  protected abstract List<DynamoInstance> bookInstance(int count, String type);
  protected abstract void terminateInstances(List<DynamoInstance> instances);
  protected abstract List<DynamoInstance> discoverExistingInstances();
  protected abstract void collectInstanceInformation(List<DynamoInstance> instances);

  public MachineManager(){
    Logger.instance().info("Discover already running instances.");
    for(DynamoInstance instance:discoverExistingInstances()){
      NodeList.getInstance().addNode(instance);
    }
  }

  public List<DynamoInstance> book(int count, String type){
    List<DynamoInstance> instances = bookInstance(count, type);
    initializeInstances(instances);
    DynamoExecutor.instance().triggerAssignment();
    return instances;
  }

  public void release(List<DynamoInstance> instances){
    Logger.instance().info("Terminating instances.");
    terminateInstances(instances);
    for(DynamoInstance instance:instances){
      NodeList.getInstance().removeNode(instance);
    }
    Logger.instance().info("Instances terminated.");
  }

  private void initializeInstances(List<DynamoInstance> dynamoInstances){
    Logger.instance().info("Launching " + dynamoInstances.size() + " instances.");
    collectInstanceInformation(dynamoInstances);
    blockUntilReachable(dynamoInstances, 120000);
    Logger.instance().info(dynamoInstances.size() + " instances available now.");
  }

  private boolean blockUntilReachable(List<DynamoInstance> instances, long timeout) {
    Logger.instance().info("Waiting for Instances to be reachable via SSH.");
    for(DynamoInstance instance:instances){
      if(!instance.blockUntilReachable(timeout)) return false;
      NodeList.getInstance().addNode(instance);
      Logger.instance().info(NodeList.getInstance().getAllDevices().size() + " devices in the cluster now.");
    }
    Logger.instance().info("Instances are reachable via SSH.");
    return true;
  }


}
