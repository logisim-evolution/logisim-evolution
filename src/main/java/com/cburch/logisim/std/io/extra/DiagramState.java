/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import java.util.Arrays;

class DiagramState implements InstanceData {
  // the array cell where to save the actual input value
  private byte usedcell = -1;
  private Value LastClock;
  private boolean moveback = false;
  private Boolean[][] diagram;
  private byte Inputs, Length; // current inputs and length (number of states)
  private byte clocknumber;

  public DiagramState(byte inputs, byte length) {
    LastClock = Value.UNKNOWN;
    diagram = new Boolean[inputs][length];
    clear();
    Inputs = inputs;
    Length = length;
    clocknumber = (byte) (Length / 2);
  }

  public void clear() { // set all to false
    for (byte i = 0; i < Inputs; i++) {
      for (byte j = 0; j < Length; j++) {
        diagram[i][j] = null;
      }
    }
    moveback = false;
  }

  @Override
  public DiagramState clone() {
    try {
      return (DiagramState) super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public byte getclocknumber() {
    return clocknumber;
  }

  public boolean getmoveback() {
    return moveback;
  }

  public Boolean getState(int i, int j) {
    return diagram[i][j];
  }

  public byte getusedcell() {
    return usedcell;
  }

  public void hastomoveback(boolean b) {
    moveback = b;
  }

  public void moveback() { // move back all old values
    if (Length >= 1)
      for (byte i = 0; i < Inputs; i++) {
        System.arraycopy(diagram[i], 1, diagram[i], 0, Length - 1);
      }
  }

  public void setclocknumber(byte i) {
    clocknumber = i < 100 ? i : 1;
  }

  public Value setLastClock(Value newClock) {
    // copy, set and return copy
    Value ret = LastClock;
    LastClock = newClock;
    return ret;
  }

  public void setState(byte i, byte j, Boolean b) {
    diagram[i][j] = b;
  }

  public void setusedcell(byte i) {
    usedcell = i;
  }

  public void updateSize(byte inputs, byte length) {
    // if it's not the same size
    if (inputs != Inputs || length != Length) {
      byte oldinputs = Inputs;
      byte oldlength = Length;
      // update current inputs and length to not go out of array bouds in
      // clear() function
      Inputs = inputs;
      Length = length;
      clocknumber = (byte) (clocknumber + (Length - oldlength) / 2);
      // create a copy of old boolean matrix
      Boolean[][] olddiagram = Arrays.copyOf(diagram, diagram.length);
      diagram = new Boolean[Inputs][Length];
      // set all to false
      clear();
      if (usedcell < Length - 1) {
        // set old values in new boolean matrix
        for (byte i = 0; i < Inputs && i < oldinputs; i++) {
          for (byte j = 0; j < Length && j < oldlength; j++) {
            diagram[i][j] = olddiagram[i][j];
          }
        }
        moveback = false;
      } else {
        int h;
        // set old values in new boolean matrix
        for (int i = 0; i < Inputs && i < oldinputs; i++) {
          h = oldlength - 1;
          for (byte j = (byte) (Length - 1); j >= 0 && h >= 0; j--) {
            diagram[i][j] = olddiagram[i][h - (oldlength - usedcell - 1)];
            h--;
          }
        }
        usedcell = (byte) (Length - 1);
        moveback = true;
      }
    }
  }
}
