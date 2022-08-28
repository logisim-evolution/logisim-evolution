/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 * 
 * This part of logisim implements (parts of) espresso:
 * 
 * Copyright (c) 1988, 1989, Regents of the University of California.
 * All rights reserved.
 *
 * Use and copying of this software and preparation of derivative works
 * based upon this software are permitted.  However, any distribution of
 * this software or derivative works must include the above copyright
 * notice.
 *
 * This software is made available AS IS, and neither the Electronics
 * Research Laboratory or the University of California make any
 * warranty about the software, its performance or its conformity to
 * any specification.
*/

package com.cburch.logisim.analyze.data;

import java.util.ArrayList;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;

public class EspressoPlaStruct {

  public class EspressoPair {
    int cnt;
    ArrayList<Integer> variable1;
    ArrayList<Integer> variable2;
  }

  public static final int PLA_F_TYPE = 1;
  public static final int PLA_D_TYPE = 2;
  public static final int PLA_R_TYPE = 4;
  public static final int PLA_FD_TYPE = PLA_F_TYPE | PLA_D_TYPE;
  public static final int PLA_FR_TYPE = PLA_F_TYPE | PLA_R_TYPE;
  public static final int PLA_DR_TYPE = PLA_D_TYPE | PLA_R_TYPE;
  public static final int PLA_FDR_TYPE = PLA_F_TYPE | PLA_D_TYPE | PLA_R_TYPE;

  private final ArrayList<ArrayList<Integer>> fSet;
  private final ArrayList<ArrayList<Integer>> dSet;
  private final ArrayList<ArrayList<Integer>> rSet;
  private final ArrayList<ArrayList<Integer>> fMin;
  private final int plaType;
  private final ArrayList<Long> phase;
  private final EspressoPair pair;
  private final EspressoCubeStruct cube;

  public EspressoPlaStruct(int format, AnalyzerModel model, EspressoCubeStruct cube) {
    this.cube = cube;
    final var truthTable = model.getTruthTable();
    final var cf = cube.newCube();
    final var nrOfInputs = truthTable.getInputColumnCount();
    plaType = PLA_FDR_TYPE;
    fSet = new ArrayList<>();
    dSet = new ArrayList<>();
    rSet = new ArrayList<>();
    fMin = new ArrayList<>();
    phase = new ArrayList<>();
    pair = new EspressoPair();
    final var desiredTerm = format == AnalyzerModel.FORMAT_SUM_OF_PRODUCTS ? Entry.ONE : Entry.ZERO;

    for (var tableIndex = 0; tableIndex < truthTable.getRowCount(); tableIndex++) {
      for (var inputIndex = 0; inputIndex < nrOfInputs; inputIndex++) {
        final var inputValue = truthTable.getInputEntry(tableIndex, inputIndex);
        if (inputValue.equals(Entry.ZERO)) {
          cube.setInsert(cf, inputIndex * 2);
        } else {
          cube.setInsert(cf, (inputIndex * 2) + 1);
        }
      }
      final var cr = cube.setCopy(cf);
      final var cd = cube.setCopy(cf);
      final var nrOfOutputs = truthTable.getOutputColumnCount();
      final var outputOffset = cube.getOutputOffset();
      var saveCf = false;
      var saveCr = false;
      var saveCd = false;
      for (var output = 0; output < nrOfOutputs; output++) {
        final var outputValue = truthTable.getOutputEntry(tableIndex, output);
        if (outputValue.equals(Entry.DONT_CARE)) {
          saveCd = true;
          cube.setInsert(cd, output + outputOffset);
        } else if (outputValue.equals(desiredTerm)) {
          saveCf = true;
          cube.setInsert(cf, output + outputOffset);
        } else {
          saveCr = true;
          cube.setInsert(cr, output + outputOffset);
        }
      }
      if (saveCf) fSet.add(cf);
      if (saveCr) rSet.add(cr);
      if (saveCd) dSet.add(cd);
    }
  }

  public ArrayList<ArrayList<Integer>> getFSet() {
    return fSet;
  }

  public ArrayList<ArrayList<Integer>> getRSet() {
    return rSet;
  }

  public ArrayList<ArrayList<Integer>> getDSet() {
    return dSet;
  }

  public ArrayList<ArrayList<Integer>> getCopy(ArrayList<ArrayList<Integer>> set) {
    final var copy = new ArrayList<ArrayList<Integer>>();
    for (final var entry : set) {
      final var newEntry = new ArrayList<Integer>();
      newEntry.addAll(entry);
      copy.add(newEntry);
    }
    return copy;
  }
}
