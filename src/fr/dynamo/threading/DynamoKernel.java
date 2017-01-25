package fr.dynamo.threading;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.instruction.InstructionSet.TypeSpec;

import fr.dynamo.DevicePreference;
import fr.dynamo.Notifyable;
import fr.dynamo.performance.NetworkEstimator;
import fr.dynamo.performance.NetworkSpeed;

public abstract class DynamoKernel extends Kernel implements Runnable{

  protected Range range;
  private DevicePreference devicePreference;
  private long executionTime = -1;
  private int remainingTries = 1;
  private final DynamoJob job;

  public DynamoKernel(DynamoJob job, Range range) {
    this(job, range,DevicePreference.NONE);
  }

  public DynamoKernel(DynamoJob job, Range range, DevicePreference devicePreference) {
    super();
    this.job = job;
    this.range = range;
    this.devicePreference = devicePreference;
  }

  public DynamoThread buildThread(OpenCLDevice device, Notifyable notifyable){
    getJob().getKernelsToRun().remove(this);
    return new DynamoThread(this, device, notifyable);
  }

  public void execute(){
    Device device = getTargetDevice();

    Range deviceSpecificRange = range;
    switch(range.getDims()){
      case 1:
        deviceSpecificRange = device.createRange(range.getGlobalSize_0(), range.getLocalSize_0());
        if(!deviceSpecificRange.isValid()){
          deviceSpecificRange = device.createRange(range.getGlobalSize_0());
        }
        break;
      case 2:
        deviceSpecificRange = device.createRange2D(range.getGlobalSize_0(), range.getGlobalSize_1(), range.getLocalSize_0(), range.getLocalSize_1());
        if(!deviceSpecificRange.isValid()){
          deviceSpecificRange = device.createRange2D(range.getGlobalSize_0(), range.getGlobalSize_1());
        }
        break;
      case 3:
        deviceSpecificRange = device.createRange3D(range.getGlobalSize_0(), range.getGlobalSize_1(), range.getGlobalSize_2(), range.getLocalSize_0(), range.getLocalSize_1(), range.getLocalSize_2());
        if(!deviceSpecificRange.isValid()){
          deviceSpecificRange = device.createRange3D(range.getGlobalSize_0(), range.getGlobalSize_1(), range.getGlobalSize_2());
        }
        break;
    }

    System.out.println("Kernel " + hashCode() + " executing " + deviceSpecificRange);
    long before = System.currentTimeMillis();

    execute(deviceSpecificRange);
    long after = System.currentTimeMillis();
    executionTime = after - before;
  }

  @Override
  public double getExecutionTime() {
    return executionTime;
  }

  public DevicePreference getDevicePreference() {
    return devicePreference;
  }

  public void setDevicePreference(DevicePreference devicePreference) {
    this.devicePreference = devicePreference;
  }

  public double getTransferrableGigabytes(){
    return getDataSize() / 1024.0 / 1024 / 1024;
  }

  public long getExpectedTransferTime(NetworkSpeed speed){
    return NetworkEstimator.calculateTranferTime(this, speed);
  }

  public long getDataSize(){
    long byteSum = 0;
    Field[] allFields = getClass().getDeclaredFields();
    for(Field f:allFields){
      Class<?> type = f.getType();
      long length = 0;

      if(type.isArray()){
        String typeString = type.toString().replace("class [", "").replace("[", "");

        try {
          f.setAccessible(true);
          length = getArrayLength(f.get(this));
          f.setAccessible(false);
        } catch (Exception e) {
          e.printStackTrace();
        }
        if(typeString.length() == 1){
          TypeSpec spec = TypeSpec.valueOf(typeString);
          byteSum += spec.getSize() * length;
        }else{
          long bytes = 0;
          for(Field nestedField:type.getComponentType().getDeclaredFields()){
            if(nestedField.getType().isPrimitive()){
              for(TypeSpec typeSpec:TypeSpec.values()){
                if(typeSpec.getLongName().equals(nestedField.getType().toString())){
                  byteSum += typeSpec.getSize() * length;
                  break;
                }
              }
            }
          }
          byteSum += bytes;
        }

      }

    }
    return byteSum;
  }

  private long getArrayLength(Object value){
    long length = 0;

      length = Array.getLength(value);

      while(value.getClass().isArray()){
        value = Array.get(value, 0);
        if(value.getClass().isArray()){
          length = length * Array.getLength(value);
        }
      }

    return length;
  }

  public int getRemainingTries() {
    return remainingTries;
  }

  public void setRemainingTries(int remainingTries) {
    this.remainingTries = remainingTries;
  }

  public void reduceRemainingTries(){
    this.remainingTries--;
  }

  public DynamoJob getJob() {
    return job;
  }


}
