package fr.dynamo.ec2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.ec2.AmazonEC2;
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

  private static MachineManager instance;

  private static AmazonEC2 client;

  private MachineManager(){
  }

  public static MachineManager instance(){
    if(client == null){
      throw new IllegalStateException("MachineManager requires to have a valid Amazon EC2 client set before initialization.");
    }

    if(instance == null){
      instance = new MachineManager();
    }

    return instance;
  }

  public AmazonEC2 getClient() {
    return client;
  }

  public static void setClient(AmazonEC2 c) {
    client = c;
  }

  public List<String> bookInstance(){
    List<String> instanceIds = new ArrayList<String>();

    RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

    runInstancesRequest.withImageId("ami-26c43149")
                       .withInstanceType("t2.micro")
                       .withMinCount(1)
                       .withMaxCount(1)
                       .withKeyName("opencl-host")
                       .withSecurityGroups("opencl-host-WebServerSecurityGroup-192ER9ITPN3RE");

    Date before = new Date();
    RunInstancesResult runInstancesResult = client.runInstances(runInstancesRequest);

    System.out.println(runInstancesResult.getReservation().getInstances().size() + " instances launched.");
    System.out.println("Waiting for instances to finish boot.");

    InstanceState pendingState = new InstanceState().withName(InstanceStateName.Pending.name());

    List<Instance> bookedInstances = runInstancesResult.getReservation().getInstances();

    for(Instance instance:bookedInstances){
      boolean pending = true;
      while(pending){
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
    TerminateInstancesRequest request = new TerminateInstancesRequest(instanceIds);
    client.terminateInstances(request);
  }


  public boolean blockUntilReachable(Collection<String> instanceIps, long timeout) {
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
    return true;
  }

}
