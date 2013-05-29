package fi.internetix.updater.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.sql.Insert;
import org.hibernate.type.Type;

public class AlterDataInsertOperation extends GenericUpdateOperation {

  public AlterDataInsertOperation(String table, List<Column> columns, List<Object> values) {
    this.table = table;
    this.columns = columns;
    this.values = values;
  }

  @Override
  public String toSQL(Dialect dialect, String defaultCatalog, String defaultSchema) {
    Insert insert = new Insert(dialect)
      .setTableName(table);
    
    String[] columnNames = new String[this.columns.size()];
    String[] valueExpressions = new String[this.columns.size()];
    boolean[] settable = new boolean[columns.size()];
    
    for (int i = 0, l = columns.size(); i < l; i++) {
      columnNames[i] = columns.get(i).getName();
      valueExpressions[i] = columns.get(i).getWriteExpr();
      settable[i] = true;
    }
    
    insert.addColumns(columnNames, settable, valueExpressions);
      
    return insert.toStatementString();
  }
  
  @Override
  public void executeStatement(PreparedStatement statement) throws SQLException {
    for (int i = 0, l = columns.size(); i < l; i++) {
      Type type = columns.get(i).getValue().getType();
      Object value = values.get(i);
      type.nullSafeSet(statement, value, i + 1, null);
    }
    
    super.executeStatement(statement);
  }

  private String table;
  private List<Column> columns;
  private List<Object> values;
}
