package edu.rutgers.news.measure;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WaneapiResponseParser {

  public WaneapiResponseParser() { }

  public WaneApiResponse parse(InputStream jsonData) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    WaneApiResponse responseObject = objectMapper.readValue(jsonData, WaneApiResponse.class);
    return responseObject;
  }
}
