import com.amd.aparapi.Range;

public class MyKernel extends TileKernel{

  final MatrixDataPoint[] data;
  final int size_$constant$;

  public MyKernel(Range range, MatrixDataPoint[] data, int size) {
    super(range);
    this.data = data;
    this.size_$constant$ = size;
  }

  @Override
  public void run() {
    int y = getGlobalId(0);
    int x = getGlobalId(1);

    float sum = 0;
    for(int i=0; i<size_$constant$; i++){
      sum += data[y * size_$constant$ + i].getA() * data[x * size_$constant$ + i].getB();
    }
    data[y*size_$constant$+x].setResult(sum);
  }
}
