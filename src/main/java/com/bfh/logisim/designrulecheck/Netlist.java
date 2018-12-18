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
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.gui.FPGACliGuiFabric;
import com.bfh.logisim.gui.IFPGAFrame;
import com.bfh.logisim.gui.IFPGAGrid;
import com.bfh.logisim.gui.IFPGAGridLayout;
import com.bfh.logisim.gui.IFPGALabel;
import com.bfh.logisim.gui.IFPGAProgressBar;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.ReptarLocalBus;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Probe;
import com.cburch.logisim.std.wiring.Tunnel;

public class Netlist implements CircuitListener {

	@Override
	public void circuitChanged(CircuitEvent event) {
		int ev = event.getAction();
		if (event.getData() instanceof InstanceComponent) {
			InstanceComponent inst = (InstanceComponent) event.getData();
			if (event.getCircuit().equals(MyCircuit)) {
				switch (ev) {
				case CircuitEvent.ACTION_ADD:
					DRCStatus = DRC_REQUIRED;
					if (inst.getFactory() instanceof SubcircuitFactory) {
						SubcircuitFactory fac = (SubcircuitFactory) inst
								.getFactory();
						Circuit sub = fac.getSubcircuit();

						if (MySubCircuitMap.containsKey(sub)) {
							MySubCircuitMap.put(sub,
									MySubCircuitMap.get(sub) + 1);
						} else {
							MySubCircuitMap.put(sub, 1);
							sub.addCircuitListener(this);
						}
					}
					break;
				case CircuitEvent.ACTION_REMOVE:
					DRCStatus = DRC_REQUIRED;
					if (inst.getFactory() instanceof SubcircuitFactory) {
						SubcircuitFactory fac = (SubcircuitFactory) inst
								.getFactory();
						Circuit sub = fac.getSubcircuit();
						if (MySubCircuitMap.containsKey(sub)) {
							if (MySubCircuitMap.get(sub) == 1) {
								MySubCircuitMap.remove(sub);
								sub.removeCircuitListener(this);
							} else {
								MySubCircuitMap.put(sub,
										MySubCircuitMap.get(sub) - 1);
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

	private class SourceInfo {
		private ConnectionPoint source;
		private byte index;

		public SourceInfo(ConnectionPoint source,
				byte index) {
			this.source = source;
			this.index = index;
		}

		public Integer getIndex() {
			return (int) index;
		}

		public ConnectionPoint getSource() {
			return source;
		}
	}

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
	private Map<Circuit, Integer> MySubCircuitMap = new HashMap<Circuit, Integer>();
	private ArrayList<NetlistComponent> MySubCircuits = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyComponents = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyClockGenerators = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyInOutPorts = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyInputPorts = new ArrayList<NetlistComponent>();
	private ArrayList<NetlistComponent> MyOutputPorts = new ArrayList<NetlistComponent>();
	private ArrayList<Component> MyComplexSplitters = new ArrayList<Component>();
	private Integer LocalNrOfInportBubles;
	private Integer LocalNrOfOutportBubles;
	private Integer LocalNrOfInOutBubles;
	private ClockTreeFactory MyClockInformation = new ClockTreeFactory();
	private Circuit MyCircuit;
	private int DRCStatus;
	private Set<Wire> wires = new HashSet<Wire>();
	private ArrayList<String> CurrentHierarchyLevel;
	public static final int DRC_REQUIRED = 4;
	public static final int DRC_PASSED = 0;
	public static final int ANNOTATE_REQUIRED = 1;
	public static final int DRC_ERROR = 2;

	public static final Color DRC_INSTANCE_MARK_COLOR = Color.RED;
	public static final Color DRC_LABEL_MARK_COLOR = Color.MAGENTA;
	public static final Color DRC_WIRE_MARK_COLOR = Color.RED;

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
		for (NetlistComponent subcirc : MySubCircuits) {
			SubcircuitFactory SubFact = (SubcircuitFactory) subcirc.GetComponent().getFactory();
			SubFact.getSubcircuit().getNetList().clear();
		}
		DRCStatus = DRC_REQUIRED;
		MyNets.clear();
		MySubCircuits.clear();
		MyComponents.clear();
		MyClockGenerators.clear();
		MyInputPorts.clear();
		MyInOutPorts.clear();
		MyOutputPorts.clear();
		MyComplexSplitters.clear();
		LocalNrOfInportBubles = 0;
		LocalNrOfOutportBubles = 0;
		LocalNrOfInOutBubles = 0;
		if (CurrentHierarchyLevel == null) {
			CurrentHierarchyLevel = new ArrayList<String>();
		} else {
			CurrentHierarchyLevel.clear();
		}
	}

	public String getName() {
		if (MyCircuit!=null)
			return MyCircuit.getName();
		else
			return "Unknown";
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

	public int DesignRuleCheckResult(FPGAReport Reporter, String HDLIdentifier,
			boolean IsTopLevel, ArrayList<String> Sheetnames) {
		ArrayList<String> CompName = new ArrayList<String>();
		Map<String, Component> Labels = new HashMap<String, Component>();
		ArrayList<SimpleDRCContainer> drc = new ArrayList<SimpleDRCContainer>();
		int CommonDRCStatus = DRC_PASSED;
		/* First we go down the tree and get the DRC status of all sub-circuits */
		for (Circuit circ : MySubCircuitMap.keySet()) {
			CommonDRCStatus |= circ.getNetList().DesignRuleCheckResult(
					Reporter, HDLIdentifier, false, Sheetnames);
		}
		/* Check if we are okay */
		if (DRCStatus == DRC_PASSED) {
			return CommonDRCStatus;
		} else {
			/* There are changes, so we clean up the old information */
			clear();
			DRCStatus = DRC_PASSED; /*
			 * we mark already passed, if an error
			 * occurs the status is changed
			 */
		}
		/*
		 * Check for duplicated sheet names, this is bad as we will have
		 * multiple "different" components with the same name
		 */
		if (MyCircuit.getName().isEmpty()) {
			/*
			 * in the current implementation of logisim this should never
			 * happen, but we leave it in
			 */
			Reporter.AddFatalError("Found a sheet in your design with an empty name. This is not allowed, please specify a name!");
			DRCStatus |= DRC_ERROR;
		}
		if (Sheetnames.contains(MyCircuit.getName())) {
			/*
			 * in the current implementation of logisim this should never
			 * happen, but we leave it in
			 */
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
		drc.add(new SimpleDRCContainer(MyCircuit, Strings.get("HDL_noLabel"),
				SimpleDRCContainer.LEVEL_FATAL,
				SimpleDRCContainer.MARK_INSTANCE));
		drc.add(new SimpleDRCContainer(MyCircuit, Strings
				.get("HDL_CompNameIsLabel"), SimpleDRCContainer.LEVEL_FATAL,
				SimpleDRCContainer.MARK_INSTANCE
				| SimpleDRCContainer.MARK_LABEL));
		drc.add(new SimpleDRCContainer(MyCircuit, Strings
				.get("HDL_LabelInvalid"), SimpleDRCContainer.LEVEL_FATAL,
				SimpleDRCContainer.MARK_INSTANCE
				| SimpleDRCContainer.MARK_LABEL));
		drc.add(new SimpleDRCContainer(MyCircuit, Strings
				.get("HDL_DuplicatedLabels"), SimpleDRCContainer.LEVEL_FATAL,
				SimpleDRCContainer.MARK_INSTANCE
				| SimpleDRCContainer.MARK_LABEL));
		drc.add(new SimpleDRCContainer(MyCircuit, Strings.get("HDL_Tristate"),
				SimpleDRCContainer.LEVEL_FATAL,
				SimpleDRCContainer.MARK_INSTANCE));
		drc.add(new SimpleDRCContainer(MyCircuit, Strings
				.get("HDL_unsupported"), SimpleDRCContainer.LEVEL_FATAL,
				SimpleDRCContainer.MARK_INSTANCE));
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
					if (!CorrectLabel.IsCorrectLabel(comp.getFactory()
							.getName(), HDLIdentifier,
							"Found that the component \""
									+ comp.getFactory().getName()
									+ "\" in circuit \"" + MyCircuit.getName(),
									Reporter)) {
						DRCStatus |= DRC_ERROR;
					}
					SubcircuitFactory sub = (SubcircuitFactory) comp
							.getFactory();
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
				drc.get(4).AddMarkComponent(comp);
				DRCStatus |= DRC_ERROR;
			}
		}
		for (int i = 0; i < drc.size(); i++)
			if (drc.get(i).DRCInfoPresent())
				Reporter.AddError(drc.get(i));
		drc.clear();
		/* Here we have to quit as the netlist generation needs a clean tree */
		if ((DRCStatus | CommonDRCStatus) != DRC_PASSED) {
			return DRCStatus | CommonDRCStatus;
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
			/*
			 * here we have to quit, as all the following steps depend on a
			 * proper netlist
			 */
			return DRCStatus | CommonDRCStatus;
		}
		if (NetlistHasShortCircuits(Reporter)) {
			clear();
			DRCStatus = DRC_ERROR;
			return DRCStatus | CommonDRCStatus;
		}
		/* Check for connections without a source */
		NetlistHasSinksWithoutSource(Reporter);
		/* Check for unconnected input pins on components and generate warnings */
		for (NetlistComponent comp : MyComponents) {
			boolean openInputs = false;
			for (int j = 0 ; j < comp.NrOfEnds() ; j++) {
				if (comp.EndIsInput(j)&&!comp.EndIsConnected(j))
					openInputs = true;
			}
			if (openInputs) {
				SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
						Strings.get("NetList_UnconnectedInputs"),
						SimpleDRCContainer.LEVEL_NORMAL,
						SimpleDRCContainer.MARK_INSTANCE);
				warn.AddMarkComponent(comp.GetComponent());
				Reporter.AddWarning(warn);
			}
		}
		/* Check for unconnected input pins on subcircuits and generate warnings */
		for (NetlistComponent comp : MySubCircuits) {
			boolean openInputs = false;
			for (int j = 0 ; j < comp.NrOfEnds() ; j++) {
				if (comp.EndIsInput(j)&&!comp.EndIsConnected(j))
					openInputs = true;
			}
			if (openInputs) {
				SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
						Strings.get("NetList_UnconnectedInputs"),
						SimpleDRCContainer.LEVEL_SEVERE,
						SimpleDRCContainer.MARK_INSTANCE);
				warn.AddMarkComponent(comp.GetComponent());
				Reporter.AddWarning(warn);
			}
		}

		/* Only if we are on the top-level we are going to build the clock-tree */
		if (IsTopLevel) {
			if (!DetectClockTree(Reporter)) {
				DRCStatus = DRC_ERROR;
				return DRCStatus | CommonDRCStatus;
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
				return DRCStatus | CommonDRCStatus;
			}
			/* Check for gated clocks */
			if (!DetectGatedClocks(Reporter)) {
				DRCStatus = DRC_ERROR;
				return DRCStatus | CommonDRCStatus;
			}
		}

		Reporter.AddInfo("Circuit \"" + MyCircuit.getName() + "\" has "
				+ NumberOfNets() + " nets and " + NumberOfBusses()
				+ " busses.");
		Reporter.AddInfo("Circuit \"" + MyCircuit.getName()
		+ "\" passed DRC check");
		DRCStatus = DRC_PASSED;
		return DRCStatus | CommonDRCStatus;
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
		ArrayList<SimpleDRCContainer> drc = new ArrayList<SimpleDRCContainer>();
		boolean errors = false;
		IFPGAGrid gbc = FPGACliGuiFabric.getFPGAGrid();
		IFPGAFrame panel = FPGACliGuiFabric.getFPGAFrame("Netlist: " + MyCircuit.getName());
		panel.setResizable(false);
		panel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		IFPGAGridLayout thisLayout = FPGACliGuiFabric.getFPGAGridLayout();
		panel.setLayout(thisLayout);
		IFPGALabel LocText =FPGACliGuiFabric.getFPGALabel("Generating Netlist for Circuit: "
				+ MyCircuit.getName());
		gbc.setGridx(0);
		gbc.setGridy(1);
		gbc.setFill(GridBagConstraints.HORIZONTAL);
		panel.add(LocText, gbc);
		IFPGAProgressBar progres = FPGACliGuiFabric.getFPGAProgressBar(0, 7);
		progres.setValue(0);
		progres.setStringPainted(true);
		gbc.setGridx(0);
		gbc.setGridy(2);
		gbc.setFill(GridBagConstraints.HORIZONTAL);
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
		MyComplexSplitters.clear();
		drc.clear();
		drc.add(new SimpleDRCContainer(MyCircuit, Strings
				.get("NetList_IOError"), SimpleDRCContainer.LEVEL_FATAL,
				SimpleDRCContainer.MARK_INSTANCE));
		drc.add(new SimpleDRCContainer(MyCircuit, Strings.get("NetList_BitwidthError"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_WIRE));
		for (Component com : components) {
			/*
			 * We do not process the splitter and tunnel, they are processed
			 * later on
			 */
			boolean Ignore = false;

			/* In this case, the probe should not be synthetised:
			 * We could set the Probe as non-HDL element. But If we set the Probe
			 * as non HDL element, logisim will not allow user to download the design.
			 *
			 * In some case we need to use Logisim Simulation before running the design on the hardware.
			 * During simulation, probes are very helpful to see signals values. And when simulation is ok,
			 * the user does not want to delete all probes.
			 * Thus, here we remove it form the netlist so it is transparent.
			 */
			if(com.getFactory() instanceof Probe){
				continue;
			}

			if (com.getFactory() instanceof SplitterFactory) {
				MyComplexSplitters.add(com);
				Ignore = true;
			}
			if (com.getFactory() instanceof Tunnel) {
				TunnelList.add(com);
				Ignore = true;
			}

			//	if (com.getFactory() instanceof PortIO) {
			//		//TunnelList.add(com);
			//		Ignore = false;
			//	}

			List<EndData> ends = com.getEnds();
			for (EndData end : ends) {
				if (!Ignore) {
					if (end.isInput() && end.isOutput()) {
						/* The IO Port can be either output or input */
						//		if (!(com.getFactory() instanceof PortIO)) {
						//			drc.get(0).AddMarkComponent(com);
						//		}
					}
					else if (end.isOutput()) {
						OutputsList.add(end.getLocation());
					} else {
						InputsList.add(end.getLocation());
					}
				}
				/* Here we are going to mark the bitwidths on the nets */
				int width = end.getWidth().getWidth();
				Location loc = end.getLocation();
				//Collection<Component> component_verify = MyCircuit.getAllContaining(loc);
				for (Net ThisNet : MyNets) {
					if (ThisNet.contains(loc)) {
						if (!ThisNet.setWidth(width)){
							drc.get(1).AddMarkComponents(ThisNet.getWires());
						}
					}
				}
			}
		}
		for (int i = 0; i < drc.size(); i++) {
			if (drc.get(i).DRCInfoPresent()) {
				errors = true;
				Reporter.AddError(drc.get(i));
			}
		}
		if (errors) {
			panel.dispose();
			return false;
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
		drc.add(new SimpleDRCContainer(MyCircuit, Strings
				.get("NetAdd_ComponentWidthMismatch"),
				SimpleDRCContainer.LEVEL_FATAL,
				SimpleDRCContainer.MARK_INSTANCE));
		Map<Location, Integer> Points = new HashMap<Location, Integer>();
		for (Component comp : components) {
			for (EndData end : comp.getEnds()) {
				Location loc = end.getLocation();
				if (Points.containsKey(loc)) {
					/* Found a connection already used */
					boolean newNet = true;
					for (Net net : MyNets) {
						if (net.contains(loc))
							newNet = false;
					}
					if (newNet) {
						int BitWidth = Points.get(loc);
						if (BitWidth == end.getWidth().getWidth()) {
							MyNets.add(new Net(loc, BitWidth));
						} else {
							drc.get(0).AddMarkComponent(comp);
						}
					}
				} else
					Points.put(loc, end.getWidth().getWidth());
			}
		}
		if (drc.get(0).DRCInfoPresent()) {
			Reporter.AddError(drc.get(0));
			panel.dispose();
			return false;
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
		drc.clear();
		drc.add(new SimpleDRCContainer(MyCircuit, Strings.get("NetMerge_BitWidthError"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_WIRE));
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
								if (!SearchNet.merge(ThisNet)) {
									drc.get(0).AddMarkComponents(SearchNet.getWires());
									drc.get(0).AddMarkComponents(ThisNet.getWires());
								}
							}
						}
					}
					if (merged) {
						NetIterator.remove();
					}
				}
			}
		}
		if (drc.get(0).DRCInfoPresent()) {
			Reporter.AddError(drc.get(0));
			panel.dispose();
			return false;
		}
		progres.setValue(3);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);

		/* At this point all net segments are build. All tunnels have been removed. There is still the processing of
		 * the splitters and the determination of the direction of the nets.
		 */

		/* First we are going to check on duplicated splitters and remove them */
		Iterator<Component> MySplitIterator = MyComplexSplitters.listIterator();
		while (MySplitIterator.hasNext()) {
			Component ThisSplitter = MySplitIterator.next();
			if (MyComplexSplitters.indexOf(ThisSplitter)<(MyComplexSplitters.size()-1)) {
				boolean FoundDuplicate = false;
				Iterator<Component> SearchIterator = MyComplexSplitters.listIterator(MyComplexSplitters.indexOf(ThisSplitter)+1);
				while (SearchIterator.hasNext()&&!FoundDuplicate) {
					Component SearchSplitter = SearchIterator.next();
					if (SearchSplitter.getLocation().equals(ThisSplitter.getLocation())) {
						FoundDuplicate = true;
						for (int i = 0 ; i < SearchSplitter.getEnds().size();i++) {
							if (!SearchSplitter.getEnd(i).getLocation().equals(ThisSplitter.getEnd(i).getLocation()))
								FoundDuplicate = false;
						}
					}
				}
				if (FoundDuplicate) {
					SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
							Strings.get("NetList_duplicatedSplitter"),
							SimpleDRCContainer.LEVEL_SEVERE,
							SimpleDRCContainer.MARK_INSTANCE);
					warn.AddMarkComponent(ThisSplitter);
					Reporter.AddWarning(warn);
					MySplitIterator.remove();
				}
			}
		}

		/* In this round we are going to detect the unconnected nets meaning those having a width of 0 and remove them */
		drc.clear();
		Iterator<Net> NetIterator = MyNets.listIterator();
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("NetList_emptynets"),SimpleDRCContainer.LEVEL_NORMAL,SimpleDRCContainer.MARK_WIRE));
		while (NetIterator.hasNext()) {
			Net wire = NetIterator.next();
			if (wire.BitWidth()==0) {
				drc.get(0).AddMarkComponents(wire.getWires());
				NetIterator.remove();
			}
		}
		if (drc.get(0).DRCInfoPresent()) {
			Reporter.AddWarning(drc.get(0));
		}
		MySplitIterator = MyComplexSplitters.iterator();
		/* We also check quickly the splitters and remove the ones where input-bus is output-bus. We mark those who are not
		 * correctly connected and remove both versions from the set.
		 */
		drc.clear();
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("NetList_ShortCircuit"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_WIRE));
		errors = false;
		while (MySplitIterator.hasNext()) {
			Component mySplitter = MySplitIterator.next();
			int BusWidth = mySplitter.getEnd(0).getWidth().getWidth();
			List<EndData> myEnds = mySplitter.getEnds();
			int MaxFanoutWidth = 0;
			int index = -1;
			for (int i = 1 ; i < myEnds.size() ; i++) {
				int width = mySplitter.getEnd(i).getWidth().getWidth();
				if (width > MaxFanoutWidth) {
					MaxFanoutWidth = width;
					index = i;
				}
			}
			/* stupid situation first: the splitters bus connection is a single fanout */
			if (BusWidth == MaxFanoutWidth) {
				Net busnet = null;
				Net connectedNet = null;
				Location BusLoc = mySplitter.getEnd(0).getLocation();
				Location ConnectedLoc = mySplitter.getEnd(index).getLocation();
				boolean issueWarning = false;
				/* here we search for the nets */
				for (Net CurrentNet : MyNets) {
					if (CurrentNet.contains(BusLoc)) {
						if (busnet != null) {
							Reporter.AddFatalError("BUG: Multiple bus nets found for a single splitter\n ==> "+
									this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
							panel.dispose();
							return false;
						} else {
							busnet = CurrentNet;
						}
					}
					if (CurrentNet.contains(ConnectedLoc)) {
						if (connectedNet != null) {
							Reporter.AddFatalError("BUG: Multiple nets found for a single splitter split connection\n ==> "+
									this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
							panel.dispose();
							return false;
						} else {
							connectedNet = CurrentNet;
						}
					}
				}
				if (connectedNet != null) {
					if (busnet != null) {
						/* we can merge both nets */
						if (!busnet.merge(connectedNet)) {
							Reporter.AddFatalError("BUG: Splitter bus merge error\n ==> "+
									this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
							panel.dispose();
							return false;
						} else {
							MyNets.remove(MyNets.indexOf(connectedNet));
						}
					} else {
						issueWarning = true;
					}
				} else {
					issueWarning = true;
				}
				if (issueWarning) {
					SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
							Strings.get("NetList_NoSplitterConnection"),
							SimpleDRCContainer.LEVEL_SEVERE,
							SimpleDRCContainer.MARK_INSTANCE);
					warn.AddMarkComponent(mySplitter);
					Reporter.AddWarning(warn);
				}
				MySplitIterator.remove(); /* Does not exist anymore */
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
		Iterator<Component> MySplitters = MyComplexSplitters.iterator();
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
				Reporter.AddFatalError("BUG: Splitter without a bus connection\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
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
			boolean unconnectedEnds = false;
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
				} else {
					unconnectedEnds = true;
				}
			}
			if (unconnectedEnds) {
				SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
						Strings.get("NetList_NoSplitterEndConnections"),
						SimpleDRCContainer.LEVEL_NORMAL,
						SimpleDRCContainer.MARK_INSTANCE);
				warn.AddMarkComponent(com);
				Reporter.AddWarning(warn);
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
					for (Component comp : MyComplexSplitters) {
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
							Reporter.AddFatalError("BUG: This is embarasing as this should never happen\n ==> "+
									this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
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
								ConnectionPoint SolderPoint = new ConnectionPoint(comp);
								SolderPoint.SetParrentNet(Rootbus,
										ConnectedBusIndex);
								Boolean IsSink = true;
								if (!thisnet.hasBitSource(bit)) {
									if (HasHiddenSource(Rootbus,
											ConnectedBusIndex, MyComplexSplitters,
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

	private ArrayList<ConnectionPoint> GetHiddenSinkNets(Net thisNet,
			Byte bitIndex, ArrayList<Component> SplitterList,
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
			ConnectionPoint SolderPoint = new ConnectionPoint(null);
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
								result.addAll(GetHiddenSinkNets(SlaveNet,
										Netindex, SplitterList,
										currentSplitter, HandledNets, false));
							} else {
								result.addAll(GetHiddenSinkNets(
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
								result.addAll(GetHiddenSinkNets(RootNet,
										Rootindices.get(bitIndex),
										SplitterList, currentSplitter,
										HandledNets, false));
							} else {
								result.addAll(GetHiddenSinkNets(RootNet
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

	private ArrayList<ConnectionPoint> GetHiddenSinks(Net thisNet,
			Byte bitIndex, ArrayList<Component> SplitterList,
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
			result.addAll(thisNet.GetBitSinks(bitIndex));
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
		for (Wire matched : MatchedWires)
			GetNet(matched, ThisNet);
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

	public ConnectionPoint GetNetlistConnectionForSubCircuitInput(String Label,
			int PortIndex, byte bitindex) {
		for (NetlistComponent search : MySubCircuits) {
			String CircuitLabel = CorrectLabel.getCorrectLabel(search
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
			if (CircuitLabel.equals(Label)) {
				/* Found the component, let's search the ends */
				for (int i = 0; i < search.NrOfEnds(); i++) {
					ConnectionEnd ThisEnd = search.getEnd(i);
					if (!ThisEnd.IsOutputEnd()
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
		/* This may be cause bugs due to dual splitter on same location situations */
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

	private SourceInfo GetHiddenSource(Net thisNet, Byte bitIndex,
			List<Component> SplitterList, Component ActiveSplitter,
			Set<String> HandledNets, Set<Wire> Segments, FPGAReport Reporter) {
		/*
		 * to prevent deadlock situations we check if we already looked at this
		 * net
		 */
		String NetId = Integer.toString(MyNets.indexOf(thisNet)) + "-"
				+ Byte.toString(bitIndex);
		if (HandledNets.contains(NetId)) {
			return null;
		} else {
			HandledNets.add(NetId);
			Segments.addAll(thisNet.getWires());
		}
		if (thisNet.hasBitSource(bitIndex)) {
			List<ConnectionPoint> sources = thisNet.GetBitSources(bitIndex);
			if (sources.size()!= 1) {
				Reporter.AddFatalError("BUG: Found multiple sources\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return null;
			}
			return new SourceInfo(sources.get(0),bitIndex);
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
								SourceInfo ret = GetHiddenSource(SlaveNet, Netindex,
										SplitterList, currentSplitter,
										HandledNets,Segments,Reporter);
								if (ret != null)
									return ret;
							} else {
								SourceInfo ret = GetHiddenSource(SlaveNet.getParent(),
										SlaveNet.getBit(Netindex),
										SplitterList, currentSplitter,
										HandledNets,Segments,Reporter);
								if (ret != null)
									return ret;
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
								SourceInfo ret = GetHiddenSource(RootNet,
										Rootindices.get(bitIndex),
										SplitterList, currentSplitter,
										HandledNets,Segments,Reporter);
								if (ret != null)
									return ret;
							} else {
								SourceInfo ret = GetHiddenSource(RootNet.getParent(),
										RootNet.getBit(Rootindices
												.get(bitIndex)), SplitterList,
										currentSplitter, HandledNets,Segments,Reporter);
								if (ret != null)
									return ret;
							}
						}
					}
				}
			}
		}
		return null;
	}

	private boolean HasHiddenSource(Net thisNet, Byte bitIndex,
			List<Component> SplitterList, Component ActiveSplitter,
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
			SubcircuitFactory SubFact = (SubcircuitFactory) sub.GetComponent()
					.getFactory();
			NewHierarchyNames.addAll(HierarchyNames);
			NewHierarchyNames.add(CorrectLabel.getCorrectLabel(sub
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
			NewHierarchyNetlists.addAll(HierarchyNetlists);
			NewHierarchyNetlists.add(SubFact.getSubcircuit().getNetList());
			if (!SubFact
					.getSubcircuit()
					.getNetList()
					.MarkClockSourceComponents(NewHierarchyNames,
							NewHierarchyNetlists, ClockSources, Reporter)) {
				return false;
			}
		}
		/*
		 * We see if some components require the Global fast FPGA
		 * clock
		 */
		for (Component comp : MyCircuit.getNonWires()) {
			if (comp.getFactory().RequiresGlobalClock()) {
				ClockSources.SetGloblaClockRequirement();
			}
		}
		/* Second pass: We mark all clock sources */
		for (NetlistComponent ClockSource : MyClockGenerators) {
			if (ClockSource.NrOfEnds() != 1) {
				Reporter.AddFatalError("BUG: Found a clock source with more than 1 connection\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return false;
			}
			ConnectionEnd ClockConnection = ClockSource.getEnd(0);
			if (ClockConnection.NrOfBits() != 1) {
				Reporter.AddFatalError("BUG: Found a clock source with a bus as output\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return false;
			}
			ConnectionPoint SolderPoint = ClockConnection
					.GetConnection((byte) 0);
			/* Check if the clock source is connected */
			if (SolderPoint.GetParrentNet() != null) {
				/* Third pass: add this clock to the list of ClockSources */
				int clockid = ClockSources.getClockId(ClockSource.GetComponent());
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
				ArrayList<ConnectionPoint> HiddenSinks = GetHiddenSinkNets(
						SolderPoint.GetParrentNet(),
						SolderPoint.GetParrentNetBitIndex(), MyComplexSplitters,
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
		return true;
	}

	public boolean NetlistHasShortCircuits(FPGAReport Reporter) {
		boolean ret = false;
		for (Net net : MyNets) {
			if (net.IsRootNet()) {
				if (net.hasShortCircuit()) {
					SimpleDRCContainer error = new SimpleDRCContainer(MyCircuit,Strings.get("NetList_ShortCircuit"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_WIRE);
					error.AddMarkComponents(net.getWires());
					Reporter.AddError(error);
					ret = true;
				}
			}
		}
		return ret;
	}

	public boolean NetlistHasSinksWithoutSource(FPGAReport Reporter) {
		/* First pass: we make a set with all sinks */
		Set<ConnectionPoint> MySinks = new HashSet<ConnectionPoint>();
		for (Net ThisNet : MyNets) {
			if (ThisNet.IsRootNet()) {
				MySinks.addAll(ThisNet.GetSinks());
			}
		}
		/* Second pass: we iterate along all the sources */
		for (Net ThisNet : MyNets) {
			if (ThisNet.IsRootNet()) {
				for (int i = 0 ; i < ThisNet.BitWidth() ; i++) {
					if (ThisNet.hasBitSource(i)) {
						boolean HasSink = false;
						ArrayList<ConnectionPoint> Sinks = ThisNet.GetBitSinks(i);
						HasSink |= !Sinks.isEmpty();
						MySinks.removeAll(Sinks);
						ArrayList<ConnectionPoint> HiddenSinkNets = GetHiddenSinks(
								ThisNet, (byte) i, MyComplexSplitters,
								null, new HashSet<String>(), true);
						HasSink |= !HiddenSinkNets.isEmpty();
						MySinks.removeAll(HiddenSinkNets);
						if (!HasSink) {
							SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
									Strings.get("NetList_SourceWithoutSink"),
									SimpleDRCContainer.LEVEL_NORMAL,
									SimpleDRCContainer.MARK_WIRE);
							warn.AddMarkComponents(ThisNet.getWires());
							Reporter.AddWarning(warn);
						}
					}
				}
			}
		}
		if (MySinks.size()!= 0) {
			for (ConnectionPoint Sink : MySinks) {
				SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
						Strings.get("NetList_UnsourcedSink"),
						SimpleDRCContainer.LEVEL_SEVERE,
						SimpleDRCContainer.MARK_INSTANCE|SimpleDRCContainer.MARK_WIRE);
				warn.AddMarkComponents(Sink.GetParrentNet().getWires());
				if (Sink.GetComp()!=null) {
					warn.AddMarkComponent(Sink.GetComp());
				}
				Reporter.AddWarning(warn);
			}
		}
		return false;
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
			Net Connection = FindConnectedNet(ThisPin.getLocation());
			if (Connection != null) {
				int PinId = comp.getEnds().indexOf(ThisPin);
				boolean PinIsSink = ThisPin.isInput();
				ConnectionEnd ThisEnd = NormalComponent.getEnd(PinId);
				Net RootNet = GetRootNet(Connection);
				if (RootNet == null) {
					Reporter.AddFatalError("BUG: Unable to find a root net for a normal component\n ==> "+
							this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
					return false;
				}
				for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
					Byte RootNetBitIndex = GetRootNetIndex(Connection, bitid);
					if (RootNetBitIndex < 0) {
						Reporter.AddFatalError("BUG:  Unable to find a root-net bit-index for a normal component\n ==> "+
								this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
						return false;
					}
					ConnectionPoint ThisSolderPoint = ThisEnd.GetConnection(bitid);
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
			Net Connection = FindConnectedNet(ThisPin.getLocation());
			int PinId = comp.getEnds().indexOf(ThisPin);
			int SubPortIndex = subNetlist.GetPortInfo(subPins[PinId]
					.getAttributeValue(StdAttr.LABEL));
			if (SubPortIndex < 0) {
				Reporter.AddFatalError("BUG:  Unable to find pin in sub-circuit\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return false;
			}
			if (Connection != null) {
				boolean PinIsSink = ThisPin.isInput();
				Net RootNet = GetRootNet(Connection);
				if (RootNet == null) {
					Reporter.AddFatalError("BUG:  Unable to find a root net for sub-circuit\n ==> "+
							this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
					return false;
				}
				for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
					Byte RootNetBitIndex = GetRootNetIndex(Connection, bitid);
					if (RootNetBitIndex < 0) {
						Reporter.AddFatalError("BUG:  Unable to find a root-net bit-index for sub-circuit\n ==> "+
								this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
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
		for (NetlistComponent SubCirc : MySubCircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) SubCirc.GetComponent()
					.getFactory();
			for (ConnectionPoint SolderPoint : SubCirc.GetConnections(ClockNet,
					ClockNetBitIndex, false)) {
				if (SolderPoint.getChildsPortIndex() < 0) {
					Reporter.AddFatalError("BUG: Subcircuit port is not annotated!\n ==> "+
							this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
					return false;
				}
				NetlistComponent InputPort = sub.getSubcircuit().getNetList()
						.GetInputPin(SolderPoint.getChildsPortIndex());
				if (InputPort == null) {
					Reporter.AddFatalError("BUG: Unable to find Subcircuit input port!\n ==> "+
							this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
					return false;
				}
				byte BitIndex = SubCirc.GetConnectionBitIndex(ClockNet,
						ClockNetBitIndex);
				if (BitIndex < 0) {
					Reporter.AddFatalError("BUG: Unable to find the bit index of a Subcircuit input port!\n ==> "+
							this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
					return false;
				}
				ConnectionPoint SubClockNet = InputPort.getEnd(0)
						.GetConnection(BitIndex);
				if (SubClockNet.GetParrentNet() != null) {
					/* we have a connected pin */
					ArrayList<String> NewHierarchyNames = new ArrayList<String>();
					ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<Netlist>();
					NewHierarchyNames.addAll(HierarchyNames);
					NewHierarchyNames.add(CorrectLabel.getCorrectLabel(SubCirc
							.GetComponent().getAttributeSet()
							.getValue(StdAttr.LABEL)));
					NewHierarchyNetlists.addAll(HierarchyNetlists);
					NewHierarchyNetlists.add(sub.getSubcircuit().getNetList());
					sub.getSubcircuit()
					.getNetList()
					.MarkClockNet(NewHierarchyNames, ClockSourceId,SubClockNet);
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
		}
		/* second pass, we check if the clock net goes up the hierarchy */
		if (!HierarchyNames.isEmpty()) {
			for (NetlistComponent OutputPort : MyOutputPorts) {
				if (!OutputPort.GetConnections(ClockNet, ClockNetBitIndex, false).isEmpty()) {
					byte bitindex = OutputPort.GetConnectionBitIndex(ClockNet,
							ClockNetBitIndex);
					ConnectionPoint SubClockNet = HierarchyNetlists
							.get(HierarchyNetlists.size() - 2)
							.GetNetlistConnectionForSubCircuit(
									HierarchyNames
									.get(HierarchyNames.size() - 1),
									MyOutputPorts.indexOf(OutputPort), bitindex);
					if (SubClockNet == null) {
						Reporter.AddFatalError("BUG: Could not find a sub-circuit connection in overlying hierarchy level!\n ==> "+
								this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
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

	private boolean DetectGatedClocks(FPGAReport Reporter) {
		/* First Pass: We gather a complete information tree about components with clock inputs and their connected nets in
		 * case it is not a clock net. The moment we call this function the clock tree has been marked already !*/
		ArrayList<Netlist> root = new ArrayList<Netlist>();
		root.add(this);
		Map<String,Map<NetlistComponent,Circuit>> NotGatedSet = new HashMap<String,Map<NetlistComponent,Circuit>>();
		Map<String,Map<NetlistComponent,Circuit>> GatedSet = new HashMap<String,Map<NetlistComponent,Circuit>>();
		SetCurrentHierarchyLevel(new ArrayList<String>());
		GetGatedClockComponents(root,null,NotGatedSet,GatedSet,new HashSet<NetlistComponent>(),Reporter);
		boolean error = false;
		for (String key : NotGatedSet.keySet()) {
			if (GatedSet.keySet().contains(key)) {
				/* big Problem, we have a component that is used with and without gated clocks */
				error = true;
				Reporter.AddFatalError(Strings.get("NetList_CircuitGatedNotGated"));
				Reporter.AddErrorIncrement(Strings.get("NetList_TraceListBegin"));
				Map<NetlistComponent,Circuit> instances = NotGatedSet.get(key);
				for (NetlistComponent comp : instances.keySet()) {
					SimpleDRCContainer warn = new SimpleDRCContainer(instances.get(comp),
							Strings.get("NetList_CircuitNotGated"),
							SimpleDRCContainer.LEVEL_NORMAL,
							SimpleDRCContainer.MARK_INSTANCE,true);
					warn.AddMarkComponent(comp.GetComponent());
					Reporter.AddError(warn);
				}
				instances = GatedSet.get(key);
				for (NetlistComponent comp : instances.keySet()) {
					SimpleDRCContainer warn = new SimpleDRCContainer(instances.get(comp),
							Strings.get("NetList_CircuitGated"),
							SimpleDRCContainer.LEVEL_NORMAL,
							SimpleDRCContainer.MARK_INSTANCE,true);
					warn.AddMarkComponent(comp.GetComponent());
					Reporter.AddError(warn);
				}
				Reporter.AddErrorIncrement(Strings.get("NetList_TraceListEnd"));
			}
		}
		return !error;
	}

	public void GetGatedClockComponents(ArrayList<Netlist> HierarchyNetlists,
			NetlistComponent SubCircuit,
			Map<String,Map<NetlistComponent,Circuit>> NotGatedSet,
			Map<String,Map<NetlistComponent,Circuit>> GatedSet,
			Set<NetlistComponent> WarnedComponents,
			FPGAReport Reporter) {
		/* First pass: we go down the tree */
		for (NetlistComponent SubCirc : MySubCircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) SubCirc.GetComponent().getFactory();
			ArrayList<String> NewHierarchyNames = new ArrayList<String>();
			ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<Netlist>();
			NewHierarchyNames.addAll(GetCurrentHierarchyLevel());
			NewHierarchyNames.add(CorrectLabel.getCorrectLabel(SubCirc
					.GetComponent().getAttributeSet()
					.getValue(StdAttr.LABEL)));
			NewHierarchyNetlists.addAll(HierarchyNetlists);
			NewHierarchyNetlists.add(sub.getSubcircuit().getNetList());
			sub.getSubcircuit().getNetList().SetCurrentHierarchyLevel(NewHierarchyNames);
			sub.getSubcircuit().getNetList().GetGatedClockComponents(NewHierarchyNetlists,SubCirc,NotGatedSet,GatedSet,WarnedComponents,Reporter);
		}
		/* Second pass: we find all components with a clock input and see if they are connected to a clock */
		boolean GatedClock = false;
		List<SourceInfo> PinSources = new ArrayList<SourceInfo>();
		List<Set<Wire>> PinWires = new ArrayList<Set<Wire>>();
		List<Set<NetlistComponent>> PinGatedComponents = new ArrayList<Set<NetlistComponent>>();
		List<SourceInfo> NonPinSources = new ArrayList<SourceInfo>();
		List<Set<Wire>> NonPinWires = new ArrayList<Set<Wire>>();
		List<Set<NetlistComponent>> NonPinGatedComponents = new ArrayList<Set<NetlistComponent>>();
		for (NetlistComponent comp : MyComponents) {
			ComponentFactory fact = comp.GetComponent().getFactory();
			if (fact.CheckForGatedClocks(comp)) {
				int[] clockpins = fact.ClockPinIndex(comp);
				for (int i = 0 ; i < clockpins.length ; i++)
					GatedClock |= HasGatedClock(comp,clockpins[i],
						PinSources,PinWires,PinGatedComponents,
						NonPinSources,NonPinWires,NonPinGatedComponents,
						WarnedComponents,Reporter);
			}
		}
		/* We have two situations:
		 * 1) The gated clock net is generated locally, in this case we can mark them and add the current system to the non-gated set as
		 *    each instance will be equal at higher/lower levels.
		 * 2) The gated clock nets are connected to a pin, in this case each instance of this circuit could be either gated or non-gated,
		 *    we have to do something on the level higher and we mark this in the sets to be processed later.
		 */

		String MyName = CorrectLabel.getCorrectLabel(CircuitName);
		if (HierarchyNetlists.size()>1) {
			if (GatedClock&&PinSources.isEmpty()) {
				GatedClock = false; /* we have only non-pin driven gated clocks */
				WarningForGatedClock(NonPinSources,NonPinGatedComponents,NonPinWires,WarnedComponents,HierarchyNetlists,Reporter,Strings.get("NetList_GatedClock"));
			}

			if (GatedClock&&!PinSources.isEmpty()) {
				for (int i = 0 ; i < PinSources.size() ; i++) {
					Reporter.AddSevereWarning(Strings.get("NetList_GatedClock"));
					Reporter.AddWarningIncrement(Strings.get("NetList_TraceListBegin"));
					SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
							Strings.get("NetList_GatedClockSink"),
							SimpleDRCContainer.LEVEL_NORMAL,
							SimpleDRCContainer.MARK_INSTANCE|SimpleDRCContainer.MARK_WIRE,true);
					warn.AddMarkComponents(PinWires.get(i));
					for (NetlistComponent comp : PinGatedComponents.get(i))
						warn.AddMarkComponent(comp.GetComponent());
					Reporter.AddWarning(warn);
					WarningTraceForGatedClock(PinSources.get(i).getSource(),PinSources.get(i).getIndex(),HierarchyNetlists,CurrentHierarchyLevel,Reporter);
					Reporter.AddWarningIncrement(Strings.get("NetList_TraceListEnd"));
				}
			}

			/* we only mark if we are not at top-level */
			if (GatedClock) {
				if (GatedSet.containsKey(MyName))
					GatedSet.get(MyName).put(SubCircuit,HierarchyNetlists.get(HierarchyNetlists.size()-2).getCircuit());
				else {
					Map<NetlistComponent,Circuit> newList = new HashMap<NetlistComponent,Circuit>();
					newList.put(SubCircuit,HierarchyNetlists.get(HierarchyNetlists.size()-2).getCircuit());
					GatedSet.put(MyName,newList);
				}
			} else {
				if (NotGatedSet.containsKey(MyName))
					NotGatedSet.get(MyName).put(SubCircuit,HierarchyNetlists.get(HierarchyNetlists.size()-2).getCircuit());
				else {
					Map<NetlistComponent,Circuit> newList = new HashMap<NetlistComponent,Circuit>();
					newList.put(SubCircuit,HierarchyNetlists.get(HierarchyNetlists.size()-2).getCircuit());
					NotGatedSet.put(MyName,newList);
				}
			}
		} else {
			/* At toplevel we warn for all possible gated clocks */
			WarningForGatedClock(NonPinSources,NonPinGatedComponents,NonPinWires,WarnedComponents,HierarchyNetlists,Reporter,Strings.get("NetList_GatedClock"));
			WarningForGatedClock(PinSources,PinGatedComponents,PinWires,WarnedComponents,HierarchyNetlists,Reporter,Strings.get("NetList_PossibleGatedClock"));
		}
	}

	private boolean HasGatedClock(NetlistComponent comp,
			int ClockPinIndex,
			List<SourceInfo> PinSources,
			List<Set<Wire>> PinWires,
			List<Set<NetlistComponent>> PinGatedComponents,
			List<SourceInfo> NonPinSources,
			List<Set<Wire>> NonPinWires,
			List<Set<NetlistComponent>> NonPinGatedComponents,
			Set<NetlistComponent> WarnedComponents,
			FPGAReport Reporter) {
		boolean GatedClock = false;
		String ClockNetName = AbstractHDLGeneratorFactory.GetClockNetName(comp,ClockPinIndex, this);
		if (ClockNetName.isEmpty()) {
			/* we search for the source in case it is connected otherwise we ignore */
			ConnectionPoint connection = comp.getEnd(ClockPinIndex).GetConnection((byte) 0);
			Net connectedNet = connection.GetParrentNet();
			byte connectedNetindex = connection.GetParrentNetBitIndex();
			if (connectedNet != null) {
				GatedClock = true;
				if (connectedNet.IsForcedRootNet()) {
					Set<Wire> Segments = new HashSet<Wire>();
					Location loc = comp.GetComponent().getEnd(ClockPinIndex).getLocation();
					for (Net thisOne : MyNets)
						if (thisOne.contains(loc)) {
							if (!thisOne.IsRootNet())
								Segments.addAll(thisOne.getWires());
						}
					SourceInfo SourceList = GetHiddenSource(connectedNet, connectedNetindex,
							MyComplexSplitters, null,new HashSet<String>(),Segments,Reporter);
					ConnectionPoint source = SourceList.getSource();
					if (source.GetComp().getFactory() instanceof Pin) {
						int index = IndexOfEntry(PinSources,source,(int)connectedNetindex);
						if (index < 0) {
							PinSources.add(SourceList);
							PinWires.add(Segments);
							Set<NetlistComponent> comps = new HashSet<NetlistComponent>();
							comps.add(comp);
							comps.add(new NetlistComponent(source.GetComp()));
							PinGatedComponents.add(comps);
						} else {
							PinGatedComponents.get(index).add(comp);
						}
					} else {
						int index = IndexOfEntry(NonPinSources,source,(int)connectedNetindex);
						if (index < 0) {
							NonPinSources.add(SourceList);
							NonPinWires.add(Segments);
							Set<NetlistComponent> comps = new HashSet<NetlistComponent>();
							comps.add(comp);
							NonPinGatedComponents.add(comps);
						} else {
							NonPinGatedComponents.get(index).add(comp);
						}
					}
				} else {
					ArrayList<ConnectionPoint> SourceList = connectedNet.GetBitSources(connectedNetindex);
					if (SourceList.size()!= 1) {
						Reporter.AddFatalError("BUG: Found multiple sources\n ==> "+
								this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
						return GatedClock;
					}
					ConnectionPoint source = SourceList.get(0);
					if (source.GetComp().getFactory() instanceof Pin) {
						int index = IndexOfEntry(PinSources,source,(int)connectedNetindex);
						if (index < 0) {
							SourceInfo NewEntry = new SourceInfo(source,connectedNetindex);
							PinSources.add(NewEntry);
							PinWires.add(connectedNet.getWires());
							Set<NetlistComponent> comps = new HashSet<NetlistComponent>();
							comps.add(comp);
							PinGatedComponents.add(comps);
						} else {
							PinGatedComponents.get(index).add(comp);
						}
					} else {
						int index = IndexOfEntry(NonPinSources,source,(int)connectedNetindex);
						if (index < 0) {
							SourceInfo NewEntry = new SourceInfo(source,connectedNetindex);
							NonPinSources.add(NewEntry);
							NonPinWires.add(connectedNet.getWires());
							Set<NetlistComponent> comps = new HashSet<NetlistComponent>();
							comps.add(comp);
							NonPinGatedComponents.add(comps);
						} else {
							NonPinGatedComponents.get(index).add(comp);
						}
					}
				}
			} else {
				/* Add severe warning, we found a memory with an unconnected clock input */
				if (!WarnedComponents.contains(comp)) {
					SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
							Strings.get("NetList_NoClockConnection"),
							SimpleDRCContainer.LEVEL_SEVERE,
							SimpleDRCContainer.MARK_INSTANCE);
					warn.AddMarkComponent(comp.GetComponent());
					Reporter.AddWarning(warn);
					WarnedComponents.add(comp);
				}
			}
		}
		return GatedClock;
	}

	private int IndexOfEntry(List<SourceInfo> SearchList,
			ConnectionPoint Connection,
			Integer index) {
		int result = -1;
		for (int i = 0 ; i < SearchList.size() ; i++) {
			SourceInfo thisEntry = SearchList.get(i);
			if (thisEntry.getSource().equals(Connection)&&
					thisEntry.getIndex().equals(index))
				result = i;
		}
		return result;
	}

	private void WarningTraceForGatedClock(ConnectionPoint Source,
			int index,
			ArrayList<Netlist> HierarchyNetlists,
			ArrayList<String> HierarchyNames,
			FPGAReport Reporter) {

		Component comp = Source.GetComp();
		if (comp.getFactory() instanceof Pin) {
			if (HierarchyNames.isEmpty())
				/* we cannot go up at toplevel, so leave */
				return;
			int idx = -1;
			for (int i = 0 ; i < MyInputPorts.size() ; i++) {
				if (MyInputPorts.get(i).GetComponent().equals(comp))
					idx = i;
			}
			if (idx < 0) {
				Reporter.AddFatalError("BUG: Could not find port!\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return;
			}
			ConnectionPoint SubNet = HierarchyNetlists
					.get(HierarchyNetlists.size() - 2)
					.GetNetlistConnectionForSubCircuitInput(HierarchyNames.get(HierarchyNames.size() - 1),idx, (byte)index);
			if (SubNet == null) {
				Reporter.AddFatalError("BUG: Could not find a sub-circuit connection in overlying hierarchy level!\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return;
			}
			if (SubNet.GetParrentNet() != null) {
				ArrayList<String> NewHierarchyNames = new ArrayList<String>();
				ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<Netlist>();
				NewHierarchyNames.addAll(HierarchyNames);
				NewHierarchyNames.remove(NewHierarchyNames.size() - 1);
				NewHierarchyNetlists.addAll(HierarchyNetlists);
				NewHierarchyNetlists
				.remove(NewHierarchyNetlists.size() - 1);
				Netlist SubNetList = HierarchyNetlists.get(HierarchyNetlists.size() - 2);
				Net NewNet = SubNet.GetParrentNet();
				Byte NewNetIndex = SubNet.GetParrentNetBitIndex();
				Set<Wire> Segments = new HashSet<Wire>();
				SourceInfo source = SubNetList.GetHiddenSource(NewNet, NewNetIndex, SubNetList.MyComplexSplitters, null, new HashSet<String>(), Segments, Reporter);
				if (source==null) {
					Reporter.AddFatalError("BUG: Unable to find source in sub-circuit!\n ==> "+
							this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
					return;
				}
				ComponentFactory sfac = source.getSource().GetComp().getFactory();
				if (sfac instanceof Pin ||
						sfac instanceof SubcircuitFactory) {
					SimpleDRCContainer warn = new SimpleDRCContainer(SubNetList.getCircuit(),
							Strings.get("NetList_GatedClockInt"),
							SimpleDRCContainer.LEVEL_NORMAL,
							SimpleDRCContainer.MARK_WIRE,true);
					warn.AddMarkComponents(Segments);
					Reporter.AddWarning(warn);
					SubNetList.WarningTraceForGatedClock(source.getSource(),source.getIndex(),NewHierarchyNetlists,NewHierarchyNames,Reporter);
				} else {
					SimpleDRCContainer warn = new SimpleDRCContainer(SubNetList.getCircuit(),
							Strings.get("NetList_GatedClockSource"),
							SimpleDRCContainer.LEVEL_NORMAL,
							SimpleDRCContainer.MARK_WIRE,true);
					warn.AddMarkComponents(Segments);
					Reporter.AddWarning(warn);
				}
			}
		}
		if (comp.getFactory() instanceof SubcircuitFactory) {
			/* TODO */
			SubcircuitFactory sub = (SubcircuitFactory) comp.getFactory();
			if (Source.getChildsPortIndex() < 0) {
				Reporter.AddFatalError("BUG: Subcircuit port is not annotated!\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return;
			}
			NetlistComponent OutputPort = sub.getSubcircuit().getNetList()
					.GetOutputPin(Source.getChildsPortIndex());
			if (OutputPort == null) {
				Reporter.AddFatalError("BUG: Unable to find Subcircuit output port!\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return;
			}
			Net ConnectedNet = Source.GetParrentNet();
			/* Find the correct subcircuit */
			NetlistComponent SubCirc = null;
			for (NetlistComponent subc : MySubCircuits) {
				if (subc.GetComponent().equals(Source.GetComp()))
					SubCirc = subc;
			}
			if (SubCirc==null) {
				Reporter.AddFatalError("BUG: Unable to find Subcircuit!\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return;
			}
			byte BitIndex = SubCirc.GetConnectionBitIndex(ConnectedNet,(byte)index);
			if (BitIndex < 0) {
				Reporter.AddFatalError("BUG: Unable to find the bit index of a Subcircuit output port!\n ==> "+
						this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
				return;
			}
			ConnectionPoint SubNet = OutputPort.getEnd(0)
					.GetConnection(BitIndex);
			if (SubNet.GetParrentNet() != null) {
				/* we have a connected pin */
				Netlist SubNetList = sub.getSubcircuit().getNetList();
				ArrayList<String> NewHierarchyNames = new ArrayList<String>();
				ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<Netlist>();
				NewHierarchyNames.addAll(HierarchyNames);
				NewHierarchyNames.add(CorrectLabel.getCorrectLabel(SubCirc
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL)));
				NewHierarchyNetlists.addAll(HierarchyNetlists);
				NewHierarchyNetlists.add(SubNetList);
				Net NewNet = SubNet.GetParrentNet();
				Byte NewNetIndex = SubNet.GetParrentNetBitIndex();
				Set<Wire> Segments = new HashSet<Wire>();
				SourceInfo source = SubNetList.GetHiddenSource(NewNet, NewNetIndex, SubNetList.MyComplexSplitters, null, new HashSet<String>(), Segments, Reporter);
				if (source==null) {
					Reporter.AddFatalError("BUG: Unable to find source in sub-circuit!\n ==> "+
							this.getClass().getName().replaceAll("\\.","/")+":"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"\n");
					return;
				}
				ComponentFactory sfac = source.getSource().GetComp().getFactory();
				if (sfac instanceof Pin ||
						sfac instanceof SubcircuitFactory) {
					SimpleDRCContainer warn = new SimpleDRCContainer(SubNetList.getCircuit(),
							Strings.get("NetList_GatedClockInt"),
							SimpleDRCContainer.LEVEL_NORMAL,
							SimpleDRCContainer.MARK_WIRE,true);
					warn.AddMarkComponents(Segments);
					Reporter.AddWarning(warn);
					SubNetList.WarningTraceForGatedClock(source.getSource(),source.getIndex(),NewHierarchyNetlists,NewHierarchyNames,Reporter);
				} else {
					SimpleDRCContainer warn = new SimpleDRCContainer(SubNetList.getCircuit(),
							Strings.get("NetList_GatedClockSource"),
							SimpleDRCContainer.LEVEL_NORMAL,
							SimpleDRCContainer.MARK_WIRE,true);
					warn.AddMarkComponents(Segments);
					Reporter.AddWarning(warn);
				}
			}
		}
	}

	private void WarningForGatedClock(List<SourceInfo> Sources,
			List<Set<NetlistComponent>> Components,
			List<Set<Wire>> Wires,
			Set<NetlistComponent> WarnedComponents,
			ArrayList<Netlist> HierarchyNetlists,
			FPGAReport Reporter,
			String Warning) {
		for (int i = 0 ; i < Sources.size() ; i++) {
			boolean AlreadyWarned = false;
			for (NetlistComponent comp : Components.get(i))
				AlreadyWarned |= WarnedComponents.contains(comp);
			if (!AlreadyWarned) {
				if (Sources.get(i).getSource().GetComp().getFactory() instanceof SubcircuitFactory) {
					Reporter.AddSevereWarning(Strings.get("NetList_GatedClock"));
					Reporter.AddWarningIncrement(Strings.get("NetList_TraceListBegin"));
					SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
							Strings.get("NetList_GatedClockSink"),
							SimpleDRCContainer.LEVEL_NORMAL,
							SimpleDRCContainer.MARK_INSTANCE|SimpleDRCContainer.MARK_WIRE,true);
					warn.AddMarkComponents(Wires.get(i));
					for (NetlistComponent comp : Components.get(i))
						warn.AddMarkComponent(comp.GetComponent());
					Reporter.AddWarning(warn);
					WarningTraceForGatedClock(Sources.get(i).getSource(),Sources.get(i).getIndex(),HierarchyNetlists,CurrentHierarchyLevel,Reporter);
					Reporter.AddWarningIncrement(Strings.get("NetList_TraceListEnd"));
				} else {
					SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
							Warning,
							SimpleDRCContainer.LEVEL_SEVERE,
							SimpleDRCContainer.MARK_INSTANCE|SimpleDRCContainer.MARK_WIRE);
					for (NetlistComponent comp : Components.get(i))
						warn.AddMarkComponent(comp.GetComponent());
					warn.AddMarkComponents(Wires.get(i));
					Reporter.AddWarning(warn);
				}
				WarnedComponents.addAll(Components.get(i));
			}
		}
	}

	static public boolean IsFlipFlop(AttributeSet attrs) {
		if (attrs.containsAttribute(StdAttr.EDGE_TRIGGER))
			return true;
		if (attrs.containsAttribute(StdAttr.TRIGGER)) {
			return ((attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING) || (attrs
					.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING));
		}
		return false;
	}
}
