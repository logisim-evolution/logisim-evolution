/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.sim;

/**
 * Listener interface for all classes who wants to be advised on current VHDL simulator status
 * changes.
 *
 * @author christian.mueller@heig-vd.ch
 * @since 2.12.0.t
 */
public interface VhdlSimulatorListener {
  void stateChanged();
}
