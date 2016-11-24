package fr.dynamo.performance;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.amd.aparapi.device.OpenCLDevice;

import fr.dynamo.threading.TileKernel;

public class PerformanceCache {

  private static PerformanceCache instance;

  private Map<String, PerformanceMeasurement> kernelPerformances = new HashMap<String, PerformanceMeasurement>();

  private PerformanceCache(){}

  public static PerformanceCache getInstance(){
    if(instance == null) instance = new PerformanceCache();
    return instance;
  }

  public void addPerformanceMeasurement(TileKernel kernel, OpenCLDevice device, long executionSpeed){
    String className = kernel.getClass().getSimpleName();

    if(!kernelPerformances.containsKey(className)){
      kernelPerformances.put(className, new PerformanceMeasurement());
    }

    kernelPerformances.get(className).addMeasurement(device, executionSpeed);

  }

  public PerformanceMeasurement getPerformanceMeasurement(TileKernel kernel){
    String className = kernel.getClass().getSimpleName();
    PerformanceMeasurement performances = kernelPerformances.get(className);
    if(performances == null) performances = new PerformanceMeasurement();
    return performances;
  }

  public void clear(){
    kernelPerformances.clear();
  }

  public void printStatistics(TileKernel kernel){
    System.out.println("Average execution times per device:");
    for(Entry<String, Long> entry:getPerformanceMeasurement(kernel).getDeviceRanking()){
      System.out.println(entry.getKey() + ": " + entry.getValue() + "ms");
    }
  }
}
