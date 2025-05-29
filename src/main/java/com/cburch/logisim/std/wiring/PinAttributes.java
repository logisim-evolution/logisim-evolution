/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;

import java.util.Arrays;
import java.util.List;

class PinAttributes extends ProbeAttributes {
  public static PinAttributes instance = new PinAttributes();
  private static final List<Attribute<?>> INPIN_ATTRIBUTES = Arrays.asList(StdAttr.FACING, Pin.ATTR_TYPE,
      StdAttr.WIDTH, Pin.ATTR_BEHAVIOR, StdAttr.LABEL, StdAttr.LABEL_LOC, StdAttr.LABEL_FONT,
      RadixOption.ATTRIBUTE, PROBEAPPEARANCE, Pin.ATTR_INITIAL);
  private static final List<Attribute<?>> TRISTATE_ATTRIBUTES = Arrays.asList(StdAttr.FACING, Pin.ATTR_TYPE,
      StdAttr.WIDTH, Pin.ATTR_BEHAVIOR, StdAttr.LABEL, StdAttr.LABEL_LOC, StdAttr.LABEL_FONT,
      RadixOption.ATTRIBUTE, PROBEAPPEARANCE /*, Pin.ATTR_INITIAL */);
  private static final List<Attribute<?>> OUTPIN_ATTRIBUTES = Arrays.asList(StdAttr.FACING, Pin.ATTR_TYPE,
      StdAttr.WIDTH, /*Pin.ATTR_BEHAVIOR, */ StdAttr.LABEL, StdAttr.LABEL_LOC, StdAttr.LABEL_FONT,
      RadixOption.ATTRIBUTE, PROBEAPPEARANCE /*, Pin.ATTR_INITIAL */);

  BitWidth width = BitWidth.ONE;
  AttributeOption type = Pin.INPUT;
  AttributeOption behavior = Pin.SIMPLE;
  Long initialValue = 0L;

  public PinAttributes() { }

  @Override
  public List<Attribute<?>> getAttributes() {
    return type == Pin.INPUT ? (behavior == Pin.TRISTATE ? TRISTATE_ATTRIBUTES : INPIN_ATTRIBUTES)
        : OUTPIN_ATTRIBUTES;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == StdAttr.WIDTH) return (V) width;
    if (attr == Pin.ATTR_TYPE) return (V) type;
    if (attr == Pin.ATTR_BEHAVIOR) return (V) behavior;
    if (attr == PROBEAPPEARANCE) return (V) appearance;
    if (attr == Pin.ATTR_INITIAL) return (V) initialValue;
    return super.getValue(attr);
  }

  boolean isInput() {
    return type == Pin.INPUT;
  }

  boolean isOutput() {
    return type == Pin.OUTPUT;
  }

  Value defaultBitValue() {
    return isOutput() ? Value.UNKNOWN : behavior == Pin.PULL_UP ? Value.TRUE : Value.FALSE;
  }

  boolean isClock() {
    if (isOutput()) return false;

    String lbl = getValue(StdAttr.LABEL);
    if (lbl == null) return false;

    return lbl.matches("(?i).*(clk|clock).*");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == StdAttr.WIDTH) {
      BitWidth NewWidth = (BitWidth) value;
      if (width == NewWidth) return;
      width = (BitWidth) value;
      if (width.getWidth() > 8 && appearance == ProbeAttributes.APPEAR_EVOLUTION_NEW)
        super.setValue(RadixOption.ATTRIBUTE, RadixOption.RADIX_16);
    } else if (attr == Pin.ATTR_TYPE) {
      if (type.equals(value)) return;
      type = (AttributeOption) value;
      fireAttributeListChanged();
    } else if (attr == Pin.ATTR_BEHAVIOR) {
      if (behavior.equals(value)) return;
      final var attrListChanged = behavior == Pin.TRISTATE || value == Pin.TRISTATE;
      behavior = (AttributeOption) value;
      if (attrListChanged) fireAttributeListChanged();
    } else if (attr == PROBEAPPEARANCE) {
      final var newAppearance = (AttributeOption) value;
      if (appearance.equals(newAppearance)) return;
      appearance = newAppearance;
    } else if (attr == RadixOption.ATTRIBUTE) {
      if (width.getWidth() == 1) {
        super.setValue(RadixOption.ATTRIBUTE, RadixOption.RADIX_2);
      } else super.setValue(attr, value);
      return;
    } else if (attr == Pin.ATTR_INITIAL) {
      final var newInitial = (Long) value;
      if (newInitial == initialValue) return;
      initialValue = newInitial;
    } else {
      super.setValue(attr, value);
      return;
    }
    fireAttributeValueChanged(attr, value, null);
  }
}
