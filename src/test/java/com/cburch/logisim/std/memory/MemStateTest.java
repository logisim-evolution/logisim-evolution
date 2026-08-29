/*
 * Logisim-evolution - digital logic design tool
 * Copyright by the logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class MemStateTest {

  @Test
  void paintRecalculatesLayoutWhenFontMetricsChange() {
    final var state = new MemState(MemContents.create(8, 8, false));
    final var image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
    final var graphics = image.createGraphics();
    try {
      graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 8));
      state.paint(graphics, 0, 0, 0, 0, 100, 100, 1);
      final var smallFontLines = state.getNrOfLines();

      graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 24));
      state.paint(graphics, 0, 0, 0, 0, 100, 100, 1);

      assertTrue(state.getNrOfLines() < smallFontLines);
    } finally {
      graphics.dispose();
    }
  }
}
