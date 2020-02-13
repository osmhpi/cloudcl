package fr.dynamo.test;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import fr.dynamo.performance.Statistics;

public class StatisticsTest {

  final Long[] values = new Long[]{9L, 2L, 5L, 4L, 12L, 7L, 8L, 11L, 9L, 3L, 7L, 4L, 12L, 5L, 4L, 10L, 9L, 6L, 9L, 4L};


  @Test
  public void testAverage() {
    assertEquals(7, Statistics.average(Arrays.asList(values)));
  }

  @Test
  public void testStandardDeviation() {
    assertEquals(3, Statistics.stdev(Arrays.asList(values)));
  }
}
