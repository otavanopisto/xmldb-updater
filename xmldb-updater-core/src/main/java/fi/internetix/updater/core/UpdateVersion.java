package fi.internetix.updater.core;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

public class UpdateVersion {

  public UpdateVersion(int major, float minor) {
    this.major = major;
    this.minor = minor;
  }
  
  public float getMinor() {
    return minor;
  }
  
  public int getMajor() {
    return major;
  }
  
  public boolean isNewerThan(UpdateVersion version) {
    return toFloat() > version.toFloat();
  }
  
  @Override
  public boolean equals(Object object) {
    if (object instanceof UpdateVersion) {
      return toFloat() == ((UpdateVersion) object).toFloat();
    }

    return false;
  }
  
  @Override
  public String toString() {
    return String.valueOf(getMajor() + getMinor());
  }

  public float toFloat() {
    return Float.valueOf(toString());
  }
  
  public static UpdateVersion parseVersion(String string) {
    if (!StringUtils.isBlank(string)) {
      String[] tmp = string.split("\\.");
      if (tmp.length == 2 && StringUtils.isNumeric(tmp[0]) && StringUtils.isNumeric(tmp[1])) {
        String major = tmp[0];
        String minor = "0." + tmp[1];
        return new UpdateVersion(NumberUtils.createInteger(major), NumberUtils.createFloat(minor));
      }
    }
    
    return null;
  }
  
  private float minor;
  private int major;
}
