package fr.dynamo.samples.kmeans;

public class Centroid {

  private float sumX;
  private float sumY;
  private int count;

  public void add(float x, float y){
    count++;
    sumX += x;
    sumY += y;
  }

  public float get_x(){
    return sumX / count;
  }

  public float get_y(){
    return sumY / count;
  }
}
