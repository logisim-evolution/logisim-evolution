/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JList;
import org.junit.jupiter.api.Test;

class ListModelCellRendererTest {
  private static final double MINIMUM_CONTRAST_RATIO = 4.5;

  @Test
  void messageColorsRemainReadableOnLightBackground() {
    assertMessageColorsReadable(Color.WHITE, Color.BLACK);
  }

  @Test
  void messageColorsRemainReadableOnDarkBackground() {
    assertMessageColorsReadable(new Color(0x3C3F41), Color.WHITE);
  }

  @Test
  void fatalErrorsUseWhiteTextAcrossThemes() {
    assertFatalColors(Color.WHITE, Color.BLACK);
    assertFatalColors(new Color(0x3C3F41), Color.WHITE);
  }

  private static void assertMessageColorsReadable(Color background, Color foreground) {
    final var list = new JList<>();
    list.setBackground(background);
    list.setForeground(foreground);
    final var renderer = new ListModelCellRenderer(true);

    assertReadable(
        renderer.getListCellRendererComponent(
            list,
            new SimpleDrcContainer("normal", SimpleDrcContainer.LEVEL_NORMAL),
            0,
            false,
            false));
    assertReadable(
        renderer.getListCellRendererComponent(
            list,
            new SimpleDrcContainer("severe", SimpleDrcContainer.LEVEL_SEVERE),
            1,
            false,
            false));
    assertReadable(
        renderer.getListCellRendererComponent(
            list,
            new SimpleDrcContainer("addendum", SimpleDrcContainer.LEVEL_NORMAL, true),
            2,
            false,
            false));
  }

  private static void assertReadable(Component component) {
    final var contrast = contrastRatio(component.getForeground(), component.getBackground());
    assertTrue(
        contrast >= MINIMUM_CONTRAST_RATIO,
        () -> String.format("Expected readable message colors, but contrast ratio was %.2f", contrast));
  }

  private static void assertFatalColors(Color listBackground, Color listForeground) {
    final var list = new JList<>();
    list.setBackground(listBackground);
    list.setForeground(listForeground);
    final var component =
        new ListModelCellRenderer(true)
            .getListCellRendererComponent(
                list,
                new SimpleDrcContainer("fatal", SimpleDrcContainer.LEVEL_FATAL),
                0,
                false,
                false);

    assertEquals(Color.RED, component.getBackground());
    assertEquals(Color.WHITE, component.getForeground());
  }

  private static double contrastRatio(Color first, Color second) {
    final var firstLuminance = relativeLuminance(first);
    final var secondLuminance = relativeLuminance(second);
    final var lighter = Math.max(firstLuminance, secondLuminance);
    final var darker = Math.min(firstLuminance, secondLuminance);
    return (lighter + 0.05) / (darker + 0.05);
  }

  private static double relativeLuminance(Color color) {
    return 0.2126 * linearChannel(color.getRed())
        + 0.7152 * linearChannel(color.getGreen())
        + 0.0722 * linearChannel(color.getBlue());
  }

  private static double linearChannel(int channel) {
    final var value = channel / 255.0;
    return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
  }
}
