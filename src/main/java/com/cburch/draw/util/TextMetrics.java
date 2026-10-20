/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.util;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class TextMetrics {

  private static final Canvas canvas = new Canvas();
  public int ascent;
  public int descent;
  public int leading;
  public int height; // = ascent + height + leading
  public int width; // valid only if constructor was given a string

  public TextMetrics(Graphics g) {
    this(g, null, null);
  }

  public TextMetrics(Graphics g, String text) {
    this(g, null, text);
  }

  public TextMetrics(Graphics g, Font font) {
    this(g, font, null);
  }

  public TextMetrics(Graphics g, Font font, String text) {
    if (g == null) {
      throw new IllegalStateException("need g");
    }
    if (font == null) font = g.getFont();
    final var fr = ((Graphics2D) g).getFontRenderContext();

    if (text == null) {
      text = "ÄAy";
      width = 0;
    } else {
      width = (int) font.getStringBounds(text, fr).getWidth();
    }

    final var lm = font.getLineMetrics(text, fr);
    ascent = (int) Math.ceil(lm.getAscent());
    descent = (int) Math.ceil(lm.getDescent());
    leading = (int) Math.ceil(lm.getLeading());
    height = ascent + descent + leading;
  }

  public TextMetrics(Component c, Font font, String text) {
    if (c == null) c = canvas;
    if (font == null) font = c.getFont();
    final var fm = c.getFontMetrics(font);
    width = (text != null ? fm.stringWidth(text) : 0);
    ascent = fm.getAscent();
    descent = fm.getDescent();
    leading = 0;
    height = ascent + descent + leading;
  }
}
