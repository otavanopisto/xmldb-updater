package fi.internetix.updater.ui;
 
import java.util.List;

import org.apache.log4j.Logger;

import fi.internetix.updater.core.Settings;
import fi.internetix.updater.core.Updater;

public class UpdaterViewController {
	
	private static Logger logger = Logger.getLogger(UpdaterViewController.class);

	public UpdaterViewController(Settings settings) {
    this.updater = new Updater(settings);
    if (!this.updater.testConnection()) {
    	System.err.println("Could not connect to database");
    	System.exit(-1);
    }
    
    view.showUi();
    
    logger.info("Updater started");
    logger.info("Current database version: " + updater.getCurrentVersion());
    
    checkForUpdates();
  }

	public void checkForUpdates() {
		List<String> updates = updater.checkForUpdates();
		view.setUpdateListItems(updates.toArray(new String[0]));
	}

	public void performUpgrade(boolean executeSqls) {
		updater.performUpgrade(executeSqls);
	}
	
  protected void exitApplication() {
    view.hideUi();
  }
  
  private UpdaterView view = new UpdaterView(this);
  private Updater updater;
}
