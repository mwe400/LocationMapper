package edu.rutgers.news.measure;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WaneJsonToJava {

	@JsonProperty("url")
	private String url;
	
	@JsonProperty("timestamp")
	private String timestamp;
	
	@JsonProperty("named_entities")
	private NamedEntities namedEntities;
	
	@JsonProperty("digest")
	private String digest;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = Utility.procesURL(url);
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getDigest() {
		return digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public NamedEntities getNamedEntities() {
		return namedEntities;
	}

	public void setNamedEntities(NamedEntities namedEntities) {
		this.namedEntities = namedEntities;
	}

}
