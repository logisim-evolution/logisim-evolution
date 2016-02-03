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

import java.util.HashSet;
import java.util.Set;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;

public class CircuitNet {
	/* This class represents one net in a netlist of a circuit */
	private Set<Location> MyConnectedPoints; /* All connected points of this net in the circuit */
	private Set<Location> MyUnconnectedPoints; /* All unconnected points of this net in the circuit */
	private Set<Wire> MySegments; /* All wire segments belonging to this net */
	private int nr_of_bits; /* the number of "wires" in this net, read: 1 Wire => single net; > 1 Wire => bus */

	public CircuitNet() {
		initialize();
	}
	
	public CircuitNet(Location loc) {
		initialize();
		MyConnectedPoints.add(loc);
	}
	
	public CircuitNet(Location loc, int width) {
		initialize();
		MyConnectedPoints.add(loc);
		nr_of_bits = width;
	}
	
	public void add(Wire segment) {
		Location p0 = segment.getEnd0();
		Location p1 = segment.getEnd1();
		if (MyUnconnectedPoints.contains(p0)) {
			MyUnconnectedPoints.remove(p0);
			MyConnectedPoints.add(p0);
		} else if (!MyConnectedPoints.contains(p0))
			MyUnconnectedPoints.add(p0);
		if (MyUnconnectedPoints.contains(p1)) {
			MyUnconnectedPoints.remove(p1);
			MyConnectedPoints.add(p1);
		} else if (!MyConnectedPoints.contains(p1))
			MyUnconnectedPoints.add(p1);
		MySegments.add(segment);
	}
	
	public int BitWidth() {
		return nr_of_bits;
	}
	
	public Set<Location> getConnectedPoints() {
		return MyConnectedPoints;
	}
	
	public Set<Location> getUnconnectedPoints() {
		return MyUnconnectedPoints;
	}
	
	public Set<Wire> getSegments() {
		return MySegments;
	}
	
	public boolean merge(CircuitNet TheNet,String Error,boolean ClearBitwidth) {
		if (TheNet.BitWidth()==nr_of_bits ||
			ClearBitwidth) {
			Set<Location> MergeSet = new HashSet<Location>(TheNet.getUnconnectedPoints());
			Set<Location> CommonSet = new HashSet<Location>();
			for (Location loc : MergeSet) {
				if (MyUnconnectedPoints.contains(loc))
					CommonSet.add(loc);
			}
			MergeSet.removeAll(CommonSet);
			MyUnconnectedPoints.removeAll(CommonSet);
			MyUnconnectedPoints.addAll(MergeSet);
			MyConnectedPoints.addAll(CommonSet);
			MyConnectedPoints.addAll(TheNet.getConnectedPoints());
			MySegments.addAll(TheNet.getSegments());
			if (ClearBitwidth)
				nr_of_bits = 0;
			return true;
		}
		Error = Error.concat(Strings.get("NetMerge_BitWidthError"));
		return false;
	}

	public boolean isEmpty() {
		return MyConnectedPoints.isEmpty()&&
				MyUnconnectedPoints.isEmpty();
	}

	public boolean contains(Location point) {
		return MyConnectedPoints.contains(point) ||
				MyUnconnectedPoints.contains(point);
	}

	private void initialize() {
		MyConnectedPoints = new HashSet<Location>();
		MyUnconnectedPoints = new HashSet<Location>();
		MySegments = new HashSet<Wire>();
		nr_of_bits = 0;
	}
}
