/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bric.colorpicker.ColorPicker;
import com.bric.colorpicker.colorslider.ColorSlider;
import com.cburch.logisim.gui.generic.TextColorChooser;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.ColorUtil;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextColorAttributeTest {
  private final String originalTheme = AppPreferences.LookAndFeel.get();

  @AfterEach
  void restoreTheme() throws Exception {
    SwingUtilities.invokeAndWait(() -> AppPreferences.LookAndFeel.set(originalTheme));
  }

  @Test
  void themeChangesDisplayButNotFileRepresentation() {
    final var stored = Text.ATTR_COLOR.parse("#0000ff80");
    AppPreferences.LookAndFeel.set("TestLight");
    assertEquals("#0000ff80", Text.ATTR_COLOR.toDisplayString(stored));

    AppPreferences.LookAndFeel.set("TestDark");
    assertEquals("#dadaff80", Text.ATTR_COLOR.toDisplayString(stored));
    assertEquals("#0000ff80", Text.ATTR_COLOR.toStandardString(stored));
    assertEquals(stored, Text.ATTR_COLOR.parse("#0000ff80"));
    assertEquals("#0000ff80", StdAttr.LABEL_COLOR.toDisplayString(stored));
  }

  @ParameterizedTest
  @CsvSource({
    "TestLight, #ffffff",
    "TestDark, #000000",
    "TestDarcula, #000000",
    "TestDark, #767676",
    "TestDark, #0000ff"
  })
  void brightnessSliderTracksInitialColorAndSubsequentEdits(String theme, String stored)
      throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      AppPreferences.LookAndFeel.set(theme);
      final var chooser = editor(Text.ATTR_COLOR.parse(stored));
      final var picker = picker(chooser);
      final var slider = java.util.Arrays.stream(picker.getComponents())
          .filter(ColorSlider.class::isInstance)
          .map(ColorSlider.class::cast)
          .findFirst().orElseThrow();
      assertEquals((int) (picker.getHSB()[2] * slider.getMaximum()), slider.getValue());
      assertNull(chooser.getValue());

      picker.setColor(Color.BLACK);
      assertEquals(slider.getMinimum(), slider.getValue());
      picker.setColor(Color.WHITE);
      assertEquals(slider.getMaximum(), slider.getValue());

      slider.setValue(slider.getMinimum());
      assertEquals(Color.BLACK, picker.getColor());
      slider.setValue(slider.getMaximum());
      assertEquals(Color.WHITE, picker.getColor());
    });
  }

  @Test
  void openingClippedColorAndReturningToItDoesNotCreateAnEdit() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      AppPreferences.LookAndFeel.set("TestDark");
      final var chooser = editor(Color.BLUE);
      final var picker = picker(chooser);
      final var displayed = Text.ATTR_COLOR.parse("#dadaff");
      assertEquals(displayed, picker.getColor());
      assertNull(chooser.getValue());
      assertEquals(displayed, preview(chooser).getForeground());

      picker.setColor(Color.RED);
      picker.setColor(displayed);
      assertNull(chooser.getValue());
      assertEquals(displayed, preview(chooser).getForeground());
    });
  }

  @Test
  void lightSelectionIsStoredDirectly() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      AppPreferences.LookAndFeel.set("TestLight");
      final var chooser = editor(Color.BLACK);
      final var selected = Text.ATTR_COLOR.parse("#12345678");
      picker(chooser).setColor(selected);

      assertEquals(selected, chooser.getValue());
      assertEquals(selected, preview(chooser).getForeground());
    });
  }

  @Test
  void darkSelectionStoresLightColorAndPreviewsItsActualRendering() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      AppPreferences.LookAndFeel.set("TestDark");
      final var chooser = editor(Color.BLACK);
      picker(chooser).setColor(Text.ATTR_COLOR.parse("#89898978"));

      assertEquals(Text.ATTR_COLOR.parse("#76767678"), chooser.getValue());
      assertEquals(Text.ATTR_COLOR.parse("#89898978"), preview(chooser).getForeground());

      // Saturated blue cannot be reproduced exactly by the existing clipped mapping.
      picker(chooser).setColor(Color.BLUE);
      assertEquals(Text.ATTR_COLOR.parse("#dadaff"), chooser.getValue());
      assertEquals(Text.ATTR_COLOR.parse("#202045"), preview(chooser).getForeground());
      assertEquals(ColorUtil.getThemeTextColor(chooser.getValue()), preview(chooser).getForeground());
    });
  }

  @Test
  void opacityOnlyEditPreservesClippedSourceRgb() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      AppPreferences.LookAndFeel.set("TestDarcula");
      final var chooser = editor(Color.BLUE);
      picker(chooser).setColor(Text.ATTR_COLOR.parse("#dadaff78"));

      assertEquals(Text.ATTR_COLOR.parse("#0000ff78"), chooser.getValue());
      assertEquals(Text.ATTR_COLOR.parse("#dadaff78"), preview(chooser).getForeground());
    });
  }

  private static TextColorChooser editor(Color stored) {
    return (TextColorChooser) Text.ATTR_COLOR.getCellEditor(null, stored);
  }

  private static ColorPicker picker(TextColorChooser chooser) {
    return (ColorPicker) chooser.getComponent(0);
  }

  private static JLabel preview(TextColorChooser chooser) {
    return (JLabel) ((JPanel) chooser.getComponent(1)).getComponent(0);
  }
}
