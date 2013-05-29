package fi.internetix.updater.core;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;

public enum Database {
  
	DB2 ("DB2", "", org.hibernate.dialect.DB2Dialect.class),
  DB2400 ("DB2 AS/400", "", org.hibernate.dialect.DB2400Dialect.class),
  DB2390 ("DB2 OS390", "", org.hibernate.dialect.DB2390Dialect.class),
  PostgreSQL ("PostgreSQL", "", org.hibernate.dialect.PostgreSQLDialect.class),
  MySQL ("MySQL", "jdbc:mysql://localhost:3306/db", MySQLDialect.class),
  MySQLInnoDB ("MySQL with InnoDB", "", org.hibernate.dialect.MySQLInnoDBDialect.class),
  MySQLMyISAM ("MySQL with MyISAM", "", org.hibernate.dialect.MySQLMyISAMDialect.class),
  Oracle9i ("Oracle 9i", "", org.hibernate.dialect.Oracle9iDialect.class),
  Oracle10g ("Oracle 10g", "", org.hibernate.dialect.Oracle10gDialect.class),
  SybaseAnywhere ("Sybase Anywhere", "", org.hibernate.dialect.SybaseAnywhereDialect.class),
  SQLServer ("Microsoft SQL Server", "", org.hibernate.dialect.SQLServerDialect.class),
  Informix ("Informix", "", org.hibernate.dialect.InformixDialect.class),
  Ingres ("Ingres", "", org.hibernate.dialect.IngresDialect.class),
  Progress ("Progress", "", org.hibernate.dialect.ProgressDialect.class),
  Mckoi ("Mckoi SQL", "", org.hibernate.dialect.MckoiDialect.class),
  Interbase ("Interbase", "", org.hibernate.dialect.InterbaseDialect.class),
  Pointbase ("Pointbase", "", org.hibernate.dialect.PointbaseDialect.class),
  Firebird ("Firebird", "", org.hibernate.dialect.FirebirdDialect.class);
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  Database(String name, String defaultUrl, Class dialect) {
    this.name = name;
    this.defaultUrl = defaultUrl;
    this.dialect = dialect;
  }
  
  public Class<Dialect> getDialect() {
    return dialect;
  }
  
  public String getDefaultUrl() {
    return defaultUrl;
  }
  
  @Override
  public String toString() {
    return name;
  }
  
  private String name;
  private String defaultUrl;
  private Class<Dialect> dialect;
}
