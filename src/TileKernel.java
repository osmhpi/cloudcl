import com.amd.aparapi.Kernel;

public abstract class TileKernel extends Kernel{

  protected int tile_$constant$;
  protected int rangeLength_$constant$;

  public TileKernel() {
    super();
  }

  public void setRangeLength(int rangeLength) {
    this.rangeLength_$constant$ = rangeLength;
  }

  public void setTile(int tile) {
    this.tile_$constant$ = tile;
  }

  public int getTile() {
    return tile_$constant$;
  }
  
}
