package fr.dynamo.threading;
import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

public abstract class TileKernel extends Kernel{

  private Range range;

  public TileKernel(Range range) {
    super();
    this.range = range;
  }

  public void execute(){
    Range deviceSpecificRange = getTargetDevice().createRange2D(range.getGlobalSize_0(), range.getGlobalSize_1());
    execute(deviceSpecificRange);
  }
}
