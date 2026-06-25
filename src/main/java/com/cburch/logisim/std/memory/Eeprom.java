/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javax.swing.JLabel;

public class Eeprom extends Mem {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "EEPROM";

  public static class Logger extends InstanceLogger {

    @Override
    public String getLogName(InstanceState state, Object option) {
      var label = state.getAttributeValue(StdAttr.LABEL);
      if (label.equals("")) {
        label = null;
      }
      if (option instanceof Number) {
        if (label == null) {
          final var disp = S.get("eepromComponent");
          final var loc = state.getInstance().getLocation();
          return disp + loc + "[" + option + "]";
        }
        return label + "[" + option + "]";
      } else {
        return label;
      }
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return state.getAttributeValue(Mem.DATA_ATTR);
    }

    @Override
    public Object[] getLogOptions(InstanceState state) {
      var addrBits = state.getAttributeValue(ADDR_ATTR).getWidth();
      if (addrBits >= logOptions.length) {
        addrBits = logOptions.length - 1;
      }
      synchronized (logOptions) {
        var ret = logOptions[addrBits];
        if (ret == null) {
          ret = new Object[1 << addrBits];
          logOptions[addrBits] = ret;
          for (var i = 0; i < ret.length; i++) {
            ret[i] = i;
          }
        }
        return ret;
      }
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      if (option instanceof Number addr) {
        final var memState = (MemState) state.getData();
        return Value.createKnown(BitWidth.create(memState.getDataBits()), memState.getContents().get(addr.longValue()));
      } else {
        return Value.NIL;
      }
    }
  }

  private static final Object[][] logOptions = new Object[9][];

  static class ContentsAttribute extends Attribute<MemContents> {
    public ContentsAttribute() {
      super("contents", S.getter("romContentsAttr"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, MemContents value) {
      if (source instanceof Frame frame) {
        final var proj = frame.getProject();
        EepromAttributes.register(value, proj);
      }
      final var ret = new ContentsCell(source, value);
      ret.mouseClicked(null);
      return ret;
    }

    @Override
    public MemContents parse(String value) {
      final var lineBreak = value.indexOf('\n');
      final var first = lineBreak < 0 ? value : value.substring(0, lineBreak);
      final var rest = lineBreak < 0 ? "" : value.substring(lineBreak + 1);
      final var toks = new StringTokenizer(first);
      try {
        final var header = toks.nextToken();
        if (!header.equals("addr/data:")) return null;
        final var addr = Integer.parseInt(toks.nextToken());
        final var data = Integer.parseInt(toks.nextToken());
        return HexFile.parseFromCircFile(rest, addr, data);
      } catch (IOException | NoSuchElementException | NumberFormatException e) {
        return null;
      }
    }

    @Override
    public String toDisplayString(MemContents value) {
      return S.get("romContentsValue");
    }

    @Override
    public String toStandardString(MemContents state) {
      final var addr = state.getLogLength();
      final var data = state.getWidth();
      final var contents = HexFile.saveToString(state);
      return "addr/data: " + addr + " " + data + "\n" + contents;
    }
  }

  @SuppressWarnings("serial")
  private static class ContentsCell extends JLabel implements BaseMouseListenerContract {
    final Window source;
    final MemContents contents;

    ContentsCell(Window source, MemContents contents) {
      super(S.get("romContentsValue"));
      this.source = source;
      this.contents = contents;
      addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (contents == null) return;
      final var proj = (source instanceof Frame frame) ? frame.getProject() : null;
      final var frame = EepromAttributes.getHexFrame(contents, proj, null);
      frame.setVisible(true);
      frame.toFront();
    }
  }

  public static final Attribute<MemContents> CONTENTS_ATTR = new ContentsAttribute();

  // The following is so that instance's MemListeners aren't freed by the
  // garbage collector until the instance itself is ready to be freed.
  private final WeakHashMap<Instance, MemListener> memListeners;

  public Eeprom() {
    super(_ID, S.getter("eepromComponent"), 0, null, true);
    setIcon(new ArithmeticIcon("EEPROM", 3));
    memListeners = new WeakHashMap<>();
    setInstanceLogger(Logger.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    final var contents = getMemContents(instance);
    final var listener = new MemListener(instance);
    memListeners.put(instance, listener);
    contents.addHexModelListener(listener);
    instance.addAttributeListener();
  }

  @Override
  void configurePorts(Instance instance) {
    RamAppearance.configurePorts(instance);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new EepromAttributes();
  }

  public static MemContents getMemContents(Instance instance) {
    return instance.getAttributeValue(CONTENTS_ATTR);
  }

  public static void closeHexFrame(Component c) {
    if (!(c instanceof InstanceComponent)) return;
    final var inst = ((InstanceComponent) c).getInstance();
    EepromAttributes.closeHexFrame(getMemContents(inst));
  }

  @Override
  HexFrame getHexFrame(Project proj, Instance instance, CircuitState state) {
    return EepromAttributes.getHexFrame(getMemContents(instance), proj, instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return RamAppearance.getBounds(attrs);
  }

  @Override
  MemState getState(Instance instance, CircuitState state) {
    var ret = (EepromState) instance.getData(state);
    if (ret == null) {
      final var contents = getMemContents(instance);
      ret = new EepromState(contents);
      instance.setData(state, ret);
    }
    return ret;
  }

  @Override
  MemState getState(InstanceState state) {
    var ret = (EepromState) state.getData();
    if (ret == null) {
      final var contents = getMemContents(state.getInstance());
      ret = new EepromState(contents);
      state.setData(ret);
    }
    return ret;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    super.instanceAttributeChanged(instance, attr);
    if ((attr == Mem.DATA_ATTR)
        || (attr == Mem.ADDR_ATTR)
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
      RamAppearance.drawRamClassic(painter);
    } else {
      RamAppearance.drawRamEvolution(painter);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    final var attrs = state.getAttributeSet();
    final var myState = (EepromState) getState(state);

    // first we check the clear pin
    if (attrs.getValue(RamAttributes.CLEAR_PIN)) {
      final var clearValue = state.getPortValue(RamAppearance.getClrIndex(0, attrs));
      if (clearValue.equals(Value.TRUE)) {
        myState.getContents().clear();
        final var dataBits = state.getAttributeValue(DATA_ATTR);

        for (var i = 0; i < RamAppearance.getNrDataOutPorts(attrs); i++) {
          final var portVal = isSeparate(attrs)
                  ? Value.createKnown(dataBits, 0)
                  : Value.createUnknown(dataBits);
          state.setPort(RamAppearance.getDataOutIndex(i, attrs), portVal, DELAY);
        }

        return;
      }
    }

    // next we get the address and the mem value currently stored
    final var addrValue = state.getPortValue(RamAppearance.getAddrIndex(0, attrs));
    long addr = addrValue.toLongValue();
    final var goodAddr = addrValue.isFullyDefined() && addr >= 0;
    if (goodAddr && addr != myState.getCurrent()) {
      myState.setCurrent(addr);
      myState.scrollToShow(addr);
    }

    // now we handle the two different behaviors, line-enables or byte-enables
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      propagateLineEnables(state, addr, goodAddr, addrValue.isErrorValue());
    } else {
      propagateByteEnables(state, addr, goodAddr, addrValue.isErrorValue());
    }
  }

  private void propagateLineEnables(InstanceState state, long addr, boolean goodAddr, boolean errorValue) {
    final var attrs = state.getAttributeSet();
    final var myState = (EepromState) getState(state);
    final var separate = isSeparate(attrs);

    final var dataLines = Math.max(1, RamAppearance.getNrLEPorts(attrs));
    final var misaligned = addr % dataLines != 0;
    final var misalignError = misaligned && !state.getAttributeValue(ALLOW_MISALIGNED);

    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    final var triggered = myState.setClock(state.getPortValue(RamAppearance.getClkIndex(0, attrs)), trigger);
    final var writeEnabled = triggered && (state.getPortValue(RamAppearance.getWEIndex(0, attrs)) == Value.TRUE);
    if (writeEnabled && goodAddr && !misalignError) {
      for (var i = 0; i < dataLines; i++) {
        if (dataLines > 1) {
          final var le = state.getPortValue(RamAppearance.getLEIndex(i, attrs));
          if (!Value.TRUE.equals(le))
            continue;
        }
        long dataValue = state.getPortValue(RamAppearance.getDataInIndex(i, attrs)).toLongValue();
        myState.getContents().set(addr + i, dataValue);
      }
    }

    // perform reads
    final var width = state.getAttributeValue(DATA_ATTR);
    final var outputEnabled = separate || !state.getPortValue(RamAppearance.getOEIndex(0, attrs)).equals(Value.FALSE);
    if (outputEnabled && goodAddr && !misalignError) {
      for (var i = 0; i < dataLines; i++) {
        long val = myState.getContents().get(addr + i);
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createKnown(width, val), DELAY);
      }
    } else if (outputEnabled && (errorValue || (goodAddr && misalignError))) {
      for (var i = 0; i < dataLines; i++)
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createError(width), DELAY);
    } else {
      for (var i = 0; i < dataLines; i++)
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createUnknown(width), DELAY);
    }
  }

  private void propagateByteEnables(InstanceState state, long addr, boolean goodAddr, boolean errorValue) {
    final var attrs = state.getAttributeSet();
    final var myState = (EepromState) getState(state);
    final var separate = isSeparate(attrs);
    long oldMemValue = myState.getContents().get(myState.getCurrent());
    long newMemValue = oldMemValue;
    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    final var weValue = state.getPortValue(RamAppearance.getWEIndex(0, attrs));
    final var async = trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    final var edge =
        !async && myState
            .setClock(state.getPortValue(RamAppearance.getClkIndex(0, attrs)), trigger);
    final var weAsync =
        (trigger.equals(StdAttr.TRIG_HIGH) && weValue.equals(Value.TRUE))
            || (trigger.equals(StdAttr.TRIG_LOW) && weValue.equals(Value.FALSE));
    final var weTriggered = (async && weAsync) || (edge && weValue.equals(Value.TRUE));
    if (goodAddr && weTriggered) {
      long dataInValue = state.getPortValue(RamAppearance.getDataInIndex(0, attrs)).toLongValue();
      if (RamAppearance.getNrBEPorts(attrs) == 0) {
        newMemValue = dataInValue;
      } else {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          long mask = 0xFFL << (i * 8);
          long andMask = ~mask;
          if (state.getPortValue(RamAppearance.getBEIndex(i, attrs)).equals(Value.TRUE)) {
            newMemValue &= andMask;
            newMemValue |= (dataInValue & mask);
          }
        }
      }
      myState.getContents().set(addr, newMemValue);
    }

    // perform reads
    final var dataBits = state.getAttributeValue(DATA_ATTR);
    final var outputNotEnabled = state.getPortValue(RamAppearance.getOEIndex(0, attrs)).equals(Value.FALSE);

    Consumer<Value> setValue = (Value value) -> state.setPort(RamAppearance.getDataOutIndex(0, attrs), value, DELAY);

    if (!separate && outputNotEnabled) {
      /* put the bus in tri-state in case of a combined bus and no output enable */
      setValue.accept(Value.createUnknown(dataBits));
      return;
    }
    /* if the OE is not activated return */
    if (outputNotEnabled) return;

    /* if the address is bogus set error value accordingly */

    if (errorValue) {
      setValue.accept(Value.createError(dataBits));
      return;
    }

    if (!goodAddr) {
      setValue.accept(Value.createUnknown(dataBits));
      return;
    }

    final var asyncRead = async || attrs.getValue(Mem.ASYNC_READ);

    if (asyncRead) {
      setValue.accept(Value.createKnown(dataBits, newMemValue));
      return;
    }

    if (edge) {
      if (attrs.getValue(Mem.READ_ATTR).equals(Mem.READAFTERWRITE))
        setValue.accept(Value.createKnown(dataBits, newMemValue));
      else
        setValue.accept(Value.createKnown(dataBits, oldMemValue));
    }
  }

  @Override
  public void removeComponent(Circuit circ, Component c, CircuitState state) {
    closeHexFrame(c);
  }

  public static boolean isSeparate(AttributeSet attrs) {
    Object bus = attrs.getValue(RamAttributes.ATTR_DBUS);
    return bus == null || bus.equals(RamAttributes.BUS_SEP);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {RamAppearance.getClkIndex(0, comp.getComponent().getAttributeSet())};
  }
}
