package com.gllue.command.handler.query.dml;

import com.gllue.cluster.ClusterState;
import com.gllue.command.handler.HandlerResult;
import com.gllue.command.handler.query.AbstractQueryHandler;
import com.gllue.config.Configurations;
import com.gllue.repository.PersistRepository;
import com.gllue.sql.parser.SQLParser;
import com.gllue.transport.core.service.TransportService;

public abstract class AbstractDMLHandler<Result extends HandlerResult>
    extends AbstractQueryHandler<Result> {
  protected AbstractDMLHandler(
      final PersistRepository repository,
      final Configurations configurations,
      final ClusterState clusterState,
      final TransportService transportService,
      final SQLParser sqlParser) {
    super(repository, configurations, clusterState, transportService, sqlParser);
  }
}