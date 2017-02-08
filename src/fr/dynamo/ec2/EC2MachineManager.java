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

import fr.dynamo.logging.Logger;

public class EC2MachineManager extends MachineManager{

  private static AmazonEC2 client;
  private static String imageId;
  private static String keyName;
  private static String securityGroup;
  private static String regionName;
  private static String propertiesPath;
  private static EC2MachineManager instance;

  public static EC2MachineManager getInstance(){
    if(instance == null) instance = new EC2MachineManager();
    return instance;
  }

  public static void setup(String pathToPropertiesFile){
    propertiesPath = pathToPropertiesFile;
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
    client = AmazonEC2ClientBuilder.standard().withCredentials(credentials).withRegion(Regions.fromName(regionName)).build();
  }

  @Override
  protected List<DynamoInstance> discoverExistingInstances(){
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

    collectInstanceInformation(dynamoInstances);
    return dynamoInstances;
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

    Logger.instance().info(runInstancesResult.getReservation().getInstances().size() + " instances launched.");

    List<Instance> bookedInstances = runInstancesResult.getReservation().getInstances();
    List<DynamoInstance> dynamoInstances = new ArrayList<DynamoInstance>();

    for(Instance instance:bookedInstances){
      dynamoInstances.add(new DynamoInstance(instance.getInstanceId()));
    }

    return dynamoInstances;
  }

  @Override
  protected void collectInstanceInformation(List<DynamoInstance> instances){
    for(DynamoInstance instance:instances){
      collectInformationForInstance(client, instance);
    }
  }

  public void collectInformationForInstance(AmazonEC2 client, DynamoInstance instance){
    List<String> instanceId = new ArrayList<String>();
    instanceId.add(instance.getInstanceId());
    DescribeInstancesRequest request = new DescribeInstancesRequest();
    request.setInstanceIds(instanceId);

    DescribeInstancesResult result = client.describeInstances(request);
    List<Reservation> reservations = result.getReservations();

    instance.setPublicIp(reservations.get(0).getInstances().get(0).getPublicIpAddress());
  }

  @Override
  public void terminateInstances(List<DynamoInstance> instances){
    List<String> instanceIds = new ArrayList<String>();
    for(DynamoInstance instance:instances){
      instanceIds.add(instance.getInstanceId());
    }

    TerminateInstancesRequest request = new TerminateInstancesRequest(instanceIds);
    client.terminateInstances(request);
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
