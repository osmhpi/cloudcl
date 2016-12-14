package fr.dynamo.ec2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

public class DynamoInstance {

  private static final int DOPENCL_PORT = 25025;

  private String instanceId;
  private String publicIp;
  private Set<OpenCLDevice> devices = new HashSet<OpenCLDevice>();

  public DynamoInstance(String instanceId) {
    super();
    this.instanceId = instanceId;
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
          soc.connect(new InetSocketAddress(getPublicIp(), DOPENCL_PORT), 500);
        }
        return true;
      } catch (IOException ex) {
      }
    }
  }

  public void collectInformation(AmazonEC2 client){
    List<String> instanceId = new ArrayList<String>();
    instanceId.add(getInstanceId());
    DescribeInstancesRequest request = new DescribeInstancesRequest();
    request.setInstanceIds(instanceId);

    DescribeInstancesResult result = client.describeInstances(request);
    List<Reservation> reservations = result.getReservations();

    publicIp = reservations.get(0).getInstances().get(0).getPublicIpAddress();
    System.out.println(publicIp);
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

  public Set<OpenCLDevice> getDevices() {
    return devices;
  }

  public void setDevices(Set<OpenCLDevice> devices) {
    Set<OpenCLDevice> devicesWithDetails = new HashSet<OpenCLDevice>();

    List<OpenCLDevice> allDevices = OpenCLPlatform.getUncachedOpenCLPlatforms().get(0).getOpenCLDevices();
    
    for(OpenCLDevice device:devices){
      for(OpenCLDevice completeDevice:allDevices){
        if(device.getDeviceId() == completeDevice.getDeviceId()){
          devicesWithDetails.add(completeDevice);
        }
      }
    }
    this.devices = devicesWithDetails;
  }

  @Override
  public boolean equals(Object obj) {
    return ((DynamoInstance)obj).instanceId == instanceId;
  }

  @Override
  public String toString(){
    return instanceId + ": " + publicIp;
  }

  @Override
  public int hashCode() {
    return instanceId.hashCode();
  }
}
