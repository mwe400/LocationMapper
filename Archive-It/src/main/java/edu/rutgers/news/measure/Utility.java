package edu.rutgers.news.measure;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utility {

	public static void closeStream(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void closeStream(OutputStream out) {
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static boolean isNotNullAndBlank(String str) {
		if (str != null && !str.trim().equals(Constants.blank)) {
			return true;
		}
		return false;
	}

	
	/**
	 * Process each URL, business logic applied to understand URL like facebook
	 * @param URL - String
	 * @return
	 */
	public static String procesURL(String input) {
		if (isNotNullAndBlank(input)) {
			// all urls are converted to lower case to avoid any mismatch
			input = input.toLowerCase();
			int index = -1;
			
			if (input.indexOf(Constants.facebook, 5) != -1) {
				if ((index = input.indexOf(Constants.slash, 10)) != -1) {
					String firstHalf = input.substring(0, index);
					String restHalf = Constants.blank;
					if(input.length() > index + 1) {
						restHalf = input.substring(index + 1);
						if((index = restHalf.indexOf(Constants.slash)) != -1 ) {
							input = firstHalf + Constants.slash + restHalf.substring(0, index);
						} else if ((index = restHalf.indexOf(Constants.question)) != -1) {
							input = firstHalf + Constants.slash + restHalf.substring(0, index);
						}
					} else {
						input = firstHalf;
					}
				}
			} else if ((index = input.indexOf(Constants.slash, 10)) != -1) {
				input = input.substring(0, index);
			} else if ((index = input.indexOf(Constants.question, 10)) != -1) {
				input = input.substring(0, index);
			}
		}
		return input;
	}

	public static void closeReader(BufferedReader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void closeFileReader(FileReader fileRead) {
		if (fileRead != null) {
			try {
				fileRead.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
