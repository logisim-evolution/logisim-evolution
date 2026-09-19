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

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.ColorUtil;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
