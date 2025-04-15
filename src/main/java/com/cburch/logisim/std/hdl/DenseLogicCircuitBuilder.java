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
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * Builds a dense logic circuit.
 */
public final class DenseLogicCircuitBuilder {
  private ArrayList<GateInfo> gates = new ArrayList<>();
  private ArrayList<CellInfo> cells = new ArrayList<>();
  private ArrayList<Integer> seqScript = new ArrayList<>();
  public final HashMap<String, Integer> symbolTable = new HashMap<>();
  private int seqDataSize = 0;

  public DenseLogicCircuitBuilder() {
    // Create constant cells.
    for (int i = 0; i < DenseLogicCircuit.LEV_COUNT; i++)
      cells.add(new CellInfo(i, true));
  }

  /**
   * Attaches a gate to the system.
   * If a gate is already attached to the given pin, a bus gate is automatically inserted.
   */
  public void attachGate(int gateType, int a, int b, int o) {
    CellInfo outCellInfo = cells.get(o);
    GateInfo existingGate = outCellInfo.currentDrivingGate;
    if (existingGate != null) {
      // there is already a driving gate!
      // setup intermediate cells and redirect.
      CellInfo existingGateOutputCell = addCellInternal(false);
      existingGate.setOutput(existingGateOutputCell);
      CellInfo incomingGateOutputCell = addCellInternal(false);
      addGate(gateType, cells.get(a), cells.get(b)).setOutput(incomingGateOutputCell);
      // add in the bus gate
      addGate(DenseLogicCircuit.GATE_BUS, existingGateOutputCell, incomingGateOutputCell).setOutput(outCellInfo);
    } else {
      addGate(gateType, cells.get(a), cells.get(b)).setOutput(outCellInfo);
    }
  }

  /**
   * Builds the dense logic circuit.
   */
  public DenseLogicCircuit build() {
    // notification array
    int[][] cellUpdateNotifiesGate = new int[cells.size()][];
    for (int i = 0; i < cellUpdateNotifiesGate.length; i++) {
      CellInfo cell = cells.get(i);
      int[] notifyArray = new int[cell.notifiesTheseGates.size()];
      int idx = 0;
      for (Integer gate : cell.notifiesTheseGates)
        notifyArray[idx++] = gate;
      cellUpdateNotifiesGate[i] = notifyArray;
    }
    // assemble gates
    byte[] gateTypes = new byte[gates.size()];
    int[] gateCellA = new int[gateTypes.length];
    int[] gateCellB = new int[gateTypes.length];
    int[] gateCellO = new int[gateTypes.length];
    for (int i = 0; i < gateTypes.length; i++) {
      GateInfo gi = gates.get(i);
      gateTypes[i] = (byte) gi.type;
      gateCellA[i] = gi.pinA.index;
      gateCellB[i] = gi.pinB.index;
      gateCellO[i] = gi.pinOutput.index;
    }
    // copy seq script
    int[] seq = new int[seqScript.size()];
    for (int i = 0; i < seq.length; i++)
      seq[i] = seqScript.get(i);
    // output
    return new DenseLogicCircuit(cellUpdateNotifiesGate, gateTypes, gateCellA, gateCellB, gateCellO, seq, seqDataSize, Collections.unmodifiableMap(new HashMap<>(symbolTable)));
  }

  /**
   * Adds a cell to the system.
   */
  private CellInfo addCellInternal(boolean ext) {
    int idx = cells.size();
    CellInfo ci = new CellInfo(idx, ext);
    cells.add(ci);
    return ci;
  }

  /**
   * Adds a cell to the system.
   */
  public int addCell(boolean ext) {
    return addCellInternal(ext).index;
  }

  /**
   * Adds a gate to the system.
   */
  private GateInfo addGate(int type, CellInfo a, CellInfo b) {
    int gateIndex = gates.size();
    GateInfo gi = new GateInfo(gateIndex, type, a, b);
    gates.add(gi);
    return gi;
  }

  /**
   * Adds a D-flipflop. Returns the Q line cell.
   */
  public int addDFF(int d, int c) {
    int q = addCellInternal(true).index;
    int x = seqDataSize++;
    seqScript.add(DenseLogicCircuit.SQOP_DFF);
    seqScript.add(d);
    seqScript.add(c);
    seqScript.add(q);
    seqScript.add(x);
    return q;
  }

  private class CellInfo {
    /**
     * Records the index.
     */
    final int index;

    /**
     * Indicates the current driving gate of this cell.
     * This is used to rewrite gates into GATE_BUS.
     */
    GateInfo currentDrivingGate;

    /**
     * Indicates an external driver. Trying to connect a gate or other driver to these is an error!
     */
    boolean externallyDriven;

    /**
     * These gates are notified by this cell.
     */
    TreeSet<Integer> notifiesTheseGates = new TreeSet<>();

    CellInfo(int index, boolean ext) {
      this.index = index;
      externallyDriven = ext;
    }
  }
  /**
   * Contains info about a gate.
   */
  private class GateInfo {
    final int type;
    CellInfo pinA;
    CellInfo pinB;
    CellInfo pinOutput;
    GateInfo(int index, int t, CellInfo a, CellInfo b) {
      type = t;
      pinA = a;
      a.notifiesTheseGates.add(index);
      pinB = b;
      b.notifiesTheseGates.add(index);
    }

    GateInfo setOutput(CellInfo cell) {
      // unlink
      if (pinOutput != null) {
        pinOutput.currentDrivingGate = null;
        pinOutput = null;
      }
      // link
      if (cell != null) {
        if (cell.externallyDriven)
          throw new RuntimeException("Cannot set driving gate for an externally driven cell!");
        if (cell.currentDrivingGate != null)
          throw new RuntimeException("setOutput does not insert bus gates automatically!");
        cell.currentDrivingGate = this;
      }
      pinOutput = cell;
      return this;
    }
  }
}
