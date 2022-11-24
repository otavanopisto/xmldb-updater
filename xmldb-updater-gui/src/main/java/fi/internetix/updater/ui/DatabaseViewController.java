package fi.internetix.updater.ui;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import fi.internetix.updater.core.Settings;

public class DatabaseViewController {
  
  @SuppressWarnings("unused")
  private static Logger logger = Logger.getLogger(DatabaseViewController.class);

  public DatabaseViewController() {
    settings = new Settings(null, null, null, null, null);
    
    view = new DatabaseView(this);
    view.showUi();
    readPreferences();
  }
  
  public void openUpdater() {
    applySettings();
    this.view.dispose();
    openUpdaterView();
  }

  private void openUpdaterView() {
    new UpdaterViewController(settings);
  }
    
  private void readPreferences() {
    String updatesFolderPath = Preferences.get("updates.folder");
    if (StringUtils.isBlank(updatesFolderPath)) {
      System.err.println("updates.folder is not defined");
      System.exit(-1);
    }
    
    File updatesFolder = new File(updatesFolderPath);
    if (!updatesFolder.exists()) {
      System.err.println("folder specified in updates.folder does not exits");
      System.exit(-1);
    }
    
    if (!updatesFolder.isDirectory()) {
      System.err.println("folder specified in updates.folder is not a folder");
      System.exit(-1);
    }
    
    view.setDatabaseVendor(Preferences.get("database.vendor"));
    view.setDatabaseUrl(Preferences.get("database.url"));
    view.setDatabaseUsername(Preferences.get("database.username"));
    view.setDatabasePassword(Preferences.get("database.password"));
    settings.setUpdatesFolder(updatesFolder);
    
    applySettings();
  }

  private void applySettings() {
    settings.setDialect(view.getDialect());
    settings.setDatabaseUrl(view.getDatabaseUrl());
    settings.setDatabaseUsername(view.getDatabaseUsername());
    settings.setDatabasePassword(view.getDatabasePassword());
  }

  private DatabaseView view;
  private Settings settings;
}
