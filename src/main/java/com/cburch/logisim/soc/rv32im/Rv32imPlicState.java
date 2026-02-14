/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.rv32im;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSlaveListener;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.util.StringUtil;
import java.util.ArrayList;

public class Rv32imPlicState implements SocBusSlaveInterface {

  /*
   * This is a minimal, single-context PLIC model intended for simulation.
   *
   * Supported registers (RISC-V PLIC specification layout):
   * - Interrupt Pending (word0/word1): 0x1000 / 0x1004
   * - Interrupt Enable  (word0/word1): 0x2000 / 0x2004
   * - Priority Threshold: 0x200000
   * - Claim/Complete:     0x200004
   */

  private static final int DEFAULT_BASE_ADDRESS = 0x0C000000;
  private static final int PLIC_MMIO_SIZE = 0x00400000;

  private static final int REG_PENDING_0 = 0x00001000;
  private static final int REG_PENDING_1 = 0x00001004;

  private static final int REG_ENABLE_0 = 0x00002000;
  private static final int REG_ENABLE_1 = 0x00002004;

  private static final int REG_THRESHOLD = 0x00200000;
  private static final int REG_CLAIM_COMPLETE = 0x00200004;

  private final ArrayList<SocBusSlaveListener> listeners = new ArrayList<>();

  private int startAddress = DEFAULT_BASE_ADDRESS;
  private String label = "";

  private SocBusInfo attachedBus;

  private int nrOfSources;
  private long pending;
  private long enable;
  private long claimed;
  private int threshold;

  public Rv32imPlicState() {
    nrOfSources = 0;
    pending = 0;
    enable = 0;
    claimed = 0;
    threshold = 0;
  }

  public void setAttachedBus(SocBusInfo info) {
    attachedBus = info;
  }

  public SocBusInfo getAttachedBus() {
    return attachedBus;
  }

  public Integer getPlicBaseAddress() {
    return startAddress;
  }

  public boolean setPlicBaseAddress(Integer addr) {
    if (addr == null) return false;
    final var aligned = (addr >> 2) << 2;
    if (startAddress == aligned) return false;
    startAddress = aligned;
    firememMapChanged();
    return true;
  }

  public String getLabel() {
    return label;
  }

  public boolean setLabel(String value) {
    if (label.equals(value)) return false;
    label = value;
    fireNameChanged();
    return true;
  }

  public void updateFromIrqPorts(InstanceState state) {
    if (state == null) return;

    final var cpuState = state.getAttributeValue(RV32imAttributes.RV32IM_STATE);
    if (cpuState == null) return;

    nrOfSources = Math.max(0, Math.min(32, cpuState.getNrOfIrqs()));

    long level = 0;
    for (int i = 0; i < nrOfSources; i++) {
      if (state.getPortValue(i + 2) == Value.TRUE) {
        final int src = i + 1;
        level |= 1L << src;
      }
    }

    final long claimedMask = claimed & getSourceMask();
    final long levelUnclaimed = level & ~claimedMask;

    /*
     * For level-triggered sources we keep pending asserted while the source is high,
     * except when it is already claimed.
     */
    pending &= (levelUnclaimed | claimedMask);
    pending |= levelUnclaimed;

    /* Always drop any pending bit that is currently claimed. */
    pending &= ~claimedMask;
  }

  public boolean isMachineExternalInterruptPending() {
    if (threshold != 0) return false;
    return ((pending & enable) & getSourceMask()) != 0;
  }

  private long getSourceMask() {
    long mask = 0;
    for (int src = 1; src <= nrOfSources; src++) {
      mask |= 1L << src;
    }
    return mask;
  }

  private int selectClaimId() {
    if (threshold != 0) return 0;

    final long candidates = (pending & enable) & getSourceMask();
    if (candidates == 0) return 0;

    for (int src = 1; src <= nrOfSources; src++) {
      if ((candidates & (1L << src)) != 0) return src;
    }
    return 0;
  }

  private int readWord(long value, boolean upper) {
    return (int) (upper ? ((value >>> 32) & 0xFFFF_FFFFL) : (value & 0xFFFF_FFFFL));
  }

  private long writeWord(long oldValue, int newWord, boolean upper) {
    final long word = ((long) newWord) & 0xFFFF_FFFFL;
    if (upper) {
      return (oldValue & 0xFFFF_FFFFL) | (word << 32);
    }
    return (oldValue & 0xFFFF_FFFF_0000_0000L) | word;
  }

  @Override
  public boolean canHandleTransaction(SocBusTransaction trans) {
    final long addr = SocSupport.convUnsignedInt(trans.getAddress());
    final long start = SocSupport.convUnsignedInt(startAddress);
    final long end = start + PLIC_MMIO_SIZE;
    return addr >= start && addr < end;
  }

  @Override
  public void handleTransaction(SocBusTransaction trans) {
    if (!canHandleTransaction(trans)) return;

    trans.setTransactionResponder(attachedBus == null ? null : attachedBus.getComponent());

    if (trans.getAccessType() != SocBusTransaction.WORD_ACCESS) {
      trans.setError(SocBusTransaction.ACCESS_TYPE_NOT_SUPPORTED_ERROR);
      return;
    }

    final long addr = SocSupport.convUnsignedInt(trans.getAddress());
    final long start = SocSupport.convUnsignedInt(startAddress);
    final int offset = (int) (addr - start);

    switch (offset) {
      case REG_PENDING_0 -> {
        if (trans.isWriteTransaction()) {
          trans.setError(SocBusTransaction.READ_ONLY_ACCESS_ERROR);
          return;
        }
        trans.setReadData(readWord(pending, false));
      }
      case REG_PENDING_1 -> {
        if (trans.isWriteTransaction()) {
          trans.setError(SocBusTransaction.READ_ONLY_ACCESS_ERROR);
          return;
        }
        trans.setReadData(readWord(pending, true));
      }
      case REG_ENABLE_0 -> {
        if (trans.isReadTransaction()) trans.setReadData(readWord(enable, false));
        if (trans.isWriteTransaction()) {
          enable = writeWord(enable, trans.getWriteData(), false);
          enable &= getSourceMask();
        }
      }
      case REG_ENABLE_1 -> {
        if (trans.isReadTransaction()) trans.setReadData(readWord(enable, true));
        if (trans.isWriteTransaction()) {
          enable = writeWord(enable, trans.getWriteData(), true);
          enable &= getSourceMask();
        }
      }
      case REG_THRESHOLD -> {
        if (trans.isReadTransaction()) trans.setReadData(threshold);
        if (trans.isWriteTransaction()) threshold = trans.getWriteData();
      }
      case REG_CLAIM_COMPLETE -> {
        if (trans.isReadTransaction()) {
          final int id = selectClaimId();
          if (id != 0) {
            final long bit = 1L << id;
            pending &= ~bit;
            claimed |= bit;
          }
          trans.setReadData(id);
        }
        if (trans.isWriteTransaction()) {
          final int id = trans.getWriteData();
          if (id < 1 || id > nrOfSources) {
            return;
          }
          claimed &= ~(1L << id);
        }
      }
      default -> trans.setError(SocBusTransaction.MISALIGNED_ADDRESS_ERROR);
    }
  }

  @Override
  public Integer getStartAddress() {
    return startAddress;
  }

  @Override
  public Integer getMemorySize() {
    return PLIC_MMIO_SIZE;
  }

  @Override
  public String getName() {
    var name = "BUG: Unknown";
    if (attachedBus != null && attachedBus.getComponent() != null) {
      name = label;
      if (StringUtil.isNullOrEmpty(name)) {
        final var loc = attachedBus.getComponent().getLocation();
        name =
            String.format(
                "%s@%d,%d",
                attachedBus.getComponent().getFactory().getDisplayName(), loc.getX(), loc.getY());
      }
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
    if (attachedBus == null || attachedBus.getComponent() == null) return null;
    return (InstanceComponent) attachedBus.getComponent();
  }

  private void fireNameChanged() {
    for (final var l : listeners) l.labelChanged();
  }

  private void firememMapChanged() {
    for (final var l : listeners) l.memoryMapChanged();
  }
}
