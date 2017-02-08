package fr.dynamo.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amd.aparapi.device.OpenCLDevice;

public class PerformanceMeasurement {

  private Map<String, List<Long>> measurements = new HashMap<String, List<Long>>();

  public PerformanceMeasurement() {
    super();
  }

  public void addMeasurement(OpenCLDevice device, long timing){
    String deviceName = device.getPerformanceIdentifier();
    if(!measurements.containsKey(deviceName)){
      measurements.put(deviceName, new ArrayList<Long>());
    }

    measurements.get(deviceName).add(timing);
  }

  public Map<String, List<Long>> getMeasurements() {
    return measurements;
  }

  public List<Entry<String, Long>> getDeviceRanking(){
    Map<String, Long> averages = new HashMap<String, Long>();
    for(String key:measurements.keySet()){
      averages.put(key, Statistics.average(measurements.get(key)));
    }
    return devicesSortedByPerformance(averages);
  }

  private List<Entry<String, Long>> devicesSortedByPerformance(Map<String, Long> map) {

    List<Entry<String, Long>> sortedEntries = new ArrayList<Entry<String, Long>>(map.entrySet());

    Collections.sort(sortedEntries,
        new Comparator<Entry<String, Long>>() {
      @Override
      public int compare(Entry<String, Long> e1, Entry<String, Long> e2) {
        return e1.getValue().compareTo(e2.getValue());
      }
    });

    return sortedEntries;
  }

}
