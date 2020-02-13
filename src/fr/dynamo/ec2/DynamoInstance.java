package fr.dynamo.ec2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

public class DynamoInstance {

  private static final int DOPENCL_PORT = 25025;

  private final String instanceId;
  private String publicIp;
  private Set<OpenCLDevice> devices = new HashSet<>();

  public DynamoInstance(String instanceId) {
    super();
    this.instanceId = instanceId;
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
    Set<OpenCLDevice> devicesWithDetails = new HashSet<>();

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
