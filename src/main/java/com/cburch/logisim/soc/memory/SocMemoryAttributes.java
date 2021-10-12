/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.memory;

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

public class SocMemoryAttributes extends AbstractAttributeSet {

  private static class SocMemoryStateAttribute extends Attribute<SocMemoryState> {

    @Override
    public SocMemoryState parse(String value) {
      return null;
    }

    @Override
    public boolean isHidden() {
      return true;
    }
  }

  public static final Attribute<SocMemoryState> SOCMEM_STATE = new SocMemoryStateAttribute();
  public static final Attribute<Integer> START_ADDRESS = Attributes.forHexInteger("StartAddress", S.getter("SocMemStartAddress"));
  public static final Attribute<BitWidth> MEM_SIZE = Attributes.forBitWidth("MemSize", S.getter("SocMemSize"), 10, 26);

  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = true;
  private SocMemoryState memState = new SocMemoryState();
  private BitWidth memSize = BitWidth.create(10);

  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          START_ADDRESS,
          MEM_SIZE,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          SocSimulationManager.SOC_BUS_SELECT,
          SOCMEM_STATE);

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    SocMemoryAttributes d = (SocMemoryAttributes) dest;
    d.labelFont = labelFont;
    d.labelVisible = labelVisible;
    d.memState = new SocMemoryState();
    d.memSize = memSize;
    d.memState.setSize(memSize);
    d.memState.setStartAddress(memState.getStartAddress());
    d.memState.getSocBusInfo().setBusId(memState.getSocBusInfo().getBusId());
    d.memState.setLabel(memState.getLabel());
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == START_ADDRESS) return (V) memState.getStartAddress();
    if (attr == MEM_SIZE) return (V) memSize;
    if (attr == StdAttr.LABEL) return (V) memState.getLabel();
    if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
    if (attr == StdAttr.LABEL_VISIBILITY) return (V) labelVisible;
    if (attr == SocSimulationManager.SOC_BUS_SELECT) return (V) memState.getSocBusInfo();
    if (attr == SOCMEM_STATE) return (V) memState;
    return null;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return attr == SOCMEM_STATE;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave() && attr != SOCMEM_STATE;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    V oldValue = getValue(attr);
    if (attr == START_ADDRESS) {
      if (memState.setStartAddress((Integer) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == MEM_SIZE) {
      if (memState.setSize((BitWidth) value)) {
        memSize = (BitWidth) value;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == StdAttr.LABEL) {
      String l = (String) value;
      if (memState.setLabel(l)) {
        fireAttributeValueChanged(attr, value, oldValue);
      }
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
      if (!labelVisible.equals(v)) {
        labelVisible = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      if (memState.setSocBusInfo((SocBusInfo) value)) {
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
  }
}
