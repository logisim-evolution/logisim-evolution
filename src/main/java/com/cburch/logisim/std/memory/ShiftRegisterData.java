/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.prefs.AppPreferences;
import java.util.Arrays;

class ShiftRegisterData extends ClockState implements InstanceData {
  private BitWidth width;
  private Value[] vs;
  private int vsPos;

  public ShiftRegisterData(BitWidth width, int len) {
    this.width = width;
    this.vs = new Value[len];
    Arrays.fill(
        this.vs,
        (AppPreferences.Memory_Startup_Unknown.get())
            ? Value.createUnknown(width)
            : Value.createKnown(width, 0));
    this.vsPos = 0;
  }

  public void clear() {
    Arrays.fill(vs, Value.createKnown(width, 0));
    vsPos = 0;
  }

  @Override
  public ShiftRegisterData clone() {
    final var ret = (ShiftRegisterData) super.clone();
    ret.vs = this.vs.clone();
    return ret;
  }

  public Value get(int index) {
    var i = vsPos + index;
    final var v = vs;
    if (i >= v.length) i -= v.length;
    return v[i];
  }

  public int getLength() {
    return vs.length;
  }

  public void push(Value v) {
    final var pos = vsPos;
    vs[pos] = v;
    vsPos = pos >= vs.length - 1 ? 0 : pos + 1;
  }

  public void set(int index, Value val) {
    var i = vsPos + index;
    final var v = vs;
    if (i >= v.length) i -= v.length;
    v[i] = val;
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
