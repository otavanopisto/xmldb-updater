package fi.internetix.updater.core;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;

public class DatabaseDriverUtils {

  public static boolean loadDrivers(String driverFolder) {
    boolean foundAnyDrivers = false;
    
    try {
      File driversFolder = new File(driverFolder);
      File[] files = driversFolder.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().endsWith(".jar");
        }
      });
      
      for (int i = 0, l = files.length; i < l; i++) {
        addDriverJar(files[i]);
        foundAnyDrivers = true;
      }

    } catch (MalformedURLException e) {
      throw new UpdaterException(e);
    }
    
    return foundAnyDrivers;
  }

  private static void addDriverJar(File jarFile) throws MalformedURLException {
    try {
      UpdaterAgent.addClassPath(jarFile);
    } catch (SecurityException e) {
      throw new UpdaterException(e);
    } catch (IllegalArgumentException e) {
      throw new UpdaterException(e);
    }
  }

}
