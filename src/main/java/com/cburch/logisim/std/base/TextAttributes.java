/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.base;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;

import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TextAttributes extends AbstractAttributeSet {
  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          Text.ATTR_TEXT, Text.ATTR_FONT, Text.ATTR_COLOR, Text.ATTR_HALIGN, Text.ATTR_VALIGN);

  private String text;
  private Font font;
  private Color color;
  private AttributeOption halign;
  private AttributeOption valign;
  private Bounds offsetBounds;

  public TextAttributes() {
    text = "";
    font = StdAttr.DEFAULT_LABEL_FONT;
    color = Color.BLACK;
    halign = Text.ATTR_HALIGN.parse("center");
    valign = Text.ATTR_VALIGN.parse("base");
    offsetBounds = null;
  }

  @Override
  protected void copyInto(AbstractAttributeSet destObj) {
    // nothing to do
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  Font getFont() {
    return font;
  }

  int getHorizontalAlign() {
    return (Integer) halign.getValue();
  }

  Bounds getOffsetBounds() {
    return offsetBounds;
  }

  String getText() {
    return text;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == Text.ATTR_TEXT) return (V) text;
    if (attr == Text.ATTR_FONT) return (V) font;
    if (attr == Text.ATTR_HALIGN) return (V) halign;
    if (attr == Text.ATTR_VALIGN) return (V) valign;
    if (attr == Text.ATTR_COLOR) return (V) color;
    return null;
  }

  int getVerticalAlign() {
    return (Integer) valign.getValue();
  }

  boolean setOffsetBounds(Bounds value) {
    Bounds old = offsetBounds;
    boolean same = Objects.equals(old, value);
    if (!same) {
      offsetBounds = value;
    }
    return !same;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == Text.ATTR_TEXT) {
      text = (String) value;
    } else if (attr == Text.ATTR_FONT) {
      font = (Font) value;
    } else if (attr == Text.ATTR_HALIGN) {
      halign = (AttributeOption) value;
    } else if (attr == Text.ATTR_VALIGN) {
      valign = (AttributeOption) value;
    } else if (attr == Text.ATTR_COLOR) {
      color = (Color) value;
    } else {
      throw new IllegalArgumentException("unknown attribute");
    }
    offsetBounds = null;
    fireAttributeValueChanged(attr, value, null);
  }
}
