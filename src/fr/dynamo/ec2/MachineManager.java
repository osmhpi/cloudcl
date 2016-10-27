package fr.dynamo.ec2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
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

  public MachineManager(String propertiesPath){
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

  public List<String> bookInstance(){
    List<String> instanceIds = new ArrayList<String>();

    RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

    runInstancesRequest.withImageId(imageId)
    .withInstanceType(cpuInstanceType)
    .withMinCount(1)
    .withMaxCount(1)
    .withKeyName(keyName)
    .withSecurityGroups(securityGroup);

    Date before = new Date();
    RunInstancesResult runInstancesResult = client.runInstances(runInstancesRequest);

    System.out.println(runInstancesResult.getReservation().getInstances().size() + " instances launched.");
    System.out.println("Waiting for instances to finish boot.");


    List<Instance> bookedInstances = runInstancesResult.getReservation().getInstances();

    blockUntilRunning(bookedInstances, 60000);

    Date after = new Date();
    System.out.println(after.getTime() - before.getTime() + " ms launch time.");

    for(Instance instance:bookedInstances){
      instanceIds.add(instance.getInstanceId());
    }

    return instanceIds;
  }

  public Map<String, String> getPublicIps(Collection<String> instanceIds){
    Map<String, String> publicIps = new HashMap<String, String>();

    DescribeInstancesRequest request =  new DescribeInstancesRequest();
    request.setInstanceIds(instanceIds);

    DescribeInstancesResult result = client.describeInstances(request);
    List<Reservation> reservations = result.getReservations();

    List<Instance> instances;
    for(Reservation res : reservations){
      instances = res.getInstances();
      for(Instance ins : instances){
        publicIps.put(ins.getInstanceId(),ins.getPublicIpAddress());
      }
    }

    return publicIps;
  }


  public void terminateInstances(List<String> instanceIds){
    System.out.println("Terminating instances.");
    TerminateInstancesRequest request = new TerminateInstancesRequest(instanceIds);
    client.terminateInstances(request);
    System.out.println("Instances terminated.");
  }


  private boolean blockUntilRunning(List<Instance> bookedInstances, long timeout){
    long start = System.currentTimeMillis();
    final InstanceState pendingState = new InstanceState().withName(InstanceStateName.Pending.name());

    for(Instance instance:bookedInstances){
      boolean pending = true;
      while(pending){
        if(System.currentTimeMillis() - start > timeout) return false;

        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instance.getInstanceId());
        DescribeInstancesResult describeInstanceResult = client.describeInstances(request);

        if(describeInstanceResult.getReservations().get(0).getInstances().get(0).getState().getCode() != pendingState.getCode()){
          pending = false;
        }
      }
    }
    return true;
  }

  public boolean blockUntilReachable(Collection<String> instanceIps, long timeout) {
    System.out.println("Waiting for Instances to be reachable via SSH.");
    long start = System.currentTimeMillis();
    outer:for(String ip:instanceIps){
      while(true){
        if(System.currentTimeMillis() - start > timeout) return false;
        try {
          try (Socket soc = new Socket()) {
            soc.connect(new InetSocketAddress(ip, 22), 500);
          }
          continue outer;
        } catch (IOException ex) {
        }
      }
    }
    System.out.println("Instances are reachable via SSH.");
    return true;
  }

}
