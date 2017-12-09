package edu.rutgers.news.measure;

/**
 * Wrapper exception used when loading app settings from properties file and command line arguments.
 *
 * Shields callers knowing which underlying exceptions could be thrown.
 */
@SuppressWarnings("serial")
class WaneSettingsException extends Exception {
  WaneSettingsException(String message) {
    super(message);
  }
  WaneSettingsException(String message, Throwable cause) {
    super(message, cause);
  }
}
