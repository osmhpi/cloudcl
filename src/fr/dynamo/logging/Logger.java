package fr.dynamo.logging;

import org.apache.commons.logging.Log;

public class Logger {

  private static Log log;

  public static Log instance(){
    if(log == null){
    }
    return log;
  }
}
