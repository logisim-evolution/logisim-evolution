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
import java.util.HashSet;
import java.util.Set;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;

public class CircuitNet {
	/* This class represents one net in a netlist of a circuit */
	private Set<Location> MyConnectedPoints; /* All connected points of this net in the circuit */
	private Set<Location> MyUnconnectedPoints; /* All unconnected points of this net in the circuit */
	private Set<Location> MyComponentConnections;
	private Set<Wire> MySegments; /* All wire segments belonging to this net */
	private int nr_of_bits; /* the number of "wires" in this net, read: 1 Wire => single net; > 1 Wire => bus */
	private String NetName;
	private boolean IsPinConnected; /* nets that are connected to a pin will not be removed */
	private boolean IsComponentConnected; /* nets that are connected to a component will not be removed */
	private boolean IsSplitterConnected; /* Nets connected to a splitter might be removed */
	private ArrayList<Boolean> IsClockNet; /* for each bit in a wire/bus we can make a clock annotation */
	private ArrayList<Component> MySource; /* for each bit in the wire/bus a source is marked */
	private ArrayList<Set<Component>> MySinks; /* for each bit in the wire/bus the sink(s) are marked */

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
	
	public boolean BitWidthDefined() {
		return nr_of_bits > 0;
	}
	
	public int BitWidth() {
		return nr_of_bits;
	}
	
	public boolean setBitWidth(int width) {
		if (nr_of_bits > 0) {
			return width != nr_of_bits;
		} 
		nr_of_bits = width;
		return true;
	}
	
	public void clearBitWidth() {
		nr_of_bits = 0;
	}
	
	public Set<Wire> getSegments() {
		return MySegments;
	}
	
	public boolean hasName() {
		return !NetName.isEmpty();
	}
	
	public boolean SetName(String Name) {
		if (!NetName.isEmpty())
			return false;
		NetName = Name;
		return true;
	}
	
	public String getName() {
		return NetName;
	}
	
	public boolean hasConnection() {
		return IsPinConnected||IsComponentConnected||IsSplitterConnected;
	}
	
	public boolean isOnlySplitterConnected() {
		return !IsPinConnected&&!IsComponentConnected&&IsSplitterConnected;
	}
	
	public boolean IsSplitterConnected() {
		return IsSplitterConnected;
	}
	
	public boolean SetPinConnection(Component comp,
			                        boolean IsSource,
			                        int nrOfBits,
			                        Location loc) {
		if (!UpdateConnection(comp,IsSource,nrOfBits,loc))
			return false;
		IsPinConnected = true;
		return true;
	}
	
	public boolean SetComponentConnection(Component comp,
            						      boolean IsSource,
            						      int nrOfBits,
            						      Location loc) {
		if (!UpdateConnection(comp,IsSource,nrOfBits,loc))
			return false;
		IsComponentConnected = true;
		return true;
	}
	
	public void SetSplitterConnection(Location loc) {
		if (MyUnconnectedPoints.contains(loc)) {
			MyUnconnectedPoints.remove(loc);
			MyConnectedPoints.add(loc);
		}
		MyComponentConnections.add(loc);
		IsSplitterConnected = true;
	}
	
	public boolean hasSource() {
		return MySource!=null;
	}
	
	public boolean hasSinks() {
		return MySinks!=null;
	}
	
	
	public boolean hasClockNets() {
		return IsClockNet != null;
	}
	
	public void RemoveComponentConnection(Location loc) {
		MyComponentConnections.remove(loc);
	}
	
	public boolean merge(CircuitNet TheNet,String Error,boolean ClearBitwidth) {
		if (TheNet.nr_of_bits!=nr_of_bits && !ClearBitwidth) {
			Error = Error.concat(Strings.get("NetMerge_BitWidthError"));
			return false;
		}
		if (TheNet.hasSource()&&hasSource()) {
			Error = Error.concat(Strings.get("NetMerge_ShorCircuit"));
			return false;
		}
		Set<Location> MergeSet = new HashSet<Location>(TheNet.MyUnconnectedPoints);
		Set<Location> CommonSet = new HashSet<Location>();
		for (Location loc : MergeSet) {
			if (MyUnconnectedPoints.contains(loc))
				CommonSet.add(loc);
		}
		MergeSet.removeAll(CommonSet);
		MyUnconnectedPoints.removeAll(CommonSet);
		MyUnconnectedPoints.addAll(MergeSet);
		MyConnectedPoints.addAll(CommonSet);
		MyConnectedPoints.addAll(TheNet.MyConnectedPoints);
		MySegments.addAll(TheNet.MySegments);
		if (ClearBitwidth)
			nr_of_bits = 0;
		if (TheNet.hasSource()) {
			MySource = TheNet.MySource;
		}
		if (TheNet.hasSinks()) {
			if (MySinks == null) {
				MySinks = TheNet.MySinks;
			} else {
				for (int i = 0 ; i < nr_of_bits ; i++) {
					Set<Component> comps = TheNet.MySinks.get(i);
					if (comps != null) {
						Error = Error.concat(Strings.get("NetMerge_EmptySinkSet"));
						return false;
					}
					MySinks.get(i).addAll(comps);
				}
			}
		}
		MyComponentConnections.addAll(TheNet.MyComponentConnections);
		IsPinConnected |= TheNet.IsPinConnected;
		IsComponentConnected |= TheNet.IsComponentConnected;
		IsSplitterConnected |= TheNet.IsSplitterConnected;
		return true;
	}
		
	
	public boolean merge(CircuitNet TheNet,String Error,int startindex) {
		if ((TheNet.BitWidth()+startindex)>nr_of_bits) {
			Error = Error.concat(Strings.get("NetMerge_BitOverFlowError"));
			return false;
		}
		if (startindex < 0) {
			Error = Error.concat(Strings.get("NetMerge_IndexNegative"));
			return false;
		}
		for (int i = 0 ; i < TheNet.BitWidth(); i++) {
			/* TODO */
		}
		return true;
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
		MyComponentConnections = new HashSet<Location>();
		MySegments = new HashSet<Wire>();
		nr_of_bits = 0;
		NetName = "";
		IsPinConnected = false;
		IsComponentConnected = false;
		IsSplitterConnected = false;
		MySource = null;
		MySinks = null;
	}

	private boolean UpdateConnection(Component comp,
			 						 boolean IsSource,
			 						 int nrOfBits,
			 						 Location loc) {
		if (BitWidthDefined()&&
				nrOfBits!=nr_of_bits)
			return false;
		if (IsSource && hasSource())
			return false;
		if (IsSource) {
			MySource = new ArrayList<Component>();
			for (int bit = 0 ; bit < nrOfBits; bit++) {
				MySource.add(comp);
			}
		} else {
			if (MySinks == null) {
				MySinks = new ArrayList<Set<Component>>();
				for (int bit = 0 ; bit < nrOfBits; bit++)
					MySinks.add(new HashSet<Component>());
			}
			for (int bit = 0 ; bit < nrOfBits; bit++)
				MySinks.get(bit).add(comp);
		}
		nr_of_bits = nrOfBits;
		if (MyUnconnectedPoints.contains(loc)) {
			MyUnconnectedPoints.remove(loc);
			MyConnectedPoints.add(loc);
		}
		MyComponentConnections.add(loc);
		return true;
	}

}
