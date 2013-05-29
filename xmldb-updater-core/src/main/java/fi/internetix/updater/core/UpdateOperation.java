package fi.internetix.updater.core;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;

public interface UpdateOperation {
  
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema);
  public void executeStatement(PreparedStatement statement) throws SQLException;
  
}
