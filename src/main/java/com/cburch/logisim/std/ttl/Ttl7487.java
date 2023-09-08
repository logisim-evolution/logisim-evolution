/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/*
 * Class simulates a TTL7487 IC
 * It takes a 4 bit input (A1-A4) and performs one of the following oppertations based on inputs B and c to produce outputs Y1-Y4
 *   B=0, C=0 output complement of A
 *   B=0, C=1 output A unchanged
 *   B=1, C=0 output all High
 *   B=1, C=1 output all Low
 * Likely use is before an adder to give control for addition/subtraction along with increment/decrement/transparent in conjunction with the adder's carry in bit
 * Datasheet: https://archive.org/details/bitsavers_tidataBookVol2_45945352/page/n404/mode/1up?view=theater
 * Alternative source (page 3-354, pdf page 405): http://bitsavers.org/components/ti/_dataBooks/1985_TI_The_TTL_Data_Book_Vol_2.pdf
 */
public class Ttl7487 extends AbstractTtlGate {
  /**
  * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
  * prevent project files from loading.
  *
  * <p>Identifier value must MUST be unique string among all tools.
  */
  public static final String _ID = "7487";
  
  //input data (port number)
  public static final byte A1 = 1;
  public static final byte A2 = 3;
  public static final byte A3 = 7;
  public static final byte A4 = 9;
  //input control (port number)
  public static final byte B = 5;
  public static final byte C = 0;
  
  //output (port number)
  public static final byte Y1 = 2;
  public static final byte Y2 = 4;
  public static final byte Y3 = 6;
  public static final byte Y4 = 8;
  
  //No Connect (datasheet pins)
  public static final byte NC1 = 4;
  public static final byte NC2 = 11;
  
  public static final byte DELAY = 1;
  
  public Ttl7487() {
    super(
        _ID,
        (byte) 14,
        new byte[] {3, 6, 9, 12},
        new byte[] {NC1, NC2},
        new String[] {"C", "A1", "Y1", "A2", "Y2", "B", "Y3", "A3", "Y4", "A4"},
        null
    );
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portNames);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    if (state.getPortValue(B) == Value.TRUE) {
      //High or low output
      state.setPort(Y1, state.getPortValue(C).not(), DELAY);
      state.setPort(Y2, state.getPortValue(C).not(), DELAY);
      state.setPort(Y3, state.getPortValue(C).not(), DELAY);
      state.setPort(Y4, state.getPortValue(C).not(), DELAY);
    } else if (state.getPortValue(B) == Value.FALSE) {
      if (state.getPortValue(C) == Value.TRUE) {
        //not inverted
        state.setPort(Y1, state.getPortValue(A1), DELAY);
        state.setPort(Y2, state.getPortValue(A2), DELAY);
        state.setPort(Y3, state.getPortValue(A3), DELAY);
        state.setPort(Y4, state.getPortValue(A4), DELAY);
      } else if (state.getPortValue(C) == Value.FALSE) {
        //inverted
        state.setPort(Y1, state.getPortValue(A1).not(), DELAY);
        state.setPort(Y2, state.getPortValue(A2).not(), DELAY);
        state.setPort(Y3, state.getPortValue(A3).not(), DELAY);
        state.setPort(Y4, state.getPortValue(A4).not(), DELAY);
      } else {
        state.setPort(Y1, Value.ERROR, DELAY);
        state.setPort(Y2, Value.ERROR, DELAY);
        state.setPort(Y3, Value.ERROR, DELAY);
        state.setPort(Y4, Value.ERROR, DELAY);
      }
    } else {
      state.setPort(Y1, Value.ERROR, DELAY);
      state.setPort(Y2, Value.ERROR, DELAY);
      state.setPort(Y3, Value.ERROR, DELAY);
      state.setPort(Y4, Value.ERROR, DELAY);
    }
    
  }
}
