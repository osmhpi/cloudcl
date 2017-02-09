package fr.dynamo.samples.kmeans;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.execution.DynamoExecutor;
import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class KMeansJob extends DynamoJob{
  public double[] centroidCoordinatesX;
  public double[] centroidCoordinatesY;

  public KMeansJob(int clusterCount, int pointCount, int iterations, DevicePreference preference, ThreadFinishedNotifyable callback) throws InterruptedException {
    super("KMeans", callback);
    centroidCoordinatesX = new double[clusterCount];
    centroidCoordinatesY = new double[clusterCount];

    double maxCoordinate = 1000.0f;

    Random random = new Random(1000);

    for(int i = 0; i<clusterCount; i++){
      centroidCoordinatesX[i] = random.nextFloat() * maxCoordinate % maxCoordinate;
      centroidCoordinatesY[i] = random.nextFloat() * maxCoordinate % maxCoordinate;
    }

    double[] coordinatesX = new double[pointCount];
    double[] coordinatesY = new double[pointCount];
    int[] relatedClusterIndex = new int[pointCount];

    for(int j = 0; j<pointCount; j++){
      coordinatesX[j] = random.nextFloat() * maxCoordinate % maxCoordinate;
      coordinatesY[j] = random.nextFloat() * maxCoordinate % maxCoordinate;
    }

    KMeansKernel kernel = new KMeansKernel(this, Range.create(pointCount, 100), coordinatesX, coordinatesY, relatedClusterIndex, centroidCoordinatesX, centroidCoordinatesY);
    kernel.setDevicePreference(preference);
    kernel.setExplicit(true);

    addKernel(kernel);

    for(int i=0;i<iterations;i++){
      reset();
      kernel.put(kernel.coordinatesX);
      kernel.put(kernel.coordinatesY);
      kernel.put(kernel.relatedClusterIndex);
      kernel.put(kernel.centroidsX);
      kernel.put(kernel.centroidsY);

      DynamoExecutor.instance().submit(this);
      awaitTermination(1, TimeUnit.DAYS);
      updateCentroids(centroidCoordinatesX, centroidCoordinatesY, getFinishedKernels().get(0));
    }
  }

  public static void updateCentroids(double[] clusters_x, double[] clusters_y, DynamoKernel dynamoKernel){
    KMeansKernel kernel = (KMeansKernel)dynamoKernel;
    kernel.get(kernel.centroidsX);
    kernel.get(kernel.centroidsY);

    HashMap<Integer, Centroid> centroids = new HashMap<Integer, Centroid>();
    for(int i=0;i<clusters_x.length;i++){
      centroids.put(i, new Centroid(clusters_x[i], clusters_y[i]));
    }

    for(int i = 0; i<kernel.relatedClusterIndex.length;i++){
      int cluster = kernel.relatedClusterIndex[i];
      double x = kernel.coordinatesX[i];
      double y = kernel.coordinatesY[i];

      centroids.get(cluster).add(x, y);
    }

    for(int i = 0; i<centroids.size();i++){
      Centroid c = centroids.get(i);

      clusters_x[i] = c.get_x();
      clusters_y[i] = c.get_y();
    }

    kernel.dispose();
  }


}
