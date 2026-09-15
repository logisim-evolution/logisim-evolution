/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;

import com.bric.colorpicker.ColorPicker;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.generic.TextColorChooser;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.TextTool;
import com.cburch.logisim.util.ColorUtil;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.prefs.PreferenceChangeEvent;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextColorPreferenceTest {
  private final String originalTheme = AppPreferences.LookAndFeel.get();

  @AfterEach
  void restoreTheme() throws Exception {
    SwingUtilities.invokeAndWait(() -> AppPreferences.LookAndFeel.set(originalTheme));
  }

  @Test
  void textPreferenceSwatchFollowsThemeWithoutChangingPreference() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final var originalPreference = AppPreferences.TEXT_TOOL_COLOR.get();
      final var button = new ColorChooserButton(null, AppPreferences.TEXT_TOOL_COLOR);
      final var stored = new Color(originalPreference);
      AppPreferences.LookAndFeel.set("TestLight");
      assertEquals(stored, swatch(button));
      AppPreferences.LookAndFeel.set("TestDark");
      assertEquals(ColorUtil.getLuminanceInvertedColor(stored), swatch(button));
      assertEquals(originalPreference, AppPreferences.TEXT_TOOL_COLOR.get());
      AppPreferences.TEXT_TOOL_COLOR.removePropertyChangeListener(button);
      AppPreferences.LookAndFeel.removePropertyChangeListener(button);
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"TestLight", "TestDark"})
  void confirmedPreferenceCreatesToolsWithOnlyTheLightColor(String theme) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final var originalPreference = AppPreferences.TEXT_TOOL_COLOR.get();
      final var button = new ColorChooserButton(null, AppPreferences.TEXT_TOOL_COLOR);
      AppPreferences.LookAndFeel.set(theme);
      final var stored = new Color(originalPreference).equals(Color.GRAY) ? Color.BLACK : Color.GRAY;
      final var selected = ColorUtil.getThemeTextColor(stored);
      try (final var dialogs = mockStatic(OptionPane.class)) {
        dialogs.when(() -> OptionPane.showConfirmDialog(
            isNull(), any(TextColorChooser.class), any(String.class),
            eq(OptionPane.OK_CANCEL_OPTION), eq(OptionPane.PLAIN_MESSAGE)))
            .thenAnswer(invocation -> {
              final TextColorChooser chooser = invocation.getArgument(1);
              ((ColorPicker) chooser.getComponent(0)).setColor(selected);
              return OptionPane.OK_OPTION;
            });
        button.doClick();
        refreshPreference();
        assertEquals(stored.getRGB(), AppPreferences.TEXT_TOOL_COLOR.get());
        assertEquals(stored, new TextTool().getAttributeSet().getValue(Text.ATTR_COLOR));
        assertEquals(stored, new AddTool(Text.FACTORY).getAttributeSet().getValue(Text.ATTR_COLOR));
        assertEquals(selected, swatch(button));
      } finally {
        AppPreferences.TEXT_TOOL_COLOR.set(originalPreference);
        refreshPreference();
        AppPreferences.TEXT_TOOL_COLOR.removePropertyChangeListener(button);
        AppPreferences.LookAndFeel.removePropertyChangeListener(button);
      }
    });
  }

  private static void refreshPreference() {
    final var monitor = AppPreferences.TEXT_TOOL_COLOR;
    monitor.preferenceChange(
        new PreferenceChangeEvent(AppPreferences.getPrefs(), monitor.getIdentifier(), ""));
  }

  @Test
  void cancelDoesNotApplyTheEditedPreference() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final var button = new ColorChooserButton(null, AppPreferences.TEXT_TOOL_COLOR);
      final var originalPreference = AppPreferences.TEXT_TOOL_COLOR.get();
      AppPreferences.LookAndFeel.set("TestDark");
      try (final var dialogs = mockStatic(OptionPane.class)) {
        dialogs.when(() -> OptionPane.showConfirmDialog(
            isNull(), any(TextColorChooser.class), any(String.class),
            eq(OptionPane.OK_CANCEL_OPTION), eq(OptionPane.PLAIN_MESSAGE)))
            .thenAnswer(invocation -> {
              final TextColorChooser chooser = invocation.getArgument(1);
              ((ColorPicker) chooser.getComponent(0)).setColor(Color.RED);
              return OptionPane.CANCEL_OPTION;
            });
        button.doClick();
        dialogs.verify(() -> OptionPane.showConfirmDialog(
            isNull(), any(TextColorChooser.class), any(String.class),
            eq(OptionPane.OK_CANCEL_OPTION), eq(OptionPane.PLAIN_MESSAGE)));
        assertEquals(originalPreference, AppPreferences.TEXT_TOOL_COLOR.get());
      } finally {
        AppPreferences.TEXT_TOOL_COLOR.removePropertyChangeListener(button);
        AppPreferences.LookAndFeel.removePropertyChangeListener(button);
      }
    });
  }

  private static Color swatch(ColorChooserButton button) {
    final var icon = button.getIcon();
    final var image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
    final var graphics = image.createGraphics();
    try {
      icon.paintIcon(button, graphics, 0, 0);
      return new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2));
    } finally {
      graphics.dispose();
    }
  }
}
