/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.util;

import com.cburch.logisim.data.Bounds;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import javax.swing.JTextField;

public class EditableLabel implements Cloneable {
  public static final int LEFT = JTextField.LEFT;
  public static final int RIGHT = JTextField.RIGHT;
  public static final int CENTER = JTextField.CENTER;

  public static final int TOP = 8;
  public static final int MIDDLE = 9;
  public static final int BASELINE = 10;
  public static final int BOTTOM = 11;

  private int x;
  private int y;
  private String text;
  private Font font;
  private Color color;
  private int horzAlign;
  private int vertAlign;
  private boolean dimsKnown;
  private int width;
  private int ascent;
  private int descent;

  public EditableLabel(int x, int y, String text, Font font) {
    this.x = x;
    this.y = y;
    this.text = text;
    this.font = font;
    this.color = Color.BLACK;
    this.horzAlign = LEFT;
    this.vertAlign = BASELINE;
    this.dimsKnown = false;
  }

  @Override
  public EditableLabel clone() {
    try {
      return (EditableLabel) super.clone();
    } catch (CloneNotSupportedException e) {
      return new EditableLabel(x, y, text, font);
    }
  }

  private void computeDimensions(Graphics g) {
    TextMetrics tm = new TextMetrics(g, text);
    width = tm.width;
    ascent = tm.ascent;
    descent = tm.descent;
    dimsKnown = true;
  }

  public void configureTextField(EditableLabelField field) {
    configureTextField(field, 1.0);
  }

  public void configureTextField(EditableLabelField field, double zoom) {
    var font = this.font;
    if (zoom != 1.0) {
      font = font.deriveFont(AffineTransform.getScaleInstance(zoom, zoom));
    }
    field.setFont(font);

    final var dim = field.getPreferredSize();
    int w;
    final var border = EditableLabelField.FIELD_BORDER;
    if (dimsKnown) {
      w = width + 1 + 2 * border;
    } else {
      final var tm = new TextMetrics(field, this.font, text);
      ascent = tm.ascent;
      descent = tm.descent;
      w = tm.width;
    }

    float x0 = x;
    float y0 = getBaseY() - ascent;
    if (zoom != 1.0) {
      x0 = (int) Math.round(x0 * zoom);
      y0 = (int) Math.round(y0 * zoom);
      w = (int) Math.round(w * zoom);
    }

    w = Math.max(w, dim.width);
    final int h = dim.height;
    x0 = switch (horzAlign) {
      case LEFT -> x0 - border;
      case CENTER -> x0 - (w / 2.0F) + 1;
      case RIGHT -> x0 - w + border + 1;
      default -> x0 - border;
    };
    y0 = y0 - border;

    field.setHorizontalAlignment(horzAlign);
    field.setForeground(color);
    field.setBounds((int) x0, (int) y0, w, h);
  }

  public boolean contains(int qx, int qy) {
    float x0 = getLeftX();
    float y0 = getBaseY();
    return (qx >= x0 && qx < x0 + width && qy >= y0 - ascent && qy < y0 + descent);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof EditableLabel that) {
      return this.x == that.x
          && this.y == that.y
          && this.text.equals(that.text)
          && this.font.equals(that.font)
          && this.color.equals(that.color)
          && this.horzAlign == that.horzAlign
          && this.vertAlign == that.vertAlign;
    } else {
      return false;
    }
  }

  private float getBaseY() {
    return switch (vertAlign) {
      case TOP -> y + ascent;
      case MIDDLE -> y + (ascent - descent) / 2.0F;
      case BASELINE -> y;
      case BOTTOM -> y - descent;
      default -> y;
    };
  }

  //
  // more complex methods
  //
  public Bounds getBounds() {
    int x0 = (int) getLeftX();
    int y0 = (int) getBaseY() - ascent;
    int w = width;
    int h = ascent + descent;
    return Bounds.create(x0, y0, w, h);
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color value) {
    color = value;
  }

  public Font getFont() {
    return font;
  }

  public void setFont(Font value) {
    font = value;
    dimsKnown = false;
  }

  public int getHorizontalAlignment() {
    return horzAlign;
  }

  public void setHorizontalAlignment(int value) {
    if (value != LEFT && value != CENTER && value != RIGHT) {
      throw new IllegalArgumentException("argument must be LEFT, CENTER, or RIGHT");
    }
    horzAlign = value;
    dimsKnown = false;
  }

  private float getLeftX() {
    return switch (horzAlign) {
      case LEFT -> x;
      case CENTER -> x - width / 2.0F;
      case RIGHT -> x - width;
      default -> x;
    };
  }

  public String getText() {
    return text;
  }

  public void setText(String value) {
    dimsKnown = false;
    text = value;
  }

  public int getVerticalAlignment() {
    return vertAlign;
  }

  public void setVerticalAlignment(int value) {
    if (value != TOP && value != MIDDLE && value != BASELINE && value != BOTTOM) {
      throw new IllegalArgumentException("argument must be TOP, MIDDLE, BASELINE, or BOTTOM");
    }
    vertAlign = value;
    dimsKnown = false;
  }

  //
  // accessor methods
  //
  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  @Override
  public int hashCode() {
    int ret = x * 31 + y;
    ret = ret * 31 + text.hashCode();
    ret = ret * 31 + font.hashCode();
    ret = ret * 31 + color.hashCode();
    ret = ret * 31 + horzAlign;
    ret = ret * 31 + vertAlign;
    return ret;
  }

  public void paint(Graphics g) {
    g.setFont(font);
    g.setColor(color);
    computeDimensions(g);
    float x0 = getLeftX();
    float y0 = getBaseY();
    ((Graphics2D) g).drawString(text, x0, y0);
  }

  public void setLocation(int x, int y) {
    this.x = x;
    this.y = y;
  }
}
