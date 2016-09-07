import com.amd.aparapi.Range;

public class WrappedKernel{

  private TileKernel kernel;
  private Range range;
  
  public WrappedKernel(TileKernel kernel, Range range) {
    super();
    this.kernel = kernel;
    this.range = range;
  }
  
  public void execute(){
    System.out.println("Executing Tile " + kernel.getTile() + " on " + kernel.getTargetDevice().getShortDescription() + " " +  kernel.getTargetDevice().getDeviceId());
    kernel.execute(range);
  }

  public TileKernel getKernel() {
    return kernel;
  }

  public Range getRange() {
    return range;
  }

  
}
