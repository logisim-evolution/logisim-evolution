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

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import java.util.WeakHashMap;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;

public class Ram extends Mem {

  public static class Logger extends InstanceLogger {

    @Override
    public String getLogName(InstanceState state, Object option) {
      String Label = state.getAttributeValue(StdAttr.LABEL);
      if (Label.equals("")) {
        Label = null;
      }
      if (option instanceof Long) {
        String disp = S.get("ramComponent");
        Location loc = state.getInstance().getLocation();
        return (Label == null) ? disp + loc + "[" + option + "]" : Label + "[" + option + "]";
      } else {
        return Label;
      }
    }

    @Override
    public Object[] getLogOptions(InstanceState state) {
      int addrBits = state.getAttributeValue(ADDR_ATTR).getWidth();
      if (addrBits >= logOptions.length) {
        addrBits = logOptions.length - 1;
      }
      synchronized (logOptions) {
        Object[] ret = logOptions[addrBits];
        if (ret == null) {
          ret = new Object[1 << addrBits];
          logOptions[addrBits] = ret;
          for (long i = 0; i < ret.length; i++) {
            ret[(int) i] = Long.valueOf(i);
          }
        }
        return ret;
      }
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      if (option instanceof Long) {
        MemState s = (MemState) state.getData();
        long addr = ((Long) option).longValue();
        return Value.createKnown(BitWidth.create(s.getDataBits()), s.getContents().get(addr));
      } else {
        return Value.NIL;
      }
    }
  }

  private static Object[][] logOptions = new Object[9][];
  private static WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<MemContents, HexFrame>();

  public Ram() {
    super("RAM", S.getter("ramComponent"), 3);
    setIcon(new ArithmeticIcon("RAM",3));
    setInstanceLogger(Logger.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    instance.addAttributeListener();
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    if (attr.equals(StdAttr.APPEARANCE)) {
      return StdAttr.APPEAR_CLASSIC;
    } else {
      return super.getDefaultAttributeValue(attr, ver);
    }
  }

  @Override
  void configurePorts(Instance instance) {
    RamAppearance.configurePorts(instance);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new RamAttributes();
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    String Label = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
    if (Label.length() == 0) {
      return "RAM";
    } else {
      return "RAMCONTENTS_" + Label;
    }
  }
  
  private MemContents getNewContents(AttributeSet attrs) {
    MemContents contents = MemContents.create(attrs.getValue(Mem.ADDR_ATTR).getWidth(), 
                                             attrs.getValue(Mem.DATA_ATTR).getWidth()); 
    contents.condFillRandom();
    return contents;
  }

  private static HexFrame getHexFrame(MemContents value, Project proj, Instance instance) {
    synchronized (windowRegistry) {
      HexFrame ret = windowRegistry.get(value);
      if (ret == null) {
        ret = new HexFrame(proj, instance, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }
  
  public static void closeHexFrame(RamState state) {
    MemContents contents = state.getContents();
    HexFrame ret;
    synchronized (windowRegistry) {
      ret = windowRegistry.remove(contents);
    }
    if (ret == null) return;
    ret.closeAndDispose();
  }

  @Override
  public HexFrame getHexFrame(Project proj, Instance instance, CircuitState circState) {
    RamState ret = (RamState) instance.getData(circState);
    return getHexFrame((ret == null) ? getNewContents(instance.getAttributeSet()) : ret.getContents(), proj, instance);
  }
  
  public boolean reset(CircuitState state, Instance instance) {
    RamState ret = (RamState) instance.getData(state);
    if (ret == null) return true;
    MemContents contents = ret.getContents();
    if (instance.getAttributeValue(RamAttributes.ATTR_TYPE).equals(RamAttributes.VOLATILE)) {
      contents.condClear();
    }
    return false;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return RamAppearance.getBounds(attrs);
  }

  public MemContents getContents(InstanceState ramState) {
    return (MemContents)getState(ramState).getContents();
  }

  @Override
  MemState getState(Instance instance, CircuitState state) {
	return getState(state.getInstanceState(instance));
  }

  @Override
  MemState getState(InstanceState state) {
    RamState ret = (RamState) state.getData();
    if (ret == null) {
      Instance instance = state.getInstance();
      ret = new RamState(instance, getNewContents(instance.getAttributeSet()), new MemListener(instance));
      state.setData(ret);
    } else {
      ret.setRam(state.getInstance());
    }
    return ret;
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) {
      MyHDLGenerator = new RamHDLGeneratorFactory();
    }
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    super.instanceAttributeChanged(instance, attr);
    if ((attr == Mem.DATA_ATTR)
        || (attr == RamAttributes.ATTR_DBUS)
        || (attr == StdAttr.TRIGGER)
        || (attr == RamAttributes.ATTR_ByteEnables)
        || (attr == StdAttr.APPEARANCE)
        || (attr == Mem.LINE_ATTR)
        || (attr == RamAttributes.CLEAR_PIN)
        || (attr == Mem.ENABLES_ATTR)) {
      instance.recomputeBounds();
      configurePorts(instance);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    if (RamAppearance.classicAppearance(painter.getAttributeSet())) {
      RamAppearance.DrawRamClassic(painter);
    } else {
      RamAppearance.DrawRamEvolution(painter);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    AttributeSet attrs = state.getAttributeSet();
    RamState myState = (RamState) getState(state);
    
    // first we check the clear pin
    if (attrs.getValue(RamAttributes.CLEAR_PIN)) {
      Value clearValue = state.getPortValue(RamAppearance.getClrIndex(0, attrs));
      if (clearValue.equals(Value.TRUE)) {
        myState.getContents().clear();
        BitWidth dataBits = state.getAttributeValue(DATA_ATTR);
        if (isSeparate(attrs)) {
          for (int i = 0 ; i < RamAppearance.getNrDataOutPorts(attrs) ; i++)
            state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createKnown(dataBits,0), DELAY);
        } else {
          for (int i = 0 ; i < RamAppearance.getNrDataOutPorts(attrs) ; i++)
            state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createUnknown(dataBits), DELAY);
        }
        return;
      }
    }

    // next we get the address and the mem value currently stored
    Value addrValue = state.getPortValue(RamAppearance.getAddrIndex(0, attrs));
    long addr = addrValue.toLongValue();
    boolean goodAddr = addrValue.isFullyDefined() && addr >= 0 ;
    if (goodAddr && addr != myState.getCurrent()) {
      myState.setCurrent(addr);
      myState.scrollToShow(addr);
    }
    
    // now we handle the two different behaviors, line-enables or byte-enables
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      propagateLineEnables(state,addr,goodAddr,addrValue.isErrorValue());
    } else {
      propagateByteEnables(state,addr,goodAddr,addrValue.isErrorValue());
    }
  }
  
  private void propagateLineEnables(InstanceState state, long addr, boolean goodAddr, boolean errorValue) {
    AttributeSet attrs = state.getAttributeSet();
    RamState myState = (RamState) getState(state);
    boolean separate = isSeparate(attrs);

    int dataLines = Math.max(1, RamAppearance.getNrLEPorts(attrs));
    boolean misaligned = addr % dataLines != 0;
    boolean misalignError = misaligned && !state.getAttributeValue(ALLOW_MISALIGNED);

    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    boolean triggered = myState.setClock(state.getPortValue(RamAppearance.getClkIndex(0, attrs)), trigger);
    boolean writeEnabled = triggered && (state.getPortValue(RamAppearance.getWEIndex(0, attrs)) == Value.TRUE);
    if (writeEnabled && goodAddr && !misalignError) {
      for (int i = 0; i < dataLines; i++) {
        if (dataLines > 1) {
          Value le = state.getPortValue(RamAppearance.getLEIndex(i, attrs));
          if (le != null && le.equals(Value.FALSE))
            continue;
        }
        long dataValue = state.getPortValue(RamAppearance.getDataInIndex(i, attrs)).toLongValue();
        myState.getContents().set(addr+i, dataValue);
      }
    }

    // perform reads
    BitWidth width = state.getAttributeValue(DATA_ATTR);
    boolean outputEnabled = separate || !state.getPortValue(RamAppearance.getOEIndex(0, attrs)).equals(Value.FALSE);
    if (outputEnabled && goodAddr && !misalignError) {
      for (int i = 0; i < dataLines; i++) {
        long val = myState.getContents().get(addr+i);
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createKnown(width, val), DELAY);
      }
    } else if (outputEnabled && (errorValue || (goodAddr && misalignError))) {
      for (int i = 0; i < dataLines; i++)
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createError(width), DELAY);
    } else {
      for (int i = 0; i < dataLines; i++)
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createUnknown(width), DELAY);
    }
  }
  
  private void propagateByteEnables(InstanceState state, long addr, boolean goodAddr, boolean errorValue) {
    AttributeSet attrs = state.getAttributeSet();
    RamState myState = (RamState) getState(state);
    boolean separate = isSeparate(attrs);
    long oldMemValue = myState.getContents().get(myState.getCurrent());
    long newMemValue = oldMemValue;
    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    Value weValue = state.getPortValue(RamAppearance.getWEIndex(0, attrs));
    boolean async = trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    boolean edge = async ? false :  myState.setClock(state.getPortValue(RamAppearance.getClkIndex(0, attrs)), trigger);
    boolean weAsync = (trigger.equals(StdAttr.TRIG_HIGH) && weValue.equals(Value.TRUE)) || 
                      (trigger.equals(StdAttr.TRIG_LOW) && weValue.equals(Value.FALSE));
    boolean weTriggered = (async && weAsync) || (edge && weValue.equals(Value.TRUE));
    if (goodAddr && weTriggered) {
      long dataInValue = state.getPortValue(RamAppearance.getDataInIndex(0, attrs)).toLongValue();
      if (RamAppearance.getNrBEPorts(attrs) == 0) {
        newMemValue = dataInValue;
      } else {
        for (int i = 0 ; i < RamAppearance.getNrBEPorts(attrs) ; i++) {
          long mask = 0xFF << (i*8);
          long andMask = mask ^ (-1L);
          if (state.getPortValue(RamAppearance.getBEIndex(i, attrs)).equals(Value.TRUE)) {
            newMemValue &= andMask;
            newMemValue |= (dataInValue & mask);
          }
        }
      }
      myState.getContents().set(addr, newMemValue);
    }
    
    // perform reads
    BitWidth dataBits = state.getAttributeValue(DATA_ATTR);
    boolean outputNotEnabled = state.getPortValue(RamAppearance.getOEIndex(0, attrs)).equals(Value.FALSE);
    if (!separate && outputNotEnabled) {
      /* put the bus in tri-state in case of a combined bus and no output enable */
      state.setPort(RamAppearance.getDataOutIndex(0, attrs), Value.createUnknown(dataBits), DELAY);
      return;
    }
    /* if the OE is not activated return */
    if (outputNotEnabled) return;
    
    /* if the address is bogus set error value */
    if (!goodAddr || errorValue) {
      state.setPort(RamAppearance.getDataOutIndex(0, attrs), Value.createError(dataBits), DELAY);
      return;
    }
    
    boolean asyncRead = async || attrs.getValue(Mem.ASYNC_READ);
    
    if (asyncRead) {
      state.setPort(RamAppearance.getDataOutIndex(0, attrs), Value.createKnown(dataBits, newMemValue), DELAY);
      return;
    }
    
    if (edge) {
      if (attrs.getValue(Mem.READ_ATTR).equals(Mem.READAFTERWRITE))
        state.setPort(RamAppearance.getDataOutIndex(0, attrs), Value.createKnown(dataBits, newMemValue), DELAY);
      else
        state.setPort(RamAppearance.getDataOutIndex(0, attrs), Value.createKnown(dataBits, oldMemValue), DELAY);
    }
  }
  
  @Override
  public void removeComponent(Circuit circ, Component c , CircuitState state) {
    if (state != null) closeHexFrame((RamState)state.getData(c));  
  }
  
  public static boolean isSeparate(AttributeSet attrs) {
    Object bus = attrs.getValue(RamAttributes.ATTR_DBUS);
    return bus == null || bus.equals(RamAttributes.BUS_SEP);
  }

  @Override
  public boolean RequiresNonZeroLabel() {
    return true;
  }

  @Override
  public boolean CheckForGatedClocks(NetlistComponent comp) {
    return true;
  }

  @Override
  public int[] ClockPinIndex(NetlistComponent comp) {
    return new int[] {RamAppearance.getClkIndex(0, comp.GetComponent().getAttributeSet())};
  }
}
