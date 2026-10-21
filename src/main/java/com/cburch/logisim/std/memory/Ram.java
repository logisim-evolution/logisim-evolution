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
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.util.StringGetter;

import java.util.WeakHashMap;
import java.util.function.Consumer;

public class Ram extends Mem {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "RAM";

  public static class Logger extends InstanceLogger {

    @Override
    public String getLogName(InstanceState state, Object option) {
      var label = state.getAttributeValue(StdAttr.LABEL);
      if (label.equals("")) {
        label = null;
      }
      if (option instanceof Number) {
        if (label == null) {
          final var disp = S.get("ramComponent");
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
  private static final WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<>();

  public Ram(String name,
          StringGetter desc,
          HdlGeneratorFactory generator,
          boolean needsLabel) {
    super(name, desc, generator, needsLabel);
  }

  public Ram() {
    super(_ID, S.getter("ramComponent"), new RamHdlGeneratorFactory(), true);
    setIcon(new ArithmeticIcon("RAM", 3));
    setInstanceLogger(Logger.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    instance.addAttributeListener();
  }

  @Override
  void configurePorts(Instance instance) {
    if (RamMultiPortAppearance.isMultiPort(instance.getAttributeSet())) {
      RamMultiPortAppearance.configurePorts(instance);
    } else {
      RamAppearance.configurePorts(instance);
    }
  }

  @Override
  public AttributeSet createAttributeSet() {
    // Only the plain RAM component offers the multi-port option. Dual RAM (a subclass of Ram)
    // and ROM keep their original attribute set.
    return new RamAttributes(getClass() == Ram.class);
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    // The inline poker of the single-port RAM only knows the first port, so it would edit the
    // wrong pins in multi-port mode. Disable it there and let the user edit the contents with the
    // regular memory window instead.
    if (key == Pokable.class && RamMultiPortAppearance.isMultiPort(instance.getAttributeSet())) {
      return null;
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var label = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
    return (label.length() == 0)
            ? "RAM"
            : "RAMCONTENTS_" + label;
  }

  private MemContents getNewContents(AttributeSet attrs) {
    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(), attrs.getValue(Mem.DATA_ATTR).getWidth(), true);
    contents.condFillRandom();
    return contents;
  }

  private static HexFrame getHexFrame(MemContents value, Project proj, Instance instance) {
    synchronized (windowRegistry) {
      var ret = windowRegistry.get(value);
      if (ret == null) {
        ret = new HexFrame(proj, instance, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }

  public static void closeHexFrame(RamState state) {
    final var contents = state.getContents();
    HexFrame ret;
    synchronized (windowRegistry) {
      ret = windowRegistry.remove(contents);
    }
    if (ret == null) return;
    ret.closeAndDispose();
  }

  @Override
  public HexFrame getHexFrame(Project proj, Instance instance, CircuitState circState) {
    final var ret = (RamState) instance.getData(circState);
    return getHexFrame((ret == null) ? getNewContents(instance.getAttributeSet()) : ret.getContents(), proj, instance);
  }

  public boolean reset(CircuitState state, Instance instance) {
    final var ret = (RamState) instance.getData(state);
    if (ret == null) return true;
    final var contents = ret.getContents();
    if (instance.getAttributeValue(RamAttributes.ATTR_TYPE).equals(RamAttributes.VOLATILE)) {
      contents.condClear();
    }
    return false;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    if (RamMultiPortAppearance.isMultiPort(attrs)) {
      return RamMultiPortAppearance.getBounds(attrs);
    }
    return RamAppearance.getBounds(attrs);
  }

  @Override
  MemState getState(Instance instance, CircuitState state) {
    return getState(state.getInstanceState(instance));
  }

  @Override
  MemState getState(InstanceState state) {
    var ret = (RamState) state.getData();
    if (ret == null) {
      final var instance = state.getInstance();
      ret = new RamState(instance, getNewContents(instance.getAttributeSet()), new MemListener(instance));
      state.setData(ret);
    } else {
      ret.setRam(state.getInstance());
    }
    return ret;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    super.instanceAttributeChanged(instance, attr);
    if ((attr == Mem.DATA_ATTR)
        || (attr == Mem.ADDR_ATTR)
        || (attr == RamAttributes.ATTR_PORTS)
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
    if (RamMultiPortAppearance.isMultiPort(painter.getAttributeSet())) {
      RamMultiPortAppearance.paint(painter);
    } else if (RamAppearance.classicAppearance(painter.getAttributeSet())) {
      RamAppearance.drawRamClassic(painter);
    } else {
      RamAppearance.drawRamEvolution(painter);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    final var attrs = state.getAttributeSet();
    if (RamMultiPortAppearance.isMultiPort(attrs)) {
      propagateMultiPort(state);
      return;
    }
    final var myState = (RamState) getState(state);

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

  /**
   * Propagation path used when more than one independent access port is configured.
   *
   * <p>All ports share the same memory contents. Ports are processed in ascending order, therefore
   * when two ports write the same address on the same propagation the port with the highest index
   * wins. This is deterministic and does not show any dialog while simulating.
   */
  private void propagateMultiPort(InstanceState state) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);
    final var ports = RamMultiPortAppearance.getNrPorts(attrs);
    final var lines = RamMultiPortAppearance.getDataLines(attrs);

    // The clear pin is shared by every port.
    if (Boolean.TRUE.equals(attrs.getValue(RamAttributes.CLEAR_PIN))) {
      final var clrIndex = RamMultiPortAppearance.getClrIndex(0, attrs);
      if (clrIndex >= 0 && Value.TRUE.equals(state.getPortValue(clrIndex))) {
        myState.getContents().clear();
        final var dataBits = state.getAttributeValue(DATA_ATTR);
        for (var p = 0; p < ports; p++) {
          for (var l = 0; l < lines; l++) {
            state.setPort(
                RamMultiPortAppearance.getDataOutIndex(p, l, attrs),
                Value.createKnown(dataBits, 0),
                DELAY);
          }
        }
        return;
      }
    }

    // Track the last address of every port. Port 0 also drives the scrolling of the memory view.
    for (var p = 0; p < ports; p++) {
      final var addrValue = state.getPortValue(RamMultiPortAppearance.getAddrIndex(p, attrs));
      final var addr = addrValue.toLongValue();
      if (addrValue.isFullyDefined() && addr >= 0 && addr != myState.getCurrent(p)) {
        myState.setCurrent(p, addr);
        if (p == 0) myState.scrollToShow(addr);
      }
    }

    final var lineMode = RamMultiPortAppearance.isLineMode(attrs);
    for (var p = 0; p < ports; p++) {
      final var addrValue = state.getPortValue(RamMultiPortAppearance.getAddrIndex(p, attrs));
      final var addr = addrValue.toLongValue();
      final var goodAddr = addrValue.isFullyDefined() && addr >= 0;
      if (lineMode) {
        propagateMultiPortLine(state, p, addr, goodAddr, addrValue.isErrorValue());
      } else {
        propagateMultiPortByte(state, p, addr, goodAddr, addrValue.isErrorValue());
      }
    }
  }

  private void propagateMultiPortLine(
      InstanceState state, int port, long addr, boolean goodAddr, boolean errorValue) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);
    final var lines = RamMultiPortAppearance.getDataLines(attrs);
    final var misaligned = addr % lines != 0;
    final var misalignError = misaligned && !state.getAttributeValue(ALLOW_MISALIGNED);

    final Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    final var clkIndex = RamMultiPortAppearance.getClkIndex(port, attrs);
    final var triggered =
        clkIndex >= 0 && myState.setClock(port, state.getPortValue(clkIndex), trigger);
    final var writeEnabled =
        triggered
            && state.getPortValue(RamMultiPortAppearance.getWEIndex(port, attrs)) == Value.TRUE;

    if (writeEnabled && goodAddr && !misalignError) {
      for (var i = 0; i < lines; i++) {
        if (lines > 1
            && !Value.TRUE.equals(
                state.getPortValue(RamMultiPortAppearance.getLEIndex(port, i, attrs)))) {
          continue;
        }
        final long dataValue =
            state
                .getPortValue(RamMultiPortAppearance.getDataInIndex(port, i, attrs))
                .toLongValue();
        myState.getContents().set(addr + i, dataValue);
      }
    }

    final var width = state.getAttributeValue(DATA_ATTR);
    for (var i = 0; i < lines; i++) {
      final var outIndex = RamMultiPortAppearance.getDataOutIndex(port, i, attrs);
      if (goodAddr && !misalignError) {
        final long val = myState.getContents().get(addr + i);
        state.setPort(outIndex, Value.createKnown(width, val), DELAY);
      } else if (errorValue || (goodAddr && misalignError)) {
        state.setPort(outIndex, Value.createError(width), DELAY);
      } else {
        state.setPort(outIndex, Value.createUnknown(width), DELAY);
      }
    }
  }

  private void propagateMultiPortByte(
      InstanceState state, int port, long addr, boolean goodAddr, boolean errorValue) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);
    final var oldMemValue = myState.getContents().get(myState.getCurrent(port));
    var newMemValue = oldMemValue;

    final Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    final var async = trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    final var weValue = state.getPortValue(RamMultiPortAppearance.getWEIndex(port, attrs));
    final var clkIndex = RamMultiPortAppearance.getClkIndex(port, attrs);
    final var edge =
        !async && clkIndex >= 0 && myState.setClock(port, state.getPortValue(clkIndex), trigger);
    final var weAsync =
        (trigger.equals(StdAttr.TRIG_HIGH) && weValue.equals(Value.TRUE))
            || (trigger.equals(StdAttr.TRIG_LOW) && weValue.equals(Value.FALSE));
    final var weTriggered = (async && weAsync) || (edge && weValue.equals(Value.TRUE));

    if (goodAddr && weTriggered) {
      final long dataInValue =
          state.getPortValue(RamMultiPortAppearance.getDataInIndex(port, 0, attrs)).toLongValue();
      final var bytes = RamMultiPortAppearance.getBytesPerWord(attrs);
      if (bytes == 0) {
        newMemValue = dataInValue;
      } else {
        for (var b = 0; b < bytes; b++) {
          final long mask = 0xFFL << (b * 8);
          final long andMask = ~mask;
          if (state.getPortValue(RamMultiPortAppearance.getBEIndex(port, b, attrs)).equals(Value.TRUE)) {
            newMemValue &= andMask;
            newMemValue |= (dataInValue & mask);
          }
        }
      }
      myState.getContents().set(addr, newMemValue);
    }

    final var dataBits = state.getAttributeValue(DATA_ATTR);
    final var oeIndex = RamMultiPortAppearance.getOEIndex(port, attrs);
    final var outputNotEnabled =
        oeIndex >= 0 && state.getPortValue(oeIndex).equals(Value.FALSE);
    if (outputNotEnabled) {
      // Multi-port mode always uses separate data buses, so the previous value stays driven.
      return;
    }

    final var outIndex = RamMultiPortAppearance.getDataOutIndex(port, 0, attrs);
    if (errorValue) {
      state.setPort(outIndex, Value.createError(dataBits), DELAY);
      return;
    }
    if (!goodAddr) {
      state.setPort(outIndex, Value.createUnknown(dataBits), DELAY);
      return;
    }

    final var asyncRead = async || Boolean.TRUE.equals(attrs.getValue(Mem.ASYNC_READ));
    if (asyncRead) {
      state.setPort(outIndex, Value.createKnown(dataBits, newMemValue), DELAY);
      return;
    }
    if (edge) {
      if (attrs.getValue(Mem.READ_ATTR).equals(Mem.READAFTERWRITE)) {
        state.setPort(outIndex, Value.createKnown(dataBits, newMemValue), DELAY);
      } else {
        state.setPort(outIndex, Value.createKnown(dataBits, oldMemValue), DELAY);
      }
    }
  }

  private void propagateLineEnables(InstanceState state, long addr, boolean goodAddr, boolean errorValue) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);
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
    final var outputEnabled = separate
        || !state.getPortValue(RamAppearance.getOEIndex(0, attrs)).equals(Value.FALSE);
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
    final var myState = (RamState) getState(state);
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

    if (outputNotEnabled) {
      if (!separate || RamAttributes.hasControlledOutput(attrs)) {
        setValue.accept(Value.createUnknown(dataBits));
      }
      return;
    }

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
    if (state != null) closeHexFrame((RamState) state.getData(c));
  }

  public static boolean isSeparate(AttributeSet attrs) {
    Object bus = attrs.getValue(RamAttributes.ATTR_DBUS);
    return RamAttributes.isSeparateDataBus(bus);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    final var attrs = comp.getComponent().getAttributeSet();
    if (RamMultiPortAppearance.isMultiPort(attrs)) {
      final var nrClocks = RamMultiPortAppearance.getNrClkPorts(attrs);
      final var ret = new int[nrClocks];
      for (var i = 0; i < nrClocks; i++) {
        ret[i] = RamMultiPortAppearance.getClkIndex(i, attrs);
      }
      return ret;
    }
    return new int[] {RamAppearance.getClkIndex(0, attrs)};
  }
}
