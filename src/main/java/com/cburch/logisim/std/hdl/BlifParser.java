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
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.cburch.hdl.HdlModel.PortDescription;
import com.cburch.logisim.instance.Port;

/**
 * Parses a BLIF file.
 * The BLIF file must contain a single circuit made up of a fixed set of supported subcircuits.
 * The objective here is to provide a Yosys-compatible target.
 * This can be compiled with something like: write_blif -icells -conn test1.blif
 * Some effort has been made to support other configurations of Yosys, but icells must be used.
 */
public final class BlifParser {
  private final List<PortDescription> inputs;
  private final List<PortDescription> outputs;
  private final List<Gate> gates;
  private final List<Mux> muxes;
  private final List<DFF> dff;
  private final List<DFFSR> dffsr;
  private final List<Latch> latches;
  private final List<String> pullups;
  private final List<String> pulldowns;
  private final String source;
  private String name;

  public BlifParser(String source) {
    this.source = source;
    this.inputs = new ArrayList<>();
    this.outputs = new ArrayList<>();
    this.gates = new ArrayList<>();
    this.muxes = new ArrayList<>();
    this.dff = new ArrayList<>();
    this.dffsr = new ArrayList<>();
    this.latches = new ArrayList<>();
    this.pullups = new ArrayList<>();
    this.pulldowns = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public void parse() {
    // Determinism is absolutely important here; exact ordering is less important, but should be consistent.
    // Preserving original order is probably not ideal, as it requires trusting the generator not to reorder things itself.
    // While Yosys does appear to avoid this, assuming this as a guarantee feels naive and dangerous.
    // TreeMap/Map uses String.compare, which does not take locale into account.
    // This is an intentional choice on my part and should apply to everything in this code that affects port order.
    var inputSet = new TreeSet<String>();
    var outputSet = new TreeSet<String>();
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
        } else if (words[0].equals(".names")) {
          // If Yosys is correctly configured, this is only used for constants.
          if (words.length == 2) {
            if (words[1].equals("$true")) {
              // constant, we generate this internally
            } else if (words[1].equals("$false")) {
              // constant, we generate this internally
            } else if (words[1].equals("$undef")) {
              // constant, we generate this internally
            } else {
              // There's probably something to be said for the need to translate these exception messages somehow...
              throw new RuntimeException("unexpected .names (did you pass '-icells -conn' to 'write_blif'?)");
            }
          } else {
            throw new RuntimeException(".names of unexpected length (did you pass '-icells -conn' to 'write_blif'?)");
          }
        } else if (words[0].equals(".conn")) {
          // .conn IN OUT ; enabled with -conn
          gates.add(new Gate(DenseLogicCircuit.GATE_BUS, words[1], words[1], words[2]));
        } else if (words[0].equals(".subckt") || words[0].equals(".gate")) {
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
          // However, this I feel is a more general set of components.
          // Note that the binary gates are in the simulator core (as DenseLogicCircuit.GATE_TYPE_NAMES).
          if (type.equals("BUF")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_BUS, pins.get("A"), pins.get("A"), pins.get("Y")));
          } else if (type.equals("NOT")) {
            // !(A|A)
            gates.add(new Gate(DenseLogicCircuit.GATE_NOR, pins.get("A"), pins.get("A"), pins.get("Y")));
          } else if (type.equals("TRIS")) {
            gates.add(new Gate(DenseLogicCircuit.GATE_TRIS, pins.get("A"), pins.get("B"), pins.get("Y")));
          } else if (type.equals("MUX")) {
            muxes.add(new Mux(pins.get("A"), pins.get("B"), pins.get("S"), pins.get("Y")));
          } else if (type.equals("PULLUP")) {
            pullups.add(pins.get("Y"));
          } else if (type.equals("PULLDOWN")) {
            pulldowns.add(pins.get("Y"));
          } else if (type.equals("DFF")) {
            dff.add(new DFF(pins.get("C"), pins.get("D"), pins.get("Q")));
          } else if (type.equals("DFFSR")) {
            dffsr.add(new DFFSR(pins.get("C"), pins.get("D"), pins.get("Q"), pins.get("S"), pins.get("R")));
          } else if (type.equals("DLATCH")) {
            latches.add(new Latch(pins.get("D"), pins.get("E"), pins.get("Q")));
          } else {
            boolean found = false;
            for (int gateType = 0; gateType < DenseLogicCircuit.GATE_TYPE_NAMES.length; gateType++) {
              if (type.equals(DenseLogicCircuit.GATE_TYPE_NAMES[gateType])) {
                // found a binary gate
                gates.add(new Gate(gateType, pins.get("A"), pins.get("B"), pins.get("Y")));
                found = true;
                break;
              }
            }
            if (!found)
              throw new RuntimeException("Unknown gate: " + type);
          }
        }
      }
    }
    // A pin can't be both an input and an output according to the layout logic.
    // Really, we should be checking for this, as it can be used to implement lots of fun fastpathy stuff.
    // Oh well.
    inputSet.removeIf((s) -> outputSet.contains(s));
    // Now parse inputs and outputs into port descriptions (aka the hard part).
    parseNamesIntoDescriptions(inputs, inputSet, Port.INPUT);
    parseNamesIntoDescriptions(outputs, outputSet, Port.OUTPUT);
  }

  /**
   * This and getPinBLIFSymbol describe the outer layer of the 'ABI' used for the ports.
   * The inner layer is the "x."/"i."/"o." prefix set described in compileBidiPin.
   * Of these, only "x." and "o." are relevant externally.
   */
  private void parseNamesIntoDescriptions(List<PortDescription> ports, SortedSet<String> src, String type) {
    // See inputSet/outputSet for data structure choice rationale.
    var maxBitNumbers = new TreeMap<String, Integer>();
    while (!src.isEmpty()) {
      String queue = src.removeFirst();
      if (queue.endsWith("]")) {
        // this *could* be a set
        int leftIdx = queue.lastIndexOf('[');
        if (leftIdx != -1) {
          String bitIndex = queue.substring(leftIdx + 1, queue.length() - 1);
          try {
            int num = Integer.parseUnsignedInt(bitIndex);
            String prefix = queue.substring(0, leftIdx);
            Integer existing = maxBitNumbers.get(prefix);
            if (existing != null)
              if (existing > num)
                num = existing;
            maxBitNumbers.put(prefix, num);
            // handled
            continue;
          } catch (NumberFormatException ex) {
            // ok, it didn't parse, fall back
          }
        }
      }
      // Single port, regardless of what it says
      ports.add(new PortDescription(queue, type, 1));
    }
    // All buses are handled in this separate pass, since aggregation needs to happen.
    // This means buses always go after individual signals.
    // Note the +1 ; the given value is the bit number, so we have to add 1 to it to get the bit count.
    for (Map.Entry<String, Integer> map : maxBitNumbers.entrySet()) {
      int width = map.getValue() + 1;
      if (width == 1) {
        // So something interesting has happened in this case: we have found a 'bus' of only 1 bit.
        // The way this 'ABI' works is based on bit width, so we really need to get that [0] back in somehow.
        ports.add(new PortDescription(map.getKey() + "[0]", type, 1));
      } else {
        ports.add(new PortDescription(map.getKey(), type, width));
      }
    }
  }

  /**
   * Describes the outer 'ABI' (and used by compilePort to compile it).
   */
  public String getPinBLIFSymbol(PortDescription port, int index) {
    int width = port.getWidthInt();
    if (width == 1) {
      return port.getName();
    } else {
      return port.getName() + "[" + index + "]";
    }
  }

  /**
   * Implements the 'inner ABI' (the input/output line mechanism used to try and prevent self-propagation).
   */
  public String getPinDLCSymbol(PortDescription port, int index, boolean output) {
    return (output ? "o." : "x.") + getPinBLIFSymbol(port, index);
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
    // install muxes
    for (Mux m : muxes) {
      // Muxes are interesting, because we don't natively support them (they would be 3-input).
      // But we do have the building blocks to implement them in a better way.
      // The TRIS and TRISI gates are attached, and a BUS gate is automatically added by the builder.
      // This creates a 3-gate mux which also handles tri-state inputs.
      // It would also be possible to create a 3-gate mux with regular logic with some inverted inputs.
      // i.e. Y = (B & S) | (A & !S); this would use the ANDNOT gate. Again: no tri-state.
      int aInput = compileGetInput(builder, m.a);
      int bInput = compileGetInput(builder, m.b);
      int sInput = compileGetInput(builder, m.s);
      int yOutput = compileGetOutput(builder, m.y);
      builder.attachGate(DenseLogicCircuit.GATE_TRISI, aInput, sInput, yOutput);
      builder.attachGate(DenseLogicCircuit.GATE_TRIS, bInput, sInput, yOutput);
    }
    // install DFFs
    for (DFF ff : dff)
      builder.attachBuffer(builder.addDFF(compileGetInput(builder, ff.c), compileGetInput(builder, ff.d)), compileGetOutput(builder, ff.q));
    // install latches
    for (Latch l : latches)
      builder.attachBuffer(builder.addLatch(compileGetInput(builder, l.d), compileGetInput(builder, l.e)), compileGetOutput(builder, l.q));
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
    for (int i = 0; i < width; i++)
      compileBidiPin(builder, getPinBLIFSymbol(port, i));
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

  public record Gate(int type, String a, String b, String y) {
  }

  public record Mux(String a, String b, String s, String y) {
  }

  public record DFF(String c, String d, String q) {
  }

  public record DFFSR(String c, String d, String q, String s, String r) {
  }

  public record Latch(String d, String e, String q) {
  }
}
