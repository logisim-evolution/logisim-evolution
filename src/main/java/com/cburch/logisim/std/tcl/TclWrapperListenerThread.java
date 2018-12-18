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

import java.util.LinkedList;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.util.SocketClient;
import com.cburch.logisim.util.UniquelyNamedThread;

public class TclWrapperListenerThread extends UniquelyNamedThread {

	SocketClient socket;
	LinkedList<String> messages;
	Simulator sim;

	Boolean socket_open = true;

	TclWrapperListenerThread(SocketClient socket, Simulator simulator) {
		super("TclWrapperListenerThread");
		this.socket = socket;
		this.messages = new LinkedList<String>();
		this.sim = simulator;
	}

	/**
	 * Get message from TCL wrapper Messages ar in the lister buffer Read is
	 * blocking, unblocks if socket closes
	 *
	 * @return The next message
	 */
	public String receive() {

		while (socket_open && messages.size() < 1) {
			try {
				sleep(100, 0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (messages.size() > 0)
			return messages.pop();
		else
			return null;
	}

	@Override
	public void run() {
		String line;

		/* Continuously receive TCL wrapper messages */
		while ((line = socket.receive()) != null) {

			/* Stock the messages in temp buffer or tick simulation if asked */
			if (line.equals("run")) {
				sim.tick();
			} else {
				messages.add(line);
			}
		}

		socket_open = false;
	}

}
