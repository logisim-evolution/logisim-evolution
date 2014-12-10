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

package com.cburch.logisim.circuit;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.cburch.logisim.circuit.appear.CircuitPins;

public abstract class CircuitTransaction {
	public static final Integer READ_ONLY = Integer.valueOf(1);
	public static final Integer READ_WRITE = Integer.valueOf(2);

	public final CircuitTransactionResult execute() {
		CircuitMutatorImpl mutator = new CircuitMutatorImpl();
		Map<Circuit, Lock> locks = CircuitLocker.acquireLocks(this, mutator);
		CircuitTransactionResult result;
		try {
			this.run(mutator);

			// Let the port locations of each subcircuit's appearance be
			// updated to reflect the changes - this needs to happen before
			// wires are repaired because it could lead to some wires being
			// split
			Collection<Circuit> modified = mutator.getModifiedCircuits();
			for (Circuit circuit : modified) {
				CircuitMutatorImpl circMutator = circuit.getLocker()
						.getMutator();
				if (circMutator == mutator) {
					CircuitPins pins = circuit.getAppearance().getCircuitPins();
					ReplacementMap repl = mutator.getReplacementMap(circuit);
					if (repl != null) {
						pins.transactionCompleted(repl);
					}
				}
			}

			// Now go through each affected circuit and repair its wires
			for (Circuit circuit : modified) {
				CircuitMutatorImpl circMutator = circuit.getLocker()
						.getMutator();
				if (circMutator == mutator) {
					WireRepair repair = new WireRepair(circuit);
					repair.run(mutator);
				} else {
					// this is a transaction executed within a transaction -
					// wait to repair wires until overall transaction is done
					circMutator.markModified(circuit);
				}
			}

			result = new CircuitTransactionResult(mutator);
			for (Circuit circuit : result.getModifiedCircuits()) {
				circuit.fireEvent(CircuitEvent.TRANSACTION_DONE, result);
			}
		} finally {
			CircuitLocker.releaseLocks(locks);
		}
		return result;
	}

	protected abstract Map<Circuit, Integer> getAccessedCircuits();

	protected abstract void run(CircuitMutator mutator);

}
