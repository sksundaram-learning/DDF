package io.ddf.jdbc;


import com.google.common.base.Strings;
import io.ddf.content.Schema;
import io.ddf.datasource.JDBCDataSourceDescriptor;
import io.ddf.DDF;
import io.ddf.DDFManager;
import io.ddf.exception.DDFException;
import io.ddf.misc.Config.ConfigConstant;
import io.ddf.util.ConfigHandler;
import io.ddf.util.IHandleConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The DDF manager for JDBC connector
 *
 * DDF can be created directly on each table on the JDBC connector. We call these DDFs: <b>direct DDF</b>.
 * <br><br>
 *
 * If <code>autocreate = true</code>, a DDF will be generated automatically for each table,
 * else DDF will be created through <code>createDDF(String tableName)</code>.<br><br>
 *
 * Note that <code>ddf@jdbc</code> still follow the general rules of DDF on using SQLHandler.
 * That is a sql query that does not specify source will by default only apply to DDFs not
 * the underlying JDBC database. <br><br>
 *
 * <i>Creating (temporary) DDFs</i>: to avoid polluting the original database, newly <b>created DDF</b> will be stored in a separate
 * database, which is specified by <code>ddf.jdbc.created.ddf.database</code>. This database is hidden from users of DDF.
 * Note that, from user point of view, the <b>created DDF</b> are the same as the direct DDF in the sense
 * that they can query both of them in the same query (e.g. join, where clause etc.)<br><br>
 *
 * TODO (by priority):
 * 1. create direct DDF (automatically or not)
 * 2.
 */
public class JDBCDDFManager extends DDFManager {

  private JDBCDataSourceDescriptor mJdbcDataSource;
  private Connection conn;
  private static IHandleConfig sConfigHandler;

  static {
    String configFileName = System.getenv(ConfigConstant.DDF_INI_ENV_VAR.toString());
    if (Strings.isNullOrEmpty(configFileName)) configFileName = ConfigConstant.DDF_INI_FILE_NAME.toString();
    sConfigHandler = new ConfigHandler(ConfigConstant.DDF_CONFIG_DIR.toString(), configFileName);
  }

  public JDBCDDFManager(JDBCDataSourceDescriptor jdbcDataSource) throws SQLException, ClassNotFoundException {

    /*
     * Register driver for the JDBC connector
     */
    String driver = sConfigHandler.getValue(this.getEngine(), ConfigConstant.JDBC_DRIVER.toString());

    Class.forName(driver);

    mJdbcDataSource = jdbcDataSource;
    conn = DriverManager.getConnection(mJdbcDataSource.getDataSourceUri().toString(),
        mJdbcDataSource.getCredentials().getUserName(),
        mJdbcDataSource.getCredentials().getPassword());

    boolean isDDFAutoCreate = Boolean.parseBoolean(sConfigHandler.getValue(ConfigConstant.ENGINE_NAME_JDBC.toString(),
        ConfigConstant.JDBC_DDF_AUTOCREATE.toString()));

    if (isDDFAutoCreate){

    } else {

    }

  }

  /**
   * Class representing column metadata of a JDBC source
   * @TODO: refactor to make it reusable on any JDBC connector
   */
  public class ColumnSchema {

    private String name;
    private Integer colType;

    /*
      Since atm the following variables are not used programmatically,
      I keep it as string to avoid multiple type conversions between layers.
       Note: the output from the JDBC connector is string
     */
    private String isNullable;
    private String isAutoIncrement;
    private String isGenerated;

    public ColumnSchema(String name, Integer colType, String isNullable, String isAutoIncrement, String isGenerated) {
      this.name = name;
      this.colType = colType;
      this.isNullable = isNullable;
      this.isAutoIncrement = isAutoIncrement;
      this.isGenerated = isGenerated;
    }

    /**
     *
     * @return DDF Column type
     */
    Schema.ColumnType getDDFType() throws DDFException {
      //TODO: review data type support
      switch(colType) {
        case Types.ARRAY: return Schema.ColumnType.ARRAY;
        case Types.BIGINT:  return Schema.ColumnType.BIGINT; 
        case Types.BINARY: return Schema.ColumnType.BINARY;
        case Types.BOOLEAN: return Schema.ColumnType.BOOLEAN;
        case Types.CHAR: return Schema.ColumnType.STRING;
        case Types.DATE: return Schema.ColumnType.DATE;
        case Types.DECIMAL: return Schema.ColumnType.DECIMAL;
        case Types.DOUBLE: return Schema.ColumnType.DOUBLE;
        case Types.FLOAT: return Schema.ColumnType.FLOAT;
        case Types.INTEGER: return Schema.ColumnType.INT;
        case Types.LONGVARCHAR: return Schema.ColumnType.STRING; //TODO: verify
        case Types.NUMERIC: return Schema.ColumnType.DECIMAL;
        case Types.NVARCHAR: return Schema.ColumnType.STRING; //TODO: verify
        case Types.SMALLINT: return Schema.ColumnType.INT;
        case Types.TIMESTAMP: return Schema.ColumnType.TIMESTAMP;
        case Types.TINYINT: return Schema.ColumnType.INT;
        case Types.VARCHAR: return Schema.ColumnType.STRING; //TODO: verify
        default: throw new DDFException(String.format("Type not support %s", JDBCUtils.getSqlTypeName(colType)));
        //TODO: complete for other types
      }
    }

    public String getName() {
      return name;
    }

    @Override public String toString() {
      return String.format("[name: %s, type: %s, isNullable: %s, isAutoIncrement: %s, isGenerated: %s]", name, colType,
          isNullable, isAutoIncrement, isGenerated);
    }
  }

  public class TableSchema extends ArrayList<ColumnSchema> {}

  @Override public DDF loadTable(String fileURL, String fieldSeparator) throws DDFException {
    throw new DDFException("Load DDF from file is not supported!");
  }

  public DDF DDF(String tableName) throws DDFException {
    return null;
  }

  @Override public DDF getOrRestoreDDFUri(String ddfURI) throws DDFException {
    return null;
  }

  @Override public DDF getOrRestoreDDF(UUID uuid) throws DDFException {
    return null;
  }

  /**
   *
   * @param
   * @return
   *
   * @TODO: refactor to make it reusable on any JDBC connector
   */
  public List<String> showTables() throws SQLException {

    //assert(conn != null, "");
    DatabaseMetaData dbMetaData = conn.getMetaData();

    ResultSet tbls = dbMetaData.getTables(null, null, null, null);
    List<String> tableList = new ArrayList<String>();

    while(tbls.next()){ tableList.add(tbls.getString("TABLE_NAME")); }
    return tableList;
  }

  /**
   * @param
   * @param tableName
   * @return
   * @throws SQLException
   * @TODO: refactor to make it reusable on any JDBC connector
   */

  public TableSchema getTableSchema(String tableName) throws SQLException {
    //assert(conn != null, "");
    DatabaseMetaData dbMetaData = conn.getMetaData();
    //assert(tableName != null, "Table name cannot be null");

    ResultSet tblSchemaResult  = dbMetaData.getColumns(null, null, tableName, null);
    TableSchema tblSchema = new TableSchema();

    while(tblSchemaResult.next()) {
      ColumnSchema colSchema  = new ColumnSchema(tblSchemaResult.getString("COLUMN_NAME"),
          tblSchemaResult.getInt("DATA_TYPE"),
          tblSchemaResult.getString("IS_NULLABLE"),
          tblSchemaResult.getString("IS_AUTOINCREMENT"),
          tblSchemaResult.getString("IS_GENERATEDCOLUMN")
      );
      tblSchema.add(colSchema);
    }
    return tblSchema;
  }

  @Override public String getEngine() {
    return ConfigConstant.ENGINE_NAME_JDBC.toString();
  }

  /**
   * @param jdbcDataSource
   * @TODO: refactor to make it reusable on any JDBC connector
   */
  public void setJdbcDataSource(JDBCDataSourceDescriptor jdbcDataSource) {
    this.mJdbcDataSource = jdbcDataSource;
  }

  /**
   * Get metadata of the table <code>tableName</code>
   * @param query SQL query string
   * @param conn Connection object
   * @return an array where each element is a list of every row in a column
   * @TODO: refactor to make it reusable on any JDBC connector
   */
  public void runQuery(String query, Connection conn) throws SQLException  {
    //    Statement stm = conn.createStatement();
    //    ResultSet rs = stm.executeQuery(query);
    //
    //    ResultSetMetaData rsMetaData = rs.getMetaData();

    /*
      Read data into array of list, each list is a column
    */
    //    val colCnt = rsMetaData.getColumnCount();
    //    var colList: Array[ListBuffer[Object]] = new Array[ListBuffer[Object]](colCnt);
    //    while(rs.next()){
    //      for (i <- 0 to colCnt-1){
    //        if (colList(i) == null) {
    //          colList(i) = new ListBuffer[Object]();
    //        }
    //        colList(i).append(rs.getObject(i+1));
    //      }
    //    }
    //    colList
  }

  /*
    Implement sql2ddf and sql
    sql(source) will be implemented in generic sql support already
    same for sql2ddf
   */
}
