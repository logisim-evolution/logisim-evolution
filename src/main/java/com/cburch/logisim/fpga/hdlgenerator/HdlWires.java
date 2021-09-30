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
import java.util.Map;

public class HdlWires {

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

  public HdlWires addWire(String name, int nrOfBits) {
    myWires.add(new Wire(WIRE, name, nrOfBits));
    return this;
  }

  public HdlWires addRegister(String name, int nrOfBits) {
    myWires.add(new Wire(REGISTER, name, nrOfBits));
    return this;
  }

  public HdlWires addAllWires(Map<String, Integer> wires) {
    for (var wire : wires.keySet())
      myWires.add(new Wire(WIRE, wire, wires.get(wire)));
    return this;
  }

  public List<String> wireKeySet() {
    final var keys = new ArrayList<String>();
    for (var wire : myWires)
      if (wire.isWire()) keys.add(wire.getName());
    return keys;
  }

  public List<String> registerKeySet() {
    final var keys = new ArrayList<String>();
    for (var wire : myWires)
      if (!wire.isWire()) keys.add(wire.getName());
    return keys;
  }

  public int get(String wireName) {
    for (var wire : myWires)
      if (wire.getName().equals(wireName)) return wire.getNrOfBits();
    throw new ArrayStoreException("Wire or register not contained in structure");
  }

  public void removeWires() {
    final var iterator = myWires.iterator();
    while (iterator.hasNext())
      if (iterator.next().isWire()) iterator.remove();
  }
}
