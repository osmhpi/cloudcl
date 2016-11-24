package fr.dynamo.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import fr.dynamo.threading.DynamoKernel;

public class KernelQueue {

  private Map<String, Queue<DynamoKernel>> queues = new HashMap<String, Queue<DynamoKernel>>();

  public void add(DynamoKernel kernel){
    String id = kernel.getJobId();

    synchronized (queues) {
      if(!queues.containsKey(id)){
        queues.put(id, new LinkedList<DynamoKernel>());
      }
      queues.get(id).add(kernel);
    }
  }

  public List<DynamoKernel> buildScheduledList(){
    List<DynamoKernel> kernels = new ArrayList<DynamoKernel>();
    synchronized (queues) {
      cleanQueues();
      for(Queue<DynamoKernel> queue:queues.values()){
        kernels.add(queue.poll());
      }

      return kernels;
    }
  }

  public void reject(DynamoKernel kernel){
    add(kernel);
  }

  private void cleanQueues(){
    for(String name : queues.keySet()){
      if(queues.get(name).isEmpty()){
        queues.remove(name);
      }
    }
  }

  public boolean isEmpty(){
    return size() == 0;
  }

  public void clear(){
    queues.clear();
  }

  public int size(){
    int sum = 0;
    for(Queue<DynamoKernel> queue:queues.values()){
      sum += queue.size();
    }
    return sum;
  }

}
