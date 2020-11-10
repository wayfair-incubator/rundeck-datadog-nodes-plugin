package com.wayfair.rundeck.plugin.resources.datadog;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import java.io.File;
import java.util.Collections;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Factory that creates a ResourceModelSource based on a UI configuration. */
@Plugin(name = "datadog-nodes-plugin", service = "ResourceModelSource")
public class DatadogResourceModelSourceFactory implements ResourceModelSourceFactory, Describable {
  private static final Logger logger =
      LoggerFactory.getLogger(DatadogResourceModelSourceFactory.class);
  private static final String PROVIDER_NAME = "datadog-nodes-plugin";
  static final String FILTER_STRING = "filter";
  static final String DATADOG_APP_KEY = "datadog_app_key";
  static final String DATADOG_API_KEY = "datadog_api_key";
  static final String DATADOG_TAG_FILE_PATH = "datadog_tag_file_path";
  static final String DATADOG_TAG_STRING = "datadog_tag_string";

  private static final Description description =
      DescriptionBuilder.builder()
          .name(PROVIDER_NAME)
          .title("Datadog Nodes Source")
          .description("Produces Nodes from Datadog Hosts")
          .property(
              PropertyUtil.string(
                  DATADOG_API_KEY,
                  "Datadog Api Key",
                  "The Datadog Api Key",
                  true,
                  null,
                  null,
                  null,
                  Collections.singletonMap(
                      StringRenderingConstants.DISPLAY_TYPE_KEY,
                      StringRenderingConstants.DisplayType.PASSWORD)))
          .property(
              PropertyUtil.string(
                  DATADOG_APP_KEY,
                  "Datadog App Key",
                  "The Datadog App Key",
                  true,
                  null,
                  null,
                  null,
                  Collections.singletonMap(
                      StringRenderingConstants.DISPLAY_TYPE_KEY,
                      StringRenderingConstants.DisplayType.PASSWORD)))
          .property(
              PropertyUtil.string(
                  FILTER_STRING,
                  "Filter",
                  "A filter string used to filter hosts. Separate filter items with a"
                      + " space. Example: `env:prod host:test datacenter:blah`. Use"
                      + " `https://app.datadoghq.com/infrastructure` to test filters",
                  false,
                  null))
          .property(
              PropertyUtil.string(
                  DATADOG_TAG_FILE_PATH,
                  "Datadog Tag File Path",
                  "Path to a file with a list of Datadog tags to map to node tags.",
                  false,
                  null,
                  file -> {
                    if (!new File(file).isFile()) {
                      throw new ValidationException("File does not exist: " + file);
                    }
                    return true;
                  }))
          .property(
              PropertyUtil.string(
                  DATADOG_TAG_STRING,
                  "Datadog Tags",
                  "A comma separated list of Datadog tags to map to nodes. These will be"
                      + " appended to the list of tags if a tag file path is given. Otherwise"
                      + " to the list of tags if a tag file path is given. Otherwise, they will"
                      + " be used on their own.",
                  false,
                  null))
          .build();

  /**
   * Creates a ResourceModelSource
   *
   * @param properties Properties passed in by Rundeck application
   * @return a DatadogResourceModelSource
   */
  public ResourceModelSource createResourceModelSource(final Properties properties) {
    logger.debug("Attempting to create a new DatadogResourceModelSource...");
    return new DatadogResourceModelSource(properties);
  }

  /**
   * Returns a Description to be used to build the UI
   *
   * @return a Description
   */
  public Description getDescription() {
    return description;
  }
}