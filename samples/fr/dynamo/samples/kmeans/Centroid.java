package fr.dynamo.samples.kmeans;

public class Centroid {

  private double sumX;
  private double sumY;
  private int count;

  public void add(double x, double y){
    count++;
    sumX += x;
    sumY += y;
  }

  public double get_x(){
    return sumX / count;
  }

  public double get_y(){
    return sumY / count;
  }
}
