/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7447 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7447";

  public static final int PORT_INDEX_B = 0;
  public static final int PORT_INDEX_C = 1;
  public static final int PORT_INDEX_LT = 2;
  public static final int PORT_INDEX_BI = 3;
  public static final int PORT_INDEX_RBI = 4;
  public static final int PORT_INDEX_D = 5;
  public static final int PORT_INDEX_A = 6;
  public static final int PORT_INDEX_QE = 7;
  public static final int PORT_INDEX_QD = 8;
  public static final int PORT_INDEX_QC = 9;
  public static final int PORT_INDEX_QB = 10;
  public static final int PORT_INDEX_QA = 11;
  public static final int PORT_INDEX_QG = 12;
  public static final int PORT_INDEX_QF = 13;

  public Ttl7447() {
    super(
        _ID,
        (byte) 16,
        new byte[] {9, 10, 11, 12, 13, 14, 15},
        new String[] {"B", "C", "LT", "BI", "RBI", "D", "A", "e", "d", "c", "b", "a", "g", "f"},
        new Ttl7447HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portNames);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    DisplayDecoder.computeDisplayDecoderOutputs(
        state,
        DisplayDecoder.getdecval(state, false, 0, PORT_INDEX_A, PORT_INDEX_B, PORT_INDEX_C, PORT_INDEX_D),
        PORT_INDEX_QA,
        PORT_INDEX_QB,
        PORT_INDEX_QC,
        PORT_INDEX_QD,
        PORT_INDEX_QE,
        PORT_INDEX_QF,
        PORT_INDEX_QG,
        PORT_INDEX_LT,
        PORT_INDEX_BI,
        PORT_INDEX_RBI);
  }
}
