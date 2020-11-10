package com.wayfair.rundeck.plugin.resources.datadog.datadog;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A representation of a Datadog Host.
 *
 * <p>See <a
 * href="https://docs.datadoghq.com/api/v1/hosts/#get-all-hosts-for-your-organization">Hosts
 * Endpoint</a> for more information.
 */
public final class DatadogHost extends GenericJson {

  /** The host name. */
  @Key("host_name")
  private String hostName;

  /** The host id. */
  @Key private long id;

  /** List of tags for each source (AWS, Datadog Agent, Chef..). */
  @Key("tags_by_source")
  private Map<String, List<String>> tags;

  /** If a host is muted or not. */
  @Key("is_muted")
  private boolean isMuted;

  /** Metadata associated with the host. */
  @Key("meta")
  private DatadogMetaData metaData;

  /** Source or cloud provider associated with the host. */
  @Key private List<String> sources;

  /**
   * Get the Datadog hostname.
   *
   * @return hostname
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Get the Datadog host id.
   *
   * @return the host id
   */
  public long getId() {
    return id;
  }

  /**
   * Get the Datadog host tags.
   *
   * <p>There can be a good amount of duplicate tags so get rid of those here.
   *
   * @return the tags
   */
  public Set<String> getDatadogTags() {
    Set<String> tagSet = new HashSet<>();

    for (Map.Entry<String, List<String>> item : tags.entrySet()) {
      tagSet.addAll(item.getValue());
    }

    return tagSet;
  }

  /**
   * Get the mute status of the the host.
   *
   * @return true if the host is muted; false otherwise
   */
  public boolean isMuted() {
    return isMuted;
  }

  /**
   * Get the host's metadata.
   *
   * @return the host's metadata
   */
  public DatadogMetaData getMetaData() {
    return metaData;
  }

  /**
   * Get the host's sources.
   *
   * @return a list of sources
   */
  public List<String> getSources() {
    return sources;
  }

  /**
   * Set the hostname.
   *
   * @param hostName the hostname
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Set the id.
   *
   * @param id the host id
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Set the host tags.
   *
   * @param tags the host tags
   */
  public void setTags(Map<String, List<String>> tags) {
    this.tags = tags;
  }

  /**
   * Set mute status.
   *
   * @param muted mute status
   */
  public void setMuted(boolean muted) {
    isMuted = muted;
  }

  /**
   * set metadata.
   *
   * @param metaData host metadata
   */
  public void setMetaData(DatadogMetaData metaData) {
    this.metaData = metaData;
  }

  /**
   * Set the source list.
   *
   * @param sources the host's sources
   */
  public void setSources(List<String> sources) {
    this.sources = sources;
  }
}