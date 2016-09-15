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

    Range deviceSpecificRange = device.createRange2D(range.getGlobalSize_0(), range.getGlobalSize_1());

    execute(deviceSpecificRange);
  }

}
