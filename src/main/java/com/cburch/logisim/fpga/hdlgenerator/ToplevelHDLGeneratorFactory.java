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
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.data.IOComponentTypes;
import com.cburch.logisim.fpga.data.LedArrayDriving;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.PinActivity;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

public class ToplevelHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private final long FpgaClockFrequency;
  private final double TickFrequency;
  private final Circuit MyCircuit;
  private final MappableResourcesContainer MyIOComponents;
  private final boolean requiresFPGAClock;
  private final boolean hasLedArray;
  private final ArrayList<FPGAIOInformationContainer> myLedArrays;
  private final HashMap<String, Boolean> LedArrayTypesUsed; 


  public ToplevelHDLGeneratorFactory(long FPGAClock, double TickClock, Circuit TopLevel, 
      MappableResourcesContainer IOComponents) {
    FpgaClockFrequency = FPGAClock;
    TickFrequency = TickClock;
    MyCircuit = TopLevel;
    MyIOComponents = IOComponents;
    var hasScanningLedArray = false;
    var hasLedArray = false;
    final var LedArrayTypesUsed = new HashMap<String, Boolean>();
    final var LedArrays = new ArrayList<FPGAIOInformationContainer>();
    for (var comp : MyIOComponents.getIOComponentInformation().getComponents()) {
      if (comp.GetType().equals(IOComponentTypes.LEDArray)) {
        if (comp.hasMap()) {
          LedArrayTypesUsed.put(LedArrayDriving.getStrings().get(comp.getArrayDriveMode()), true);
          LedArrays.add(comp);
          comp.setArrayId(LedArrays.indexOf(comp));
          hasLedArray = true;
          if (!(comp.getArrayDriveMode() == LedArrayDriving.LedDefault) 
              && !(comp.getArrayDriveMode() == LedArrayDriving.RgbDefault))
            hasScanningLedArray = true;
        }
      }
    }
    requiresFPGAClock = hasScanningLedArray;
    this.hasLedArray = hasLedArray;
    this.LedArrayTypesUsed = LedArrayTypesUsed;
    myLedArrays = LedArrays;
  }
  
  public boolean hasLedArray() {
    return hasLedArray;
  }
  
  public boolean hasLedArrayType(String type) {
    if (!LedArrayTypesUsed.containsKey(type)) return false;
    return LedArrayTypesUsed.get(type);
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist, AttributeSet attrs) {
    final var Components = new ArrayList<String>();
    final var NrOfClockTrees = TheNetlist.NumberOfClockTrees();
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
    for (var type : LedArrayDriving.Driving_strings) {
      if (hasLedArrayType(type)) {
        final var Worker = LedArrayGenericHDLGeneratorFactory.getSpecificHDLGenerator(type);
        final var Name = LedArrayGenericHDLGeneratorFactory.getSpecificHDLName(type);
        if (Worker != null && Name != null)
          Components.addAll(Worker.GetComponentInstantiation(TheNetlist, null, Name));
      }
    }
    final var Worker = new CircuitHDLGeneratorFactory(MyCircuit);
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
    final var InOut = new TreeMap<String, Integer>();
    for (var io : MyIOComponents.GetMappedIOPinNames()) {
      InOut.put(io, 1);
    }
    return InOut;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var Outputs = new TreeMap<String, Integer>();
    for (var io : MyIOComponents.GetMappedOutputPinNames()) {
      Outputs.put(io, 1);
    }
    for (var ledArray : myLedArrays) {
      Outputs.putAll(LedArrayGenericHDLGeneratorFactory.getExternalSignals(
          ledArray.getArrayDriveMode(), 
          ledArray.getNrOfRows(), 
          ledArray.getNrOfColumns(), 
          myLedArrays.indexOf(ledArray)));
    }
    return Outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var Inputs = new TreeMap<String, Integer>();
    final var NrOfClockTrees = TheNetlist.NumberOfClockTrees();
    /* First we instantiate the Clock tree busses when present */
    if (NrOfClockTrees > 0 || TheNetlist.RequiresGlobalClockConnection() || requiresFPGAClock) {
      Inputs.put(TickComponentHDLGeneratorFactory.FPGAClock, 1);
    }
    for (var in : MyIOComponents.GetMappedInputPinNames()) {
      Inputs.put(in, 1);
    }
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = new ArrayList<String>();
    final var NrOfClockTrees = TheNetlist.NumberOfClockTrees();
    /* First we process all components */
    Contents.addAll(MakeRemarkBlock("Here all signal adaptations are performed", 3));
    for (var key : MyIOComponents.getMappableResources().keySet()) {
      final var comp = MyIOComponents.getMappableResources().get(key);
      Contents.addAll(AbstractHDLGeneratorFactory.GetToplevelCode(comp));
    }
    /* now we process the clock tree components */
    if (NrOfClockTrees > 0) {
      Contents.addAll(MakeRemarkBlock("Here the clock tree components are defined", 3));
      final var Ticker = new TickComponentHDLGeneratorFactory(FpgaClockFrequency, TickFrequency);
      Contents.addAll(Ticker.GetComponentMap(null, 0L, null, null, ""));
      var index = 0L;
      for (var Clockgen : TheNetlist.GetAllClockSources()) {
        final var ThisClock = new NetlistComponent(Clockgen);
        Contents.addAll(
            Clockgen.getFactory()
                .getHDLGenerator(ThisClock.GetComponent().getAttributeSet())
                .GetComponentMap(TheNetlist, index++, ThisClock, null, ""));
      }
    }
    Contents.add("");
    /* Here the map is performed */
    Contents.addAll(MakeRemarkBlock("Here the toplevel component is connected", 3));
    final var DUT = new CircuitHDLGeneratorFactory(MyCircuit);
    Contents.addAll(
        DUT.GetComponentMap(
            TheNetlist,
            0L,
            null,
            MyIOComponents,
            CorrectLabel.getCorrectLabel(MyCircuit.getName())));
    if (hasLedArray) {
      Contents.add("");
      Contents.addAll(MakeRemarkBlock("Here the Led arrays are connected", 3));
      for (var array : myLedArrays) {
        Contents.addAll(LedArrayGenericHDLGeneratorFactory.GetComponentMap(
            array.getArrayDriveMode(), 
            array.getNrOfRows(), 
            array.getNrOfColumns(), 
            myLedArrays.indexOf(array),
            FpgaClockFrequency,
            array.GetActivityLevel() == PinActivity.ActiveLow));
        Contents.addAll(LedArrayGenericHDLGeneratorFactory.getArrayConnections(array, myLedArrays.indexOf(array)));
      }
    }
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
    final var Wires = new TreeMap<String, Integer>();
    final var NrOfClockTrees = Nets.NumberOfClockTrees();
    final var NrOfInputBubbles = Nets.NumberOfInputBubbles();
    final var NrOfOutputBubbles = Nets.NumberOfOutputBubbles();
    final var NrOfInputPorts = Nets.NumberOfInputPorts();
    final var NrOfInOutPorts = Nets.NumberOfInOutPorts();
    final var NrOfOutputPorts = Nets.NumberOfOutputPorts();
    if (NrOfClockTrees > 0) {
      Wires.put(TickComponentHDLGeneratorFactory.FPGATick, 1);
      for (var clockBus = 0; clockBus < NrOfClockTrees; clockBus++) {
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
      for (var input = 0; input < NrOfInputPorts; input++) {
        String SName = "s_"
            + CorrectLabel.getCorrectLabel(
                Nets.GetInputPin(input)
                    .GetComponent()
                    .getAttributeSet()
                    .getValue(StdAttr.LABEL));
        final var NrOfBits = Nets.GetInputPin(input).GetComponent().getEnd(0).getWidth().getWidth();
        Wires.put(SName, NrOfBits);
      }
    }
    if (NrOfInOutPorts > 0) {
      for (var inout = 0; inout < NrOfInOutPorts; inout++) {
        final var SName = "s_"
            + CorrectLabel.getCorrectLabel(
                Nets.GetInOutPin(inout)
                    .GetComponent()
                    .getAttributeSet()
                    .getValue(StdAttr.LABEL));
        final var NrOfBits = Nets.GetInOutPin(inout).GetComponent().getEnd(0).getWidth().getWidth();
        Wires.put(SName, NrOfBits);
      }
    }
    if (NrOfOutputPorts > 0) {
      for (var output = 0; output < NrOfOutputPorts; output++) {
        final var SName = "s_"
            + CorrectLabel.getCorrectLabel(
                Nets.GetOutputPin(output)
                    .GetComponent()
                    .getAttributeSet()
                    .getValue(StdAttr.LABEL));
        final var NrOfBits = Nets.GetOutputPin(output).GetComponent().getEnd(0).getWidth().getWidth();
        Wires.put(SName, NrOfBits);
      }
    }
    for (var ledArray : myLedArrays) {
      Wires.putAll(LedArrayGenericHDLGeneratorFactory.getInternalSignals(
          ledArray.getArrayDriveMode(), 
          ledArray.getNrOfRows(), 
          ledArray.getNrOfColumns(), 
          myLedArrays.indexOf(ledArray)));
    }
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
