package com.bfh.logisim.designrulecheck;

import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

public class Strings {
	public static String get(String key) {
		return source.get(key);
	}

	public static String get(String key, String arg) {
		return StringUtil.format(source.get(key), arg);
	}

	public static String get(String key, String arg0, String arg1) {
		return StringUtil.format(source.get(key), arg0, arg1);
	}

	public static StringGetter getter(String key) {
		return source.getter(key);
	}

	public static StringGetter getter(String key, String arg) {
		return source.getter(key, arg);
	}

	private static LocaleManager source = new LocaleManager(
			"resources/logisim", "fpgacom");

}
