/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.pio;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.soc.memory.SocMemoryAttributes;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class PioAttributes extends AbstractAttributeSet {

  private static class PIOStateAttribute extends Attribute<PioState> {
    @Override
    public PioState parse(String value) {
      return null;
    }

    @Override
    public boolean isHidden() {
      return true;
    }
  }

  public static final AttributeOption PORT_BIDIR =
      new AttributeOption("bidir", S.getter("SocPioBidir"));
  public static final AttributeOption PORT_INPUT =
      new AttributeOption("inputonly", S.getter("SocPioInputOnly"));
  public static final AttributeOption PORT_OUTPUT =
      new AttributeOption("outputonly", S.getter("SocPioOutputOnly"));
  public static final AttributeOption PORT_INOUT =
      new AttributeOption("inout", S.getter("SocPioInout"));

  public static final AttributeOption CAPT_RISING =
      new AttributeOption("rising", S.getter("SocPioRisingEdge"));
  public static final AttributeOption CAPT_FALLING =
      new AttributeOption("falling", S.getter("SocPioFallingEdge"));
  public static final AttributeOption CAPT_ANY =
      new AttributeOption("any", S.getter("SocPioAnyEdge"));

  public static final AttributeOption IRQ_LEVEL =
      new AttributeOption("level", S.getter("SocPioIrqLevel"));
  public static final AttributeOption IRQ_EDGE =
      new AttributeOption("edge", S.getter("SocPioIrQEdge"));

  public static final Attribute<PioState> PIO_STATE = new PIOStateAttribute();
  public static final Attribute<AttributeOption> PIO_DIRECTION =
      Attributes.forOption(
          "direction",
          S.getter("SocPioDirection"),
          new AttributeOption[] {PORT_BIDIR, PORT_INPUT, PORT_OUTPUT, PORT_INOUT});
  public static final Attribute<Integer> PIO_OUT_RESET =
      Attributes.forHexInteger("outputresetvalue", S.getter("SocPioOutputResetValue"));
  public static final Attribute<Boolean> PIO_OUT_BIT =
      Attributes.forBoolean("outputbitsetclear", S.getter("SocPioOutputIndividualBits"));
  public static final Attribute<Boolean> PIO_SYNC_CAPT =
      Attributes.forBoolean("inputssynccapt", S.getter("SocPioInputsSyncCapture"));
  public static final Attribute<AttributeOption> PIO_CAPT_TYPE =
      Attributes.forOption(
          "capturetype",
          S.getter("SocPioCaptureEdge"),
          new AttributeOption[] {CAPT_RISING, CAPT_FALLING, CAPT_ANY});
  public static final Attribute<Boolean> PIO_CAPT_BIT =
      Attributes.forBoolean("inputscaptbit", S.getter("SocPioInputCaptureBit"));
  public static final Attribute<Boolean> PIO_GEN_IRQ =
      Attributes.forBoolean("genirq", S.getter("SocPioGenIRQ"));
  public static final Attribute<AttributeOption> PIO_IRQ_TYPE =
      Attributes.forOption(
          "irqtype", S.getter("SicPioIrqType"), new AttributeOption[] {IRQ_LEVEL, IRQ_EDGE});

  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = true;
  private PioState state = new PioState();

  private ArrayList<Attribute<?>> myAttributes = new ArrayList<>();

  public PioAttributes() {
    updateAttributeList();
  }

  private boolean updateAttributeList() {
    ArrayList<Attribute<?>> newList = new ArrayList<>();
    boolean changes = false;
    newList.add(SocMemoryAttributes.START_ADDRESS);
    newList.add(StdAttr.WIDTH);
    newList.add(PIO_DIRECTION);
    if (state.getPortDirection() != PORT_INPUT) {
      changes |= !myAttributes.contains(PIO_OUT_RESET);
      newList.add(PIO_OUT_RESET);
      newList.add(PIO_OUT_BIT);
    } else changes |= myAttributes.contains(PIO_OUT_RESET);
    if (state.getPortDirection() != PORT_OUTPUT) {
      changes |= !myAttributes.contains(PIO_SYNC_CAPT);
      newList.add(PIO_SYNC_CAPT);
      if (state.inputIsCapturedSynchronisely()) {
        changes |= !myAttributes.contains(PIO_CAPT_TYPE);
        newList.add(PIO_CAPT_TYPE);
        newList.add(PIO_CAPT_BIT);
      } else changes |= myAttributes.contains(PIO_CAPT_TYPE);
      newList.add(PIO_GEN_IRQ);
      if (state.inputIsCapturedSynchronisely()) {
        changes |= !myAttributes.contains(PIO_IRQ_TYPE);
        newList.add(PIO_IRQ_TYPE);
      } else changes |= myAttributes.contains(PIO_IRQ_TYPE);
    } else changes |= myAttributes.contains(PIO_SYNC_CAPT);
    newList.add(StdAttr.LABEL);
    newList.add(StdAttr.LABEL_FONT);
    newList.add(StdAttr.LABEL_VISIBILITY);
    newList.add(SocSimulationManager.SOC_BUS_SELECT);
    newList.add(PIO_STATE);
    myAttributes = newList;
    return changes;
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    PioAttributes d = (PioAttributes) dest;
    d.labelFont = labelFont;
    d.labelVisible = labelVisible;
    d.state = new PioState();
    state.copyInto(d.state);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return myAttributes;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return attr == PIO_STATE;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave() && attr != PIO_STATE;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == SocMemoryAttributes.START_ADDRESS) return (V) state.getStartAddress();
    if (attr == StdAttr.WIDTH) return (V) state.getNrOfIOs();
    if (attr == PIO_DIRECTION) return (V) state.getPortDirection();
    if (attr == StdAttr.LABEL) return (V) state.getLabel();
    if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
    if (attr == StdAttr.LABEL_VISIBILITY) return (V) labelVisible;
    if (attr == SocSimulationManager.SOC_BUS_SELECT) return (V) state.getAttachedBus();
    if (attr == PIO_OUT_RESET) return (V) state.getOutputResetValue();
    if (attr == PIO_OUT_BIT) return (V) state.outputSupportsBitManipulations();
    if (attr == PIO_SYNC_CAPT) return (V) state.inputIsCapturedSynchronisely();
    if (attr == PIO_CAPT_TYPE) return (V) state.getInputCaptureEdge();
    if (attr == PIO_CAPT_BIT) return (V) state.inputCaptureSupportsBitClearing();
    if (attr == PIO_GEN_IRQ) return (V) state.inputGeneratesIrq();
    if (attr == PIO_IRQ_TYPE) return (V) state.getIrqType();
    if (attr == PIO_STATE) return (V) state;
    return null;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    V oldValue = getValue(attr);
    if (attr == SocMemoryAttributes.START_ADDRESS) {
      if (state.setStartAddress((Integer) value)) fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.WIDTH) {
      if (state.setNrOfIOs((BitWidth) value)) fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == PIO_DIRECTION) {
      if (state.setPortDirection((AttributeOption) value)) {
        if (updateAttributeList()) fireAttributeListChanged();
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == PIO_OUT_RESET) {
      if (state.setOutputResetValue((Integer) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == PIO_OUT_BIT) {
      if (state.setOutputBitManupulations((Boolean) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == PIO_SYNC_CAPT) {
      if (state.setInputSynchronousCapture((Boolean) value)) {
        if (updateAttributeList()) fireAttributeListChanged();
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == PIO_CAPT_TYPE) {
      if (state.setInputCaptureEdge((AttributeOption) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == PIO_CAPT_BIT) {
      if (state.setInputCaptureBitClearing((Boolean) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == PIO_GEN_IRQ) {
      if (state.setIrqGeneration((Boolean) value)) {
        if (updateAttributeList()) fireAttributeListChanged();
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == PIO_IRQ_TYPE) {
      if (state.setIrqType((AttributeOption) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.LABEL) {
      if (state.setLabel((String) value)) fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.LABEL_FONT) {
      Font f = (Font) value;
      if (labelFont != f) {
        labelFont = f;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      Boolean b = (Boolean) value;
      if (b != labelVisible) {
        labelVisible = b;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      if (state.setAttachedBus((SocBusInfo) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
  }
}
