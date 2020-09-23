package fr.dynamo.samples.kmeans;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class KMeansKernel extends DynamoKernel{

  public final double[] coordinatesX;
  public final double[] coordinatesY;
  public final double[] centroidsX;
  public final double[] centroidsY;
  public final int[] relatedClusterIndex;
  private final int centroidsCount_$constant$;

  public KMeansKernel(DynamoJob job, Range range, double[] coordinatesX, double[] coordinatesY, int[] relatedClusterIndex, double[] centroidsX, double[] centroidsY){
     super(job, range);
     this.coordinatesX = coordinatesX;
     this.coordinatesY = coordinatesY;
     this.relatedClusterIndex = relatedClusterIndex;
     this.centroidsX = centroidsX;
     this.centroidsY = centroidsY;
     this.centroidsCount_$constant$ = centroidsX.length;
  }

  @Override
  public void run() {
    int index = getGlobalId(0);

    double x = coordinatesX[index];
    double y = coordinatesY[index];
    int clusterIndex = -1;

    double minimalDistance = Double.MAX_VALUE;

    for(int i=0; i<centroidsCount_$constant$; i++){
      double clusterX = centroidsX[i];
      double clusterY = centroidsY[i];

      /*
       * Calculate the power yourself because using the pow function will cause huge performance penalties (20x slower)!
       * For example don't use 'double distance = Math.sqrt(Math.pow((clusterX - x),2) + Math.pow((clusterY - y),2));'
       */

      double xDistance = (clusterX - x) * (clusterX - x);
      double yDistance = (clusterY - y) * (clusterY - y);
      double distance = Math.sqrt(xDistance + yDistance);

      if(distance < minimalDistance){
        minimalDistance = distance;
        clusterIndex = i;
      }
    }

    relatedClusterIndex[index] = clusterIndex;
  }


}
