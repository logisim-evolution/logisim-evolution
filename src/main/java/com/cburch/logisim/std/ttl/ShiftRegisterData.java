/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.prefs.AppPreferences;

import java.util.Arrays;

public class ShiftRegisterData extends ClockState implements InstanceData {
  private BitWidth width;
  private Value[] vs;
  private int vsPos;

  public ShiftRegisterData(BitWidth width, int len) {
    final var initialValue = AppPreferences.Memory_Startup_Unknown.get() ? Value.createUnknown(width) : Value.createKnown(width, 0);

    this.width = width;
    this.vs = new Value[len];
    Arrays.fill(this.vs, initialValue);
    this.vsPos = 0;
  }

  public void clear() {
    Arrays.fill(vs, Value.createKnown(width, 0));
    vsPos = 0;
  }

  @Override
  public ShiftRegisterData clone() {
    ShiftRegisterData ret = (ShiftRegisterData) super.clone();
    ret.vs = this.vs.clone();
    return ret;
  }

  /**
   * Convert an external index i.e. from an argument to a public method to an index
   * in the vs array
   *
   * @param index the external index
   * @return the internal index
   */
  private int toInternalIndex(int index) {
    return (vsPos + index) % vs.length;
  }

  public Value get(int index) {
    return vs[toInternalIndex(index)];
  }

  public int getLength() {
    return vs.length;
  }

  /**
   * Push the contents of the shift register towards the zero index
   * and insert the new value at the top index
   *
   * @param v the value to be pushed into the shift register
   */
  public void pushDown(Value v) {
    vs[vsPos] = v;

    vsPos = toInternalIndex(1);
  }

  /**
   * Push the contents of the shift register towards the top index
   * and insert the new value at the zero index
   *
   * @param v the value to be pushed into the shift register
   */
  public void pushUp(Value v) {
    vsPos = toInternalIndex(-1);

    vs[vsPos] = v;
  }

  public void set(int index, Value val) {
    vs[toInternalIndex(index)] = val;
  }

  public void setDimensions(BitWidth newWidth, int newLength) {
    var v = vs;
    final var oldWidth = width;
    final var oldW = oldWidth.getWidth();
    final var newW = newWidth.getWidth();
    if (v.length != newLength) {
      final var newV = new Value[newLength];
      var j = vsPos;
      final var copy = Math.min(newLength, v.length);
      for (var i = 0; i < copy; i++) {
        newV[i] = v[j];
        j++;
        if (j == v.length) j = 0;
      }
      Arrays.fill(newV, copy, newLength, Value.createKnown(newWidth, 0));
      v = newV;
      vsPos = 0;
      vs = newV;
    }
    if (oldW != newW) {
      for (var i = 0; i < v.length; i++) {
        final var vi = v[i];
        if (vi.getWidth() != newW) {
          v[i] = vi.extendWidth(newW, Value.FALSE);
        }
      }
      width = newWidth;
    }
  }
}
