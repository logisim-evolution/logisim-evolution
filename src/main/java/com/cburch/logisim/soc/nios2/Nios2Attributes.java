/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.nios2;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocSimulationManager;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

public class Nios2Attributes extends AbstractAttributeSet {

  private static class Nios2StateAttribute extends Attribute<Nios2State> {

    @Override
    public Nios2State parse(String value) {
      return null;
    }

    @Override
    public boolean isHidden() {
      return true;
    }
  }

  public static final Attribute<Nios2State> NIOS2_STATE = new Nios2StateAttribute();
  public static final Attribute<BitWidth> NR_OF_IRQS =
      Attributes.forBitWidth("irqWidth", S.getter("rv32imIrqWidth"), 0, 32);
  public static final Attribute<Integer> RESET_VECTOR =
      Attributes.forHexInteger("resetVector", S.getter("rv32ResetVector"));
  public static final Attribute<Integer> EXCEPTION_VECTOR =
      Attributes.forHexInteger("exceptionVector", S.getter("rv32ExceptionVector"));
  public static final Attribute<Integer> BREAK_VECTOR =
      Attributes.forHexInteger("breakVector", S.getter("nios2BreakVector"));
  public static final Attribute<Boolean> NIOS_STATE_VISIBLE =
      Attributes.forBoolean("stateVisible", S.getter("rv32StateVisible"));

  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = true;
  private Nios2State upState = new Nios2State();
  private Boolean stateVisible = true;

  private static final List<Attribute<?>> ATTRIBUTES =
        Arrays.asList(
            RESET_VECTOR,
            EXCEPTION_VECTOR,
            BREAK_VECTOR,
            NR_OF_IRQS,
            NIOS_STATE_VISIBLE,
            StdAttr.LABEL,
            StdAttr.LABEL_FONT,
            StdAttr.LABEL_VISIBILITY,
            SocSimulationManager.SOC_BUS_SELECT,
            NIOS2_STATE);

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    Nios2Attributes d = (Nios2Attributes) dest;
    d.labelFont = labelFont;
    d.labelVisible = labelVisible;
    d.stateVisible = stateVisible;
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
    if (attr == StdAttr.LABEL_VISIBILITY) return (V) labelVisible;
    if (attr == SocSimulationManager.SOC_BUS_SELECT) return (V) upState.getAttachedBus();
    if (attr == NIOS2_STATE) return (V) upState;
    if (attr == NIOS_STATE_VISIBLE) return (V) stateVisible;
    return null;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return attr == NIOS2_STATE;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave() && attr != NIOS2_STATE;
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
      if (upState.setNrOfIrqs(((BitWidth) value).getWidth()))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      if (upState.setAttachedBus((SocBusInfo) value))
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
      if (v != labelVisible) {
        labelVisible = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == NIOS_STATE_VISIBLE) {
      Boolean v = (Boolean) value;
      if (stateVisible != v) {
        stateVisible = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
  }

}
