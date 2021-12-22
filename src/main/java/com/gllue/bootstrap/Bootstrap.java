package com.gllue.bootstrap;

import com.gllue.cluster.ClusterStateInitializer;
import com.gllue.common.Initializer;
import com.gllue.common.concurrent.ThreadPoolInitializer;
import com.gllue.repository.zookeeper.ZookeeperInitializer;
import com.gllue.transport.backend.BackendServer;
import com.gllue.transport.core.service.TransportService;
import com.gllue.transport.core.service.TransportServiceInitializer;
import com.gllue.transport.frontend.FrontendServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Bootstrap {
  private final FrontendServer frontendServer = FrontendServer.getInstance();
  private final BackendServer backendServer = BackendServer.getInstance();

  private final Initializer[] initializers =
      new Initializer[] {
        new ZookeeperInitializer(),
        new TransportServiceInitializer(),
        new ThreadPoolInitializer(),
        new ClusterStateInitializer(),
        backendServer,
        frontendServer,
      };

  public ServerContext initialize() throws Exception {
    var contextBuilder = new ServerContext.Builder();
    var context = contextBuilder.build();
    for (var initializer : initializers) {
      log.info("Initialize {}...", initializer.name());
      initializer.initialize(context);
    }
    log.info("Server has been initialized.");
    return context;
  }

  public void start() throws Exception {
    frontendServer.start();
  }

  public void close() throws Exception {
    for (int i = initializers.length - 1; i >= 0; i--) {
      try {
        log.info("Closing {}...", initializers[i].name());
        initializers[i].close();
      } catch (Exception e) {
        log.error("An exception occurred during closing {}...", initializers[i].name(), e);
      }
    }
  }

  public static void main(String[] args) {
    var bootstrap = new Bootstrap();
    try {
      bootstrap.initialize();
      bootstrap.start();
    } catch (Throwable e1) {
      try {
        log.error("Closing bootstrap...", e1);
        bootstrap.close();
      } catch (Exception e2) {
        log.error("An error is occurred when closing the bootstrap.", e2);
      }
    }
  }
}
