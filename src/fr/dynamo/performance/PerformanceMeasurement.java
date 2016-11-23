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

  public List<Entry<String, Long>> getDeviceRanking(){
    Map<String, Long> averages = new HashMap<String, Long>();
    for(String key:measurements.keySet()){
      long sum = 0;
      long count = 0;
      for(long l:measurements.get(key)){
        count++;
        sum += l;
      }
      averages.put(key, sum/count);
    }
    return entriesSortedByValues(averages);
  }

  static <K,V extends Comparable<? super V>>
  List<Entry<K, V>> entriesSortedByValues(Map<K,V> map) {

    List<Entry<K,V>> sortedEntries = new ArrayList<Entry<K,V>>(map.entrySet());

    Collections.sort(sortedEntries,
        new Comparator<Entry<K,V>>() {
      @Override
      public int compare(Entry<K,V> e1, Entry<K,V> e2) {
        return e1.getValue().compareTo(e2.getValue());
      }
    }
        );

    return sortedEntries;
  }

}
