package fi.internetix.updater.core;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.ForeignKey;

public class CreateForeignKeyUpdateOperation extends GenericUpdateOperation {

  public CreateForeignKeyUpdateOperation(ForeignKey foreignKey) {
    this.foreignKey = foreignKey;
  }
  
  @Override
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema) {
    return foreignKey.sqlCreateString(dialect, null, defaultCatalog, defaultSchema);
  }
   
  private ForeignKey foreignKey;
}
