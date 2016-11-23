package fr.dynamo.ec2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class MachineManager {

  private AmazonEC2 client;
  private String imageId;
  private String cpuInstanceType;
  private String gpuInstanceType;
  private String keyName;
  private String securityGroup;

  private NodeList nodeList;

  public MachineManager(String propertiesPath) throws IOException{
    nodeList = new NodeList();

    AWSCredentialsProvider credentials = new ProfileCredentialsProvider(new ProfilesConfigFile(propertiesPath), "default");
    this.client = AmazonEC2ClientBuilder.standard().withCredentials(credentials).withRegion(Regions.EU_CENTRAL_1).build();

    Properties prop = new Properties();
    try {
      prop.load(new FileInputStream(new File(propertiesPath)));
    } catch (IOException e) {
      e.printStackTrace();
    }

    imageId = prop.getProperty("image_id");
    cpuInstanceType = prop.getProperty("cpu_instance_type");
    gpuInstanceType = prop.getProperty("gpu_instance_type");
    keyName = prop.getProperty("key_name");
    securityGroup = prop.getProperty("security_group");
  }

  public AmazonEC2 getClient() {
    return client;
  }

  public List<DynamoInstance> bookInstance(int count){

    RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

    runInstancesRequest.withImageId(imageId)
    .withInstanceType(cpuInstanceType)
    .withMinCount(count)
    .withMaxCount(count)
    .withKeyName(keyName)
    .withSecurityGroups(securityGroup);

    Date before = new Date();
    RunInstancesResult runInstancesResult = client.runInstances(runInstancesRequest);

    System.out.println(runInstancesResult.getReservation().getInstances().size() + " instances launched.");
    System.out.println("Waiting for instances to finish boot.");


    List<Instance> bookedInstances = runInstancesResult.getReservation().getInstances();
    List<DynamoInstance> dynamoInstances = new ArrayList<DynamoInstance>();

    for(Instance instance:bookedInstances){
      dynamoInstances.add(new DynamoInstance(instance.getInstanceId()));
    }

    blockUntilRunning(dynamoInstances, 60000);

    collectInstanceInformation(dynamoInstances);

    Date after = new Date();
    System.out.println(after.getTime() - before.getTime() + " ms launch time.");

    return dynamoInstances;
  }

  public void collectInstanceInformation(List<DynamoInstance> instances){
    for(DynamoInstance instance:instances){
      instance.collectInformation(client);
    }
  }

  public void terminateInstances(List<DynamoInstance> instances){
    System.out.println("Terminating instances.");
    List<String> instanceIds = new ArrayList<String>();
    for(DynamoInstance instance:instances){
      instanceIds.add(instance.getInstanceId());
      nodeList.removeNode(instance);
    }

    TerminateInstancesRequest request = new TerminateInstancesRequest(instanceIds);
    client.terminateInstances(request);
    System.out.println("Instances terminated.");
  }


  private boolean blockUntilRunning(List<DynamoInstance> bookedInstances, long timeout){
    for(DynamoInstance instance:bookedInstances){
      if(!instance.blockUntilRunning(client, timeout)) return false;
    }
    return true;
  }

  public boolean blockUntilReachable(List<DynamoInstance> instances, long timeout) {
    System.out.println("Waiting for Instances to be reachable via SSH.");
    for(DynamoInstance instance:instances){
      if(!instance.blockUntilReachable(120000)) return false;
      nodeList.addNode(instance);
    }
    System.out.println("Instances are reachable via SSH.");
    return true;
  }

}
