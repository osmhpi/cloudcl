package fr.dynamo.ec2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

import fr.dynamo.execution.DynamoExecutor;

public class EC2MachineManager implements MachineManager{

  private AmazonEC2 client;
  private String imageId;
  private String keyName;
  private String securityGroup;
  private String regionName;
  private static String propertiesPath;
  private static EC2MachineManager instance;

  public static EC2MachineManager getInstance(){
    if(instance == null)
      try {
        instance = new EC2MachineManager(propertiesPath);
      } catch (IOException e) {
        e.printStackTrace();
      }
    return instance;
  }

  public static void setup(String pathToPropertiesFile){
    propertiesPath = pathToPropertiesFile;
  }

  private EC2MachineManager(String propertiesPath) throws IOException{
    Properties prop = new Properties();
    try {
      prop.load(new FileInputStream(new File(propertiesPath)));
    } catch (IOException e) {
      e.printStackTrace();
    }
    imageId = prop.getProperty("image_id");
    keyName = prop.getProperty("key_name");
    securityGroup = prop.getProperty("security_group");

    regionName = prop.getProperty("aws_region");

    AWSCredentialsProvider credentials = new ProfileCredentialsProvider(new ProfilesConfigFile(propertiesPath), "default");
    this.client = AmazonEC2ClientBuilder.standard().withCredentials(credentials).withRegion(Regions.fromName(regionName)).build();

    discoverExistingInstances();
  }

  private void discoverExistingInstances(){
    System.out.println("Discover already running instances.");
    List<DynamoInstance> dynamoInstances = new ArrayList<DynamoInstance>();

    DescribeInstancesRequest request = new DescribeInstancesRequest();
    DescribeInstancesResult result = getClient().describeInstances(request);
    List<Reservation> reservations = result.getReservations();

    for (Reservation reservation : reservations) {
      List<Instance> instances = reservation.getInstances();
      for(Instance i:instances){
        if(i.getImageId().equals(imageId) && !i.getState().getName().toString().equals("terminated")){
          dynamoInstances.add(new DynamoInstance(i.getInstanceId()));
        }
      }
    }
    initializeInstances(dynamoInstances);
  }

  public AmazonEC2 getClient() {
    return client;
  }

  @Override
  public List<DynamoInstance> bookInstance(int count, String type){

    RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

    runInstancesRequest.withImageId(imageId)
    .withInstanceType(type)
    .withMinCount(count)
    .withMaxCount(count)
    .withKeyName(keyName)
    .withSecurityGroups(securityGroup);

    RunInstancesResult runInstancesResult = client.runInstances(runInstancesRequest);

    System.out.println(runInstancesResult.getReservation().getInstances().size() + " instances launched.");
    System.out.println("Waiting for instances to finish boot.");


    List<Instance> bookedInstances = runInstancesResult.getReservation().getInstances();
    List<DynamoInstance> dynamoInstances = new ArrayList<DynamoInstance>();

    for(Instance instance:bookedInstances){
      dynamoInstances.add(new DynamoInstance(instance.getInstanceId()));
    }

    initializeInstances(dynamoInstances);

    DynamoExecutor.instance().triggerAssignment();

    return dynamoInstances;
  }

  private void initializeInstances(List<DynamoInstance> dynamoInstances){
    System.out.println("Launching " + dynamoInstances.size() + " instances.");

    blockUntilRunning(dynamoInstances, 60000);

    collectInstanceInformation(dynamoInstances);

    blockUntilReachable(dynamoInstances, 120000);
    System.out.println(dynamoInstances.size() + " instances available now.");

  }

  private void collectInstanceInformation(List<DynamoInstance> instances){
    for(DynamoInstance instance:instances){
      instance.collectInformation(client);
    }
  }

  @Override
  public void terminateInstances(List<DynamoInstance> instances){
    System.out.println("Terminating instances.");
    List<String> instanceIds = new ArrayList<String>();
    for(DynamoInstance instance:instances){
      instanceIds.add(instance.getInstanceId());
      NodeList.getInstance().removeNode(instance);
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

  public String getImageId() {
    return imageId;
  }

  public String getKeyName() {
    return keyName;
  }

  public String getSecurityGroup() {
    return securityGroup;
  }

  public String getRegion() {
    return regionName;
  }

}
