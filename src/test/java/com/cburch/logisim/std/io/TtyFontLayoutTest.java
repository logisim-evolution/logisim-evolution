/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.data.Bounds;
import java.awt.Font;
import org.junit.jupiter.api.Test;

class TtyFontLayoutTest {

  private static final Font LARGE_FONT = new Font("monospaced", Font.PLAIN, 24);

  @Test
  void defaultFontIsTheHistoricalMonospacedFont() {
    final var tty = new Tty();
    final var attrs = tty.createAttributeSet();

    assertEquals(Tty.DEFAULT_FONT, attrs.getValue(Tty.ATTR_FONT));
  }

  @Test
  void defaultFontAttributeIsOmittedFromSavedFiles() {
    // XmlWriter skips an attribute whose value equals the factory default, so existing files that
    // predate this attribute round-trip unchanged and new files only store a customized font.
    final var tty = new Tty();
    final var dflt = tty.getDefaultAttributeValue(Tty.ATTR_FONT, LogisimVersion.fromString("3.9.0"));

    assertEquals(Tty.DEFAULT_FONT, dflt);
  }

  @Test
  void fontAttributeRoundTripsThroughItsStandardString() {
    // .circ serialization stores and reloads a font through toStandardString/parse, so a parsed
    // font must serialize back to the same standard string (family, style, and size preserved).
    final var standard = Tty.ATTR_FONT.toStandardString(LARGE_FONT);
    final var parsed = Tty.ATTR_FONT.parse(standard);

    assertEquals(standard, Tty.ATTR_FONT.toStandardString(parsed));
    assertEquals(LARGE_FONT.getSize(), parsed.getSize());
    assertEquals(LARGE_FONT.getStyle(), parsed.getStyle());
    assertEquals(LARGE_FONT.getFamily(), parsed.getFamily());
  }

  @Test
  void largerFontGrowsTheComponentBounds() {
    final var tty = new Tty();
    final var attrs = tty.createAttributeSet();

    final var defaultBounds = tty.getOffsetBounds(attrs);

    attrs.setValue(Tty.ATTR_FONT, LARGE_FONT);
    final var largeBounds = tty.getOffsetBounds(attrs);

    assertTrue(
        largeBounds.getWidth() > defaultBounds.getWidth(),
        "a larger font must widen the component");
    assertTrue(
        largeBounds.getHeight() > defaultBounds.getHeight(),
        "a larger font must heighten the component");
  }

  @Test
  void defaultFontLayoutMatchesTheLegacyRowHeight() {
    // The legacy layout used a fixed ROW_HEIGHT and a BORDER on each side. The default monospaced
    // 11-point font must not shrink that height, so existing circuits keep their footprint.
    final var tty = new Tty();
    final var attrs = tty.createAttributeSet();
    final var rows = Tty.resolveRowCount(attrs);

    final var bounds = tty.getOffsetBounds(attrs);
    assertEquals(2 * Tty.BORDER + rows * Tty.ROW_HEIGHT, bounds.getHeight());
  }

  @Test
  void ordinaryAndSharedLayoutAgree() {
    // TtyShape delegates to Tty.computeBounds, so the dynamic appearance and the ordinary component
    // must produce identical dimensions for the same rows, columns, and font.
    final var tty = new Tty();
    final var attrs = tty.createAttributeSet();
    attrs.setValue(Tty.ATTR_FONT, LARGE_FONT);

    final var rows = Tty.resolveRowCount(attrs);
    final var cols = Tty.resolveColumnCount(attrs);

    final var ordinary = tty.getOffsetBounds(attrs);
    final var shared = Tty.computeBounds(0, 0, rows, cols, LARGE_FONT);

    assertEquals(ordinary.getWidth(), shared.getWidth());
    assertEquals(ordinary.getHeight(), shared.getHeight());
  }

  @Test
  void columnWidthUsesTheWidestPrintableAsciiAdvance() {
    // A column is sized from the maximum advance over printable ASCII (32..126), never smaller than
    // the advance of any single printable character such as 'W' or 'm'.
    final var image = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    final var g = image.getGraphics();
    g.setFont(LARGE_FONT);
    final var fm = g.getFontMetrics();

    final var colWidth = Tty.getColWidth(fm);
    for (var c = 32; c <= 126; c++) {
      assertTrue(colWidth >= fm.charWidth(c), "column must fit every printable ASCII glyph");
    }
    g.dispose();

    final var bounds = Tty.computeBounds(0, 0, 1, 10, LARGE_FONT);
    assertEquals(2 * Tty.BORDER + 10 * colWidth, bounds.getWidth());
  }
}
