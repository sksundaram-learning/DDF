package io.ddf.datasource;

import io.ddf.DDF;
import io.ddf.DDFManager;
import io.ddf.exception.DDFException;

import java.util.Map;

/**
 * A generic {@link DataSourceDescriptor} that contains the source URI and options map.
 * Intended to use as source for DDF created with {@link DDFManager#createDDF(Map)}.
 */
public class GenericDataSourceDescriptor extends DataSourceDescriptor {

  private final String sourceUri;
  private final Map<Object, Object> options;

  public GenericDataSourceDescriptor(String sourceUri, Map<Object, Object> options) {
    this.sourceUri = sourceUri;
    this.options = options;
  }

  @Override
  public DDF load(DDFManager manager) throws DDFException {
    return manager.createDDF(options);
  }

  public String getSourceUri() {
    return sourceUri;
  }

  public Map<Object, Object> getOptions() {
    return options;
  }
}