package edu.rutgers.news.measure;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;

public class WasapiConnection {
  private WaneHttpClient waneHttpClient;

  public WasapiConnection(WaneHttpClient waneHttpClient) throws IOException {
    this.waneHttpClient = waneHttpClient;
    // Login to IA- NMRP
    this.waneHttpClient.login();
  }


  /**
   * @return null when requestURL is null (for callers that just page through responses' "next" links)
   */
  public WaneApiResponse jsonQuery(String requestURL) throws IOException {
    if (requestURL == null)
      return null;

    HttpGet jsonRequest = new HttpGet(requestURL);
    return waneHttpClient.execute(jsonRequest, new JsonResponseHandler());
  }

  // requestURL - result job token URL
  public List<WaneApiResponse> pagedJsonQuery(String requestURL) throws IOException {
    List<WaneApiResponse> wasapiRespList = new LinkedList<WaneApiResponse>();

    WaneApiResponse wasapiResp = jsonQuery(requestURL);
    while(wasapiResp != null) {
      wasapiRespList.add(wasapiResp);
      wasapiResp = jsonQuery(wasapiResp.getNext());
    }

    return wasapiRespList;
  }

  public Boolean downloadQuery(String downloadURL, final String outputPath)
      throws ClientProtocolException, HttpResponseException, IOException {
    HttpGet fileRequest = new HttpGet(downloadURL);
    return waneHttpClient.execute(fileRequest, new DownloadResponseHandler(outputPath));
  }


  public void close() throws IOException {
    waneHttpClient.close();
  }
}
