package com.gllue.myproxy.metadata.command;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gllue.myproxy.metadata.command.AbstractTableUpdateCommand.Column;
import com.gllue.myproxy.metadata.model.ColumnType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateTableCommandTest extends BaseCommandTest {
  static final String DATASOURCE = "ds";

  @Test
  public void testCreateNewTable() {
    mockConfigurations();
    mockRootMetaData();

    var databaseName = "db";
    var tableName = "table";
    var columnName1 = "col1";
    var columnName2 = "col2";
    var columnName3 = "col3";
    var context = buildContext();
    var command =
        new CreateTableCommand(
            DATASOURCE,
            databaseName,
            tableName,
            new Column[] {
              new Column(columnName1, ColumnType.DATE, false, null),
              new Column(columnName2, ColumnType.DATE, false, null),
              new Column(columnName3, ColumnType.DATE, false, null),
            });

    var database = prepareDatabase(databaseName);
    when(rootMetaData.getDatabase(DATASOURCE, databaseName)).thenReturn(database);

    doAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              var path = (String) args[0];
              var newDatabase = bytesToDatabaseMetaData((byte[]) args[1]);
              var newTable = newDatabase.getTable(tableName);

              assertEquals(getPersistPath(database.getIdentity()), path);
              assertEquals(tableName, newTable.getName());
              assertEquals(3, newTable.getNumberOfColumns());
              return null;
            })
        .when(repository)
        .save(anyString(), any());

    command.execute(context);

    verify(repository).save(anyString(), any(byte[].class));
    verify(rootMetaData).addDatabase(any(), eq(true));
  }
}
