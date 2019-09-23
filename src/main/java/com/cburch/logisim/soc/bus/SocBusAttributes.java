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

package com.cburch.logisim.soc.bus;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Font;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusInfo;

public class SocBusAttributes extends AbstractAttributeSet {

  public static class SocBusIdAttribute extends Attribute<SocBusInfo> {
  
   public SocBusIdAttribute() {
     super("SocBusIdentifier",null);
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
  
  public static final Attribute<BitWidth> NrOfTracesAttr = Attributes.forBitWidth("TraceSize", S.getter("SocBusTraceSize"));
  public static final Attribute<SocBusInfo> SOC_BUS_ID = new SocBusIdAttribute();
  public static final Attribute<Boolean> SOC_TRACE_VISABLE = Attributes.forBoolean("TraceVisible", S.getter("SocBusTraceVisable"));
  private Font LabelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean LabelVisable = true;
  private BitWidth TraceSize = BitWidth.create(5);
  private String Label = "";
  private SocBusInfo ID = new SocBusInfo(null);
  private Boolean traceVisable = true;

  private static List<Attribute<?>> ATTRIBUTES =
        Arrays.asList(
            new Attribute<?>[] {
              NrOfTracesAttr,
              SOC_TRACE_VISABLE,
              StdAttr.LABEL,
              StdAttr.LABEL_FONT,
              StdAttr.LABEL_VISIBILITY,
              SOC_BUS_ID
            });

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    SocBusAttributes d = (SocBusAttributes) dest;
    d.LabelFont = LabelFont;
    d.LabelVisable = LabelVisable;
    d.TraceSize = TraceSize;
    d.Label = Label;
    d.traceVisable = traceVisable;
    d.ID = new SocBusInfo(null);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == NrOfTracesAttr)
      return (V) TraceSize;
    if (attr == StdAttr.LABEL)
      return (V) Label;
    if (attr == StdAttr.LABEL_FONT)
      return (V) LabelFont;
    if (attr == StdAttr.LABEL_VISIBILITY)
      return (V) LabelVisable;
    if (attr == SOC_BUS_ID) {
      if (ID.getBusId() == null || ID.getBusId().isEmpty()) {
        Date date = new Date();
        String[] names = this.toString().split("@");
          ID.setBusId(String.format("0x%016X%s", date.getTime(), names[names.length-1]));
        }
      return (V) ID;
    }
    if (attr == SOC_TRACE_VISABLE) return (V) traceVisable;
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
      if (LabelVisable != v) {
        LabelVisable = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == SOC_BUS_ID) {
      ID.setBusId(((SocBusInfo) value).getBusId());
      return;
    }
    if (attr == SOC_TRACE_VISABLE) {
      Boolean v = (Boolean) value;
      if (traceVisable != v) {
        traceVisable = v;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
  }
}
