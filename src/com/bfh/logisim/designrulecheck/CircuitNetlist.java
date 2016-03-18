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
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.std.wiring.Clock;
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
	public static final Color DRC_WIRE_MARK_COLOR = Color.RED;

	private Map<Circuit,Integer> MySubCircuitMap; /* This is an important information as it contains all my subcircuits; it is handled by the CircuitListener */
	private Circuit MyCircuit;
	private String CircuitName;
	private Set<CircuitNet> MyNets;
	private Set<Component> MyUsedInputPins;
	private Set<Component> MyUsedOutputPins;
	private Set<Component> MyUsedSplitters;
	private Set<Component> MyUsedClockGenerators;
	private Set<Component> MyUsedBubbleComponents;
	private Set<Component> MyUsedSubCircuits;
	private Set<Component> MyUsedNormalComponents;
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
			CommonDRCStatus |= circ.getCircuitNetList().DesignRuleCheckResult(Reporter, HDLIdentifier, false, Sheetnames);
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
		ArrayList<SimpleDRCContainer> drc = new ArrayList<SimpleDRCContainer>();
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
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("NetAdd_ComponentWidthMismatch"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_INSTANCE));
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
							drc.get(0).AddMarkComponent(comp);
						}
					}
				} else
					Points.put(loc,end.getWidth().getWidth());
			}
		}
		if (drc.get(0).DRCInfoPresent()) {
			Reporter.AddError(drc.get(0));
			panel.dispose();
			return false;
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
							panel.dispose();
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
		drc.clear();
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("NetList_IOError"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_INSTANCE));
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("NetList_ShortCircuit"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_WIRE));
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("NetList_BitwidthError"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_WIRE|SimpleDRCContainer.MARK_INSTANCE));
		CompIterator = comps.iterator();
		boolean errors = false;
		while (CompIterator.hasNext()) {
			Component comp = CompIterator.next();
			/* first check: do we have an identified source/sink behavior, meaning the connections of the
			 *              component may not be of type inpunt and output.
			 */
			if (comp.getFactory() instanceof SplitterFactory) {
				/* The splitters are special and will be processed later on, at this stage we only mark the
				 * bitwidths on the connected nets and put them in a set
				 */
				if (!ProcessSplitter(comp,drc,Reporter))
					errors = true;
			} else {
				boolean found_io = false;
				for (EndData end : comp.getEnds()) {
					if (end.isInput()&end.isOutput()) {
						drc.get(0).AddMarkComponent(comp);
						found_io = true;
					}
				}
				if (!found_io) {
					if (comp.getFactory() instanceof Pin) {
						if (!ProcessPin(comp,drc,Reporter))
							errors = true;
					} else if (comp.getFactory() instanceof Clock) {
						/* A clock component has only a single connection and a single bit 
						 * at this stage we are only going to mark the connection, the processing of clock-nets is
						 * done at the root circuit at the end */
						if (!ProcessClock(comp,drc,Reporter))
							errors = true;
					} else {
						/* here all other components are handled */
						if (!ProcessComponent(comp,drc,Reporter))
							errors = true;
					}
				}
			}
		}
		for (int i = 0 ; i < drc.size(); i++) {
			if (drc.get(i).DRCInfoPresent()) {
				errors = true;
				Reporter.AddError(drc.get(i));
			}
		}
		if (errors) {
			panel.dispose();
			return false;
		}
		/* at this moment:
		 * 1) all connections have been marked. 
		 * 2) The connected nets have their bitwidths set.
		 * 3) All splitter connections have been marked.
		 * 4) All source and sink locations have been marked.
		 * 5) All components have been sorted by type.
		 */
		progres.setValue(2);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		/* FIFTH PASS: here we are going to remove unused nets and process splitters */
		drc.clear();
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("NetList_emptynets"),SimpleDRCContainer.LEVEL_NORMAL,SimpleDRCContainer.MARK_WIRE));
		Set<CircuitNet> SplitterNets = new HashSet<CircuitNet>();
		Iterator<CircuitNet> NetIterator = MyNets.iterator();
		while (NetIterator.hasNext()) {
			CircuitNet net = NetIterator.next();
			if (!net.BitWidthDefined()) {
				drc.get(0).AddMarkComponents(net.getSegments());
				NetIterator.remove();
			} else if (net.IsSplitterConnected()) {
				SplitterNets.add(net);
			}
		}
		if (drc.get(0).DRCInfoPresent())
			Reporter.AddWarning(drc.get(0));
		/* Here we prepare the splitter information */
		Map<Component,ArrayList<CircuitNet>> SplitterTree = new HashMap<Component,ArrayList<CircuitNet>>();
		for (Component mySplitter : MyUsedSplitters) {
			SplitterTree.put(mySplitter, new ArrayList<CircuitNet>());
			List<EndData> myEnds = mySplitter.getEnds();
			for (int i = 0 ; i < myEnds.size() ; i++)
				SplitterTree.get(mySplitter).add(null);
			for (EndData end : myEnds) {
				Location loc = end.getLocation();
				for (CircuitNet net : SplitterNets) {
					if (net.contains(loc)) {
						int idx = myEnds.indexOf(end);
						SplitterTree.get(mySplitter).set(idx,net);
					}
				}
			}
		}
		/* now we can interpret the splitter information */
		drc.clear();
		drc.add(new SimpleDRCContainer(MyCircuit,Strings.get("NetList_ShortCircuit"),SimpleDRCContainer.LEVEL_FATAL,SimpleDRCContainer.MARK_WIRE));
		errors = false;
		Iterator<Component> MySplitIterator = MyUsedSplitters.iterator();
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
				CircuitNet busnet = SplitterTree.get(mySplitter).get(0);
				CircuitNet connectedNet = SplitterTree.get(mySplitter).get(index);
				if (connectedNet != null) {
					/* we can merge both nets */
					String Message = "";
					if (busnet.hasSource()&&connectedNet.hasSource()) {
						drc.get(0).AddMarkComponents(busnet.getSegments());
						drc.get(0).AddMarkComponents(connectedNet.getSegments());
					} else {
						if (!busnet.merge(connectedNet,Message,false)) {
							Reporter.AddFatalError("BUG: "+Message+" :"+
									this.getClass().getName().replaceAll("\\.","/")+"\n");
							panel.dispose();
							return false;
						} else {
							/* At this stage only nets connected to input pins have a name, we have to
							 * preserve the name.
							 */
							if (connectedNet.hasName()) {
								if (!busnet.SetName(connectedNet.getName())) {
									Reporter.AddFatalError("BUG: Error Setting netname :"+
											this.getClass().getName().replaceAll("\\.","/")+"\n");
									panel.dispose();
									return false;
								}
							}
							MyNets.remove(connectedNet);
							for (Component comp : SplitterTree.keySet()) {
								for (int i = 0 ; i < comp.getEnds().size(); i++)
									if (SplitterTree.get(comp).get(i) != null &&
										SplitterTree.get(comp).get(i).equals(connectedNet))
										SplitterTree.get(comp).set(i, busnet);
							}
						}
					}
				} else {
					SimpleDRCContainer warn = new SimpleDRCContainer(MyCircuit,
							                                         Strings.get("NetList_NoSplitterConnection"),
							                                         SimpleDRCContainer.LEVEL_SEVERE,
							                                         SimpleDRCContainer.MARK_INSTANCE);
					warn.AddMarkComponent(mySplitter);
					Reporter.AddWarning(warn);
				}
				MySplitIterator.remove(); /* Does not exist anymore */
				continue;
			}
		}
		for (int i = 0 ; i < drc.size(); i++) {
			if (drc.get(i).DRCInfoPresent()) {
				errors = true;
				Reporter.AddError(drc.get(i));
			}
		}
		if (errors) {
			panel.dispose();
			return false;
		}
		/* At this stage all trivial merges have been done and the nets are "clean" it's time to
		 * build the connection tree as source and sink connections are still "simple" meaning for the
		 * busses all bit-indexes have the same source/sink. Going to optimize further at this stage would
		 * break this "simplicity" 
		 */
		progres.setValue(3);
		ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.paintImmediately(ProgRect);
		
		
		
		
		panel.dispose();
		Reporter.AddInfo("DONE for "+MyCircuit.getName()+" nr of nets : "+MyNets.size());

		drc.clear(); /* cleanup */
		return false; /* for the moment being, until I finished the complete netlist generator */
	}
	
	private boolean ProcessPin(Component comp,
			ArrayList<SimpleDRCContainer> drc,
			FPGAReport Reporter) {
		/* A pin has only one connection to process */
		boolean IsSource = comp.getEnd(0).isOutput();
		Location loc = comp.getEnd(0).getLocation();
		int bits = comp.getEnd(0).getWidth().getWidth();
		/* search the connected net */
		int connectioncount = 0;
		for (CircuitNet net : MyNets) {
			if (net.contains(loc)) {
				/* found the net */
				connectioncount++;
				if (net.hasSource()&&
						IsSource) {
					drc.get(1).AddMarkComponents(net.getSegments());
				} else {
					if (net.BitWidthDefined()&&
						(net.BitWidth()!=bits)) {
						drc.get(2).AddMarkComponent(comp);
						drc.get(2).AddMarkComponents(net.getSegments());
					} else {
						if (!net.SetPinConnection(comp, IsSource, bits, loc)) {
							Reporter.AddFatalError("BUG: adding pin :"+
									this.getClass().getName().replaceAll("\\.","/")+"\n");
							return false;
						}
						if (IsSource) {
							/* the sources set the name on the net only in case of pins */
							String Label = comp.getAttributeSet().getValue(StdAttr.LABEL);
							if (Label.isEmpty()) {
								Reporter.AddFatalError("BUG: empty label :"+
										this.getClass().getName().replaceAll("\\.","/")+"\n");
								return false;
							}
							net.SetName(Label);
							MyUsedInputPins.add(comp);
						} else {
							MyUsedOutputPins.add(comp);
						}
					}
				}
			}
		}
		if (connectioncount > 1) {
			Reporter.AddFatalError("BUG: multiple net connections on pin component in :"+
					this.getClass().getName().replaceAll("\\.","/")+"\n");
			return false;
		}
		return true;
	}
	
	private boolean ProcessSplitter(Component comp,
			ArrayList<SimpleDRCContainer> drc,
			FPGAReport Reporter) {
		boolean BusEndConnection = false;
		boolean SplitEndConnection = false;
		Set<CircuitNet> MyMarkedNets = new HashSet<CircuitNet>();
		Map<CircuitNet,Location> MyConnectedNets = new HashMap<CircuitNet,Location>();
		for (EndData end : comp.getEnds()) {
			Location loc = end.getLocation();
			int BitWidth = end.getWidth().getWidth();
			int nr_of_connections = 0;
			for (CircuitNet net : MyNets) {
				if (net.contains(loc)) {
					nr_of_connections++;
					MyConnectedNets.put(net, loc);
					if (comp.getEnds().indexOf(end)==0) 
						BusEndConnection = true;
					else
						SplitEndConnection = true;
					if (net.BitWidthDefined()) {
						if (net.BitWidth()!=BitWidth) {
							drc.get(2).AddMarkComponents(net.getSegments());
							drc.get(2).AddMarkComponent(comp);
						}
					} else {
						if (!net.setBitWidth(BitWidth)) {
							Reporter.AddFatalError("BUG: setting bitwidth :"+
									this.getClass().getName().replaceAll("\\.","/")+"\n");
							return false;
						} else {
							MyMarkedNets.add(net);
						}
					}
				}
			}
			if (nr_of_connections > 1) {
				Reporter.AddFatalError("BUG: multiple splitter connections :"+
						this.getClass().getName().replaceAll("\\.","/")+"\n");
				return false;
			}
		}
		if (BusEndConnection&&SplitEndConnection) {
			MyUsedSplitters.add(comp); /* it is connected in a proper way */
			for (CircuitNet net : MyConnectedNets.keySet()) {
				net.SetSplitterConnection(MyConnectedNets.get(net));
			}
		} else {
			/* improper connected splitter, we remove the marks */
			for (CircuitNet net : MyMarkedNets) {
				net.clearBitWidth();
			}
		}
		MyMarkedNets.clear(); /* cleanup */
		MyConnectedNets.clear();
		return true;
	}
	
	private boolean ProcessClock(Component comp,
			ArrayList<SimpleDRCContainer> drc,
			FPGAReport Reporter) {
		Location loc = comp.getEnd(0).getLocation();
		int nr_of_connections = 0;
		for (CircuitNet net : MyNets) {
			if (net.contains(loc)) {
				if (net.BitWidthDefined()&&net.BitWidth()!= 1) {
					drc.get(2).AddMarkComponents(net.getSegments());
					drc.get(2).AddMarkComponent(comp);
				} else
				if (net.hasSource()) {
					drc.get(1).AddMarkComponents(net.getSegments());
				} else {
					nr_of_connections++;
					if (!net.SetComponentConnection(comp, true, 1, loc)) {
						Reporter.AddFatalError("BUG: adding clock :"+
								this.getClass().getName().replaceAll("\\.","/")+"\n");
						return false;
					}
				}
			}
		}
		if (nr_of_connections > 1) {
			Reporter.AddFatalError("BUG: multiple clock connections :"+
					this.getClass().getName().replaceAll("\\.","/")+"\n");
			return false;
		}
		if (nr_of_connections==1)
			MyUsedClockGenerators.add(comp);
		return true;
	}
	
	private boolean ProcessComponent(Component comp,
			ArrayList<SimpleDRCContainer> drc,
			FPGAReport Reporter) {
		boolean hasconnection = false;
		for (EndData end : comp.getEnds()) {
			Location loc = end.getLocation();
			boolean IsSource = end.isOutput();
			int Bitwidth = end.getWidth().getWidth();
			int nr_of_connections = 0;
			for (CircuitNet net : MyNets) {
				if (net.contains(loc)) {
					nr_of_connections++;
					if (IsSource&&net.hasSource()) {
						drc.get(1).AddMarkComponents(net.getSegments());
					} else {
						if (net.BitWidthDefined()&&net.BitWidth()!=Bitwidth) {
							drc.get(2).AddMarkComponent(comp);
							drc.get(2).AddMarkComponents(net.getSegments());
						} else {
							if (!net.SetComponentConnection(comp, IsSource, Bitwidth, loc)) {
								Reporter.AddFatalError("BUG: adding "+comp.getFactory().getName()+" :"+
										this.getClass().getName().replaceAll("\\.","/")+"\n");
								return false;
							}
							hasconnection = true;
						}
					}
				}
			}
			if (nr_of_connections > 1) {
				Reporter.AddFatalError("BUG: multiple net connections on "+comp.getFactory().getName()+" component in :"+
						this.getClass().getName().replaceAll("\\.","/")+"\n");
				return false;
			}
		}
		if (hasconnection) {
			if (comp.getFactory() instanceof SubcircuitFactory) {
				MyUsedSubCircuits.add(comp);
			} else if (comp.getFactory().getIOInformation()!=null) {
				MyUsedBubbleComponents.add(comp);
			} else {
				MyUsedNormalComponents.add(comp);
			}
		}
		return true;
	}

	public void clear() {
		if (MyNets == null)
			MyNets = new HashSet<CircuitNet>();
		else
			MyNets.clear();
		if (MyUsedInputPins == null)
			MyUsedInputPins = new HashSet<Component>();
		else
			MyUsedInputPins.clear();
		if (MyUsedOutputPins == null)
			MyUsedOutputPins = new HashSet<Component>();
		else
			MyUsedOutputPins.clear();
		if (MyUsedSplitters == null)
			MyUsedSplitters = new HashSet<Component>();
		else
			MyUsedSplitters.clear();
		if (MyUsedClockGenerators == null)
			MyUsedClockGenerators = new HashSet<Component>();
		else
			MyUsedClockGenerators.clear();
		if (MyUsedBubbleComponents == null)
			MyUsedBubbleComponents = new HashSet<Component>();
		else
			MyUsedBubbleComponents.clear();
		if (MyUsedSubCircuits == null)
			MyUsedSubCircuits = new HashSet<Component>();
		else
			MyUsedSubCircuits.clear();
		if (MyUsedNormalComponents == null)
			MyUsedNormalComponents = new HashSet<Component>();
		else
			MyUsedNormalComponents.clear();
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
