/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ToplevelHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private final long FpgaClockFrequency;
  private final double TickFrequency;
  private final Circuit MyCircuit;
  private final MappableResourcesContainer MyIOComponents;


  public ToplevelHDLGeneratorFactory(
      long FPGAClock, double TickClock, Circuit TopLevel, MappableResourcesContainer IOComponents) {
    FpgaClockFrequency = FPGAClock;
    TickFrequency = TickClock;
    MyCircuit = TopLevel;
    MyIOComponents = IOComponents;
    // this.useFPGAClock = useFPGAClock;
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Components = new ArrayList<>();
    int NrOfClockTrees = TheNetlist.NumberOfClockTrees();
    if (NrOfClockTrees > 0) {
      TickComponentHDLGeneratorFactory Ticker =
          new TickComponentHDLGeneratorFactory(
              FpgaClockFrequency, TickFrequency);
      Components.addAll(
          Ticker.GetComponentInstantiation(
              TheNetlist, null, Ticker.getComponentStringIdentifier()));
      HDLGeneratorFactory ClockWorker =
          TheNetlist.GetAllClockSources()
              .get(0)
              .getFactory()
              .getHDLGenerator(TheNetlist.GetAllClockSources().get(0).getAttributeSet());
      Components.addAll(
          ClockWorker.GetComponentInstantiation(
              TheNetlist,
              TheNetlist.GetAllClockSources().get(0).getAttributeSet(),
              TheNetlist.GetAllClockSources()
                  .get(0)
                  .getFactory()
                  .getHDLName(TheNetlist.GetAllClockSources().get(0).getAttributeSet())));
    }
    CircuitHDLGeneratorFactory Worker = new CircuitHDLGeneratorFactory(MyCircuit);
    Components.addAll(
        Worker.GetComponentInstantiation(
            TheNetlist,
            null,
            CorrectLabel.getCorrectLabel(MyCircuit.getName())));
    return Components;
  }

  @Override
  public String getComponentStringIdentifier() {
    return FPGAToplevelName;
  }

  @Override
  public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> InOut = new TreeMap<>();
    for (String io : MyIOComponents.GetMappedIOPinNames()) {
      InOut.put(io, 1);
    }
    return InOut;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    for (String io : MyIOComponents.GetMappedOutputPinNames()) {
      Outputs.put(io, 1);
    }
    return Outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    int NrOfClockTrees = TheNetlist.NumberOfClockTrees();
    /* First we instantiate the Clock tree busses when present */
    if (NrOfClockTrees > 0 || TheNetlist.RequiresGlobalClockConnection()) {
      Inputs.put(TickComponentHDLGeneratorFactory.FPGAClock, 1);
    }
    for (String in : MyIOComponents.GetMappedInputPinNames()) {
      Inputs.put(in, 1);
    }
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    int NrOfClockTrees = TheNetlist.NumberOfClockTrees();
    /* First we process all components */
    Contents.addAll(MakeRemarkBlock("Here all signal adaptations are performed", 3));
    for (ArrayList<String> key : MyIOComponents.getMappableResources().keySet()) {
      MapComponent comp = MyIOComponents.getMappableResources().get(key);
      Contents.addAll(AbstractHDLGeneratorFactory.GetToplevelCode(comp));
    }
    /* now we peocess the clock tree components */
    if (NrOfClockTrees > 0) {
      Contents.addAll(MakeRemarkBlock("Here the clock tree components are defined", 3));
      TickComponentHDLGeneratorFactory Ticker =
          new TickComponentHDLGeneratorFactory(
              FpgaClockFrequency, TickFrequency);
      Contents.addAll(Ticker.GetComponentMap(null, (long) 0, null, null, ""));
      long index = 0;
      for (Component Clockgen : TheNetlist.GetAllClockSources()) {
        NetlistComponent ThisClock = new NetlistComponent(Clockgen);
        Contents.addAll(
            Clockgen.getFactory()
                .getHDLGenerator(ThisClock.GetComponent().getAttributeSet())
                .GetComponentMap(TheNetlist, index++, ThisClock, null, ""));
      }
    }
    Contents.add("");
    /* Here the map is performed */
    Contents.addAll(MakeRemarkBlock("Here the toplevel component is connected", 3));
    CircuitHDLGeneratorFactory DUT = new CircuitHDLGeneratorFactory(MyCircuit);
    Contents.addAll(
        DUT.GetComponentMap(
            TheNetlist,
            (long) 0,
            null,
            MyIOComponents,
            CorrectLabel.getCorrectLabel(MyCircuit.getName())));
    return Contents;
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
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    int NrOfClockTrees = Nets.NumberOfClockTrees();
    int NrOfInputBubbles = Nets.NumberOfInputBubbles();
    int NrOfOutputBubbles = Nets.NumberOfOutputBubbles();
    int NrOfInputPorts = Nets.NumberOfInputPorts();
    int NrOfInOutPorts = Nets.NumberOfInOutPorts();
    int NrOfOutputPorts = Nets.NumberOfOutputPorts();
    if (NrOfClockTrees > 0) {
      Wires.put(TickComponentHDLGeneratorFactory.FPGATick, 1);
      for (int clockBus = 0; clockBus < NrOfClockTrees; clockBus++) {
        Wires.put(
            "s_" + ClockTreeName + clockBus,
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
    if (NrOfOutputBubbles > 0) {
      if (NrOfOutputBubbles > 1) {
        Wires.put("s_LOGISIM_OUTPUT_BUBBLES", NrOfOutputBubbles);
      } else {
        Wires.put("s_LOGISIM_OUTPUT_BUBBLES", 0);
      }
    }
    if (NrOfInputPorts > 0) {
      for (int input = 0; input < NrOfInputPorts; input++) {
        String SName =
            "s_"
                + CorrectLabel.getCorrectLabel(
                    Nets.GetInputPin(input)
                        .GetComponent()
                        .getAttributeSet()
                        .getValue(StdAttr.LABEL));
        int NrOfBits = Nets.GetInputPin(input).GetComponent().getEnd(0).getWidth().getWidth();
        Wires.put(SName, NrOfBits);
      }
    }
    if (NrOfInOutPorts > 0) {
      for (int inout = 0; inout < NrOfInOutPorts; inout++) {
        String SName =
            "s_"
                + CorrectLabel.getCorrectLabel(
                    Nets.GetInOutPin(inout)
                        .GetComponent()
                        .getAttributeSet()
                        .getValue(StdAttr.LABEL));
        int NrOfBits = Nets.GetInOutPin(inout).GetComponent().getEnd(0).getWidth().getWidth();
        Wires.put(SName, NrOfBits);
      }
    }
    if (NrOfOutputPorts > 0) {
      for (int output = 0; output < NrOfOutputPorts; output++) {
        String SName =
            "s_"
                + CorrectLabel.getCorrectLabel(
                    Nets.GetOutputPin(output)
                        .GetComponent()
                        .getAttributeSet()
                        .getValue(StdAttr.LABEL));
        int NrOfBits = Nets.GetOutputPin(output).GetComponent().getEnd(0).getWidth().getWidth();
        Wires.put(SName, NrOfBits);
      }
    }
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
