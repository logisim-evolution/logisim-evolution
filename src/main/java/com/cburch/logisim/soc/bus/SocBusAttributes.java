/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.bus;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusInfo;
import java.awt.Font;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SocBusAttributes extends AbstractAttributeSet {

  public static final Attribute<BitWidth> NrOfTracesAttr =
      Attributes.forBitWidth("TraceSize", S.getter("SocBusTraceSize"));
  public static final Attribute<SocBusInfo> SOC_BUS_ID = new SocBusIdAttribute();
  public static final Attribute<Boolean> SOC_TRACE_VISIBLE =
      Attributes.forBoolean("TraceVisible", S.getter("SocBusTraceVisible"));
  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          NrOfTracesAttr,
          SOC_TRACE_VISIBLE,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          SOC_BUS_ID);
  private Font LabelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean LabelVisible = true;
  private BitWidth TraceSize = BitWidth.create(5);
  private String Label = "";
  private SocBusInfo ID = new SocBusInfo(null);
  private Boolean traceVisible = true;

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    SocBusAttributes d = (SocBusAttributes) dest;
    d.LabelFont = LabelFont;
    d.LabelVisible = LabelVisible;
    d.TraceSize = TraceSize;
    d.Label = Label;
    d.traceVisible = traceVisible;
    d.ID = new SocBusInfo(null);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == NrOfTracesAttr) return (V) TraceSize;
    if (attr == StdAttr.LABEL) return (V) Label;
    if (attr == StdAttr.LABEL_FONT) return (V) LabelFont;
    if (attr == StdAttr.LABEL_VISIBILITY) return (V) LabelVisible;
    if (attr == SOC_BUS_ID) {
      if (ID.getBusId() == null || ID.getBusId().isEmpty()) {
        Date date = new Date();
        String[] names = this.toString().split("@");
        ID.setBusId(String.format("0x%016X%s", date.getTime(), names[names.length - 1]));
      }
      return (V) ID;
    }
    if (attr == SOC_TRACE_VISIBLE) return (V) traceVisible;
    return null;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return attr == SOC_BUS_ID;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    V oldValue = getValue(attr);
    if (attr == NrOfTracesAttr) {
      BitWidth v = (BitWidth) value;
      if (!TraceSize.equals(v)) {
        TraceSize = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == StdAttr.LABEL) {
      String v = (String) value;
      if (!Label.equals(v)) {
        Label = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == StdAttr.LABEL_FONT) {
      Font f = (Font) value;
      if (!LabelFont.equals(f)) {
        LabelFont = f;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      Boolean v = (Boolean) value;
      if (LabelVisible != v) {
        LabelVisible = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == SOC_BUS_ID) {
      ID.setBusId(((SocBusInfo) value).getBusId());
      return;
    }
    if (attr == SOC_TRACE_VISIBLE) {
      Boolean v = (Boolean) value;
      if (traceVisible != v) {
        traceVisible = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
  }

  public static class SocBusIdAttribute extends Attribute<SocBusInfo> {

    public SocBusIdAttribute() {
      super("SocBusIdentifier", null);
    }

    @Override
    public SocBusInfo parse(String value) {
      return new SocBusInfo(value);
    }

    @Override
    public boolean isHidden() {
      return true;
    }

    @Override
    public String toStandardString(SocBusInfo value) {
      return value.getBusId();
    }
  }
}
