/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.prefs.AppPreferences;
import com.formdev.flatlaf.FlatLaf;
import java.awt.Window;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ThemeManager {

  public static final String THEME_PROPERTY = "theme";

  private static final PropertyChangeSupport support = new PropertyChangeSupport(ThemeManager.class);

  private static boolean darkMode = false;

  private ThemeManager() {}

  public static boolean isDarkMode() {
    return darkMode;
  }

  public static void setDarkMode(boolean dark) {
    darkMode = dark;
  }

  public static boolean isDarkLaf(javax.swing.LookAndFeel laf) {
    if (laf == null) return false;
    final var name = laf.getClass().getName();
    return name.contains("Dark") || name.contains("Darcula");
  }

  public static boolean isDarkLaf(String lafClassName) {
    return lafClassName != null && (lafClassName.contains("Dark") || lafClassName.contains("Darcula"));
  }

  public static void applyTheme() {
    final var laf = UIManager.getLookAndFeel();
    final var wasDark = darkMode;
    darkMode = isDarkLaf(laf);

    AppPreferences.applyThemeDefaults(darkMode);
    Value.refreshColors();

    for (final var w : Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(w);
    }

    support.firePropertyChange(THEME_PROPERTY, wasDark, darkMode);
  }

  public static void addPropertyChangeListener(PropertyChangeListener listener) {
    support.addPropertyChangeListener(listener);
  }

  public static void removePropertyChangeListener(PropertyChangeListener listener) {
    support.removePropertyChangeListener(listener);
  }
}
