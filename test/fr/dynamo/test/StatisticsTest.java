package fr.dynamo.test;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import fr.dynamo.performance.Statistics;

public class StatisticsTest {

  Long[] values = new Long[]{(long) 9, (long)2, (long)5, (long)4, (long)12, (long)7, (long)8, (long)11, (long)9, (long)3, (long)7, (long)4, (long)12, (long)5, (long)4, (long)10, (long)9, (long)6, (long)9, (long)4};


  @Test
  public void testAverage() {
    assertEquals(7, Statistics.average(Arrays.asList(values)));
  }

  @Test
  public void testStandardDeviation() {
    assertEquals(3, Statistics.stdev(Arrays.asList(values)));
  }
}
