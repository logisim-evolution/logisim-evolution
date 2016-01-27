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
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.PortIO;
import com.cburch.logisim.std.io.ReptarLocalBus;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Tunnel;

public class Netlist {

	public class NetInfo {

		private Net TheNet;
		private byte BitIndex;

		public NetInfo(Net ConcernedNet, byte Index) {
			TheNet = ConcernedNet;
			BitIndex = Index;
		}

		public Byte getIndex() {
			return BitIndex;
		}

		public Net getNet() {
			return TheNet;
		}
	}

	private String CircuitName;
	private ArrayList<Net> MyNets = new ArrayList<Net>();
	private ArrayList<NetlistComponent> MySubCircuits = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyComponents = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyClockGenerators = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyInOutPorts = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyInputPorts = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyOutputPorts = new ArrayList<NetlistComponent>();
	private Integer LocalNrOfInportBubles;
	private Integer LocalNrOfOutportBubles;
	private Integer LocalNrOfInOutBubles;
	private ClockTreeFactory MyClockInformation = new ClockTreeFactory();
	private Circuit MyCircuit;
	private int DRCStatus;
	private Set<Wire> wires = new HashSet<Wire>();
	private ArrayList<String> CurrentHierarchyLevel;
	public static final int DRC_REQUIRED = -1;
	public static final int DRC_PASSED = 0;
	public static final int ANNOTATE_REQUIRED = 1;

	public static final int DRC_ERROR = 2;

	public Netlist(Circuit ThisCircuit) {
		MyCircuit = ThisCircuit;
		this.clear();
	}

	public void cleanClockTree(ClockSourceContainer ClockSources) {
		/* First pass, we cleanup all old information */
		MyClockInformation.clean();
		MyClockInformation.SetSourceContainer(ClockSources);
		/* Second pass, we go down the hierarchy */
		for (NetlistComponent sub : MySubCircuits) {
			SubcircuitFactory SubFact = (SubcircuitFactory) sub.GetComponent()
					.getFactory();
			SubFact.getSubcircuit().getNetList().cleanClockTree(ClockSources);
		}
	}

	public void clear() {
		DRCStatus = DRC_REQUIRED;
		MyNets.clear();
		MySubCircuits.clear();
		MyComponents.clear();
		MyClockGenerators.clear();
		MyInputPorts.clear();
		MyInOutPorts.clear();
		MyOutputPorts.clear();
		LocalNrOfInportBubles = 0;
		LocalNrOfOutportBubles = 0;
		LocalNrOfInOutBubles = 0;
		if (CurrentHierarchyLevel == null) {
			CurrentHierarchyLevel = new ArrayList<String>();
		} else {
			CurrentHierarchyLevel.clear();
		}
	}

	public void ConstructHierarchyTree(Set<String> ProcessedCircuits,
			ArrayList<String> HierarchyName, Integer GlobalInputID,
			Integer GlobalOutputID, Integer GlobalInOutID) {
		if (ProcessedCircuits == null) {
			ProcessedCircuits = new HashSet<String>();
		}
		/*
		 * The first step is to go down to the leaves and visit all involved
		 * sub-circuits to construct the local bubble information and form the
		 * Mappable components tree
		 */
		LocalNrOfInportBubles = 0;
		LocalNrOfOutportBubles = 0;
		LocalNrOfInOutBubles = 0;
		for (NetlistComponent comp : MySubCircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent()
					.getFactory();
			ArrayList<String> MyHierarchyName = new ArrayList<String>();
			MyHierarchyName.addAll(HierarchyName);
			MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
					.toString()));
			boolean FirstTime = !ProcessedCircuits.contains(sub.getName()
					.toString());
			if (FirstTime) {
				ProcessedCircuits.add(sub.getName());
				sub.getSubcircuit()
						.getNetList()
						.ConstructHierarchyTree(ProcessedCircuits,
								MyHierarchyName, GlobalInputID, GlobalOutputID,
								GlobalInOutID);
			}
			int subInputBubbles = sub.getSubcircuit().getNetList()
					.NumberOfInputBubbles();
			int subInOutBubbles = sub.getSubcircuit().getNetList()
					.NumberOfInOutBubbles();
			int subOutputBubbles = sub.getSubcircuit().getNetList()
					.NumberOfOutputBubbles();
			comp.SetLocalBubbleID(LocalNrOfInportBubles, subInputBubbles,
					LocalNrOfOutportBubles, subOutputBubbles,
					LocalNrOfInOutBubles, subInOutBubbles);
			LocalNrOfInportBubles += subInputBubbles;
			LocalNrOfInOutBubles += subInOutBubbles;
			LocalNrOfOutportBubles += subOutputBubbles;
			comp.AddGlobalBubbleID(MyHierarchyName, GlobalInputID,
					subInputBubbles, GlobalOutputID, subOutputBubbles,
					GlobalInOutID, subInOutBubbles);
			if (!FirstTime) {
				sub.getSubcircuit()
						.getNetList()
						.EnumerateGlobalBubbleTree(MyHierarchyName,
								GlobalInputID, GlobalOutputID, GlobalInOutID);
			}
			GlobalInputID += subInputBubbles;
			GlobalInOutID += subInOutBubbles;
			GlobalOutputID += subOutputBubbles;
		}
		/*
		 * Here we processed all sub-circuits of the local hierarchy level, now
		 * we have to process the IO components
		 */
		for (NetlistComponent comp : MyComponents) {
			if (comp.GetIOInformationContainer() != null) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(HierarchyName);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				int subInputBubbles = comp.GetIOInformationContainer()
						.GetNrOfInports();
				if (comp.GetComponent().getFactory() instanceof DipSwitch) {
					subInputBubbles = comp.GetComponent().getAttributeSet()
							.getValue(DipSwitch.ATTR_SIZE);
				}
				int subInOutBubbles = comp.GetIOInformationContainer()
						.GetNrOfInOutports();
				int subOutputBubbles = comp.GetIOInformationContainer()
						.GetNrOfOutports();
				comp.SetLocalBubbleID(LocalNrOfInportBubles, subInputBubbles,
						LocalNrOfOutportBubles, subOutputBubbles,
						LocalNrOfInOutBubles, subInOutBubbles);
				LocalNrOfInportBubles += subInputBubbles;
				LocalNrOfInOutBubles += subInOutBubbles;
				LocalNrOfOutportBubles += subOutputBubbles;
				comp.AddGlobalBubbleID(MyHierarchyName, GlobalInputID,
						subInputBubbles, GlobalOutputID, subOutputBubbles,
						GlobalInOutID, subInOutBubbles);
				GlobalInputID += subInputBubbles;
				GlobalInOutID += subInOutBubbles;
				GlobalOutputID += subOutputBubbles;
			}
		}
	}
	
	public void ClearNetlist() {
		for (Component comp : MyCircuit.getNonWires()) {
			if (comp.getFactory() instanceof SubcircuitFactory) {
				SubcircuitFactory fac = (SubcircuitFactory) comp.getFactory();
				fac.getSubcircuit().getNetList().ClearNetlist();
			}
		}
		this.clear();
	}

	public int DesignRuleCheckResult(FPGAReport Reporter, String HDLIdentifier,
			boolean IsTopLevel, ArrayList<String> Sheetnames) {
		ArrayList<String> CompName = new ArrayList<String>();
		ArrayList<Set<String>> AnnotationNames = new ArrayList<Set<String>>();
		/* Check if we are okay */
		if (DRCStatus == DRC_PASSED) {
			/* we have to go through our sub-circuits */
			for (NetlistComponent comp : MySubCircuits) {
				SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent()
						.getFactory();
				/* Here we recurse into the sub-circuits */
				DRCStatus = sub
						.getSubcircuit()
						.getNetList()
						.DesignRuleCheckResult(Reporter, HDLIdentifier, false,Sheetnames);
				if (DRCStatus != DRC_PASSED) {
					return DRCStatus;
				}
			}
			return DRC_PASSED;
		} else {
			/* There are changes, so we clean up the old information */
			this.clear();
		}
		/*
		 * Check for duplicated sheet names, this is bad as we will have
		 * multiple "different" components with the same name
		 */
		if (MyCircuit.getName().isEmpty()) {
			Reporter.AddFatalError("Found a sheet in your design with an empty name. This is not allowed, please specify a name!");
			return DRC_ERROR;
		}
		if (Sheetnames.contains(MyCircuit.getName())) {
			Reporter.AddFatalError("Found more than one sheet in your design with the name :\""
					+ MyCircuit.getName()
					+ "\". This is not allowed, please make sure that all sheets have a unique name!");
			return DRC_ERROR;
		} else {
			Sheetnames.add(MyCircuit.getName());
		}
		for (Component comp : MyCircuit.getNonWires()) {
			String ComponentName = comp.getFactory().getHDLName(
					comp.getAttributeSet());
			/*
			 * Here we check if the components are supported for the HDL
			 * generation
			 */
			if (!comp.getFactory().HDLSupportedComponent(HDLIdentifier,
					comp.getAttributeSet())) {
				Reporter.AddFatalError("Found unsupported component: \""
						+ comp.getFactory().getName() + "\" for "
						+ HDLIdentifier.toString()
						+ " generation in circuit : \"" + MyCircuit.getName()
						+ "\"");
				DRCStatus = DRC_ERROR;
				return DRCStatus;
			}
			if (comp.getFactory() instanceof SubcircuitFactory) {
				/* Special care has to be taken for sub-circuits */
				if (!CorrectLabel.IsCorrectLabel(comp.getFactory().getName(),
						HDLIdentifier, "Found that the component \""
								+ comp.getFactory().getName()
								+ "\" in circuit \"" + MyCircuit.getName(),
						Reporter)) {
					DRCStatus = DRC_ERROR;
					return DRCStatus;
				}
			}
			/* Now we add the name to the set if it is not already in */
			if (!CompName.contains(ComponentName)) {
				CompName.add(ComponentName);
				AnnotationNames.add(new HashSet<String>());
			}
		}
		for (Component comp : MyCircuit.getNonWires()) {
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
					Reporter.AddError("Component \""
							+ comp.getFactory().getName()
							+ "\" in sheet "
							+ MyCircuit.getName()
							+ " does not have a label! Run annotate or add labels");
					DRCStatus = ANNOTATE_REQUIRED;
					return DRCStatus;
				}
				if (CompName.contains(Label)) {
					Reporter.AddSevereError("Sheet \""
							+ MyCircuit.getName()
							+ "\" has one or more components with the name \""
							+ Label
							+ "\" and also components with a label of the same name. This is not supported!");
					DRCStatus = DRC_ERROR;
					return DRCStatus;
				}
				if (!CorrectLabel
						.IsCorrectLabel(Label, HDLIdentifier, "Component \""
								+ comp.getFactory().getName() + "\" in sheet "
								+ MyCircuit.getName() + " with label \""
								+ Label.toString(), Reporter)) {
					DRCStatus = DRC_ERROR;
					return DRCStatus;
				}
				if (AnnotationNames.get(CompName.indexOf(ComponentName))
						.contains(Label)) {
					Reporter.AddSevereError("Duplicated label \""
							+ comp.getAttributeSet().getValue(StdAttr.LABEL)
									.toString() + "\" found for component "
							+ comp.getFactory().getName() + " in sheet "
							+ MyCircuit.getName());
					DRCStatus = DRC_ERROR;
					return DRCStatus;
				} else {
					AnnotationNames.get(CompName.indexOf(ComponentName)).add(
							Label);
				}
				if (comp.getFactory() instanceof SubcircuitFactory) {
					/* Special care has to be taken for sub-circuits */
					if (Label.equals(ComponentName.toUpperCase())) {
						Reporter.AddError("Found that the component \""
								+ comp.getFactory().getName() + "\" in sheet "
								+ MyCircuit.getName() + " has a label that"
								+ " corresponds to the component name!");
						Reporter.AddError("Labels must be unique and may not correspond to the component name!");
						DRCStatus = DRC_ERROR;
						return DRCStatus;
					}
					if (CompName.contains(Label)) {
						Reporter.AddError("Subcircuit name "
								+ comp.getFactory().getName() + " in sheet "
								+ MyCircuit.getName()
								+ " is a reserved name; please rename!");
						DRCStatus = DRC_ERROR;
						return DRCStatus;
					}
					SubcircuitFactory sub = (SubcircuitFactory) comp
							.getFactory();
					/* Here we recurse into the sub-circuits */
					DRCStatus = sub
							.getSubcircuit()
							.getNetList()
							.DesignRuleCheckResult(Reporter, HDLIdentifier,
									false, Sheetnames);
					if (DRCStatus != DRC_PASSED) {
						return DRCStatus;
					}
					LocalNrOfInportBubles = LocalNrOfInportBubles
							+ sub.getSubcircuit().getNetList()
									.NumberOfInputBubbles();
					LocalNrOfOutportBubles = LocalNrOfOutportBubles
							+ sub.getSubcircuit().getNetList()
									.NumberOfOutputBubbles();
					LocalNrOfInOutBubles = LocalNrOfInOutBubles
							+ sub.getSubcircuit().getNetList()
									.NumberOfInOutBubbles();
				}
			}
			/* Now we check that no tri-state are present */
			if (comp.getFactory().HasThreeStateDrivers(comp.getAttributeSet())) {
				String Label = comp.getAttributeSet().getValue(StdAttr.LABEL)
						.toString();
				if (Label.isEmpty()) {
					Reporter.AddSevereError("Found a tri-state driver or floating output for component \""
							+ comp.getFactory().getName()
							+ "\" in sheet \""
							+ MyCircuit.getName()
							+ "\". Float and tri-states are not supported!");
				} else {
					Reporter.AddSevereError("Found a tri-state driver or floating output for component \""
							+ comp.getFactory().getName()
							+ "\" with label \""
							+ Label
							+ "\" in sheet \""
							+ MyCircuit.getName()
							+ "\". Float and tri-states are not supported!");

				}
				DRCStatus = DRC_ERROR;
				return DRCStatus;
			}
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
			return DRCStatus;
		}
		if (this.NetlistHasShortCircuits()) {
			Reporter.AddFatalError("Circuit \"" + MyCircuit.getName()
					+ "\" has short-circuits!");
			this.clear();
			DRCStatus = DRC_ERROR;
			return DRCStatus;
		}
		Reporter.AddInfo("Circuit \"" + MyCircuit.getName() + "\" has "
				+ this.NumberOfNets() + " nets and " + this.NumberOfBusses()
				+ " busses.");
		Reporter.AddInfo("Circuit \"" + MyCircuit.getName()
				+ "\" passed DRC check");
		/* Only if we are on the top-level we are going to build the clock-tree */
		if (IsTopLevel) {
			if (!DetectClockTree(Reporter)) {
				DRCStatus = DRC_ERROR;
				return DRCStatus;
			}
			ConstructHierarchyTree(null, new ArrayList<String>(),
					new Integer(0), new Integer(0), new Integer(0));
			int ports = NumberOfInputPorts() + NumberOfOutputPorts()
					+ LocalNrOfInportBubles + LocalNrOfOutportBubles
					+ LocalNrOfInOutBubles;
			if (ports == 0) {
				Reporter.AddFatalError("Toplevel \"" + MyCircuit.getName()
						+ "\" has no input(s) and/or no output(s)!");
				DRCStatus = DRC_ERROR;
				return DRCStatus;
			}
		}
		DRCStatus = DRC_PASSED;
		return DRCStatus;
	}

	private boolean DetectClockTree(FPGAReport Reporter) {
		/*
		 * First pass, we remove all information of previously detected
		 * clock-trees
		 */
		ClockSourceContainer ClockSources = MyClockInformation
				.GetSourceContainer();
		cleanClockTree(ClockSources);
		/* Second pass, we build the clock tree */
		ArrayList<Netlist> HierarchyNetlists = new ArrayList<Netlist>();
		HierarchyNetlists.add(this);
		return MarkClockSourceComponents(new ArrayList<String>(),
				HierarchyNetlists, ClockSources, Reporter);
	}

	/* Here all private handles are defined */
	private void EnumerateGlobalBubbleTree(ArrayList<String> HierarchyName,
			int StartInputID, int StartOutputID, int StartInOutID) {
		for (NetlistComponent comp : MySubCircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent()
					.getFactory();
			ArrayList<String> MyHierarchyName = new ArrayList<String>();
			MyHierarchyName.addAll(HierarchyName);
			MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
					.toString()));
			sub.getSubcircuit()
					.getNetList()
					.EnumerateGlobalBubbleTree(MyHierarchyName,
							StartInputID + comp.GetLocalBubbleInputStartId(),
							StartOutputID + comp.GetLocalBubbleOutputStartId(),
							StartInOutID + comp.GetLocalBubbleInOutStartId());
		}
		for (NetlistComponent comp : MyComponents) {
			if (comp.GetIOInformationContainer() != null) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(HierarchyName);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				int subInputBubbles = comp.GetIOInformationContainer()
						.GetNrOfInports();
				int subInOutBubbles = comp.GetIOInformationContainer()
						.GetNrOfInOutports();
				int subOutputBubbles = comp.GetIOInformationContainer()
						.GetNrOfOutports();
				comp.AddGlobalBubbleID(MyHierarchyName,
						StartInputID + comp.GetLocalBubbleInputStartId(),
						subInputBubbles,
						StartOutputID + comp.GetLocalBubbleOutputStartId(),
						subOutputBubbles, StartInOutID, subInOutBubbles);
			}
		}
	}

	private Net FindConnectedNet(Location loc) {
		for (Net Current : MyNets) {
			if (Current.contains(loc)) {
				return Current;
			}
		}
		return null;
	}

	private boolean GenerateNetlist(FPGAReport Reporter, String HDLIdentifier) {
		GridBagConstraints gbc = new GridBagConstraints();
		JFrame panel = new JFrame("Netlist: " + MyCircuit.getName());
		panel.setResizable(false);
		panel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		GridBagLayout thisLayout = new GridBagLayout();
		panel.setLayout(thisLayout);
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// panel.setLocation(mlocation.x, mlocation.y);
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

		CircuitName = MyCircuit.getName();
		wires.clear();
		wires.addAll(MyCircuit.getWires());
		/*
		 * FIRST PASS: In this pass we take all wire segments and see if they
		 * are connected to other segments. If they are connected we build a
		 * net.
		 */
		while (wires.size() != 0) {
			Net NewNet = new Net();
			GetNet(null, NewNet);
			if (!NewNet.isEmpty()) {
				MyNets.add(NewNet);
			}
		}
		/*
		 * Here we start to detect direct input-output component connections,
		 * read we detect "hidden" nets
		 */
		Set<Component> components = MyCircuit.getNonWires();
		/* we Start with the creation of an outputs list */
		Set<Location> OutputsList = new HashSet<Location>();
		Set<Location> InputsList = new HashSet<Location>();
		Set<Component> TunnelList = new HashSet<Component>();
		Set<Component> SplitterList = new HashSet<Component>();
		for (Component com : components) {
			/*
			 * We do not process the splitter and tunnel, they are processed
			 * later on
			 */
			if (com.getFactory() instanceof PortIO||
				com.getFactory() instanceof ReptarLocalBus)
				continue;
			else if (com.getFactory() instanceof SplitterFactory)
				SplitterList.add(com);
			else if (com.getFactory() instanceof Tunnel)
				TunnelList.add(com);
			else {
				List<EndData> ends = com.getEnds();
				for (EndData end : ends) {
					if (end.isInput() && end.isOutput()) {
						Reporter.AddFatalError("Detected INOUT pin on component \""
								+ com.getFactory().getName()
								+ "\" in circuit \""
								+ MyCircuit.getName()
								+ "\"!");
						this.clear();
						panel.dispose();
						return false;
					}
					if (end.isOutput()) {
						OutputsList.add(end.getLocation());
					} else {
						InputsList.add(end.getLocation());
					}
				}
			}
		}
		progres.setValue(1);
		Rectangle ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		/*
		 * Now we check if an input pin is connected to an output and in case of
		 * a Splitter if it is connected to either of them
		 */
		Set<Location> ZeroLengthNets = new HashSet<Location>();
		for (Component com : components) {
			if ((com.getFactory() instanceof SplitterFactory)
					|| (com.getFactory() instanceof Tunnel)) {
				List<EndData> ends = com.getEnds();
				for (EndData end : ends) {
					/* first we check for "normal" components */
					if (InputsList.contains(end.getLocation())
							|| OutputsList.contains(end.getLocation())) {
						/*
						 * We found a hidden Net. Let's check that it is not
						 * already contained in an existing net before adding it
						 * to the ZeroLengthNets Set
						 */
						boolean connected = false;
						for (Net net : MyNets) {
							connected |= net.contains(end.getLocation());
						}
						if (!connected) {
							ZeroLengthNets.add(end.getLocation());
						}
					}
					/* Now we have to detect inter Splitter/Tunnel connections */
					for (Component tun : TunnelList) {
						if (!tun.equals(com)) {
							List<EndData> tends = tun.getEnds();
							for (EndData thisend : tends) {
								if (thisend.getLocation().equals(
										end.getLocation())) {
									boolean connected = false;
									for (Net net : MyNets) {
										connected |= net.contains(end
												.getLocation());
									}
									if (!connected) {
										ZeroLengthNets.add(end.getLocation());
									}
								}
							}
						}
					}
					for (Component tun : SplitterList) {
						if (!tun.equals(com)) {
							List<EndData> tends = tun.getEnds();
							for (EndData thisend : tends) {
								if (thisend.getLocation().equals(
										end.getLocation())) {
									boolean connected = false;
									for (Net net : MyNets) {
										connected |= net.contains(end
												.getLocation());
									}
									if (!connected) {
										ZeroLengthNets.add(end.getLocation());
									}
								}
							}
						}
					}
				}
			} else {
				List<EndData> ends = com.getEnds();
				for (EndData end : ends) {
					if (end.isInput()
							&& OutputsList.contains(end.getLocation())) {
						/*
						 * We found a hidden Net. Let's check that it is not
						 * already contained in an existing net before adding it
						 * to the ZeroLengthNets Set
						 */
						boolean connected = false;
						for (Net net : MyNets) {
							connected |= net.contains(end.getLocation());
						}
						if (!connected) {
							ZeroLengthNets.add(end.getLocation());
						}
					}
				}
			}
		}
		InputsList.clear();
		OutputsList.clear();
		for (Location Loc : ZeroLengthNets) {
			Net NewNet = new Net(Loc);
			MyNets.add(NewNet);
		}
		progres.setValue(2);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		/*
		 * Here we are going to process the tunnels and possible merging of the
		 * tunneled nets
		 */
		boolean TunnelsPresent = false;
		for (Component com : TunnelList) {
			List<EndData> ends = com.getEnds();
			for (EndData end : ends) {
				for (Net ThisNet : MyNets) {
					if (ThisNet.contains(end.getLocation())) {
						ThisNet.addTunnel(com.getAttributeSet().getValue(
								StdAttr.LABEL));
						TunnelsPresent = true;
					}
				}
			}
		}
		if (TunnelsPresent) {
			Iterator<Net> NetIterator = MyNets.listIterator();
			while (NetIterator.hasNext()) {
				Net ThisNet = NetIterator.next();
				if (ThisNet.HasTunnel()
						&& (MyNets.indexOf(ThisNet) < (MyNets.size() - 1))) {
					boolean merged = false;
					Iterator<Net> SearchIterator = MyNets.listIterator(MyNets
							.indexOf(ThisNet) + 1);
					while (SearchIterator.hasNext() && !merged) {
						Net SearchNet = SearchIterator.next();
						for (String name : ThisNet.TunnelNames()) {
							if (SearchNet.ContainsTunnel(name) && !merged) {
								merged = true;
								SearchNet.merge(ThisNet);
							}
						}
					}
					if (merged) {
						NetIterator.remove();
					}
				}
			}
		}
		progres.setValue(3);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		/*
		 * Now all nets (connections) in the circuit are visible. Lets check for
		 * busses
		 */
		for (Component com : components) {
			List<EndData> ends = com.getEnds();
			for (EndData end : ends) {
				if (end.getWidth().getWidth() > 1) {
					/*
					 * We found a bus, let's check if this pin is connected to a
					 * net
					 */
					for (Net ThisNet : MyNets) {
						if (ThisNet.contains(end.getLocation())) {
							ThisNet.setBus(end.getWidth().getWidth());
						}
					}
				}
			}
		}
		progres.setValue(4);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		/*
		 * Finally we have to process the splitters to determine the bus
		 * hierarchy (if any)
		 */
		/*
		 * In this round we only process the evident splitters and remove them
		 * from the list
		 */
		Iterator<Component> MySplitters = SplitterList.iterator();
		while (MySplitters.hasNext()) {
			Component com = MySplitters.next();
			/*
			 * Currently by definition end(0) is the combined end of the
			 * splitter
			 */
			List<EndData> ends = com.getEnds();
			EndData CombinedEnd = ends.get(0);
			int RootNet = -1;
			/* We search for the root net in the list of nets */
			for (int i = 0; i < MyNets.size() && RootNet < 0; i++) {
				if (MyNets.get(i).contains(CombinedEnd.getLocation())) {
					RootNet = i;
				}
			}
			if (RootNet < 0) {
				Reporter.AddFatalError("Could not find the rootnet of a Splitter in circuit \""
						+ MyCircuit.getName() + "\"!");
				this.clear();
				panel.dispose();
				return false;
			}
			/*
			 * Now we process all the other ends to find the child busses/nets
			 * of this root bus
			 */
			ArrayList<Integer> Connections = new ArrayList<Integer>();
			for (int i = 1; i < ends.size(); i++) {
				EndData ThisEnd = ends.get(i);
				/* Find the connected net */
				int ConnectedNet = -1;
				for (int j = 0; j < MyNets.size() && ConnectedNet < 1; j++) {
					if (MyNets.get(j).contains(ThisEnd.getLocation())) {
						ConnectedNet = j;
					}
				}
				Connections.add(ConnectedNet);
			}
			for (int i = 1; i < ends.size(); i++) {
				int ConnectedNet = Connections.get(i - 1);
				if (ConnectedNet >= 0) {
					/* There is a net connected to this splitter's end point */
					if (!MyNets.get(ConnectedNet)
							.setParent(MyNets.get(RootNet))) {
						MyNets.get(ConnectedNet).ForceRootNet();
					}
					/* Here we have to process the inherited bits of the parent */
					byte[] BusBitConnection = ((Splitter) com).GetEndpoints();
					for (byte b = 0; b < BusBitConnection.length; b++) {
						if (BusBitConnection[b] == i) {
							MyNets.get(ConnectedNet).AddParrentBit(b);
						}
					}
				}
			}
		}
		progres.setValue(5);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		/*
		 * Now the complete netlist is created, we have to check that each
		 * net/bus entry has only 1 source and 1 or more sinks. If there exist
		 * more than 1 source we have a short circuit! We keep track of the
		 * sources and sinks at the root nets/buses
		 */
		for (Net ThisNet : MyNets) {
			if (ThisNet.IsRootNet()) {
				ThisNet.InitializeSourceSinks();
			}
		}
		/*
		 * We are going to iterate through all components and their respective
		 * pins to see if they are connected to a net, and if yes if they
		 * present a source or sink. We omit the splitter and tunnel as we
		 * already processed those
		 */
		for (Component comp : components) {
			if (comp.getFactory() instanceof SubcircuitFactory) {
				if (!ProcessSubcircuit(comp, Reporter)) {
					this.clear();
					panel.dispose();
					return false;
				}
			} else if ((comp.getFactory() instanceof Pin)
					|| (comp.getFactory().getIOInformation() != null)
					|| (comp.getFactory().getHDLGenerator(HDLIdentifier,
							comp.getAttributeSet()) != null)) {
				if (!ProcessNormalComponent(comp, Reporter)) {
					this.clear();
					panel.dispose();
					return false;
				}
			}
		}
		progres.setValue(6);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		/*
		 * Here we are going to process the complex splitters, note that in the
		 * previous handling of the splitters we marked all nets connected to a
		 * complex splitter with a forcerootnet annotation; we are going to
		 * cycle trough all these nets
		 */
		for (Net thisnet : MyNets) {
			if (thisnet.IsForcedRootNet()) {
				/* Cycle through all the bits of this net */
				for (int bit = 0; bit < thisnet.BitWidth(); bit++) {
					for (Component comp : SplitterList) {
						/*
						 * Currently by definition end(0) is the combined end of
						 * the splitter
						 */
						List<EndData> ends = comp.getEnds();
						EndData CombinedEnd = ends.get(0);
						int ConnectedBus = -1;
						/* We search for the root net in the list of nets */
						for (int i = 0; i < MyNets.size() && ConnectedBus < 0; i++) {
							if (MyNets.get(i).contains(
									CombinedEnd.getLocation())) {
								ConnectedBus = i;
							}
						}
						if (ConnectedBus < 0) {
							/*
							 * This should never happen as we already checked in
							 * the first pass
							 */
							Reporter.AddFatalError("Internal error!");
							this.clear();
							panel.dispose();
							return false;
						}
						for (int endid = 1; endid < ends.size(); endid++) {
							/*
							 * we iterate through all bits to see if the current
							 * net is connected to this splitter
							 */
							if (thisnet.contains(ends.get(endid).getLocation())) {
								/*
								 * first we have to get the bitindices of the
								 * rootbus
								 */
								/*
								 * Here we have to process the inherited bits of
								 * the parent
								 */
								byte[] BusBitConnection = ((Splitter) comp)
										.GetEndpoints();
								ArrayList<Byte> IndexBits = new ArrayList<Byte>();
								for (byte b = 0; b < BusBitConnection.length; b++) {
									if (BusBitConnection[b] == endid) {
										IndexBits.add(b);
									}
								}
								byte ConnectedBusIndex = IndexBits.get(bit);
								/* Figure out the rootbusid and rootbusindex */
								Net Rootbus = MyNets.get(ConnectedBus);
								while (!Rootbus.IsRootNet()) {
									ConnectedBusIndex = Rootbus
											.getBit(ConnectedBusIndex);
									Rootbus = Rootbus.getParent();
								}
								ConnectionPoint SolderPoint = new ConnectionPoint();
								SolderPoint.SetParrentNet(Rootbus,
										ConnectedBusIndex);
								Boolean IsSink = true;
								if (!thisnet.hasBitSource(bit)) {
									if (HasHiddenSource(Rootbus,
											ConnectedBusIndex, SplitterList,
											comp, new HashSet<String>())) {
										IsSink = false;
									}
								}
								if (IsSink) {
									thisnet.addSinkNet(bit, SolderPoint);
								} else {
									thisnet.addSourceNet(bit, SolderPoint);
								}
							}
						}
					}
				}
			}
		}
		progres.setValue(7);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		panel.dispose();
		/* So now we have all information we need! */
		return true;
	}

	public ArrayList<Component> GetAllClockSources() {
		return MyClockInformation.GetSourceContainer().getSources();
	}

	public ArrayList<Net> GetAllNets() {
		return MyNets;
	}

	public Circuit getCircuit() {
		return MyCircuit;
	}

	public String getCircuitName() {
		return CircuitName;
	}

	public int GetClockSourceId(ArrayList<String> HierarchyLevel, Net WhichNet,
			Byte Bitid) {
		return MyClockInformation.GetClockSourceId(HierarchyLevel, WhichNet,
				Bitid);
	}

	public int GetClockSourceId(Component comp) {
		return MyClockInformation.GetClockSourceId(comp);
	}

	public ArrayList<NetlistComponent> GetClockSources() {
		return MyClockGenerators;
	}

	public ArrayList<String> GetCurrentHierarchyLevel() {
		return CurrentHierarchyLevel;
	}

	public int GetEndIndex(NetlistComponent comp, String PinLabel,
			boolean IsOutputPort) {
		String label = CorrectLabel.getCorrectLabel(PinLabel);
		SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent()
				.getFactory();
		for (int end = 0; end < comp.NrOfEnds(); end++) {
			if (comp.getEnd(end).IsOutputEnd() == IsOutputPort) {
				if (comp.getEnd(end).GetConnection((byte) 0)
						.getChildsPortIndex() == sub.getSubcircuit()
						.getNetList().GetPortInfo(label)) {
					return end;
				}
			}
		}
		return -1;
	}

	private ArrayList<ConnectionPoint> GetHiddenSinks(Net thisNet,
			Byte bitIndex, Set<Component> SplitterList,
			Component ActiveSplitter, Set<String> HandledNets,
			Boolean isSourceNet) {
		ArrayList<ConnectionPoint> result = new ArrayList<ConnectionPoint>();
		/*
		 * to prevent deadlock situations we check if we already looked at this
		 * net
		 */
		String NetId = Integer.toString(MyNets.indexOf(thisNet)) + "-"
				+ Byte.toString(bitIndex);
		if (HandledNets.contains(NetId)) {
			return result;
		} else {
			HandledNets.add(NetId);
		}
		if (thisNet.hasBitSinks(bitIndex) && !isSourceNet) {
			ConnectionPoint SolderPoint = new ConnectionPoint();
			SolderPoint.SetParrentNet(thisNet, bitIndex);
			result.add(SolderPoint);
		}
		/* Check if we have a connection to another splitter */
		for (Component currentSplitter : SplitterList) {
			if (ActiveSplitter != null) {
				if (currentSplitter.equals(ActiveSplitter)) {
					continue;
				}
			}
			List<EndData> ends = currentSplitter.getEnds();
			for (byte end = 0; end < ends.size(); end++) {
				if (thisNet.contains(ends.get(end).getLocation())) {
					/* Here we have to process the inherited bits of the parent */
					byte[] BusBitConnection = ((Splitter) currentSplitter)
							.GetEndpoints();
					if (end == 0) {
						/* this is a main net, find the connected end */
						Byte SplitterEnd = BusBitConnection[bitIndex];
						/* Find the corresponding Net index */
						Byte Netindex = 0;
						for (int index = 0; index < bitIndex; index++) {
							if (BusBitConnection[index] == SplitterEnd) {
								Netindex++;
							}
						}
						/* Find the connected Net */
						Net SlaveNet = null;
						for (Net thisnet : MyNets) {
							if (thisnet.contains(ends.get(SplitterEnd)
									.getLocation())) {
								SlaveNet = thisnet;
							}
						}
						if (SlaveNet != null) {
							if (SlaveNet.IsRootNet()) {
								/* Trace down the slavenet */
								result.addAll(GetHiddenSinks(SlaveNet,
										Netindex, SplitterList,
										currentSplitter, HandledNets, false));
							} else {
								result.addAll(GetHiddenSinks(
										SlaveNet.getParent(),
										SlaveNet.getBit(Netindex),
										SplitterList, currentSplitter,
										HandledNets, false));
							}
						}
					} else {
						ArrayList<Byte> Rootindices = new ArrayList<Byte>();
						for (byte b = 0; b < BusBitConnection.length; b++) {
							if (BusBitConnection[b] == end) {
								Rootindices.add(b);
							}
						}
						Net RootNet = null;
						for (Net thisnet : MyNets) {
							if (thisnet.contains(currentSplitter.getEnd(0)
									.getLocation())) {
								RootNet = thisnet;
							}
						}
						if (RootNet != null) {
							if (RootNet.IsRootNet()) {
								result.addAll(GetHiddenSinks(RootNet,
										Rootindices.get(bitIndex),
										SplitterList, currentSplitter,
										HandledNets, false));
							} else {
								result.addAll(GetHiddenSinks(RootNet
										.getParent(), RootNet
										.getBit(Rootindices.get(bitIndex)),
										SplitterList, currentSplitter,
										HandledNets, false));
							}
						}
					}
				}
			}
		}
		return result;
	}

	public NetlistComponent GetInOutPin(int index) {
		if ((index < 0) || (index >= MyInOutPorts.size())) {
			return null;
		}
		return MyInOutPorts.get(index);
	}

	public NetlistComponent GetInOutPort(int Index) {
		if ((Index < 0) || (Index >= MyInOutPorts.size())) {
			return null;
		}
		return MyInOutPorts.get(Index);
	}

	public NetlistComponent GetInputPin(int index) {
		if ((index < 0) || (index >= MyInputPorts.size())) {
			return null;
		}
		return MyInputPorts.get(index);
	}

	public NetlistComponent GetInputPort(int Index) {
		if ((Index < 0) || (Index >= MyInputPorts.size())) {
			return null;
		}
		return MyInputPorts.get(Index);
	}

	public Map<ArrayList<String>, NetlistComponent> GetMappableResources(
			ArrayList<String> Hierarchy, boolean toplevel) {
		Map<ArrayList<String>, NetlistComponent> Components = new HashMap<ArrayList<String>, NetlistComponent>();
		/* First we search through my sub-circuits and add those IO components */
		for (NetlistComponent comp : MySubCircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent()
					.getFactory();
			ArrayList<String> MyHierarchyName = new ArrayList<String>();
			MyHierarchyName.addAll(Hierarchy);
			MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
					.toString()));
			Components.putAll(sub.getSubcircuit().getNetList()
					.GetMappableResources(MyHierarchyName, false));
		}
		/* Now we search for all local IO components */
		for (NetlistComponent comp : MyComponents) {
			if (comp.GetIOInformationContainer() != null) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(Hierarchy);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				Components.put(MyHierarchyName, comp);
			}
		}
		/* On the toplevel we have to add the pins */
		if (toplevel) {
			for (NetlistComponent comp : MyInputPorts) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(Hierarchy);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				Components.put(MyHierarchyName, comp);
			}
			for (NetlistComponent comp : MyInOutPorts) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(Hierarchy);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				Components.put(MyHierarchyName, comp);
			}
			for (NetlistComponent comp : MyOutputPorts) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(Hierarchy);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				Components.put(MyHierarchyName, comp);
			}
		}
		return Components;
	}

	private void GetNet(Wire wire, Net ThisNet) {
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
		for (int i = 0; i < MatchedWires.size(); i++) {
			GetNet(MatchedWires.get(i), ThisNet);
		}
		MatchedWires.clear();
	}

	public Integer GetNetId(Net selectedNet) {
		return MyNets.indexOf(selectedNet);
	}

	public ConnectionPoint GetNetlistConnectionForSubCircuit(String Label,
			int PortIndex, byte bitindex) {
		for (NetlistComponent search : MySubCircuits) {
			String CircuitLabel = CorrectLabel.getCorrectLabel(search
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
			if (CircuitLabel.equals(Label)) {
				/* Found the component, let's search the ends */
				for (int i = 0; i < search.NrOfEnds(); i++) {
					ConnectionEnd ThisEnd = search.getEnd(i);
					if (ThisEnd.IsOutputEnd()
							&& (bitindex < ThisEnd.NrOfBits())) {
						if (ThisEnd.GetConnection(bitindex)
								.getChildsPortIndex() == PortIndex) {
							return ThisEnd.GetConnection(bitindex);
						}
					}
				}
			}
		}
		return null;
	}

	public ArrayList<NetlistComponent> GetNormalComponents() {
		return MyComponents;
	}

	public NetlistComponent GetOutputPin(int index) {
		if ((index < 0) || (index >= MyOutputPorts.size())) {
			return null;
		}
		return MyOutputPorts.get(index);
	}

	public int GetPortInfo(String Label) {
		String Source = CorrectLabel.getCorrectLabel(Label);
		for (NetlistComponent Inport : MyInputPorts) {
			String Comp = CorrectLabel.getCorrectLabel(Inport.GetComponent()
					.getAttributeSet().getValue(StdAttr.LABEL));
			if (Comp.equals(Source)) {
				int index = MyInputPorts.indexOf(Inport);
				return index;
			}
		}
		for (NetlistComponent InOutport : MyInOutPorts) {
			String Comp = CorrectLabel.getCorrectLabel(InOutport.GetComponent()
					.getAttributeSet().getValue(StdAttr.LABEL));
			if (Comp.equals(Source)) {
				int index = MyInOutPorts.indexOf(InOutport);
				return index;
			}
		}
		for (NetlistComponent Outport : MyOutputPorts) {
			String Comp = CorrectLabel.getCorrectLabel(Outport.GetComponent()
					.getAttributeSet().getValue(StdAttr.LABEL));
			if (Comp.equals(Source)) {
				int index = MyOutputPorts.indexOf(Outport);
				return index;
			}
		}
		return -1;
	}

	private Net GetRootNet(Net Child) {
		if (Child == null) {
			return null;
		}
		if (Child.IsRootNet()) {
			return Child;
		}
		Net RootNet = Child.getParent();
		while (!RootNet.IsRootNet()) {
			RootNet = RootNet.getParent();
		}
		return RootNet;
	}

	private byte GetRootNetIndex(Net Child, byte BitIndex) {
		if (Child == null) {
			return -1;
		}
		if ((BitIndex < 0) || (BitIndex > Child.BitWidth())) {
			return -1;
		}
		if (Child.IsRootNet()) {
			return BitIndex;
		}
		Net RootNet = Child.getParent();
		Byte RootIndex = Child.getBit(BitIndex);
		while (!RootNet.IsRootNet()) {
			RootIndex = RootNet.getBit(RootIndex);
			RootNet = RootNet.getParent();
		}
		return RootIndex;
	}

	public Set<Splitter> getSplitters() {
		Set<Splitter> SplitterList = new HashSet<Splitter>();
		for (Component comp : MyCircuit.getNonWires()) {
			if (comp.getFactory() instanceof SplitterFactory) {
				SplitterList.add((Splitter) comp);
			}
		}
		return SplitterList;
	}

	public ArrayList<NetlistComponent> GetSubCircuits() {
		return MySubCircuits;
	}

	private boolean HasHiddenSource(Net thisNet, Byte bitIndex,
			Set<Component> SplitterList, Component ActiveSplitter,
			Set<String> HandledNets) {
		/*
		 * to prevent deadlock situations we check if we already looked at this
		 * net
		 */
		String NetId = Integer.toString(MyNets.indexOf(thisNet)) + "-"
				+ Byte.toString(bitIndex);
		if (HandledNets.contains(NetId)) {
			return false;
		} else {
			HandledNets.add(NetId);
		}
		if (thisNet.hasBitSource(bitIndex)) {
			return true;
		}
		/* Check if we have a connection to another splitter */
		for (Component currentSplitter : SplitterList) {
			if (currentSplitter.equals(ActiveSplitter)) {
				continue;
			}
			List<EndData> ends = currentSplitter.getEnds();
			for (byte end = 0; end < ends.size(); end++) {
				if (thisNet.contains(ends.get(end).getLocation())) {
					/* Here we have to process the inherited bits of the parent */
					byte[] BusBitConnection = ((Splitter) currentSplitter)
							.GetEndpoints();
					if (end == 0) {
						/* this is a main net, find the connected end */
						Byte SplitterEnd = BusBitConnection[bitIndex];
						/* Find the corresponding Net index */
						Byte Netindex = 0;
						for (int index = 0; index < bitIndex; index++) {
							if (BusBitConnection[index] == SplitterEnd) {
								Netindex++;
							}
						}
						/* Find the connected Net */
						Net SlaveNet = null;
						for (Net thisnet : MyNets) {
							if (thisnet.contains(ends.get(SplitterEnd)
									.getLocation())) {
								SlaveNet = thisnet;
							}
						}
						if (SlaveNet != null) {
							if (SlaveNet.IsRootNet()) {
								/* Trace down the slavenet */
								if (HasHiddenSource(SlaveNet, Netindex,
										SplitterList, currentSplitter,
										HandledNets)) {
									return true;
								}
							} else {
								if (HasHiddenSource(SlaveNet.getParent(),
										SlaveNet.getBit(Netindex),
										SplitterList, currentSplitter,
										HandledNets)) {
									return true;
								}
							}
						}
					} else {
						ArrayList<Byte> Rootindices = new ArrayList<Byte>();
						for (byte b = 0; b < BusBitConnection.length; b++) {
							if (BusBitConnection[b] == end) {
								Rootindices.add(b);
							}
						}
						Net RootNet = null;
						for (Net thisnet : MyNets) {
							if (thisnet.contains(currentSplitter.getEnd(0)
									.getLocation())) {
								RootNet = thisnet;
							}
						}
						if (RootNet != null) {
							if (RootNet.IsRootNet()) {
								if (HasHiddenSource(RootNet,
										Rootindices.get(bitIndex),
										SplitterList, currentSplitter,
										HandledNets)) {
									return true;
								}
							} else {
								if (HasHiddenSource(RootNet.getParent(),
										RootNet.getBit(Rootindices
												.get(bitIndex)), SplitterList,
										currentSplitter, HandledNets)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	public boolean IsContinuesBus(NetlistComponent comp, int EndIndex) {
		boolean ContinuesBus = true;
		if ((EndIndex < 0) || (EndIndex >= comp.NrOfEnds())) {
			return true;
		}
		ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
		int NrOfBits = ConnectionInformation.NrOfBits();
		if (NrOfBits == 1) {
			return true;
		}
		Net ConnectedNet = ConnectionInformation.GetConnection((byte) 0)
				.GetParrentNet();
		byte ConnectedNetIndex = ConnectionInformation.GetConnection((byte) 0)
				.GetParrentNetBitIndex();
		for (int i = 1; (i < NrOfBits) && ContinuesBus; i++) {
			if (ConnectedNet != ConnectionInformation.GetConnection((byte) i)
					.GetParrentNet()) {
				/* This bit is connected to another bus */
				ContinuesBus = false;
			}
			if ((ConnectedNetIndex + 1) != ConnectionInformation.GetConnection(
					(byte) i).GetParrentNetBitIndex()) {
				/* Connected to a none incremental position of the bus */
				ContinuesBus = false;
			} else {
				ConnectedNetIndex++;
			}
		}
		return ContinuesBus;
	}

	public boolean IsValid() {
		return DRCStatus == DRC_PASSED;
	}

	public void MarkClockNet(ArrayList<String> HierarchyNames,
			int clocksourceid, ConnectionPoint connection) {
		MyClockInformation.AddClockNet(HierarchyNames, clocksourceid,
				connection);
	}

	public boolean MarkClockSourceComponents(ArrayList<String> HierarchyNames,
			ArrayList<Netlist> HierarchyNetlists,
			ClockSourceContainer ClockSources, FPGAReport Reporter) {
		/* First pass: we go down the hierarchy till the leaves */
		for (NetlistComponent sub : MySubCircuits) {
			ArrayList<String> NewHierarchyNames = new ArrayList<String>();
			ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<Netlist>();
			NewHierarchyNames.addAll(HierarchyNames);
			NewHierarchyNames.add(CorrectLabel.getCorrectLabel(sub
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
			NewHierarchyNetlists.addAll(HierarchyNetlists);
			NewHierarchyNetlists.add(this);
			SubcircuitFactory SubFact = (SubcircuitFactory) sub.GetComponent()
					.getFactory();
			if (!SubFact
					.getSubcircuit()
					.getNetList()
					.MarkClockSourceComponents(NewHierarchyNames,
							NewHierarchyNetlists, ClockSources, Reporter)) {
				return false;
			}
		}
		/*
		 * We build the splitter list to be able to trace hidden sinks in the
		 * same time we see if some components require the Global fast FPGA
		 * clock
		 */
		Set<Component> SplitterList = new HashSet<Component>();
		for (Component comp : MyCircuit.getNonWires()) {
			if (comp.getFactory() instanceof SplitterFactory) {
				SplitterList.add(comp);
			}
			if (comp.getFactory().RequiresGlobalClock()) {
				ClockSources.SetGloblaClockRequirement();
			}
		}
		/* Second pass: We mark all clock sources */
		for (NetlistComponent source : MyClockGenerators) {
			if (source.NrOfEnds() != 1) {
				Reporter.AddFatalError("INTERNAL ERROR: Found a clock source with more than 1 connection");
				return false;
			}
			ConnectionEnd ClockConnection = source.getEnd(0);
			if (ClockConnection.NrOfBits() != 1) {
				Reporter.AddFatalError("INTERNAL ERROR: Found a clock source with a bus as output");
				return false;
			}
			ConnectionPoint SolderPoint = ClockConnection
					.GetConnection((byte) 0);
			/* Check if the clock source is connected */
			if (SolderPoint.GetParrentNet() != null) {
				/* Third pass: add this clock to the list of ClockSources */
				int clockid = ClockSources.getClockId(source.GetComponent());
				/* Forth pass: Add this source as clock source to the tree */
				MyClockInformation.AddClockSource(HierarchyNames, clockid,
						SolderPoint);
				/* Fifth pass: trace the clock net all the way */
				if (!TraceClockNet(SolderPoint.GetParrentNet(),
						SolderPoint.GetParrentNetBitIndex(), clockid,
						HierarchyNames, HierarchyNetlists, Reporter)) {
					return false;
				}
				/*
				 * Sixth pass: We have to account for the complex splitters;
				 * therefore we have also to trace through our own netlist to
				 * find the clock connections
				 */
				ArrayList<ConnectionPoint> HiddenSinks = GetHiddenSinks(
						SolderPoint.GetParrentNet(),
						SolderPoint.GetParrentNetBitIndex(), SplitterList,
						null, new HashSet<String>(), true);
				for (ConnectionPoint thisNet : HiddenSinks) {
					MarkClockNet(HierarchyNames, clockid, thisNet);
					if (!TraceClockNet(thisNet.GetParrentNet(),
							thisNet.GetParrentNetBitIndex(), clockid,
							HierarchyNames, HierarchyNetlists, Reporter)) {
						return false;
					}
				}
			}
		}
		/* Now we remove all non-root nets */
		/* Note that all clock sources have been marked */
		Iterator<Net> MyIterator = MyNets.iterator();
		while (MyIterator.hasNext()) {
			Net thisnet = MyIterator.next();
			if (!thisnet.IsRootNet()) {
				MyIterator.remove();
			} else {
				thisnet.FinalCleanup();
			}
		}
		return true;
	}

	public boolean NetlistHasShortCircuits() {
		boolean ret = false;
		for (Net net : MyNets) {
			if (net.IsRootNet()) {
				ret |= net.hasShortCircuit();
			}
		}
		return ret;
	}

	public int NumberOfBusses() {
		int nr_of_busses = 0;
		for (Net ThisNet : MyNets) {
			if (ThisNet.IsRootNet() && ThisNet.isBus()) {
				nr_of_busses++;
			}
		}
		return nr_of_busses;
	}

	public int NumberOfClockTrees() {
		return MyClockInformation.GetSourceContainer().getNrofSources();
	}

	public int NumberOfInOutBubbles() {
		return LocalNrOfInOutBubles;
	}

	public int NumberOfInOutPortBits() {
		int count = 0;
		for (NetlistComponent inp : MyInOutPorts) {
			count += inp.getEnd(0).NrOfBits();
		}
		return count;
	}

	public int NumberOfInOutPorts() {
		return MyInOutPorts.size();
	}

	public int NumberOfInputBubbles() {
		return LocalNrOfInportBubles;
	}

	public int NumberOfInputPortBits() {
		int count = 0;
		for (NetlistComponent inp : MyInputPorts) {
			count += inp.getEnd(0).NrOfBits();
		}
		return count;
	}

	public int NumberOfInputPorts() {
		return MyInputPorts.size();
	}

	public int NumberOfNets() {
		int nr_of_nets = 0;
		for (Net ThisNet : MyNets) {
			if (ThisNet.IsRootNet() && !ThisNet.isBus()) {
				nr_of_nets++;
			}
		}
		return nr_of_nets;
	}

	public int NumberOfOutputBubbles() {
		return LocalNrOfOutportBubles;
	}

	public int NumberOfOutputPortBits() {
		int count = 0;
		for (NetlistComponent outp : MyOutputPorts) {
			count += outp.getEnd(0).NrOfBits();
		}
		return count;
	}

	public int NumberOfOutputPorts() {
		return MyOutputPorts.size();
	}

	private boolean ProcessNormalComponent(Component comp, FPGAReport Reporter) {
		NetlistComponent NormalComponent = new NetlistComponent(comp);
		for (EndData ThisPin : comp.getEnds()) {
			if (ThisPin.isInput()
					&& ThisPin.isOutput()
					&& !(comp.getFactory() instanceof PortIO || comp
							.getFactory() instanceof ReptarLocalBus)) {
				Reporter.AddFatalError("Found IO pin on component \""
						+ comp.getFactory().getName() + "\" in circuit \""
						+ MyCircuit.getName() + "\"!");
				return false;
			}
			Net Connection = FindConnectedNet(ThisPin.getLocation());
			if (Connection != null) {
				int PinId = comp.getEnds().indexOf(ThisPin);
				boolean PinIsSink = ThisPin.isInput();
				ConnectionEnd ThisEnd = NormalComponent.getEnd(PinId);
				Net RootNet = GetRootNet(Connection);
				if (RootNet == null) {
					Reporter.AddFatalError("INTERNAL ERROR: Unable to find a root net!");
					return false;
				}
				for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
					Byte RootNetBitIndex = GetRootNetIndex(Connection, bitid);
					if (RootNetBitIndex < 0) {
						Reporter.AddFatalError("INTERNAL ERROR: Unable to find a root-net bit-index!");
						return false;
					}
					ConnectionPoint ThisSolderPoint = ThisEnd
							.GetConnection(bitid);
					ThisSolderPoint.SetParrentNet(RootNet, RootNetBitIndex);
					if (PinIsSink) {
						RootNet.addSink(RootNetBitIndex, ThisSolderPoint);
					} else {
						RootNet.addSource(RootNetBitIndex, ThisSolderPoint);
					}
				}
			}
		}
		if (comp.getFactory() instanceof Clock) {
			MyClockGenerators.add(NormalComponent);
		} else if (comp.getFactory() instanceof Pin) {
			if (comp.getEnd(0).isInput()) {
				MyOutputPorts.add(NormalComponent);
			} else {
				MyInputPorts.add(NormalComponent);
			}
		} else if (comp.getFactory() instanceof PortIO) {
			MyInOutPorts.add(NormalComponent);
			MyComponents.add(NormalComponent);
		} else if (comp.getFactory() instanceof ReptarLocalBus) {
			MyInOutPorts.add(NormalComponent);
			MyInputPorts.add(NormalComponent);
			MyOutputPorts.add(NormalComponent);
			MyComponents.add(NormalComponent);
		} else {
			MyComponents.add(NormalComponent);
		}
		return true;
	}

	private boolean ProcessSubcircuit(Component comp, FPGAReport Reporter) {
		NetlistComponent Subcircuit = new NetlistComponent(comp);
		SubcircuitFactory sub = (SubcircuitFactory) comp.getFactory();
		Instance[] subPins = ((CircuitAttributes) comp.getAttributeSet())
				.getPinInstances();
		Netlist subNetlist = sub.getSubcircuit().getNetList();
		for (EndData ThisPin : comp.getEnds()) {
			if (ThisPin.isInput() && ThisPin.isOutput()) {
				Reporter.AddFatalError("Found IO pin on component \""
						+ comp.getFactory().getName() + "\" in circuit \""
						+ MyCircuit.getName() + "\"! (subCirc)");
				return false;
			}
			Net Connection = FindConnectedNet(ThisPin.getLocation());
			int PinId = comp.getEnds().indexOf(ThisPin);
			int SubPortIndex = subNetlist.GetPortInfo(subPins[PinId]
					.getAttributeValue(StdAttr.LABEL));
			if (SubPortIndex < 0) {
				Reporter.AddFatalError("INTERNAL ERROR: Unable to find pin in sub-circuit!");
				return false;
			}
			if (Connection != null) {
				boolean PinIsSink = ThisPin.isInput();
				Net RootNet = GetRootNet(Connection);
				if (RootNet == null) {
					Reporter.AddFatalError("INTERNAL ERROR: Unable to find a root net!");
					return false;
				}
				for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
					Byte RootNetBitIndex = GetRootNetIndex(Connection, bitid);
					if (RootNetBitIndex < 0) {
						Reporter.AddFatalError("INTERNAL ERROR: Unable to find a root-net bit-index!");
						return false;
					}
					Subcircuit.getEnd(PinId).GetConnection(bitid)
							.SetParrentNet(RootNet, RootNetBitIndex);
					if (PinIsSink) {
						RootNet.addSink(RootNetBitIndex,
								Subcircuit.getEnd(PinId).GetConnection(bitid));
					} else {
						RootNet.addSource(RootNetBitIndex,
								Subcircuit.getEnd(PinId).GetConnection(bitid));
					}
					/*
					 * Special handling for sub-circuits; we have to find out
					 * the connection to the corresponding net in the underlying
					 * net-list; At this point the underlying net-lists have
					 * already been generated.
					 */
					Subcircuit.getEnd(PinId).GetConnection(bitid)
							.setChildsPortIndex(SubPortIndex);
				}
			} else {
				for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
					Subcircuit.getEnd(PinId).GetConnection(bitid)
							.setChildsPortIndex(SubPortIndex);
				}
			}
		}
		MySubCircuits.add(Subcircuit);
		return true;
	}

	public String projName() {
		return MyCircuit.getProjName();
	}

	public boolean RequiresGlobalClockConnection() {
		return MyClockInformation.GetSourceContainer()
				.RequiresFPGAGlobalClock();
	}

	public void SetCurrentHierarchyLevel(ArrayList<String> Level) {
		CurrentHierarchyLevel.clear();
		CurrentHierarchyLevel.addAll(Level);
	}

	public boolean TraceClockNet(Net ClockNet, byte ClockNetBitIndex,
			int ClockSourceId, ArrayList<String> HierarchyNames,
			ArrayList<Netlist> HierarchyNetlists, FPGAReport Reporter) {
		/* first pass, we check if the clock net goes down the hierarchy */
		for (NetlistComponent search : MySubCircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) search.GetComponent()
					.getFactory();
			for (ConnectionPoint SolderPoint : search.GetConnections(ClockNet,
					ClockNetBitIndex, false)) {
				if (SolderPoint.getChildsPortIndex() < 0) {
					Reporter.AddFatalError("INTERNAL ERROR: Subcircuit port is not annotated!");
					return false;
				}
				NetlistComponent InputPort = sub.getSubcircuit().getNetList()
						.GetInputPin(SolderPoint.getChildsPortIndex());
				if (InputPort == null) {
					Reporter.AddFatalError("INTERNAL ERROR: Unable to find Subcircuit input port!");
					return false;
				}
				byte BitIndex = search.GetConnectionBitIndex(ClockNet,
						ClockNetBitIndex);
				if (BitIndex < 0) {
					Reporter.AddFatalError("INTERNAL ERROR: Unable to find the bit index of a Subcircuit input port!");
					return false;
				}
				ConnectionPoint SubClockNet = InputPort.getEnd(0)
						.GetConnection(BitIndex);
				if (SubClockNet.GetParrentNet() == null) {
					/* we do not have a connected pin */
					continue;
				}
				ArrayList<String> NewHierarchyNames = new ArrayList<String>();
				ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<Netlist>();
				NewHierarchyNames.addAll(HierarchyNames);
				NewHierarchyNames.add(CorrectLabel.getCorrectLabel(search
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL)));
				NewHierarchyNetlists.addAll(HierarchyNetlists);
				NewHierarchyNetlists.add(this);
				sub.getSubcircuit()
						.getNetList()
						.MarkClockNet(NewHierarchyNames, ClockSourceId,
								SubClockNet);
				if (!sub.getSubcircuit()
						.getNetList()
						.TraceClockNet(SubClockNet.GetParrentNet(),
								SubClockNet.GetParrentNetBitIndex(),
								ClockSourceId, NewHierarchyNames,
								NewHierarchyNetlists, Reporter)) {
					return false;
				}
			}
		}
		/* second pass, we check if the clock net goes up the hierarchy */
		if (!HierarchyNames.isEmpty()) {
			for (NetlistComponent search : MyOutputPorts) {
				if (!search.GetConnections(ClockNet, ClockNetBitIndex, false)
						.isEmpty()) {
					byte bitindex = search.GetConnectionBitIndex(ClockNet,
							ClockNetBitIndex);
					ConnectionPoint SubClockNet = HierarchyNetlists
							.get(HierarchyNetlists.size() - 2)
							.GetNetlistConnectionForSubCircuit(
									HierarchyNames
											.get(HierarchyNames.size() - 1),
									MyOutputPorts.indexOf(search), bitindex);
					if (SubClockNet == null) {
						Reporter.AddFatalError("INTERNAL ERROR! Could not find a sub-circuit connection in overlying hierarchy level!");
						return false;
					}
					if (SubClockNet.GetParrentNet() == null) {
					} else {
						ArrayList<String> NewHierarchyNames = new ArrayList<String>();
						ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<Netlist>();
						NewHierarchyNames.addAll(HierarchyNames);
						NewHierarchyNames.remove(NewHierarchyNames.size() - 1);
						NewHierarchyNetlists.addAll(HierarchyNetlists);
						NewHierarchyNetlists
								.remove(NewHierarchyNetlists.size() - 1);
						HierarchyNetlists.get(HierarchyNetlists.size() - 2)
								.MarkClockNet(NewHierarchyNames, ClockSourceId,
										SubClockNet);
						if (!HierarchyNetlists
								.get(HierarchyNetlists.size() - 2)
								.TraceClockNet(SubClockNet.GetParrentNet(),
										SubClockNet.GetParrentNetBitIndex(),
										ClockSourceId, NewHierarchyNames,
										NewHierarchyNetlists, Reporter)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
}
