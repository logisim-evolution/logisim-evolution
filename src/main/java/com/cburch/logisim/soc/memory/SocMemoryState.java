/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSlaveListener;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.util.StringUtil;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class SocMemoryState implements SocBusSlaveInterface {

  public class SocMemoryInfo implements InstanceData, Cloneable {
    private class SocMemoryInfoBlock {
      private final LinkedList<Integer> contents = new LinkedList<>();
      private int startAddress;
      private final Random rand = new Random();

      public SocMemoryInfoBlock(int address, int data) {
        startAddress = (address >> 2) << 2;
        contents.add(data);
      }

      public boolean canAddBefore(int address) {
        final var previousAddress = getStartAddress() - 4;
        return (address >= previousAddress) && (address < getStartAddress());
      }

      public boolean canAddAfter(int address) {
        return (address >= getEndAddress()) && (address < getEndAddress() + 4);
      }

      public boolean contains(int address) {
        return (address >= getStartAddress()) && (address < getEndAddress());
      }

      public boolean canAdd(int address) {
        return canAddBefore(address) || canAddAfter(address);
      }

      public boolean addInfo(int address, int data) {
        if (canAddBefore(address)) {
          contents.addFirst(data);
          startAddress -= 4;
          return true;
        }
        if (canAddAfter(address)) {
          contents.add(data);
          return true;
        }
        if (contains(address)) {
          final var index = (address - startAddress) >> 2;
          contents.set(index, data);
          return true;
        }
        return false;
      }

      public int getValue(int address) {
        final var index = (address - startAddress) >> 2;
        if (index >= contents.size()) {
          return rand.nextInt();
        }
        return contents.get(index);
      }

      public int getStartAddress() {
        return startAddress;
      }

      public int getEndAddress() {
        return startAddress + contents.size() * 4;
      }
    }

    private final ArrayList<SocMemoryInfoBlock> memInfo;

    public SocMemoryInfo() {
      memInfo = new ArrayList<>();
    }

    @Override
    public SocMemoryInfo clone() {
      try {
        return (SocMemoryInfo) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public int getWord(int address) {
      for (final var info : memInfo)
        if (info.contains(address)) return info.getValue(address);
      return rand.nextInt();
    }

    public void writeWord(int address, int wdata) {
      final var adders = new ArrayList<SocMemoryInfoBlock>();
      for (final var info : memInfo) {
        if (info.contains(address)) {
          info.addInfo(address, wdata);
          return;
        }
        if (info.canAdd(address)) adders.add(info);
      }
      if (adders.isEmpty()) {
        /* we have to create a new set */
        memInfo.add(new SocMemoryInfoBlock(address, wdata));
        return;
      }
      if (adders.size() == 1) {
        /* easy case we can add in front or at the end of an existing one */
        adders.get(0).addInfo(address, wdata);
        return;
      }
      if (adders.size() > 2) {
        // FIXME: hardcoded string
        System.out.println("BUG! Memory management does not function corectly for the SocMemory component!");
        return;
      }
      final var addBefore = adders.get(0).canAddBefore(address)
                            ? adders.get(0)
                            : adders.get(1).canAddBefore(address) ? adders.get(1) : null;
      final var addAfter = adders.get(0).canAddAfter(address)
                           ? adders.get(0)
                           : adders.get(1).canAddAfter(address) ? adders.get(1) : null;
      if (addBefore == null || addAfter == null) {
        System.out.println("BUG! Memory management does not function corectly for the SocMemory component!");
        return;
      }
      addAfter.addInfo(address, wdata);
      for (var i = addBefore.getStartAddress(); i < addBefore.getEndAddress(); i += 4) {
        addAfter.addInfo(i, addBefore.getValue(i));
      }
      memInfo.remove(addBefore);
    }
  }

  private int startAddress;
  private int sizeInBytes;
  private final Random rand = new Random();
  private final SocBusInfo attachedBus;
  private String label;
  private final ArrayList<SocBusSlaveListener> listeners;

  public SocMemoryState() {
    startAddress = 0;
    sizeInBytes = 1024;
    attachedBus = new SocBusInfo("");
    label = "";
    listeners = new ArrayList<>();
  }

  @Override
  public Integer getStartAddress() {
    return startAddress;
  }

  @Override
  public Integer getMemorySize() {
    return sizeInBytes;
  }

  public boolean setStartAddress(int address) {
    final var addr = (address >> 2) << 2;
    if (addr == startAddress) return false;
    startAddress = addr;
    firememMapChanged();
    return true;
  }

  public boolean setSize(BitWidth i) {
    final var size = (int) Math.pow(2, i.getWidth());
    if (sizeInBytes == size) return false;
    sizeInBytes = size;
    firememMapChanged();
    return true;
  }

  public SocBusInfo getSocBusInfo() {
    return attachedBus;
  }

  public boolean setSocBusInfo(SocBusInfo i) {
    if (attachedBus.getBusId().equals(i.getBusId())) return false;
    attachedBus.setBusId(i.getBusId());
    return true;
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

  @Override
  public String getName() {
    if (attachedBus == null || attachedBus.getComponent() == null) return "BUG: Unknown";
    var name = label;
    if (StringUtil.isNullOrEmpty(name)) {
      final var loc = attachedBus.getComponent().getLocation();
      name = String.format("%s@%d,%d", attachedBus.getComponent().getFactory().getDisplayName(), loc.getX(), loc.getY());
    }
    return name;
  }

  @Override
  public InstanceComponent getComponent() {
    if (attachedBus == null || attachedBus.getComponent() == null) return null;
    return (InstanceComponent) attachedBus.getComponent();
  }

  @Override
  public void registerListener(SocBusSlaveListener l) {
    if (!listeners.contains(l)) listeners.add(l);
  }

  @Override
  public void removeListener(SocBusSlaveListener l) {
    listeners.remove(l);
  }

  public SocMemoryInfo getNewState() {
    return new SocMemoryInfo();
  }

  @Override
  public boolean canHandleTransaction(SocBusTransaction trans) {
    final var addr = SocSupport.convUnsignedInt(trans.getAddress());
    final var start = SocSupport.convUnsignedInt(startAddress);
    final var end = start + sizeInBytes;
    return (addr >= start) && (addr < end);
  }

  @Override
  public void handleTransaction(SocBusTransaction trans) {
    if (!canHandleTransaction(trans)) /* this should never happen */ return;
    if (trans.isReadTransaction()) {
      trans.setReadData(performReadAction(trans.getAddress(), trans.getAccessType()));
    }
    if (trans.isWriteTransaction()) {
      performWriteAction(trans.getAddress(), trans.getWriteData(), trans.getAccessType());
    }
    trans.setTransactionResponder(attachedBus.getComponent());
  }

  private SocMemoryInfo getRegPropagateState() {
    return (SocMemoryInfo) attachedBus.getSocSimulationManager().getdata(attachedBus.getComponent());
  }

  private int performReadAction(int address, int type) {
    final SocMemoryInfo data = getRegPropagateState();
    final var value = (data == null) ? rand.nextInt() : data.getWord((address >> 2) << 2);
    final var adbit1 = (address >> 1) & 1;

    switch (type) {
      case SocBusTransaction.WORD_ACCESS:
        return value;
      case SocBusTransaction.HALF_WORD_ACCESS:
        if (adbit1 == 1) {
          return (value >> 16) & 0xFFFF;
        } else {
          return value & 0xFFFF;
        }
      default:
        final var adbit1_0 = address & 3;
        return switch (adbit1_0) {
          case 0 -> value & 0xFF;
          case 1 -> (value >> 8) & 0xFF;
          case 2 -> (value >> 16) & 0xFF;
          default -> (value >> 24) & 0xFF;
        };
    }
  }

  private void performWriteAction(int address, int data, int type) {
    int wData = data;
    if (type != SocBusTransaction.WORD_ACCESS) {
      var oldData = performReadAction(address, SocBusTransaction.WORD_ACCESS);
      if (type == SocBusTransaction.HALF_WORD_ACCESS) {
        final var bit1 = (address >> 1) & 1;
        var mdata = data & 0xFFFF;
        if (bit1 == 1) {
          oldData &= 0xFFFF;
          mdata <<= 16;
        } else {
          oldData = ((oldData >> 16) & 0xFFFF) << 16;
        }
        wData = oldData | mdata;
      } else {
        final var byte0 = oldData & 0xFF;
        final var byte1 = ((oldData >> 8) & 0xFF) << 8;
        final var byte2 = ((oldData >> 16) & 0xFF) << 16;
        final var byte3 = ((oldData >> 24) & 0xFF) << 24;
        final var mdata = data & 0xFF;
        final var bit10 = address & 3;
        wData = switch (bit10) {
          case 0 -> byte3 | byte2 | byte1 | mdata;
          case 1 -> byte3 | byte2 | byte0 | (mdata << 8);
          case 2 -> byte3 | byte1 | byte0 | (mdata << 16);
          default -> byte2 | byte1 | byte0 | (mdata << 24);
        };
      }
    }
    getRegPropagateState().writeWord(address, wData);
  }

  private void fireNameChanged() {
    for (final var listener : listeners) {
      listener.labelChanged();
    }
  }

  private void firememMapChanged() {
    for (final var listener : listeners) {
      listener.memoryMapChanged();
    }
  }

}
