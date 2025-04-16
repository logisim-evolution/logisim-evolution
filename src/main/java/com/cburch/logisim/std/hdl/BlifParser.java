/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import com.cburch.hdl.HdlModel.PortDescription;
import com.cburch.logisim.instance.Port;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Parses a BLIF file.
 * The BLIF file must contain a single circuit made up of a fixed set of supported subcircuits.
 * The objective here is to provide a Yosys-compatible target.
 * This can be compiled with something like: write_blif -icells -conn test1.blif
 * Some effort has been made to support other configurations of Yosys, but icells must be used.
 */
public final class BlifParser {
  public static final String RECOMMENDED_LOGIC_LIBRARY_URL =
      "https://github.com/YosysHQ/yosys/tree/main/examples/cmos";

  private final List<PortDescription> inputs;
  private final List<PortDescription> outputs;
  private final List<Gate> gates;
  private final List<Mux> muxes;
  private final List<Dff> dff;
  private final List<Dffsr> dffsr;
  private final List<Latch> latches;
  private final List<String> pullups;
  private final List<String> pulldowns;
  private final String source;
  private String name;

  /**
   * Creates the BlifParser with the source.
   * The parse won't be complete until parse() is called.
   * And it should only be called once.
   */
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

  /**
   * Gets the model name (or null if none).
   */
  public String getName() {
    return name;
  }

  /**
   * Parses the BLIF file.
   * Call only once.
   */
  public void parse() {
    // Determinism is absolutely important here.
    // Exact ordering is less important, but should be consistent.
    // Preserving original order is probably not ideal:
    //  it requires trusting the generator not to reorder things itself.
    // While Yosys does appear to avoid this, assuming this as a guarantee feels dangerous.
    // TreeMap/Map uses String.compare, which does not take locale into account.
    // This is an intentional choice and should apply to anything that affects port order.
    var inputSet = new TreeSet<String>();
    var outputSet = new TreeSet<String>();
    for (String s : source.split("\n")) {
      String lineActual = s.trim();
      if (lineActual.startsWith(".")) {
        // actual command, we may need to listen to it
        String[] words = lineActual.split(" ");
        if (words[0].equals(".inputs")) {
          for (int i = 1; i < words.length; i++) {
            inputSet.add(words[i]);
          }
        } else if (words[0].equals(".outputs")) {
          for (int i = 1; i < words.length; i++) {
            outputSet.add(words[i]);
          }
        } else if (words[0].equals(".model")) {
          // use this as a name source if possible
          if (words.length > 1) {
            if (name != null)
              throw new RuntimeException("many tops (try 'synth -flatten -top " + name + "')");
            name = words[1];
          }
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
              // There's probably something to be said about translating these exception messages.
              throw new RuntimeException("odd .names (try 'write_blif -icells -conn out.blif')");
            }
          } else {
            throw new RuntimeException("odd .names (try 'write_blif -icells -conn out.blif')");
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

          // This list is partially based on yosys examples/cmos/cmos_cells.lib
          // However, this I feel is a more general set of components.
          // Note that the binary gates are in DenseLogicCircuit.GATE_TYPE_NAMES
          if (type.equals("BUF")) {
            String a = pins.get("A");
            String y = pins.get("Y");
            gates.add(new Gate(DenseLogicCircuit.GATE_BUS, a, a, y));
          } else if (type.equals("NOT")) {
            // !(A|A)
            String a = pins.get("A");
            String y = pins.get("Y");
            gates.add(new Gate(DenseLogicCircuit.GATE_NOR, a, a, y));
          } else if (type.equals("MUX")) {
            String a = pins.get("A");
            String b = pins.get("B");
            String select = pins.get("S");
            String y = pins.get("Y");
            muxes.add(new Mux(a, b, select, y));
          } else if (type.equals("PULLUP")) {
            pullups.add(pins.get("Y"));
          } else if (type.equals("PULLDOWN")) {
            pulldowns.add(pins.get("Y"));
          } else if (type.equals("DFF")) {
            String c = pins.get("C");
            String d = pins.get("D");
            String q = pins.get("Q");
            dff.add(new Dff(c, d, q));
          } else if (type.equals("DFFSR")) {
            String c = pins.get("C");
            String d = pins.get("D");
            String q = pins.get("Q");
            dffsr.add(new Dffsr(c, d, q, pins.get("S"), pins.get("R")));
          } else if (type.equals("DLATCH")) {
            String d = pins.get("D");
            String e = pins.get("E");
            String q = pins.get("Q");
            latches.add(new Latch(d, e, q));
          } else {
            boolean found = false;
            String[] gateTypeNames = DenseLogicCircuit.GATE_TYPE_NAMES;
            for (int gateType = 0; gateType < gateTypeNames.length; gateType++) {
              if (type.equals(gateTypeNames[gateType])) {
                // found a binary gate
                gates.add(new Gate(gateType, pins.get("A"), pins.get("B"), pins.get("Y")));
                found = true;
                break;
              }
            }
            if (!found) {
              throw new RuntimeException("unknown gate " + type + "\n" +
                  "try basing your script on " + RECOMMENDED_LOGIC_LIBRARY_URL);
            }
          }
        }
      }
    }
    // A pin can't be both an input and an output according to the layout logic.
    // Really, it'd be good to generate pins according to their type (or use).
    // Oh well.
    inputSet.removeIf((s) -> outputSet.contains(s));
    // Now parse inputs and outputs into port descriptions (aka the hard part).
    parseNameSet(inputs, inputSet, Port.INPUT);
    parseNameSet(outputs, outputSet, Port.OUTPUT);
  }

  /**
   * This and getPinBlifSymbol describe the outer layer of the 'ABI' used for the ports.
   * The inner layer is the "x."/"i."/"o." prefix set described in compileBidiPin.
   * Of these, only "x." and "o." are relevant externally.
   */
  private void parseNameSet(List<PortDescription> ports, SortedSet<String> src, String type) {
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
            if (existing != null && existing > num) {
              num = existing;
            }
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
    // Note the +1 ; the given value is the bit number.
    // So we have to add 1 to it to get the bit count.
    for (Map.Entry<String, Integer> map : maxBitNumbers.entrySet()) {
      int width = map.getValue() + 1;
      if (width == 1) {
        // So something interesting has happened in this case:
        // We have found a 'bus' of only 1 bit.
        // The way this 'ABI' works is based on bit width.
        // We really need to get that [0] back in somehow.
        ports.add(new PortDescription(map.getKey() + "[0]", type, 1));
      } else {
        ports.add(new PortDescription(map.getKey(), type, width));
      }
    }
  }

  /**
   * Describes the outer 'ABI' (and used by compilePort to compile it).
   */
  public String getPinBlifSymbol(PortDescription port, int index) {
    int width = port.getWidthInt();
    if (width == 1) {
      return port.getName();
    } else {
      return port.getName() + "[" + index + "]";
    }
  }

  /**
   * Implements the 'inner ABI'.
   * Splitting the line into two prevents self-propagation for bi-directional ports.
   */
  public String getPinDlcSymbol(PortDescription port, int index, boolean output) {
    return (output ? "o." : "x.") + getPinBlifSymbol(port, index);
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
    // i. : Internal input; bus(x, o); for use by gates inside the circuit.
    // o. : External output; what this circuit sends to the outside world.
    //      Does not include x or i.
    // The integration layer is expected to write to x. cells and read from o. cells.
    builder.symbolTable.put("i.$false", DenseLogicCircuit.LEV_LOW);
    builder.symbolTable.put("o.$false", DenseLogicCircuit.LEV_LOW);
    builder.symbolTable.put("i.$true", DenseLogicCircuit.LEV_HIGH);
    builder.symbolTable.put("o.$true", DenseLogicCircuit.LEV_HIGH);
    builder.symbolTable.put("i.$undef", DenseLogicCircuit.LEV_NONE);
    builder.symbolTable.put("o.$undef", DenseLogicCircuit.LEV_NONE);
    // prepare inputs & outputs
    for (PortDescription pd : inputs) {
      compilePort(builder, pd);
    }
    for (PortDescription pd : outputs) {
      compilePort(builder, pd);
    }
    // install gates
    for (Gate g : gates) {
      int a = compileGetInput(builder, g.a);
      int b = compileGetInput(builder, g.b);
      int y = compileGetOutput(builder, g.y);
      builder.attachGate(g.type, a, b, y);
    }
    // install muxes
    for (Mux m : muxes) {
      // Muxes are interesting.
      // We don't natively support them (they would be 3-input).
      // But we do have the building blocks to implement them in a better way.
      // The TRIS and TRISI gates are attached.
      // A BUS gate is automatically added by the builder.
      // This creates a 3-gate mux which also handles tri-state inputs.
      // It would also be possible to create a regular 3-gate mux:
      // i.e. Y = (B & S) | (A & !S); this would use the ANDNOT gate. Again: no tri-state.
      int a = compileGetInput(builder, m.a);
      int b = compileGetInput(builder, m.b);
      int s = compileGetInput(builder, m.s);
      int y = compileGetOutput(builder, m.y);
      builder.attachGate(DenseLogicCircuit.GATE_TRISI, a, s, y);
      builder.attachGate(DenseLogicCircuit.GATE_TRIS, b, s, y);
    }
    // install DFFs
    for (Dff ff : dff) {
      int c = compileGetInput(builder, ff.c);
      int d = compileGetInput(builder, ff.d);
      int q = compileGetOutput(builder, ff.q);
      builder.attachBuffer(builder.addDff(c, d), q);
    }
    // install latches
    for (Latch l : latches) {
      int d = compileGetInput(builder, l.d);
      int e = compileGetInput(builder, l.e);
      int q = compileGetOutput(builder, l.q);
      builder.attachBuffer(builder.addLatch(d, e), q);
    }
    for (Dffsr ff : dffsr) {
      int c = compileGetInput(builder, ff.c);
      int d = compileGetInput(builder, ff.d);
      int q = compileGetOutput(builder, ff.q);
      int s = compileGetInput(builder, ff.s);
      int r = compileGetInput(builder, ff.r);
      builder.attachBuffer(builder.addDffsr(c, d, s, r), q);
    }
    // install pullups/pulldowns
    for (String s : pullups) {
      builder.setCellPull(compileGetOutput(builder, s), DenseLogicCircuit.LEV_HIGH);
    }
    for (String s : pulldowns) {
      builder.setCellPull(compileGetOutput(builder, s), DenseLogicCircuit.LEV_LOW);
    }
    // done!
    return builder.build();
  }

  /**
   * Creates three cells: external input, internal input, external output.
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
    for (int i = 0; i < width; i++) {
      compileBidiPin(builder, getPinBlifSymbol(port, i));
    }
  }

  private int compileGetInput(DenseLogicCircuitBuilder builder, String pin) {
    Integer existing = builder.symbolTable.get("i." + pin);
    if (existing == null) {
      existing = compileNewWire(builder, pin);
    }
    return existing;
  }

  private int compileGetOutput(DenseLogicCircuitBuilder builder, String pin) {
    Integer existing = builder.symbolTable.get("o." + pin);
    if (existing == null) {
      existing = compileNewWire(builder, pin);
    }
    return existing;
  }

  private int compileNewWire(DenseLogicCircuitBuilder builder, String pin) {
    int res = builder.addCell(false);
    builder.symbolTable.put("i." + pin, res);
    builder.symbolTable.put("o." + pin, res);
    return res;
  }

  private record Gate(int type, String a, String b, String y) {
  }

  private record Mux(String a, String b, String s, String y) {
  }

  private record Dff(String c, String d, String q) {
  }

  private record Dffsr(String c, String d, String q, String s, String r) {
  }

  private record Latch(String d, String e, String q) {
  }
}
