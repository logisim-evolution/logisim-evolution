/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.LoggableContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InstanceLoggerAdapter implements LoggableContract {

  static final Logger loggerS = LoggerFactory.getLogger(InstanceLoggerAdapter.class);

  private InstanceComponent comp;
  private InstanceLogger logger;
  private InstanceStateImpl state;

  public InstanceLoggerAdapter(
      InstanceComponent comp, Class<? extends InstanceLogger> loggerClass) {
    try {
      this.comp = comp;
      this.logger = loggerClass.getDeclaredConstructor().newInstance();
      this.state = new InstanceStateImpl(null, comp);
    } catch (Exception t) {
      final var className = loggerClass.getName();
      loggerS.error("Error while instantiating logger {}: {}", className, t.getClass().getName());
      final var msg = t.getMessage();
      if (msg != null) loggerS.error("  ({})", msg);
      logger = null;
    }
  }

  @Override
  public String getLogName(Object option) {
    return logger == null ? null : logger.getLogName(state, option);
  }

  @Override
  public BitWidth getBitWidth(Object option) {
    return logger == null ? null : logger.getBitWidth(state, option);
  }

  @Override
  public boolean isInput(Object option) {
    return logger == null ? false : logger.isInput(state, option);
  }

  @Override
  public Object[] getLogOptions() {
    return logger == null ? null : logger.getLogOptions(state);
  }

  @Override
  public Value getLogValue(CircuitState circuitState, Object option) {
    if (logger != null) {
      if (state.getCircuitState() != circuitState) state.repurpose(circuitState, comp);
      return logger.getLogValue(state, option);
    } else {
      return Value.UNKNOWN;
    }
  }
}
