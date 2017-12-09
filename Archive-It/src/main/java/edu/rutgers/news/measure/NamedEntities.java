package edu.rutgers.news.measure;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NamedEntities {

	@JsonProperty("locations")
	private List<String> locations;
	
	@JsonProperty("organizations")
	private List<String> organizations;
	
	@JsonProperty("persons")
	private List<String> persons;
	
	public List<String> getLocations() {
		return locations;
	}
	public void setLocations(List<String> locations) {
		if(locations != null && locations.size() == 1) {
			if(!Utility.isNotNullAndBlank(locations.get(0))) {
				locations = null;
			}
		}
		this.locations = locations;
		
	}
	public List<String> getOrganizations() {
		return organizations;
	}
	public void setOrganizations(List<String> organizations) {
		this.organizations = organizations;
	}
	public List<String> getPersons() {
		return persons;
	}
	public void setPersons(List<String> persons) {
		this.persons = persons;
	}
	
	
}
