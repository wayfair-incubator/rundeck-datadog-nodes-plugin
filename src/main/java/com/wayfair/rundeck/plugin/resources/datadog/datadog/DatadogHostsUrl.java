package com.wayfair.rundeck.plugin.resources.datadog.datadog;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.Key;

/** DatadogHostsUrl gives us a GenericUrl to map params to. */
class DatadogHostsUrl extends GenericUrl {

  /**
   * Constructs from an encoded URL.
   *
   * @param encodedUrl an encoded URL
   */
  DatadogHostsUrl(String encodedUrl) {
    super(encodedUrl);
  }

  /** Number of hosts to return. Max 1000. */
  @Key public int count;

  /** Host result to start search from. */
  @Key public int start;

  /** String to filter search results. */
  @Key public String filter;
}