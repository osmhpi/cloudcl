package fr.dynamo.logging;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.PropertyConfigurator;

public class Logger {

  private static Log log;

  public static Log instance(){
    if(log == null){
      String log4jConfPath = "log4j.properties";
      if(new File(log4jConfPath).exists()){
        PropertyConfigurator.configure(log4jConfPath);
      }
      log = new Log4JLogger("Dynamic OpenCL");
    }
    return log;
  }
}
