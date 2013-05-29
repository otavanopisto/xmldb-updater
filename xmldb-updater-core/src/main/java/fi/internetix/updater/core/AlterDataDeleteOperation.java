package fi.internetix.updater.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.sql.Delete;
import org.hibernate.type.Type;

public class AlterDataDeleteOperation extends GenericUpdateOperation {

  public AlterDataDeleteOperation(String table, List<Column> whereColumns, List<String> whereOperators, List<Object> whereValues) {
    this.table = table;
    this.whereColumns = whereColumns;
    this.whereOperators = whereOperators;
    this.whereValues = whereValues;
  }

  @Override
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema) {
    Delete delete = new Delete()
      .setTableName(table);
    
    for (int i = 0, l = whereColumns.size(); i < l; i++) {
      if (whereValues.get(i) == null)
        delete.addWhereFragment(whereColumns.get(i).getName() + " is null");
      else
        delete.addWhereFragment(whereColumns.get(i).getName() + " " + whereOperators.get(i) + whereColumns.get(i).getWriteExpr());
    }
    
    return delete.toStatementString();
  }
  
  @Override
  public void executeStatement(PreparedStatement statement) throws SQLException {
    int parameterIndex = 1;
    
    for (int i = 0, l = whereColumns.size(); i < l; i++) {
      Type type = whereColumns.get(i).getValue().getType();
      Object value = whereValues.get(i);
      if (value != null) {
        type.nullSafeSet(statement, value, parameterIndex, null);
        parameterIndex++;
      }
    }
    
    super.executeStatement(statement);
  }

  private String table;
  private List<Column> whereColumns;
  private List<String> whereOperators;
  private List<Object> whereValues;
}
