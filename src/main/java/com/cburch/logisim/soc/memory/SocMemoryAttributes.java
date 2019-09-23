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

package com.cburch.logisim.soc.memory;

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

public class SocMemoryAttributes extends AbstractAttributeSet {

  private static class SocMemoryStateAttribute extends Attribute<SocMemoryState> {

    @Override
    public SocMemoryState parse(String value) { return null; }
    
    @Override
    public boolean isHidden() {return true;}

  }

  public static final Attribute<SocMemoryState> SOCMEM_STATE = new SocMemoryStateAttribute();
  public static final Attribute<Integer> START_ADDRESS = Attributes.forHexInteger("StartAddress", S.getter("SocMemStartAddress"));
  public static final Attribute<BitWidth> MEM_SIZE = Attributes.forBitWidth("MemSize", S.getter("SocMemSize"), 10, 26);
  
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisable = true;
  private SocMemoryState memState = new SocMemoryState();
  private BitWidth memSize = BitWidth.create(10);
  
  private static List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
    	  new Attribute<?>[] {
    	    START_ADDRESS,
    	    MEM_SIZE,
    	    StdAttr.LABEL,
    	    StdAttr.LABEL_FONT,
    	    StdAttr.LABEL_VISIBILITY,
    	    SocSimulationManager.SOC_BUS_SELECT,
    	    SOCMEM_STATE,
      });

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    SocMemoryAttributes d = (SocMemoryAttributes) dest;
    d.labelFont = labelFont;
    d.labelVisable = labelVisable;
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
	if (attr == StdAttr.LABEL_VISIBILITY) return (V) labelVisable;
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
    return attr != SOCMEM_STATE;
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
      if (!labelVisable.equals(v)) {
        labelVisable = v;
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
