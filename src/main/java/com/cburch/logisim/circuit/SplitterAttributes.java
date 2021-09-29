/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplitterAttributes extends AbstractAttributeSet {
  public static class BitOutAttribute extends Attribute<Integer> {
    final int which;
    BitOutOption[] options;

    private BitOutAttribute(int which, BitOutOption[] options) {
      super("bit" + which, S.getter("splitterBitAttr", "" + which));
      this.which = which;
      this.options = options;
    }

    private BitOutAttribute createCopy() {
      return new BitOutAttribute(which, options);
    }

    public boolean sameOptions(BitOutAttribute other) {
      if (options.length != other.options.length) return false;
      for (final var a : options) {
        var found = false;
        for (final var b : other.options) {
          if (a.toString().equals(b.toString())) {
            found = true;
            break;
          }
        }
        if (!found) return false;
      }
      return true;
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public java.awt.Component getCellEditor(Integer value) {
      final var index = value;
      final var combo = new ComboBox<>(options);
      combo.setSelectedIndex(index);
      combo.setMaximumRowCount(options.length);
      return combo;
    }

    public Object getDefault() {
      return which + 1;
    }

    @Override
    public Integer parse(String value) {
      if (value.equals(UNCHOSEN_VAL)) {
        return 0;
      } else {
        return 1 + Integer.parseInt(value);
      }
    }

    @Override
    public String toDisplayString(Integer value) {
      return options[value].toString();
    }

    @Override
    public String toStandardString(Integer value) {
      final var index = value;
      if (index == 0) {
        return UNCHOSEN_VAL;
      } else {
        return "" + (index - 1);
      }
    }
  }

  private static class BitOutOption {
    final int value;
    final boolean isVertical;
    final boolean isLast;

    BitOutOption(int value, boolean isVertical, boolean isLast) {
      this.value = value;
      this.isVertical = isVertical;
      this.isLast = isLast;
    }

    @Override
    public String toString() {
      if (value < 0) {
        return S.get("splitterBitNone");
      } else {
        var ret = "" + value;
        Direction noteDir;
        if (value == 0) {
          noteDir = isVertical ? Direction.NORTH : Direction.EAST;
        } else if (isLast) {
          noteDir = isVertical ? Direction.SOUTH : Direction.WEST;
        } else {
          noteDir = null;
        }
        if (noteDir != null) {
          ret += " (" + noteDir.toVerticalDisplayString() + ")";
        }
        return ret;
      }
    }
  }

  static byte[] computeDistribution(int fanout, int bits, int order) {
    final var ret = new byte[bits];
    if (order >= 0) {
      if (fanout >= bits) {
        for (var i = 0; i < bits; i++) ret[i] = (byte) (i + 1);
      } else {
        final var threads_per_end = bits / fanout;
        var endsWithExtra = bits % fanout;
        var curEnd = -1; // immediately increments
        var leftInEnd = 0;
        for (var i = 0; i < bits; i++) {
          if (leftInEnd == 0) {
            ++curEnd;
            leftInEnd = threads_per_end;
            if (endsWithExtra > 0) {
              ++leftInEnd;
              --endsWithExtra;
            }
          }
          ret[i] = (byte) (1 + curEnd);
          --leftInEnd;
        }
      }
    } else {
      if (fanout >= bits) {
        for (int i = 0; i < bits; i++) ret[i] = (byte) (fanout - i);
      } else {
        final var threads_per_end = bits / fanout;
        var endsWithExtra = bits % fanout;
        var curEnd = -1;
        var leftInEnd = 0;
        for (int i = bits - 1; i >= 0; i--) {
          if (leftInEnd == 0) {
            ++curEnd;
            leftInEnd = threads_per_end;
            if (endsWithExtra > 0) {
              ++leftInEnd;
              --endsWithExtra;
            }
          }
          ret[i] = (byte) (1 + curEnd);
          --leftInEnd;
        }
      }
    }
    return ret;
  }

  public static final AttributeOption APPEAR_LEGACY = new AttributeOption("legacy", S.getter("splitterAppearanceLegacy"));

  public static final Attribute<Integer> ATTR_SPACING = Attributes.forIntegerRange("spacing", S.getter("splitterSpacing"), 1, 9);

  public static final AttributeOption APPEAR_LEFT = new AttributeOption("left", S.getter("splitterAppearanceLeft"));

  public static final AttributeOption APPEAR_RIGHT = new AttributeOption("right", S.getter("splitterAppearanceRight"));
  public static final AttributeOption APPEAR_CENTER = new AttributeOption("center", S.getter("splitterAppearanceCenter"));

  public static final Attribute<AttributeOption> ATTR_APPEARANCE = Attributes.forOption("appear", S.getter("splitterAppearanceAttr"), new AttributeOption[] {APPEAR_LEFT, APPEAR_RIGHT, APPEAR_CENTER, APPEAR_LEGACY});

  public static final Attribute<BitWidth> ATTR_WIDTH = Attributes.forBitWidth("incoming", S.getter("splitterBitWidthAttr"));

  public static final Attribute<Integer> ATTR_FANOUT = Attributes.forIntegerRange("fanout", S.getter("splitterFanOutAttr"), 1, 64);

  private static final List<Attribute<?>> INIT_ATTRIBUTES = Arrays.asList(StdAttr.FACING, ATTR_FANOUT, ATTR_WIDTH, ATTR_APPEARANCE, ATTR_SPACING);

  private static final String UNCHOSEN_VAL = "none";
  private ArrayList<Attribute<?>> attrs = new ArrayList<>(INIT_ATTRIBUTES);
  private SplitterParameters parameters;
  AttributeOption appear = APPEAR_LEFT;
  Direction facing = Direction.EAST;
  int spacing = 1;
  byte fanout = 2; // number of ends this splits into
  byte[] bitEnd = new byte[2]; // how each bit maps to an end (0 if nowhere);

  // other values will be between 1 and fanout
  BitOutOption[] options = null;

  SplitterAttributes() {
    configureOptions();
    configureDefaults();
    parameters = new SplitterParameters(this);
  }

  public boolean isNoConnect(int index) {
    for (final var b : bitEnd) {
      if (b == index)
        return false;
    }
    return true;
  }

  private void configureDefaults() {
    final var offs = INIT_ATTRIBUTES.size();
    var curNum = attrs.size() - offs;

    // compute default values
    final var dflt = computeDistribution(fanout, bitEnd.length, 1);

    var changed = curNum != bitEnd.length;

    // remove excess attributes
    while (curNum > bitEnd.length) {
      curNum--;
      attrs.remove(offs + curNum);
    }

    // set existing attributes
    for (var i = 0; i < curNum; i++) {
      if (bitEnd[i] != dflt[i]) {
        final var attr = (BitOutAttribute) attrs.get(offs + i);
        bitEnd[i] = dflt[i];
        fireAttributeValueChanged(attr, (int) bitEnd[i], null);
      }
    }

    // add new attributes
    for (var i = curNum; i < bitEnd.length; i++) {
      final var attr = new BitOutAttribute(i, options);
      bitEnd[i] = dflt[i];
      attrs.add(attr);
    }

    if (changed) fireAttributeListChanged();
  }

  private void configureOptions() {
    // compute the set of options for BitOutAttributes
    options = new BitOutOption[fanout + 1];
    var isVertical = facing == Direction.EAST || facing == Direction.WEST;
    for (var i = -1; i < fanout; i++) {
      options[i + 1] = new BitOutOption(i, isVertical, i == fanout - 1);
    }

    // go ahead and set the options for the existing attributes
    final var offs = INIT_ATTRIBUTES.size();
    final var curNum = attrs.size() - offs;
    for (var i = 0; i < curNum; i++) {
      final var attr = (BitOutAttribute) attrs.get(offs + i);
      attr.options = options;
    }
  }

  @Override
  protected void copyInto(AbstractAttributeSet destObj) {
    final var dest = (SplitterAttributes) destObj;
    dest.parameters = this.parameters;
    dest.attrs = new ArrayList<>(this.attrs.size());
    dest.attrs.addAll(INIT_ATTRIBUTES);
    for (int i = INIT_ATTRIBUTES.size(), n = this.attrs.size(); i < n; i++) {
      final var attr = (BitOutAttribute) this.attrs.get(i);
      dest.attrs.add(attr.createCopy());
    }

    dest.facing = this.facing;
    dest.fanout = this.fanout;
    dest.appear = this.appear;
    dest.spacing = this.spacing;
    dest.bitEnd = this.bitEnd.clone();
    dest.options = this.options;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attrs;
  }

  Attribute<?> getBitOutAttribute(int index) {
    return attrs.get(INIT_ATTRIBUTES.size() + index);
  }

  public SplitterParameters getParameters() {
    if (parameters == null) parameters = new SplitterParameters(this);
    return parameters;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == StdAttr.FACING) {
      return (V) facing;
    } else if (attr == ATTR_FANOUT) {
      return (V) Integer.valueOf(fanout);
    } else if (attr == ATTR_WIDTH) {
      return (V) BitWidth.create(bitEnd.length);
    } else if (attr == ATTR_APPEARANCE) {
      return (V) appear;
    } else if (attr == ATTR_SPACING) {
      return (V) Integer.valueOf(spacing);
    } else if (attr instanceof BitOutAttribute bitOut) {
      return (V) Integer.valueOf(bitEnd[bitOut.which]);
    } else {
      return null;
    }
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == StdAttr.FACING) {
      final var newFacing = (Direction) value;
      if (facing.equals(newFacing)) return;
      facing = (Direction) value;
      configureOptions();
      parameters = null;
    } else if (attr == ATTR_FANOUT) {
      int newValue = (Integer) value;
      final var bits = bitEnd;
      for (var i = 0; i < bits.length; i++) {
        if (bits[i] > newValue) bits[i] = (byte) newValue;
      }
      if (fanout == (byte) newValue) return;
      fanout = (byte) newValue;
      configureOptions();
      configureDefaults();
      parameters = null;
    } else if (attr == ATTR_WIDTH) {
      final var width = (BitWidth) value;
      if (bitEnd.length == width.getWidth()) return;
      bitEnd = new byte[width.getWidth()];
      configureOptions();
      configureDefaults();
    } else if (attr == ATTR_SPACING) {
      final var s = (Integer) value;
      if (s == spacing) return;
      spacing = s;
      parameters = null;
    } else if (attr == ATTR_APPEARANCE) {
      final var appearance = (AttributeOption) value;
      if (appear.equals(appearance)) return;
      appear = appearance;
      parameters = null;
    } else if (attr instanceof BitOutAttribute bitOutAttr) {
      int val = (value instanceof Integer) ? (Integer) value : ((BitOutOption) value).value + 1;
      if (val >= 0 && val <= fanout) {
        if (bitEnd[bitOutAttr.which] == (byte) val) return;
        bitEnd[bitOutAttr.which] = (byte) val;
      } else return;
    } else {
      throw new IllegalArgumentException("unknown attribute " + attr);
    }
    fireAttributeValueChanged(attr, value, null);
  }
}
