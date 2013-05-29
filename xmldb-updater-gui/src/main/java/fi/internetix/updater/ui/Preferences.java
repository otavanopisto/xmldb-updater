package fi.internetix.updater.ui;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Preferences {
  
  public static synchronized String get(String name) {
    if (preferences == null) {
      preferences = new Properties();
      try {
        preferences.load(new FileReader("preferences.properties"));
      } catch (FileNotFoundException e) {
        logger.warn("Preferences file reading failed: " + e.getMessage());
      } catch (IOException e) {
        logger.warn("Preferences file reading failed: " + e.getMessage());
      }
    }
    
    return (String) preferences.get(name);
  }
  
  private static Properties preferences;
  private static Logger logger = Logger.getRootLogger();
}
