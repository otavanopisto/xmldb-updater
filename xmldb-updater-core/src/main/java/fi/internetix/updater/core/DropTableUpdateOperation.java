package fi.internetix.updater.core;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Table;

public class DropTableUpdateOperation extends GenericUpdateOperation {
  
  public DropTableUpdateOperation(String tableName) {
    this.tableName = tableName;
  }
  
  @Override
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema) {
    StringBuffer sql = new StringBuffer( "drop table " )
      .append(new Table(tableName).getQualifiedName( dialect, defaultCatalog, defaultSchema ) );
  
    return sql.toString();
  }
  
  private String tableName;
}
