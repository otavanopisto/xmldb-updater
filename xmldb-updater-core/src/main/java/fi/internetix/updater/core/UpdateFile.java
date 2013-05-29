package fi.internetix.updater.core;

import java.io.File;

public class UpdateFile {

  public UpdateFile(File file, UpdateVersion updateVersion) {
    this.file = file;
    this.updateVersion = updateVersion;
  }
  
  public File getFile() {
    return file;
  }
  
  public UpdateVersion getUpdateVersion() {
    return updateVersion;
  }
  
  @Override
  public String toString() {
    return updateVersion + " - " + file.getName();
  }
  
  private File file;
  private UpdateVersion updateVersion;
}
