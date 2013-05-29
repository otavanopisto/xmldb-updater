package fi.internetix.updater.core;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;

public class DropForeignKeyUpdateOperation extends GenericUpdateOperation {

  public DropForeignKeyUpdateOperation(Table table, String foreignKeyName) {
    this.table = table;
    this.foreignKeyName = foreignKeyName;
  }
  
  @Override
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema) {
    ForeignKey foreignKey = new ForeignKey();
    foreignKey.setTable(table);
    foreignKey.setName(foreignKeyName);
    return foreignKey.sqlDropString(dialect, defaultCatalog, defaultSchema);
  }
   
  private Table table;
  private String foreignKeyName;
}
