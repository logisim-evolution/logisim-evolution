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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cburch.logisim.util.LineBuffer;

public class HdlTypes {

  private interface HdlType {
    String getTypeDefinition();

    String getTypeName();
  }

  private static class HdlEnum implements HdlType {
    private final List<String> myEntries = new ArrayList<>();
    private final String myTypeName;

    public HdlEnum(String name) {
      myTypeName = name;
    }

    public HdlEnum add(String entry) {
      for (var item = 0; item < myEntries.size(); item++)
        if (myEntries.get(item).compareTo(entry) > 0) {
          myEntries.add(item, entry);
          return this;
        }
      myEntries.add(entry);
      return this;
    }

    @Override
    public String getTypeDefinition() {
      final var contents = new StringBuilder();
      if (Hdl.isVhdl())
        contents.append(LineBuffer.formatVhdl("{{type}} {{1}} {{is}} (", myTypeName));
      else contents.append("typedef enum { ");
      var first = true;
      for (final var entry : myEntries) {
        if (first) first = false;
        else contents.append(", ");
        contents.append(entry);
      }
      if (Hdl.isVhdl()) contents.append(");");
      else contents.append(String.format("} %s;", myTypeName));
      return contents.toString();
    }

    @Override
    public String getTypeName() {
      return myTypeName;
    }
  }

  private static class HdlArray implements HdlType {
    private final String myTypeName;
    private final String myGenericBitWidth;
    private final int myBitWidth;
    private final int myNrOfEntries;

    public HdlArray(String name, String genericBitWidth, int nrOfEntries) {
      myTypeName = name;
      myGenericBitWidth = genericBitWidth;
      myBitWidth = -1;
      myNrOfEntries = nrOfEntries;
    }

    public HdlArray(String name, int nrOfBits, int nrOfEntries) {
      myTypeName = name;
      myGenericBitWidth = null;
      myBitWidth = nrOfBits;
      myNrOfEntries = nrOfEntries;
    }

    @Override
    public String getTypeDefinition() {
      final var contents = new StringBuilder();
      if (Hdl.isVhdl()) {
        contents
            .append(
                LineBuffer.formatVhdl(
                    "{{type}} {{1}} {{is}} {{array}} ( {{2}} {{downto}} 0 ) {{of}} ",
                    myTypeName, myNrOfEntries));
        if (myGenericBitWidth == null && myBitWidth == 1) {
          contents.append("std_logic;");
        } else {
          contents
              .append("std_logic_vector( ")
              .append(
                  myGenericBitWidth == null
                      ? Integer.toString(myBitWidth - 1)
                      : String.format("%s - 1", myGenericBitWidth))
              .append(
                  LineBuffer.formatVhdl(
                      " " + "{{downto}} 0);")); // Important: The leading space is required
        }
      } else {
        contents
            .append("typedef logic [")
            .append(
                myGenericBitWidth == null
                    ? Integer.toString(myBitWidth - 1)
                    : String.format("%s - 1", myGenericBitWidth))
            .append(String.format(":0] %s [%d:0];", myTypeName, myNrOfEntries));
      }
      return contents.toString();
    }

    @Override
    public String getTypeName() {
      return myTypeName;
    }
  }

  private final Map<Integer, HdlType> myTypes = new HashMap<>();
  private final Map<String, Integer> myWires = new HashMap<>();

  public HdlTypes addEnum(int identifier, String name) {
    myTypes.put(identifier, new HdlEnum(name));
    return this;
  }

  public HdlTypes addEnumEntry(int identifier, String entry) {
    if (!myTypes.containsKey(identifier))
      throw new IllegalArgumentException("Enum type not contained in array");
    final var myEnum = (HdlEnum) myTypes.get(identifier);
    myEnum.add(entry);
    return this;
  }

  public HdlTypes addArray(int identifier, String name, String genericBitWidth, int nrOfEntries) {
    myTypes.put(identifier, new HdlArray(name, genericBitWidth, nrOfEntries));
    return this;
  }

  public HdlTypes addArray(int identifier, String name, int nrOfBits, int nrOfEntries) {
    myTypes.put(identifier, new HdlArray(name, nrOfBits, nrOfEntries));
    return this;
  }

  public HdlTypes addWire(String name, int typeIdentifier) {
    myWires.put(name, typeIdentifier);
    return this;
  }

  public int getNrOfTypes() {
    return myTypes.keySet().size();
  }

  public List<String> getTypeDefinitions() {
    final var defs = LineBuffer.getHdlBuffer();
    for (final var entry : myTypes.keySet()) defs.add(myTypes.get(entry).getTypeDefinition());
    return defs.getWithIndent();
  }

  public Map<String, String> getTypedWires() {
    final var contents = new HashMap<String, String>();
    for (final var wire : myWires.keySet()) {
      final var typeId = myWires.get(wire);
      if (!myTypes.containsKey(typeId))
        throw new IllegalArgumentException("Enum or array type not contained in array");
      contents.put(wire, myTypes.get(typeId).getTypeName());
    }
    return contents;
  }

  public void clear() {
    myTypes.clear();
    myWires.clear();
  }
}
