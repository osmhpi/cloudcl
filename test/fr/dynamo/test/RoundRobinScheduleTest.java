package fr.dynamo.test;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.amd.aparapi.Range;

import fr.dynamo.scheduling.job.JobScheduler;
import fr.dynamo.scheduling.job.RoundRobinJobScheduler;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class RoundRobinScheduleTest {

  final JobScheduler scheduler = new RoundRobinJobScheduler();
  final List<DynamoJob> jobs = new ArrayList<>();

  @Before
  public void prepare(){
    jobs.clear();

    for(int i=0; i<4;i++){
      DynamoJob job = new DynamoJob("Test_" + i);

      job.addKernel(new DynamoKernel(job, Range.create(0)) {
        @Override
        public void run() {
        }
      });

      job.addKernel(new DynamoKernel(job, Range.create(0)) {
        @Override
        public void run() {
        }
      });

      jobs.add(job);
    }

  }

  @Test
  public void testTaskCount() {
    List<DynamoKernel> kernels = scheduler.schedule(jobs);

    assertEquals(8, kernels.size());
  }

  @Test
  public void testFullRound() {
    List<DynamoKernel> kernels = scheduler.schedule(jobs);

    for(int n=0; n<2; n++){
      for(int i=0;i<4;i++){
        assertEquals("Test_" + i, kernels.get(0).getJob().getName());
        kernels.remove(0);
      }
    }
  }

  @Test
  public void testAdjacentRounds() {

    for(int n=0; n<2; n++){
      for(int i = 0;i<4;i++){
        List<DynamoKernel> kernels = scheduler.schedule(jobs);
        assertEquals("Test_" + i, kernels.get(0).getJob().getName());
      }
    }
  }


}
