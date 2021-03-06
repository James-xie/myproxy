package com.gllue.myproxy.metadata.loader;

import com.gllue.myproxy.common.io.stream.ByteArrayStreamInput;
import com.gllue.myproxy.constant.ServerConstants;
import com.gllue.myproxy.metadata.codec.DatabaseMetaDataCodec;
import com.gllue.myproxy.metadata.codec.MetaDataCodec;
import com.gllue.myproxy.metadata.model.DatabaseMetaData;
import com.gllue.myproxy.metadata.model.MultiDatabasesMetaData;
import com.gllue.myproxy.repository.PersistRepository;

public class MultiDatabasesMetaDataLoader {
  private static final String PATH_SEPARATOR = ServerConstants.PATH_SEPARATOR;
  private final PersistRepository repository;

  public MultiDatabasesMetaDataLoader(final PersistRepository repository) {
    this.repository = repository;
  }

  public MultiDatabasesMetaData load(final String basePath) {
    var builder = new MultiDatabasesMetaData.Builder();
    if (repository.exists(basePath)) {
      var children = repository.getChildrenKeys(basePath);
      for (var child : children) {
        builder.addDatabase(buildDatabaseMetaData(basePath, child));
      }
    }
    return builder.build();
  }

  private String concatPath(final String path1, final String path2) {
    return String.join(PATH_SEPARATOR, path1, path2);
  }

  private MetaDataCodec<DatabaseMetaData> getCodec() {
    return DatabaseMetaDataCodec.getInstance();
  }

  private DatabaseMetaData buildDatabaseMetaData(String path, final String dbName) {
    path = concatPath(path, dbName);
    var data = repository.get(path);
    return getCodec().decode(new ByteArrayStreamInput(data));
  }
}
