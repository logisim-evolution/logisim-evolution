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
          new Attribute<?>[] {
            StdAttr.FACING,
            RadixOption.ATTRIBUTE,
            StdAttr.LABEL,
            StdAttr.LABEL_LOC,
            StdAttr.LABEL_FONT,
            PROBEAPPEARANCE
          });

  public static AttributeOption GetDefaultProbeAppearance() {
    if (AppPreferences.NEW_INPUT_OUTPUT_SHAPES.getBoolean()) return APPEAR_EVOLUTION_NEW;
    else return StdAttr.APPEAR_CLASSIC;
  }

  Direction facing = Direction.EAST;
  String label = "";
  Object labelloc = Direction.WEST;
  Font labelfont = StdAttr.DEFAULT_LABEL_FONT;
  RadixOption radix = RadixOption.RADIX_2;
  BitWidth width = BitWidth.ONE;
  AttributeOption Appearance = StdAttr.APPEAR_CLASSIC;

  public ProbeAttributes() {}

  @Override
  protected void copyInto(AbstractAttributeSet destObj) {
    ; // nothing to do
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
    if (attr == PROBEAPPEARANCE) return (E) Appearance;
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
      if (Appearance.equals(NewAppearance)) return;
      Appearance = NewAppearance;
    } else {
      throw new IllegalArgumentException("unknown attribute");
    }
    fireAttributeValueChanged(attr, value, Oldvalue);
  }

  @Override
  public void AttributeValueChanged(ConvertEvent e) {
    setValue(PROBEAPPEARANCE, e.GetValue());
  }
}
