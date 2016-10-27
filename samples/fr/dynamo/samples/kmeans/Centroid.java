package fr.dynamo.samples.kmeans;

public class Centroid {

  private double currentX;
  private double currentY;
  private double sumX;
  private double sumY;
  private int count;

  public Centroid(double currentX, double currentY) {
    super();
    this.currentX = currentX;
    this.currentY = currentY;
  }

  public void add(double x, double y){
    count++;
    sumX += x;
    sumY += y;
  }

  public double get_x(){
    if(count == 0) return currentX;
    return sumX / count;
  }

  public double get_y(){
    if(count == 0) return currentY;
    return sumY / count;
  }
}
