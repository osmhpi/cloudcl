package fr.dynamo.samples.kmeans;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.execution.KernelExecutor;

public class Main {

  public static void main(String[] args) throws InterruptedException {

    int pointCount = Integer.parseInt(args[0]);
    int clusterCount = Integer.parseInt(args[1]);
    float maxCoordinate = 1000.0f;
    float diff = 0.01f;

    Random random = new Random(1000);

    float[] coordinatesX = new float[pointCount];
    float[] coordinatesY = new float[pointCount];
    int[] relatedClusterIndex = new int[pointCount];

    for(int i = 0; i<pointCount; i++){
      coordinatesX[i] = random.nextFloat() * maxCoordinate % maxCoordinate;
      coordinatesY[i] = random.nextFloat() * maxCoordinate % maxCoordinate;
    }

    float[] centroidCoordinatesX = new float[clusterCount];
    float[] centroidCoordinatesY = new float[clusterCount];

    for(int i = 0; i<clusterCount; i++){
      centroidCoordinatesX[i] = random.nextFloat() * maxCoordinate % maxCoordinate;
      centroidCoordinatesY[i] = random.nextFloat() * maxCoordinate % maxCoordinate;
    }

    boolean firstIteration = true;

    outer: while(true){
      KernelExecutor executor = new KernelExecutor();
      KMeansKernel kernel = new KMeansKernel(Range.create(pointCount, 100), coordinatesX, coordinatesY, relatedClusterIndex, centroidCoordinatesX, centroidCoordinatesY);
      kernel.setExplicit(true);

      if(firstIteration){
        kernel.put(coordinatesX).put(coordinatesY).put(relatedClusterIndex).put(centroidCoordinatesX).put(centroidCoordinatesY);
        firstIteration = false;
      }

      kernel.setDevicePreference(DevicePreference.CPU_ONLY);
      executor.execute(kernel);
      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.DAYS);
      kernel.get(centroidCoordinatesX).get(centroidCoordinatesY);

      float[] oldCentroidsX = centroidCoordinatesX.clone();
      float[] oldCentroidsY = centroidCoordinatesY.clone();

      System.out.println(Arrays.toString(centroidCoordinatesX) + " " + Arrays.toString(centroidCoordinatesY));

      updateCentroids(centroidCoordinatesX, centroidCoordinatesY, kernel);
      System.out.println(Arrays.toString(centroidCoordinatesX) + " " + Arrays.toString(centroidCoordinatesY));
      for(int i=0; i<clusterCount;i++){
        if(Math.abs(oldCentroidsX[i] - centroidCoordinatesX[i]) > diff) continue outer;
        if(Math.abs(oldCentroidsY[i] - centroidCoordinatesY[i]) > diff) continue outer;
      }
      break;
    }

    System.out.println("FINAL");
    System.out.println(Arrays.toString(centroidCoordinatesX) + " " + Arrays.toString(centroidCoordinatesY));
  }

  public static void updateCentroids(float[] clusters_x, float[] clusters_y, KMeansKernel kernel){

    HashMap<Integer, Centroid> centroids = new HashMap<Integer, Centroid>();
    for(int i=0;i<clusters_x.length;i++){
      centroids.put(i, new Centroid());
    }

    for(int i = 0; i<kernel.relatedClusterIndex.length;i++){
      int cluster = kernel.relatedClusterIndex[i];
      float x = kernel.coordinatesX[i];
      float y = kernel.coordinatesY[i];

      centroids.get(cluster).add(x, y);
    }

    for(int i = 0; i<centroids.size();i++){
      Centroid c = centroids.get(i);

      clusters_x[i] = c.get_x();
      clusters_y[i] = c.get_y();
    }
  }

}
