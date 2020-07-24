/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
    int which;
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
      for (BitOutOption a : options) {
        boolean found = false;
        for (BitOutOption b : other.options) {
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
      int index = value.intValue();
      ComboBox combo = new ComboBox<>(options);
      combo.setSelectedIndex(index);
      combo.setMaximumRowCount(options.length);
      return combo;
    }

    public Object getDefault() {
      return Integer.valueOf(which + 1);
    }

    @Override
    public Integer parse(String value) {
      if (value.equals(unchosen_val)) {
        return Integer.valueOf(0);
      } else {
        return Integer.valueOf(1 + Integer.parseInt(value));
      }
    }

    @Override
    public String toDisplayString(Integer value) {
      int index = value.intValue();
      return options[index].toString();
    }

    @Override
    public String toStandardString(Integer value) {
      int index = value.intValue();
      if (index == 0) {
        return unchosen_val;
      } else {
        return "" + (index - 1);
      }
    }
  }

  private static class BitOutOption {
    int value;
    boolean isVertical;
    boolean isLast;

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
        String ret = "" + value;
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
    byte[] ret = new byte[bits];
    if (order >= 0) {
      if (fanout >= bits) {
        for (int i = 0; i < bits; i++) ret[i] = (byte) (i + 1);
      } else {
        int threads_per_end = bits / fanout;
        int ends_with_extra = bits % fanout;
        int cur_end = -1; // immediately increments
        int left_in_end = 0;
        for (int i = 0; i < bits; i++) {
          if (left_in_end == 0) {
            ++cur_end;
            left_in_end = threads_per_end;
            if (ends_with_extra > 0) {
              ++left_in_end;
              --ends_with_extra;
            }
          }
          ret[i] = (byte) (1 + cur_end);
          --left_in_end;
        }
      }
    } else {
      if (fanout >= bits) {
        for (int i = 0; i < bits; i++) ret[i] = (byte) (fanout - i);
      } else {
        int threads_per_end = bits / fanout;
        int ends_with_extra = bits % fanout;
        int cur_end = -1;
        int left_in_end = 0;
        for (int i = bits - 1; i >= 0; i--) {
          if (left_in_end == 0) {
            ++cur_end;
            left_in_end = threads_per_end;
            if (ends_with_extra > 0) {
              ++left_in_end;
              --ends_with_extra;
            }
          }
          ret[i] = (byte) (1 + cur_end);
          --left_in_end;
        }
      }
    }
    return ret;
  }

  public static final AttributeOption APPEAR_LEGACY =
      new AttributeOption("legacy", S.getter("splitterAppearanceLegacy"));

  public static final Attribute<Integer> ATTR_SPACING =
      Attributes.forIntegerRange("spacing", S.getter("splitterSpacing"), 1, 9);

  public static final AttributeOption APPEAR_LEFT =
      new AttributeOption("left", S.getter("splitterAppearanceLeft"));

  public static final AttributeOption APPEAR_RIGHT =
      new AttributeOption("right", S.getter("splitterAppearanceRight"));
  public static final AttributeOption APPEAR_CENTER =
      new AttributeOption("center", S.getter("splitterAppearanceCenter"));

  public static final Attribute<AttributeOption> ATTR_APPEARANCE =
      Attributes.forOption(
          "appear",
          S.getter("splitterAppearanceAttr"),
          new AttributeOption[] {APPEAR_LEFT, APPEAR_RIGHT, APPEAR_CENTER, APPEAR_LEGACY});

  public static final Attribute<BitWidth> ATTR_WIDTH =
      Attributes.forBitWidth("incoming", S.getter("splitterBitWidthAttr"));

  public static final Attribute<Integer> ATTR_FANOUT =
      Attributes.forIntegerRange("fanout", S.getter("splitterFanOutAttr"), 1, 64);

  private static final List<Attribute<?>> INIT_ATTRIBUTES =
      Arrays.asList(
          new Attribute<?>[] {
            StdAttr.FACING, ATTR_FANOUT, ATTR_WIDTH, ATTR_APPEARANCE, ATTR_SPACING
          });

  private static final String unchosen_val = "none";
  private ArrayList<Attribute<?>> attrs = new ArrayList<Attribute<?>>(INIT_ATTRIBUTES);
  private SplitterParameters parameters;
  AttributeOption appear = APPEAR_LEFT;
  Direction facing = Direction.EAST;
  int spacing = 1;
  byte fanout = 2; // number of ends this splits into
  byte[] bit_end = new byte[2]; // how each bit maps to an end (0 if nowhere);

  // other values will be between 1 and fanout
  BitOutOption[] options = null;

  SplitterAttributes() {
    configureOptions();
    configureDefaults();
    parameters = new SplitterParameters(this);
  }
  
  public boolean isNoConnect(int index) {
    for (int i = 0; i < bit_end.length; i++) {
      if (bit_end[i] == index)
        return false;
    }
    return true;
  }

  private void configureDefaults() {
    int offs = INIT_ATTRIBUTES.size();
    int curNum = attrs.size() - offs;

    // compute default values
    byte[] dflt = computeDistribution(fanout, bit_end.length, 1);

    boolean changed = curNum != bit_end.length;

    // remove excess attributes
    while (curNum > bit_end.length) {
      curNum--;
      attrs.remove(offs + curNum);
    }

    // set existing attributes
    for (int i = 0; i < curNum; i++) {
      if (bit_end[i] != dflt[i]) {
        BitOutAttribute attr = (BitOutAttribute) attrs.get(offs + i);
        bit_end[i] = dflt[i];
        fireAttributeValueChanged(attr, Integer.valueOf(bit_end[i]), null);
      }
    }

    // add new attributes
    for (int i = curNum; i < bit_end.length; i++) {
      BitOutAttribute attr = new BitOutAttribute(i, options);
      bit_end[i] = dflt[i];
      attrs.add(attr);
    }

    if (changed) fireAttributeListChanged();
  }

  private void configureOptions() {
    // compute the set of options for BitOutAttributes
    options = new BitOutOption[fanout + 1];
    boolean isVertical = facing == Direction.EAST || facing == Direction.WEST;
    for (int i = -1; i < fanout; i++) {
      options[i + 1] = new BitOutOption(i, isVertical, i == fanout - 1);
    }

    // go ahead and set the options for the existing attributes
    int offs = INIT_ATTRIBUTES.size();
    int curNum = attrs.size() - offs;
    for (int i = 0; i < curNum; i++) {
      BitOutAttribute attr = (BitOutAttribute) attrs.get(offs + i);
      attr.options = options;
    }
  }

  @Override
  protected void copyInto(AbstractAttributeSet destObj) {
    SplitterAttributes dest = (SplitterAttributes) destObj;
    dest.parameters = this.parameters;
    dest.attrs = new ArrayList<Attribute<?>>(this.attrs.size());
    dest.attrs.addAll(INIT_ATTRIBUTES);
    for (int i = INIT_ATTRIBUTES.size(), n = this.attrs.size(); i < n; i++) {
      BitOutAttribute attr = (BitOutAttribute) this.attrs.get(i);
      dest.attrs.add(attr.createCopy());
    }

    dest.facing = this.facing;
    dest.fanout = this.fanout;
    dest.appear = this.appear;
    dest.spacing = this.spacing;
    dest.bit_end = this.bit_end.clone();
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
    SplitterParameters ret = parameters;
    if (ret == null) {
      ret = new SplitterParameters(this);
      parameters = ret;
    }
    return ret;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == StdAttr.FACING) {
      return (V) facing;
    } else if (attr == ATTR_FANOUT) {
      return (V) Integer.valueOf(fanout);
    } else if (attr == ATTR_WIDTH) {
      return (V) BitWidth.create(bit_end.length);
    } else if (attr == ATTR_APPEARANCE) {
      return (V) appear;
    } else if (attr == ATTR_SPACING) {
      return (V) Integer.valueOf(spacing);
    } else if (attr instanceof BitOutAttribute) {
      BitOutAttribute bitOut = (BitOutAttribute) attr;
      return (V) Integer.valueOf(bit_end[bitOut.which]);
    } else {
      return null;
    }
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == StdAttr.FACING) {
      Direction NewFacing = (Direction) value;
      if (facing.equals(NewFacing)) return;
      facing = (Direction) value;
      configureOptions();
      parameters = null;
    } else if (attr == ATTR_FANOUT) {
      int newValue = ((Integer) value).intValue();
      byte[] bits = bit_end;
      for (int i = 0; i < bits.length; i++) {
        if (bits[i] > newValue) bits[i] = (byte) newValue;
      }
      if (fanout == (byte) newValue) return;
      fanout = (byte) newValue;
      configureOptions();
      configureDefaults();
      parameters = null;
    } else if (attr == ATTR_WIDTH) {
      BitWidth width = (BitWidth) value;
      if (bit_end.length == width.getWidth()) return;
      bit_end = new byte[width.getWidth()];
      configureOptions();
      configureDefaults();
    } else if (attr == ATTR_SPACING) {
      int s = (Integer) value;
      if (s == spacing) return;
      spacing = s;
      parameters = null;
    } else if (attr == ATTR_APPEARANCE) {
      AttributeOption appearance = (AttributeOption) value;
      if (appear.equals(appearance)) return;
      appear = appearance;
      parameters = null;
    } else if (attr instanceof BitOutAttribute) {
      BitOutAttribute bitOutAttr = (BitOutAttribute) attr;
      int val;
      if (value instanceof Integer) {
        val = ((Integer) value).intValue();
      } else {
        val = ((BitOutOption) value).value + 1;
      }
      if (val >= 0 && val <= fanout) {
        if (bit_end[bitOutAttr.which] == (byte) val) return;
        bit_end[bitOutAttr.which] = (byte) val;
      } else return;
    } else {
      throw new IllegalArgumentException("unknown attribute " + attr);
    }
    fireAttributeValueChanged(attr, value, null);
  }
}
