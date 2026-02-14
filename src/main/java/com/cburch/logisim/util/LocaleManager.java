/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
   * Always returns "fixed" value. Implements StringGetter interface to let this class be used for
   * i.e. settings for which there's no point of providing localizable strings (i.e. screen
   * resolutsions, or plain numeric values), thus using LocaleGetter is an overkill.
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

  private static class LocaleFormatterWithString extends LocaleGetter {
    final Object [] args;

    LocaleFormatterWithString(LocaleManager source, String key, String... args) {
      super(source, key);
      this.args = args;
    }

    @Override
    public String toString() {
      return source.get(key, args);
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

  private static final String SETTINGS_NAME = "settings";
  private static final ArrayList<LocaleManager> managers = new ArrayList<>();
  public static final SimpleDateFormat PARSER_SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  private static final ArrayList<LocaleListener> listeners = new ArrayList<>();
  private static final HashMap<Character, String> repl = null;
  private static Locale curLocale = null;

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

  public static void addLocaleListener(LocaleListener l) {
    listeners.add(l);
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
    UIManager.put(
        "FileChooser.detailsViewButtonToolTipText", S.get("LMdetailsViewButtonToolTipText"));
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
    UIManager.put(
        "FileChooser.directoryOpenButtonToolTipText", S.get("LMdirectoryOpenButtonToolTipText"));
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
        select = Objects.requireNonNullElseGet(backup, () -> Locale.ENGLISH);
      }

      curLocale = select;
      Locale.setDefault(select);
      for (final var man : managers) {
        man.loadDefault();
      }
      updateButtonText();
      fireLocaleChanged();
    }
  }

  public JComponent createLocaleSelector() {
    var locales = getLocaleOptions();
    if (locales == null || locales.length == 0) {
      var cur = getLocale();
      if (cur == null) cur = Locale.ENGLISH;
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
   */
  public String fmt(String key, Object... args) {
    return String.format(get(key), args);
  }
  /* << kwalsh */

  public Locale[] getLocaleOptions() {
    String locs = null;
    try {
      if (settings != null) locs = settings.getString("locales");
    } catch (MissingResourceException ignored) {
      // Do nothing.
    }
    if (locs == null) return new Locale[] {};

    final var retl = new ArrayList<Locale>();
    final var toks = new StringTokenizer(locs);
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
        final var loc = new Locale.Builder().setLanguage(language).setRegion(country).build();
        retl.add(loc);
      }
    }

    return retl.toArray(new Locale[0]);
  }

  public StringGetter getter(String key) {
    return new LocaleGetter(this, key);
  }

  public StringGetter getter(String key, String... args) {
    return new LocaleFormatterWithString(this, key, args);
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
      } catch (MissingResourceException ignored) {
        // Do nothing.
      }
    }

    try {
      loadLocale(Locale.getDefault());
      if (locale != null) return;
    } catch (MissingResourceException ignored) {
      // Do nothing.
    }
    try {
      loadLocale(Locale.ENGLISH);
      if (locale != null) return;
    } catch (MissingResourceException ignored) {
      // Do nothing.
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
