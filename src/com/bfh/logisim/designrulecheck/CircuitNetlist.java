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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import com.bfh.logisim.fpgagui.FPGAReport;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Tunnel;

public class CircuitNetlist implements CircuitListener {
	
	@Override
	public void circuitChanged(CircuitEvent event) {
		int ev = event.getAction();
		if (event.getData() instanceof InstanceComponent) {
			InstanceComponent inst = (InstanceComponent)event.getData();
			if (event.getCircuit().equals(MyCircuit)) {
				switch (ev) {
				case CircuitEvent.ACTION_ADD : 
					DRCStatus = DRC_REQUIRED;
					if (inst.getFactory() instanceof SubcircuitFactory) {
						SubcircuitFactory fac = (SubcircuitFactory)inst.getFactory();
						Circuit sub = fac.getSubcircuit();
					
						if (MySubCircuitMap.containsKey(sub)) {
							MySubCircuitMap.put(sub, MySubCircuitMap.get(sub)+1);
						} else {
							MySubCircuitMap.put(sub, 1 );
							sub.addCircuitListener(this);
						}
					}
					break;
				case CircuitEvent.ACTION_REMOVE :
					DRCStatus = DRC_REQUIRED;
					if (inst.getFactory() instanceof SubcircuitFactory) {
						SubcircuitFactory fac = (SubcircuitFactory)inst.getFactory();
						Circuit sub = fac.getSubcircuit();
						if (MySubCircuitMap.containsKey(sub)) {
							if (MySubCircuitMap.get(sub)==1) {
								MySubCircuitMap.remove(sub);
								sub.removeCircuitListener(this);
							} else {
								MySubCircuitMap.put(sub, MySubCircuitMap.get(sub)-1);
							}
						}
					}
					break;
				case CircuitEvent.ACTION_CHANGE:
				case CircuitEvent.ACTION_CLEAR:
				case CircuitEvent.ACTION_INVALIDATE:
					DRCStatus = DRC_REQUIRED;
					break;
				}
			} else {
				if (inst.getFactory() instanceof Pin) {
					DRCStatus = DRC_REQUIRED;
				}
			}
		}
	}

	public static final int DRC_REQUIRED = 4;
	public static final int DRC_PASSED = 0;
	public static final int ANNOTATE_REQUIRED = 1;
	public static final int DRC_ERROR = 2;
	public static final Color DRC_INSTANCE_MARK_COLOR = Color.RED;
	public static final Color DRC_LABEL_MARK_COLOR = Color.MAGENTA;
	public static final Color DRC_WIRE_MARK_COLOR = Color.ORANGE;

	private Map<Circuit,Integer> MySubCircuitMap; /* This is an important information as it contains all my subcircuits; it is handled by the CircuitListener */
	private Circuit MyCircuit;
	private String CircuitName;
	private Set<CircuitNet> MyNets;
	private int DRCStatus;

	public CircuitNetlist(Circuit ThisCircuit) {
		MyCircuit = ThisCircuit;
		CircuitName = ThisCircuit.getName();
		MySubCircuitMap = new HashMap<Circuit,Integer>();
		clear();
	}
	
	public int DesignRuleCheckResult(FPGAReport Reporter, String HDLIdentifier,
			boolean IsTopLevel, ArrayList<String> Sheetnames) {
		ArrayList<String> CompName = new ArrayList<String>();
		Map<String,Component> Labels = new HashMap<String,Component>();
		ArrayList<SimpleDRCContainer> drc = new ArrayList<SimpleDRCContainer>();
		int CommonDRCStatus = DRC_PASSED;
		/* First we go down the tree and get the DRC status of all sub-circuits */
		for (Circuit circ:MySubCircuitMap.keySet()) {
			CommonDRCStatus |= circ.getNetList().DesignRuleCheckResult(Reporter, HDLIdentifier, false, Sheetnames);
		}
		/* Check if we are okay */
		if (DRCStatus == DRC_PASSED) {
			return CommonDRCStatus;
		} else {
			/* There are changes, so we clean up the old information */
			clear();
			DRCStatus = DRC_PASSED; /* we mark already passed, if an error occurs the status is changed */
		}
		/*
		 * Check for duplicated sheet names, this is bad as we will have
		 * multiple "different" components with the same name
		 */
		if (MyCircuit.getName().isEmpty()) {
			/* in the current implementation of logisim this should never happen, but we leave it in */
			Reporter.AddFatalError("Found a sheet in your design with an empty name. This is not allowed, please specify a name!");
			DRCStatus |= DRC_ERROR;
		}
		if (Sheetnames.contains(MyCircuit.getName())) {
			/* in the current implementation of logisim this should never happen, but we leave it in */
			Reporter.AddFatalError("Found more than one sheet in your design with the name :\""
					+ MyCircuit.getName()
					+ "\". This is not allowed, please make sure that all sheets have a unique name!");
			DRCStatus |= DRC_ERROR;
		} else {
			Sheetnames.add(MyCircuit.getName());
		}
		/* Preparing stage */
		for (Component comp : MyCircuit.getNonWires()) {
			String ComponentName = comp.getFactory().getHDLName(
					comp.getAttributeSet());
			if (!CompName.contains(ComponentName)) {
				CompName.add(ComponentName);
			}
		}
		drc.clear();
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("HDL_noLabel"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_INSTANCE));
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("HDL_CompNameIsLabel"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_INSTANCE|SimpleDRCContainer.MARK_LABEL));
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("HDL_LabelInvalid"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_INSTANCE|SimpleDRCContainer.MARK_LABEL));
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("HDL_DuplicatedLabels"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_INSTANCE|SimpleDRCContainer.MARK_LABEL));
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("HDL_Tristate"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_INSTANCE));
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("HDL_unsupported"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_INSTANCE));
		for (Component comp : MyCircuit.getNonWires()) {
			/*
			 * Here we check if the components are supported for the HDL
			 * generation
			 */
			if (!comp.getFactory().HDLSupportedComponent(HDLIdentifier,
					comp.getAttributeSet())) {
				drc.get(5).AddMarkComponent(comp);
				DRCStatus |= DRC_ERROR;
			}
			/*
			 * we check that all components that require a non zero label
			 * (annotation) have a label set
			 */
			if (comp.getFactory().RequiresNonZeroLabel()) {
				String Label = CorrectLabel.getCorrectLabel(
						comp.getAttributeSet().getValue(StdAttr.LABEL)
								.toString()).toUpperCase();
				String ComponentName = comp.getFactory().getHDLName(
						comp.getAttributeSet());
				if (Label.isEmpty()) {
					drc.get(0).AddMarkComponent(comp);
					DRCStatus |= ANNOTATE_REQUIRED;
				} else {
					if (CompName.contains(Label)) {
						drc.get(1).AddMarkComponent(comp);
						DRCStatus |= DRC_ERROR;
					}
					if (!CorrectLabel.IsCorrectLabel(Label, HDLIdentifier)) {
						/* this should not happen anymore */
						drc.get(2).AddMarkComponent(comp);
						DRCStatus |= DRC_ERROR;
					}
					if (Labels.containsKey(Label)) {
						drc.get(3).AddMarkComponent(comp);
						drc.get(3).AddMarkComponent(Labels.get(Label));
						DRCStatus |= DRC_ERROR;
					} else {
						Labels.put(Label, comp);
					}
				}
				if (comp.getFactory() instanceof SubcircuitFactory) {
					/* Special care has to be taken for sub-circuits */
					if (Label.equals(ComponentName.toUpperCase())) {
						drc.get(1).AddMarkComponent(comp);
						DRCStatus |= DRC_ERROR;
					}
					if (!CorrectLabel.IsCorrectLabel(comp.getFactory().getName(),
							HDLIdentifier, "Found that the component \""
									+ comp.getFactory().getName()
									+ "\" in circuit \"" + MyCircuit.getName(),
							Reporter)) {
						DRCStatus |= DRC_ERROR;
					}
//					SubcircuitFactory sub = (SubcircuitFactory) comp
//							.getFactory();
//					LocalNrOfInportBubles = LocalNrOfInportBubles
//							+ sub.getSubcircuit().getNetList()
//									.NumberOfInputBubbles();
//					LocalNrOfOutportBubles = LocalNrOfOutportBubles
//							+ sub.getSubcircuit().getNetList()
//									.NumberOfOutputBubbles();
//					LocalNrOfInOutBubles = LocalNrOfInOutBubles
//							+ sub.getSubcircuit().getNetList()
//									.NumberOfInOutBubbles();
				}
			}
			/* Now we check that no tri-state are present */
			if (comp.getFactory().HasThreeStateDrivers(comp.getAttributeSet())) {
				drc.get(4).AddMarkComponent(comp);
				DRCStatus |= DRC_ERROR;
			}
		}
		for (int i = 0 ; i < drc.size(); i++)
			if (drc.get(i).DRCInfoPresent())
				Reporter.AddError(drc.get(i));
		drc.clear();
		/* Here we have to quit as the netlist generation needs a clean tree */
		if ((DRCStatus|CommonDRCStatus) != DRC_PASSED) {
			return DRCStatus|CommonDRCStatus;
		}
		/*
		 * Okay we now know for sure that all elements are supported, lets build
		 * the net list
		 */
		Reporter.AddInfo("Building netlist for sheet \"" + MyCircuit.getName()
				+ "\"");
		if (!this.GenerateNetlist(Reporter, HDLIdentifier)) {
			this.clear();
			DRCStatus = DRC_ERROR;
			/* here we have to quit, as all the following steps depend on a proper netlist */
			return DRCStatus|CommonDRCStatus;
		}
		return DRC_PASSED;
	}
	
	private boolean GenerateNetlist(FPGAReport Reporter, String HDLIdentifier) {
		GridBagConstraints gbc = new GridBagConstraints();
		JFrame panel = new JFrame("Netlist: " + MyCircuit.getName());
		panel.setResizable(false);
		panel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		GridBagLayout thisLayout = new GridBagLayout();
		panel.setLayout(thisLayout);
		JLabel LocText = new JLabel("Generating Netlist for Circuit: "
				+ MyCircuit.getName());
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(LocText, gbc);
		JProgressBar progres = new JProgressBar(0, 7);
		progres.setValue(0);
		progres.setStringPainted(true);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(progres, gbc);
		panel.pack();
		panel.setLocation(Projects.getCenteredLoc(panel.getWidth(),
				panel.getHeight()));
		panel.setVisible(true);

		MyNets.clear();
		Rectangle ProgRect;
		Set<Wire> wires = new HashSet<Wire>(MyCircuit.getWires());
		Set<Component> comps = new HashSet<Component>(MyCircuit.getNonWires());
		String Error = "";
		/*
		 * FIRST PASS: In this pass we take all wire segments and see if they
		 * are connected to other segments. If they are connected we build a
		 * net. Note: The wires do not have any information on the bit-width.
		 */
		while (wires.size() != 0) {
			CircuitNet NewNet = new CircuitNet();
			GetNet(null, NewNet,wires);
			if (!NewNet.isEmpty()) {
				MyNets.add(NewNet);
			}
		}
		/* SECOND PASS: Detect all zero length wires; e.g. all components that are connected directly together */
		Map<Location,Integer> Points = new HashMap<Location,Integer>();
		for (Component comp : comps) {
			for (EndData end : comp.getEnds()) {
				Location loc = end.getLocation();
				if (Points.containsKey(loc)) {
					/* Found a connection already used */
					boolean newNet = true;
					for (CircuitNet net : MyNets) {
						if (net.contains(loc))
							newNet = false;
					}
					if (newNet) {
						int BitWidth = Points.get(loc);
						if (BitWidth == end.getWidth().getWidth()) {
							MyNets.add(new CircuitNet(loc));
						} else {
							Reporter.AddFatalError(CircuitName+": "+Strings.get("NetAdd_ComponentWidthMismatch"));
							return false;
						}
					}
				} else
					Points.put(loc,end.getWidth().getWidth());
			}
		}
		
		/* THIRD PASS: Now we process all tunnels and merge the tunneled nets */
		boolean TunnelsPresent = false;
		Iterator<Component> CompIterator = comps.iterator();
		Map<CircuitNet,Set<String>> TunnelInfo = new HashMap<CircuitNet,Set<String>>();
		while (CompIterator.hasNext()) {
			Component comp = CompIterator.next();
			if (comp.getFactory() instanceof Tunnel) {
				TunnelsPresent = true;
				List<EndData> ends = comp.getEnds();
				for (EndData end : ends) {
					for (CircuitNet ThisNet : MyNets) {
						if (ThisNet.contains(end.getLocation())) {
							/* found a connection */
							if (!TunnelInfo.containsKey(ThisNet)) {
								TunnelInfo.put(ThisNet, new HashSet<String>());
							}
							TunnelInfo.get(ThisNet).add(comp.getAttributeSet().getValue(StdAttr.LABEL).trim());
						}
					}
				}
				/* we remove the tunnel as it has been processed */
				CompIterator.remove();
			}
		}
		/* Here the merge takes place */
		if (TunnelsPresent) {
			Iterator<CircuitNet> MergeCandidates = TunnelInfo.keySet().iterator();
			while (MergeCandidates.hasNext()) {
				CircuitNet IterItem = MergeCandidates.next();
				boolean merged = false;
				for (CircuitNet CompItem : TunnelInfo.keySet()) {
					if (CompItem.equals(IterItem)||merged)
						continue;
					Set<String> CompTunnels = new HashSet<String>(TunnelInfo.get(CompItem));
					int size = CompTunnels.size();
					CompTunnels.removeAll(TunnelInfo.get(IterItem));
					if (size != CompTunnels.size()) {
						/* we have to merge */
						merged = true;
						/* First we merge IterItem with CompItem */
						if (!CompItem.merge(IterItem, Error, true)) {
							Reporter.AddFatalError(CircuitName+": "+Error);
							return false;
						}
						/* Now we update the set of CompItem's tunnel names */
						TunnelInfo.get(CompItem).addAll(TunnelInfo.get(IterItem));
						/* Now we remove it from my set of wires */
						MyNets.remove(IterItem);
					}
				}
				if (merged)
					MergeCandidates.remove();
			}
		}
		TunnelInfo.clear();
		
		/* At this point all net segments are build. All tunnels have been removed. There is still the processing of
		 * the splitters and the determination of the direction of the nets.
		 */
		progres.setValue(1);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		/* FOURTH PASS: Here the "wire widths" are determined, the connection to the components, and source/sink 
		 * 
		 */
		
		panel.dispose();
		return false; /* for the moment being, until I finished the complete netlist generator */
	}

	private void clear() {
		if (MyNets == null)
			MyNets = new HashSet<CircuitNet>();
		else
			MyNets.clear();
		DRCStatus = DRC_REQUIRED;
	}

	private static void GetNet(Wire wire, CircuitNet ThisNet, Set<Wire> wires) {
		Iterator<Wire> MyIterator = wires.iterator();
		ArrayList<Wire> MatchedWires = new ArrayList<Wire>();
		Wire CompWire = wire;
		while (MyIterator.hasNext()) {
			Wire ThisWire = MyIterator.next();
			if (CompWire == null) {
				CompWire = ThisWire;
				ThisNet.add(ThisWire);
				MyIterator.remove();
			} else if (ThisWire.sharesEnd(CompWire)) {
				MatchedWires.add(ThisWire);
				ThisNet.add(ThisWire);
				MyIterator.remove();
			}
		}
		for (Wire matched : MatchedWires)
			GetNet(matched,ThisNet,wires);
		MatchedWires.clear();
	}

}
