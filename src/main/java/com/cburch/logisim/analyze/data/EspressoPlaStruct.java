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

import java.util.List;

import com.cburch.logisim.analyze.model.AnalyzerModel;

public class EspressoPlaStruct {

  public class EspressoSetFamily {
    int wSize;
    int sfSize;
    int capacity;
    int count;
    int activeCount;
    List<Long> data;
  }

  public class EspressoPair {
    int cnt;
    List<Long> variable1;
    List<Long> variable2;
  }

  public static final int PLA_F_TYPE = 1;
  public static final int PLA_D_TYPE = 2;
  public static final int PLA_R_TYPE = 4;
  public static final int PLA_FD_TYPE = PLA_F_TYPE | PLA_D_TYPE;
  public static final int PLA_FR_TYPE = PLA_F_TYPE | PLA_R_TYPE;
  public static final int PLA_DR_TYPE = PLA_D_TYPE | PLA_R_TYPE;
  public static final int PLA_FDR_TYPE = PLA_F_TYPE | PLA_D_TYPE | PLA_R_TYPE;

  private final EspressoSetFamily fSet;
  private final EspressoSetFamily dSet;
  private final EspressoSetFamily rSet;
  private final int plaType;
  private final List<Long> phase;
  private final EspressoPair pair;
  private final EspressoCubeStruct cube;

  public EspressoPlaStruct(int format, AnalyzerModel model, EspressoCubeStruct cube) {
    this.cube = cube;
    final var truthTable = model.getTruthTable();
    final var cf = cube.getTemp(0);
    final var cr = cube.getTemp(1);
    final var cd = cube.getTemp(2);
    cube.setClear(cf, cube.getSize());
    final var nrOfInputs = truthTable.getInputColumnCount();
    plaType = PLA_FDR_TYPE;
  }
}
