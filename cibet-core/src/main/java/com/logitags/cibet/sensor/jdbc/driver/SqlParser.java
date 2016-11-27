/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.sensor.jdbc.driver;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.ControlEvent;

/**
 * Parses the sql statement.
 */
public class SqlParser implements StatementVisitor {

   private Log log = LogFactory.getLog(SqlParser.class);

   private static final String NO_PRIMARYKEY = "-";

   private static Map<String, ControlEvent> controlEventMap = Collections
         .synchronizedMap(new HashMap<String, ControlEvent>());

   private static Map<String, String> targetMap = Collections
         .synchronizedMap(new HashMap<String, String>());

   /**
    * [sql statement|List[SqlParameter]]
    */
   private static Map<String, List<SqlParameter>> parameterMap = Collections
         .synchronizedMap(new HashMap<String, List<SqlParameter>>());

   private static Map<String, List<SqlParameter>> insUpdColumnsMap = Collections
         .synchronizedMap(new HashMap<String, List<SqlParameter>>());

   private static Map<String, SqlParameter> primaryKeys = Collections
         .synchronizedMap(new HashMap<String, SqlParameter>());

   private String sql;

   private Connection connection;

   private Statement statement;

   /**
    * name of the primary key column
    */
   private String primaryKeyColumn;

   public SqlParser(Connection conn, String sql) {
      if (sql == null) {
         throw new IllegalArgumentException(
               "Failed to instantiate SqlParser: Constructor parameter sql is null");
      }
      this.sql = sql;
      this.connection = conn;
   }

   private Statement getStatement() {
      if (statement == null) {
         try {
            statement = new CCJSqlParserManager().parse(new StringReader(sql));
         } catch (JSQLParserException e) {
            log.error(e.getMessage(), e);
            throw new CibetJdbcException(e.getMessage(), e);
         }
      }
      return statement;
   }

   /**
    * parses the ControlEvent from the SQL statement. Other statements than
    * INSERT, UPDATE or DELETE are not considered and return null.
    * 
    * @return INSERT, UPDATE, DELETE or null
    */
   public ControlEvent getControlEvent() {
      if (!controlEventMap.containsKey(sql)) {
         if (sql == null || sql.toLowerCase().trim().startsWith("alter")) {
            visitAlter();
         } else {
            getStatement().accept(this);
            log();
         }
      }
      return controlEventMap.get(sql);
   }

   /**
    * parses the table name from the SQL statement.
    * <p>
    * INSERT[.] INTO [schema.]tablename[ \n;][.]
    * <p>
    * DELETE[.] FROM [schema.]tablename[ \n;][.]
    * <p>
    * UPDATE[.] [schema.]tablename SET[.]
    * 
    * @return
    */
   public String getTargetType() {
      if (!targetMap.containsKey(sql)) {
         getStatement().accept(this);
         log();
      }
      return targetMap.get(sql);
   }

   public SqlParameter getPrimaryKey() {
      if (!primaryKeys.containsKey(sql)) {
         getStatement().accept(this);
         log();
      }
      return primaryKeys.get(sql);
   }

   /**
    * Returns a map of column names and values from an insert or update
    * statement.
    * 
    * @return
    */
   public List<SqlParameter> getInsertUpdateColumns() {
      if (!insUpdColumnsMap.containsKey(sql)) {
         getStatement().accept(this);
         log();
      }
      return (List<SqlParameter>) ((ArrayList) insUpdColumnsMap.get(sql))
            .clone();
   }

   public List<SqlParameter> getParameters() {
      if (!parameterMap.containsKey(sql)) {
         getStatement().accept(this);
         log();
      }
      return parameterMap.get(sql);
   }

   private void refineColumnNames() {
      if (connection == null) return;
      List<SqlParameter> list = insUpdColumnsMap.get(sql);
      if (list.get(0).getColumn().equals("?1")) {
         String tableName = getTargetType();
         log.debug("load column names of table " + tableName
               + " from resultset metadata");
         ResultSet rs = null;
         java.sql.Statement stmt = null;
         try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("select * from " + tableName
                  + " where 1 = 0");
            ResultSetMetaData md = rs.getMetaData();

            int counter = 1;
            for (SqlParameter param : list) {
               param.setColumn(md.getColumnName(counter));
               counter++;
            }

         } catch (SQLException e) {
            throw new CibetJdbcException(e.getMessage(), e);
         } finally {
            try {
               if (rs != null) rs.close();
               if (stmt != null) stmt.close();
            } catch (SQLException e) {
               log.error(e.getMessage(), e);
            }
         }
      }
   }

   private void findPrimaryKeyColumn() {
      if (primaryKeyColumn == null) {
         if (connection == null) {
            primaryKeyColumn = NO_PRIMARYKEY;
         } else {
            ResultSet rs = null;
            ResultSet mdRs = null;
            java.sql.Statement stmt = null;
            try {
               String dummy = "select * from " + getTargetType()
                     + " where 1 = 0";
               log.debug(dummy);
               stmt = connection.createStatement();
               rs = stmt.executeQuery(dummy);
               ResultSetMetaData md = rs.getMetaData();

               String catalogue = md.getCatalogName(1);
               String schema = md.getSchemaName(1);
               String tableName = md.getTableName(1);
               log.debug("retrieve primary keys for " + catalogue + "."
                     + schema + "." + tableName);

               mdRs = connection.getMetaData().getPrimaryKeys(catalogue,
                     schema, tableName);
               if (mdRs.next()) {
                  primaryKeyColumn = mdRs.getString(4);
                  if (mdRs.next()) {
                     log.debug("table has more than one primary key column");
                     primaryKeyColumn = NO_PRIMARYKEY;
                  } else {
                     log.debug("found primary key column " + primaryKeyColumn);
                  }

               } else {
                  findPrimaryKeyColumnOracle();
               }

            } catch (SQLException e) {
               throw new CibetJdbcException(e.getMessage(), e);
            } finally {
               try {
                  if (rs != null) rs.close();
                  if (mdRs != null) mdRs.close();
                  if (stmt != null) stmt.close();
               } catch (SQLException e) {
                  log.error(e.getMessage(), e);
               }

            }
         }
      }
   }

   private void findPrimaryKeyColumnOracle() {
      if (primaryKeyColumn == null) {
         if (connection == null) {
            primaryKeyColumn = NO_PRIMARYKEY;
         } else {
            try {
               log.debug(connection.getCatalog());
               DatabaseMetaData md = connection.getMetaData();
               log.debug(md.getUserName());

               String tableName = getTargetType();
               String tableCatalog = null;
               String tableSchema = null;
               ResultSet resultSet = md.getTables(null, null, "%",
                     new String[] { "TABLE" });
               while (resultSet.next()) {
                  String table = resultSet.getString(3);
                  if (table.equalsIgnoreCase(tableName)) {
                     String scheme = resultSet.getString(2);
                     if (scheme != null
                           && scheme.equalsIgnoreCase(md.getUserName())) {
                        tableName = table;
                        tableCatalog = resultSet.getString(1);
                        tableSchema = scheme;
                        break;
                     }
                  }
               }

               ResultSet rs = md.getPrimaryKeys(tableCatalog, tableSchema,
                     tableName);
               if (rs.next()) {
                  primaryKeyColumn = rs.getString(4);
                  if (rs.next()) {
                     log.debug("table has more than one primary key column");
                     primaryKeyColumn = NO_PRIMARYKEY;
                  } else {
                     log.debug("found primary key column " + primaryKeyColumn);
                  }
               } else {
                  log.debug("table has no primary key column");
                  primaryKeyColumn = NO_PRIMARYKEY;
               }

            } catch (SQLException e) {
               throw new CibetJdbcException(e.getMessage(), e);
            }
         }
      }
   }

   private void parseWhere(Expression where, int sequence) {
      log.debug("parse WHERE expression " + where);
      if (where == null) {
         log.info("No WHERE clause. Seems not to be a primary key condition");
      } else if (where instanceof AndExpression) {
         AndExpression and = (AndExpression) where;
         parseWhere(and.getLeftExpression(), sequence);
         parseWhere(and.getRightExpression(), sequence + 1);

      } else if (where instanceof BinaryExpression) {
         BinaryExpression exp = (BinaryExpression) where;
         if (!(exp.getLeftExpression() instanceof Column)) {
            if (log.isDebugEnabled()) {
               log.debug(exp.getLeftExpression().toString()
                     + exp.getStringExpression() + exp.getRightExpression()
                     + " is not a unique condition for the primary key column");
            }
            return;
         }

         List<SqlParameter> paramList = parameterMap.get(sql);
         if (paramList == null) {
            paramList = new ArrayList<SqlParameter>();
            parameterMap.put(sql, paramList);
         }

         findPrimaryKeyColumn();
         SqlExpressionParser expParser = new SqlExpressionParser();
         exp.getRightExpression().accept(expParser);
         Object value = expParser.getValue();
         String colName = ((Column) exp.getLeftExpression()).getColumnName();

         SqlParameter sqlParam = new SqlParameter(colName, value);
         if ("?".equals(value)) {
            sequence++;
            sqlParam.setSequence(sequence);
         }
         if (colName.equalsIgnoreCase(primaryKeyColumn)
               && exp instanceof EqualsTo) {
            primaryKeys.put(sql, sqlParam);
         }
         paramList.add(sqlParam);
         if (log.isDebugEnabled()) {
            log.debug("parse WHERE column " + sqlParam);
         }
      } else {
         log.info("WHERE clause does not contain a unique primary key condition.");
      }
   }

   @Override
   public synchronized void visit(Select arg0) {
      controlEventMap.put(sql, null);
      targetMap.put(sql, null);
      insUpdColumnsMap.put(sql, new ArrayList<SqlParameter>());
      parameterMap.put(sql, new LinkedList<SqlParameter>());
      emptyPrimaryKey(SqlParameterType.WHERE_PARAMETER);
   }

   @Override
   public synchronized void visit(Delete del) {
      controlEventMap.put(sql, ControlEvent.DELETE);
      targetMap.put(sql, del.getTable().getName());

      List<SqlParameter> paramList = new ArrayList<SqlParameter>();
      parameterMap.put(sql, paramList);
      List<SqlParameter> updList = new ArrayList<SqlParameter>();
      insUpdColumnsMap.put(sql, updList);

      parseWhere(del.getWhere(), 0);
      if (primaryKeys.get(sql) == null) {
         // no unique primary key condition. Not controlled
         controlEventMap.put(sql, null);
         emptyPrimaryKey(SqlParameterType.WHERE_PARAMETER);
      }
   }

   @Override
   public synchronized void visit(Update upd) {
      controlEventMap.put(sql, ControlEvent.UPDATE);
      targetMap.put(sql, upd.getTable().getName());
      findPrimaryKeyColumn();

      if (upd.getColumns().size() != upd.getExpressions().size()) {
         String err = "Failed to parse UPDATE statement "
               + sql
               + ": number of columns is not equal to number of values in SET clause";
         log.error(err);
         throw new CibetJdbcException(err);
      }

      List<SqlParameter> paramList = new ArrayList<SqlParameter>();
      parameterMap.put(sql, paramList);
      List<SqlParameter> updList = new ArrayList<SqlParameter>();
      insUpdColumnsMap.put(sql, updList);

      int sequence = 0;
      for (int i = 0; i < upd.getExpressions().size(); i++) {
         Expression exp = (Expression) upd.getExpressions().get(i);
         SqlExpressionParser expParser = new SqlExpressionParser();
         exp.accept(expParser);
         Object value = expParser.getValue();
         SqlParameter sqlParam = new SqlParameter(((Column) upd.getColumns()
               .get(i)).getColumnName(), value);
         if ("?".equals(value)) {
            sequence++;
            sqlParam.setSequence(sequence);
         }

         paramList.add(sqlParam);
         updList.add(sqlParam);
      }

      parseWhere(upd.getWhere(), sequence);
      if (primaryKeys.get(sql) == null) {
         // no unique primary key condition. Not controlled
         controlEventMap.put(sql, null);
         emptyPrimaryKey(SqlParameterType.WHERE_PARAMETER);
      }
   }

   @Override
   public synchronized void visit(Insert ins) {
      controlEventMap.put(sql, ControlEvent.INSERT);
      targetMap.put(sql, ins.getTable().getName());
      findPrimaryKeyColumn();

      List<SqlParameter> paramList = new ArrayList<SqlParameter>();
      parameterMap.put(sql, paramList);
      List<SqlParameter> updList = new ArrayList<SqlParameter>();
      insUpdColumnsMap.put(sql, updList);

      ItemsList il = ins.getItemsList();
      if (il instanceof ExpressionList) {
         ExpressionList el = (ExpressionList) il;

         if (ins.getColumns() != null
               && ins.getColumns().size() != el.getExpressions().size()) {
            String err = "Failed to parse INSERT statement "
                  + sql
                  + ": number of columns is not equal to number of values in VALUES clause";
            log.error(err);
            throw new CibetJdbcException(err);
         }

         int sequence = 0;
         for (int i = 0; i < el.getExpressions().size(); i++) {
            Expression exp = (Expression) el.getExpressions().get(i);
            SqlExpressionParser expParser = new SqlExpressionParser();
            exp.accept(expParser);
            Object value = expParser.getValue();
            String columnName = ins.getColumns() == null ? "?" + (i + 1)
                  : ((Column) ins.getColumns().get(i)).getColumnName();

            SqlParameter sqlParam = new SqlParameter(columnName, value);
            if ("?".equals(value)) {
               sequence++;
               sqlParam.setSequence(sequence);
            }
            if (sqlParam.getColumn().equalsIgnoreCase(primaryKeyColumn)) {
               primaryKeys.put(sql, sqlParam);
            }
            paramList.add(sqlParam);
            updList.add(sqlParam);
         }

         refineColumnNames();

         if (primaryKeys.get(sql) == null) {
            emptyPrimaryKey(SqlParameterType.INSERT_PARAMETER);
         }

      } else {
         log.warn("Subselects not supported in statement " + sql);
         controlEventMap.put(sql, null);
      }
   }

   private synchronized void visitAlter() {
      controlEventMap.put(sql, null);
      targetMap.put(sql, null);
      insUpdColumnsMap.put(sql, new ArrayList<SqlParameter>());
      parameterMap.put(sql, new LinkedList<SqlParameter>());
      emptyPrimaryKey(SqlParameterType.WHERE_PARAMETER);
   }

   @Override
   public synchronized void visit(Replace arg0) {
      controlEventMap.put(sql, null);
      targetMap.put(sql, null);
      insUpdColumnsMap.put(sql, new ArrayList<SqlParameter>());
      parameterMap.put(sql, new LinkedList<SqlParameter>());
      emptyPrimaryKey(SqlParameterType.WHERE_PARAMETER);
   }

   @Override
   public synchronized void visit(Drop arg0) {
      controlEventMap.put(sql, null);
      targetMap.put(sql, null);
      insUpdColumnsMap.put(sql, new ArrayList<SqlParameter>());
      parameterMap.put(sql, new LinkedList<SqlParameter>());
      emptyPrimaryKey(SqlParameterType.WHERE_PARAMETER);
   }

   @Override
   public synchronized void visit(Truncate arg0) {
      controlEventMap.put(sql, null);
      targetMap.put(sql, null);
      insUpdColumnsMap.put(sql, new ArrayList<SqlParameter>());
      parameterMap.put(sql, new LinkedList<SqlParameter>());
      emptyPrimaryKey(SqlParameterType.WHERE_PARAMETER);
   }

   @Override
   public synchronized void visit(CreateTable arg0) {
      controlEventMap.put(sql, null);
      targetMap.put(sql, null);
      insUpdColumnsMap.put(sql, new ArrayList<SqlParameter>());
      parameterMap.put(sql, new LinkedList<SqlParameter>());
      emptyPrimaryKey(SqlParameterType.WHERE_PARAMETER);
   }

   public void log() {
      if (log.isDebugEnabled()) {
         StringBuffer buf = new StringBuffer();
         buf.append("\n***************************\n");
         buf.append("PARSED SQL STATEMENT: ");
         buf.append(sql);
         buf.append("\n***************************\n");
         buf.append("CONTROL EVENT: ");
         buf.append(controlEventMap.get(sql));
         buf.append("\nTARGET: ");
         buf.append(targetMap.get(sql));
         buf.append("\nPRIMARY KEY CONSTRAINT: ");
         buf.append(primaryKeys.get(sql).getColumn());
         buf.append(" = ");
         buf.append(primaryKeys.get(sql).getValue());

         buf.append("\nPARAMETERS: ");
         List<SqlParameter> l = parameterMap.get(sql);
         for (SqlParameter par : l) {
            buf.append("\n");
            buf.append(par.getSequence() == 0 ? "-" : par.getSequence());
            buf.append(": ");
            buf.append(par.getColumn());
            buf.append(" = ");
            buf.append(par.getValue());
         }
         buf.append("\n***************************\n");
         log.debug(buf);
      }
   }

   /**
    * @return the sql
    */
   public String getSql() {
      return sql;
   }

   private void emptyPrimaryKey(SqlParameterType type) {
      SqlParameter sqlParam = new SqlParameter(primaryKeyColumn, null);
      primaryKeys.put(sql, sqlParam);
   }
}
