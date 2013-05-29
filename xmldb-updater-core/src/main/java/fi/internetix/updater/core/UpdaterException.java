package fi.internetix.updater.core;
public class UpdaterException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UpdaterException(String message) {
    super(message);
  }

  public UpdaterException(Exception e) {
    super(e);
  }
  
  public UpdaterException(String message, Exception e) {
    super(message, e);
  }
}
