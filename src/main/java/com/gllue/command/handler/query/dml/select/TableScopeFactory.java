package com.gllue.command.handler.query.dml.select;

import static com.gllue.common.util.SQLStatementUtils.getSchema;
import static com.gllue.common.util.SQLStatementUtils.getTableName;
import static com.gllue.common.util.SQLStatementUtils.unquoteName;

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.gllue.common.exception.NoDatabaseException;
import com.gllue.metadata.model.DatabaseMetaData;
import com.gllue.metadata.model.MultiDatabasesMetaData;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TableScopeFactory {
  private final String datasource;
  private final String defaultDatabase;
  private final MultiDatabasesMetaData databasesMetaData;

  private void addTableToScope(TableScope scope, String schema, String tableName, String alias) {
    DatabaseMetaData database;
    if (schema == null) {
      if (defaultDatabase == null) {
        throw new NoDatabaseException();
      }
      schema = defaultDatabase;
    }

    database = databasesMetaData.getDatabase(datasource, schema);
    if (database != null) {
      var table = database.getTable(tableName);
      if (table != null) {
        if (alias == null) {
          alias = tableName;
        } else {
          alias = unquoteName(alias);
        }
        scope.addTable(schema, alias, table);
      }
    }
  }

  private void resolveTableSource(TableScope scope, SQLTableSource tableSource) {
    if (tableSource instanceof SQLExprTableSource) {
      var source = (SQLExprTableSource) tableSource;
      var tableName = getTableName(source);
      if (tableName == null) {
        throw new IllegalArgumentException(
            String.format("No table name in table source. [%s]", tableSource));
      }

      addTableToScope(scope, getSchema(source), tableName, source.getAlias());
    } else if (tableSource instanceof SQLJoinTableSource) {
      var source = (SQLJoinTableSource) tableSource;
      resolveTableSource(scope, source.getLeft());
      resolveTableSource(scope, source.getRight());
    }
  }

  public TableScope newTableScope(
      final TableScope parent, final boolean inherit, final SQLTableSource tableSource) {
    var scope = new TableScope(parent, inherit);
    resolveTableSource(scope, tableSource);
    return scope;
  }
}
