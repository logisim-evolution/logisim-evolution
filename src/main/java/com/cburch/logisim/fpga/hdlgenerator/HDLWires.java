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

public class HDLWires {

  public static final int WIRE = 0;
  public static final int REGISTER = 1;

  private class Wire {

    private final int myType;
    private final String myName;
    private final int myNrOfBits;
    
    public Wire(int type, String name, int nrOfBits) {
      myType = type;
      myName = name;
      myNrOfBits = nrOfBits;
    }

    public boolean isWire() {
      return myType == WIRE;
    }

    public String getName() {
      return myName;
    }

    public int getNrOfBits() {
      return myNrOfBits;
    }
  }

  private final List<Wire> myWires = new ArrayList<>();

  public List<String> wireKeySet() {
    final var keys = new ArrayList<String> ();
    for (var wire : myWires) 
      if (wire.isWire()) keys.add(wire.getName());
    return keys;
  }
  
  public int get(String wireName) {
    for (var wire : myWires)
      if (wire.getName().equals(wireName)) return wire.myNrOfBits;
    throw new ArrayStoreException("Wire or register not contained in structure");
  }
}
