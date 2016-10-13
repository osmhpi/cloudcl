package fr.dynamo.samples.kmeans;
import com.amd.aparapi.Range;

import fr.dynamo.threading.TileKernel;

public class KMeansKernel extends TileKernel{

  public float[] coordinatesX;
  public float[] coordinatesY;
  public float[] centroidsX;
  public float[] centroidsY;
  public int[] relatedClusterIndex;

  public KMeansKernel(Range range, float[] coordinatesX, float[] coordinatesY, int[] relatedClusterIndex, float[] centroidsX, float[] centroidsY){
     super(range);
     this.coordinatesX = coordinatesX;
     this.coordinatesY = coordinatesY;
     this.relatedClusterIndex = relatedClusterIndex;
     this.centroidsX = centroidsX;
     this.centroidsY = centroidsY;
  }

  @Override
  public void run() {
    int index = getGlobalId(0);

    float x = coordinatesX[index];
    float y = coordinatesY[index];
    int clusterIndex = -1;

    float minimalDistance = Float.MAX_VALUE;

    for(int i=0; i<centroidsX.length; i++){
      float clusterX = centroidsX[i];
      float clusterY = centroidsY[i];

      float distance = (float)Math.sqrt(Math.pow((clusterX - x),2) + Math.pow((clusterY - y),2));
      if(distance <= minimalDistance){
        minimalDistance = distance;
        clusterIndex = i;
      }
    }

    relatedClusterIndex[index] = clusterIndex;
  }


}
