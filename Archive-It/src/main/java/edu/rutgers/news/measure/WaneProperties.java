package edu.rutgers.news.measure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.commons.validator.routines.UrlValidator;

public class WaneProperties {

	public static final String ACCCOUNT_ID_PARAM_NAME = "accountId";
	public static final String AUTH_URL_PARAM_NAME = "authurl";
	public static final String BASE_URL_PARAM_NAME = "baseurl";
	public static final String CHECKSUM_ALGORITHM_PARAM_NAME = "checksumAlgorithm";
	public static final String COLLECTION_ID_PARAM_NAME = "collectionId";
	public static final String CRAWL_ID_PARAM_NAME = "crawlId";
	public static final String CRAWL_ID_LOWER_BOUND_PARAM_NAME = "crawlIdLowerBound";
	public static final String CRAWL_START_AFTER_PARAM_NAME = "crawlStartAfter";
	public static final String CRAWL_START_BEFORE_PARAM_NAME = "crawlStartBefore";
	public static final String FILENAME_PARAM_NAME = "filename";
	public static final String PASSWORD_PARAM_NAME = "password";
	public static final String RETRIES_PARAM_NAME = "retries";
	public static final String USERNAME_PARAM_NAME = "username";
	
	public static final String TEMP_WANE_FILE_DIR = "tempWaneGZFileDownloadDir";
	public static final String TEMP_WANE_EXTRACT_DIR = "tempWaneFileExtractDir";
	public static final String WANE_FILE_DELETE_FLAG = "deletewanefiles";
	public static final String ZIP_FILE_DELETE_FLAG = "deletewaneGZfiles";

	private static final String OUTPUT_CSV_FILE_PATH = "outputCSVFile";
	
	private static final String EXCLUDE_LOC_STR = "excludeInvalidLocationStr";
	
	private static final String LOCATION_FREQUENCY_CUT_POINT = "locationFrequencyCutPoint";

	private String settingsFileLocation = "./config/settings.properties";
	
	private static final String BASE_COMMUNITY_FILE = "communityExcelFile";
	private static final String WANE_DOWNLOADED_MANUALLY = "manuallyDownloadWaneFlag";
	
	protected PrintStream errStream = System.err;
	protected Properties settings;

	protected Map<String,String> cityMapping;
	protected String[] excludeLocStr;

	protected WaneProperties() {} 

	public void setErrStream(PrintStream errStream) {
		this.errStream = errStream;
	}

	public String accountId() {
		return settings.getProperty(ACCCOUNT_ID_PARAM_NAME);
	}

	public String authUrlString() {
		return settings.getProperty(AUTH_URL_PARAM_NAME);
	}

	public String baseUrlString() {
		return settings.getProperty(BASE_URL_PARAM_NAME);
	}

	public String checksumAlgorithm() {
		return settings.getProperty(CHECKSUM_ALGORITHM_PARAM_NAME);
	}

	public String collectionId() {
		return settings.getProperty(COLLECTION_ID_PARAM_NAME);
	}

	public String crawlId() {
		return settings.getProperty(CRAWL_ID_PARAM_NAME);
	}

	public String crawlIdLowerBound() {
		return settings.getProperty(CRAWL_ID_LOWER_BOUND_PARAM_NAME);
	}

	// e.g. 2014-01-01, see
	// https://github.com/WASAPI-Community/data-transfer-apis/tree/master/ait-reference-specification#paths--examples
	public String crawlStartAfter() {
		return settings.getProperty(CRAWL_START_AFTER_PARAM_NAME);
	}

	// e.g. 2014-01-01, see
	// https://github.com/WASAPI-Community/data-transfer-apis/tree/master/ait-reference-specification#paths--examples
	public String crawlStartBefore() {
		return settings.getProperty(CRAWL_START_BEFORE_PARAM_NAME);
	}

	public String filename() {
		return settings.getProperty(FILENAME_PARAM_NAME);
	}

	public String password() {
		return settings.getProperty(PASSWORD_PARAM_NAME);
	}

	public String retries() {
		return settings.getProperty(RETRIES_PARAM_NAME);
	}

	public String username() {
		return settings.getProperty(USERNAME_PARAM_NAME);
	}

	protected void validateSettings() throws WaneSettingsException {
		List<String> errMessages = getSettingsErrorMessages();
		if (errMessages.size() > 0) {
			StringBuilder buf = new StringBuilder("Invalid settings state:\n");
			for (String errMsg : errMessages) {
				buf.append("  ");
				buf.append(errMsg);
				buf.append(".\n");
			}
			throw new WaneSettingsException(buf.toString());
		}
	}

	protected List<String> getSettingsErrorMessages() {
		String[] schemes = { "https" };
		UrlValidator urlValidator = new UrlValidator(schemes);
		IntegerValidator intValidator = new IntegerValidator();
		List<String> errMessages = new LinkedList<String>();

		if (isNullOrEmpty(getWaneFileDownloadDir()) || !isDirWritable(getWaneFileDownloadDir()))
			errMessages.add(TEMP_WANE_FILE_DIR + " is required, (and must be an extant, writable directory)");

		if (isNullOrEmpty(getWaneFileExtractDir()) || !isDirWritable(getWaneFileExtractDir()))
			errMessages.add(TEMP_WANE_EXTRACT_DIR + " is required, (and must be an extant, writable directory)");

		if (isNullOrEmpty(getCSVOutputFile()))
			errMessages.add(OUTPUT_CSV_FILE_PATH + " is required.");

		if (isNullOrEmpty(getCommunityFile()))
			errMessages.add(BASE_COMMUNITY_FILE + " is required.");

		
		// required only if files are require to download from server
		if(!isDownloadedManually()) {
			if (isNullOrEmpty(baseUrlString()) || !urlValidator.isValid(baseUrlString()))
				errMessages.add(BASE_URL_PARAM_NAME + " is required, and must be a valid URL");
			/*if (isNullOrEmpty(authUrlString()) || !urlValidator.isValid(authUrlString()))
				errMessages.add(AUTH_URL_PARAM_NAME + " is required, and must be a valid URL");
			if (isNullOrEmpty(username()))
				errMessages.add(USERNAME_PARAM_NAME + " is required");
			if (isNullOrEmpty(password()))
				errMessages.add(PASSWORD_PARAM_NAME + " is required");*/
			if (isNullOrEmpty(checksumAlgorithm()) || !("md5".equals(checksumAlgorithm())
					|| "sha1".equals(checksumAlgorithm())))
				errMessages.add(CHECKSUM_ALGORITHM_PARAM_NAME + " is required and must be md5 or sha1");
			if (isNullOrEmpty(retries()) || !intValidator.isValid(retries())
					|| !intValidator.minValue(Integer.valueOf(retries()), 0))
				errMessages.add(RETRIES_PARAM_NAME + " is required and must be an integer >= 0");
	
			// optional, validate if specified
			if (!isNullOrEmpty(accountId()) && !intValidator.isValid(accountId()))
				errMessages.add(ACCCOUNT_ID_PARAM_NAME + " must be an integer (if specified)");
			if (!isNullOrEmpty(collectionId()) && !intValidator.isValid(collectionId()))
				errMessages.add(COLLECTION_ID_PARAM_NAME + " must be an integer (if specified)");
			if (!isNullOrEmpty(crawlId()) && !intValidator.isValid(crawlId()))
				errMessages.add(CRAWL_ID_PARAM_NAME + " must be an integer (if specified)");
			if (!isNullOrEmpty(crawlStartBefore()) && !normalizeIso8601Setting(CRAWL_START_BEFORE_PARAM_NAME))
				errMessages.add(CRAWL_START_BEFORE_PARAM_NAME + " must be a valid ISO 8601 date string (if specified)");
			if (!isNullOrEmpty(crawlStartAfter()) && !normalizeIso8601Setting(CRAWL_START_AFTER_PARAM_NAME))
				errMessages.add(CRAWL_START_AFTER_PARAM_NAME + " must be a valid ISO 8601 date string (if specified)");
			if (!isNullOrEmpty(crawlIdLowerBound()) && !intValidator.isValid(crawlIdLowerBound()))
				errMessages.add(CRAWL_ID_LOWER_BOUND_PARAM_NAME + " must be an integer (if specified)");
		}
		return errMessages;
	}

	protected static boolean isNullOrEmpty(String str) {
		return str == null || str.isEmpty();
	}

	protected static boolean isDirWritable(String dirPath) {
		File outputBaseDirFile = new File(dirPath);
		return outputBaseDirFile.exists() && outputBaseDirFile.isDirectory() && outputBaseDirFile.canWrite();
	}

	/**
	 * Attempts to normalize an ISO 8601 date string setting to the format accepted
	 * by the WASAPI endpoint. Prints a warning if normalization results in a change
	 * to the setting value.
	 *
	 * @param settingName
	 *            the name of the setting field that contains the date string. you
	 *            should use a _PARAM_NAME constant.
	 * @return boolean indicating whether the setting string was successfully parsed
	 *         and normalized (true if successful, false otherwise)
	 */
	protected boolean normalizeIso8601Setting(String settingName) {
		try {
			String rawDateStr = settings.getProperty(settingName);
			String normalizedDateStr = normalizeIso8601StringForEndpoint(rawDateStr);
			if (!rawDateStr.equals(normalizedDateStr)) {
				settings.setProperty(settingName, normalizedDateStr);
				errStream.println("Normalized " + settingName + " to " + normalizedDateStr + " from " + rawDateStr
						+ " (possible loss in time precision)");
			}
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/**
	 * Takes an ISO 8601 date string, and normalizes it to a format acceptable by
	 * the WASAPI endpoint. As of this writing, the WASAPI endpoint only accepts
	 * dates of the format yyyy-MM-dd. Accepting any ISO 8601 date and normalizing
	 * it makes parsing (and testing of parsing) easier.
	 *
	 * @param settingName
	 *            the name of the setting field that contains the date string. you
	 *            should use one of the _PARAM_NAME constants.
	 * @return a boolean indicating whether the setting string was successfully
	 *         parsed and normalized (true if successful, false otherwise)
	 */
	private static String normalizeIso8601StringForEndpoint(String rawDateStr) throws IllegalArgumentException {
		Calendar cal = DatatypeConverter.parseDateTime(rawDateStr);
		SimpleDateFormat wasapiFormat = new SimpleDateFormat("yyyy-MM-dd");
		return wasapiFormat.format(cal.getTime());
	}


	public WaneProperties(String[] args) throws WaneSettingsException {
		if (args != null && args.length > 0) {
			settingsFileLocation = args[0];
		}
		loadPropertiesFile();
		validateSettings();
	}

	public int getRetries() {
		return Integer.parseInt(settings.getProperty(RETRIES_PARAM_NAME));
	}

	private void loadPropertiesFile() throws WaneSettingsException {
		if (settings == null) {
			InputStream input = null;
			try {
				input = new FileInputStream(settingsFileLocation);
				settings = new Properties();
				settings.load(input);
			} catch (FileNotFoundException e) {
				errStream.println("Properties file not found. Invalid file :" + settingsFileLocation);
				throw new WaneSettingsException(e.getMessage(),e);
			} catch (IOException e) {
				errStream.println("Issues while loading Properties file :" + settingsFileLocation);
				throw new WaneSettingsException(e.getMessage(),e);
			} finally {
				Utility.closeStream(input);
			}
		} else {
			errStream.println("Properties already loaded from " + settingsFileLocation);
		}
	}

	public String getWaneFileDownloadDir() {
		return settings.getProperty(TEMP_WANE_FILE_DIR);
	}

	public String getWaneFileExtractDir() {
		return settings.getProperty(TEMP_WANE_EXTRACT_DIR);
	}

	public boolean deleteWaneFileFlag() {
		if (Utility.isNotNullAndBlank(settings.getProperty(WANE_FILE_DELETE_FLAG))
				&& settings.getProperty(WANE_FILE_DELETE_FLAG).equalsIgnoreCase(Constants.yes)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean deleteZipFileFlag() {
		if (Utility.isNotNullAndBlank(settings.getProperty(ZIP_FILE_DELETE_FLAG))
				&& settings.getProperty(ZIP_FILE_DELETE_FLAG).equalsIgnoreCase(Constants.yes)) {
			return true;
		} else {
			return false;
		}
	}

	public String getCSVOutputFile() {
		return settings.getProperty(OUTPUT_CSV_FILE_PATH);
	}

	public String getLocationFrequencyCutPoint() {
		return settings.getProperty(LOCATION_FREQUENCY_CUT_POINT);
	}

	public String getCommunityFile() {
		return settings.getProperty(BASE_COMMUNITY_FILE);
	}

	
	public Map<String, String> getCityMapping() {
		if (cityMapping == null) {
			cityMapping = new HashMap<String, String>();
			File file = new File(settings.getProperty("cityMappingFile"));
			if (file.exists()) {
				Scanner scanner;
				try {
					scanner = new Scanner(file);
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine().trim();
						if (!line.startsWith("#") && line.length() > 0 && line.indexOf("=") != -1) {
							String[] cityMapArr = line.split("=");
							cityMapping.put(cityMapArr[0], cityMapArr[1]);
						}
					}
					scanner.close();
				} catch (FileNotFoundException e) {
					errStream.println("Properties file not found. Invalid file :" + file.getAbsolutePath());
					e.printStackTrace();
				} catch (Exception e) {
					errStream.println("Excpetion Occurred while reading :" + file.getAbsolutePath() + " . Process will continue excluding this part.");
					e.printStackTrace();
				}
			}
		}
		return cityMapping;
	}

	public String[] getLocationExclusion() {
		if (excludeLocStr == null) {
			excludeLocStr = settings.getProperty(EXCLUDE_LOC_STR).split(Constants.comma);
		}
		return excludeLocStr;
	}

	public boolean isDownloadedManually() {
		return Constants.yes.equalsIgnoreCase(settings.getProperty(WANE_DOWNLOADED_MANUALLY)) ? true : false;
	}

}
