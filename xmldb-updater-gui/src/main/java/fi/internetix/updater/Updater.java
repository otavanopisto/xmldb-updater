package fi.internetix.updater;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import fi.internetix.updater.core.DatabaseDriverUtils;
import fi.internetix.updater.core.UpdaterAgent;
import fi.internetix.updater.core.UpdaterException;
import fi.internetix.updater.ui.DatabaseViewController;

public class Updater {

  public static void main(String[] args) {
    if (!DatabaseDriverUtils.loadDrivers("drivers")) {
      System.err.println("Could not find any database drivers");
      System.exit(-1);
    }
    new DatabaseViewController();
  } 
  
}
