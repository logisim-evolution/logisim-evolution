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

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.swing.JLabel;

public class Ram extends Mem {

  static class ContentsAttribute extends Attribute<MemContents> {

    public ContentsAttribute() {
      super("contents", S.getter("ramContentsAttr"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, MemContents value) {
      ContentsCell ret = new ContentsCell(source, value);
      ret.mouseClicked(null);
      return ret;
    }

    public MemContents parse(String value) {
      int lineBreak = value.indexOf('\n');
      String first = lineBreak < 0 ? value : value.substring(0, lineBreak);
      String rest = lineBreak < 0 ? "" : value.substring(lineBreak + 1);
      StringTokenizer toks = new StringTokenizer(first);
      try {
        String header = toks.nextToken();
        if (!header.equals("addr/data:")) {
          return null;
        }
        int addr = Integer.parseInt(toks.nextToken());
        int data = Integer.parseInt(toks.nextToken());
        MemContents ret = MemContents.create(addr, data, false);
        HexFile.open(ret, new StringReader(rest));
        return ret;
      } catch (IOException e) {
        return null;
      } catch (NumberFormatException e) {
        return null;
      } catch (NoSuchElementException e) {
        return null;
      }
    }

    @Override
    public String toDisplayString(MemContents value) {
      return S.get("romContentsValue");
    }

    @Override
    public String toStandardString(MemContents state) {
      int addr = state.getLogLength();
      int data = state.getWidth();
      StringWriter ret = new StringWriter();
      ret.write("addr/data: " + addr + " " + data + "\n");
      try {
        HexFile.save(ret, state);
      } catch (IOException e) {
      }
      return ret.toString();
    }
  }

  @SuppressWarnings("serial")
  private static class ContentsCell extends JLabel implements MouseListener {

    Window source;
    MemContents contents;

    ContentsCell(Window source, MemContents contents) {
      super(S.get("romContentsValue"));
      this.source = source;
      this.contents = contents;
      addMouseListener(this);
    }

    public void mouseClicked(MouseEvent e) {
      if (contents == null) {
        return;
      }
      Project proj = source instanceof Frame ? ((Frame) source).getProject() : null;
      HexFrame frame = RamAttributes.getHexFrame(contents, proj);
      frame.setVisible(true);
      frame.toFront();
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}
  }

  public static class Logger extends InstanceLogger {

    @Override
    public String getLogName(InstanceState state, Object option) {
      String Label = state.getAttributeValue(StdAttr.LABEL);
      if (Label.equals("")) {
        Label = null;
      }
      if (option instanceof Integer) {
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
          for (int i = 0; i < ret.length; i++) {
            ret[i] = Integer.valueOf(i);
          }
        }
        return ret;
      }
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      if (option instanceof Integer) {
        MemState s = (MemState) state.getData();
        int addr = ((Integer) option).intValue();
        return Value.createKnown(BitWidth.create(s.getDataBits()), s.getContents().get(addr));
      } else {
        return Value.NIL;
      }
    }
  }

  public static Attribute<MemContents> CONTENTS_ATTR = new ContentsAttribute();
  private static Object[][] logOptions = new Object[9][];

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

  @Override
  HexFrame getHexFrame(Project proj, Instance instance, CircuitState circState) {
    RamState ret = (RamState) instance.getData(circState);
    return RamAttributes.getHexFrame(
        (ret == null) ? instance.getAttributeValue(CONTENTS_ATTR) : ret.getContents(), proj);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return RamAppearance.getBounds(attrs);
  }

  @Override
  MemState getState(Instance instance, CircuitState state) {
    RamState ret = (RamState) instance.getData(state);
    if (ret == null) {
      MemContents contents = instance.getAttributeValue(Ram.CONTENTS_ATTR);
      ret = new RamState(instance, contents.clone(), new MemListener(instance));
      instance.setData(state, ret);
    } else {
      ret.setRam(instance);
    }
    return ret;
  }

  @Override
  MemState getState(InstanceState state) {
    RamState ret = (RamState) state.getData();
    if (ret == null) {
      MemContents contents = state.getInstance().getAttributeValue(Ram.CONTENTS_ATTR);
      Instance instance = state.getInstance();
      ret = new RamState(instance, contents.clone(), new MemListener(instance));
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
    int addr = addrValue.toIntValue();
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
  
  private void propagateLineEnables(InstanceState state, int addr, boolean goodAddr, boolean errorValue) {
    AttributeSet attrs = state.getAttributeSet();
    RamState myState = (RamState) getState(state);
    boolean separate = isSeparate(attrs);

    int dataLines = Math.max(1, RamAppearance.getNrLEPorts(attrs));

    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    boolean triggered = myState.setClock(state.getPortValue(RamAppearance.getClkIndex(0, attrs)), trigger);
    boolean writeEnabled = triggered && (state.getPortValue(RamAppearance.getWEIndex(0, attrs)) == Value.TRUE);
    if (writeEnabled && goodAddr && (addr % dataLines == 0)) {
      for (int i = 0; i < dataLines; i++) {
        if (dataLines > 1) {
          Value le = state.getPortValue(RamAppearance.getLEIndex(i, attrs));
          if (le != null && le.equals(Value.FALSE))
            continue;
        }
        int dataValue = state.getPortValue(RamAppearance.getDataInIndex(i, attrs)).toIntValue();
        myState.getContents().set(addr+i, dataValue);
      }
    }

    // perform reads
    BitWidth width = state.getAttributeValue(DATA_ATTR);
    boolean outputEnabled = separate || !state.getPortValue(RamAppearance.getOEIndex(0, attrs)).equals(Value.FALSE);
    if (outputEnabled && goodAddr && (addr % dataLines == 0)) {
      for (int i = 0; i < dataLines; i++) {
        int val = myState.getContents().get(addr+i);
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createKnown(width, val), DELAY);
      }
    } else if (outputEnabled && (errorValue || (goodAddr && (addr % dataLines != 0)))) {
      for (int i = 0; i < dataLines; i++)
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createError(width), DELAY);
    } else {
      for (int i = 0; i < dataLines; i++)
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createUnknown(width), DELAY);
    }
  }
  
  private void propagateByteEnables(InstanceState state, int addr, boolean goodAddr, boolean errorValue) {
    AttributeSet attrs = state.getAttributeSet();
    RamState myState = (RamState) getState(state);
    boolean separate = isSeparate(attrs);
    int oldMemValue = myState.getContents().get(myState.getCurrent());
    int newMemValue = oldMemValue;
    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    Value weValue = state.getPortValue(RamAppearance.getWEIndex(0, attrs));
    boolean async = trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    boolean edge = async ? false :  myState.setClock(state.getPortValue(RamAppearance.getClkIndex(0, attrs)), trigger);
    boolean weAsync = (trigger.equals(StdAttr.TRIG_HIGH) && weValue.equals(Value.TRUE)) || 
                      (trigger.equals(StdAttr.TRIG_LOW) && weValue.equals(Value.FALSE));
    boolean weTriggered = (async && weAsync) || (edge && weValue.equals(Value.TRUE));
    if (goodAddr && weTriggered) {
      int dataInValue = state.getPortValue(RamAppearance.getDataInIndex(0, attrs)).toIntValue();
      if (RamAppearance.getNrBEPorts(attrs) == 0) {
        newMemValue = dataInValue;
      } else {
        for (int i = 0 ; i < RamAppearance.getNrBEPorts(attrs) ; i++) {
          int mask = 0xFF << (i*8);
          int andMask = mask ^ (-1);
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
