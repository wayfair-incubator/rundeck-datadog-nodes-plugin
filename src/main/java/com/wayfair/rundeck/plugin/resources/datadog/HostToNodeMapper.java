package com.wayfair.rundeck.plugin.resources.datadog;

import static com.wayfair.rundeck.plugin.resources.datadog.datadog.DatadogHostRequest.getDatadogHosts;

import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;
import com.google.common.annotations.VisibleForTesting;
import com.wayfair.rundeck.plugin.resources.datadog.datadog.DatadogHost;
import com.wayfair.rundeck.plugin.resources.datadog.datadog.DatadogMetaData;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HostToNodeMapper maps Datadog hosts to Rundeck Nodes. */
class HostToNodeMapper {
  private static final Logger logger = LoggerFactory.getLogger(HostToNodeMapper.class);
  private final String filterString;
  private final String appKey;
  private final String apiKey;
  private final Set<String> inputTagSet;

  /**
   * Constructs a HostToNodeMapper.
   *
   * @param filterString the filter string for hosts
   * @param appKey the Datadog APP key
   * @param apiKey the Datadog API key
   * @param inputTagSet the Datadog tags to map to nodes
   */
  HostToNodeMapper(String filterString, String appKey, String apiKey, Set<String> inputTagSet) {
    this.filterString = filterString;
    this.appKey = appKey;
    this.apiKey = apiKey;
    this.inputTagSet = inputTagSet;
  }

  /**
   * Maps host properties to Rundeck node attributes.
   *
   * @param hosts Datadog hosts
   * @param inputTagSet Datadog tags
   */
  @VisibleForTesting
  static NodeSetImpl mapHosts(Set<DatadogHost> hosts, Set<String> inputTagSet) {
    final NodeSetImpl nodeSet = new NodeSetImpl();

    for (DatadogHost host : hosts) {
      final NodeEntryImpl node = new NodeEntryImpl();

      if (inputTagSet.size() > 0) {
        Set<String> datadogTags =
            host.getDatadogTags().stream()
                .filter(tag -> inputTagSet.contains(tag.split(":")[0]))
                .collect(Collectors.toSet());

        node.setTags(datadogTags);

        logger.debug("[Datadog Node Source] Tags: " + datadogTags);
      }

      // Set the node and host name to the fqdn format
      String fqdn = host.getHostName();

      node.setNodename(fqdn);
      node.setHostname(fqdn);

      DatadogMetaData metaData = host.getMetaData();
      String osPlatform = "";

      if (metaData != null && metaData.getOperatingSystemPlatform() != null) {
        osPlatform = metaData.getOperatingSystemPlatform().toLowerCase().trim();
      } else {
        // Check for any Datadog tags for operatingsystem if there is no platform metadata
        // This can happen with certain nodes from integrations like vSphere. The Datadog agent
        // will generally add this tag though.
        for (String tag : host.getDatadogTags()) {
          if (tag.toLowerCase().startsWith("operatingsystem:")) {
            osPlatform = tag.toLowerCase().trim().split(":")[1];
          }
        }
      }

      // Rundeck needs this information so we need to discard the host if it's missing
      if ("".equals(osPlatform)) {
        logger.warn("Could not determine an OS for the following host: " + fqdn);
        continue;
      }

      // OS Family is important for Rundeck's selection of node executors
      node.setOsFamily(osPlatform.contains("win") ? "windows" : "unix");

      // Grab the metadata if available for more detailed OS information
      if (metaData != null && !metaData.isEmpty()) {
        if ((metaData.getUnixVersionList() != null && metaData.getUnixVersionList().size() >= 2)
            && (metaData.getWindowsVersionList() != null
            && metaData.getWindowsVersionList().size() >= 2)) {
          String osVersion =
              "windows".equals(node.getOsFamily())
                  ? host.getMetaData().getWindowsVersionList().get(1)
                  : host.getMetaData().getUnixVersionList().get(1);

          if (!"".equals(osVersion)) {
            node.setOsVersion(osVersion);
          }

          String osName =
              "windows".equals(node.getOsFamily())
                  ? metaData.getWindowsVersionList().get(0)
                  : metaData.getUnixVersionList().get(0);

          if (!"".equals(osName)) {
            node.setOsName(osName);
          }
        }
      }

      if (node.getOsName() == null || "".equals(node.getOsName())) {
        node.setOsName(osPlatform);
      }

      node.setAttribute("isMuted", Boolean.toString(host.isMuted()));
      node.setAttribute("sources", host.getSources().toString());

      logger.debug("Adding node: " + node.getNodename());
      nodeSet.putNode(node);
    }

    return nodeSet;
  }

  /**
   * Retrieves hosts from Datadog and maps them to Rundeck nodes.
   *
   * @return a set of Rundeck nodes
   * @throws ResourceModelSourceException Rundeck resource model exception
   */
  NodeSetImpl performQuery() throws ResourceModelSourceException {
    Set<DatadogHost> hostSet;

    logger.info("[Datadog Node Source] Begin - Getting Datadog hosts");
    try {
      hostSet = getDatadogHosts(filterString, appKey, apiKey);
    } catch (IOException | ExecutionException | TimeoutException | InterruptedException ex) {
      throw new ResourceModelSourceException(
          "Exception while fetching Datadog hosts: " + ex.toString(), ex);
    }

    if (hostSet.isEmpty()) {
      logger.info("[Datadog Node Source] No hosts were returned from Datadog");
    }

    NodeSetImpl nodeSet = mapHosts(hostSet, inputTagSet);

    logger.info(
        "[Datadog Node Source] End - Returning " + nodeSet.getNodes().size() + " Datadog hosts");

    return nodeSet;
  }
}