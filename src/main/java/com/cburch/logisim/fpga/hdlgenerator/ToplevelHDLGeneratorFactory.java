/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitHDLGeneratorFactory;
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
import com.cburch.logisim.std.io.LedArrayGenericHDLGeneratorFactory;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

public class ToplevelHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
  private final long fpgaClockFrequency;
  private final double tickFrequency;
  private final Circuit myCircuit;
  private final MappableResourcesContainer myIOComponents;
  private final boolean requiresFPGAClock;
  private final boolean hasLedArray;
  private final ArrayList<FPGAIOInformationContainer> myLedArrays;
  private final HashMap<String, Boolean> ledArrayTypesUsed;

  public ToplevelHDLGeneratorFactory(long fpgaClock, double tickClock, Circuit topLevel,
      MappableResourcesContainer ioComponents) {
    super("toplevel");
    fpgaClockFrequency = fpgaClock;
    tickFrequency = tickClock;
    myCircuit = topLevel;
    myIOComponents = ioComponents;
    var hasScanningLedArray = false;
    var hasLedArray = false;
    final var ledArrayTypesUsed = new HashMap<String, Boolean>();
    final var ledArrays = new ArrayList<FPGAIOInformationContainer>();
    for (final var comp : myIOComponents.getIOComponentInformation().getComponents()) {
      if (comp.GetType().equals(IOComponentTypes.LEDArray)) {
        if (comp.hasMap()) {
          ledArrayTypesUsed.put(LedArrayDriving.getStrings().get(comp.getArrayDriveMode()), true);
          ledArrays.add(comp);
          comp.setArrayId(ledArrays.indexOf(comp));
          hasLedArray = true;
          if (!(comp.getArrayDriveMode() == LedArrayDriving.LED_DEFAULT)
              && !(comp.getArrayDriveMode() == LedArrayDriving.RGB_DEFAULT))
            hasScanningLedArray = true;
        }
      }
    }
    requiresFPGAClock = hasScanningLedArray;
    this.hasLedArray = hasLedArray;
    this.ledArrayTypesUsed = ledArrayTypesUsed;
    myLedArrays = ledArrays;
  }

  public boolean hasLedArray() {
    return hasLedArray;
  }

  public boolean hasLedArrayType(String type) {
    if (!ledArrayTypesUsed.containsKey(type)) return false;
    return ledArrayTypesUsed.get(type);
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist theNetlist, AttributeSet attrs) {
    final var components = new ArrayList<String>();
    final var nrOfClockTrees = theNetlist.numberOfClockTrees();
    if (nrOfClockTrees > 0) {
      TickComponentHDLGeneratorFactory ticker =
          new TickComponentHDLGeneratorFactory(
              fpgaClockFrequency, tickFrequency);
      components.addAll(
          ticker.getComponentInstantiation(
              theNetlist, null, ticker.getComponentStringIdentifier()));
      HDLGeneratorFactory clockWorker =
          theNetlist.getAllClockSources()
              .get(0)
              .getFactory()
              .getHDLGenerator(theNetlist.getAllClockSources().get(0).getAttributeSet());
      components.addAll(
          clockWorker.getComponentInstantiation(
              theNetlist,
              theNetlist.getAllClockSources().get(0).getAttributeSet(),
              theNetlist.getAllClockSources()
                  .get(0)
                  .getFactory()
                  .getHDLName(theNetlist.getAllClockSources().get(0).getAttributeSet())));
    }
    for (final var type : LedArrayDriving.DRIVING_STRINGS) {
      if (hasLedArrayType(type)) {
        final var worker = LedArrayGenericHDLGeneratorFactory.getSpecificHDLGenerator(type);
        final var name = LedArrayGenericHDLGeneratorFactory.getSpecificHDLName(type);
        if (worker != null && name != null)
          components.addAll(worker.getComponentInstantiation(theNetlist, null, name));
      }
    }
    final var worker = new CircuitHDLGeneratorFactory(myCircuit);
    components.addAll(
        worker.getComponentInstantiation(
            theNetlist,
            null,
            CorrectLabel.getCorrectLabel(myCircuit.getName())));
    return components;
  }

  @Override
  public String getComponentStringIdentifier() {
    return FPGA_TOP_LEVEL_NAME;
  }

  @Override
  public SortedMap<String, Integer> GetInOutList(Netlist theNetlist, AttributeSet attrs) {
    final var inOut = new TreeMap<String, Integer>();
    for (var io : myIOComponents.GetMappedIOPinNames()) {
      inOut.put(io, 1);
    }
    return inOut;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist theNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    for (var io : myIOComponents.GetMappedOutputPinNames()) {
      outputs.put(io, 1);
    }
    for (var ledArray : myLedArrays) {
      outputs.putAll(LedArrayGenericHDLGeneratorFactory.getExternalSignals(
          ledArray.getArrayDriveMode(),
          ledArray.getNrOfRows(),
          ledArray.getNrOfColumns(),
          myLedArrays.indexOf(ledArray)));
    }
    return outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist theNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    final var nrOfClockTrees = theNetlist.numberOfClockTrees();
    /* First we instantiate the Clock tree busses when present */
    if (nrOfClockTrees > 0 || theNetlist.requiresGlobalClockConnection() || requiresFPGAClock) {
      inputs.put(TickComponentHDLGeneratorFactory.FPGA_CLOCK, 1);
    }
    for (var in : myIOComponents.GetMappedInputPinNames()) {
      inputs.put(in, 1);
    }
    return inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = new LineBuffer();
    final var nrOfClockTrees = theNetlist.numberOfClockTrees();
    /* First we process all components */
    contents.addRemarkBlock("Here all signal adaptations are performed");
    for (final var key : myIOComponents.getMappableResources().keySet()) {
      final var comp = myIOComponents.getMappableResources().get(key);
      contents.add(AbstractHDLGeneratorFactory.GetToplevelCode(comp));
    }
    /* now we process the clock tree components */
    if (nrOfClockTrees > 0) {
      contents.addRemarkBlock("Here the clock tree components are defined");
      final var ticker = new TickComponentHDLGeneratorFactory(fpgaClockFrequency, tickFrequency);
      contents.add(ticker.getComponentMap(null, 0L, null, null, ""));
      var index = 0L;
      for (var clockGen : theNetlist.getAllClockSources()) {
        final var thisClock = new NetlistComponent(clockGen);
        contents.add(
            clockGen.getFactory()
                .getHDLGenerator(thisClock.getComponent().getAttributeSet())
                .getComponentMap(theNetlist, index++, thisClock, null, ""));
      }
    }
    contents.add("");

    /* Here the map is performed */
    contents.addRemarkBlock("Here the toplevel component is connected");
    final var dut = new CircuitHDLGeneratorFactory(myCircuit);
    contents.add(
        dut.getComponentMap(
            theNetlist,
            0L,
            null,
            myIOComponents,
            CorrectLabel.getCorrectLabel(myCircuit.getName())));
    if (hasLedArray) {
      contents.add("").addRemarkBlock("Here the Led arrays are connected");
      for (var array : myLedArrays) {
        contents.add(
            LedArrayGenericHDLGeneratorFactory.GetComponentMap(
                array.getArrayDriveMode(),
                array.getNrOfRows(),
                array.getNrOfColumns(),
                myLedArrays.indexOf(array),
                fpgaClockFrequency,
                array.GetActivityLevel() == PinActivity.ACTIVE_LOW));
        contents.add(LedArrayGenericHDLGeneratorFactory.getArrayConnections(array, myLedArrays.indexOf(array)));
      }
    }
    return contents.get();
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist nets) {
    final var wires = new TreeMap<String, Integer>();
    final var nrOfClockTrees = nets.numberOfClockTrees();
    final var nrOfInputBubbles = nets.getNumberOfInputBubbles();
    final var nrOfOutputBubbles = nets.numberOfOutputBubbles();
    final var nrOfInputPorts = nets.getNumberOfInputPorts();
    final var nrOfInOutPorts = nets.numberOfInOutPorts();
    final var nrOfOutputPorts = nets.numberOfOutputPorts();
    if (nrOfClockTrees > 0) {
      wires.put(TickComponentHDLGeneratorFactory.FPGA_TICK, 1);
      for (var clockBus = 0; clockBus < nrOfClockTrees; clockBus++) {
        wires.put("s_" + CLOCK_TREE_NAME + clockBus, ClockHDLGeneratorFactory.NR_OF_CLOCK_BITS);
      }
    }
    if (nrOfInputBubbles > 0) {
      if (nrOfInputBubbles > 1) {
        wires.put("s_LOGISIM_INPUT_BUBBLES", nrOfInputBubbles);
      } else {
        wires.put("s_LOGISIM_INPUT_BUBBLES", 0);
      }
    }
    if (nrOfOutputBubbles > 0) {
      if (nrOfOutputBubbles > 1) {
        wires.put("s_LOGISIM_OUTPUT_BUBBLES", nrOfOutputBubbles);
      } else {
        wires.put("s_LOGISIM_OUTPUT_BUBBLES", 0);
      }
    }
    if (nrOfInputPorts > 0) {
      for (var input = 0; input < nrOfInputPorts; input++) {
        String sName = "s_"
            + CorrectLabel.getCorrectLabel(
                nets.getInputPin(input)
                    .getComponent()
                    .getAttributeSet()
                    .getValue(StdAttr.LABEL));
        final var nrOfBits = nets.getInputPin(input).getComponent().getEnd(0).getWidth().getWidth();
        wires.put(sName, nrOfBits);
      }
    }
    if (nrOfInOutPorts > 0) {
      for (var inout = 0; inout < nrOfInOutPorts; inout++) {
        final var sName = "s_"
            + CorrectLabel.getCorrectLabel(
                nets.getInOutPin(inout)
                    .getComponent()
                    .getAttributeSet()
                    .getValue(StdAttr.LABEL));
        final var nrOfBits = nets.getInOutPin(inout).getComponent().getEnd(0).getWidth().getWidth();
        wires.put(sName, nrOfBits);
      }
    }
    if (nrOfOutputPorts > 0) {
      for (var output = 0; output < nrOfOutputPorts; output++) {
        final var sName = "s_"
            + CorrectLabel.getCorrectLabel(
                nets.getOutputPin(output)
                    .getComponent()
                    .getAttributeSet()
                    .getValue(StdAttr.LABEL));
        final var nrOfBits = nets.getOutputPin(output).getComponent().getEnd(0).getWidth().getWidth();
        wires.put(sName, nrOfBits);
      }
    }
    for (final var ledArray : myLedArrays) {
      wires.putAll(LedArrayGenericHDLGeneratorFactory.getInternalSignals(
          ledArray.getArrayDriveMode(),
          ledArray.getNrOfRows(),
          ledArray.getNrOfColumns(),
          myLedArrays.indexOf(ledArray)));
    }
    return wires;
  }
}
