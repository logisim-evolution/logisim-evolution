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

package com.bfh.logisim.designrulecheck;

import java.util.ArrayList;

import com.cburch.logisim.comp.Component;

public class ConnectionEnd {

	private boolean IsOutput;
	private Byte nr_of_bits;
	private ArrayList<ConnectionPoint> MyConnections;

	public ConnectionEnd(boolean OutputEnd, Byte nr_of_bits, Component comp) {
		IsOutput = OutputEnd;
		this.nr_of_bits = nr_of_bits;
		MyConnections = new ArrayList<ConnectionPoint>();
		for (byte i = 0; i < nr_of_bits; i++)
			MyConnections.add(new ConnectionPoint(comp));
	}

	public ConnectionPoint GetConnection(Byte BitIndex) {
		if ((BitIndex < 0) || (BitIndex >= nr_of_bits))
			return null;
		return MyConnections.get(BitIndex);
	}

	public boolean IsOutputEnd() {
		return IsOutput;
	}

	public int NrOfBits() {
		return nr_of_bits;
	}

	public boolean SetChildPortIndex(Net ConnectedNet, Byte BitIndex,
			int PortIndex) {
		if ((BitIndex < 0) || (BitIndex >= nr_of_bits))
			return false;
		ConnectionPoint Connection = MyConnections.get(BitIndex);
		if (Connection == null)
			return false;
		Connection.setChildsPortIndex(PortIndex);
		return true;
	}

	public boolean SetConnection(ConnectionPoint Connection, Byte BitIndex) {
		if ((BitIndex < 0) || (BitIndex >= nr_of_bits))
			return false;
		MyConnections.set(BitIndex, Connection);
		return true;
	}
}
