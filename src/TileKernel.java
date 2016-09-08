import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

public abstract class TileKernel extends Kernel{

  private Range range;

  public TileKernel(Range range) {
    super();
    this.range = range;
  }

  public void execute(){
    System.out.println(range);
    Range deviceSpecificRange = getTargetDevice().createRange2D(range.getGlobalSize_0(), range.getGlobalSize_1());
    System.out.println("Execute on " + getTargetDevice().getShortDescription() + " " + getTargetDevice().getDeviceId() + " " + deviceSpecificRange);

    execute(deviceSpecificRange);
  }
}
