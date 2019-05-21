package lu.uni.svv.StressTesting.utils;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class VerySimpleFormatter extends Formatter {

	 private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

		@Override
		public String format(LogRecord record) {
			return String.format("%1$s\n", formatMessage(record));
		}
}
