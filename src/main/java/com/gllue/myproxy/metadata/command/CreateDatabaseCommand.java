package com.gllue.myproxy.metadata.command;

import com.gllue.myproxy.metadata.command.context.CommandExecutionContext;
import com.gllue.myproxy.metadata.model.DatabaseMetaData;
import com.gllue.myproxy.metadata.model.MultiDatabasesMetaData;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CreateDatabaseCommand extends AbstractMetaDataCommand<MultiDatabasesMetaData> {
  private final String datasource;
  private final String name;

  @Override
  public void execute(CommandExecutionContext<MultiDatabasesMetaData> context) {
    var metadata = context.getRootMetaData();
    Preconditions.checkArgument(
        !metadata.hasDatabase(datasource, name), "Database already exists. [%s]", name);

    var builder = new DatabaseMetaData.Builder();
    builder.setDatasource(datasource);
    builder.setName(name);
    var database = builder.build();
    var path = getPersistPathForMetaData(context, database);
    metadata.addDatabase(database, false);
    saveMetaData(context, path, database);

    log.info("Create database. [{}]", name);
  }
}