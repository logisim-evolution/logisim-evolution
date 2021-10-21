/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.LinkedList;
import lombok.Getter;
import lombok.Setter;

public class TextField {
  public static final int H_LEFT = GraphicsUtil.H_LEFT;
  public static final int H_CENTER = GraphicsUtil.H_CENTER;
  public static final int H_RIGHT = GraphicsUtil.H_RIGHT;
  public static final int V_TOP = GraphicsUtil.V_TOP;
  public static final int V_CENTER = GraphicsUtil.V_CENTER;
  public static final int V_CENTER_OVERALL = GraphicsUtil.V_CENTER_OVERALL;
  public static final int V_BASELINE = GraphicsUtil.V_BASELINE;
  public static final int V_BOTTOM = GraphicsUtil.V_BOTTOM;

  @Getter private int x;
  @Getter private int y;
  @Getter @Setter private int horizontalAlign;
  @Getter @Setter private int verticalAlign;
  @Getter @Setter private Font font;
  @Getter private String text = "";
  private final LinkedList<TextFieldListener> listeners = new LinkedList<>();

  public TextField(int x, int y, int horizontalAlign, int verticalAlign) {
    this(x, y, horizontalAlign, verticalAlign, null);
  }

  public TextField(int x, int y, int horizontalAlign, int verticalAlign, Font font) {
    this.x = x;
    this.y = y;
    this.horizontalAlign = horizontalAlign;
    this.verticalAlign = verticalAlign;
    this.font = font;
  }

  //
  // listener methods
  //
  public void addTextFieldListener(TextFieldListener l) {
    listeners.add(l);
  }

  public void draw(Graphics g) {
    final var old = g.getFont();
    if (font != null) g.setFont(font);

    var x = this.x;
    var y = this.y;
    final var fm = g.getFontMetrics();
    final var width = fm.stringWidth(text);
    final var ascent = fm.getAscent();
    final var descent = fm.getDescent();
    switch (horizontalAlign) {
      case TextField.H_CENTER:
        x -= width / 2;
        break;
      case TextField.H_RIGHT:
        x -= width;
        break;
      default:
        break;
    }
    switch (verticalAlign) {
      case TextField.V_TOP:
        y += ascent;
        break;
      case TextField.V_CENTER:
        y += ascent / 2;
        break;
      case TextField.V_CENTER_OVERALL:
        y += (ascent - descent) / 2;
        break;
      case TextField.V_BOTTOM:
        y -= descent;
        break;
      default:
        break;
    }
    g.drawString(text, x, y);
    g.setFont(old);
  }

  public void fireTextChanged(TextFieldEvent e) {
    for (final var l : new ArrayList<>(listeners)) {
      l.textChanged(e);
    }
  }

  public Bounds getBounds(Graphics g) {
    var x = this.x;
    var y = this.y;
    final var fm = (font == null) ? g.getFontMetrics() : g.getFontMetrics(font);
    final var width = fm.stringWidth(text);
    final var ascent = fm.getAscent();
    final var descent = fm.getDescent();

    switch (horizontalAlign) {
      case TextField.H_CENTER:
        x -= width / 2;
        break;
      case TextField.H_RIGHT:
        x -= width;
        break;
      default:
        break;
    }

    switch (verticalAlign) {
      case TextField.V_TOP:
        y += ascent;
        break;
      case TextField.V_CENTER:
        y += ascent / 2;
        break;
      case TextField.V_CENTER_OVERALL:
        y += (ascent - descent) / 2;
        break;
      case TextField.V_BOTTOM:
        y -= descent;
        break;
      default:
        break;
    }
    return Bounds.create(x, y - ascent, width, ascent + descent);
  }

  public TextFieldCaret getCaret(Graphics g, int pos) {
    return new TextFieldCaret(this, g, pos);
  }

  //
  // graphics methods
  //
  public TextFieldCaret getCaret(Graphics g, int x, int y) {
    return new TextFieldCaret(this, g, x, y);
  }

  //
  // access methods
  //
  public void removeTextFieldListener(TextFieldListener l) {
    listeners.remove(l);
  }

  public void setAlign(int halign, int valign) {
    this.horizontalAlign = halign;
    this.verticalAlign = valign;
  }

  public void setLocation(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public void setLocation(int x, int y, int halign, int valign) {
    this.x = x;
    this.y = y;
    this.horizontalAlign = halign;
    this.verticalAlign = valign;
  }

  //
  // modification methods
  //
  public void setText(String text) {
    if (!text.equals(this.text)) {
      final var e = new TextFieldEvent(this, this.text, text);
      this.text = text;
      fireTextChanged(e);
    }
  }

}
