package fi.internetix.updater.core;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.mapping.ValueVisitor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Updater {

  private static Logger logger = Logger.getLogger(Updater.class);

  public Updater(Settings settings) {
    this.settings = settings;
  }
  
  public void performUpgrade(boolean executeSqls) {
    logger.info("Upgrading started");
    
    List<Document> documents;
    try {
      documents = getUpgradeDocuments();

      for (Document document : documents) {
        readMappings(upgradeBatch, document);
      }

    } catch (ParserConfigurationException e) {
      throw new UpdaterException(e);
    } catch (SAXException e) {
      throw new UpdaterException(e);
    } catch (IOException e) {
      throw new UpdaterException(e);
    } catch (TransformerException e) {
      throw new UpdaterException(e);
    }
    
    logger.info("Upgrade batch created");
    
    String defaultCatalog = null; // TODO: Resolve correct catalog
    String defaultSchema = null; // TODO: Resolve correct schema
    
    if (executeSqls) {
      logger.info("Executing SQLs");
    } else {
      logger.info("Simulating SQLs");
    }

    preformPreUpdateOperations();
    
    for (UpdateOperation operation : upgradeBatch.getOperations()) {
      executeUpdateOperation(defaultCatalog, defaultSchema, operation, executeSqls);
    }
    
    preformPostUpdateOperations();
    
    if (executeSqls) {
      updateVersionInfo();
      logger.info("Upgrade succesful");
    }
  }
  
  public boolean testConnection() {
    try {
      Connection c = getConnection();
      if (c != null) {
        boolean closed = c.isClosed();
        if (closed) {
          logger.error("Database connection is closed");
        } else {
          return true;
        }
      } else {
        logger.error("Could not obtain database connection");
      }
    } catch (SQLException e) {
      logger.error(e);
    }
    
    return false;
  }
    
  private void executeUpdateOperation(String defaultCatalog, String defaultSchema, UpdateOperation updateOperation, boolean executeSqls) {
    String SQL = updateOperation.toSQL(getDialect(), defaultCatalog, defaultSchema);

    if (executeSqls) {
      try {
        logger.info("Executing Query: " + SQL);
        PreparedStatement statement = getConnection().prepareStatement(SQL);
       
        updateOperation.executeStatement(statement);
        
        getConnection().commit();
        statement.close();
        logger.info("Execution Successful.");
      } catch (SQLException e) {
        logger.error("Error occured while running SQL query: " + e.getMessage());
        
        try {
          getConnection().rollback();
          throw new UpdaterException(e);
        } catch (SQLException e1) {
          throw new UpdaterException(e1);
        }
      }
    } else {
      logger.info(SQL);
    }
  }
  
  public List<String> checkForUpdates() {
    logger.info("Checking for updates...");
    
    newestVersion = getCurrentVersion();
    
    List<String> updates = new ArrayList<String>();
    List<UpdateFile> updateFiles = getUpdateFiles();
    for (UpdateFile updateFile : updateFiles) {
      updates.add(updateFile.toString());
      if (updateFile.getUpdateVersion().isNewerThan(newestVersion))
        newestVersion = updateFile.getUpdateVersion();
    }
    
    logger.info(updates.size() + " update(s) found.");
    
    if (updates.size() > 0) {
      logger.info("Current version is " + getCurrentVersion() + " and newest update version is " + newestVersion + ". Upgrade is needed");
    } else {
      logger.info("Current version is " + getCurrentVersion() + " and newest update version is " + newestVersion + ". Upgrade is not needed");
    }
    
    return updates;
  }

  private List<Document> getUpgradeDocuments() throws ParserConfigurationException, SAXException, IOException {
    List<Document> result = new ArrayList<Document>();

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

    List<UpdateFile> updateFiles = getUpdateFiles();
    for (UpdateFile upgradeFile : updateFiles) {
      Document document = documentBuilder.parse(new InputSource(new FileReader(upgradeFile.getFile())));
      result.add(document);
    }
    
    return result;
  }

  private List<UpdateFile> getUpdateFiles() {
    if (updateFiles == null) {
      updateFiles = new ArrayList<UpdateFile>();
      
      FileFilter xmlFileFilter = new XMLFileFilter();
  
      File file = settings.getUpdatesFolder();
      if (file.isDirectory()) {
        for (File versionFolder : file.listFiles()) {
          if (versionFolder.isDirectory()) {
            UpdateVersion fileVersion = UpdateVersion.parseVersion(versionFolder.getName());
            
            if ((fileVersion != null) && (fileVersion.isNewerThan(getCurrentVersion()))) {
              File[] folderFiles = versionFolder.listFiles(xmlFileFilter);
              for (File folderFile : folderFiles) {
                updateFiles.add(new UpdateFile(folderFile, fileVersion));
              }
            }
          }
        }
      }
      
      Collections.sort(updateFiles, new Comparator<UpdateFile>() {
        @Override
        public int compare(UpdateFile file1, UpdateFile file2) {
          if (!file1.getUpdateVersion().equals(file2)) {
            int result = file1.getUpdateVersion().isNewerThan(file2.getUpdateVersion()) ? 1 : -1;
            if (result == 0) {
              return file2.getFile().getName().compareTo(file1.getFile().getName());
            }
            
            return result;
          }
          
          return 0;
        }
      });
    }

    return updateFiles;
  }
  
  public void preformPreUpdateOperations() {
    // TODO: Only for MySQL
    try {
      logger.info("Disabling foreign key checks");
      PreparedStatement statement = getConnection().prepareStatement("SET foreign_key_checks = 0");
      statement.executeQuery();
    } catch (SQLException e) {
      logger.error("Updater table does not exists. Adding create batch operation");
      e.printStackTrace();
    }
  }
  
  public void preformPostUpdateOperations() {
    // TODO: Only for MySQL
    try {
      logger.info("Enabling foreign key checks");
      // TODO: Setting foreign_key_checks to 1 does not trigger a scan of the existing table data. 
      PreparedStatement statement = getConnection().prepareStatement("SET foreign_key_checks = 1");
      statement.executeQuery();
    } catch (SQLException e) {
      logger.error("Updater table does not exists. Adding create batch operation");
      e.printStackTrace();
    }
  }
  
  
  public UpdateVersion getCurrentVersion() {
    if (currentVersion == null) {
      try {
        PreparedStatement statement = getConnection().prepareStatement("select value from UpdaterProperties where name = ?");
        statement.setString(1, "databaseVersion");
        ResultSet resultSet = statement.executeQuery();
        
        if (resultSet.next()) {
          String versionString = resultSet.getString(1);
          if (!StringUtils.isBlank(versionString))
            currentVersion = UpdateVersion.parseVersion(versionString);
        }
      } catch (SQLException e) {
        logger.error("Updater table does not exists. Adding create batch operation");
        createUpdaterTable();
      }
    }
    
    if (currentVersion == null)
      currentVersion = new UpdateVersion(0, 0);
    return currentVersion; 
  }
  
  private void updateVersionInfo() {
    try {
      PreparedStatement statement = getConnection().prepareStatement("update UpdaterProperties set value = ? where name = ?");
      statement.setString(1, newestVersion.toString());
      statement.setString(2, "databaseVersion");
      
      if (statement.executeUpdate() == 0) {
        statement = getConnection().prepareStatement("insert into UpdaterProperties (name, value) values (?, ?)");
        statement.setString(1, "databaseVersion");
        statement.setString(2, newestVersion.toString());
        statement.execute();
      }
      
      getConnection().commit();
    } catch (SQLException e) {
      try {
        getConnection().rollback();
        logger.error("Database version setting failed");
        throw new UpdaterException("Database version setting failed: " + e);
      } catch (SQLException e1) {
        logger.error("Database version setting failed");
        throw new UpdaterException("Database version setting failed" + e1);
      }
    }
  }
  
  private void createUpdaterTable() {
    Table updaterTable = new Table("UpdaterProperties");
    
    Column nameColumn = new Column("name");
    nameColumn.setValue(new ValueAdapter(updaterTable, TypeFactory.basic("java.lang.String")));
    nameColumn.setNullable(false);
    nameColumn.setUnique(true);
    nameColumn.setLength(255);
    updaterTable.addColumn(nameColumn);
    
    Column valueColumn = new Column("value");
    valueColumn.setValue(new ValueAdapter(updaterTable, TypeFactory.basic("java.lang.String")));
    valueColumn.setNullable(false);
    valueColumn.setUnique(false);
    valueColumn.setLength(255);
    updaterTable.addColumn(valueColumn);
    
    PrimaryKey primaryKey = new PrimaryKey();
    primaryKey.addColumn(nameColumn);
    updaterTable.setPrimaryKey(primaryKey);
    
    upgradeBatch.addCreateTable(updaterTable);
  }
  
  private void readMappings(UpgradeBatch upgradeBatch, Document document) throws TransformerException {
    try {
      // TODO: Refactor to mapping factory or something similar
      
      NodeList childNodes = document.getDocumentElement().getChildNodes();
      for (int i = 0, l = childNodes.getLength(); i < l; i++) {
        if (childNodes.item(i) instanceof Element) {
          Element childElement = (Element) childNodes.item(i);
          if ("createTables".equals(childElement.getTagName())) {
            NodeIterator tableIterator = XPathAPI.selectNodeIterator(childElement, "table");
            Element tableElement;
            while ((tableElement = (Element) tableIterator.nextNode()) != null) {
              handleCreateTableMapping(upgradeBatch, tableElement);
            }
          } else if ("dropTables".equals(childElement.getTagName())) {
            NodeIterator tableIterator = XPathAPI.selectNodeIterator(childElement, "table");
            Element tableElement;
            while ((tableElement = (Element) tableIterator.nextNode()) != null) {
              handleDropTableMapping(upgradeBatch, tableElement);
            }
          } else if ("alterTables".equals(childElement.getTagName())) {
            NodeIterator tableIterator = XPathAPI.selectNodeIterator(childElement, "table");
            Element tableElement;
            while ((tableElement = (Element) tableIterator.nextNode()) != null) {
              handleAlterTableMapping(upgradeBatch, tableElement);
            }
          } else if ("alterData".equals(childElement.getTagName())) {
            NodeIterator insertIterator = XPathAPI.selectNodeIterator(childElement, "insert");
            Element insertElement;
            while ((insertElement = (Element) insertIterator.nextNode()) != null) {
              handleAlterDataInsert(upgradeBatch, insertElement);
            }

            NodeIterator updateIterator = XPathAPI.selectNodeIterator(childElement, "update");
            Element updateElement;
            while ((updateElement = (Element) updateIterator.nextNode()) != null) {
              handleAlterDataUpdate(upgradeBatch, updateElement);
            }

            NodeIterator deleteIterator = XPathAPI.selectNodeIterator(childElement, "delete");
            Element deleteElement;
            while ((deleteElement = (Element) deleteIterator.nextNode()) != null) {
              handleAlterDataDelete(upgradeBatch, deleteElement);
            }

          } else if ("sql".equals(childElement.getTagName())) {
            upgradeBatch.addSql(childElement.getTextContent());
          }
        }
      }
    } catch (UpdaterException e) {
      logger.error(e);
      throw e;
    }
  }

  private void handleCreateTableMapping(UpgradeBatch upgradeBatch, Element tableElement) throws TransformerException {
    String tableName = tableElement.getAttribute("name");
    if (StringUtils.isBlank(tableName))
      throw new UpdaterException("Create table: Table must have a name");

    String primaryKeysString = tableElement.getAttribute("primaryKeys");
    if (StringUtils.isBlank(primaryKeysString))
      throw new UpdaterException("Create table: Table must have at least one primary column");

    List<String> primaryKeys = Arrays.asList(primaryKeysString.split(","));

    Table table = new Table(tableName);
    NodeIterator fieldIterator = XPathAPI.selectNodeIterator(tableElement, "fields/field");
    Element fieldElement;
    while ((fieldElement = (Element) fieldIterator.nextNode()) != null) {
      Column column = parseAddTableColumn(table, fieldElement);
      table.addColumn(column);
      setTableColumn(table, column);
    }
    
    PrimaryKey primaryKey = new PrimaryKey();
    for (String primaryColumn : primaryKeys) {
      Column column = getTableColumn(table, primaryColumn);
      if (column != null) {
        primaryKey.addColumn(column);
      } else {
        throw new UpdaterException("Create table: Primary column " + primaryColumn + " not found from the "  + tableName + " table");
      }
    }
    
    table.setPrimaryKey(primaryKey);

    upgradeBatch.addCreateTable(table);

    NodeIterator keyIterator = XPathAPI.selectNodeIterator(tableElement, "foreignKeys/key");
    Element keyElement;
    while ((keyElement = (Element) keyIterator.nextNode()) != null) {
      ForeignKey foreignKey = parseAddForeignKey(table, keyElement);
      upgradeBatch.addCreateForeignKey(foreignKey);
    }
    
    // TODO: Indexes
  }
  
  private void handleAlterTableMapping(UpgradeBatch upgradeBatch, Element tableElement) throws TransformerException {
    // TODO: Modify field
    
    String tableName = tableElement.getAttribute("name");
    if (StringUtils.isBlank(tableName))
      throw new UpdaterException("Alter table: Table must have a name");
    
    Table table = new Table(tableName);
    NodeIterator fieldIterator = XPathAPI.selectNodeIterator(tableElement, "addFields/field");
    Element fieldElement;
    while ((fieldElement = (Element) fieldIterator.nextNode()) != null) {
      Column column = parseAddTableColumn(table, fieldElement);
      table.addColumn(column);
      setTableColumn(table, column);
      upgradeBatch.addAddColumn(table, column);
    }
    
    fieldIterator = XPathAPI.selectNodeIterator(tableElement, "dropFields/field");
    while ((fieldElement = (Element) fieldIterator.nextNode()) != null) {
      String columnName = fieldElement.getAttribute("name");
      if (StringUtils.isBlank(columnName))
        throw new UpdaterException("Drop column: Column needs a name");
      removeTableColumn(table, columnName);
      upgradeBatch.addDropColumn(table, columnName);
    }
    
    fieldIterator = XPathAPI.selectNodeIterator(tableElement, "changeFields/field");
    while ((fieldElement = (Element) fieldIterator.nextNode()) != null) {
      String oldFieldName = fieldElement.getAttribute("name");
      String newFieldName = fieldElement.getAttribute("newName");
      String fieldType = fieldElement.getAttribute("type");
      boolean fieldNullable = !"false".equals(fieldElement.getAttribute("nullable"));
      String defaultValue = fieldElement.getAttribute("defaultValue");
      Integer length = null;
      String lengthAttr = fieldElement.getAttribute("length");
      if (NumberUtils.isNumber(lengthAttr))
        length = NumberUtils.createInteger(lengthAttr);
      Integer scale = null;
      String scaleAttr = fieldElement.getAttribute("scale");
      if (NumberUtils.isNumber(scaleAttr))
        scale = NumberUtils.createInteger(scaleAttr);
      boolean unique = "true".equalsIgnoreCase(fieldElement.getAttribute("unique"));

      org.hibernate.type.Type type = parseColumnType(fieldType);
      
      if (StringUtils.isBlank(newFieldName)) {
        newFieldName = oldFieldName;
      }

      Column column = new Column(newFieldName);
      column.setNullable(fieldNullable);
      column.setUnique(unique);
      column.setValue(new ValueAdapter(table, type));

      if (!StringUtils.isBlank(defaultValue))
        column.setDefaultValue(defaultValue);
      if (length != null)
        column.setLength(length);
      if (scale != null)
        column.setScale(scale);
      
      String columnName = fieldElement.getAttribute("name");
      if (StringUtils.isBlank(columnName))
        throw new UpdaterException("Drop column: Column needs a name");
      removeTableColumn(table, columnName);
      upgradeBatch.addChangeColumn(table, oldFieldName, column);
    }

    NodeIterator keyIterator = XPathAPI.selectNodeIterator(tableElement, "dropForeignKeys/key");
    Element keyElement;
    while ((keyElement = (Element) keyIterator.nextNode()) != null) {
      String keyName = keyElement.getAttribute("name");
      if (StringUtils.isBlank(keyName))
        throw new UpdaterException("Drop foreign key: Specify name for foreign key");
      upgradeBatch.addDropForeignKey(table, keyName);
    }

    keyIterator = XPathAPI.selectNodeIterator(tableElement, "addForeignKeys/key");
    while ((keyElement = (Element) keyIterator.nextNode()) != null) {
      ForeignKey foreignKey = parseAddForeignKey(table, keyElement);
      upgradeBatch.addCreateForeignKey(foreignKey);
    }
  }
  
  private ForeignKey parseAddForeignKey(Table table, Element keyElement) {
    String keyName = keyElement.getAttribute("name");
    String referencedTableName = keyElement.getAttribute("referencedTable");
    if (StringUtils.isBlank(referencedTableName))
      throw new UpdaterException("Add foreign key: Key must specify referencedTable");

    String columnsString = keyElement.getAttribute("columns");
    if (StringUtils.isBlank(columnsString))
      throw new UpdaterException("Add foreign key: Key must specify columns");
    List<String> columnNames = Arrays.asList(columnsString.split(","));
    
    String referencedColumnsString = keyElement.getAttribute("referencedColumns");
    if (StringUtils.isBlank(referencedColumnsString))
      throw new UpdaterException("Add foreign key: Key must specify referencedColumns");
    List<String> referencedColumnNames = Arrays.asList(referencedColumnsString.split(","));
    
    Table referencedTable = new Table(referencedTableName);
    
    ForeignKey foreignKey = new ForeignKey();
    
    List<Column> columns = new ArrayList<Column>();
    for (String columnName : columnNames) {
      Column keyColumn = getTableColumn(table, StringUtils.strip(columnName));
      if (keyColumn == null) {
        keyColumn = new Column(StringUtils.strip(columnName));
      }
      foreignKey.addColumn(keyColumn);
      columns.add(keyColumn);
    }
    
    List<Column> referencedColumns = new ArrayList<Column>();
    for (String referencedColumnName : referencedColumnNames) {
      Column referencedColumn = getTableColumn(referencedTable, StringUtils.strip(referencedColumnName)); 
      if (referencedColumn == null) {
        referencedColumn = new Column(StringUtils.strip(referencedColumnName));
      }
      
      referencedColumns.add(referencedColumn);
    }
    
    if (StringUtils.isBlank(keyName)) {
      keyName = getUniqueForeignKeyName(table, columns, referencedColumns, referencedTable);
    }

    foreignKey.setTable(table);
    foreignKey.setName(keyName);
    foreignKey.setReferencedTable(referencedTable);

    foreignKey.addReferencedColumns(referencedColumns.iterator());
    
    return foreignKey;
  }
  
  private Column parseAddTableColumn(Table table, Element fieldElement) {
    String fieldName = fieldElement.getAttribute("name");
    String fieldType = fieldElement.getAttribute("type");
    boolean fieldNullable = !"false".equalsIgnoreCase(fieldElement.getAttribute("nullable"));
    String defaultValue = fieldElement.getAttribute("defaultValue");
    Integer length = null;
    String lengthAttr = fieldElement.getAttribute("length");
    if (NumberUtils.isNumber(lengthAttr))
      length = NumberUtils.createInteger(lengthAttr);
    Integer scale = null;
    String scaleAttr = fieldElement.getAttribute("scale");
    if (NumberUtils.isNumber(scaleAttr))
      scale = NumberUtils.createInteger(scaleAttr);
    boolean unique = "true".equalsIgnoreCase(fieldElement.getAttribute("unique"));
    
    org.hibernate.type.Type type = parseColumnType(fieldType);
    
    Column column = new Column(fieldName);
    column.setNullable(fieldNullable);
    column.setUnique(unique);
    column.setValue(new ValueAdapter(table, type));

    if (!StringUtils.isBlank(defaultValue))
      column.setDefaultValue(defaultValue);
    if (length != null)
      column.setLength(length);
    if (scale != null)
      column.setScale(scale);

    return column;
  }
  
  private void handleDropTableMapping(UpgradeBatch upgradeBatch, Element tableElement) {
    String tableName = tableElement.getAttribute("name");
    if (StringUtils.isBlank(tableName))
      throw new UpdaterException("Table must have a name");
    upgradeBatch.addDropTable(tableName);
  }
  
  private void handleAlterDataInsert(UpgradeBatch upgradeBatch, Element insertElement) throws TransformerException {
    String tableName = insertElement.getAttribute("table");
    if (StringUtils.isBlank(tableName))
      throw new UpdaterException("Alter data insert: insert clause need table");
    
    List<Column> columns = new ArrayList<Column>();
    List<Object> values = new ArrayList<Object>();
    
    NodeIterator valueIterator = XPathAPI.selectNodeIterator(insertElement, "value");
    Element valueElement;
    while ((valueElement = (Element) valueIterator.nextNode()) != null) {
      String columnName = valueElement.getAttribute("column");
      String valueString = valueElement.getAttribute("value");
      String fieldType = valueElement.getAttribute("type");
      Type type = parseColumnType(fieldType);
      Object value = stringToObject(type, valueString);
      
      Column column = new Column(columnName);
      column.setValue(new ValueAdapter(new Table(tableName), type));
      columns.add(column);
      values.add(value);
    }
    
    upgradeBatch.addAlterDataInsert(tableName, columns, values);
  }
  
  private void handleAlterDataUpdate(UpgradeBatch upgradeBatch, Element insertElement) throws TransformerException {
    String tableName = insertElement.getAttribute("table");
    if (StringUtils.isBlank(tableName))
      throw new UpdaterException("Alter data update: update clause need table");
    
    List<Column> updateColumns = new ArrayList<Column>();
    List<Object> updateValues = new ArrayList<Object>();
    List<Column> whereColumns = new ArrayList<Column>();
    List<String> whereOperators = new ArrayList<String>();
    List<Object> whereValues = new ArrayList<Object>();
    
    NodeIterator updateValueIterator = XPathAPI.selectNodeIterator(insertElement, "set/value");
    Element valueElement;
    while ((valueElement = (Element) updateValueIterator.nextNode()) != null) {
      String columnName = valueElement.getAttribute("column");
      String valueString = valueElement.getAttribute("value");
      String fieldType = valueElement.getAttribute("type");
      Type type = parseColumnType(fieldType);
      Object value = stringToObject(type, valueString);
      
      Column column = new Column(columnName);
      column.setValue(new ValueAdapter(new Table(tableName), type));
      updateColumns.add(column);
      updateValues.add(value);
    }
    
    NodeIterator whereEqualsIterator = XPathAPI.selectNodeIterator(insertElement, "where/equals");
    Element equalsElement;
    while ((equalsElement = (Element) whereEqualsIterator.nextNode()) != null) {
      String columnName = equalsElement.getAttribute("column");
      String valueString = equalsElement.getAttribute("value");
      String fieldType = equalsElement.getAttribute("type");
      Type type = parseColumnType(fieldType);
      Object value = stringToObject(type, valueString);
      
      Column column = new Column(columnName);
      column.setValue(new ValueAdapter(new Table(tableName), type));
      whereColumns.add(column);
      whereOperators.add("=");
      whereValues.add(value);
    }
    
    upgradeBatch.addAlterDataUpdate(tableName, updateColumns, updateValues, whereColumns, whereOperators, whereValues);
  }

  private void handleAlterDataDelete(UpgradeBatch upgradeBatch, Element deleteElement) throws TransformerException {
    String tableName = deleteElement.getAttribute("table");
    if (StringUtils.isBlank(tableName))
      throw new UpdaterException("Alter data delete: delete clause need table");
    
    List<Column> whereColumns = new ArrayList<Column>();
    List<String> whereOperators = new ArrayList<String>();
    List<Object> whereValues = new ArrayList<Object>();
    
    NodeIterator whereEqualsIterator = XPathAPI.selectNodeIterator(deleteElement, "where/equals");
    Element equalsElement;
    while ((equalsElement = (Element) whereEqualsIterator.nextNode()) != null) {
      String columnName = equalsElement.getAttribute("column");
      String valueString = equalsElement.getAttribute("value");
      String fieldType = equalsElement.getAttribute("type");
      Type type = parseColumnType(fieldType);
      Object value = stringToObject(type, valueString);
      
      Column column = new Column(columnName);
      column.setValue(new ValueAdapter(new Table(tableName), type));
      whereColumns.add(column);
      whereOperators.add("=");
      whereValues.add(value);
    }
    
    upgradeBatch.addAlterDataDelete(tableName, whereColumns, whereOperators, whereValues);
  }
  
  private Object stringToObject(Type type, String stringValue) {
    Class<?> returnedClass = type.getReturnedClass();
    
    if ("NULL".equals(stringValue))
      return null;
    
    if (returnedClass.equals(String.class))
      return stringValue;
    
    if (returnedClass.equals(Float.class))
      return NumberUtils.createFloat(stringValue);
    
    if (returnedClass.equals(Double.class))
      return NumberUtils.createDouble(stringValue);
    
    if (returnedClass.equals(Integer.class))
      return NumberUtils.createInteger(stringValue);
    
    if (returnedClass.equals(Long.class))
      return NumberUtils.createLong(stringValue);
    
    if (returnedClass.equals(Boolean.class))
      return Boolean.parseBoolean(stringValue);
    
    if (returnedClass.equals(Date.class)) {
      if ("NOW".equals(stringValue))
        return new Date();
      return new Date(NumberUtils.createLong(stringValue));
    }
    
    throw new RuntimeException("Unsupported type " + type.getReturnedClass());
  }
  
  private org.hibernate.type.Type parseColumnType(String fieldType) {
    if (fieldType.startsWith("Types.")) {
      String typeName = fieldType.split("\\.")[1];
      try {
        Field field = Types.class.getField(typeName);
        int sqlType = field.getInt(null);
        String hibernateType = getDialect().getHibernateTypeName(sqlType);
        return TypeFactory.basic(hibernateType);
      } catch (SecurityException e) {
        throw new UpdaterException(e);
      } catch (NoSuchFieldException e) {
        throw new UpdaterException(e);
      } catch (IllegalArgumentException e) {
        throw new UpdaterException(e);
      } catch (IllegalAccessException e) {
        throw new UpdaterException(e);
      }
    } else {
      return TypeFactory.basic(fieldType);
    }
  }
  
  private String getUniqueForeignKeyName(Table table, List<Column> columns, List<Column> referencedColumns, Table referencedTable) {
    int result = 0;
    
    for (Column column : columns) {
      result += column.hashCode();
    }
    
    if (referencedTable != null) {
      result += referencedTable.getName().hashCode();
    }
    
    for (Column referencedColumn : referencedColumns) {
      result += referencedColumn.hashCode();
    }
    
    return "FK" + (Integer.toHexString(table.getName().hashCode()) + Integer.toHexString(result)).toUpperCase();
  }
  
  private void removeTableColumn(Table table, String columnName) {
    Map<String, Column> columns = tableColumns.get(table);  
    if (columns != null) {
      columns.remove(columnName);
    } 
  }
  
  private void setTableColumn(Table table, Column column) {
    Map<String, Column> columns = tableColumns.get(table);  
    if (columns == null) {
      columns = new HashMap<String, Column>();
      tableColumns.put(table, columns);
    }
    
    columns.put(column.getName(), column);
  }
  
  private Column getTableColumn(Table table, String columnName) {
    Map<String, Column> columns = tableColumns.get(table);  
    if (columns == null) {
      columns = new HashMap<String, Column>();
      tableColumns.put(table, columns);
    }
    
    return columns.get(columnName);
  }
  
  private Dialect getDialect() {
    if (dialect == null) {
      try {
        dialect = settings.getDialect().newInstance();
      } catch (Exception e) {
        System.err.println("Could not initialize dialect");
        System.exit(-1);
      }
    }
    
    return dialect;
  }
  
  private Connection getConnection() throws SQLException {
    DriverManager.drivers().forEach(drv -> System.out.println("driver: " + drv.getMajorVersion()));
    
    if (connection == null) {
      connection = DriverManager.getConnection(settings.getDatabaseUrl(), settings.getDatabaseUsername(), settings.getDatabasePassword());
      connection.setAutoCommit(false);
    }
    
    return connection;
  }

  private class XMLFileFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
      return pathname.getName().endsWith(".xml");
    }
  }
  
  private Settings settings;
  private UpgradeBatch upgradeBatch = new UpgradeBatch();
  private UpdateVersion currentVersion;
  private UpdateVersion newestVersion;
  private List<UpdateFile> updateFiles;
  private Connection connection;
  private Dialect dialect;
  private Map<Table, Map<String, Column>> tableColumns = new HashMap<Table, Map<String, Column>>();

  private class ValueAdapter implements Value {

    private static final long serialVersionUID = 1L;

    public ValueAdapter(Table table, Type type) {
      this.table = table;
      this.type = type;
    }

    @Override
    public void setTypeUsingReflection(String arg0, String arg1) throws MappingException {
    }

    @Override
    public boolean isValid(Mapping arg0) throws MappingException {
      return false;
    }

    @Override
    public boolean isSimpleValue() {
      return false;
    }

    @Override
    public boolean isNullable() {
      return false;
    }

    @Override
    public boolean isAlternateUniqueKey() {
      return false;
    }

    @Override
    public boolean hasFormula() {
      return false;
    }

    @Override
    public Type getType() throws MappingException {
      return type;
    }

    @Override
    public Table getTable() {
      return table;
    }

    @Override
    public FetchMode getFetchMode() {
      return null;
    }

    @Override
    public boolean[] getColumnUpdateability() {
      return null;
    }

    @Override
    public int getColumnSpan() {
      return 0;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Iterator getColumnIterator() {
      return null;
    }

    @Override
    public boolean[] getColumnInsertability() {
      return null;
    }

    @Override
    public void createForeignKey() throws MappingException {
    }

    @Override
    public Object accept(ValueVisitor arg0) {
      return null;
    }

    private Table table;
    private Type type;
  }

}
