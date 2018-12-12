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
package com.cburch.logisim.std.tcl;

import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.InstanceStateImpl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.SocketClient;
import com.cburch.logisim.data.Value;

/**
 * The TCL components needs some activity for each instance of component. Here
 * we extend the InstanceComponent for the Tcl components to create those
 * activities
 */
public class TclComponentData implements InstanceData {

	/**
	 * Retrieves the state associated with this Tcl console in the circuit
	 * state, generating the state if necessary.
	 */
	public static TclComponentData get(InstanceState state) {
		TclComponentData ret = (TclComponentData) state.getData();
		if (ret == null) {
			// If it doesn't yet exist, then we'll set it up with our default
			// values and put it into the circuit state so it can be retrieved
			// in future propagations.
			ret = new TclComponentData(state);
			state.setData(ret);
		}
		return ret;
	}

	private SocketClient tclClient;

	private TclWrapperListenerThread tclWrapperListenerThread;

	private TclWrapper tclWrapper;

	private InstanceState instanceState;

        private Value prevClockValue = Value.UNKNOWN;

	TclComponentData(InstanceState state) {

		instanceState = state;

		tclClient = new SocketClient();
		tclWrapper = new TclWrapper(this);
	}

	@Override
	public Object clone() {
		return null;
	}

	public InstanceState getState() {
		return instanceState;
	}

	public SocketClient getTclClient() {
		return tclClient;
	}

	public TclWrapper getTclWrapper() {
		return tclWrapper;
	}

	public boolean isConnected() {
		return tclClient.isConnected();
	}
        
        public boolean isNewTick() {
                boolean newTick = false;
                boolean found   = false;

                for (Port p : instanceState.getInstance().getPorts()) {
                    if (p.getToolTip().equals("sysclk_i")) {
                        Value val = instanceState.getPortValue(instanceState.getPortIndex(p));
                        newTick = (val != prevClockValue);
                        if (newTick) {
                            prevClockValue = val;
                        }
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    throw new UnsupportedOperationException("Could not find the 'sysclock' in the TCL component");
                }

                return newTick;
        }

	public String receive() {
		return tclWrapperListenerThread.receive();
	}

	public void send(String message) {
		tclClient.send(message);
	}

	public void tclWrapperStartCallback() {

		tclClient.start();

		tclWrapperListenerThread = new TclWrapperListenerThread(tclClient,
				((InstanceStateImpl) instanceState).getCircuitState()
						.getProject().getSimulator());
		tclWrapperListenerThread.start();
	}
}
