/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.*;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.proj.Project;

import java.util.WeakHashMap;
import java.util.function.Consumer;

import static com.cburch.logisim.std.Strings.S;

public class DualportRam extends Mem {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "DPRAM";

  public static class Logger extends InstanceLogger {

    @Override
    public String getLogName(InstanceState state, Object option) {
      var label = state.getAttributeValue(StdAttr.LABEL);
      if (label.equals("")) {
        label = null;
      }
      if (option instanceof Long) {
        final var disp = S.get("dpramComponent");
        final var loc = state.getInstance().getLocation();
        return (label == null) ? disp + loc + "[" + option + "]" : label + "[" + option + "]";
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
      if (option instanceof Long addr) {
        final var memState = (MemState) state.getData();
        return Value.createKnown(BitWidth.create(memState.getDataBits()), memState.getContents().get(addr));
      } else {
        return Value.NIL;
      }
    }
  }

  private static final Object[][] logOptions = new Object[9][];
  private static final WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<>();

  public DualportRam() {
    super(_ID, S.getter("dpramComponent"), 3, new DualportRamHdlGeneratorFactory(), true);
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
    DualportRamAppearance.configurePorts(instance);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new DualportRamAttributes();
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var label = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
    return (label.length() == 0)
            ? "DPRAM"
            : "DPRAMCONTENTS_" + label;
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
    if (instance.getAttributeValue(DualportRamAttributes.ATTR_TYPE).equals(DualportRamAttributes.VOLATILE)) {
      contents.condClear();
    }
    return false;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return DualportRamAppearance.getBounds(attrs);
  }

  public MemContents getContents(InstanceState ramState) {
    return getState(ramState).getContents();
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
        || (attr == StdAttr.TRIGGER)
        || (attr == DualportRamAttributes.ATTR_ByteEnables)
        || (attr == StdAttr.APPEARANCE)
        || (attr == Mem.LINE_ATTR)
        || (attr == DualportRamAttributes.CLEAR_PIN)
        || (attr == Mem.ENABLES_ATTR)) {
      instance.recomputeBounds();
      configurePorts(instance);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    if (DualportRamAppearance.classicAppearance(painter.getAttributeSet())) {
      DualportRamAppearance.drawRamClassic(painter);
    } else {
      DualportRamAppearance.drawRamEvolution(painter);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);

    // first we check the clear pin
    if (attrs.getValue(DualportRamAttributes.CLEAR_PIN)) {
      final var clearValue = state.getPortValue(DualportRamAppearance.getClrIndex(0, attrs));
      if (clearValue.equals(Value.TRUE)) {
        myState.getContents().clear();
        final var dataBits = state.getAttributeValue(DATA_ATTR);

        for (var i = 0; i < DualportRamAppearance.getNrDataOutPorts(attrs); i++) {
          final var portVal = Value.createKnown(dataBits, 0);
          state.setPort(DualportRamAppearance.getDataOutIndex(i, attrs), portVal, DELAY);
        }

        return;
      }
    }

    // next we get the address and the mem value currently stored
    final var addrValue = state.getPortValue(DualportRamAppearance.getAddrIndex(0, attrs));
    final var addrValue2 = state.getPortValue(DualportRamAppearance.getAddrIndex(1, attrs));
    final var addrValueWrite = state.getPortValue(DualportRamAppearance.getAddrIndex(2, attrs));
    long addr = addrValue.toLongValue();
    long addr2 = addrValue2.toLongValue();
    long addrWrite = addrValueWrite.toLongValue();
    final var goodAddr = addrValue.isFullyDefined() && addr >= 0;
    final var goodAddr2 = addrValue2.isFullyDefined() && addr >= 0;
    final var goodAddrWrite = addrValueWrite.isFullyDefined() && addr >= 0;
    if (goodAddrWrite && addrWrite != myState.getCurrent()) {
      myState.setCurrent(addrWrite);
      myState.scrollToShow(addrWrite);
    }

    // now we handle the two different behaviors, line-enables or byte-enables
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      propagateLineEnables(state, addr, goodAddr, addr2, goodAddr2, addrWrite, goodAddrWrite, addrValue.isErrorValue(), addrValue2.isErrorValue(), addrValueWrite.isErrorValue());
    } else {
      propagateByteEnables(state, addr, goodAddr, addr2, goodAddr2, addrWrite, goodAddrWrite, addrValue.isErrorValue(), addrValue2.isErrorValue(), addrValueWrite.isErrorValue());
    }
  }

  private void propagateLineEnables(InstanceState state, long addr, boolean goodAddr,
                                    long addr2, boolean goodAddr2,
                                    long addrWrite, boolean goodAddrWrite,
                                    boolean errorValue, boolean errorValue2,
                                    boolean errorValueWrite) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);

    final var dataLines = Math.max(1, DualportRamAppearance.getNrLEPorts(attrs));
    final var misaligned = addr % dataLines != 0;
    final var misalignError = misaligned && !state.getAttributeValue(ALLOW_MISALIGNED);

    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    final var triggered = myState.setClock(state.getPortValue(DualportRamAppearance.getClkIndex(0, attrs)), trigger);
    final var writeEnabled = triggered && (state.getPortValue(DualportRamAppearance.getWEIndex(0, attrs)) == Value.TRUE);
    if (writeEnabled && goodAddrWrite && !misalignError) {
      long dataValue = state.getPortValue(DualportRamAppearance.getDataInIndex(0, attrs)).toLongValue();
      myState.getContents().set(addrWrite, dataValue);
    }

    // perform reads
    final var width = state.getAttributeValue(DATA_ATTR);
    final var outputEnabled = !state.getPortValue(DualportRamAppearance.getLEIndex(0, attrs)).equals(Value.FALSE);
    final var outputEnabled2 = !state.getPortValue(DualportRamAppearance.getLEIndex(1, attrs)).equals(Value.FALSE);
    if (outputEnabled && goodAddr && !misalignError) {
      //for (var i = 0; i < dataLines; i++) {
        long val = myState.getContents().get(addr);
        state.setPort(DualportRamAppearance.getDataOutIndex(0, attrs), Value.createKnown(width, val), DELAY);
      //}
    } else if (outputEnabled && (errorValue || (goodAddr && misalignError))) {
      //for (var i = 0; i < dataLines; i++)
        state.setPort(DualportRamAppearance.getDataOutIndex(0, attrs), Value.createError(width), DELAY);
    } else {
      //for (var i = 0; i < dataLines; i++)
        state.setPort(DualportRamAppearance.getDataOutIndex(0, attrs), Value.createUnknown(width), DELAY);
    }
    if (outputEnabled2 && goodAddr2 && !misalignError) {
      long val2 = myState.getContents().get(addr2);
      state.setPort(DualportRamAppearance.getDataOutIndex(1, attrs), Value.createKnown(width, val2), DELAY);
    } else if (outputEnabled2 && (errorValue2 || (goodAddr2 && misalignError))) {
      state.setPort(DualportRamAppearance.getDataOutIndex(1, attrs), Value.createError(width), DELAY);
    } else {
      state.setPort(DualportRamAppearance.getDataOutIndex(1, attrs), Value.createUnknown(width), DELAY);
    }
  }

  private void propagateByteEnables(InstanceState state, long addr, boolean goodAddr,
                                    long addr2, boolean goodAddr2,
                                    long addrWrite, boolean goodAddrWrite,
                                    boolean errorValue, boolean errorValue2,
                                    boolean errorValueWrite) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);
    long newMemValue = myState.getContents().get(myState.getCurrent());
    long oldMemValue1 = myState.getContents().get(addr);
    long oldMemValue2 = myState.getContents().get(addr2);
    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    final var weValue = state.getPortValue(DualportRamAppearance.getWEIndex(0, attrs));
    final var async = trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    final var edge =
        !async && myState
            .setClock(state.getPortValue(DualportRamAppearance.getClkIndex(0, attrs)), trigger);
    final var weAsync =
        (trigger.equals(StdAttr.TRIG_HIGH) && weValue.equals(Value.TRUE))
            || (trigger.equals(StdAttr.TRIG_LOW) && weValue.equals(Value.FALSE));
    final var weTriggered = (async && weAsync) || (edge && weValue.equals(Value.TRUE));
    if (goodAddrWrite && weTriggered) {
      long dataInValue = state.getPortValue(DualportRamAppearance.getDataInIndex(0, attrs)).toLongValue();
      if (DualportRamAppearance.getNrBEPorts(attrs) == 0) {
        newMemValue = dataInValue;
      } else {
        for (var i = 0; i < DualportRamAppearance.getNrBEPorts(attrs); i++) {
          long mask = 0xFFL << (i * 8);
          long andMask = ~mask;
          if (state.getPortValue(DualportRamAppearance.getBEIndex(i, attrs)).equals(Value.TRUE)) {
            newMemValue &= andMask;
            newMemValue |= (dataInValue & mask);
          }
        }
      }
      myState.getContents().set(addrWrite, newMemValue);
    }

    // perform reads
    final var dataBits = state.getAttributeValue(DATA_ATTR);
    final var outputNotEnabled = state.getPortValue(DualportRamAppearance.getOEIndex(0, attrs)).equals(Value.FALSE);
    final var outputNotEnabled2 = state.getPortValue(DualportRamAppearance.getOEIndex(1, attrs)).equals(Value.FALSE);

    Consumer<Value> setValue = (Value value) -> state.setPort(DualportRamAppearance.getDataOutIndex(0, attrs), value, DELAY);
    Consumer<Value> setValue2 = (Value value) -> state.setPort(DualportRamAppearance.getDataOutIndex(1, attrs), value, DELAY);

    /* if both OEs are not activated, just return */
    if (outputNotEnabled && outputNotEnabled2) return;

    /* if the address is bogus set error value accordingly */

    if (errorValue && !outputNotEnabled) {
      setValue.accept(Value.createError(dataBits));
      return;
    }

    if (errorValue2 && !outputNotEnabled2) {
      setValue2.accept(Value.createError(dataBits));
      return;
    }

    if (!goodAddr && !outputNotEnabled) {
      setValue.accept(Value.createUnknown(dataBits));
      return;
    }

    if (!goodAddr2 && !outputNotEnabled2) {
      setValue.accept(Value.createUnknown(dataBits));
      return;
    }

    final var asyncRead = async || attrs.getValue(Mem.ASYNC_READ);

    if (asyncRead) {
      if (!outputNotEnabled)
        setValue.accept(Value.createKnown(dataBits, myState.getContents().get(addr)));
      if (!outputNotEnabled2)
        setValue2.accept(Value.createKnown(dataBits, myState.getContents().get(addr2)));
      return;
    }

    if (edge) {
      if (attrs.getValue(Mem.READ_ATTR).equals(Mem.READAFTERWRITE)) {
        if (!outputNotEnabled) {
          setValue.accept(Value.createKnown(dataBits, myState.getContents().get(addr)));
        }
        if (!outputNotEnabled2) {
          setValue2.accept(Value.createKnown(dataBits, myState.getContents().get(addr2)));
        }
      } else {
        if (!outputNotEnabled) {
          setValue.accept(Value.createKnown(dataBits, oldMemValue1));
        }
        if (!outputNotEnabled2) {
          setValue2.accept(Value.createKnown(dataBits, oldMemValue2));
        }
      }
    }
  }

  @Override
  public void removeComponent(Circuit circ, Component c, CircuitState state) {
    if (state != null) closeHexFrame((RamState) state.getData(c));
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {DualportRamAppearance.getClkIndex(0, comp.getComponent().getAttributeSet())};
  }
}