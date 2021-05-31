/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Loggable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InstanceLoggerAdapter implements Loggable {

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
      String className = loggerClass.getName();
      loggerS.error("Error while instantiating logger {}: {}", className,
          t.getClass().getName());
      String msg = t.getMessage();
      if (msg != null)
        loggerS.error("  ({})", msg); // OK
      logger = null;
    }
  }

  public String getLogName(Object option) {
    return logger == null ? null : logger.getLogName(state, option);
  }

  public BitWidth getBitWidth(Object option) {
    return logger == null ? null : logger.getBitWidth(state, option);
  }
  
  public boolean isInput(Object option) {
    return logger == null ? false : logger.isInput(state, option);
  }

  public Object[] getLogOptions() {
    return logger == null ? null : logger.getLogOptions(state);
  }

  public Value getLogValue(CircuitState circuitState, Object option) {
    if (logger != null) {
      if (state.getCircuitState() != circuitState)
        state.repurpose(circuitState, comp);
      return logger.getLogValue(state, option);
    } else {
      return Value.UNKNOWN;
    }
  }
}
