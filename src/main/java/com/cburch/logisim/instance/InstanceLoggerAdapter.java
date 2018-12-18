/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Loggable;

class InstanceLoggerAdapter implements Loggable {

	final static Logger loggerS = LoggerFactory
			.getLogger(InstanceLoggerAdapter.class);

	private InstanceComponent comp;
	private InstanceLogger logger;
	private InstanceStateImpl state;

	public InstanceLoggerAdapter(InstanceComponent comp,
			Class<? extends InstanceLogger> loggerClass) {
		try {
			this.comp = comp;
			this.logger = loggerClass.newInstance();
			this.state = new InstanceStateImpl(null, comp);
		} catch (Exception t) {
			handleError(t, loggerClass);
			logger = null;
		}
	}

	public String getLogName(Object option) {
		if (logger != null) {
			return logger.getLogName(state, option);
		} else {
			return null;
		}
	}

	public Object[] getLogOptions(CircuitState circState) {
		if (logger != null) {
			updateState(circState);
			return logger.getLogOptions(state);
		} else {
			return null;
		}
	}

	public Value getLogValue(CircuitState circuitState, Object option) {
		if (logger != null) {
			updateState(circuitState);
			return logger.getLogValue(state, option);
		} else {
			return Value.UNKNOWN;
		}
	}

	private void handleError(Throwable t,
			Class<? extends InstanceLogger> loggerClass) {
		String className = loggerClass.getName();
		loggerS.error("Error while instantiating logger {}: {}", className, t
				.getClass().getName());
		String msg = t.getMessage();
		if (msg != null)
			loggerS.error("  ({})", msg); // OK
	}

	private void updateState(CircuitState circuitState) {
		if (state.getCircuitState() != circuitState) {
			state.repurpose(circuitState, comp);
		}
	}
}
