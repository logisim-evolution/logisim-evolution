/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package edu.cornell.cs3410;

import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

class Strings {
	private static LocaleManager source
		= new LocaleManager("resources/logisim", "std");

	public static String get(String key) {
		return source.get(key);
	}
	public static String get(String key, String arg0) {
		return StringUtil.format(source.get(key), arg0);
	}
	public static String get(String key, String arg0, String arg1) {
		return StringUtil.format(source.get(key), arg0, arg1);
	}
	public static StringGetter getter(String key) {
		return source.getter(key);
	}
}
