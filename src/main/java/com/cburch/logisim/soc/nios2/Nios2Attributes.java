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

package com.cburch.logisim.soc.nios2;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocSimulationManager;

public class Nios2Attributes extends AbstractAttributeSet {

  private static class Nios2StateAttribute extends Attribute<Nios2State> {

    @Override
    public Nios2State parse(String value) {return null;}

    @Override
    public boolean isHidden() {return true;}
  }
  
  public static final Attribute<Nios2State> NIOS2_STATE = new Nios2StateAttribute();
  public static final Attribute<BitWidth> NR_OF_IRQS = Attributes.forBitWidth("irqWidth", S.getter("rv32imIrqWidth"),0,32);
  public static final Attribute<Integer> RESET_VECTOR = Attributes.forHexInteger("resetVector", S.getter("rv32ResetVector"));
  public static final Attribute<Integer> EXCEPTION_VECTOR  = Attributes.forHexInteger("exceptionVector", S.getter("rv32ExceptionVector"));
  public static final Attribute<Integer> BREAK_VECTOR  = Attributes.forHexInteger("breakVector", S.getter("nios2BreakVector"));
  public static final Attribute<Boolean> NIOS_STATE_VISIBLE = Attributes.forBoolean("stateVisable", S.getter("rv32StateVisable"));

  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisable = true;
  private Nios2State upState = new Nios2State();
  private Boolean stateVisable = true;

  private static List<Attribute<?>> ATTRIBUTES =
        Arrays.asList(
            new Attribute<?>[] {
              RESET_VECTOR,
              EXCEPTION_VECTOR,
              BREAK_VECTOR,
              NR_OF_IRQS,
              NIOS_STATE_VISIBLE,
              StdAttr.LABEL,
              StdAttr.LABEL_FONT,
              StdAttr.LABEL_VISIBILITY,
              SocSimulationManager.SOC_BUS_SELECT,
              NIOS2_STATE
            });

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    Nios2Attributes d = (Nios2Attributes) dest;
    d.labelFont = labelFont;
    d.labelVisable = labelVisable;
    d.stateVisable = stateVisable;
    d.upState = new Nios2State();
    upState.copyInto(d.upState);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == RESET_VECTOR) return (V) upState.getResetVector();
    if (attr == EXCEPTION_VECTOR) return (V) upState.getExceptionVector();
    if (attr == BREAK_VECTOR) return (V) upState.getBreakVector();
    if (attr == NR_OF_IRQS) return (V) BitWidth.create(upState.getNrOfIrqs());
    if (attr == StdAttr.LABEL) return (V) upState.getLabel();
    if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
    if (attr == StdAttr.LABEL_VISIBILITY) return (V) labelVisable;
    if (attr == SocSimulationManager.SOC_BUS_SELECT) return (V)upState.getAttachedBus();
    if (attr == NIOS2_STATE) return (V) upState;
    if (attr == NIOS_STATE_VISIBLE) return (V) stateVisable;
    return null;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return attr == NIOS2_STATE;
  }
  
  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr != NIOS2_STATE;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    V oldValue = getValue(attr);
    if (attr == RESET_VECTOR) {
      if (upState.setResetVector((int) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == EXCEPTION_VECTOR) {
      if (upState.setExceptionVector((int) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == BREAK_VECTOR) {
      if (upState.setBreakVector((int) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == NR_OF_IRQS) {
      if (upState.setNrOfIrqs(((BitWidth)value).getWidth()))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      if (upState.setAttachedBus((SocBusInfo)value)) 
    fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.LABEL) {
      if (upState.setLabel((String) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.LABEL_FONT) {
      Font f = (Font) value;
      if (!labelFont.equals(f)) {
        labelFont = f;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      Boolean v = (Boolean) value;
      if (v != labelVisable) {
        labelVisable = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == NIOS_STATE_VISIBLE) {
      Boolean v = (Boolean) value;
      if (stateVisable != v) {
        stateVisable = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
  }

}
