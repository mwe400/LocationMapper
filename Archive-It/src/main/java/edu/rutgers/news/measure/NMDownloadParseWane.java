
package edu.rutgers.news.measure;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is primary class which contains the business logic for NMRP project.
 * 
 * @author Vaishnavi Sridhar
 *
 */
public class NMDownloadParseWane {

	protected WaneProperties settings;

	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	static Set<String> websitesToTrack = new HashSet<String>();

	// Community is key and Value is Set of URLs, (one to many mapping populating
	// while reading from Excel Sheet)
	static Map<String, Set<String>> communityURLMap = new HashMap<String, Set<String>>();

	// key is URL and value is Community (one to one mapping populating while
	// reading from Excel Sheet)
	static Map<String, String> urlCommunityMap = new HashMap<String, String>();

	static List<String> urlList = new ArrayList<String>();

	private static final char SEP = File.separatorChar;

	private WasapiConnection wasapiConn;
	
	private int unauthorizedAccessAttempt = 0;

	// Contains the final data, Key - Date, Value - Location Frequency data
	// corresponding to each County
	static Map<Date, Map<String, ResultMap>> result = new TreeMap<Date, Map<String, ResultMap>>();

	public static void main(String[] args) {
		Date currentDate = new Date();
		System.out.println("Process started :" + currentDate);
		long startTime = currentDate.getTime();

		NMDownloadParseWane parseWane = new NMDownloadParseWane();
		try {
			parseWane.executeProcess(args);
		} catch (Exception e) {
			e.printStackTrace();
		}

		long endTime = new Date().getTime();
		System.out.println("Process Execution time (Seconds) :" + (endTime - startTime) / 1000);

	}

	private void executeProcess(String[] args) throws Exception {

		// load the properties file related to this application.
		settings = new WaneProperties(args);

		// read the excel sheet and load the URLS and associated community into Java
		// object.
		readFromExcelSheet();

		if (websitesToTrack.size() > 0) {
			// download the wane files from archive it and parse them into java objects.

			if (settings.isDownloadedManually()) {
				File waneDir = new File(settings.getWaneFileDownloadDir());
				for (File fullFile : waneDir.listFiles()) {
					System.out.println(fullFile.getAbsolutePath());
					parseWanetoJson(extractFiles(fullFile.getAbsolutePath()));
				}
			} else {
				downloadandExtractWaneFiles();
			}

			// Diagnosis the data from wane files and create the final output.
			createOutputCSVFile();
		} else {
			System.out.println("No websites to track or no Source File (Excel) ");
		}
		System.out.println(result);
	}

	/**
	 * Method responsible for Login to repository and downloading wane files
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws WaneSettingsException
	 */
	void downloadandExtractWaneFiles() throws IOException, NoSuchAlgorithmException, WaneSettingsException {
		// connecting to IA of NMRP
		List<WaneApiResponse> waneapiRespList = getWasapiConn().pagedJsonQuery(settings.baseUrlString());

		if (waneapiRespList != null && waneapiRespList.get(0) != null) {
			WasapiCrawlSelector crawlSelector = new WasapiCrawlSelector(waneapiRespList);
			for (Integer crawlId : desiredCrawlIds(crawlSelector)) {
				for (WasapiFile file : crawlSelector.getFilesForCrawl(crawlId)) {
					downloadAndValidateFile(file);
				}
			}
		}
	}

	// package level method for testing
	WasapiConnection getWasapiConn() throws IOException {
		if (wasapiConn == null)
			wasapiConn = new WasapiConnection(new WaneHttpClient(settings));
		return wasapiConn;
	}

	/**
	 * Download wane.gz file and validate against the checksum defined
	 * 
	 * @param file
	 * @throws WaneSettingsException
	 * @throws NoSuchAlgorithmException
	 */
	void downloadAndValidateFile(WasapiFile file) throws WaneSettingsException, NoSuchAlgorithmException {

		String fullFilePath = prepareOutputLocation(file);
		if (fullFilePath == null) {
			System.err.println("fullFilePath is null - can't retrieve file");
			return;
		}
		int numRetries = Integer.parseInt(settings.retries());
		int attempts = 0;
		boolean checksumValidated = false;
		do {
			attempts++;
			try {
				boolean downloadSuccess = getWasapiConn().downloadQuery(file.getLocations()[0], fullFilePath);
				if (downloadSuccess && checksumValidate(settings.checksumAlgorithm(), file, fullFilePath)) {
					// System.out.println("file retrieved successfully: " + file.getLocations()[0]);
					checksumValidated = true; // break out of loop
					parseWanetoJson(extractFiles(fullFilePath));
				}
			} catch (HttpResponseException e) {
				String prefix = "ERROR: HttpResponseException (" + e.getMessage()
						+ ") downloading file (will not retry): ";
				System.err.println(prefix + file.getLocations()[0]);
				System.err.println(" HTTP ResponseCode was " + e.getStatusCode());
				if(unauthorizedAccessAttempt == 0 && e.getStatusCode() == HttpStatus.SC_UNAUTHORIZED 
						&& !Constants.blank.equals(settings.getWaneAuthURL())) {
					settings.overrideAuthURL();
					wasapiConn = null;
				} else {
					attempts = numRetries + 1; // no more attempts
				}
			} catch (ClientProtocolException e) {
				String prefix = "ERROR: ClientProtocolException (" + e.getMessage()
						+ ") downloading file (will not retry): ";
				System.err.println(prefix + file.getLocations()[0]);
				attempts = numRetries + 1; // no more attempts
			} catch (IOException e) {
				// swallow exception and try again - it may be a network issue
				System.err.println("WARNING: exception downloading file (will retry): " + file.getLocations()[0]);
				throw new WaneSettingsException(e.getMessage(), e);
			}
		} while (attempts <= numRetries && !checksumValidated);

		if (attempts == numRetries + 1) // RE-tries, not number of attempts
			System.err.println("file not retrieved or unable to validate checksum: " + file.getLocations()[0]);
	}

	// package level method for testing
	String prepareOutputLocation(WasapiFile file) {
		String outputPath = settings.getWaneFileDownloadDir() + "AIT_" + file.getCollectionId() + SEP
				+ file.getCrawlId() + SEP + file.getCrawlStartDateStr().replace(":", "_");
		new File(outputPath).mkdirs();
		return outputPath + SEP + file.getFilename();
	}

	// package level method for testing
	boolean checksumValidate(String algorithm, WasapiFile file, String fullFilePath)
			throws NoSuchAlgorithmException, IOException {
		String checksum = file.getChecksums().get(algorithm);
		if (checksum == null) {
			System.err.println("No checksum of type: " + algorithm + " available: " + file.getChecksums().toString());
			return false;
		}

		if (Constants.MD5.equalsIgnoreCase(algorithm))
			return WasapiValidator.validateMd5(checksum, fullFilePath);
		else if (Constants.SHA1.equalsIgnoreCase(algorithm))
			return WasapiValidator.validateSha1(checksum, fullFilePath);
		else {
			System.err.println("Unsupported checksum algorithm: " + algorithm + ".  Options are 'md5' or 'sha1'");
			return false;
		}
	}

	private List<Integer> desiredCrawlIds(WasapiCrawlSelector crawlSelector) {
		Integer myInteger = IntegerValidator.getInstance().validate(settings.crawlIdLowerBound());
		if (myInteger != null) {
			int crawlsAfter = myInteger.intValue();
			return crawlSelector.getSelectedCrawlIds(crawlsAfter); // returns only the wanes based on the crawl lower
																	// bound
		} else
			return crawlSelector.getSelectedCrawlIds(0); // returns all the wane files in the job token
	}

	/**
	 * Method extractFiles() take *.wane.gz file as input unzip the file and place
	 * the file in appropriate location mentioned in setings.properties
	 * 
	 * @param zipFileName
	 * @return Un-zipped File Path (.wane) - String
	 */
	private String extractFiles(String zipFileName) {
		byte[] buffer = new byte[1024];
		File inputFile = new File(zipFileName);
		GZIPInputStream gzis = null;
		FileInputStream fis = null;
		FileOutputStream outStream = null;
		String destFileName = Constants.blank;
		try {
			if (inputFile.getName().endsWith(Constants.gzFileExtension)) {
				fis = new FileInputStream(inputFile);
				gzis = new GZIPInputStream(fis);
				destFileName = settings.getWaneFileExtractDir() + File.separator + inputFile.getName();
				if (inputFile.getName().endsWith(Constants.gzFileExtension)) {
					destFileName = destFileName.substring(0, destFileName.length() - 3);
				}
				outStream = new FileOutputStream(destFileName);
				int len = 0;
				if (inputFile.getName().endsWith(Constants.gzFileExtension)) {
					while ((len = gzis.read(buffer)) > 0) {
						outStream.write(buffer, 0, len);
					}
				}
			}

		} catch (IOException e) {
			destFileName = Constants.blank;
		} finally {
			Utility.closeStream(outStream);
			Utility.closeStream(gzis);
			Utility.closeStream(fis);
			if (settings.deleteZipFileFlag()) {
				inputFile.delete();
			}
		}
		return destFileName;
	}

	/**
	 * Method parseWanetoJson() takes wane file as inputs and read line by line. It
	 * populates each line to Java Object (from json string) and store the data for
	 * further calculation
	 * 
	 * @param waneFilePath
	 * @throws WaneSettingsException
	 */
	private void parseWanetoJson(String waneFilePath) throws WaneSettingsException {

		File waneFile = new File(waneFilePath);
		if (waneFile.exists()) {
			ObjectMapper objectMapper = new ObjectMapper();

			System.out.println("Wane File - " + waneFile.getName());
			System.out.println("--------------------------------");

			FileReader fileRead = null;
			BufferedReader reader = null;
			String readLine = null;
			Set<String> waneURLSet = null;

			try {
				fileRead = new FileReader(waneFile);
				reader = new BufferedReader(fileRead);

				while ((readLine = reader.readLine()) != null) {

					WaneJsonToJava eachLineObject = objectMapper.readValue(readLine, WaneJsonToJava.class);
					List<String> locations = eachLineObject.getNamedEntities().getLocations();

					if (websitesToTrack.contains(eachLineObject.getUrl()) && locations != null) {

						String city = urlCommunityMap.get(eachLineObject.getUrl());
						Date crawllDate = null;
						try {
							crawllDate = dateFormat.parse(eachLineObject.getTimestamp().substring(0, 8));
						} catch (ParseException e) {
							// consuming date format parsing Exception, considering other records might be
							// correct and process should continue
							e.printStackTrace();
						}

						if (crawllDate != null) {
							Map<String, ResultMap> finalOutput = result.get(crawllDate);
							if (finalOutput == null) {
								finalOutput = new HashMap<String, ResultMap>();
							}
							ResultMap rMap = finalOutput.get(city);
							if (rMap == null) {
								rMap = new ResultMap();
								waneURLSet = new HashSet<String>();
								waneURLSet.add(eachLineObject.getUrl());
							} else {
								locations.addAll(rMap.getLocation());
								waneURLSet = rMap.getUrl();
								waneURLSet.add(eachLineObject.getUrl());
							}
							rMap.setLocation(locations);
							rMap.setUrl(waneURLSet);
							finalOutput.put(city, rMap);
							result.put(crawllDate, finalOutput);
						}
					}
				}

			} catch (FileNotFoundException e) {
				throw new WaneSettingsException(e.getMessage(), e);
			} catch (IOException e) {
				throw new WaneSettingsException(e.getMessage(), e);
			} finally {
				Utility.closeReader(reader);
				Utility.closeFileReader(fileRead);
			}
			if (settings.deleteWaneFileFlag()) {
				waneFile.delete();
			}
		}
	}

	/**
	 * Method createOutputCSVFile() is responsible to generate the final output.
	 * This method also generates the location frequency statistics.
	 * settings.properties file will contain the outputCSVFile property which this
	 * method will use.
	 */
	private void createOutputCSVFile() {
		BufferedWriter bw = null;
		FileOutputStream fOut = null;
		OutputStreamWriter osw = null;

		try {
			fOut = new FileOutputStream(settings.getCSVOutputFile());
			osw = new OutputStreamWriter(fOut, "UTF-8");
			bw = new BufferedWriter(osw);

			StringBuilder sb = new StringBuilder();
			sb.append("Crawl Date").append(Constants.comma).append("County").append(Constants.comma);
			sb.append("Location").append(Constants.comma).append("Frequency").append(Constants.comma)
					.append("URL Tracked");
			bw.write(sb.toString());
			bw.newLine();

			for (Date crawlDate : result.keySet()) {

				for (String city : result.get(crawlDate).keySet()) {

					ResultMap rMap = result.get(crawlDate).get(city);
					rMap.generateStatistics(settings);

					StringBuilder URL = new StringBuilder();
					for (String eachUrl : rMap.getUrl()) {
						URL.append(eachUrl).append(Constants.pipe);
					}
					URL.deleteCharAt(URL.lastIndexOf("|"));

					int counter = 0;
					for (String key : rMap.getLocationCount().keySet()) {
						long locationFrequency = rMap.getLocationCount().get(key);

						if (locationFrequency >= Integer.parseInt(settings.getLocationFrequencyCutPoint())) {
							sb = new StringBuilder();
							sb.append(crawlDate);
							sb.append(Constants.comma);
							sb.append(city);
							sb.append(Constants.comma);
							sb.append(key).append(Constants.comma);
							sb.append(locationFrequency);
							sb.append(Constants.comma);
							if (counter == 0) {
								sb.append(URL.toString());
							}
							bw.write(sb.toString());
							bw.newLine();
							counter++;
						}
					}
				}
			}
			bw.flush();
			bw.close();
			osw.close();
			fOut.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			new WaneSettingsException(e.getMessage(), e);
		} catch (IOException e) {
			new WaneSettingsException(e.getMessage(), e);
		}

	}

	private void readFromExcelSheet() {

		// reading from excel, this can be pulled from some URL location, currently part
		// of workspace.
		File communityFile = new File(settings.getCommunityFile());

		if (communityFile.exists()) {

			XSSFWorkbook workBook = null;
			OPCPackage pkg = null;
			try {
				pkg = OPCPackage.openOrCreate(communityFile);
				workBook = new XSSFWorkbook(pkg);

				XSSFSheet sheet = workBook.getSheetAt(0);
				String cityCell = null;
				String urlCell = null;
				Set<String> urlList = null;

				if (sheet.getPhysicalNumberOfRows() > 1) {
					Iterator<Row> rowPointer = sheet.iterator();

					// skip the header
					if (rowPointer.hasNext()) {
						rowPointer.next();
					}

					while (rowPointer.hasNext()) {
						Row eachRow = rowPointer.next();
						if (eachRow != null && eachRow.getPhysicalNumberOfCells() > 1) {
							cityCell = eachRow.getCell(0).getStringCellValue();
							urlCell = Utility.procesURL(eachRow.getCell(5).getStringCellValue());

							if (Utility.isNotNullAndBlank(cityCell) && Utility.isNotNullAndBlank(urlCell)) {
								cityCell = cityCell.replaceAll(Constants.comma, Constants.dash);
								websitesToTrack.add(urlCell);
								// might need to change
								urlCommunityMap.put(urlCell, cityCell);

								if (communityURLMap.get(cityCell) == null) {
									urlList = new HashSet<String>();
									urlList.add(urlCell);
									communityURLMap.put(cityCell, urlList);
								} else {
									urlList = communityURLMap.get(cityCell);
									urlList.add(urlCell);
									communityURLMap.put(cityCell, urlList);
								}
							}
						}
					}
				}
				pkg.close();
			} catch (FileNotFoundException e) {
				new WaneSettingsException(communityFile.getAbsolutePath() + " : Please check the File.", e);
			} catch (IOException e) {
				new WaneSettingsException(communityFile.getAbsolutePath() + " : Please check the File.", e);
			} catch (InvalidFormatException e) {
				new WaneSettingsException(communityFile.getAbsolutePath() + " : Please check the File.", e);
			} finally {
				communityFile = null;
			}
		}
	}

}
