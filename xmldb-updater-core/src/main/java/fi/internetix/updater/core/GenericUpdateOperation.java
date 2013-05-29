package fi.internetix.updater.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class GenericUpdateOperation implements UpdateOperation {

  @Override
  public void executeStatement(PreparedStatement statement) throws SQLException {
    statement.execute();
  }
  
}
