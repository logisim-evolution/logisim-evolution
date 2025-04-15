/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.cburch.hdl.HdlModel.PortDescription;
import com.cburch.logisim.instance.Port;

/**
 * Parses a BLIF file.
 * The BLIF file must contain a single circuit made up of a fixed set of supported subcircuits.
 * The objective here is to provide a Yosys-compatible target.
 * This can be compiled with something like: write_blif -icells -buf BUF A Y test1.blif
 */
public class BlifParser {
  private final List<PortDescription> inputs;
  private final List<PortDescription> outputs;
  private final List<Gate> gates;
  private final List<DFF> dff;
  private final List<DFFSR> dffsr;
  private final List<String> pullups;
  private final List<String> pulldowns;
  private final String source;
  private String name;

  public BlifParser(String source) {
    this.source = source;
    this.inputs = new ArrayList<>();
    this.outputs = new ArrayList<>();
    this.gates = new ArrayList<>();
    this.dff = new ArrayList<>();
    this.dffsr = new ArrayList<>();
    this.pullups = new ArrayList<>();
    this.pulldowns = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public void parse() {
    TreeSet<String> inputSet = new TreeSet<>();
    TreeSet<String> outputSet = new TreeSet<>();
    for (String s : source.split("\n")) {
      String lineActual = s.trim();
      if (lineActual.startsWith(".")) {
        // actual command, we may need to listen to it
        String[] words = lineActual.split(" ");
        if (words[0].equals(".inputs")) {
          for (int i = 1; i < words.length; i++)
            inputSet.add(words[i]);
        } else if (words[0].equals(".outputs")) {
          for (int i = 1; i < words.length; i++)
            outputSet.add(words[i]);
        } else if (words[0].equals(".subckt")) {
          String type = words[1];
          HashMap<String, String> pins = new HashMap<>();
          for (int i = 2; i < words.length; i++) {
            String assignment = words[i];
            int eq = assignment.indexOf('=');
            String k = assignment.substring(0, eq);
            String v = assignment.substring(eq + 1);
            pins.put(k, v);
          }

          // This list is partially based on https://github.com/YosysHQ/yosys/blob/0c689091e2a0959b1a6173de1bd7bd679b6120b2/examples/cmos/cmos_cells.lib
          // However, this I feel is a more general set of gates.
          if (type.equals("BUF")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_BUS, pins.get("A"), pins.get("A"), pins.get("Y")));
          } else if (type.equals("NOT")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_NOR, pins.get("A"), pins.get("A"), pins.get("Y")));
          } else if (type.equals("AND")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_AND, pins.get("A"), pins.get("B"), pins.get("Y")));
          } else if (type.equals("NAND")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_NAND, pins.get("A"), pins.get("B"), pins.get("Y")));
          } else if (type.equals("OR")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_OR, pins.get("A"), pins.get("B"), pins.get("Y")));
          } else if (type.equals("NOR")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_NOR, pins.get("A"), pins.get("B"), pins.get("Y")));
          } else if (type.equals("XOR")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_XOR, pins.get("A"), pins.get("B"), pins.get("Y")));
          } else if (type.equals("NXOR")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_NXOR, pins.get("A"), pins.get("B"), pins.get("Y")));
          } else if (type.equals("TRIS")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_TRIS, pins.get("A"), pins.get("B"), pins.get("Y")));
          } else if (type.equals("PULLUP")) {
            pullups.add(pins.get("Y"));
          } else if (type.equals("PULLDOWN")) {
            pulldowns.add(pins.get("Y"));
          } else if (type.equals("DFF")) {
            dff.add(new DFF(pins.get("C"), pins.get("D"), pins.get("Q")));
          } else if (type.equals("DFFSR")) {
            dffsr.add(new DFFSR(pins.get("C"), pins.get("D"), pins.get("Q"), pins.get("S"), pins.get("R")));
          } else {
            throw new RuntimeException("Unknown subcircuit " + type);
          }
        }
      }
    }
    // A pin can't be both an input and an output according to the layout logic.
    // Really, we should be checking for this, as it can be used to implement lots of fun fastpathy stuff.
    // Oh well.
    inputSet.removeIf((s) -> outputSet.contains(s));
    // Now parse inputs and outputs into port descriptions (aka the hard part).
    parseNamesIntoDescriptions(inputs, inputSet);
    parseNamesIntoDescriptions(outputs, outputSet);
  }

  private void parseNamesIntoDescriptions(List<PortDescription> ports, SortedSet<String> src) {
    // For now, use a very bad implementation just to get things kind of working.
    while (!src.isEmpty()) {
      String queue = src.removeFirst();
      ports.add(new PortDescription(queue, Port.INOUT, 1));
    }
  }

  public List<PortDescription> getInputs() {
    return inputs;
  }

  public List<PortDescription> getOutputs() {
    return outputs;
  }

  /**
   * Compiles the BLIF into the DenseLogicCircuit format used for simulation.
   */
  public DenseLogicCircuit compile() {
    DenseLogicCircuitBuilder builder = new DenseLogicCircuitBuilder();
    // The builder's symbol table is used to do all the complicated stuff.
    // x. : External input. The simulator can drive inputs using this.
    // i. : Internal input; merges external input and internal output, for use by gates inside the circuit.
    // o. : External output; what this circuit sends to the outside world, unbiased by external input.
    // The integration layer is expected to write to x. cells and read from o. cells.
    builder.symbolTable.put("i.$false", DenseLogicCircuit.LEV_LOW);
    builder.symbolTable.put("o.$false", DenseLogicCircuit.LEV_LOW);
    builder.symbolTable.put("i.$true", DenseLogicCircuit.LEV_HIGH);
    builder.symbolTable.put("o.$true", DenseLogicCircuit.LEV_HIGH);
    builder.symbolTable.put("i.$undef", DenseLogicCircuit.LEV_NONE);
    builder.symbolTable.put("o.$undef", DenseLogicCircuit.LEV_NONE);
    // prepare inputs & outputs
    for (PortDescription pd : inputs)
      compilePort(builder, pd);
    for (PortDescription pd : outputs)
      compilePort(builder, pd);
    // install gates
    for (Gate g : gates) {
      int aInput = compileGetInput(builder, g.a);
      int bInput = compileGetInput(builder, g.b);
      int yOutput = compileGetOutput(builder, g.y);
      builder.attachGate(g.type, aInput, bInput, yOutput);
    }
    // install DFFs
    for (DFF ff : dff)
      builder.attachBuffer(builder.addDFF(compileGetInput(builder, ff.c), compileGetInput(builder, ff.d)), compileGetOutput(builder, ff.q));
    for (DFFSR ff : dffsr) {
      int c = compileGetInput(builder, ff.c);
      int d = compileGetInput(builder, ff.d);
      int q = compileGetOutput(builder, ff.q);
      int s = compileGetInput(builder, ff.s);
      int r = compileGetInput(builder, ff.r);
      builder.attachBuffer(builder.addDFFSR(c, d, s, r), q);
    }
    // install pullups/pulldowns
    for (String s : pullups)
      builder.setCellPull(compileGetOutput(builder, s), DenseLogicCircuit.LEV_HIGH);
    for (String s : pulldowns)
      builder.setCellPull(compileGetOutput(builder, s), DenseLogicCircuit.LEV_LOW);
    // done!
    return builder.build();
  }

  /**
   * Creates three cells: the external input cell, the internal input cell, and the external output cell.
   * Internal input merges external input and internal output.
   */
  private void compileBidiPin(DenseLogicCircuitBuilder builder, String pin) {
    int externalInput = builder.addCell(true);
    int internalInput = builder.addCell(false);
    int externalOutput = builder.addCell(false);
    builder.symbolTable.put("x." + pin, externalInput);
    builder.symbolTable.put("i." + pin, internalInput);
    builder.symbolTable.put("o." + pin, externalOutput);
    builder.attachGate(DenseLogicCircuit.GATE_BUS, externalInput, externalOutput, internalInput);
  }
  /**
   * Creates bidi pins for a port.
   */
  private void compilePort(DenseLogicCircuitBuilder builder, PortDescription port) {
    int width = port.getWidthInt();
    if (width == 1) {
      compileBidiPin(builder, port.getName());
    } else {
      for (int i = 0; i < width; i++)
        compileBidiPin(builder, port.getName() + "[" + i + "]");
    }
  }
  private int compileGetInput(DenseLogicCircuitBuilder builder, String pin) {
    Integer existing = builder.symbolTable.get("i." + pin);
    if (existing == null)
      existing = compileNewWire(builder, pin);
    return existing;
  }
  private int compileGetOutput(DenseLogicCircuitBuilder builder, String pin) {
    Integer existing = builder.symbolTable.get("o." + pin);
    if (existing == null)
      existing = compileNewWire(builder, pin);
    return existing;
  }
  private int compileNewWire(DenseLogicCircuitBuilder builder, String pin) {
    int res = builder.addCell(false);
    builder.symbolTable.put("i." + pin, res);
    builder.symbolTable.put("o." + pin, res);
    return res;
  }

  /**
   * BLIF subcircuit data.
   */
  public record Gate(int type, String a, String b, String y) {
  }

  /**
   * BLIF subcircuit data.
   */
  public record DFF(String c, String d, String q) {
  }

  /**
   * BLIF subcircuit data.
   */
  public record DFFSR(String c, String d, String q, String s, String r) {
  }
}
