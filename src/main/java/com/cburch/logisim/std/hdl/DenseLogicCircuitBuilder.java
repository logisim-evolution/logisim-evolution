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
import java.util.Map;
import java.util.TreeSet;

/**
 * Builds a dense logic circuit.
 * This inserts the logic for multiple gates attached to the same line.
 */
public final class DenseLogicCircuitBuilder {
  public static final boolean DEBUG = false;

  private ArrayList<GateInfo> gates = new ArrayList<>();
  private ArrayList<CellInfo> cells = new ArrayList<>();
  private ArrayList<Integer> seqScript = new ArrayList<>();
  public final HashMap<String, Integer> symbolTable = new HashMap<>();
  private int seqDataSize = 0;

  /**
   * Creates a circuit builder with just the constant cells.
   */
  public DenseLogicCircuitBuilder() {
    // Create constant cells.
    for (int i = 0; i < DenseLogicCircuit.LEV_COUNT; i++) {
      CellInfo ci = new CellInfo(i, true);
      ci.pull = (byte) i;
      cells.add(ci);
    }
  }

  /**
   * Attaches a gate to the system.
   * If a gate is already attached to the given pin, a bus gate is automatically inserted.
   */
  public void attachGate(int gateType, int a, int b, int o) {
    CellInfo outCellInfo = cells.get(o);
    GateInfo oldGate = outCellInfo.currentDrivingGate;
    if (oldGate != null) {
      // there is already a driving gate!
      if (oldGate.pinA == oldGate.pinB && oldGate.type == DenseLogicCircuit.GATE_BUS) {
        // opt: if the driving gate is a BUF, we can use the spare port.
        if (a == b && gateType == DenseLogicCircuit.GATE_BUS) {
          // if these are both BUFs, we can combine them.
          oldGate.setPinB(cells.get(b));
        } else {
          CellInfo newGateOut = addCellInternal(false);
          addGate(gateType, cells.get(a), cells.get(b)).setOutput(newGateOut);
          oldGate.setPinB(newGateOut);
        }
      } else {
        // setup intermediate cells and redirect.
        CellInfo oldGateOut = addCellInternal(false);
        oldGate.setOutput(oldGateOut);
        if (a == b && gateType == DenseLogicCircuit.GATE_BUS) {
          // opt: BUF fast-path for an incoming BUF on any existing gate
          addGate(DenseLogicCircuit.GATE_BUS, oldGateOut, cells.get(a)).setOutput(outCellInfo);
        } else {
          CellInfo newGateOut = addCellInternal(false);
          addGate(gateType, cells.get(a), cells.get(b)).setOutput(newGateOut);
          // add in the bus gate
          addGate(DenseLogicCircuit.GATE_BUS, oldGateOut, newGateOut).setOutput(outCellInfo);
        }
      }
    } else {
      addGate(gateType, cells.get(a), cells.get(b)).setOutput(outCellInfo);
    }
  }

  /**
   * Shorthand for attaching a BUS gate as a buffer.
   */
  public void attachBuffer(int from, int to) {
    attachGate(DenseLogicCircuit.GATE_BUS, from, from, to);
  }

  /**
   * Builds the dense logic circuit.
   */
  public DenseLogicCircuit build() {
    var inverseMap = new HashMap<Integer, String>();
    for (Map.Entry<String, Integer> ent : symbolTable.entrySet()) {
      inverseMap.put(ent.getValue(), ent.getKey());
    }
    if (DEBUG) {
      System.out.println("-- DenseLogicCircuitBuilder --");
    }
    // pull & notification array
    byte[] cellPull = new byte[cells.size()];
    int[][] cellUpdateNotifiesGate = new int[cells.size()][];
    for (int i = 0; i < cellPull.length; i++) {
      CellInfo cell = cells.get(i);
      cellPull[i] = cell.pull;
      int[] notifyArray = new int[cell.notifiesTheseGates.size()];
      int idx = 0;
      for (Integer gate : cell.notifiesTheseGates) {
        notifyArray[idx++] = gate;
      }
      cellUpdateNotifiesGate[i] = notifyArray;
      if (DEBUG) {
        String addInfo = "";
        String sym = inverseMap.get(i);
        if (sym != null) {
          addInfo = " (" + sym + ")";
        }
        System.out.print(" C" + i + addInfo + " notifies");
        for (int na : notifyArray) {
          System.out.print(" G");
          System.out.print(na);
        }
        System.out.println();
      }
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
      if (DEBUG) {
        System.out.print(" G" + i + " ");
        debugWriteCell(inverseMap, gi.pinOutput.index);
        System.out.print(" = ");
        debugWriteCell(inverseMap, gi.pinA.index);
        System.out.print(" " + DenseLogicCircuit.GATE_TYPE_NAMES[gi.type] + " ");
        debugWriteCell(inverseMap, gi.pinB.index);
        System.out.println();
      }
    }
    // copy seq script
    int[] seq = new int[seqScript.size()];
    for (int i = 0; i < seq.length; i++) {
      seq[i] = seqScript.get(i);
    }
    // output
    return new DenseLogicCircuit(cellPull, cellUpdateNotifiesGate, gateTypes,
        gateCellA, gateCellB, gateCellO, seq, seqDataSize,
        Collections.unmodifiableMap(new HashMap<>(symbolTable)));
  }

  private void debugWriteCell(Map<Integer, String> inverseMap, int i) {
    System.out.print("C");
    System.out.print(i);
    String sym = inverseMap.get(i);
    if (sym != null) {
      System.out.print(" (" + sym + ")");
    }
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
   * Sets the pullup/pulldown of a cell, used when gates evaluate to LEV_NONE.
   * This is also the initial value of the cell.
   * Note that there's only one pull per cell.
   */
  public void setCellPull(int cell, int pull) {
    cells.get(cell).pull = (byte) pull;
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
  public int addDff(int c, int d) {
    int q = addCellInternal(true).index;
    int x = seqDataSize++;
    seqScript.add(DenseLogicCircuit.SQOP_DFF);
    seqScript.add(d);
    seqScript.add(c);
    seqScript.add(q);
    seqScript.add(x);
    setCellPull(q, DenseLogicCircuit.LEV_LOW);
    return q;
  }

  /**
   * Adds a DFFSR flipflop. Returns the Q line cell.
   */
  public int addDffsr(int c, int d, int s, int r) {
    int q = addCellInternal(true).index;
    int xc = seqDataSize++;
    // C
    seqScript.add(DenseLogicCircuit.SQOP_DFF);
    seqScript.add(d);
    seqScript.add(c);
    seqScript.add(q);
    seqScript.add(xc);
    // https://en.wikipedia.org/wiki/Flip-flop_(electronics)#/media/File:Edge_triggered_D_flip_flop_with_set_and_reset.svg
    // This schematic indicates that the S/R lines are asynchronous and not edge-based.
    // Therefore, use SQOP_LATCH to directly overwrite the latch contents.
    // S
    seqScript.add(DenseLogicCircuit.SQOP_LATCH);
    seqScript.add(DenseLogicCircuit.LEV_HIGH);
    seqScript.add(s);
    seqScript.add(q);
    // R
    seqScript.add(DenseLogicCircuit.SQOP_LATCH);
    seqScript.add(DenseLogicCircuit.LEV_LOW);
    seqScript.add(r);
    seqScript.add(q);
    setCellPull(q, DenseLogicCircuit.LEV_LOW);
    return q;
  }

  /**
   * Adds a D-latch. Returns the Q line cell.
   */
  public int addLatch(int d, int e) {
    int q = addCellInternal(true).index;
    seqScript.add(DenseLogicCircuit.SQOP_LATCH);
    seqScript.add(d);
    seqScript.add(e);
    seqScript.add(q);
    setCellPull(q, DenseLogicCircuit.LEV_LOW);
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
     * Indicates pull/init.
     */
    byte pull = (byte) DenseLogicCircuit.LEV_NONE;

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
    final int index;
    CellInfo pinA;
    CellInfo pinB;
    CellInfo pinOutput;

    GateInfo(int index, int t, CellInfo a, CellInfo b) {
      this.index = index;
      type = t;
      pinA = a;
      a.notifiesTheseGates.add(index);
      pinB = b;
      b.notifiesTheseGates.add(index);
    }

    void setPinB(CellInfo b) {
      if (pinA != pinB) {
        pinB.notifiesTheseGates.remove(index);
      }
      this.pinB = b;
      pinB.notifiesTheseGates.add(index);
    }

    GateInfo setOutput(CellInfo cell) {
      // unlink
      if (pinOutput != null) {
        pinOutput.currentDrivingGate = null;
        pinOutput = null;
      }
      // link
      if (cell != null) {
        if (cell.externallyDriven) {
          throw new RuntimeException("Cannot set driving gate for an externally driven cell!");
        }
        if (cell.currentDrivingGate != null) {
          throw new RuntimeException("setOutput does not insert bus gates automatically!");
        }
        cell.currentDrivingGate = this;
      }
      pinOutput = cell;
      return this;
    }
  }
}
