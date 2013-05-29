package fi.internetix.updater.core;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;

public class UpgradeBatch {
  
  public UpgradeBatch() {
  }
  
  public void addCreateTable(Table table) {
    operations.add(new CreateTableUpdateOperation(table));
  }
  
  public void addCreateForeignKey(ForeignKey foreignKey) {
    operations.add(new CreateForeignKeyUpdateOperation(foreignKey));
  }
  
  public void addAddColumn(Table table, Column column) {
    operations.add(new AddColumnUpdateOperation(table, column));
  }
  
  public void addChangeColumn(Table table, String oldName, Column column) {
    operations.add(new ChangeColumnUpdateOperation(table, oldName, column));
  }
  
  public void addDropColumn(Table table, String columnName) {
    operations.add(new DropColumnUpdateOperation(table, columnName));
  }
 
  public void addDropForeignKey(Table table, String foreignKeyName) {
    operations.add(new DropForeignKeyUpdateOperation(table, foreignKeyName));
  }
  
  public void addDropTable(String tableName) {
    operations.add(new DropTableUpdateOperation(tableName));
  }
  
  public void addAlterDataInsert(String table, List<Column> columns, List<Object> values) {
    operations.add(new AlterDataInsertOperation(table, columns, values));
  }
  
  public void addAlterDataUpdate(String table, List<Column> updaterColumns, List<Object> updateValues, List<Column> whereColumns, List<String> whereOperators, List<Object> whereValues) {
    operations.add(new AlterDataUpdateOperation(table, updaterColumns, updateValues, whereColumns, whereOperators, whereValues));
  }
  
  public void addAlterDataDelete(String table, List<Column> whereColumns, List<String> whereOperators, List<Object> whereValues) {
    operations.add(new AlterDataDeleteOperation(table, whereColumns, whereOperators, whereValues));
  }
  
  public void addSql(String sql) {
    operations.add(new SqlUpdateOperation(sql));
  }
  
  public List<UpdateOperation> getOperations() {
    return operations;
  }

  private List<UpdateOperation> operations = new ArrayList<UpdateOperation>();
}
