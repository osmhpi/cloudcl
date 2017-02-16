package fr.dynamo.scheduling.device;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.ec2.NodeList;
import fr.dynamo.threading.DynamoKernel;

public class NetworkAwareDeviceScheduler extends AbstractDeviceScheduler{

  @Override
  public List<KernelDevicePairing> scheduleDevices(List<DynamoKernel> kernels, List<OpenCLDevice> unusedDevices) {
    List<KernelDevicePairing> pairings = new ArrayList<KernelDevicePairing>();

    kernels.sort(new Comparator<DynamoKernel>() {
      @Override
      public int compare(DynamoKernel o1, DynamoKernel o2) {
        return (int) (o1.getDataSize() - o2.getDataSize());
      }
    });


    List<OpenCLDevice> unusedLocalDevices = new ArrayList<OpenCLDevice>();
    List<OpenCLDevice> unusedCloudDevices = new ArrayList<OpenCLDevice>();

    Set<OpenCLDevice> cloudDevices = NodeList.getInstance().getCloudDevices();

    for(OpenCLDevice device:unusedDevices){
      if(cloudDevices.contains(device)){
        unusedCloudDevices.add(device);
      }else{
        unusedLocalDevices.add(device);
      }
    }

    for(OpenCLDevice localDevice:unusedLocalDevices){
      for(int i = kernels.size() - 1; i >= 0; i--){
        DynamoKernel k = kernels.get(i);

        if(k.getDevicePreference().fitsPreference(localDevice)){
          pairings.add(new KernelDevicePairing(k, localDevice));
          kernels.remove(i);
          break;
        }
      }
    }

    for(OpenCLDevice cloudDevice:unusedCloudDevices){
      for(int i = 0; i < kernels.size(); i++){
        DynamoKernel k = kernels.get(i);

        if(k.getDevicePreference().fitsPreference(cloudDevice)){
          pairings.add(new KernelDevicePairing(k, cloudDevice));
          kernels.remove(i);
          break;
        }
      }
    }

    return pairings;
  }
}
