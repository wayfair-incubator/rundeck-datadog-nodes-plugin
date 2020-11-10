package com.wayfair.rundeck.plugin.resources.datadog;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DatadogResourceModelSource is the entry point for Rundeck core to retrieve nodes from Datadog.
 */
public class DatadogResourceModelSource implements ResourceModelSource {
  private static final Logger logger = LoggerFactory.getLogger(DatadogResourceModelSource.class);
  private final HostToNodeMapper mapper;

  /** Constructor */
  DatadogResourceModelSource(final Properties configuration) {
    String filterString =
        configuration.getProperty(DatadogResourceModelSourceFactory.FILTER_STRING);

    logger.debug("The built filterString: " + filterString);

    String appKey = configuration.getProperty(DatadogResourceModelSourceFactory.DATADOG_APP_KEY);
    String apiKey = configuration.getProperty(DatadogResourceModelSourceFactory.DATADOG_API_KEY);

    Set<String> inputTagSet = new HashSet<>();

    String tagFilePath =
        configuration.getProperty(DatadogResourceModelSourceFactory.DATADOG_TAG_FILE_PATH);

    if (tagFilePath != null) {
      try {
        Scanner scanner = new Scanner(new FileReader(tagFilePath));
        while (scanner.hasNext()) {
          inputTagSet.add(scanner.nextLine().trim());
        }
      } catch (FileNotFoundException fnf) {
        logger.error("Exception while attempting to read the tag file: " + fnf.toString());
      }
    }

    String inputTagString =
        configuration.getProperty(DatadogResourceModelSourceFactory.DATADOG_TAG_STRING);

    if (inputTagString != null && !"".equals(inputTagString)) {
      inputTagSet.addAll(Arrays.asList(inputTagString.replaceAll("\\s", "").split(",")));
    }

    logger.debug("Creating a HostToNodeMapper");
    mapper = new HostToNodeMapper(filterString, appKey, apiKey, inputTagSet);
  }

  /**
   * Gets the nodes to return to Rundeck application.
   *
   * <p>Will throw a ResourceModelSourceException for any exceptions so Rundeck will use a cached
   * node set if there are any errors in retrieving hosts.
   *
   * @return a set of Rundeck nodes
   * @throws ResourceModelSourceException for any issues with retrieving nodes
   */
  @Override
  public INodeSet getNodes() throws ResourceModelSourceException {
    INodeSet nodes;
    try {
      nodes = mapper.performQuery();
    } catch (ResourceModelSourceException e) {
      throw new ResourceModelSourceException(
          "Exception while retrieving Datadog hosts: " + e.toString(), e);
    }

    return nodes;
  }
}