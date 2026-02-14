/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.dma;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusMasterInterface;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSlaveListener;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.util.StringUtil;
import java.util.ArrayList;

/**
 * Core state and logic for the SoC DMA engine.
 *
 * <p>MMIO register map (active register set is loaded from/stored to DmaRegState):
 * <pre>
 *   Offset  Register    Access  Description
 *   0x00    SRC_ADDR    R/W     Source address (must be word-aligned)
 *   0x04    DST_ADDR    R/W     Destination address (must be word-aligned)
 *   0x08    LENGTH      R/W     Transfer length in bytes (must be multiple of 4)
 *   0x0C    CONTROL     R/W     bit0: START (write-1-to-start), bit1: IRQ_EN
 *   0x10    STATUS      R/W1C   bit0: BUSY (RO), bit1: DONE (W1C)
 *   0x14    BYTES_DONE  RO      Number of bytes already transferred
 * </pre>
 */
public class DmaState implements SocBusSlaveInterface, SocBusMasterInterface {

  /* ---------- MMIO register offsets ---------- */
  public static final int SRC_ADDR_REG = 0x00;
  public static final int DST_ADDR_REG = 0x04;
  public static final int LENGTH_REG = 0x08;
  public static final int CONTROL_REG = 0x0C;
  public static final int STATUS_REG = 0x10;
  public static final int BYTES_DONE_REG = 0x14;
  public static final int REG_REGION_SIZE = 0x18; // 6 registers * 4 bytes

  /* ---------- CONTROL register bits ---------- */
  public static final int CTRL_START = 1 << 0;
  public static final int CTRL_IRQ_EN = 1 << 1;

  /* ---------- STATUS register bits ---------- */
  public static final int STAT_BUSY = 1 << 0;
  public static final int STAT_DONE = 1 << 1;

  /* ---------- Runtime state (per-simulation instance) ---------- */

  /**
   * Holds the mutable register state for one simulation instance of the DMA engine.
   * A new instance is created for each simulation run.
   */
  public static class DmaRegState implements InstanceData, Cloneable {
    int srcAddr;
    int dstAddr;
    int length;
    int control;
    boolean busy;
    boolean done;
    int bytesDone;
    boolean irqAsserted;
    Value lastClock = Value.UNKNOWN;

    public DmaRegState() {
      reset();
    }

    public void reset() {
      srcAddr = 0;
      dstAddr = 0;
      length = 0;
      control = 0;
      busy = false;
      done = false;
      bytesDone = 0;
      irqAsserted = false;
    }

    @Override
    public DmaRegState clone() {
      try {
        return (DmaRegState) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  /* ---------- Configuration state (shared across simulations) ---------- */
  private Integer startAddress = 0;
  private int burstSize = 16; // words per clock tick (must be power of 2)
  private final SocBusInfo controlBus = new SocBusInfo("");
  private final SocBusInfo sourceBus = new SocBusInfo("");
  private final SocBusInfo destBus = new SocBusInfo("");
  private String label = "";
  private final ArrayList<SocBusSlaveListener> listeners = new ArrayList<>();

  /* ========== Configuration getters / setters ========== */

  public int getBurstSize() {
    return burstSize;
  }

  public boolean setBurstSize(int size) {
    if (size == burstSize) return false;
    burstSize = size;
    return true;
  }

  public SocBusInfo getControlBus() {
    return controlBus;
  }

  public SocBusInfo getSourceBus() {
    return sourceBus;
  }

  public SocBusInfo getDestBus() {
    return destBus;
  }

  public String getLabel() {
    return label;
  }

  public boolean setLabel(String l) {
    if (label.equals(l)) return false;
    label = l;
    fireNameChanged();
    return true;
  }

  public boolean setStartAddress(Integer value) {
    int addr = (value >> 2) << 2; // force word-alignment
    if (addr == startAddress) return false;
    startAddress = addr;
    fireMemMapChanged();
    return true;
  }

  public boolean setControlBus(SocBusInfo info) {
    if (controlBus.getBusId().equals(info.getBusId())) return false;
    controlBus.setBusId(info.getBusId());
    return true;
  }

  public boolean setSourceBus(SocBusInfo info) {
    if (sourceBus.getBusId().equals(info.getBusId())) return false;
    sourceBus.setBusId(info.getBusId());
    return true;
  }

  public boolean setDestBus(SocBusInfo info) {
    if (destBus.getBusId().equals(info.getBusId())) return false;
    destBus.setBusId(info.getBusId());
    return true;
  }

  public void copyInto(DmaState dest) {
    dest.setStartAddress(getStartAddress());
    dest.setBurstSize(burstSize);
    dest.setLabel(label);
    dest.setControlBus(controlBus);
    dest.setSourceBus(sourceBus);
    dest.setDestBus(destBus);
  }

  public DmaRegState getNewState() {
    return new DmaRegState();
  }

  /* ========== Clock-driven burst transfer ========== */

  /**
   * Called from {@code SocDma.propagate()} on each clock rising edge.
   * Transfers up to {@code burstSize} words from source to destination.
   */
  public void executeBurst(DmaRegState regs, CircuitState cState) {
    if (!regs.busy) return;

    SocSimulationManager mgr = controlBus.getSocSimulationManager();
    if (mgr == null) return;

    String srcBusId = getEffectiveSourceBusId();
    String dstBusId = getEffectiveDestBusId();
    if (srcBusId == null || dstBusId == null) return;

    int remaining = regs.length - regs.bytesDone;
    int wordsToTransfer = Math.min(burstSize, remaining / 4);
    int wordsTransferred = 0;

    for (int i = 0; i < wordsToTransfer; i++) {
      int offset = regs.bytesDone + i * 4;

      // Read one word from source (hidden to avoid flooding the bus trace;
      // read-side sniffing is not useful for observers like VGA)
      SocBusTransaction readTrans = new SocBusTransaction(
          SocBusTransaction.READ_TRANSACTION,
          regs.srcAddr + offset,
          0,
          SocBusTransaction.WORD_ACCESS,
          controlBus.getComponent());
      readTrans.setAsHiddenTransaction();
      mgr.initializeTransaction(readTrans, srcBusId, cState);
      if (readTrans.hasError()) break;

      // Write one word to destination (not hidden so bus sniffers like VGA
      // can observe the writes and update their framebuffer in real time)
      SocBusTransaction writeTrans = new SocBusTransaction(
          SocBusTransaction.WRITE_TRANSACTION,
          regs.dstAddr + offset,
          readTrans.getReadData(),
          SocBusTransaction.WORD_ACCESS,
          controlBus.getComponent());
      mgr.initializeTransaction(writeTrans, dstBusId, cState);
      if (writeTrans.hasError()) break;

      wordsTransferred++;
    }

    regs.bytesDone += wordsTransferred * 4;

    if (regs.bytesDone >= regs.length) {
      regs.busy = false;
      regs.done = true;
      if ((regs.control & CTRL_IRQ_EN) != 0) {
        regs.irqAsserted = true;
      }
    }
  }

  /**
   * Returns the effective source bus ID.  Falls back to the control bus if
   * the dedicated source bus is not configured.
   */
  private String getEffectiveSourceBusId() {
    String id = sourceBus.getBusId();
    if (StringUtil.isNullOrEmpty(id)) id = controlBus.getBusId();
    return StringUtil.isNullOrEmpty(id) ? null : id;
  }

  /**
   * Returns the effective destination bus ID.  Falls back to the control bus
   * if the dedicated destination bus is not configured.
   */
  private String getEffectiveDestBusId() {
    String id = destBus.getBusId();
    if (StringUtil.isNullOrEmpty(id)) id = controlBus.getBusId();
    return StringUtil.isNullOrEmpty(id) ? null : id;
  }

  /* ========== SocBusMasterInterface ========== */

  @Override
  public void initializeTransaction(SocBusTransaction trans, String busId, CircuitState cState) {
    if (controlBus.getSocSimulationManager() == null) return;
    controlBus.getSocSimulationManager().initializeTransaction(trans, busId, cState);
  }

  /* ========== SocBusSlaveInterface (MMIO registers) ========== */

  @Override
  public boolean canHandleTransaction(SocBusTransaction trans) {
    long addr = SocSupport.convUnsignedInt(trans.getAddress());
    long start = SocSupport.convUnsignedInt(startAddress);
    long end = start + REG_REGION_SIZE;
    return addr >= start && addr < end;
  }

  @Override
  public void handleTransaction(SocBusTransaction trans) {
    if (!canHandleTransaction(trans)) return;
    trans.setTransactionResponder(controlBus.getComponent());

    if (trans.getAccessType() != SocBusTransaction.WORD_ACCESS) {
      trans.setError(SocBusTransaction.ACCESS_TYPE_NOT_SUPPORTED_ERROR);
      return;
    }

    long addr = SocSupport.convUnsignedInt(trans.getAddress());
    long start = SocSupport.convUnsignedInt(startAddress);
    int regOffset = (int) (addr - start);

    DmaRegState regs = getRegPropagateState();
    if (regs == null) return;

    switch (regOffset) {
      case SRC_ADDR_REG -> handleSrcAddrReg(trans, regs);
      case DST_ADDR_REG -> handleDstAddrReg(trans, regs);
      case LENGTH_REG -> handleLengthReg(trans, regs);
      case CONTROL_REG -> handleControlReg(trans, regs);
      case STATUS_REG -> handleStatusReg(trans, regs);
      case BYTES_DONE_REG -> handleBytesDoneReg(trans, regs);
      default -> trans.setError(SocBusTransaction.MISALIGNED_ADDRESS_ERROR);
    }
  }

  private void handleSrcAddrReg(SocBusTransaction trans, DmaRegState regs) {
    if (trans.isReadTransaction()) trans.setReadData(regs.srcAddr);
    if (trans.isWriteTransaction()) {
      if (!regs.busy) regs.srcAddr = (trans.getWriteData() >> 2) << 2;
    }
  }

  private void handleDstAddrReg(SocBusTransaction trans, DmaRegState regs) {
    if (trans.isReadTransaction()) trans.setReadData(regs.dstAddr);
    if (trans.isWriteTransaction()) {
      if (!regs.busy) regs.dstAddr = (trans.getWriteData() >> 2) << 2;
    }
  }

  private void handleLengthReg(SocBusTransaction trans, DmaRegState regs) {
    if (trans.isReadTransaction()) trans.setReadData(regs.length);
    if (trans.isWriteTransaction()) {
      if (!regs.busy) regs.length = (trans.getWriteData() >> 2) << 2;
    }
  }

  private void handleControlReg(SocBusTransaction trans, DmaRegState regs) {
    if (trans.isReadTransaction()) trans.setReadData(regs.control);
    if (trans.isWriteTransaction()) {
      int val = trans.getWriteData();
      regs.control = val & CTRL_IRQ_EN; // preserve IRQ_EN

      // START bit: write-1-to-start, triggers a new transfer if not busy
      if ((val & CTRL_START) != 0 && !regs.busy && regs.length > 0) {
        regs.busy = true;
        regs.done = false;
        regs.bytesDone = 0;
        regs.irqAsserted = false;
      }
    }
  }

  private void handleStatusReg(SocBusTransaction trans, DmaRegState regs) {
    if (trans.isReadTransaction()) {
      int status = 0;
      if (regs.busy) status |= STAT_BUSY;
      if (regs.done) status |= STAT_DONE;
      trans.setReadData(status);
    }
    if (trans.isWriteTransaction()) {
      // Write-1-to-clear for DONE bit
      if ((trans.getWriteData() & STAT_DONE) != 0) {
        regs.done = false;
        regs.irqAsserted = false;
      }
    }
  }

  private void handleBytesDoneReg(SocBusTransaction trans, DmaRegState regs) {
    if (trans.isReadTransaction()) {
      trans.setReadData(regs.bytesDone);
    }
    if (trans.isWriteTransaction()) {
      trans.setError(SocBusTransaction.READ_ONLY_ACCESS_ERROR);
    }
  }

  private DmaRegState getRegPropagateState() {
    SocSimulationManager mgr = controlBus.getSocSimulationManager();
    if (mgr == null) return null;
    return (DmaRegState) mgr.getdata(controlBus.getComponent());
  }

  /* ========== SocBusSlaveInterface metadata ========== */

  @Override
  public Integer getStartAddress() {
    return startAddress;
  }

  @Override
  public Integer getMemorySize() {
    return REG_REGION_SIZE;
  }

  @Override
  public String getName() {
    if (controlBus.getComponent() == null) return "BUG: Unknown";
    var name = label;
    if (StringUtil.isNullOrEmpty(name)) {
      final var loc = controlBus.getComponent().getLocation();
      name = String.format("%s@%d,%d",
          controlBus.getComponent().getFactory().getDisplayName(),
          loc.getX(), loc.getY());
    }
    return name;
  }

  @Override
  public void registerListener(SocBusSlaveListener l) {
    if (!listeners.contains(l)) listeners.add(l);
  }

  @Override
  public void removeListener(SocBusSlaveListener l) {
    listeners.remove(l);
  }

  @Override
  public InstanceComponent getComponent() {
    if (controlBus.getComponent() == null) return null;
    return (InstanceComponent) controlBus.getComponent();
  }

  private void fireNameChanged() {
    for (SocBusSlaveListener l : listeners) l.labelChanged();
  }

  private void fireMemMapChanged() {
    for (SocBusSlaveListener l : listeners) l.memoryMapChanged();
  }
}
