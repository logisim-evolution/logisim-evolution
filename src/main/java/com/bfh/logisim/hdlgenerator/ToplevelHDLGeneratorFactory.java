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

package com.bfh.logisim.hdlgenerator;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.io.PortIO;
import com.cburch.logisim.std.io.ReptarLocalBus;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.std.wiring.Pin;

public class ToplevelHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

	private long FpgaClockFrequency;
	private double TickFrequency;
	private Circuit MyCircuit;
	private MappableResourcesContainer MyIOComponents;

	// private boolean useFPGAClock;

	public ToplevelHDLGeneratorFactory(long FPGAClock, double TickClock,
			Circuit TopLevel, MappableResourcesContainer IOComponents) {
		FpgaClockFrequency = FPGAClock;
		TickFrequency = TickClock;
		MyCircuit = TopLevel;
		MyIOComponents = IOComponents;
		// this.useFPGAClock = useFPGAClock;
	}

	@Override
	public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist,
			AttributeSet attrs) {
		ArrayList<String> Components = new ArrayList<String>();
		int NrOfClockTrees = TheNetlist.NumberOfClockTrees();
		if (NrOfClockTrees > 0) {
			TickComponentHDLGeneratorFactory Ticker = new TickComponentHDLGeneratorFactory(
					FpgaClockFrequency, TickFrequency/* , useFPGAClock */);
			Components
					.addAll(Ticker.GetComponentInstantiation(TheNetlist, null,
							Ticker.getComponentStringIdentifier(),
							VHDL/* , false */));
			HDLGeneratorFactory ClockWorker = TheNetlist
					.GetAllClockSources()
					.get(0)
					.getFactory()
					.getHDLGenerator(
							VHDL,
							TheNetlist.GetAllClockSources().get(0).getAttributeSet());
			Components.addAll(ClockWorker
					.GetComponentInstantiation(
							TheNetlist,
							TheNetlist.GetAllClockSources().get(0)
									.getAttributeSet(),
							TheNetlist
									.GetAllClockSources()
									.get(0)
									.getFactory()
									.getHDLName(
											TheNetlist.GetAllClockSources()
													.get(0).getAttributeSet()),
							VHDL/* , false */));
		}
		CircuitHDLGeneratorFactory Worker = new CircuitHDLGeneratorFactory(
				MyCircuit);
		// boolean hasLB = false;
		// for(NetlistComponent comp : TheNetlist.GetNormalComponents()){
		// if(comp.GetComponent().getFactory() instanceof ReptarLocalBus){
		// hasLB = true;
		// break;
		// }
		// }
		Components.addAll(Worker.GetComponentInstantiation(TheNetlist, null,
				CorrectLabel.getCorrectLabel(MyCircuit.getName()),
				VHDL/* , hasLB */));
		return Components;
	}

	@Override
	public String getComponentStringIdentifier() {
		return FPGAToplevelName;
	}

	@Override
	public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> InOut = new TreeMap<String, Integer>();
		for (int NrOfInOut = 0; NrOfInOut < MyIOComponents
				.GetNrOfToplevelInOutPins(); NrOfInOut++) {
			InOut.put(
					HDLGeneratorFactory.FPGAInOutPinName + "_"
							+ Integer.toString(NrOfInOut), 1);
		}
		return InOut;
	}

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
		int NrOfClockTrees = TheNetlist.NumberOfClockTrees();
		/* First we instantiate the Clock tree busses when present */
		if (NrOfClockTrees > 0 || TheNetlist.RequiresGlobalClockConnection()) {
			Inputs.put(TickComponentHDLGeneratorFactory.FPGAClock, 1);
		}
		for (int NrOfInputs = 0; NrOfInputs < MyIOComponents
				.GetNrOfToplevelInputPins(); NrOfInputs++) {
			Inputs.put(
					HDLGeneratorFactory.FPGAInputPinName + "_"
							+ Integer.toString(NrOfInputs), 1);
		}
		return Inputs;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		int NrOfClockTrees = TheNetlist.NumberOfClockTrees();
		String Preamble = (HDLType.equals(VHDL)) ? "" : "assign ";
		String BracketOpen = (HDLType.equals(VHDL)) ? "(" : "[";
		String BracketClose = (HDLType.equals(VHDL)) ? ")" : "]";
		String AssignOperator = (HDLType.equals(VHDL)) ? " <= "
				: " = ";
		String NotOperator = (HDLType.equals(VHDL)) ? "NOT " : "~";
		StringBuffer Temp = new StringBuffer();
		/* First we process all pins */
		Contents.addAll(MakeRemarkBlock(
				"Here all signal adaptations are performed", 3, HDLType));
		for (ArrayList<String> CompId : MyIOComponents.GetComponents()) {
			if (MyIOComponents.GetComponent(CompId).GetComponent().getFactory() instanceof Pin) {
				Component ThisPin = MyIOComponents.GetComponent(CompId)
						.GetComponent();
				ArrayList<String> MyMaps = MyIOComponents
						.GetMapNamesList(CompId);
				if (MyMaps == null) {
					Reporter.AddFatalError("Component has no map information, bizar! "
							+ CompId.toString());
					return Contents;
				}
				int PinPinId = 0;
				for (int MapOffset = 0; MapOffset < MyMaps.size(); MapOffset++) {
					String map = MyMaps.get(MapOffset);
					int InputId = MyIOComponents.GetFPGAInputPinId(map);
					int OutputId = MyIOComponents.GetFPGAOutputPinId(map);
					int NrOfPins = MyIOComponents.GetNrOfPins(map);
					boolean Invert = MyIOComponents.RequiresToplevelInversion(
							CompId, map);
					for (int PinId = 0; PinId < NrOfPins; PinId++) {
						Temp.setLength(0);
						Temp.append("   " + Preamble);
						if (InputId >= 0) {
							Temp.append("s_"
									+ CorrectLabel.getCorrectLabel(ThisPin
											.getAttributeSet().getValue(
													StdAttr.LABEL)));
							if (ThisPin.getEnd(0).getWidth().getWidth() > 1) {
								Temp.append(BracketOpen + PinPinId
										+ BracketClose);
							}
							PinPinId++;
							Temp.append(AssignOperator);
							if (Invert) {
								Temp.append(NotOperator);
							}
							Temp.append(HDLGeneratorFactory.FPGAInputPinName);
							Temp.append("_" + Integer.toString(InputId + PinId));
							Temp.append(";");
							Contents.add(Temp.toString());
						}
						if (OutputId >= 0) {
							Temp.append(HDLGeneratorFactory.FPGAOutputPinName);
							Temp.append("_"
									+ Integer.toString(OutputId + PinId));
							Temp.append(AssignOperator);
							if (Invert) {
								Temp.append(NotOperator);
							}
							Temp.append("s_"
									+ CorrectLabel.getCorrectLabel(ThisPin
											.getAttributeSet().getValue(
													StdAttr.LABEL)));
							if (ThisPin.getEnd(0).getWidth().getWidth() > 1) {
								Temp.append(BracketOpen + PinPinId
										+ BracketClose);
							}
							PinPinId++;
							Temp.append(";");
							Contents.add(Temp.toString());
						}
					}
				}
			}
		}
		/* Now we process the bubbles */
		Contents.addAll(MakeRemarkBlock(
				"Here all inlined adaptations are performed", 3, HDLType));
		for (ArrayList<String> CompId : MyIOComponents.GetComponents()) {
			if (!(MyIOComponents.GetComponent(CompId).GetComponent()
					.getFactory() instanceof Pin)
					&& !(MyIOComponents.GetComponent(CompId).GetComponent()
							.getFactory() instanceof PortIO)
					&& !(MyIOComponents.GetComponent(CompId).GetComponent()
							.getFactory() instanceof ReptarLocalBus)) {
				HDLGeneratorFactory Generator = MyIOComponents
						.GetComponent(CompId)
						.GetComponent()
						.getFactory()
						.getHDLGenerator(
								HDLType,
								MyIOComponents.GetComponent(CompId)
										.GetComponent().getAttributeSet());
				if (Generator == null) {
					Reporter.AddError("No generator for component "
							+ CompId.toString());
				} else {
					Contents.addAll(Generator.GetInlinedCode(HDLType, CompId,
							Reporter, MyIOComponents));
				}
			} else if (MyIOComponents.GetComponent(CompId).GetComponent()
					.getFactory() instanceof ReptarLocalBus) {
				((ReptarLocalBus) MyIOComponents.GetComponent(CompId)
						.GetComponent().getFactory())
						.setMapInfo(MyIOComponents);
			} else if (MyIOComponents.GetComponent(CompId).GetComponent()
					.getFactory() instanceof PortIO) {
				((PortIO) MyIOComponents.GetComponent(CompId).GetComponent()
						.getFactory()).setMapInfo(MyIOComponents);
			}
		}
		if (NrOfClockTrees > 0) {
			Contents.addAll(MakeRemarkBlock(
					"Here the clock tree components are defined", 3, HDLType));
			TickComponentHDLGeneratorFactory Ticker = new TickComponentHDLGeneratorFactory(
					FpgaClockFrequency, TickFrequency/* , useFPGAClock */);
			Contents.addAll(Ticker.GetComponentMap(null, (long) 0, null,
					Reporter, "", HDLType));
			long index = 0;
			for (Component Clockgen : TheNetlist.GetAllClockSources()) {
				NetlistComponent ThisClock = new NetlistComponent(Clockgen);
				Contents.addAll(Clockgen
						.getFactory()
						.getHDLGenerator(HDLType,
								ThisClock.GetComponent().getAttributeSet())
						.GetComponentMap(TheNetlist, index++, ThisClock,
								Reporter, "Bla", HDLType));
			}
		}
		Contents.add("");
		/* Here the map is performed */
		Contents.addAll(MakeRemarkBlock(
				"Here the toplevel component is connected", 3, HDLType));
		CircuitHDLGeneratorFactory DUT = new CircuitHDLGeneratorFactory(
				MyCircuit);
		Contents.addAll(DUT.GetComponentMap(TheNetlist, (long) 0, null,
				Reporter, CorrectLabel.getCorrectLabel(MyCircuit.getName()),
				HDLType));
		return Contents;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		int k = 0;
		for (int NrOfOutputs = 0; NrOfOutputs < MyIOComponents
				.GetNrOfToplevelOutputPins(); NrOfOutputs++) {
			if (MyIOComponents
					.GetFPGAOutputPinId(MyIOComponents.currentBoardName
							+ ":/LocalBus") > -1
					&& (NrOfOutputs == MyIOComponents
							.GetFPGAOutputPinId(MyIOComponents.currentBoardName
									+ ":/LocalBus") || NrOfOutputs == MyIOComponents
							.GetFPGAOutputPinId(MyIOComponents.currentBoardName
									+ ":/LocalBus") + 1)) {
				Outputs.put("FPGA_LB_OUT_" + Integer.toString(k), 1);
				k++;
			} else {
				Outputs.put(HDLGeneratorFactory.FPGAOutputPinName + "_"
						+ Integer.toString(NrOfOutputs), 1);
			}
		}
		return Outputs;
	}

	@Override
	public String GetSubDir() {
		/*
		 * this method returns the module directory where the HDL code needs to
		 * be placed
		 */
		return "toplevel";
	}

	@Override
	public SortedMap<String, Integer> GetWireList(AttributeSet attrs,
			Netlist Nets) {
		SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
		int NrOfClockTrees = Nets.NumberOfClockTrees();
		int NrOfInputBubbles = Nets.NumberOfInputBubbles();
		int NrOfInOutBubbles = Nets.NumberOfInOutBubbles();
		int NrOfOutputBubbles = Nets.NumberOfOutputBubbles();
		int NrOfInputPorts = Nets.NumberOfInputPorts();
		int NrOfInOutPorts = Nets.NumberOfInOutPorts();
		int NrOfOutputPorts = Nets.NumberOfOutputPorts();
		if (NrOfClockTrees > 0) {
			Wires.put(TickComponentHDLGeneratorFactory.FPGATick, 1);
			for (int clockBus = 0; clockBus < NrOfClockTrees; clockBus++) {
				Wires.put("s_" + ClockTreeName + Integer.toString(clockBus),
						ClockHDLGeneratorFactory.NrOfClockBits);
			}
		}
		if (NrOfInputBubbles > 0) {
			if (NrOfInputBubbles > 1) {
				Wires.put("s_LOGISIM_INPUT_BUBBLES", NrOfInputBubbles);
			} else {
				Wires.put("s_LOGISIM_INPUT_BUBBLES", 0);
			}
		}
		if (NrOfInOutBubbles > 0) {
			if (NrOfInOutBubbles > 1) {
				Wires.put("s_LOGISIM_INOUT_BUBBLES", NrOfInOutBubbles);
			} else {
				Wires.put("s_LOGISIM_INOUT_BUBBLES", 0);
			}
		}
		if (NrOfOutputBubbles > 0) {
			if (NrOfOutputBubbles > 1) {
				Wires.put("s_LOGISIM_OUTPUT_BUBBLES", NrOfOutputBubbles);
			} else {
				Wires.put("s_LOGISIM_OUTPUT_BUBBLES", 0);
			}
		}
		if (NrOfInputPorts > 0) {
			for (int input = 0; input < NrOfInputPorts; input++) {
				String SName = "s_"
						+ CorrectLabel.getCorrectLabel(Nets.GetInputPin(input)
								.GetComponent().getAttributeSet()
								.getValue(StdAttr.LABEL));
				int NrOfBits = Nets.GetInputPin(input).GetComponent().getEnd(0)
						.getWidth().getWidth();
				Wires.put(SName, NrOfBits);
			}
		}
		if (NrOfInOutPorts > 0) {
			for (int inout = 0; inout < NrOfInOutPorts; inout++) {
				String SName = "s_"
						+ CorrectLabel.getCorrectLabel(Nets.GetInOutPin(inout)
								.GetComponent().getAttributeSet()
								.getValue(StdAttr.LABEL));
				int NrOfBits = Nets.GetInOutPin(inout).GetComponent().getEnd(0)
						.getWidth().getWidth();
				Wires.put(SName, NrOfBits);
			}
		}
		if (NrOfOutputPorts > 0) {
			for (int output = 0; output < NrOfOutputPorts; output++) {
				String SName = "s_"
						+ CorrectLabel.getCorrectLabel(Nets
								.GetOutputPin(output).GetComponent()
								.getAttributeSet().getValue(StdAttr.LABEL));
				int NrOfBits = Nets.GetOutputPin(output).GetComponent()
						.getEnd(0).getWidth().getWidth();
				Wires.put(SName, NrOfBits);
			}
		}
		return Wires;
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
		return true;
	}
}
