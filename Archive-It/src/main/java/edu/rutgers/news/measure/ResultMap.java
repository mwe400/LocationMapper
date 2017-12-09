package edu.rutgers.news.measure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResultMap {

	protected Set<String> url;
	protected List<String> location;
	protected Map<String, Long> locationCount;

	public Map<String, Long> getLocationCount() {
		return locationCount;
	}

	public void setLocationCount(Map<String, Long> locationCount) {
		this.locationCount = locationCount;
	}

	public Set<String> getUrl() {
		return url;
	}

	public void setUrl(Set<String> url) {
		this.url = url;
	}

	public List<String> getLocation() {
		return location;
	}

	public void setLocation(List<String> location) {
		this.location = location;
	}

	public void generateStatistics(WaneProperties settings) {
		Iterator<String> it = location.iterator();
		locationCount = new HashMap<String, Long>();
		while (it.hasNext()) {
			String loc = validateLocation(it.next().toUpperCase(), settings);
			if (loc != null) {
				if (locationCount.get(loc) == null) {
					locationCount.put(loc, 1L);
				} else {
					locationCount.put(loc, locationCount.get(loc) + 1);
				}
			}
		}
		setLocationCount(locationCount);
	}

	private String validateLocation(String loc, WaneProperties settings) {
		for (String str : settings.getLocationExclusion()) {
			if (loc.indexOf(str) != -1) {
				return null;
			}
		}
		loc = loc.replaceAll(Constants.dot, Constants.blank);
		if (settings.getCityMapping() != null && settings.getCityMapping().get(loc) != null) {
			loc = settings.getCityMapping().get(loc);
		}
		return loc;
	}

}
