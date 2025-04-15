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

  // Gate types
  // 'Bus merge operator': Joins A and B onto the bus.
  public static final int GATE_BUS = 0;
  // Tri-state: If B is HIGH, then A is forwarded; else NONE
  public static final int GATE_TRIS = 1;
  public static final int GATE_AND = 2;
  public static final int GATE_OR = 3;
  public static final int GATE_XOR = 4;
  public static final int GATE_NAND = 5;
  public static final int GATE_NOR = 6;
  public static final int GATE_NXOR = 7;
  // Pullup/pulldown: Iff A = LEV_NONE then it is set to B.
  public static final int GATE_PULL = 8;

  /**
   * D-flipflop[D, C, Q, X]: When cell[C] goes to high, set cell[Q] to cell[D]. Uses seqData[X] to detect this change. 
   */
  public static final int SQOP_DFF = 0;

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
   * The size of the update queue section of auxData / start of sequential logic data.
   */
  public final int updateQueueSize;
  /**
   * The amount of auxillary data (contains the update queue and sequential logic data).
   */
  public final int auxDataSize;
  /**
   * The amount and types of combinatorial gates.
   */
  public final byte[] gateTypes;
  /**
   * Combinatorial gate input A.
   */
  public final int[] gateCellA;
  /**
   * Combinatorial gate input B.
   */
  public final int[] gateCellB;
  /**
   * Combinatorial gate output cell.
   */
  public final int[] gateCellO;
  /**
   * Cell updates notify these gates.
   */
  public final int[][] cellUpdateNotifiesGate;
  /**
   * Sequential script.
   */
  public final int[] sequentialScript;
  /**
   * "Symbol table" of cells.
   * Doesn't necessarily cover all cells.
   */
  public final Map<String, Integer> symbolTable;

  public DenseLogicCircuit(int[][] cellUpdateNotifiesGate, byte[] gateTypes, int[] gateCellA, int[] gateCellB, int[] gateCellO, int[] seq, int seqDataSize, Map<String, Integer> symbolTable) {
    this.cellCount = cellUpdateNotifiesGate.length;
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
    byte[] dat = new byte[cellCount];
    for (int i = 0; i < LEV_COUNT; i++)
      dat[i] = (byte) i;
    return dat;
  }

  /**
   * Creates a newly initialized aux. data array for a new simulation.
   */
  public final int[] newAuxData() {
    int[] dat = new int[auxDataSize];
    // setup the initial queue
    // each gate is followed by next gate
    for (int i = 0; i < gateCount - 1; i++)
      dat[i] = i + 1;
    // last gate ends list
    dat[gateCount - 1] = -1;
    // list starts with first gate
    dat[gateCount] = 0;
    return dat;
  }

  /**
   * Marks a cell update in the given auxData.
   */
  public final void markCellUpdate(int cell, int[] auxData) {
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
    // This is a pessimistic count, it shouldn't be needed in most cases.
    // The main problem is A = !A.
    long maxIterationCount = (long) gateCount;
    maxIterationCount *= maxIterationCount;
    for (long i = 0; i < maxIterationCount; i++) {
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
        vO = (vA == LEV_HIGH && vB == LEV_HIGH) ? LEV_LOW : LEV_HIGH;
        break;
      case GATE_NOR:
        vO = (vA == LEV_HIGH || vB == LEV_HIGH) ? LEV_LOW : LEV_HIGH;
        break;
      case GATE_NXOR:
        vO = (vA == LEV_HIGH ^ vB == LEV_HIGH) ? LEV_LOW : LEV_HIGH;
        break;
      case GATE_PULL:
        vO = vA == LEV_NONE ? vB : vA;
        break;
      }
      int outCell = gateCellO[gateToEval];
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
        if (xNew == LEV_HIGH && xNew != xOld) {
          cells[q] = cells[d];
          // inlined: markCellUpdate
          for (int g : cellUpdateNotifiesGate[q]) {
            if (auxData[g] == -1) {
              auxData[g] = auxData[gateCount];
              auxData[gateCount] = g;
            }
          }
        }
      } break;
      }
    }
  }
}
