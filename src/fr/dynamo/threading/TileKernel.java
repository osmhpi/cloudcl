package fr.dynamo.threading;
import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device;

public abstract class TileKernel extends Kernel{

  private Range range;

  public TileKernel(Range range) {
    super();
    this.range = range;
  }

  public void execute(){
    Device device = getTargetDevice();

    Range deviceSpecificRange = range;

    switch(range.getDims()){
      case 0:
        deviceSpecificRange = device.createRange(range.getGlobalSize_0(), range.getLocalSize_0());
        if(!deviceSpecificRange.isValid()){
          deviceSpecificRange = device.createRange(range.getGlobalSize_0());
        }
        break;
      case 1:
        deviceSpecificRange = device.createRange2D(range.getGlobalSize_0(), range.getGlobalSize_1(), range.getLocalSize_0(), range.getLocalSize_1());
        if(!deviceSpecificRange.isValid()){
          deviceSpecificRange = device.createRange2D(range.getGlobalSize_0(), range.getGlobalSize_1());
        }
        break;
      case 2:
        deviceSpecificRange = device.createRange3D(range.getGlobalSize_0(), range.getGlobalSize_1(), range.getGlobalSize_2(), range.getLocalSize_0(), range.getLocalSize_1(), range.getLocalSize_2());
        if(!deviceSpecificRange.isValid()){
          deviceSpecificRange = device.createRange3D(range.getGlobalSize_0(), range.getGlobalSize_1(), range.getGlobalSize_2());
        }
        break;
    }

    execute(deviceSpecificRange);
  }

}
