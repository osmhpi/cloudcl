package fr.dynamo.performance;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.threading.DynamoJob;

public class PerformanceCache {

  private static PerformanceCache instance;

  private Map<String, PerformanceMeasurement> kernelPerformances = new HashMap<String, PerformanceMeasurement>();

  private PerformanceCache(){}

  public static PerformanceCache getInstance(){
    if(instance == null) instance = new PerformanceCache();
    return instance;
  }

  public void addPerformanceMeasurement(DynamoJob job, OpenCLDevice device, long executionSpeed){
    String id = job.getId();

    if(!kernelPerformances.containsKey(id)){
      kernelPerformances.put(id, new PerformanceMeasurement());
    }

    kernelPerformances.get(id).addMeasurement(device, executionSpeed);

  }

  public PerformanceMeasurement getPerformanceMeasurement(DynamoJob job){
    String id = job.getId();
    PerformanceMeasurement performances = kernelPerformances.get(id);
    if(performances == null) performances = new PerformanceMeasurement();
    return performances;
  }

  public void clear(){
    kernelPerformances.clear();
  }

  public void printStatistics(DynamoJob job){
    System.out.println("Average execution times per device:");
    for(Entry<String, Long> entry:getPerformanceMeasurement(job).getDeviceRanking()){
      System.out.println(entry.getKey() + ": " + entry.getValue() + "ms");
    }
  }
}
