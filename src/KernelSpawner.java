import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.Device.TYPE;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelManager;

public class KernelSpawner {

  
  public static List<WrappedKernel> create(TileKernel kernel, Range range, TYPE deviceType, int deviceCount){
    List<OpenCLDevice> devices = OpenCLDevice.listDevices(deviceType);
    List<WrappedKernel> kernels = new ArrayList<WrappedKernel>();
    deviceCount = Math.min(deviceCount, devices.size());
    int dividedRangeLength = range.getGlobalSize_0() / deviceCount;

    for(int count = 0; count < deviceCount; count++){
      Device device = devices.get(count);
      Range dividedRange = device.createRange2D(dividedRangeLength, range.getGlobalSize_1());
      
      TileKernel clonedKernel = (TileKernel) kernel.clone();
      clonedKernel.setTile(count);
      clonedKernel.setRangeLength(dividedRangeLength);

      WrappedKernel wrappedKernel = new WrappedKernel(clonedKernel, dividedRange);
      LinkedHashSet<Device> preferences = new LinkedHashSet<Device>();

      preferences.add(device);
      KernelManager.instance().setPreferredDevices(wrappedKernel.getKernel(), preferences);
      kernels.add(wrappedKernel);
    }
    
    return kernels;
  }
  
  public static List<WrappedKernel> create(TileKernel kernel, Range range){
    return create(kernel, range, null, Integer.MAX_VALUE);
  }
  
}
