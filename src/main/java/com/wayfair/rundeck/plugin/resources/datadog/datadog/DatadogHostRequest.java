package com.wayfair.rundeck.plugin.resources.datadog.datadog;

import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs the request to the Datadog Hosts endpoint to retrieve all hosts for your organization.
 *
 * <p>Makes a request to <a
 * href="https://docs.datadoghq.com/api/v1/hosts/#get-all-hosts-for-your-organization">Datadog Host
 * Endpoint</a> to retrieve all hosts. This endpoint retrieves a maximum of 1000 hosts per page and
 * can be quite slow if your organization has many hosts. To work around this, we parallelize the
 * calls to speed things up.
 */
public class DatadogHostRequest {

  private static final Logger logger = LoggerFactory.getLogger(DatadogHostRequest.class);

  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  private static final JsonFactory JSON_FACTORY = new JacksonFactory();

  // Datadog's 'hosts' endpoint returns at MAX 1000 hosts
  private static final int HOST_COUNT_MAX = 1000;

  // Initial host result to start search at
  private static final int INITIAL_START_VALUE = 0;

  // Result timeout value in seconds
  private static final int RESULT_TIMEOUT_VALUE = 10;

  // Datadog's 'hosts' endpoint
  private static final String HOSTS_URL = "https://api.datadoghq.com/api/v1/hosts";

  // ExponentialBackOff constants

  // Initial retry interval in milliseconds
  private static final int INITIAL_RETRY_INTERVAL = 500;

  // Maximum elapsed time in milliseconds
  private static final int MAXIMUM_ELAPSED_TIME = 900000;

  // Maximum value of the back off period in milliseconds
  private static final int MAXIMUM_INTERVAL = 900000;

  // Multiplier - Multiply the current interval with for each retry attempt
  private static final double MULTIPLIER = 1.5;

  // Randomization Factor - Randomization factor to use for creating a range around the retry
  // interval
  private static final double RANDOMIZATION_FACTOR = 0.5;

  /**
   * Retrieve hosts from Datadog.
   *
   * <p>To save time, retrieves the first page of hosts and use the `total_matching` response field
   * to calculate the amount of threads to use to get the remaining hosts. Uses 1 thread per
   * request for a max of 1000 hosts. Hosts that have been active within the last 2 hours are
   * returned (documentation shows 3 hours is the default, but testing shows this to be 2).
   *
   * @param filter string to filter search results.
   * @param appKey Datadog APP Key
   * @param apiKey Datadog API Key
   * @return a list of Datadog hosts
   */
  public static Set<DatadogHost> getDatadogHosts(String filter, String appKey, String apiKey)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {

    Set<DatadogHost> allHosts = new HashSet<>();

    /*
     *  Make one call to DD hosts endpoint to get the first HOST_COUNT_MAX group and to get
     *  a total matching host count that will be used to calculate the amount of threads
     *  necessary to retrieve the rest
     */
    HttpRequest request = buildRequest(filter, INITIAL_START_VALUE, appKey, apiKey);

    DatadogHosts hostsResponse = request.execute().parseAs(DatadogHosts.class);

    if (hostsResponse.getHostList().size() == 0) {
      return allHosts;
    }

    allHosts.addAll(hostsResponse.getHostList());

    double totalMatching = hostsResponse.getTotalMatchingHosts();

    // Now that we have the first set of hosts, get the rest in parallel
    if (totalMatching != 0 && totalMatching > HOST_COUNT_MAX) {

      int threads = (int) Math.floor(totalMatching / HOST_COUNT_MAX);
      logger.debug("Attempting to run with " + threads + " threads.");

      ExecutorService executorService = Executors.newFixedThreadPool(threads);

      List<Future<HttpResponse>> futureList = new ArrayList<>();

      int startAt = HOST_COUNT_MAX;

      for (int i = 0; i < threads; i++) {
        HttpRequest hostRequest = buildRequest(filter, startAt, appKey, apiKey);
        startAt += HOST_COUNT_MAX;

        Future<HttpResponse> responseFuture = hostRequest.executeAsync(executorService);

        futureList.add(responseFuture);
      }

      /* Get the results from the futures and check for cancellations. The get method will
       * block and should allow time for all requests to complete
       */
      logger.debug("Getting the results from the Futures");
      for (Future<HttpResponse> result : futureList) {
        // Will throw an exception if the timeout is reached and fail the import
        // Node cache will be used instead
        DatadogHosts hosts =
            result.get(RESULT_TIMEOUT_VALUE, TimeUnit.SECONDS).parseAs(DatadogHosts.class);
        if (hosts != null) {
          allHosts.addAll(hosts.getHostList());
        }
      }

      executorService.shutdown();
    }

    return allHosts;
  }

  /**
   * Make the request to the hosts endpoint.
   *
   * @param filter Datadog filter string
   * @param start the host index to start the request from
   * @param appKey the Datadog application key
   * @param apiKey the Datadog api key
   * @return an HttpRequest
   * @throws IOException if there is a problem building the GET request
   */
  private static HttpRequest buildRequest(String filter, int start, String appKey, String apiKey)
      throws IOException {
    HttpRequestFactory requestFactory =
        HTTP_TRANSPORT.createRequestFactory(
            (HttpRequest request) -> {
              request.setParser(new JsonObjectParser(JSON_FACTORY));
            });

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType("application/json");
    headers.set("DD-APPLICATION-KEY", appKey);
    headers.set("DD-API-KEY", apiKey);

    ExponentialBackOff backoff =
        new ExponentialBackOff.Builder()
            .setInitialIntervalMillis(INITIAL_RETRY_INTERVAL)
            .setMaxElapsedTimeMillis(MAXIMUM_ELAPSED_TIME)
            .setMaxIntervalMillis(MAXIMUM_INTERVAL)
            .setMultiplier(MULTIPLIER)
            .setRandomizationFactor(RANDOMIZATION_FACTOR)
            .build();

    HttpBackOffUnsuccessfulResponseHandler backOffHandler =
        new HttpBackOffUnsuccessfulResponseHandler(backoff);

    DatadogHostsUrl hostsUrl = new DatadogHostsUrl(HOSTS_URL);
    hostsUrl.filter = filter;
    hostsUrl.count = HOST_COUNT_MAX;
    hostsUrl.start = start;

    HttpRequest hostsRequest = requestFactory.buildGetRequest(hostsUrl);
    hostsRequest.setHeaders(headers);
    hostsRequest.setUnsuccessfulResponseHandler(backOffHandler);

    return hostsRequest;
  }
}