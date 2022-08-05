/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class TunnelAttributes extends AbstractAttributeSet {
  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(StdAttr.FACING, StdAttr.WIDTH, StdAttr.LABEL, StdAttr.LABEL_FONT);

  private Direction facing;
  private BitWidth width;
  private String label;
  private Font labelFont;
  private Bounds offsetBounds;
  private int labelX;
  private int labelY;
  private int labelHAlign;
  private int labelVAlign;

  public TunnelAttributes() {
    facing = Direction.WEST;
    width = BitWidth.ONE;
    label = "";
    labelFont = StdAttr.DEFAULT_LABEL_FONT;
    offsetBounds = null;
    configureLabel();
  }

  private void configureLabel() {
    Direction facing = this.facing;
    int x;
    int y;
    int halign;
    int valign;
    int margin = Tunnel.ARROW_MARGIN;
    if (facing == Direction.NORTH) {
      x = 0;
      y = margin;
      halign = TextField.H_CENTER;
      valign = TextField.V_TOP;
    } else if (facing == Direction.SOUTH) {
      x = 0;
      y = -margin;
      halign = TextField.H_CENTER;
      valign = TextField.V_BOTTOM;
    } else if (facing == Direction.EAST) {
      x = -margin;
      y = 0;
      halign = TextField.H_RIGHT;
      valign = TextField.V_CENTER_OVERALL;
    } else {
      x = margin;
      y = 0;
      halign = TextField.H_LEFT;
      valign = TextField.V_CENTER_OVERALL;
    }
    labelX = x;
    labelY = y;
    labelHAlign = halign;
    labelVAlign = valign;
  }

  @Override
  protected void copyInto(AbstractAttributeSet destObj) {
    // nothing to do
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  Direction getFacing() {
    return facing;
  }

  Font getFont() {
    return labelFont;
  }

  String getLabel() {
    return label;
  }

  int getLabelHAlign() {
    return labelHAlign;
  }

  int getLabelVAlign() {
    return labelVAlign;
  }

  int getLabelX() {
    return labelX;
  }

  int getLabelY() {
    return labelY;
  }

  Bounds getOffsetBounds() {
    return offsetBounds;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == StdAttr.FACING) return (V) facing;
    if (attr == StdAttr.WIDTH) return (V) width;
    if (attr == StdAttr.LABEL) return (V) label;
    if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
    return null;
  }

  boolean setOffsetBounds(Bounds value) {
    Bounds old = offsetBounds;
    boolean same = Objects.equals(old, value);
    if (!same) {
      offsetBounds = value;
    }
    return !same;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    V Oldvalue = null;
    if (attr == StdAttr.FACING) {
      facing = (Direction) value;
      configureLabel();
    } else if (attr == StdAttr.WIDTH) {
      width = (BitWidth) value;
    } else if (attr == StdAttr.LABEL) {
      String val = (String) value;
      Oldvalue = (V) label;
      label = val;
    } else if (attr == StdAttr.LABEL_FONT) {
      labelFont = (Font) value;
    } else {
      throw new IllegalArgumentException("unknown attribute");
    }
    offsetBounds = null;
    fireAttributeValueChanged(attr, value, Oldvalue);
  }
}
