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

package com.cburch.logisim.soc.jtaguart;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocSimulationManager;

public class JtagUartAttributes  extends AbstractAttributeSet {

  private static class JtagUartStateAttribute extends Attribute<JtagUartState> {
    @Override
    public JtagUartState parse(String value) { return null; }
    
    @Override
    public boolean isHidden() {return true;}
  }
  
  public static final AttributeOption OPT_8 = new AttributeOption("8",S.getter("JtagSize8")); 
  public static final AttributeOption OPT_16 = new AttributeOption("16",S.getter("JtagSize16")); 
  public static final AttributeOption OPT_32 = new AttributeOption("32",S.getter("JtagSize32")); 
  public static final AttributeOption OPT_64 = new AttributeOption("64",S.getter("JtagSize64")); 
  public static final AttributeOption OPT_128 = new AttributeOption("128",S.getter("JtagSize128")); 
  public static final AttributeOption OPT_256 = new AttributeOption("256",S.getter("JtagSize256")); 
  public static final AttributeOption OPT_512 = new AttributeOption("512",S.getter("JtagSize512")); 
  public static final AttributeOption OPT_1024 = new AttributeOption("1024",S.getter("JtagSize1024")); 
  public static final AttributeOption OPT_2048 = new AttributeOption("2048",S.getter("JtagSize2048")); 
  public static final AttributeOption OPT_4096 = new AttributeOption("4096",S.getter("JtagSize4096")); 
  public static final AttributeOption OPT_8192 = new AttributeOption("8192",S.getter("JtagSize8192")); 
  public static final AttributeOption OPT_16384 = new AttributeOption("16384",S.getter("JtagSize16384")); 
  public static final AttributeOption OPT_32768 = new AttributeOption("32768",S.getter("JtagSize32768")); 
  public static final AttributeOption[] SIZE_ARRAY = new AttributeOption[] { OPT_8, OPT_16, OPT_32, OPT_64,
		  OPT_128, OPT_256, OPT_512, OPT_1024, OPT_2048, OPT_4096, OPT_8192, OPT_16384, OPT_32768};
  
  public static final Attribute<Integer> START_ADDRESS = Attributes.forHexInteger("StartAddress", S.getter("VgaStartAddress"));
  public static final Attribute<JtagUartState> JTAG_STATE = new JtagUartStateAttribute();
  public static final Attribute<AttributeOption> WRITE_FIFO_SIZE = Attributes.forOption("WriteFifoSize",
          S.getter("UartJtagWriteFifoSize"), SIZE_ARRAY);
  public static final Attribute<Integer> WRITE_IRQ_THRESHOLD = Attributes.forInteger("WriteThreshold", S.getter("JtagUartWriteIrqThreshold"));
  public static final Attribute<AttributeOption> READ_FIFO_SIZE = Attributes.forOption("ReadFifoSize",
          S.getter("UartJtagREADFifoSize"), SIZE_ARRAY);
  public static final Attribute<Integer> READ_IRQ_THRESHOLD = Attributes.forInteger("ReadThreshold", S.getter("JtagUartReadIrqThreshold"));

  private static List<Attribute<?>> ATTRIBUTES =
        Arrays.asList(
          new Attribute<?>[] {
             START_ADDRESS,
             WRITE_FIFO_SIZE,
             WRITE_IRQ_THRESHOLD,
             READ_FIFO_SIZE,
             READ_IRQ_THRESHOLD,
             StdAttr.LABEL,
             StdAttr.LABEL_FONT,
             StdAttr.LABEL_VISIBILITY,
             SocSimulationManager.SOC_BUS_SELECT,
             JTAG_STATE});

  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = true;
  private JtagUartState state = new JtagUartState();

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    JtagUartAttributes d = (JtagUartAttributes) dest;
    d.labelFont = labelFont;
    d.labelVisible = labelVisible;
    d.state = new JtagUartState();
    state.copyInto(d.state);
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) { return attr == JTAG_STATE; }
  
  @Override
  public boolean isToSave(Attribute<?> attr) { return attr != JTAG_STATE; }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == START_ADDRESS) return (V) state.getStartAddress();
    if (attr == WRITE_FIFO_SIZE) return (V) state.getWriteFifoSize();
    if (attr == WRITE_IRQ_THRESHOLD) return (V) state.getWriteIrqThreshold();
    if (attr == READ_FIFO_SIZE) return (V) state.getReadFifoSize();
    if (attr == READ_IRQ_THRESHOLD) return (V) state.getReadIrqThreshold();
    if (attr == StdAttr.LABEL) return (V) state.getLabel();
    if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
    if (attr == StdAttr.LABEL_VISIBILITY) return (V) labelVisible;
    if (attr == SocSimulationManager.SOC_BUS_SELECT) return (V) state.getAttachedBus();
    if (attr == JTAG_STATE) return (V) state;
    return null;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    V oldValue = getValue(attr);
    if (attr == START_ADDRESS) {
      if (state.setStartAddress((int) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == WRITE_FIFO_SIZE) {
      if (state.setWriteFifoSize((AttributeOption) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == WRITE_IRQ_THRESHOLD) {
      if (state.setWriteIrqThreshold((Integer) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == READ_FIFO_SIZE) {
      if (state.setReadFifoSize((AttributeOption) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == READ_IRQ_THRESHOLD) {
      if (state.setReadIrqThreshold((Integer) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.LABEL) {
      if (state.setLabel((String) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
     }
     if (attr == StdAttr.LABEL_FONT) {
       Font f = (Font) value;
       if (!f.equals(labelFont)) {
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
