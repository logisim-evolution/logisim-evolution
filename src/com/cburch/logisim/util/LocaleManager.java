/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

public class LocaleManager {
	private static class LocaleGetter implements StringGetter {
		private LocaleManager source;
		private String key;

		LocaleGetter(LocaleManager source, String key) {
			this.source = source;
			this.key = key;
		}

		public String toString() {
			return source.get(key);
		}

		/*
		 * @Override public String toString() { return get(); }
		 */
	}

	public static void addLocaleListener(LocaleListener l) {
		listeners.add(l);
	}

	public static boolean canReplaceAccents() {
		return fetchReplaceAccents() != null;
	}

	private static HashMap<Character, String> fetchReplaceAccents() {
		HashMap<Character, String> ret = null;
		String val;
		try {
			val = Strings.source.locale.getString("accentReplacements");
		} catch (MissingResourceException e) {
			return null;
		}
		StringTokenizer toks = new StringTokenizer(val, "/");
		while (toks.hasMoreTokens()) {
			String tok = toks.nextToken().trim();
			char c = '\0';
			String s = null;
			if (tok.length() == 1) {
				c = tok.charAt(0);
				s = "";
			} else if (tok.length() >= 2 && tok.charAt(1) == ' ') {
				c = tok.charAt(0);
				s = tok.substring(2).trim();
			}
			if (s != null) {
				if (ret == null)
					ret = new HashMap<Character, String>();
				ret.put(new Character(c), s);
			}
		}
		return ret;
	}

	private static void fireLocaleChanged() {
		for (LocaleListener l : listeners) {
			l.localeChanged();
		}
	}

	public static Locale getLocale() {
		if (curLocale == null) {
			curLocale = Locale.getDefault();
		}
		return curLocale;
	}

	public static void removeLocaleListener(LocaleListener l) {
		listeners.remove(l);
	}

	private static String replaceAccents(String src,
			HashMap<Character, String> repl) {
		// find first non-standard character - so we can avoid the
		// replacement process if possible
		int i = 0;
		int n = src.length();
		for (; i < n; i++) {
			char ci = src.charAt(i);
			if (ci < 32 || ci >= 127)
				break;
		}
		if (i == n)
			return src;

		// ok, we'll have to consider replacing accents
		char[] cs = src.toCharArray();
		StringBuilder ret = new StringBuilder(src.substring(0, i));
		for (int j = i; j < cs.length; j++) {
			char cj = cs[j];
			if (cj < 32 || cj >= 127) {
				String out = repl.get(Character.valueOf(cj));
				if (out != null) {
					ret.append(out);
				} else {
					ret.append(cj);
				}
			} else {
				ret.append(cj);
			}
		}
		return ret.toString();
	}

	public static void setLocale(Locale loc) {
		Locale cur = getLocale();
		if (!loc.equals(cur)) {
			Locale[] opts = Strings.getLocaleManager().getLocaleOptions();
			Locale select = null;
			Locale backup = null;
			String locLang = loc.getLanguage();
			for (Locale opt : opts) {
				if (select == null && opt.equals(loc)) {
					select = opt;
				}
				if (backup == null && opt.getLanguage().equals(locLang)) {
					backup = opt;
				}
			}
			if (select == null) {
				if (backup == null) {
					select = new Locale("en");
				} else {
					select = backup;
				}
			}

			curLocale = select;
			Locale.setDefault(select);
			for (LocaleManager man : managers) {
				man.loadDefault();
			}
			repl = replaceAccents ? fetchReplaceAccents() : null;
			fireLocaleChanged();
		}
	}

	public static void setReplaceAccents(boolean value) {
		HashMap<Character, String> newRepl = value ? fetchReplaceAccents()
				: null;
		replaceAccents = value;
		repl = newRepl;
		fireLocaleChanged();
	}

	// static members
	private static final String SETTINGS_NAME = "settings";

	private static ArrayList<LocaleManager> managers = new ArrayList<LocaleManager>();

	private static String DATE_FORMAT = Strings.get("dateFormat");

	public final static SimpleDateFormat parserSDF = new SimpleDateFormat(
			LocaleManager.DATE_FORMAT);

	private static ArrayList<LocaleListener> listeners = new ArrayList<LocaleListener>();

	private static boolean replaceAccents = false;

	private static HashMap<Character, String> repl = null;

	private static Locale curLocale = null;
	// instance members
	private String dir_name;
	private String file_start;
	private ResourceBundle settings = null;
	private ResourceBundle locale = null;

	private ResourceBundle dflt_locale = null;

	public LocaleManager(String dir_name, String file_start) {
		this.dir_name = dir_name;
		this.file_start = file_start;
		loadDefault();
		managers.add(this);
	}

	public JComponent createLocaleSelector() {
		Locale[] locales = getLocaleOptions();
		if (locales == null || locales.length == 0) {
			Locale cur = getLocale();
			if (cur == null)
				cur = new Locale("en");
			locales = new Locale[] { cur };
		}
		return new JScrollPane(new LocaleSelector(locales));
	}

	public String get(String key) {
		String ret;
		try {
			ret = locale.getString(key);
		} catch (MissingResourceException e) {
			ResourceBundle backup = dflt_locale;
			if (backup == null) {
				Locale backup_loc = Locale.US;
				backup = ResourceBundle.getBundle(dir_name + "/en/"
						+ file_start, backup_loc);
				dflt_locale = backup;
			}
			try {
				ret = backup.getString(key);
			} catch (MissingResourceException e2) {
				ret = key;
			}
		}
		HashMap<Character, String> repl = LocaleManager.repl;
		if (repl != null)
			ret = replaceAccents(ret, repl);
		return ret;
	}

	public Locale[] getLocaleOptions() {
		String locs = null;
		try {
			if (settings != null)
				locs = settings.getString("locales");
		} catch (java.util.MissingResourceException e) {
		}
		if (locs == null)
			return new Locale[] {};

		ArrayList<Locale> retl = new ArrayList<Locale>();
		StringTokenizer toks = new StringTokenizer(locs);
		while (toks.hasMoreTokens()) {
			String f = toks.nextToken();
			String language;
			String country;
			if (f.length() >= 2) {
				language = f.substring(0, 2);
				country = (f.length() >= 5 ? f.substring(3, 5) : null);
			} else {
				language = null;
				country = null;
			}
			if (language != null) {
				Locale loc = country == null ? new Locale(language)
						: new Locale(language, country);
				retl.add(loc);
			}
		}

		return retl.toArray(new Locale[retl.size()]);
	}

	public StringGetter getter(String key) {
		return new LocaleGetter(this, key);
	}

	public StringGetter getter(String key, String arg) {
		return StringUtil.formatter(getter(key), arg);
	}

	public StringGetter getter(String key, StringGetter arg) {
		return StringUtil.formatter(getter(key), arg);
	}

	private void loadDefault() {
		if (settings == null) {
			try {
				settings = ResourceBundle.getBundle(dir_name + "/"
						+ SETTINGS_NAME);
			} catch (java.util.MissingResourceException e) {
			}
		}

		try {
			loadLocale(Locale.getDefault());
			if (locale != null)
				return;
		} catch (java.util.MissingResourceException e) {
		}
		try {
			loadLocale(Locale.ENGLISH);
			if (locale != null)
				return;
		} catch (java.util.MissingResourceException e) {
		}
		Locale[] choices = getLocaleOptions();
		if (choices != null && choices.length > 0)
			loadLocale(choices[0]);
		if (locale != null)
			return;
		throw new RuntimeException("No locale bundles are available");
	}

	private void loadLocale(Locale loc) {
		String bundleName = dir_name + "/" + loc.getLanguage() + "/"
				+ file_start;
		locale = ResourceBundle.getBundle(bundleName, loc);
	}
}
