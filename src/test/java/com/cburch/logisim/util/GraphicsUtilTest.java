/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class GraphicsUtilTest {

  @Test
  void multilineBoundsUseLongestLineAndAllLineHeights() {
    final var graphics =
        new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB).createGraphics();
    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

    final var first =
        GraphicsUtil.getTextBounds(
            graphics, "long", 0, 0, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);
    final var second =
        GraphicsUtil.getTextBounds(
            graphics, "x", 0, 0, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);
    final var multiline =
        GraphicsUtil.getTextBounds(
            graphics, "long\nx", 0, 0, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);

    assertEquals(Math.max(first.width, second.width), multiline.width);
    assertEquals(first.height * 2, multiline.height);
    graphics.dispose();
  }

  @Test
  void multilineCursorAndHitTestingUseClickedLine() {
    final var graphics =
        new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB).createGraphics();
    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    final var text = "ab\ncde";
    final var bounds =
        GraphicsUtil.getTextBounds(
            graphics, text, 0, 0, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);
    final var secondLineStart =
        GraphicsUtil.getTextCursor(
            graphics, text, 0, 0, 3, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);

    assertTrue(secondLineStart.y > bounds.y);
    assertEquals(
        3,
        GraphicsUtil.getTextPosition(
            graphics,
            text,
            secondLineStart.x,
            secondLineStart.y + secondLineStart.height / 2,
            GraphicsUtil.H_LEFT,
            GraphicsUtil.V_TOP));
    graphics.dispose();
  }

  @Test
  void selectionAcrossLinesProducesOneRectanglePerLine() {
    final var graphics =
        new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB).createGraphics();
    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

    final var selection =
        GraphicsUtil.getTextSelectionBounds(
            graphics,
            "ab\ncde",
            0,
            0,
            1,
            5,
            GraphicsUtil.H_LEFT,
            GraphicsUtil.V_TOP);

    assertEquals(2, selection.size());
    assertTrue(selection.get(1).y > selection.get(0).y);
    graphics.dispose();
  }
}
