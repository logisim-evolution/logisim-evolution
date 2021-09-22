/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.Port;


public class HDLPorts {
 
  public static final String CLOCK = "clock";
  public static final String TICK = "tick";

  private class PortInfo {

    private final String myPortType;
    private final String myName;
    private final int myNrOfBits;
    private final int myComponentPinId;
    private final String myFixedMap;
    private boolean mySinglePinException = false;
    private Attribute<?> myBitWidthAttribute = null;
    private boolean myPullToZero = true;
    private boolean isClock = false;

    public PortInfo(String type, String name, int nrOfBits, String fixedMap) {
      myPortType = type;
      myName = name;
      myNrOfBits = nrOfBits;
      myComponentPinId = -1; // fixed map
      myFixedMap = fixedMap;
    }

    public PortInfo(String type, String name, int nrOfBits, int compPinId) {
      this(type, name, nrOfBits, compPinId, null);
      mySinglePinException = false;
    }
    
    public PortInfo(String type, String name, int nrOfBits, int compPinId, boolean pullToZero) {
      this(type, name, nrOfBits, compPinId, null);
      mySinglePinException = false;
      myPullToZero = pullToZero;
    }
    
    public PortInfo(String type, String name, int nrOfBits, int compPinId, Attribute<?> nrOfBitsAttr) {
      myPortType = type;
      myName = name;
      myNrOfBits = nrOfBits;
      myComponentPinId = compPinId;
      myFixedMap = null;
      mySinglePinException = true;
      myBitWidthAttribute = nrOfBitsAttr;
    }
    
    int getNrOfBits(AttributeSet attrs) {
      if (mySinglePinException) {
        if (!attrs.containsAttribute(myBitWidthAttribute)) throw new IllegalArgumentException("Bitwidth attribute not found");
        final var value = attrs.getValue(myBitWidthAttribute);
        var nrOfBits = 0;
        if (value instanceof BitWidth) {
          nrOfBits = ((BitWidth) value).getWidth();
        } else if (value instanceof Integer) {
          nrOfBits = (int) value;
        } else throw new IllegalArgumentException("No Bitwidth or Integer value");
        return (nrOfBits == 1) ? 1 : (myNrOfBits > 0) ? myNrOfBits : nrOfBits;
      }
      return myNrOfBits;
    }
  }

  private final List<PortInfo> myPorts = new ArrayList<PortInfo>();

  public HDLPorts add(String type, String name, int nrOfBits, String fixedMap) {
    final var realType = Port.CLOCK.equals(type) ? Port.INPUT : type;
    final var newPort = new PortInfo(realType, name, nrOfBits, fixedMap);
    newPort.isClock = Port.CLOCK.equals(type);
    myPorts.add(newPort);
    return this;
  }

  public HDLPorts add(String type, String name, int nrOfBits, int compPinId) {
    final var realType = Port.CLOCK.equals(type) ? Port.INPUT : type;
    final var newPort = new PortInfo(realType, name, nrOfBits, compPinId);
    newPort.isClock = Port.CLOCK.equals(type);
    myPorts.add(newPort);
    return this;
  }

  public HDLPorts add(String type, String name, int nrOfBits, int compPinId, boolean pullToZero) {
    final var realType = Port.CLOCK.equals(type) ? Port.INPUT : type;
    final var newPort = new PortInfo(realType, name, nrOfBits, compPinId, pullToZero);
    newPort.isClock = Port.CLOCK.equals(type);
    myPorts.add(newPort);
    return this;
  }

  public HDLPorts add(String type, String name, int nrOfBits, int compPinId, Attribute<?> nrOfBitsAttr) {
    final var realType = Port.CLOCK.equals(type) ? Port.INPUT : type;
    final var newPort = new PortInfo(realType, name, nrOfBits, compPinId, nrOfBitsAttr);
    newPort.isClock = Port.CLOCK.equals(type);
    myPorts.add(newPort);
    return this;
  }

  public boolean isEmpty() {
    return myPorts.isEmpty();
  }
  
  public ArrayList<String> keySet() {
    return keySet(null);
  }
  
  public ArrayList<String> keySet(String type) {
    final var keySet = new ArrayList<String>();
    for (var port : myPorts)
      if (type == null || port.myPortType.equals(type))
        keySet.add(port.myName);
    return keySet;
  }
  
  public int get(String name, AttributeSet attrs) {
    for (var port : myPorts)
      if (port.myName.equals(name)) return port.getNrOfBits(attrs);
    throw new ArrayStoreException("port not contained in structure");
  }
  
  public boolean isFixedMapped(String name) {
    for (var port : myPorts)
      if (port.myName.equals(name)) return port.myComponentPinId < 0;
    throw new ArrayStoreException("port not contained in structure");
  }
  
  public String getFixedMap(String name) {
    if (isFixedMapped(name)) {
      for (var port : myPorts)
        if (port.myName.equals(name)) return port.myFixedMap;
    }
    throw new ArrayStoreException("port not contained in structure or not fixed mapped");
  }
  
  public int getComponentPortId(String name) {
    for (var port : myPorts)
      if (port.myName.equals(name)) return port.myComponentPinId;
    throw new ArrayStoreException("port not contained in structure");
  }
  
  public void removePorts() {
    myPorts.clear();
  }
  
  public boolean doPullDownOnFloat(String name) {
    for (var port : myPorts)
      if (port.myName.equals(name)) return port.myPullToZero;
    throw new ArrayStoreException("port not contained in structure");
  }
  
  public boolean isClock(String name) {
    for (var port : myPorts)
      if (port.myName.equals(name)) return port.isClock;
    throw new ArrayStoreException("port not contained in structure");
  }
}
