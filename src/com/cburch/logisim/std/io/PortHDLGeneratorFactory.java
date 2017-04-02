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
package com.cburch.logisim.std.io;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;

public class PortHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

	private class InOutMap {

		private Point p;
		private Type type;
		private int portNb;

		public InOutMap(Type type, Point p, int portNb) {
			this.p = p;
			this.type = type;
			this.portNb = portNb;
		}

		public int getEnd() {
			return p.y;
		}

		public int getPortNb() {
			return portNb;
		}

		public int getSize() {
			return (p.y - p.x) + 1;
		}

		public int getStart() {
			return p.x;
		}

		public Type getType() {
			return type;
		}
	}

	private enum Type {

		IN, OUT, INOUT
	}

	private static final String inBusName = "PIO_IN_BUS";
	private static final String outBusName = "PIO_OUT_BUS";

	private static final String inOutBusName = "PIO_INOUT_BUS";

	private HashMap<String, HashMap<Integer, InOutMap>> compMap = new HashMap<String, HashMap<Integer, InOutMap>>();

	private Location findEndConnection(Location start, Circuit circ) {
		Location newLoc = start;
		Collection<Wire> wiresCol = circ.getWires(newLoc);
		Iterator<Wire> wires = wiresCol.iterator();
		if (wiresCol.size() != 1) {
			return null;
		}
		Wire net = null;
		Wire oldNet = null;

		while (wires.hasNext()) {
			net = wires.next();
			if (net != oldNet) {
				newLoc = net.getEnd0().equals(newLoc) ? net.getEnd1() : net
						.getEnd0();
				wiresCol = circ.getWires(newLoc);
				wires = wiresCol.iterator();
				oldNet = net;
			}
		}
		return newLoc;
	}

	// #4
	@Override
	public ArrayList<String> GetArchitecture(Netlist TheNetlist,
			AttributeSet attrs, String ComponentName, FPGAReport Reporter,
			String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		if (HDLType.equals(VHDL)) {
			Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
					HDLType, TheNetlist.projName()));
			Contents.add("");
			Contents.add("ARCHITECTURE PlatformIndependent OF "
					+ ComponentName.toString() + " IS ");
			Contents.add("");
			Contents.add("BEGIN");
			Contents.add("");
			int currentInOutIdx = -1;
			for (int i = 0; i < compMap.get(ComponentName).size(); i++) {
				if (compMap.get(ComponentName).get(i).getType() == Type.IN) {
					if (compMap.get(ComponentName).get(i).getSize() == 1) {
						Contents.add(inOutBusName + "_" + currentInOutIdx + "("
								+ compMap.get(ComponentName).get(i).getEnd()
								+ ")" + " <= " + inBusName + "_" + i + ";");
					} else {
						Contents.add(inOutBusName + "_" + currentInOutIdx + "("
								+ compMap.get(ComponentName).get(i).getEnd()
								+ " DOWNTO "
								+ compMap.get(ComponentName).get(i).getStart()
								+ ")" + " <= " + inBusName + "_" + i + ";");
					}
				} else if (compMap.get(ComponentName).get(i).getType() == Type.OUT) {
					if (compMap.get(ComponentName).get(i).getSize() == 1) {
						Contents.add(outBusName + "_" + i + " <= "
								+ inOutBusName + "_" + currentInOutIdx + "("
								+ compMap.get(ComponentName).get(i).getEnd()
								+ ");");
					} else {
						Contents.add(outBusName + "_" + i + " <= "
								+ inOutBusName + "_" + currentInOutIdx + "("
								+ compMap.get(ComponentName).get(i).getEnd()
								+ " DOWNTO "
								+ compMap.get(ComponentName).get(i).getStart()
								+ ");");
					}
				} else if (compMap.get(ComponentName).get(i).getType() == Type.INOUT) {
					currentInOutIdx = i;
				}
			}
			Contents.add("");
			Contents.add("END PlatformIndependent;");
		}
		return Contents;
	}

	private Point getBitRange(byte[] bits, int wireNr) {
		int i;
		int start = -1;
		boolean first = true;
		int count = -1;
		for (i = 0; i < bits.length; i++) {
			if (bits[i] == wireNr) {
				if (first) {
					first = false;
					start = i;
				}
				count++;
			}
		}
		return new Point(start, start + count);
	}

	// #8,10,11,13
	@Override
	public String getComponentStringIdentifier() {
		return "PORTIO";
	}

	// #2
	@Override
	public ArrayList<String> GetEntity(Netlist TheNetlist, AttributeSet attrs,
			String ComponentName, FPGAReport Reporter, String HDLType) {

		NetlistComponent ComponentInfo = null;
		compMap.put(ComponentName, new HashMap<Integer, InOutMap>());
		for (NetlistComponent comp : TheNetlist.GetNormalComponents()) {
			if (comp.GetComponent().getAttributeSet().equals(attrs)) {
				ComponentInfo = comp;
				break;
			}
		}

		int mapIdx = 0;
		for (int portNr = 0; portNr < ComponentInfo.GetComponent().getEnds()
				.size(); portNr++) {
			Location splitterLoc = findEndConnection(ComponentInfo
					.GetComponent().getEnd(portNr).getLocation(),
					TheNetlist.getCircuit());
			if (splitterLoc == null) {
				Reporter.AddFatalError("Found 0, 2 or more connections on PortIO's splitter ("
						+ ComponentName + ")");
				return null;
			}
			for (Splitter split : TheNetlist.getSplitters()) {
				if (split.getLocation().equals(splitterLoc)) { // trouve le
																// premier
																// splitter du
																// Port
					compMap.get(ComponentName).put(
							mapIdx,
							new InOutMap(Type.INOUT, new Point(0, split
									.GetEndpoints().length - 1), portNr));
					int splitPortNr = 0;
					for (EndData end : split.getEnds()) {
						if (!end.getLocation().equals(splitterLoc)) { // parcours
																		// les
																		// sortie
																		// du
																		// splitter
							Location compLoc = findEndConnection(
									end.getLocation(), TheNetlist.getCircuit());
							if (compLoc == null) {
								Reporter.AddFatalError("Found 0, 2 or more connections on PortIO's splitter ("
										+ ComponentName + ")");
								return null;
							}
							for (Component comp : TheNetlist.getCircuit()
									.getNonWires(compLoc)) { // parcours le
																// (les?)
																// composant
																// connecte a la
																// sortie du
																// splitter
								for (EndData port : comp.getEnds()) {
									if (port.getLocation().equals(compLoc)) { // trouve
																				// le
																				// port
																				// du
																				// composant
																				// relie
																				// au
																				// splitter
										if (!(comp instanceof Splitter)
												&& !(comp instanceof PortIO)) {
											if (port.isInput()) {
												compMap.get(ComponentName)
														.put(mapIdx,
																new InOutMap(
																		Type.OUT,
																		getBitRange(
																				split.GetEndpoints(),
																				splitPortNr),
																		portNr));
											} else if (port.isOutput()) {
												compMap.get(ComponentName)
														.put(mapIdx,
																new InOutMap(
																		Type.IN,
																		getBitRange(
																				split.GetEndpoints(),
																				splitPortNr),
																		portNr));
											}
										} else {
											Reporter.AddFatalError("Cannot connect PortIO's splitter to other splitter or PortIO ("
													+ ComponentName + ")");
											return null;
										}
									}
								}
							}
						}
						mapIdx++;
						splitPortNr++;
					}
				}
			}
		}

		ArrayList<String> Contents = new ArrayList<String>();
		Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
				VHDL, TheNetlist.projName()));
		Contents.addAll(FileWriter.getExtendedLibrary());
		Contents.add("ENTITY " + ComponentName + " IS");
		Contents.add("   PORT ( ");

		for (int i = 0; i < compMap.get(ComponentName).size(); i++) {
			String line = "          ";
			switch (compMap.get(ComponentName).get(i).getType()) {
			case IN:
				line += inBusName + "_" + i + "  : IN ";
				break;
			case OUT:
				line += outBusName + "_" + i + "  : OUT ";
				break;
			case INOUT:
				line += inOutBusName + "_" + i + "  : INOUT ";
				break;
			default:
				Reporter.AddFatalError("Found component of unknown type ("
						+ compMap.get(ComponentName).get(i).toString() + ")");
			}
			if (compMap.get(ComponentName).get(i).getSize() == 1) {
				line += "std_logic";
			} else {
				line += "std_logic_vector ("
						+ (compMap.get(ComponentName).get(i).getSize() - 1)
						+ " DOWNTO 0)";
			}

			if (i == (compMap.get(ComponentName).size() - 1)) {
				line += ")";
			}
			line += ";";
			Contents.add(line);
		}
		Contents.add("END " + ComponentName + ";");
		Contents.add("");
		return Contents;
	}

	// #6
	@Override
	public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist,
			AttributeSet attrs) {
		String ComponentName = attrs.getValue(StdAttr.LABEL);
		SortedMap<String, Integer> InOuts = new TreeMap<String, Integer>();
		for (int i = 0; i < compMap.get(ComponentName).size(); i++) {
			if (compMap.get(ComponentName).get(i).getType() == Type.INOUT) {
				InOuts.put(inOutBusName + "_" + i, compMap.get(ComponentName)
						.get(i).getSize());
			}
		}

		return InOuts;
	}

	// #5
	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		String ComponentName = attrs.getValue(StdAttr.LABEL);
		SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
		for (int i = 0; i < compMap.get(ComponentName).size(); i++) {
			if (compMap.get(ComponentName).get(i).getType() == Type.IN) {
				Inputs.put(inBusName + "_" + i,
						compMap.get(ComponentName).get(i).getSize());
			}
		}

		return Inputs;
	}

	// #7
	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		String ComponentName = attrs.getValue(StdAttr.LABEL);
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		for (int i = 0; i < compMap.get(ComponentName).size(); i++) {
			if (compMap.get(ComponentName).get(i).getType() == Type.OUT) {
				Outputs.put(outBusName + "_" + i, compMap.get(ComponentName)
						.get(i).getSize());
			}
		}

		return Outputs;
	}

	// #9,12
	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		String ComponentName = ComponentInfo.GetComponent().getAttributeSet()
				.getValue(StdAttr.LABEL);
		SortedMap<String, String> PortMap = new TreeMap<String, String>();

		for (int i = 0; i < compMap.get(ComponentName).size(); i++) {
			String key = null;
			String name = null;
			int start = -1;
			int end = -1;
			switch (compMap.get(ComponentName).get(i).getType()) {
			case IN:
				key = inBusName + "_" + i;
				name = BusName
						+ Integer.toString(Nets.GetNetId(ComponentInfo
								.getEnd(compMap.get(ComponentName).get(i)
										.getPortNb()).GetConnection((byte) 0)
								.GetParrentNet()));
				start = compMap.get(ComponentName).get(i).getStart();
				end = compMap.get(ComponentName).get(i).getEnd();
				break;
			case OUT:
				key = outBusName + "_" + i;
				name = BusName
						+ Integer.toString(Nets.GetNetId(ComponentInfo
								.getEnd(compMap.get(ComponentName).get(i)
										.getPortNb()).GetConnection((byte) 0)
								.GetParrentNet()));
				start = compMap.get(ComponentName).get(i).getStart();
				end = compMap.get(ComponentName).get(i).getEnd();
				break;
			case INOUT:
				key = inOutBusName + "_" + i;
				name = LocalInOutBubbleBusname;
				start = end = ComponentInfo.GetLocalBubbleInOutStartId();
				start += compMap.get(ComponentName).get(i).getStart()
						+ compMap.get(ComponentName).get(i).getPortNb() * 32;
				end += compMap.get(ComponentName).get(i).getEnd()
						+ compMap.get(ComponentName).get(i).getPortNb() * 32;
				break;
			default:
				Reporter.AddFatalError("Found component of unknown type ("
						+ compMap.get(ComponentName).get(i).toString() + ")");
			}
			if (compMap.get(ComponentName).get(i).getSize() == 1) {
				PortMap.put(key, name + "(" + end + ")");
			} else {
				PortMap.put(key, name + "(" + end + " DOWNTO " + start + ")");
			}
		}

		return PortMap;
	}

	// #1,3
	@Override
	public String GetSubDir() {
		/*
		 * this method returns the module sub-directory where the HDL code is
		 * placed
		 */
		return "io";
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
		return HDLType.equals(VHDL);
	}
}
