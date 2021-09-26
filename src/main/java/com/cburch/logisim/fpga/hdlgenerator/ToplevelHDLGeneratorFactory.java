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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.io.LedArrayGenericHDLGeneratorFactory;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class ToplevelHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
  private final long fpgaClockFrequency;
  private final double tickFrequency;
  private final Circuit myCircuit;
  private final MappableResourcesContainer myIOComponents;
  private final boolean requiresFPGAClock;
  private final boolean hasLedArray;
  private final ArrayList<FPGAIOInformationContainer> myLedArrays;
  private final HashMap<String, Boolean> ledArrayTypesUsed;
  public static final String HDL_DIRECTORY = "toplevel";

  public ToplevelHDLGeneratorFactory(long fpgaClock, double tickClock, Circuit topLevel,
      MappableResourcesContainer ioComponents) {
    super(HDL_DIRECTORY);
    fpgaClockFrequency = fpgaClock;
    tickFrequency = tickClock;
    myCircuit = topLevel;
    myIOComponents = ioComponents;
    var hasScanningLedArray = false;
    var hasLedArray = false;
    final var nets = topLevel.getNetList();
    final var ledArrayTypesUsed = new HashMap<String, Boolean>();
    final var ledArrays = new ArrayList<FPGAIOInformationContainer>();
    final var nrOfClockTrees = nets.numberOfClockTrees();
    final var nrOfInputBubbles = nets.getNumberOfInputBubbles();
    final var nrOfInOutBubbles = nets.numberOfInOutBubbles();
    final var nrOfOutputBubbles = nets.numberOfOutputBubbles();
    final var nrOfInputPorts = nets.getNumberOfInputPorts();
    final var nrOfInOutPorts = nets.numberOfInOutPorts();
    final var nrOfOutputPorts = nets.numberOfOutputPorts();
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
    if (nrOfClockTrees > 0) {
      myWires.addWire(TickComponentHDLGeneratorFactory.FPGA_TICK, 1);
      for (var clockId = 0; clockId < nrOfClockTrees; clockId++)
        myWires.addWire(String.format("s_%s%d", CLOCK_TREE_NAME, clockId), ClockHDLGeneratorFactory.NR_OF_CLOCK_BITS);
    }
    if (nrOfInputBubbles > 0)
      myWires.addWire(String.format("s_%s", HDLGeneratorFactory.LOCAL_INPUT_BUBBLE_BUS_NAME),
          nrOfInputBubbles > 1 ? nrOfInputBubbles : 0);
    if (nrOfInOutBubbles > 0)
      myWires.addWire(String.format("s_%s", HDLGeneratorFactory.LOCAL_INOUT_BUBBLE_BUS_NAME),
          nrOfInOutBubbles > 1 ? nrOfInOutBubbles : 0);
    if (nrOfOutputBubbles > 0)
      myWires.addWire(String.format("s_%s", HDLGeneratorFactory.LOCAL_OUTPUT_BUBBLE_BUS_NAME),
          nrOfOutputBubbles > 1 ? nrOfOutputBubbles : 0);
    if (nrOfInputPorts > 0) {
      for (var input = 0; input < nrOfInputPorts; input++) {
        final var inputName = String.format("s_%s", CorrectLabel.getCorrectLabel(
            nets.getInputPin(input).getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        final var nrOfBits = nets.getInputPin(input).getComponent().getEnd(0).getWidth().getWidth();
        myWires.addWire(inputName, nrOfBits);
      }
    }
    if (nrOfInOutPorts > 0) {
      for (var inout = 0; inout < nrOfInOutPorts; inout++) {
        final var ioName = String.format("s_%s", CorrectLabel.getCorrectLabel(
            nets.getInOutPin(inout).getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        final var nrOfBits = nets.getInOutPin(inout).getComponent().getEnd(0).getWidth().getWidth();
        myWires.addWire(ioName, nrOfBits);
      }
    }
    if (nrOfOutputPorts > 0) {
      for (var output = 0; output < nrOfOutputPorts; output++) {
        final var outputName = String.format("s_%s", CorrectLabel.getCorrectLabel(
            nets.getOutputPin(output).getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        final var nrOfBits = nets.getOutputPin(output).getComponent().getEnd(0).getWidth().getWidth();
        myWires.addWire(outputName, nrOfBits);
      }
    }
    for (final var ledArray : myLedArrays) {
      myWires.addAllWires(LedArrayGenericHDLGeneratorFactory.getInternalSignals(
          ledArray.getArrayDriveMode(),
          ledArray.getNrOfRows(),
          ledArray.getNrOfColumns(),
          myLedArrays.indexOf(ledArray)));
      final var ports = LedArrayGenericHDLGeneratorFactory.getExternalSignals(
          ledArray.getArrayDriveMode(),
          ledArray.getNrOfRows(),
          ledArray.getNrOfColumns(),
          myLedArrays.indexOf(ledArray));
      for (final var port : ports.keySet())
        myPorts.add(Port.OUTPUT, port, ports.get(port), null);
    }
    if (nrOfClockTrees > 0 || nets.requiresGlobalClockConnection() || requiresFPGAClock)
      myPorts.add(Port.INPUT, TickComponentHDLGeneratorFactory.FPGA_CLOCK, 1, null);
    for (final var in : myIOComponents.GetMappedInputPinNames())
      myPorts.add(Port.INPUT, in, 1, null);
    for (final var io : myIOComponents.GetMappedOutputPinNames()) {
      myPorts.add(Port.OUTPUT, io, 1, null);
    }
    for (final var io : myIOComponents.GetMappedIOPinNames())
      myPorts.add(Port.INOUT, io, 1, null);
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
      final var ticker = new TickComponentHDLGeneratorFactory(fpgaClockFrequency, tickFrequency);
      components.addAll(ticker.getComponentInstantiation(theNetlist, null, TickComponentHDLGeneratorFactory.HDL_IDENTIFIER));
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
  public ArrayList<String> getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
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
      var index = 0L;
      final var ticker = new TickComponentHDLGeneratorFactory(fpgaClockFrequency, tickFrequency);
      contents.add(ticker.getComponentMap(null, index++, null, TickComponentHDLGeneratorFactory.HDL_IDENTIFIER));
      for (final var clockGen : theNetlist.getAllClockSources()) {
        final var thisClock = new NetlistComponent(clockGen);
        contents.add(
            clockGen.getFactory()
                .getHDLGenerator(thisClock.getComponent().getAttributeSet())
                .getComponentMap(theNetlist, index++, thisClock, ""));
      }
    }
    contents.add("");

    /* Here the map is performed */
    contents.addRemarkBlock("Here the toplevel component is connected");
    final var dut = new CircuitHDLGeneratorFactory(myCircuit);
    contents.add(dut.getComponentMap(theNetlist, 0L, myIOComponents, CorrectLabel.getCorrectLabel(myCircuit.getName())));
    // Here the led arrays are connected
    if (hasLedArray) {
      contents.add("").addRemarkBlock("Here the Led arrays are connected");
      for (final var array : myLedArrays) {
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
}
