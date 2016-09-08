import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.kernel.KernelManager;

public class KernelExecutor<T extends TileKernel> {
  List<T> kernels;
  List<Thread> threads = new ArrayList<Thread>();

  long startTime = -1;
  long endTime = -1;

  public KernelExecutor(List<T> kernels) {
    super();
    this.kernels = kernels;
  }

  public void execute(){
    assignDevice();
    start();
    await();
    dispose();
  }

  public long executionTime(){
    if(startTime == -1 || endTime == -1){
      return -1;
    }
    return endTime - startTime;
  }

  private void assignDevice(){
    List<OpenCLDevice> devices = OpenCLDevice.listDevices(null);
    for(int i=0; i<kernels.size();i++){
      TileKernel kernel = kernels.get(i);
      Device device = devices.get(i % devices.size());
      LinkedHashSet<Device> preferences = new LinkedHashSet<Device>();

      preferences.add(device);
      KernelManager.instance().setPreferredDevices(kernel, preferences);
    }
  }

  private void start(){
    startTime = System.currentTimeMillis();
    for(TileKernel k : kernels){
      final TileKernel finalKernel = k;
      Thread t = new Thread(new Runnable() {

        @Override
        public void run() {
          finalKernel.execute();
        }
      });
      threads.add(t);
      t.start();
    }
  }

  private void await(){
    for(Thread t:threads){
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    endTime = System.currentTimeMillis();
  }

  private void dispose(){
    for(TileKernel k : kernels){
      k.dispose();
    }
  }
}
