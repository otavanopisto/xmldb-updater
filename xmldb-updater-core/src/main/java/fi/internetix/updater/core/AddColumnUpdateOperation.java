package fi.internetix.updater.core;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

public class AddColumnUpdateOperation extends GenericUpdateOperation {

  public AddColumnUpdateOperation(Table table, Column column) {
    this.table = table;
    this.column = column;
  }
  
  @Override
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema) {
    StringBuffer root = new StringBuffer( "alter table " )
      .append( table.getQualifiedName( dialect, defaultCatalog, defaultSchema ) )
      .append( ' ' )
      .append( dialect.getAddColumnString());
    
    StringBuffer alter = new StringBuffer( root.toString() )
      .append( ' ' )
      .append( column.getQuotedName( dialect ) )
      .append( ' ' )
      .append( column.getSqlType( dialect, null));
    
    String defaultValue = column.getDefaultValue();
    if ( defaultValue != null ) {
      alter.append( " default " ).append( defaultValue );
    }
  
    if ( column.isNullable() ) {
      alter.append( dialect.getNullColumnString() );
    } else {
      alter.append( " not null" );
    }
  
    boolean useUniqueConstraint = column.isUnique() && dialect.supportsUnique() &&
        ( !column.isNullable() || dialect.supportsNotNullUnique() );
    if ( useUniqueConstraint ) {
      alter.append( " unique" );
    }
  
    if ( column.hasCheckConstraint() && dialect.supportsColumnCheck() ) {
      alter.append( " check(" )
          .append( column.getCheckConstraint() )
          .append( ")" );
    }
  
    String columnComment = column.getComment();
    if ( columnComment != null ) {
      alter.append( dialect.getColumnComment( columnComment ) );
    }
    
    return alter.toString();
  }
   
  private Table table;
  private Column column;
}
