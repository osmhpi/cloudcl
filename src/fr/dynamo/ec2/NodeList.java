package fr.dynamo.ec2;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.jni.OpenCLJNI;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

import fr.dynamo.logging.Logger;

public class NodeList extends OpenCLJNI{

  private Set<DynamoInstance> nodes = new HashSet<DynamoInstance>();

  private static NodeList instance;

  private NodeList() throws IOException{
    if(!System.getenv().containsKey("DCL_NODE_FILE")){
      throw new IOException("Environment Variable for DCL_NODE_FILE not defined.");
    }
    OpenCLPlatform.getUncachedOpenCLPlatforms();
 }

  public static NodeList getInstance(){
    if(instance == null)
      try {
        instance = new NodeList();
      } catch (IOException e) {
        e.printStackTrace();
      }

    return instance;
  }

  public void addNode(DynamoInstance node){
    if(node.getPublicIp() == null){
      Logger.instance().warn("Instance cant't be added because of missing public IP: " + node.toString());
    }

    synchronized(nodes){
      Logger.instance().debug("Adding: " + node.getPublicIp() + " of Platform " + OpenCLPlatform.getUncachedOpenCLPlatforms().get(0));
      nodes.add(node);
      List<OpenCLDevice> devices = addNode(OpenCLPlatform.getUncachedOpenCLPlatforms().get(0), node.getPublicIp());
      node.setDevices(new HashSet<OpenCLDevice>(devices));
    }
  }

  public void removeNode(DynamoInstance node){
    synchronized(nodes){
      nodes.remove(node);
      removeNode(OpenCLPlatform.getUncachedOpenCLPlatforms().get(0), node.getDevices().iterator().next().getDeviceId());
    }
  }

  public Set<DynamoInstance> getNodes(){
    synchronized(nodes){
      return nodes;
    }
  }

  public Set<OpenCLDevice> getAllDevices(){
    Set<OpenCLDevice> devices = new HashSet<OpenCLDevice>();
    synchronized(nodes){
      for (OpenCLPlatform platform : OpenCLPlatform.getUncachedOpenCLPlatforms()) {
        for (OpenCLDevice device : platform.getOpenCLDevices()) {
          devices.add(device);
        }
      }
    }
    return devices;
  }

}
