package fr.dynamo.samples.kmeans;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.execution.KernelExecutor;

public class Main {

  public static void main(String[] args) throws InterruptedException {
    KernelExecutor executor = new KernelExecutor();

    int pointCountPerKernel = Integer.parseInt(args[0]);
    int clusterCount = Integer.parseInt(args[1]);
    int kernelCount = Integer.parseInt(args[2]);

    double maxCoordinate = 1000.0f;
    double diff = 0.1f;

    Random random = new Random(1000);

    double[] centroidCoordinatesX = new double[clusterCount];
    double[] centroidCoordinatesY = new double[clusterCount];

    for(int i = 0; i<clusterCount; i++){
      centroidCoordinatesX[i] = random.nextFloat() * maxCoordinate % maxCoordinate;
      centroidCoordinatesY[i] = random.nextFloat() * maxCoordinate % maxCoordinate;
    }

    List<KMeansKernel> kernels = new ArrayList<KMeansKernel>();

    for(int i = 0; i<kernelCount; i++){

      double[] coordinatesX = new double[pointCountPerKernel];
      double[] coordinatesY = new double[pointCountPerKernel];
      int[] relatedClusterIndex = new int[pointCountPerKernel];

      for(int j = 0; j<pointCountPerKernel; j++){
        coordinatesX[j] = random.nextFloat() * maxCoordinate % maxCoordinate;
        coordinatesY[j] = random.nextFloat() * maxCoordinate % maxCoordinate;
      }

      KMeansKernel kernel = new KMeansKernel(Range.create(pointCountPerKernel, 100), coordinatesX, coordinatesY, relatedClusterIndex, centroidCoordinatesX, centroidCoordinatesY);
      kernels.add(kernel);
    }

    boolean firstIteration = true;

    long before = System.currentTimeMillis();
    outer: while(true){
      for(KMeansKernel kernel:kernels){
        //kernel.setExplicit(true);

        if(firstIteration){
          kernel.put(kernel.coordinatesX).put(kernel.coordinatesY);
          firstIteration = false;
        }

        kernel.put(kernel.relatedClusterIndex).put(centroidCoordinatesX).put(centroidCoordinatesY);

        kernel.setDevicePreference(DevicePreference.CPU_ONLY);
        executor.execute(kernel);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
        kernel.get(kernel.relatedClusterIndex);
      }

      double[] oldCentroidsX = centroidCoordinatesX.clone();
      double[] oldCentroidsY = centroidCoordinatesY.clone();

      updateCentroids(centroidCoordinatesX, centroidCoordinatesY, kernels);

      for(int i=0; i<clusterCount;i++){
        if(Math.abs(oldCentroidsX[i] - centroidCoordinatesX[i]) > diff) continue outer;
        if(Math.abs(oldCentroidsY[i] - centroidCoordinatesY[i]) > diff) continue outer;
      }

      break outer;

    }

    System.out.println("FINAL");
    System.out.println(Arrays.toString(centroidCoordinatesX) + " " + Arrays.toString(centroidCoordinatesY));

    long after = System.currentTimeMillis();
    System.out.println(after-before + " ms");
  }

  public static void updateCentroids(double[] clusters_x, double[] clusters_y, List<KMeansKernel> kernels){

    HashMap<Integer, Centroid> centroids = new HashMap<Integer, Centroid>();
    for(int i=0;i<clusters_x.length;i++){
      centroids.put(i, new Centroid(clusters_x[i], clusters_y[i]));
    }

    for(KMeansKernel kernel:kernels){
      for(int i = 0; i<kernel.relatedClusterIndex.length;i++){
        int cluster = kernel.relatedClusterIndex[i];
        double x = kernel.coordinatesX[i];
        double y = kernel.coordinatesY[i];

        centroids.get(cluster).add(x, y);
      }
    }

    for(int i = 0; i<centroids.size();i++){
      Centroid c = centroids.get(i);

      clusters_x[i] = c.get_x();
      clusters_y[i] = c.get_y();
    }
  }

}
