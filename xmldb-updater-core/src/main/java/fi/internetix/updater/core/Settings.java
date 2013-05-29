package fi.internetix.updater.core;

import java.io.File;

import org.hibernate.dialect.Dialect;

public class Settings {

	public Settings(File updatesFolder, Class<Dialect> dialect, String databaseUrl, String databaseUsername, String databasePassword) {
		super();

		this.updatesFolder = updatesFolder;
		this.dialect = dialect;
		this.databaseUrl = databaseUrl;
		this.databaseUsername = databaseUsername;
		this.databasePassword = databasePassword;
	}

	public String getDatabasePassword() {
		return databasePassword;
	}
	
	public void setDatabasePassword(String databasePassword) {
		this.databasePassword = databasePassword;
	}
	
	public String getDatabaseUrl() {
		return databaseUrl;
	}
	
	public void setDatabaseUrl(String databaseUrl) {
		this.databaseUrl = databaseUrl;
	}
	
	public String getDatabaseUsername() {
		return databaseUsername;
	}
	
	public void setDatabaseUsername(String databaseUsername) {
		this.databaseUsername = databaseUsername;
	}
	
	public Class<Dialect> getDialect() {
		return dialect;
	}
	
	public void setDialect(Class<Dialect> dialect) {
		this.dialect = dialect;
	}
	
	public File getUpdatesFolder() {
		return updatesFolder;
	}
	
	public void setUpdatesFolder(File updatesFolder) {
		this.updatesFolder = updatesFolder;
	}

	private File updatesFolder;
	private Class<Dialect> dialect;
	private String databaseUrl;
  private String databaseUsername;
	private String databasePassword;
}
