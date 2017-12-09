package edu.rutgers.news.measure;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WaneApiResponse {

	@JsonProperty("count")
	private int count;

	@JsonProperty("next")
	private String next;

	@JsonProperty("previous")
	private String previous;

	@JsonProperty("files")
	private WasapiFile[] files;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getPrevious() {
		return previous;
	}

	public void setPrevious(String previous) {
		this.previous = previous;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	public WasapiFile[] getFiles() {
		return files;
	}

	public void setFiles(WasapiFile[] files) {
		this.files = files;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("***** Wasapi Response Details *****\n");
		sb.append("count: " + Integer.toString(getCount()) + "\n");
		sb.append("previous: " + getPrevious() + "\n");
		sb.append("next: " + getNext() + "\n");
		sb.append("files:\n");
		for (int i = 0; i < files.length; i++) {
			sb.append("  file " + Integer.toString(i) + ":\n");
			sb.append("    " + files[i].toString());
		}
		return sb.toString();
	}
}
