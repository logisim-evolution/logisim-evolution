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

package com.cburch.logisim.soc.memory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSlaveListener;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSupport;

public class SocMemoryState implements SocBusSlaveInterface {

  public class SocMemoryInfo implements InstanceData,Cloneable {
    private class SocMemoryInfoBlock {
      private LinkedList<Integer> contents = new LinkedList<Integer>();
      private int startAddress;
      private Random rand = new Random();
    
      public SocMemoryInfoBlock(int address, int data) {
        startAddress = (address>>2)<<2;
        contents.add(data);
      }
    
      public boolean canAddBefore( int address ) {
        int previousAddress = getStartAddress()-4;
        return (address >= previousAddress) && (address < getStartAddress());
      }
    
      public boolean canAddAfter( int address ) {
        return (address >= getEndAddress())&&(address < getEndAddress()+4);
      }
    
      public boolean contains( int address ) {
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
          int index = (address-startAddress)>>2;
          contents.set(index, data);
          return true;
        }
        return false;
      }
    
      public int getValue(int address) {
        int index = (address-startAddress)>>2;
        if (index >= contents.size()) {
          return rand.nextInt();
        }
        return contents.get(index);
      }
    
      public int getStartAddress() {
        return startAddress;
      }
    
      public int getEndAddress() {
        return startAddress+contents.size()*4;
      }
    }

    private ArrayList<SocMemoryInfoBlock> memInfo;
    
    public SocMemoryInfo() {
      memInfo = new ArrayList<SocMemoryInfoBlock>();
    }

    public SocMemoryInfo clone() {
      try {
        return (SocMemoryInfo) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
      
    public int getWord(int address) {
      for (SocMemoryInfoBlock info : memInfo)
        if (info.contains(address))
          return info.getValue(address);
      return rand.nextInt();
    }

    public void writeWord(int address, int wdata) {
      ArrayList<SocMemoryInfoBlock> adders = new ArrayList<SocMemoryInfoBlock>();
      for (SocMemoryInfoBlock info : memInfo) {
        if (info.contains(address)) {
          info.addInfo(address, wdata);
          return;
        }
        if (info.canAdd(address))
          adders.add(info);
      }
      if (adders.isEmpty()) {
        /* we have to create a new set */
        memInfo.add(new SocMemoryInfoBlock(address,wdata));
        return;
      }
      if (adders.size() == 1) {
        /* easy case we can add in front or at the end of an existing one */
        adders.get(0).addInfo(address, wdata);
        return;
      }
      if (adders.size() > 2) {
        System.out.println("BUG! Memory management does not function corectly for the SocMemory component!");
        return;
      }
      SocMemoryInfoBlock addbefore = adders.get(0).canAddBefore(address) ? adders.get(0) :
          adders.get(1).canAddBefore(address) ? adders.get(1) : null;
      SocMemoryInfoBlock addAfter = adders.get(0).canAddAfter(address) ? adders.get(0) :
          adders.get(1).canAddAfter(address) ? adders.get(1) : null;
      if (addbefore == null || addAfter == null) {
        System.out.println("BUG! Memory management does not function corectly for the SocMemory component!");
        return;
      }
      addAfter.addInfo(address, wdata);
      for (int i = addbefore.getStartAddress() ; i < addbefore.getEndAddress() ; i += 4) {
        addAfter.addInfo(i, addbefore.getValue(i));
      }
      memInfo.remove(addbefore);
    }
  }
  
  private int startAddress;
  private int sizeInBytes;
  private Random rand = new Random();
  private SocBusInfo attachedBus;
  private String label;
  private ArrayList<SocBusSlaveListener> listeners;
  
  public SocMemoryState() {
    startAddress = 0;
    sizeInBytes = 1024;
    attachedBus = new SocBusInfo("");
    label = "";
    listeners = new ArrayList<SocBusSlaveListener>();
  }
  
  public Integer getStartAddress() {
    return startAddress;
  }
  
  public Integer getMemorySize() {
    return sizeInBytes;
  }
  
  public boolean setStartAddress(int address) {
    int addr = (address >> 2)<<2;
    if (addr == startAddress)
      return false;
    startAddress = addr;
    firememMapChanged();
    return true;
  }
  
  public boolean setSize(BitWidth i) {
    int size = (int) Math.pow(2, i.getWidth());
    if (sizeInBytes == size)
      return false;
    sizeInBytes = size;
    firememMapChanged();
    return true;
  }
  
  public SocBusInfo getSocBusInfo() {
    return attachedBus;
  }
  
  public boolean setSocBusInfo(SocBusInfo i) {
    if (attachedBus.getBusId().equals(i.getBusId()))
      return false;
    attachedBus.setBusId(i.getBusId());
    return true;
  }
  
  public String getLabel() {
    return label;
  }
  
  public boolean setLabel(String l) {
    if (label.equals(l))
      return false;
    label = l;
    fireNameChanged();
    return true;
  }
  
  public String getName() {
    if (attachedBus == null || attachedBus.getComponent() == null)
      return "BUG: Unknown";
    String name = label;
    if (name == null || name.isEmpty()) {
      Location loc = attachedBus.getComponent().getLocation();
      name = attachedBus.getComponent().getFactory().getDisplayName()+"@"+loc.getX()+","+loc.getY();
    }
    return name;
  }
  
  public InstanceComponent getComponent() {
    if (attachedBus == null || attachedBus.getComponent() == null)
      return null;
    return (InstanceComponent) attachedBus.getComponent();
  }

  public void registerListener(SocBusSlaveListener l) {
    if (!listeners.contains(l))
      listeners.add(l);
  }
  
  public void removeListener(SocBusSlaveListener l) {
    if (listeners.contains(l))
      listeners.remove(l);
  }
  
  public SocMemoryInfo getNewState() {
    return new SocMemoryInfo();
  }

  @Override
  public boolean canHandleTransaction(SocBusTransaction trans) {
	long addr = SocSupport.convUnsignedInt(trans.getAddress());
	long start = SocSupport.convUnsignedInt(startAddress);
	long end = start+sizeInBytes;
    return (addr >= start)&&(addr < end);
  }
  
  @Override
  public void handleTransaction(SocBusTransaction trans) {
	if (!canHandleTransaction(trans)) /* this should never happen */
	  return;
	if (trans.isReadTransaction()) {
	  trans.setReadData(performReadAction(trans.getAddress(),trans.getAccessType()));
	}
	if (trans.isWriteTransaction()) {
	  performWriteAction(trans.getAddress(),trans.getWriteData(),trans.getAccessType());
	}
	trans.setTransactionResponder(attachedBus.getComponent());
  }
  
  private SocMemoryInfo getRegPropagateState() {
    return (SocMemoryInfo) attachedBus.getSocSimulationManager().getdata(attachedBus.getComponent());
  }
  
  private int performReadAction(int address, int type) {
    SocMemoryInfo data = getRegPropagateState();
    int value = (data == null) ? rand.nextInt() : data.getWord((address>>2)<<2);
    int adbit1 = (address >> 1)&1;
    switch (type) {
      case SocBusTransaction.WordAccess : return value;
      case SocBusTransaction.HalfWordAccess : if (adbit1 == 1)
    	                                        return (value>>16)&0xFFFF;
                                              else
                                            	return value&0xFFFF;
    }
    int adbit1_0 = address&3;
    switch (adbit1_0) {
      case 0 : return value&0xFF;
      case 1 : return (value>>8)&0xFF;
      case 2 : return (value>>16)&0xFF;
      default: return (value >>24)&0xFF;
    }
  }
  
  private void performWriteAction(int address, int data, int type) {
	int wdata = data;
	if (type != SocBusTransaction.WordAccess) {
	  int oldData = performReadAction(address,SocBusTransaction.WordAccess);
	  if (type == SocBusTransaction.HalfWordAccess) {
	    int bit1 = (address >> 1)&1;
	    int mdata = data&0xFFFF;
	    if (bit1 == 1) {
	      oldData &= 0xFFFF;
	      mdata <<= 16;
	      wdata = oldData|mdata;
	    } else {
	      oldData = ((oldData >>16)&0xFFFF)<<16;
	      wdata = oldData|mdata;
	    }
	  } else {
	    int byte0 = oldData&0xFF;
	    int byte1 = ((oldData>>8)&0xFF)<<8;
	    int byte2 = ((oldData>>16)&0xFF)<<16;
	    int byte3 = ((oldData>>24)&0xFF)<<24;
	    int mdata = data&0xFF;
	    int bit10 = address&3;
	    switch (bit10) {
	      case 0 : wdata = byte3|byte2|byte1|mdata;
	               break;
	      case 1 : wdata = byte3|byte2|byte0|(mdata<<8);
                   break;
	      case 2 : wdata = byte3|byte1|byte0|(mdata<<16);
                   break;
	      default : wdata = byte2|byte1|byte0|(mdata<<24);
	    }
	  }
	}
	getRegPropagateState().writeWord(address, wdata);
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
