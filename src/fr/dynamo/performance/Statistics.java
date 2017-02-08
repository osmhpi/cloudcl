package fr.dynamo.performance;

import java.util.List;

public class Statistics {

  public static long average(List<Long> measurements){
    long sum = 0;
    long count = 0;
    for(long l:measurements){
      count++;
      sum += l;
    }
    return sum/count;
  }

  public static long stdev(List<Long> measurements){
    long average = average(measurements);

    long squaredDifference = 0;
    for(long l:measurements){
      squaredDifference += Math.pow(average - l, 2);
    }

    return Math.round(Math.sqrt(squaredDifference / measurements.size()));
  }


}
