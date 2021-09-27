/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.StringGetter;

public class Port {
  private static String defaultExclusive(String s) {
    if (s == null) throw new IllegalArgumentException("Null port type");
    else if (s.equals(INPUT)) return SHARED;
    else if (s.equals(OUTPUT)) return EXCLUSIVE;
    else if (s.equals(INOUT)) return SHARED;
    else throw new IllegalArgumentException("Not recognized port type: " + s);
  }

  private static boolean toExclusive(String s) {
    if (s == null) throw new IllegalArgumentException("Null exclusion type");
    else if (s.equals(EXCLUSIVE)) return true;
    else if (s.equals(SHARED)) return false;
    else throw new IllegalArgumentException("Not recognized exclusion type: " + s);
  }

  private static int toType(String s) {
    if (s == null) throw new IllegalArgumentException("Null port type");
    else if (s.equals(INPUT)) return EndData.INPUT_ONLY;
    else if (s.equals(OUTPUT)) return EndData.OUTPUT_ONLY;
    else if (s.equals(INOUT)) return EndData.INPUT_OUTPUT;
    else throw new IllegalArgumentException("Not recognized port type: " + s);
  }

  public static final String INPUT = "input";
  public static final String OUTPUT = "output";

  public static final String INOUT = "inout";
  public static final String EXCLUSIVE = "exclusive";
  public static final String SHARED = "shared";
  public static final String CLOCK = "clock"; // used for HDL-generation
  private final int dx;
  private final int dy;
  private final int type;
  private final BitWidth widthFixed;

  private final Attribute<BitWidth> widthAttr;

  private final boolean exclude;

  private StringGetter toolTip;

  public Port(int dx, int dy, String type, Attribute<BitWidth> attr) {
    this(dx, dy, type, attr, defaultExclusive(type));
  }

  public Port(int dx, int dy, String type, Attribute<BitWidth> attr, String exclude) {
    this.dx = dx;
    this.dy = dy;
    this.type = toType(type);
    this.widthFixed = null;
    this.widthAttr = attr;
    this.exclude = toExclusive(exclude);
    this.toolTip = null;
  }

  public Port(int dx, int dy, String type, BitWidth bits) {
    this(dx, dy, type, bits, defaultExclusive(type));
  }

  public Port(int dx, int dy, String type, BitWidth bits, String exclude) {
    this.dx = dx;
    this.dy = dy;
    this.type = toType(type);
    this.widthFixed = bits;
    this.widthAttr = null;
    this.exclude = toExclusive(exclude);
    this.toolTip = null;
  }

  public Port(int dx, int dy, String type, int bits) {
    this(dx, dy, type, BitWidth.create(bits), defaultExclusive(type));
  }

  public Port(int dx, int dy, String type, int bits, String exclude) {
    this(dx, dy, type, BitWidth.create(bits), exclude);
  }

  public BitWidth getFixedBitWidth() {
    return widthFixed == null ? BitWidth.UNKNOWN : widthFixed;
  }

  public String getToolTip() {
    StringGetter getter = toolTip;
    return getter == null ? null : getter.toString();
  }

  public int getType() {
    return type;
  }

  public Attribute<BitWidth> getWidthAttribute() {
    return widthAttr;
  }

  public void setToolTip(StringGetter value) {
    toolTip = value;
  }

  public EndData toEnd(Location loc, AttributeSet attrs) {
    final var pt = loc.translate(dx, dy);
    if (widthFixed != null) return new EndData(pt, widthFixed, type, exclude);
    final Object val = attrs.getValue(widthAttr);
    if (val instanceof BitWidth) return new EndData(pt, (BitWidth) val, type, exclude);
    throw new IllegalArgumentException("Width attribute not set");
  }
}
