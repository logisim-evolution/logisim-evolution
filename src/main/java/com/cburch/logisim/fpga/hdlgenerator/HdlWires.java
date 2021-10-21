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
import lombok.Getter;

public class HdlWires {

  public static final int WIRE = 0;
  public static final int REGISTER = 1;

  private class Wire {
    private final int myType;
    @Getter private final String name;
    @Getter private final int nrOfBits;

    public Wire(int type, String name, int nrOfBits) {
      myType = type;
      this.name = name;
      this.nrOfBits = nrOfBits;
    }

    public boolean isWire() {
      return myType == WIRE;
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
    for (var wire : myWires) {
      if (!wire.isWire()) keys.add(wire.getName());
    }
    return keys;
  }

  public int get(String wireName) {
    for (var wire : myWires)
      if (wire.getName().equals(wireName)) return wire.getNrOfBits();
    throw new ArrayStoreException("Wire or register not contained in structure");
  }

  public void removeWires() {
    final var iterator = myWires.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().isWire()) iterator.remove();
    }
  }
}
