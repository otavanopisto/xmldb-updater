package fi.internetix.updater;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import fi.internetix.updater.core.UpdaterException;
import fi.internetix.updater.ui.DatabaseViewController;

public class Updater {

  public static void main(String[] args) {
    loadDrivers();
    new DatabaseViewController();
  } 
  
  private static void loadDrivers() {
    try {
      File driversFolder = new File("drivers");
      File[] files = driversFolder.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().endsWith(".jar");
        }
      });
      
      if (files != null) {
        for (int i = 0, l = files.length; i < l; i++) {
          addDriverJar(files[i]);
        }
      } else {
        System.err.println("Could not find any database drivers");
        System.exit(-1);
      }

    } catch (MalformedURLException e) {
      throw new UpdaterException(e);
    }
  }

  private static void addDriverJar(File jarFile) throws MalformedURLException {
    URL jarUrl = jarFile.toURI().toURL();
    try {
      URLClassLoader sysLoader = new URLClassLoader(new URL[0]);

      Method sysMethod = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{ URL.class });
      sysMethod.setAccessible(true);
      sysMethod.invoke(sysLoader, new Object[]{ jarUrl });

      UpdaterAgent.addClassPath(jarFile);
      
    } catch (SecurityException e) {
      throw new UpdaterException(e);
    } catch (NoSuchMethodException e) {
      throw new UpdaterException(e);
    } catch (IllegalArgumentException e) {
      throw new UpdaterException(e);
    } catch (IllegalAccessException e) {
      throw new UpdaterException(e);
    } catch (InvocationTargetException e) {
      throw new UpdaterException(e);
    }
  }

}
