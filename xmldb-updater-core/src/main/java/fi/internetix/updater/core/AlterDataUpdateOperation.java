package fi.internetix.updater.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.sql.Update;
import org.hibernate.type.Type;

public class AlterDataUpdateOperation extends GenericUpdateOperation {

  public AlterDataUpdateOperation(String table, List<Column> updaterColumns, List<Object> updateValues, List<Column> whereColumns, List<String> whereOperators, List<Object> whereValues) {
    this.table = table;
    this.updateColumns = updaterColumns;
    this.updateValues = updateValues;
    this.whereColumns = whereColumns;
    this.whereOperators = whereOperators;
    this.whereValues = whereValues;
  }

  @Override
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema) {
    Update update = new Update(dialect)
      .setTableName(table);
    
    String[] updateColumnNames = new String[this.updateColumns.size()];
    String[] updateValueExpressions = new String[this.updateColumns.size()];
    boolean[] settable = new boolean[updateColumns.size()];
    
    for (int i = 0, l = updateColumns.size(); i < l; i++) {
      updateColumnNames[i] = updateColumns.get(i).getName();
      updateValueExpressions[i] = updateColumns.get(i).getWriteExpr();
      settable[i] = true;
    }
    
    update.addColumns(updateColumnNames, settable, updateValueExpressions);

    for (int i = 0, l = whereColumns.size(); i < l; i++) {
      if (whereValues.get(i) == null)
        update.addWhereColumn(whereColumns.get(i).getName(), " is null");
      else
        update.addWhereColumn(whereColumns.get(i).getName(), whereOperators.get(i) + whereColumns.get(i).getWriteExpr());
    }
    
    return update.toStatementString();
  }
  
  @Override
  public void executeStatement(PreparedStatement statement) throws SQLException {
    int parameterIndex = 1;
    
    for (int i = 0, l = updateColumns.size(); i < l; i++) {
      Type type = updateColumns.get(i).getValue().getType();
      Object value = updateValues.get(i);
      type.nullSafeSet(statement, value, parameterIndex, null);
      parameterIndex++;
    }
    
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
  private List<Column> updateColumns;
  private List<Object> updateValues;
  private List<Column> whereColumns;
  private List<String> whereOperators;
  private List<Object> whereValues;
}
