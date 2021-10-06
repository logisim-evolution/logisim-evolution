/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.ConvertEvent;
import com.cburch.logisim.prefs.ConvertEventListener;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

public class ProbeAttributes extends AbstractAttributeSet implements ConvertEventListener {
  public static ProbeAttributes instance = new ProbeAttributes();

  public static final AttributeOption APPEAR_EVOLUTION_NEW =
      new AttributeOption("NewPins", S.getter("probeNewPin"));

  public static final Attribute<AttributeOption> PROBEAPPEARANCE =
      Attributes.forOption(
          "appearance",
          S.getter("stdAppearanceAttr"),
          new AttributeOption[] {StdAttr.APPEAR_CLASSIC, APPEAR_EVOLUTION_NEW});

  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          StdAttr.FACING,
          RadixOption.ATTRIBUTE,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          PROBEAPPEARANCE);

  public static AttributeOption getDefaultProbeAppearance() {
    if (AppPreferences.NEW_INPUT_OUTPUT_SHAPES.getBoolean()) return APPEAR_EVOLUTION_NEW;
    else return StdAttr.APPEAR_CLASSIC;
  }

  Direction facing = Direction.EAST;
  String label = "";
  Object labelloc = Direction.WEST;
  Font labelfont = StdAttr.DEFAULT_LABEL_FONT;
  RadixOption radix = RadixOption.RADIX_2;
  BitWidth width = BitWidth.ONE;
  AttributeOption appearance = StdAttr.APPEAR_CLASSIC;

  public ProbeAttributes() {}

  @Override
  protected void copyInto(AbstractAttributeSet destObj) {
    // nothing to do
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValue(Attribute<E> attr) {
    if (attr == StdAttr.FACING) return (E) facing;
    if (attr == StdAttr.LABEL) return (E) label;
    if (attr == StdAttr.LABEL_LOC) return (E) labelloc;
    if (attr == StdAttr.LABEL_FONT) return (E) labelfont;
    if (attr == RadixOption.ATTRIBUTE) return (E) radix;
    if (attr == PROBEAPPEARANCE) return (E) appearance;
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    V Oldvalue = null;
    if (attr == StdAttr.FACING) {
      Direction newValue = (Direction) value;
      if (facing.equals(newValue)) return;
      facing = (Direction) value;
    } else if (attr == StdAttr.LABEL) {
      String val = (String) value;
      if (label.equals(val)) return;
      Oldvalue = (V) label;
      label = val;
    } else if (attr == StdAttr.LABEL_LOC) {
      if (labelloc.equals(value)) return;
      labelloc = value;
    } else if (attr == StdAttr.LABEL_FONT) {
      Font NewValue = (Font) value;
      if (labelfont.equals(NewValue)) return;
      labelfont = NewValue;
    } else if (attr == RadixOption.ATTRIBUTE) {
      RadixOption NewValue = (RadixOption) value;
      if (radix.equals(NewValue)) return;
      radix = NewValue;
    } else if (attr == PROBEAPPEARANCE) {
      AttributeOption NewAppearance = (AttributeOption) value;
      if (appearance.equals(NewAppearance)) return;
      appearance = NewAppearance;
    } else {
      throw new IllegalArgumentException("unknown attribute");
    }
    fireAttributeValueChanged(attr, value, Oldvalue);
  }

  @Override
  public void attributeValueChanged(ConvertEvent e) {
    setValue(PROBEAPPEARANCE, e.getValue());
  }
}
