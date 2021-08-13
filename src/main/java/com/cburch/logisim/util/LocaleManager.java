/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

public class LocaleManager {
  private static class LocaleGetter implements StringGetter {
    final LocaleManager source;
    final String key;

    LocaleGetter(LocaleManager source, String key) {
      this.source = source;
      this.key = key;
    }

    @Override
    public String toString() {
      return source.get(key);
    }
  }

  /**
   * Always returns "fixed" value. Implements StringGetter interface
   * to let this class be used for i.e. settings for which there's no
   * point of providing localizable strings (i.e. screen resolutsions,
   * or plain numeric values), thus using LocaleGetter is an overkill.
   */
  private static class FixedString implements StringGetter {
    final String value;

    FixedString(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }
  }

  /* kwalsh  >> */
  private static class LocaleFormatterWithString extends LocaleGetter {
    final String arg;

    LocaleFormatterWithString(LocaleManager source, String key, String arg) {
      super(source, key);
      this.arg = arg;
    }

    @Override
    public String toString() {
      return source.fmt(key, arg);
    }
  }

  private static class LocaleFormatterWithGetter extends LocaleGetter {
    final StringGetter arg;

    LocaleFormatterWithGetter(LocaleManager source, String key, StringGetter arg) {
      super(source, key);
      this.arg = arg;
    }

    @Override
    public String toString() {
      return source.fmt(key, arg.toString());
    }
  }
  /* << kwalsh */

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
      val = S.locale.getString("accentReplacements");
    } catch (MissingResourceException e) {
      return null;
    }
    final var toks = new StringTokenizer(val, "/");
    while (toks.hasMoreTokens()) {
      final var tok = toks.nextToken().trim();
      var c = '\0';
      String s = null;
      if (tok.length() == 1) {
        c = tok.charAt(0);
        s = "";
      } else if (tok.length() >= 2 && tok.charAt(1) == ' ') {
        c = tok.charAt(0);
        s = tok.substring(2).trim();
      }
      if (s != null) {
        if (ret == null) ret = new HashMap<>();
        ret.put(c, s);
      }
    }
    return ret;
  }

  private static void fireLocaleChanged() {
    for (final var l : listeners) {
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

  private static String replaceAccents(String src, HashMap<Character, String> repl) {
    // find first non-standard character - so we can avoid the
    // replacement process if possible
    var i = 0;
    var n = src.length();
    for (; i < n; i++) {
      final var ci = src.charAt(i);
      if (ci < 32 || ci >= 127) break;
    }
    if (i == n) return src;

    // ok, we'll have to consider replacing accents
    char[] cs = src.toCharArray();
    final var ret = new StringBuilder(src.substring(0, i));
    for (int j = i; j < cs.length; j++) {
      char cj = cs[j];
      if (cj < 32 || cj >= 127) {
        String out = repl.get(cj);
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

  private static void updateButtonText() {
    UIManager.put("FileChooser.openDialogTitleText", S.get("LMopenDialogTitleText"));
    UIManager.put("FileChooser.saveDialogTitleText", S.get("LMsaveDialogTitleText"));
    UIManager.put("FileChooser.lookInLabelText", S.get("LMlookInLabelText"));
    UIManager.put("FileChooser.saveInLabelText", S.get("LMsaveInLabelText"));
    UIManager.put("FileChooser.upFolderToolTipText", S.get("LMupFolderToolTipText"));
    UIManager.put("FileChooser.homeFolderToolTipText", S.get("LMhomeFolderToolTipText"));
    UIManager.put("FileChooser.newFolderToolTipText", S.get("LMnewFolderToolTipText"));
    UIManager.put("FileChooser.listViewButtonToolTipText", S.get("LMlistViewButtonToolTipText"));
    UIManager.put("FileChooser.detailsViewButtonToolTipText", S.get("LMdetailsViewButtonToolTipText"));
    UIManager.put("FileChooser.fileNameHeaderText", S.get("LMfileNameHeaderText"));
    UIManager.put("FileChooser.fileSizeHeaderText", S.get("LMfileSizeHeaderText"));
    UIManager.put("FileChooser.fileTypeHeaderText", S.get("LMfileTypeHeaderText"));
    UIManager.put("FileChooser.fileDateHeaderText", S.get("LMfileDateHeaderText"));
    UIManager.put("FileChooser.fileAttrHeaderText", S.get("LMfileAttrHeaderText"));
    UIManager.put("FileChooser.fileNameLabelText", S.get("LMfileNameLabelText"));
    UIManager.put("FileChooser.filesOfTypeLabelText", S.get("LMfilesOfTypeLabelText"));
    UIManager.put("FileChooser.openButtonText", S.get("LMopenButtonText"));
    UIManager.put("FileChooser.openButtonToolTipText", S.get("LMopenButtonToolTipText"));
    UIManager.put("FileChooser.saveButtonText", S.get("LMsaveButtonText"));
    UIManager.put("FileChooser.saveButtonToolTipText", S.get("LMsaveButtonToolTipText"));
    UIManager.put("FileChooser.directoryOpenButtonText", S.get("LMdirectoryOpenButtonText"));
    UIManager.put("FileChooser.directoryOpenButtonToolTipText", S.get("LMdirectoryOpenButtonToolTipText"));
    UIManager.put("FileChooser.cancelButtonText", S.get("LMcancelButtonText"));
    UIManager.put("FileChooser.cancelButtonToolTipText", S.get("LMcancelButtonToolTipText"));
    UIManager.put("FileChooser.newFolderErrorText", S.get("LMnewFolderErrorText"));
    UIManager.put("FileChooser.acceptAllFileFilterText", S.get("LMacceptAllFileFilterText"));
    UIManager.put("OptionPane.okButtonText", S.get("LMokButtonText"));
    UIManager.put("OptionPane.yesButtonText", S.get("LMyesButtonText"));
    UIManager.put("OptionPane.noButtonText", S.get("LMnoButtonText"));
    UIManager.put("OptionPane.cancelButtonText", S.get("LMcancelButtonText"));
    UIManager.put("ProgressMonitor.progressText", S.get("LMprogressText"));
  }

  public static void setLocale(Locale loc) {
    final var cur = getLocale();
    if (!loc.equals(cur)) {
      final var opts = S.getLocaleOptions();
      Locale select = null;
      Locale backup = null;
      String locLang = loc.getLanguage();
      for (final var opt : opts) {
        if (select == null && opt.equals(loc)) {
          select = opt;
        }
        if (backup == null && opt.getLanguage().equals(locLang)) {
          backup = opt;
        }
      }
      if (select == null) {
        select = Objects.requireNonNullElseGet(backup, () -> new Locale("en"));
      }

      curLocale = select;
      Locale.setDefault(select);
      for (final var man : managers) {
        man.loadDefault();
      }
      repl = replaceAccents ? fetchReplaceAccents() : null;
      updateButtonText();
      fireLocaleChanged();
    }
  }

  public static void setReplaceAccents(boolean value) {
    final var newRepl = value ? fetchReplaceAccents() : null;
    replaceAccents = value;
    repl = newRepl;
    fireLocaleChanged();
  }

  // static members
  private static final String SETTINGS_NAME = "settings";

  private static final ArrayList<LocaleManager> managers = new ArrayList<>();

  private static final String DATE_FORMAT = S.get("dateFormat");

  public static final SimpleDateFormat parserSDF = new SimpleDateFormat(LocaleManager.DATE_FORMAT);

  private static final ArrayList<LocaleListener> listeners = new ArrayList<>();

  private static boolean replaceAccents = false;

  private static HashMap<Character, String> repl = null;

  private static Locale curLocale = null;
  // instance members
  private final String dirName;
  private final String fileStart;
  private ResourceBundle settings = null;
  private ResourceBundle locale = null;

  public LocaleManager(String dirName, String fileStart) {
    this.dirName = dirName;
    this.fileStart = fileStart;
    loadDefault();
    managers.add(this);
  }

  public JComponent createLocaleSelector() {
    var locales = getLocaleOptions();
    if (locales == null || locales.length == 0) {
      var cur = getLocale();
      if (cur == null) cur = new Locale("en");
      locales = new Locale[] {cur};
    }
    return new JScrollPane(new LocaleSelector(locales));
  }

  public String get(String key) {
    String ret;
    try {
      ret = locale.getString(key);
    } catch (MissingResourceException e) {
      ret = key;
    }
    final var repl = LocaleManager.repl;
    if (repl != null) ret = replaceAccents(ret, repl);
    return ret;
  }

  public String get(String key, Object... args) {
    return String.format(get(key), args);
  }

  /**
   * @Deprecated Use get(key, ...)
   *
   */
  public String fmt(String key, Object... args) {
    return String.format(get(key), args);
  }
  /* << kwalsh */

  public Locale[] getLocaleOptions() {
    String locs = null;
    try {
      if (settings != null) locs = settings.getString("locales");
    } catch (java.util.MissingResourceException ignored) {
    }
    if (locs == null) return new Locale[] {};

    final var retl = new ArrayList<Locale>();
    StringTokenizer toks = new StringTokenizer(locs);
    while (toks.hasMoreTokens()) {
      final var f = toks.nextToken();
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
        Locale loc = country == null ? new Locale(language) : new Locale(language, country);
        retl.add(loc);
      }
    }

    return retl.toArray(new Locale[0]);
  }

  public StringGetter getter(String key) {
    return new LocaleGetter(this, key);
  }

  public StringGetter getter(String key, String arg) {
    return new LocaleFormatterWithString(this, key, arg);
  }

  public StringGetter getter(String key, StringGetter arg) {
    return new LocaleFormatterWithGetter(this, key, arg);
  }

  public StringGetter fixedString(String value) {
    return new FixedString(value);
  }

  private void loadDefault() {
    if (settings == null) {
      try {
        settings = ResourceBundle.getBundle(dirName + "/" + SETTINGS_NAME);
      } catch (java.util.MissingResourceException ignored) {
      }
    }

    try {
      loadLocale(Locale.getDefault());
      if (locale != null) return;
    } catch (java.util.MissingResourceException ignored) {
    }
    try {
      loadLocale(Locale.ENGLISH);
      if (locale != null) return;
    } catch (java.util.MissingResourceException ignored) {
    }
    Locale[] choices = getLocaleOptions();
    if (choices != null && choices.length > 0) loadLocale(choices[0]);
    if (locale != null) return;
    throw new RuntimeException("No locale bundles are available");
  }

  private void loadLocale(Locale loc) {
    final var bundleName = dirName + "/strings/" + fileStart + "/" + fileStart;
    locale = ResourceBundle.getBundle(bundleName, loc);
  }
}
