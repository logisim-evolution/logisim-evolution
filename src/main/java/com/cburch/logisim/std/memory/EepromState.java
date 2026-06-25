/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.std.memory.Mem.MemListener;

public class EepromState extends MemState {

  private ClockState clockState;

  EepromState(MemContents contents) {
    super(contents);
    this.clockState = new ClockState();
  }

  @Override
  public EepromState clone() {
    EepromState ret = (EepromState) super.clone();
    ret.clockState = this.clockState.clone();
    return ret;
  }

  public boolean setClock(Value newClock, Object trigger) {
    return clockState.updateClock(newClock, trigger);
  }
}
