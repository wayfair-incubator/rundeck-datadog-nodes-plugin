package com.wayfair.rundeck.plugin.resources.datadog.datadog;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import java.util.List;

/**
 * Datadog metadata associated with a host.
 *
 * <p>Unfortunately, there isn't any documentation around this data. Using this for an accurate OS
 * type.
 */
public final class DatadogMetaData extends GenericJson {
  /** The OS platform. */
  @Key("platform")
  private String operatingSystemPlatform;

  /** Array of Windows Version information. */
  @Key("winV")
  private List<String> windowsVersionList;

  /** Array of Unix version information. */
  @Key("nixV")
  private List<String> unixVersionList;

  /**
   * Get the host operating system.
   *
   * @return the operating system type
   */
  public String getOperatingSystemPlatform() {
    return operatingSystemPlatform;
  }

  /**
   * Get Windows version details.
   *
   * @return List of Windows version details
   */
  public List<String> getWindowsVersionList() {
    return windowsVersionList;
  }

  /**
   * Get Unix version details.
   *
   * @return List of Unix version details
   */
  public List<String> getUnixVersionList() {
    return unixVersionList;
  }

  public void setOperatingSystemPlatform(String operatingSystemPlatform) {
    this.operatingSystemPlatform = operatingSystemPlatform;
  }

  public void setWindowsVersionList(List<String> windowsVersionList) {
    this.windowsVersionList = windowsVersionList;
  }

  public void setUnixVersionList(List<String> unixVersionList) {
    this.unixVersionList = unixVersionList;
  }
}