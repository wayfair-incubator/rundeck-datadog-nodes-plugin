package com.wayfair.rundeck.plugin.resources.datadog.datadog;

import com.google.api.client.util.Key;
import java.util.List;

/**
 * A representation of a Datadog `Hosts` endpoint response.
 *
 * <p>See <a
 * href="https://docs.datadoghq.com/api/v1/hosts/#get-all-hosts-for-your-organization">Hosts
 * Endpoint</a> for more details.
 */
public class DatadogHosts {

  /** Number of host matching the query. */
  @Key("total_matching")
  private double totalMatchingHosts;

  /** Array of hosts. */
  @Key("host_list")
  private List<DatadogHost> hostList;

  /**
   * Get the count of hosts that match the request query.
   *
   * @return the total count
   */
  double getTotalMatchingHosts() {
    return totalMatchingHosts;
  }

  /**
   * Get the host list.
   *
   * @return a list of Datadog hosts
   */
  List<DatadogHost> getHostList() {
    return hostList;
  }
}