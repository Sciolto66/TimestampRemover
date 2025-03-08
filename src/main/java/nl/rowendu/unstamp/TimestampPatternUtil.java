package nl.rowendu.unstamp;

import java.util.regex.Pattern;

public class TimestampPatternUtil {
  private static final String TIMESTAMP_PATTERN =
      "(?:\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z]|"
          + "\\d{4}-\\d{2}-\\d{2}|"
          + "\\d{2}:\\d{2}:\\d{2}[.,]\\d{3}|"
          + "\\d{2}:\\d{2}:\\d{2})\\s*";

  private static final String TIMESTAMP_PATTERN_START =
      "^(?:\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z]|"
          + "\\d{4}-\\d{2}-\\d{2}|"
          + "\\d{2}:\\d{2}:\\d{2}[.,]\\d{3}|"
          + "\\d{2}:\\d{2}:\\d{2})\\s*";

  // Private constructor to prevent instantiation
  private TimestampPatternUtil() {
    // Utility class - no instantiation
  }

  public static Pattern getPattern(boolean startOfLineOnly) {
    return Pattern.compile(startOfLineOnly ? TIMESTAMP_PATTERN_START : TIMESTAMP_PATTERN);
  }
}
