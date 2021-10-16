/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.jtaguart;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSlaveListener;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.pio.PioState.PioRegState;
import com.cburch.logisim.util.StringUtil;
import java.util.ArrayList;
import java.util.LinkedList;

public class JtagUartState  implements SocBusSlaveInterface {

  public class JtagUartFifoState implements InstanceData, Cloneable {

    private final LinkedList<Integer> WriteFifo = new LinkedList<>();
    private final LinkedList<Integer> ReadFifo = new LinkedList<>();
    private boolean readIrqEnable = false;
    private boolean writeIrqEnable = false;
    private boolean acBit = false;
    private Value lastReset = Value.UNKNOWN;
    private Value lastClock = Value.UNKNOWN;
    private boolean doReset = false;
    private boolean endReset = false;

    public JtagUartFifoState() {
      reset();
    }

    public void reset() {
      WriteFifo.clear();
      ReadFifo.clear();
      readIrqEnable = false;
      writeIrqEnable = false;
      acBit = false;
    }

    public void setReset(Value reset) {
      doReset = (lastReset == Value.FALSE && reset == Value.TRUE);
      endReset = (lastReset == Value.UNKNOWN) || (lastReset == Value.TRUE && reset == Value.FALSE);
      lastReset = reset;
    }

    public boolean risingEdge(Value clock) {
      Value last = lastClock;
      lastClock = clock;
      return (last == Value.FALSE && clock == Value.TRUE);
    }

    @Override
    public PioRegState clone() {
      try {
        return (PioRegState) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public void writeDataRegister(int value) {
      int wdata = value & 0xFF;
      if (WriteFifo.size() >= getSize(writeFifoSize)) return;
      WriteFifo.add(wdata);
    }

    public Integer readDataRegister() {
      int result = 0;
      if (ReadFifo.isEmpty()) return 0;
      result = ReadFifo.getFirst() & 0xFF;
      ReadFifo.removeFirst();
      result |= (1 << 15);
      result |= (ReadFifo.size() & 0xFFFF) << 16;
      return result;
    }

    public void writeControlRegister(int value) {
      readIrqEnable = (value & 1) != 0;
      writeIrqEnable = (value & 2) != 0;
      if ((value & (1 << 10)) != 0) acBit = false;
    }

    public Integer readControlRegister() {
      int result = 0;
      if (readIrqEnable) result |= 1;
      if (writeIrqEnable) result |= 2;
      if (readIrqPending()) result |= 1 << 8;
      if (writeIrqPending()) result |= 1 << 9;
      if (acBit) result |= 1 << 10;
      int avail = getSize(writeFifoSize) - WriteFifo.size();
      result |= (avail & 0xFFFF) << 16;
      return result;
    }

    private boolean readIrqPending() {
      if (!readIrqEnable) return false;
      int readFifoEmptySpaces = getSize(readFifoSize) - ReadFifo.size();
      return readFifoEmptySpaces <= readIrqThreshold;
    }

    private boolean writeIrqPending() {
      if (!writeIrqEnable) return false;
      return WriteFifo.size() <= writeIrqThreshold;
    }

    public boolean isIrqPending() {
      return readIrqPending() || writeIrqPending();
    }

    public boolean isWriteFifoEmpty() {
      return WriteFifo.isEmpty();
    }

    public void setAcBit() {
      acBit = true;
    }

    public int popWriteFifo() {
      if (WriteFifo.isEmpty()) return -1;
      int val = WriteFifo.getFirst();
      WriteFifo.removeFirst();
      return val;
    }

    public void pushReadFifo(Integer val) {
      if (ReadFifo.size() >= getSize(readFifoSize)) return;
      ReadFifo.add(val);
    }

    private int getSize(AttributeOption opt) {
      if (opt.equals(JtagUartAttributes.OPT_8)) return 8;
      if (opt.equals(JtagUartAttributes.OPT_16)) return 16;
      if (opt.equals(JtagUartAttributes.OPT_32)) return 32;
      if (opt.equals(JtagUartAttributes.OPT_64)) return 64;
      if (opt.equals(JtagUartAttributes.OPT_128)) return 128;
      if (opt.equals(JtagUartAttributes.OPT_256)) return 256;
      if (opt.equals(JtagUartAttributes.OPT_512)) return 512;
      if (opt.equals(JtagUartAttributes.OPT_1024)) return 1024;
      if (opt.equals(JtagUartAttributes.OPT_2048)) return 2048;
      if (opt.equals(JtagUartAttributes.OPT_4096)) return 4096;
      if (opt.equals(JtagUartAttributes.OPT_8192)) return 8192;
      if (opt.equals(JtagUartAttributes.OPT_16384)) return 16384;
      if (opt.equals(JtagUartAttributes.OPT_32768)) return 32768;
      return -1;
    }
  }

  private String label = "";
  private final SocBusInfo attachedBus = new SocBusInfo("");
  private Integer startAddress = 0;
  private AttributeOption writeFifoSize = JtagUartAttributes.OPT_64;
  private Integer writeIrqThreshold = 8;
  private AttributeOption readFifoSize = JtagUartAttributes.OPT_64;
  private Integer readIrqThreshold = 8;
  private final ArrayList<SocBusSlaveListener> listeners = new ArrayList<>();

  public String getLabel() {
    return label;
  }

  public SocBusInfo getAttachedBus() {
    return attachedBus;
  }

  @Override
  public Integer getStartAddress() {
    return startAddress;
  }

  public AttributeOption getWriteFifoSize() {
    return writeFifoSize;
  }

  public Integer getWriteIrqThreshold() {
    return writeIrqThreshold;
  }

  public AttributeOption getReadFifoSize() {
    return readFifoSize;
  }

  public Integer getReadIrqThreshold() {
    return readIrqThreshold;
  }

  public boolean setLabel(String l) {
    if (l.equals(label)) return false;
    label = l;
    fireNameChanged();
    return true;
  }

  public boolean setAttachedBus(SocBusInfo atb) {
    if (attachedBus.getBusId().equals(atb.getBusId())) return false;
    attachedBus.setBusId(atb.getBusId());
    return true;
  }

  public boolean setStartAddress(int addr) {
    if (addr == startAddress) return false;
    startAddress = addr;
    firememMapChanged();
    return true;
  }

  public boolean setWriteFifoSize(AttributeOption wfs) {
    if (writeFifoSize.equals(wfs)) return false;
    writeFifoSize = wfs;
    return true;
  }

  public boolean setWriteIrqThreshold(Integer val) {
    if (writeIrqThreshold == val) return false;
    writeIrqThreshold = val;
    return true;
  }

  public boolean setReadFifoSize(AttributeOption rfs) {
    if (readFifoSize.equals(rfs)) return false;
    readFifoSize = rfs;
    return true;
  }

  public boolean setReadIrqThreshold(Integer val) {
    if (readIrqThreshold == val) return false;
    readIrqThreshold = val;
    return true;
  }

  public void copyInto(JtagUartState d) {
    d.label = label;
    d.attachedBus.setBusId(attachedBus.getBusId());
    d.startAddress = startAddress;
    d.writeFifoSize = writeFifoSize;
    d.readFifoSize = readFifoSize;
    d.writeIrqThreshold = writeIrqThreshold;
    d.readIrqThreshold = readIrqThreshold;
  }

  public void handleOperations(InstanceState state) {
    Value curReset = state.getPortValue(JtagUart.RESET_PIN);
    Value curClock = state.getPortValue(JtagUart.CLOCK_PIN);
    JtagUartFifoState instState = (JtagUartFifoState) state.getData();
    if (instState == null) {
      instState = new JtagUartFifoState();
      state.setData(instState);
    }
    instState.setReset(curReset);
    if (instState.doReset) {
      state.setPort(JtagUart.READ_ENABLE_PIN, Value.FALSE, 5);
      state.setPort(JtagUart.CLEAR_KEYBOARD_PIN, Value.TRUE, 5);
      state.setPort(JtagUart.DATA_OUT_PIN, Value.createKnown(7, 0), 5);
      state.setPort(JtagUart.WRITE_PIN, Value.FALSE, 5);
      state.setPort(JtagUart.CLEAR_TTY_PIN, Value.TRUE, 5);
      state.setPort(JtagUart.IRQ_PIN, Value.FALSE, 5);
      instState.reset();
    }
    if (instState.endReset) {
      state.setPort(JtagUart.READ_ENABLE_PIN, Value.FALSE, 5);
      state.setPort(JtagUart.CLEAR_KEYBOARD_PIN, Value.FALSE, 5);
      state.setPort(JtagUart.DATA_OUT_PIN, Value.createKnown(7, 0), 5);
      state.setPort(JtagUart.WRITE_PIN, Value.FALSE, 5);
      state.setPort(JtagUart.CLEAR_TTY_PIN, Value.FALSE, 5);
      state.setPort(JtagUart.IRQ_PIN, Value.FALSE, 5);
    }
    if (curReset == Value.TRUE) return;
    if (instState.risingEdge(curClock)) {
      state.setPort(JtagUart.IRQ_PIN, instState.isIrqPending() ? Value.TRUE : Value.FALSE, 5);
      if (instState.isWriteFifoEmpty()) {
        state.setPort(JtagUart.WRITE_PIN, Value.FALSE, 5);
      } else {
        int val = instState.popWriteFifo();
        instState.setAcBit();
        state.setPort(JtagUart.WRITE_PIN, Value.TRUE, 5);
        state.setPort(JtagUart.DATA_OUT_PIN, Value.createKnown(7, val), 5);
      }
      if (state.getPortValue(JtagUart.AVAILABLE_PIN) == Value.TRUE
          && state.getPortValue(JtagUart.READ_ENABLE_PIN) == Value.FALSE) {
        instState.setAcBit();
        instState.pushReadFifo((int) state.getPortValue(JtagUart.DATA_IN_PIN).toLongValue());
        state.setPort(JtagUart.READ_ENABLE_PIN, Value.TRUE, 5);
      } else {
        state.setPort(JtagUart.READ_ENABLE_PIN, Value.FALSE, 5);
      }
    }
  }

  @Override
  public boolean canHandleTransaction(SocBusTransaction trans) {
    long addr = SocSupport.convUnsignedInt(trans.getAddress());
    long start = SocSupport.convUnsignedInt(startAddress);
    long end = start + 8L;
    return (addr >= start && addr < end);
  }

  @Override
  public void handleTransaction(SocBusTransaction trans) {
    if (!canHandleTransaction(trans)) return;
    trans.setTransactionResponder(attachedBus.getComponent());
    long addr = SocSupport.convUnsignedInt(trans.getAddress());
    long start = SocSupport.convUnsignedInt(startAddress);
    if (trans.getAccessType() != SocBusTransaction.WORD_ACCESS) {
      trans.setError(SocBusTransaction.ACCESS_TYPE_NOT_SUPPORTED_ERROR);
      return;
    }
    JtagUartFifoState state =
        (JtagUartFifoState)
            attachedBus.getSocSimulationManager().getdata(attachedBus.getComponent());
    long index = (addr - start);
    if (index == 0) {
      if (trans.isReadTransaction()) {
        trans.setReadData(state.readDataRegister());
      }
      if (trans.isWriteTransaction()) {
        state.writeDataRegister(trans.getWriteData());
      }
      return;
    }
    if (index == 4) {
      if (trans.isReadTransaction()) {
        trans.setReadData(state.readControlRegister());
      }
      if (trans.isWriteTransaction()) {
        state.writeControlRegister(trans.getWriteData());
      }
      return;
    }
    trans.setError(SocBusTransaction.MISALIGNED_ADDRESS_ERROR);
  }

  @Override
  public Integer getMemorySize() {
    return 8;
  }

  @Override
  public String getName() {
    var name = "BUG: Unknown";
    if (attachedBus != null && attachedBus.getComponent() != null) {
      name = label;
      if (StringUtil.isNullOrEmpty(name)) {
        final var loc = attachedBus.getComponent().getLocation();
        name = String.format("%s@%d,%d", attachedBus.getComponent().getFactory().getDisplayName(), loc.getX(), loc.getY());
      }
    }
    return name;
  }

  @Override
  public void registerListener(SocBusSlaveListener l) {
    if (!listeners.contains(l))
      listeners.add(l);
  }

  @Override
  public void removeListener(SocBusSlaveListener l) {
    listeners.remove(l);
  }

  @Override
  public InstanceComponent getComponent() {
    if (attachedBus == null || attachedBus.getComponent() == null)
      return null;
    return (InstanceComponent) attachedBus.getComponent();
  }

  private void fireNameChanged() {
    for (SocBusSlaveListener l : listeners)
      l.labelChanged();
  }

  private void firememMapChanged() {
    for (SocBusSlaveListener l : listeners)
      l.memoryMapChanged();
  }

}
