package fr.dynamo.ec2;

import com.amazonaws.services.ec2.AmazonEC2;

public class MachineManager {

  private static MachineManager instance;

  private static AmazonEC2 client;

  private MachineManager(){
  }

  public static MachineManager instance(){
    if(client == null){
      throw new IllegalStateException("MachineManager requires to have a valid Amazon EC2 client set before initialization.");
    }

    if(instance == null){
      instance = new MachineManager();
    }

    return instance;
  }

  public AmazonEC2 getClient() {
    return client;
  }

  public static void setClient(AmazonEC2 c) {
    client = c;
  }



}
