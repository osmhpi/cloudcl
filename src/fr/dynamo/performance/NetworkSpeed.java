package fr.dynamo.performance;

public enum NetworkSpeed {
  MBIT10  (1250000),
  MBIT100 (12500000),
  GBIT    (125000000),
  GBIT10  (1250000000);

  public final long bytesPerSecond;

  NetworkSpeed(long bytesPerSecond){
    this.bytesPerSecond = bytesPerSecond;
  }
}
