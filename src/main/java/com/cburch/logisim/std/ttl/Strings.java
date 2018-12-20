package com.cburch.logisim.std.ttl;

import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;

class Strings {
	private static LocaleManager source = new LocaleManager("resources/logisim", "std");

	public static String get(String key) {
		return source.get(key);
	}

	public static StringGetter getter(String key) {
		return source.getter(key);
	}

	public static StringGetter getter(String key, String arg) {
		return source.getter(key, arg);
	}
}
