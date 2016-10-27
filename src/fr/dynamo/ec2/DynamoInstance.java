package fr.dynamo.ec2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;

public class DynamoInstance {

  private String instanceId;
  private String publicIp;

  public DynamoInstance(String instanceId) {
    super();
    this.instanceId = instanceId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getPublicIp() {
    return publicIp;
  }

  public void setPublicIp(String publicIp) {
    this.publicIp = publicIp;
  }

  @Override
  public boolean equals(Object obj) {
    return ((DynamoInstance)obj).instanceId == instanceId;
  }

  @Override
  public String toString(){
    return instanceId + ": " + publicIp;
  }

  public boolean blockUntilRunning(AmazonEC2 client, long timeout){
    long start = System.currentTimeMillis();
    final InstanceState pendingState = new InstanceState().withName(InstanceStateName.Pending.name());

    boolean pending = true;
    while(pending){
      if(System.currentTimeMillis() - start > timeout) return false;

      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(getInstanceId());
      DescribeInstancesResult describeInstanceResult = client.describeInstances(request);

      if(describeInstanceResult.getReservations().get(0).getInstances().get(0).getState().getCode() != pendingState.getCode()){
        pending = false;
      }
    }

    return true;
  }

  public boolean blockUntilReachable(long timeout){
    long start = System.currentTimeMillis();

    while(true){
      if(System.currentTimeMillis() - start > timeout) return false;
      try {
        try (Socket soc = new Socket()) {
          soc.connect(new InetSocketAddress(getPublicIp(), 22), 500);
        }
        return true;
      } catch (IOException ex) {
      }
    }
  }

  public void collectInformation(AmazonEC2 client){
    List<String> instanceId = new ArrayList<String>();
    DescribeInstancesRequest request = new DescribeInstancesRequest();
    request.setInstanceIds(instanceId);

    DescribeInstancesResult result = client.describeInstances(request);
    List<Reservation> reservations = result.getReservations();

    publicIp = reservations.get(0).getInstances().get(0).getPublicIpAddress();
  }
}
