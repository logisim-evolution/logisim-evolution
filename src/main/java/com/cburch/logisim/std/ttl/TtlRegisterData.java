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

public class TtlRegisterData extends ClockState implements InstanceData {

  private final Value[] values;
  private final BitWidth bits;

  /**
   * Creates a new instance of TtlRegisterData which
   *
   * @param width the number of bits in a single data word
   * @param depth the number of data words
   */
  public TtlRegisterData(BitWidth width, int depth) {
    values = new Value[depth];
    Arrays.fill(values, (AppPreferences.Memory_Startup_Unknown.get())
                        ? Value.createUnknown(width)
                        : Value.createKnown(width, 0));

    bits = width;
  }

  public TtlRegisterData(BitWidth width) {
    this(width, 1);
  }

  public void setValue(int i, Value value) {
    this.values[i] = value;
  }

  public void setValue(Value value) {
    setValue(0, value);
  }

  public Value getValue(int i) {
    return values[i];
  }

  public Value getValue() {
    return getValue(0);
  }

  public BitWidth getWidth() {
    return bits;
  }
}
