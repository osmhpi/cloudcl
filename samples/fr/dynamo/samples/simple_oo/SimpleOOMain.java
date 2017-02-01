package fr.dynamo.samples.simple_oo;

import java.util.concurrent.TimeUnit;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class SimpleOOMain {

  public static void main(String[] args) throws InterruptedException {

    int size = 1000;
    final Bean[] beans = new Bean[size];

    for(int i=0; i < size; i++){
      beans[i] = new Bean(i, i);
    }

    DynamoJob job = new DynamoJob("Simple OO");

    DynamoKernel kernel = new DynamoKernel(job, Range.create(size), DevicePreference.GPU_ONLY) {

      @Override
      public void run() {
        int i = getGlobalId();
        beans[i].x = beans[i].x * 10;
        beans[i].y = beans[i].y * 10;
      }
    };

    job.addKernel(kernel);

    DynamoExecutor.instance().submit(job);

    job.awaitTermination(1, TimeUnit.DAYS);

    for(Bean b:beans){
      System.out.println(b.x + " " +b.y);
    }
  }

}
