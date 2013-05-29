package fi.internetix.updater.core;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Table;

public class DropColumnUpdateOperation extends GenericUpdateOperation {

  public DropColumnUpdateOperation(Table table, String columnName) {
    this.table = table;
    this.columnName = columnName;
  }
  
  @Override
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema) {
    StringBuffer alter = new StringBuffer( "alter table " )
      .append( table.getQualifiedName( dialect, defaultCatalog, defaultSchema ) )
      .append( ' ' )
      .append("drop column ")
      .append(columnName);
    
    return alter.toString();
  }
   
  private Table table;
  private String columnName;
}
