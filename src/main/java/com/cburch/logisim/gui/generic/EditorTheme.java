/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.prefs.AppPreferences;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.SwingUtilities;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

public final class EditorTheme {
  private static final String LISTENER_KEY = EditorTheme.class.getName() + ".listener";
  private static final String THEME_ROOT = "/org/fife/ui/rsyntaxtextarea/themes/";

  private EditorTheme() {}

  public static void install(RSyntaxTextArea editor) {
    apply(editor);

    final PropertyChangeListener listener =
        event -> {
          if (AppPreferences.LookAndFeel.isSource(event)) {
            SwingUtilities.invokeLater(() -> apply(editor));
          } else if (AppPreferences.LIGHT_EDITOR_THEME.isSource(event)
              || AppPreferences.DARK_EDITOR_THEME.isSource(event)) {
            apply(editor);
          }
        };
    editor.putClientProperty(LISTENER_KEY, listener);
    AppPreferences.addPropertyChangeListener(listener);
  }

  private static void apply(RSyntaxTextArea editor) {
    final var preference =
        AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get())
            ? AppPreferences.DARK_EDITOR_THEME
            : AppPreferences.LIGHT_EDITOR_THEME;
    final var themePath = THEME_ROOT + preference.get() + ".xml";
    try (final var input = EditorTheme.class.getResourceAsStream(themePath)) {
      if (input != null) {
        Theme.load(input).apply(editor);
      }
    } catch (IOException ignored) {
    }
  }
}
