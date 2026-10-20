/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;

/**
 * Factory notified on every simulator tick, even when its inputs are stable.
 *
 * <p>Propagation delay cannot represent a pulse that must stay visible between ticks. Clock
 * components are not a fit either: this notification is not a clock source for FPGA timing
 * analysis.
 */
public interface TickAware {
  /**
   * Advances tick-based state.
   *
   * @param state circuit state that owns {@code comp}
   * @param ticks simulator tick count after this tick, the same value {@code Clock} receives
   * @param comp component instance to update
   * @return true when the component must be propagated
   */
  boolean tick(CircuitState state, int ticks, Component comp);
}
