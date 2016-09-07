import java.util.ArrayList;
import java.util.List;

public class KernelExecutor {

  public static void execute(List<WrappedKernel> kernels){
    List<Thread> threads = new ArrayList<Thread>();
    for(WrappedKernel k : kernels){
      final WrappedKernel finalKernel = k;
      Thread t = new Thread(new Runnable() {
        
        @Override
        public void run() {     
          finalKernel.execute();          
        }
      });
      threads.add(t);
      t.start();
    }
    
    for(Thread t:threads){
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
