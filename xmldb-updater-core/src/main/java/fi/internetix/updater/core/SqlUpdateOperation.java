package fi.internetix.updater.core;
import org.hibernate.dialect.Dialect;

public class SqlUpdateOperation extends GenericUpdateOperation {

  public SqlUpdateOperation(String sql) {
    this.sql = sql;
  }
  
  @Override
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema) {
    return sql;
  }
   
  private String sql;
}