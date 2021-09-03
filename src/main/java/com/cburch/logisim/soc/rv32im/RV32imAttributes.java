/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.rv32im;

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

public class RV32imAttributes extends AbstractAttributeSet {

  private static class Rv32imStateAttribute extends Attribute<RV32im_state> {

    @Override
    public RV32im_state parse(String value) {
      return null;
    }

    @Override
    public boolean isHidden() {
      return true;
    }
  }

  public static final Attribute<RV32im_state> RV32IM_STATE = new Rv32imStateAttribute();
  public static final Attribute<BitWidth> NR_OF_IRQS =
      Attributes.forBitWidth("irqWidth", S.getter("rv32imIrqWidth"), 0, 32);
  public static final Attribute<Integer> RESET_VECTOR =
      Attributes.forHexInteger("resetVector", S.getter("rv32ResetVector"));
  public static final Attribute<Integer> EXCEPTION_VECTOR =
      Attributes.forHexInteger("exceptionVector", S.getter("rv32ExceptionVector"));
  public static final Attribute<Boolean> RV32IM_STATE_VISIBLE =
      Attributes.forBoolean("stateVisible", S.getter("rv32StateVisible"));

  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = true;
  private RV32im_state upState = new RV32im_state();
  private Boolean stateVisible = true;

  private static final List<Attribute<?>> ATTRIBUTES =
        Arrays.asList(
            RESET_VECTOR,
            EXCEPTION_VECTOR,
            NR_OF_IRQS,
            RV32IM_STATE_VISIBLE,
            StdAttr.LABEL,
            StdAttr.LABEL_FONT,
            StdAttr.LABEL_VISIBILITY,
            SocSimulationManager.SOC_BUS_SELECT,
            RV32IM_STATE);

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    RV32imAttributes d = (RV32imAttributes) dest;
    d.labelFont = labelFont;
    d.labelVisible = labelVisible;
    d.stateVisible = stateVisible;
    d.upState = new RV32im_state();
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
    if (attr == NR_OF_IRQS) return (V) BitWidth.create(upState.getNrOfIrqs());
    if (attr == StdAttr.LABEL) return (V) upState.getLabel();
    if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
    if (attr == StdAttr.LABEL_VISIBILITY) return (V) labelVisible;
    if (attr == SocSimulationManager.SOC_BUS_SELECT) return (V) upState.getAttachedBus();
    if (attr == RV32IM_STATE) return (V) upState;
    if (attr == RV32IM_STATE_VISIBLE) return (V) stateVisible;
    return null;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return attr == RV32IM_STATE;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave() && attr != RV32IM_STATE;
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
    if (attr == RV32IM_STATE_VISIBLE) {
      Boolean v = (Boolean) value;
      if (stateVisible != v) {
        stateVisible = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
  }
}
