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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import javax.swing.JTextField;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class EditableLabel implements Cloneable {
  public static final int LEFT = JTextField.LEFT;
  public static final int RIGHT = JTextField.RIGHT;
  public static final int CENTER = JTextField.CENTER;

  public static final int TOP = 8;
  public static final int MIDDLE = 9;
  public static final int BASELINE = 10;
  public static final int BOTTOM = 11;

  @Getter private int x;
  @Getter private int y;
  @Getter private String text;
  @Getter private Font font;
  @Getter @Setter private Color color;
  @Getter private int horizontalAlignment;
  @Getter private int verticalAlignment;
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
    this.horizontalAlignment = LEFT;
    this.verticalAlignment = BASELINE;
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
    val tm = new TextMetrics(g, text);
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

    val dim = field.getPreferredSize();
    int w;
    val border = EditableLabelField.FIELD_BORDER;
    if (dimsKnown) {
      w = width + 1 + 2 * border;
    } else {
      val tm = new TextMetrics(field, this.font, text);
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
    val h = dim.height;
    x0 = switch (horizontalAlignment) {
      case LEFT -> x0 - border;
      case CENTER -> x0 - (w / 2.0f) + 1;
      case RIGHT -> x0 - w + border + 1;
      default -> x0 - border;
    };
    y0 = y0 - border;

    field.setHorizontalAlignment(horizontalAlignment);
    field.setForeground(color);
    field.setBounds((int) x0, (int) y0, w, h);
  }

  public boolean contains(int qx, int qy) {
    val x0 = getLeftX();
    val y0 = getBaseY();
    return (qx >= x0 && qx < x0 + width && qy >= y0 - ascent && qy < y0 + descent);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof EditableLabel) {
      val that = (EditableLabel) other;
      return this.x == that.x
          && this.y == that.y
          && this.text.equals(that.text)
          && this.font.equals(that.font)
          && this.color.equals(that.color)
          && this.horizontalAlignment == that.horizontalAlignment
          && this.verticalAlignment == that.verticalAlignment;
    }
    return false;
  }

  private float getBaseY() {
    return switch (verticalAlignment) {
      case TOP -> y + ascent;
      case MIDDLE -> y + (ascent - descent) / 2.0f;
      case BASELINE -> y;
      case BOTTOM -> y - descent;
      default -> y;
    };
  }

  //
  // more complex methods
  //
  public Bounds getBounds() {
    val x0 = (int) getLeftX();
    val y0 = (int) getBaseY() - ascent;
    val w = width;
    val h = ascent + descent;
    return Bounds.create(x0, y0, w, h);
  }

  public void setFont(Font value) {
    font = value;
    dimsKnown = false;
  }

  public void setHorizontalAlignment(int value) {
    if (value != LEFT && value != CENTER && value != RIGHT) {
      throw new IllegalArgumentException("argument must be LEFT, CENTER, or RIGHT");
    }
    horizontalAlignment = value;
    dimsKnown = false;
  }

  private float getLeftX() {
    return switch (horizontalAlignment) {
      case LEFT -> x;
      case CENTER -> x - width / 2.0f;
      case RIGHT -> x - width;
      default -> x;
    };
  }

  public void setText(String value) {
    dimsKnown = false;
    text = value;
  }

  public void setVerticalAlignment(int value) {
    if (value != TOP && value != MIDDLE && value != BASELINE && value != BOTTOM) {
      throw new IllegalArgumentException("argument must be TOP, MIDDLE, BASELINE, or BOTTOM");
    }
    verticalAlignment = value;
    dimsKnown = false;
  }

  //
  // accessor methods
  //
  @Override
  public int hashCode() {
    int ret = x * 31 + y;
    ret = ret * 31 + text.hashCode();
    ret = ret * 31 + font.hashCode();
    ret = ret * 31 + color.hashCode();
    ret = ret * 31 + horizontalAlignment;
    ret = ret * 31 + verticalAlignment;
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
