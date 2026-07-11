/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * Modified and converted to Dual Port RAM by: abdelrhman alaa
 * GitHub: https://github.com/abdelrhman1040
 * Date: February 2026
 *
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import java.util.function.Consumer;

public class DualRam extends Ram {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "DualRAM";

  public DualRam() {
    super(_ID, S.getter("dualRamComponent"), null, true);
    setIcon(new ArithmeticIcon("D-RAM", 3));
    setInstanceLogger(Logger.class);
  }

  @Override
  void configurePorts(Instance instance) {
    DualRamAppearance.configurePorts(instance);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var label = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
    return (label.length() == 0)
        ? "DUAL_RAM"
        : "DUALRAMCONTENTS_" + label;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return DualRamAppearance.getBounds(attrs);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    if (DualRamAppearance.classicAppearance(painter.getAttributeSet())) {
      DualRamAppearance.drawRamClassic(painter);
    } else {
      DualRamAppearance.drawRamEvolution(painter);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);

    // If Clear is active: wipe memory contents and reset data outputs for both
    // ports (A & B).
    if (attrs.getValue(RamAttributes.CLEAR_PIN)) {
      final var clearValue = state.getPortValue(DualRamAppearance.getClrIndex(0, attrs));
      if (clearValue.equals(Value.TRUE)) {
        myState.getContents().clear();
        final var dataBits = state.getAttributeValue(DATA_ATTR);

        final int totalOut = DualRamAppearance.getNrDataOutPorts(attrs);
        for (var i = 0; i < totalOut; i++) {
          int index = DualRamAppearance.getDataOutIndex(i, attrs);
          final var portVal = isSeparate(attrs)
              ? Value.createKnown(dataBits, 0)
              : Value.createUnknown(dataBits);
          state.setPort(index, portVal, DELAY);
        }
        return;
      }
    }

    for (int portIndex = 0; portIndex < 2; portIndex++) {
      final var addrValue = state.getPortValue(DualRamAppearance.getAddrIndex(portIndex, attrs));
      long addr = addrValue.toLongValue();
      final var goodAddr = addrValue.isFullyDefined() && addr >= 0;

      if (goodAddr && addr != myState.getCurrent(portIndex)) {
        myState.setCurrent(portIndex, addr);
        if (portIndex == 0)
          myState.scrollToShow(addr);
      }
      if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
        propagateLineEnables(state, portIndex, addr, goodAddr, addrValue.isErrorValue());
      } else {
        propagateByteEnables(state, portIndex, addr, goodAddr, addrValue.isErrorValue());
      }

    }
  }

  // Parameter added: int portIndex (to distinguish between Port A and Port B)
  private void propagateLineEnables(InstanceState state, int portIndex, long addr, boolean goodAddr,
      boolean errorValue) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);
    final var separate = isSeparate(attrs);

    final var totalLEs = DualRamAppearance.getNrLEPorts(attrs);
    final var dataLines = (totalLEs == 0) ? 1 : totalLEs / 2;

    final var misaligned = addr % dataLines != 0;
    final var misalignError = misaligned && !state.getAttributeValue(ALLOW_MISALIGNED);

    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);

    final var triggered = myState.setClock(portIndex,
        state.getPortValue(DualRamAppearance.getClkIndex(portIndex, attrs)), trigger);
    final var writeEnabled = triggered
        && (state.getPortValue(DualRamAppearance.getWEIndex(portIndex, attrs)) == Value.TRUE);

    if (writeEnabled && goodAddr && !misalignError) {
      for (var i = 0; i < dataLines; i++) {
        int absIndex = (portIndex * dataLines) + i;

        if (dataLines > 1) {
          final var le = state.getPortValue(DualRamAppearance.getLEIndex(absIndex, attrs));
          if (!Value.TRUE.equals(le))
            continue;
        }
        long dataValue = state.getPortValue(DualRamAppearance.getDataInIndex(absIndex, attrs)).toLongValue();
        myState.getContents().set(addr + i, dataValue);
      }
    }

    final var width = state.getAttributeValue(DATA_ATTR);
    final var outputEnabled = separate
        || !state.getPortValue(DualRamAppearance.getOEIndex(portIndex, attrs)).equals(Value.FALSE);

    if (outputEnabled && goodAddr && !misalignError) {
      for (var i = 0; i < dataLines; i++) {
        int absIndex = (portIndex * dataLines) + i;
        long val = myState.getContents().get(addr + i);
        state.setPort(DualRamAppearance.getDataOutIndex(absIndex, attrs), Value.createKnown(width, val), DELAY);
      }
    } else if (outputEnabled && (errorValue || (goodAddr && misalignError))) {
      for (var i = 0; i < dataLines; i++) {
        int absIndex = (portIndex * dataLines) + i;
        state.setPort(DualRamAppearance.getDataOutIndex(absIndex, attrs), Value.createError(width), DELAY);
      }
    } else {
      for (var i = 0; i < dataLines; i++) {
        int absIndex = (portIndex * dataLines) + i;
        state.setPort(DualRamAppearance.getDataOutIndex(absIndex, attrs), Value.createUnknown(width), DELAY);
      }
    }
  }

  private void propagateByteEnables(InstanceState state, int portIndex, long addr, boolean goodAddr,
      boolean errorValue) {
    final var attrs = state.getAttributeSet();
    final var myState = (RamState) getState(state);
    final var separate = isSeparate(attrs);
    long oldMemValue = myState.getContents().get(myState.getCurrent());
    long newMemValue = oldMemValue;
    // perform writes
    Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
    final var weValue = state.getPortValue(DualRamAppearance.getWEIndex(portIndex, attrs));
    final var async = trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);

    Value clkValue = Value.FALSE;
    if (!async) {
      int clkIdx = DualRamAppearance.getClkIndex(portIndex, attrs);
      if (clkIdx != -1) {
        clkValue = state.getPortValue(clkIdx);
      }
    }

    final var edge = !async && myState.setClock(portIndex, clkValue, trigger);

    final var weAsync = (trigger.equals(StdAttr.TRIG_HIGH) && weValue.equals(Value.TRUE))
        || (trigger.equals(StdAttr.TRIG_LOW) && weValue.equals(Value.FALSE));
    final var weTriggered = (async && weAsync) || (edge && weValue.equals(Value.TRUE));

    if (goodAddr && weTriggered) {
      long dataInValue = state.getPortValue(DualRamAppearance.getDataInIndex(portIndex, attrs)).toLongValue();
      if (DualRamAppearance.getNrBEPorts(attrs) == 0) {
        newMemValue = dataInValue;
      } else {
        int bytesPerWord = DualRamAppearance.getNrBEPorts(attrs) / 2;

        for (var i = 0; i < bytesPerWord; i++) {
          long mask = 0xFFL << (i * 8);
          long andMask = ~mask;
          int bePinIndex = (portIndex * bytesPerWord) + i;

          if (state.getPortValue(DualRamAppearance.getBEIndex(bePinIndex, attrs)).equals(Value.TRUE)) {
            newMemValue &= andMask;
            newMemValue |= (dataInValue & mask);
          }
        }
      }
      myState.getContents().set(addr, newMemValue);
    }

    // perform reads
    final var dataBits = state.getAttributeValue(DATA_ATTR);
    final var outputNotEnabled = state.getPortValue(DualRamAppearance.getOEIndex(portIndex, attrs)).equals(Value.FALSE);

    Consumer<Value> setValue = (Value value) -> state.setPort(DualRamAppearance.getDataOutIndex(portIndex, attrs),
        value, DELAY);

    if (!separate && outputNotEnabled) {
      /* put the bus in tri-state in case of a combined bus and no output enable */
      setValue.accept(Value.createUnknown(dataBits));
      return;
    }
    /* if the OE is not activated return */
    if (outputNotEnabled)
      return;
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
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {
        DualRamAppearance.getClkIndex(0, comp.getComponent().getAttributeSet()),
        DualRamAppearance.getClkIndex(1, comp.getComponent().getAttributeSet())
    };
  }
}
