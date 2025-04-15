/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import java.util.Map;

import com.cburch.logisim.data.Value;

/**
 * A stored dense logic circuit.
 * The state of the circuit is made up of byte 'cells'.
 */
public final class DenseLogicCircuit {
  // Levels. These are intended to be OR'd by GATE_BUS.
  public static final int LEV_NONE = 0;
  public static final int LEV_LOW = 1;
  public static final int LEV_HIGH = 2;
  public static final int LEV_ERR = 3;
  public static final int LEV_COUNT = 4;
  /**
   * Translates DenseLogicCircuit levels to Logisim values.
   */
  public static final Value[] LEV_TO_LS = {Value.UNKNOWN, Value.FALSE, Value.TRUE, Value.ERROR};

  // Gate types
  /**
   * 'Bus merge operator': Joins A and B onto the bus.
   */
  public static final int GATE_BUS = 0;
  /**
   * Tri-state: If B is HIGH, then A is forwarded; else NONE
   */
  public static final int GATE_TRIS = 1;
  /**
   * GATE_TRIS with inverted B input.
   * This was added to simplify 2-input muxes.
   * Doing this to D-flipflops would break counters.
   * I'm not strictly sure latches won't break either, so I don't believe too much engineering effort is justified here.
   */
  public static final int GATE_TRISI = 2;
  public static final int GATE_AND = 3;
  public static final int GATE_OR = 4;
  public static final int GATE_XOR = 5;
  public static final int GATE_NAND = 6;
  public static final int GATE_NOR = 7;
  public static final int GATE_NXOR = 8;
  public static final int GATE_ANDNOT = 9;
  public static final int GATE_ORNOT = 10;
  /**
   * These are used both for debugging and also by BlifParser.
   */
  public static final String[] GATE_TYPE_NAMES = {
      "BUS", "TRIS", "TRISI", "AND",
      "OR", "XOR", "NAND", "NOR",
      "NXOR", "ANDNOT", "ORNOT"};

  /**
   * D-flipflop[D, C, Q, X]: When cell[C] goes to high, set cell[Q] to cell[D]. Uses seqData[X] to detect this change.
   * Notably, DFF scripts may reuse resources to build more complex flip-flops that all manipulate the same bit of memory.
   * The flip-flop is coerced at all times to be either high or low to prevent errors spreading.
   */
  public static final int SQOP_DFF = 0;
  /**
   * D-latch[D, E, Q]: Whenever cell[E] is high, set cell[Q] to cell[D].
   * When reading from cell[D], it is coerced to be either high or low. 
   */
  public static final int SQOP_LATCH = 1;

  /**
   * The amount of cells in the circuit.
   * Each cell has an update list.
   * The first LEV_COUNT cells are reserved as constants.
   */
  public final int cellCount;

  /**
   * The amount of combinatorial gates in the circuit.
   */
  public final int gateCount;

  /**
   * Cell pull values.
   * These are also used as the initial values of each cell.
   * They are attached to the cell, and not the gate, because pullups/pulldowns must execute after all the bus nodes.
   * These include bus nodes that may get added after the pullup/pulldown is added.
   */
  private final byte[] cellPull;
  /**
   * The size of the update queue section of auxData / start of sequential logic data.
   */
  private final int updateQueueSize;
  /**
   * The amount of auxillary data (contains the update queue and sequential logic data).
   */
  private final int auxDataSize;
  /**
   * The amount and types of combinatorial gates.
   */
  private final byte[] gateTypes;
  /**
   * Combinatorial gate input A.
   */
  private final int[] gateCellA;
  /**
   * Combinatorial gate input B.
   */
  private final int[] gateCellB;
  /**
   * Combinatorial gate output cell.
   */
  private final int[] gateCellO;
  /**
   * Cell updates notify these gates.
   */
  private final int[][] cellUpdateNotifiesGate;
  /**
   * Sequential script.
   */
  private final int[] sequentialScript;
  /**
   * "Symbol table" of cells.
   * Doesn't necessarily cover all cells.
   */
  public final Map<String, Integer> symbolTable;

  public DenseLogicCircuit(byte[] cellPull, int[][] cellUpdateNotifiesGate, byte[] gateTypes, int[] gateCellA, int[] gateCellB, int[] gateCellO, int[] seq, int seqDataSize, Map<String, Integer> symbolTable) {
    this.cellCount = cellPull.length;
    this.cellPull = cellPull;
    this.gateCount = gateTypes.length;
    this.updateQueueSize = gateCount + 1;
    this.auxDataSize = updateQueueSize + seqDataSize;
    this.cellUpdateNotifiesGate = cellUpdateNotifiesGate;
    this.gateTypes = gateTypes;
    this.gateCellA = gateCellA;
    this.gateCellB = gateCellB;
    this.gateCellO = gateCellO;
    this.sequentialScript = seq;
    this.symbolTable = symbolTable;
  }

  /**
   * Simulates a tick of the dense logic circuit.
   */
  public final void simulate(byte[] cells, int[] auxData) {
    simulateCombinatorial(cells, auxData);
    simulateSequential(cells, auxData);
    simulateCombinatorial(cells, auxData);
  }

  /**
   * Creates a newly initialized cell array for a new simulation.
   */
  public final byte[] newCells() {
    return cellPull.clone();
  }

  /**
   * Creates a newly initialized aux. data array for a new simulation.
   */
  public final int[] newAuxData() {
    int[] dat = new int[auxDataSize];
    if (gateCount != 0) {
      // setup the initial queue
      // each gate is followed by next gate
      for (int i = 0; i < gateCount - 1; i++)
        dat[i] = i + 1;
      // last gate ends list
      dat[gateCount - 1] = -1;
      // list starts with first gate
      dat[gateCount] = 0;
    } else {
      // there are no gates, so the update queue must be empty
      dat[gateCount] = -1;
    }
    return dat;
  }

  /**
   * Updates a cell.
   */
  public final void setCell(int cell, byte value, byte[] cells, int[] auxData) {
    if (cells[cell] == value)
      return;
    cells[cell] = value;
    for (int g : cellUpdateNotifiesGate[cell]) {
      if (auxData[g] == -1) {
        auxData[g] = auxData[gateCount];
        auxData[gateCount] = g;
      }
    }
  }

  /**
   * Simulates the combinatorial part of a tick.
   * This has to be run twice; once to get data into the sequential components, once to get it out.
   */
  private final void simulateCombinatorial(byte[] cells, int[] auxData) {
    // Maximum iteration count of the amount of gates multiplied by itself.
    // This is a pessimistic count; it shouldn't be needed in most cases.
    // The main problem is A = !A.
    long maxIterationCount = (long) gateCount;
    maxIterationCount *= maxIterationCount;
    while (maxIterationCount > 0) {
      maxIterationCount--;
      int gateToEval = auxData[gateCount];
      if (gateToEval == -1)
        break;
      // update next gate register & clear this gate
      auxData[gateCount] = auxData[gateToEval];
      auxData[gateToEval] = -1;
      // run gate
      int vA = cells[gateCellA[gateToEval]];
      int vB = cells[gateCellB[gateToEval]];
      int vO = LEV_NONE;
      switch (gateTypes[gateToEval]) {
      case GATE_BUS:
        vO = vA | vB;
        break;
      case GATE_TRIS:
        if (vB == LEV_HIGH)
          vO = vA;
        break;
      case GATE_TRISI:
        if (vB != LEV_HIGH)
          vO = vA;
        break;
      case GATE_AND:
        vO = (vA == LEV_HIGH && vB == LEV_HIGH) ? LEV_HIGH : LEV_LOW;
        break;
      case GATE_OR:
        vO = (vA == LEV_HIGH || vB == LEV_HIGH) ? LEV_HIGH : LEV_LOW;
        break;
      case GATE_XOR:
        vO = (vA == LEV_HIGH ^ vB == LEV_HIGH) ? LEV_HIGH : LEV_LOW;
        break;
      case GATE_NAND:
        vO = (vA == LEV_HIGH && vB == LEV_HIGH) ? LEV_LOW : LEV_HIGH; // inverted output
        break;
      case GATE_NOR:
        vO = (vA == LEV_HIGH || vB == LEV_HIGH) ? LEV_LOW : LEV_HIGH; // inverted output
        break;
      case GATE_NXOR:
        vO = (vA == LEV_HIGH ^ vB == LEV_HIGH) ? LEV_LOW : LEV_HIGH; // inverted output
        break;
      case GATE_ANDNOT:
        vO = (vA == LEV_HIGH && vB != LEV_HIGH) ? LEV_HIGH : LEV_LOW;
        break;
      case GATE_ORNOT:
        vO = (vA == LEV_HIGH || vB != LEV_HIGH) ? LEV_HIGH : LEV_LOW;
        break;
      }
      int outCell = gateCellO[gateToEval];
      if (vO == LEV_NONE)
        vO = cellPull[outCell];
      if (vO != cells[outCell]) {
        cells[outCell] = (byte) vO;
        // inlined: markCellUpdate
        for (int g : cellUpdateNotifiesGate[outCell]) {
          if (auxData[g] == -1) {
            auxData[g] = auxData[gateCount];
            auxData[gateCount] = g;
          }
        }
      }
    }
  }

  /**
   * Simulates the sequential part of a tick.
   */
  private final void simulateSequential(byte[] cells, int[] auxData) {
    int ptr = 0;
    while (ptr < sequentialScript.length) {
      switch (sequentialScript[ptr++]) {
      case SQOP_DFF: {
        int d = sequentialScript[ptr++];
        int c = sequentialScript[ptr++];
        int q = sequentialScript[ptr++];
        // notice the 'relocation' of x to past the update queue
        int x = updateQueueSize + sequentialScript[ptr++];
        int xOld = auxData[x];
        int xNew = cells[c];
        auxData[x] = xNew;
        int newValueSrc = q;
        if (xNew == LEV_HIGH && xNew != xOld)
          newValueSrc = d;
        byte newValue = (byte) (cells[newValueSrc] == LEV_HIGH ? LEV_HIGH : LEV_LOW);
        if (cells[q] != newValue) {
          cells[q] = newValue;
          // inlined: markCellUpdate
          for (int g : cellUpdateNotifiesGate[q]) {
            if (auxData[g] == -1) {
              auxData[g] = auxData[gateCount];
              auxData[gateCount] = g;
            }
          }
        }
      } break;
      case SQOP_LATCH: {
        int d = sequentialScript[ptr++];
        int e = sequentialScript[ptr++];
        int q = sequentialScript[ptr++];
        if (cells[e] == LEV_HIGH) {
          byte newValue = (byte) (cells[d] == LEV_HIGH ? LEV_HIGH : LEV_LOW);
          if (cells[q] != newValue) {
            cells[q] = newValue;
            // inlined: markCellUpdate
            for (int g : cellUpdateNotifiesGate[q]) {
              if (auxData[g] == -1) {
                auxData[g] = auxData[gateCount];
                auxData[gateCount] = g;
              }
            }
          }
        }
      } break;
      }
    }
  }
}
