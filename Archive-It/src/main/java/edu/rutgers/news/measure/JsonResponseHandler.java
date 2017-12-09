package edu.rutgers.news.measure;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;

public class JsonResponseHandler implements ResponseHandler<WaneApiResponse> {

  public WaneApiResponse handleResponse(final HttpResponse response)
      throws ClientProtocolException, HttpResponseException, IOException {
    HttpEntity entity = response.getEntity();
    if (WasapiValidator.validateResponse(response.getStatusLine(), entity == null))
      return new WaneapiResponseParser().parse(entity.getContent());
    else return null;
  }
}
