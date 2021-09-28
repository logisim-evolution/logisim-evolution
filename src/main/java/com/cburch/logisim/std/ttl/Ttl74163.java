/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class Ttl74163 extends Ttl74161 {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74163";

  public Ttl74163() {
    super(_ID);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = getStateData(state);

    final var triggered = data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING);
    var counter = data.getValue().toLongValue();
    if (triggered) {
      final var nClear = state.getPortValue(PORT_INDEX_nCLR).toLongValue();
      final var nLoad = state.getPortValue(PORT_INDEX_nLOAD).toLongValue();
      if (nClear == 0) {
        counter = 0;
      } else if (nLoad == 0) {
        counter = state.getPortValue(PORT_INDEX_A).toLongValue()
            + (state.getPortValue(PORT_INDEX_B).toLongValue() << 1)
            + (state.getPortValue(PORT_INDEX_C).toLongValue() << 2)
            + (state.getPortValue(PORT_INDEX_D).toLongValue() << 3);
      } else  if (state.getPortValue(PORT_INDEX_EnP).and(state.getPortValue(PORT_INDEX_EnT)).toLongValue() == 1) {
        counter = (counter + 1) & 15;
      }
    }
    updateState(state, counter);
  }

}
