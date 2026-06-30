/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Value;

public class ClockedMemState extends MemState {

  private ClockState clockState;

  ClockedMemState(MemContents contents) {
    super(contents);
    this.clockState = new ClockState();
  }

  @Override
  public ClockedMemState clone() {
    ClockedMemState ret = (ClockedMemState) super.clone();
    ret.clockState = this.clockState.clone();
    return ret;
  }

  public boolean setClock(Value newClock, Object trigger) {
    return clockState.updateClock(newClock, trigger);
  }
}
