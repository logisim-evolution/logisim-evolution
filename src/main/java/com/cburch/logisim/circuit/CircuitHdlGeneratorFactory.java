/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CircuitHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private enum bubbleType {
    INPUT, OUTPUT, INOUT
  }
  
  private final Circuit myCircuit;

  public CircuitHdlGeneratorFactory(Circuit source) {
    myCircuit = source;
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var inOutBubbles = theNetlist.numberOfInOutBubbles();
    final var inputBubbles = theNetlist.getNumberOfInputBubbles();
    final var outputBubbles = theNetlist.numberOfOutputBubbles();
    // First we add the wires
    for (final var wire : theNetlist.getAllNets())
      if (!wire.isBus())
        myWires.addWire(String.format("%s%d", NET_NAME, theNetlist.getNetId(wire)), 1);
    // Now we add the busses
    for (final var wire : theNetlist.getAllNets())
      if (wire.isBus() && wire.isRootNet())
        myWires.addWire(String.format("%s%d", BUS_NAME, theNetlist.getNetId(wire)), wire.getBitWidth());
    if (inOutBubbles > 0)
      myPorts.add(Port.INOUT, LOCAL_INOUT_BUBBLE_BUS_NAME, inOutBubbles > 1 ? inOutBubbles : 0, 0);
    for (var clock = 0; clock < theNetlist.numberOfClockTrees(); clock++)
      myPorts.add(Port.INPUT, String.format("%s%d", CLOCK_TREE_NAME, clock), ClockHdlGeneratorFactory.NR_OF_CLOCK_BITS, 0);
    if (theNetlist.requiresGlobalClockConnection())
      myPorts.add(Port.INPUT, TickComponentHdlGeneratorFactory.FPGA_CLOCK, 1, 0);
    if (inputBubbles > 0)
      myPorts.add(Port.INPUT, LOCAL_INPUT_BUBBLE_BUS_NAME, inputBubbles > 1 ? inputBubbles : 0, 0);
    for (var input = 0; input < theNetlist.getNumberOfInputPorts(); input++) {
      final var selectedInput = theNetlist.getInputPin(input);
      if (selectedInput != null)  {
        final var name = selectedInput.getComponent().getAttributeSet().getValue(StdAttr.LABEL);
        final var nrOfBits = selectedInput.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
        myPorts.add(Port.INPUT, CorrectLabel.getCorrectLabel(name), nrOfBits, 0);
      }
    }
    if (outputBubbles > 0) {
      myPorts.add(Port.OUTPUT, LOCAL_OUTPUT_BUBBLE_BUS_NAME, outputBubbles > 1 ? outputBubbles : 0, 0);
    }
    for (var output = 0; output < theNetlist.numberOfOutputPorts(); output++) {
      final var selectedInput = theNetlist.getOutputPin(output);
      if (selectedInput != null)  {
        final var name = selectedInput.getComponent().getAttributeSet().getValue(StdAttr.LABEL);
        final var nrOfBits = selectedInput.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
        myPorts.add(Port.OUTPUT, CorrectLabel.getCorrectLabel(name), nrOfBits, 0);
      }
    }
  }

  @Override
  public boolean generateAllHDLDescriptions(Set<String> handledComponents, String workingDir, List<String> hierarchy) {
    return generateAllHDLDescriptions(handledComponents, workingDir, hierarchy, false);
  }

  public boolean generateAllHDLDescriptions(
      Set<String> handledComponents,
      String workingDir,
      List<String> hierarchy,
      boolean gatedInstance) {
    if (myCircuit == null) {
      return false;
    }
    if (hierarchy == null) {
      hierarchy = new ArrayList<>();
    }
    final var myNetList = myCircuit.getNetList();
    if (myNetList == null) {
      return false;
    }
    var workPath = workingDir;
    if (!workPath.endsWith(File.separator)) {
      workPath += File.separator;
    }
    myNetList.setCurrentHierarchyLevel(hierarchy);
    /* First we handle the normal components */
    for (final var thisComponent : myNetList.getNormalComponents()) {
      final var componentName =
          thisComponent.getComponent()
              .getFactory()
              .getHDLName(thisComponent.getComponent().getAttributeSet());
      if (!handledComponents.contains(componentName)) {
        final var worker =
            thisComponent.getComponent()
                .getFactory()
                .getHDLGenerator(thisComponent.getComponent().getAttributeSet());
        if (worker == null) {
          // FIXME: hardcoded string
          Reporter.report.addFatalError(
              "INTERNAL ERROR: Cannot find the VHDL generator factory for component "
                  + componentName);
          return false;
        }
        if (!worker.isOnlyInlined()) {
          if (!Hdl.writeEntity(
              workPath + worker.getRelativeDirectory(),
              worker.getEntity(
                  myNetList,
                  thisComponent.getComponent().getAttributeSet(),
                  componentName),
              componentName)) {
            return false;
          }
          if (!Hdl.writeArchitecture(
              workPath + worker.getRelativeDirectory(),
              worker.getArchitecture(
                  myNetList,
                  thisComponent.getComponent().getAttributeSet(),
                  componentName),
              componentName)) {
            return false;
          }
        }
        handledComponents.add(componentName);
      }
    }
    /* Now we go down the hierarchy to get all other components */
    for (final var thisCircuit : myNetList.getSubCircuits()) {
      final var worker =
          (CircuitHdlGeneratorFactory)
              thisCircuit.getComponent()
                  .getFactory()
                  .getHDLGenerator(thisCircuit.getComponent().getAttributeSet());
      if (worker == null) {
        // FIXME: hardcoded string
        Reporter.report.addFatalError(
            "INTERNAL ERROR: Unable to get a subcircuit VHDL generator for '"
                + thisCircuit.getComponent().getFactory().getName()
                + "'");
        return false;
      }
      hierarchy.add(
          CorrectLabel.getCorrectLabel(
              thisCircuit.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      if (!worker.generateAllHDLDescriptions(
          handledComponents, workingDir, hierarchy, thisCircuit.isGatedInstance())) {
        return false;
      }
      hierarchy.remove(hierarchy.size() - 1);
    }
    /* I also have to generate myself */
    var componentName = CorrectLabel.getCorrectLabel(myCircuit.getName());
    if (gatedInstance) componentName = componentName.concat("_gated");
    if (!handledComponents.contains(componentName)) {
      if (!Hdl.writeEntity(
          workPath + getRelativeDirectory(),
          getEntity(myNetList, null, componentName),
          componentName)) {
        return false;
      }

      if (!Hdl.writeArchitecture(
          workPath + getRelativeDirectory(),
          getArchitecture(myNetList, null, componentName),
          componentName)) {
        return false;
      }
    }
    handledComponents.add(componentName);
    return true;
  }

  /* here the private handles are defined */
  private String getBubbleIndex(netlistComponent comp, bubbleType type) {
    final var fmt = "{{<}}{{1}} {{2}} {{3}}{{>}}";
    return switch (type) {
      case INPUT -> LineBuffer.format(fmt, comp.getLocalBubbleInputEndId(), 
          Hdl.vectorLoopId(), comp.getLocalBubbleInputStartId());
      case OUTPUT -> LineBuffer.format(fmt, comp.getLocalBubbleOutputEndId(), 
          Hdl.vectorLoopId(), comp.getLocalBubbleOutputStartId()); 
      case INOUT -> LineBuffer.format(fmt, comp.getLocalBubbleInOutEndId(), 
          Hdl.vectorLoopId(), comp.getLocalBubbleInOutStartId());
      default -> "";
    };
  }

  @Override
  public LineBuffer getComponentDeclarationSection(Netlist theNetlist, AttributeSet attrs) {
    final var components = LineBuffer.getBuffer();
    final var instantiatedComponents = new HashSet<String>();
    for (final var gate : theNetlist.getNormalComponents()) {
      final var compName = gate.getComponent().getFactory().getHDLName(gate.getComponent().getAttributeSet());
      if (!instantiatedComponents.contains(compName)) {
        instantiatedComponents.add(compName);
        final var worker = gate.getComponent().getFactory().getHDLGenerator(gate.getComponent().getAttributeSet());
        if (worker != null) {
          if (!worker.isOnlyInlined()) {
            components.empty().add(worker.getComponentInstantiation(theNetlist, 
                gate.getComponent().getAttributeSet(), compName));
          }
        }
      }
    }
    instantiatedComponents.clear();
    for (final var gate : theNetlist.getSubCircuits()) {
      var compName = gate.getComponent().getFactory().getHDLName(gate.getComponent().getAttributeSet());
      if (gate.isGatedInstance()) compName = compName.concat("_gated");
      if (!instantiatedComponents.contains(compName)) {
        instantiatedComponents.add(compName);
        final var worker = gate.getComponent().getFactory().getHDLGenerator(gate.getComponent().getAttributeSet());
        SubcircuitFactory sub = (SubcircuitFactory) gate.getComponent().getFactory();
        if (worker != null) {
          components.empty().add(worker.getComponentInstantiation(sub.getSubcircuit().getNetList(),
              gate.getComponent().getAttributeSet(), compName));
        }
      }
    }
    return components;
  }

  public Map<String, String> getHdlWiring(Netlist theNets) {
    final var contents = new HashMap<String, String>();
    // we cycle through all nets with a forced root net annotation
    for (final var thisNet : theNets.getAllNets()) {
      if (thisNet.isForcedRootNet()) {
        // now we cycle through all the bits
        final var wireId = theNets.getNetId(thisNet);
        for (var bit = 0; bit < thisNet.getBitWidth(); bit++) {
          // First we perform all source connections
          for (final var source : thisNet.getSourceNets(bit)) {
            final var destination = thisNet.isBus() ? LineBuffer.formatHdl("{{1}}{{2}}{{<}}{{3}}{{>}}", BUS_NAME, wireId, bit)
                :  LineBuffer.formatHdl("{{1}}{{2}}", NET_NAME, wireId);
            final var sourceWire = LineBuffer.formatHdl("{{1}}{{2}}{{<}}{{3}}{{>}}", BUS_NAME,
                theNets.getNetId(source.getParentNet()), source.getParentNetBitIndex());
            contents.put(destination, sourceWire);
          }
          // Next we perform all sink connections
          for (final var source : thisNet.getSinkNets(bit)) {
            final var destination = LineBuffer.formatHdl("{{1}}{{2}}{{<}}{{3}}{{>}}", BUS_NAME,
                theNets.getNetId(source.getParentNet()), source.getParentNetBitIndex());
            final var sourceWire = thisNet.isBus() ? LineBuffer.formatHdl("{{1}}{{2}}{{<}}{{3}}{{>}}", BUS_NAME, wireId, bit)
                :  LineBuffer.formatHdl("{{1}}{{2}}", NET_NAME, wireId);
            contents.put(destination, sourceWire);
          }
        }
      }
    }
    return contents;
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    var isFirstLine = true;
    final var compIds = new HashMap<String, Long>();
    final var wires = new HashMap<String, String>();
    /* we start with the connection of the clock sources */
    for (final var clockSource : theNetList.getClockSources()) {
      if (!clockSource.isEndConnected(0)) {
        // FIXME: hardcoded string
        final var msg = String.format("Clock component found with no connection, skipping: '%s'",
                clockSource.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
        Reporter.report.addWarning(msg);
        continue;
      }
      final var clockNet = Hdl.getClockNetName(clockSource, 0, theNetList);
      if (clockNet.isEmpty()) {
        // FIXME: hardcoded string
        Reporter.report.addFatalError("INTERNAL ERROR: Cannot find clocknet!");
      }
      final var destination = Hdl.getNetName(clockSource, 0, true, theNetList);
      final var source = theNetList.requiresGlobalClockConnection() ? TickComponentHdlGeneratorFactory.FPGA_CLOCK
          :  LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}", clockNet, ClockHdlGeneratorFactory.DERIVED_CLOCK_INDEX);
      wires.put(destination, source);
    }
    if (!wires.isEmpty()) {
      contents.empty().addRemarkBlock("All clock generator connections are defined here");
      Hdl.addAllWiresSorted(contents, wires);
    }
    /* Here we define all wiring; hence all complex splitter connections */
    wires.putAll(getHdlWiring(theNetList));
    if (!wires.isEmpty()) {
      contents.empty().addRemarkBlock("Here all wiring is defined");
      Hdl.addAllWiresSorted(contents, wires);
    }
    /* Now we define all input signals; hence Input port -> Internal Net */
    for (var i = 0; i < theNetList.getNumberOfInputPorts(); i++) {
      final var myInput = theNetList.getInputPin(i);
      final var pinName = CorrectLabel.getCorrectLabel(myInput.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      wires.putAll(getSignalMap(pinName, myInput, 0, theNetList));
    }
    if (!wires.isEmpty()) {
      contents.empty().addRemarkBlock("Here all input connections are defined");
      Hdl.addAllWiresSorted(contents, wires);
    }
    /* Now we define all output signals; hence Internal Net -> Input port */
    for (var i = 0; i < theNetList.numberOfOutputPorts(); i++) {
      netlistComponent myOutput = theNetList.getOutputPin(i);
      final var pinName = CorrectLabel.getCorrectLabel(myOutput.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      wires.putAll(getSignalMap(pinName, myOutput, 0, theNetList));
    }
    if (!wires.isEmpty()) {
      contents.empty().addRemarkBlock("Here all output connections are defined");
      Hdl.addAllWiresSorted(contents, wires);
    }
    /* Here all in-lined components are generated */
    isFirstLine = true;
    for (final var comp : theNetList.getNormalComponents()) {
      var worker = comp.getComponent().getFactory().getHDLGenerator(comp.getComponent().getAttributeSet());
      if (worker != null) {
        if (worker.isOnlyInlined()) {
          final var inlinedName = comp.getComponent().getFactory().getHDLName(comp.getComponent().getAttributeSet());
          final var InlinedId = "InlinedComponent";
          var id = (compIds.containsKey(InlinedId)) ? compIds.get(InlinedId) : (long) 1;
          if (isFirstLine) {
            contents.add("");
            contents.addRemarkBlock("Here all in-lined components are defined");
            isFirstLine = false;
          }
          final var thisAttrs = comp.getComponent().getAttributeSet();
          final var hasLabel = thisAttrs.containsAttribute(StdAttr.LABEL) 
              && !thisAttrs.getValue(StdAttr.LABEL).isEmpty();
          final var compName = hasLabel ? CorrectLabel.getCorrectLabel(thisAttrs.getValue(StdAttr.LABEL)) : "";
          final var remarkLine = LineBuffer.format("{{1}}{{2}}{{3}}", comp.getComponent().getFactory().getDisplayName(),
              hasLabel ? ": " : "", compName);
          contents
              .empty()
              .addRemarkLine(remarkLine)
              .add(worker.getInlinedCode(theNetList, id++, comp, inlinedName));
          compIds.put(InlinedId, id);
        }
      }
    }
    /* Here all "normal" components are generated */
    isFirstLine = true;
    for (final var comp : theNetList.getNormalComponents()) {
      var worker = comp.getComponent().getFactory().getHDLGenerator(comp.getComponent().getAttributeSet());
      if (worker != null) {
        if (!worker.isOnlyInlined()) {
          final var compName = comp.getComponent().getFactory().getHDLName(comp.getComponent().getAttributeSet());
          final var compId = "NormalComponent";
          var id = (compIds.containsKey(compId)) ? compIds.get(compId) : (long) 1;
          if (isFirstLine) {
            contents.empty().addRemarkBlock("Here all normal components are defined");
            isFirstLine = false;
          }
          contents.add(worker.getComponentMap(theNetList, id++, comp, compName)).empty();
          compIds.put(compId, id);
        }
      }
    }
    /* Finally we instantiate all sub-circuits */
    isFirstLine = true;
    for (final var comp : theNetList.getSubCircuits()) {
      final var worker = comp.getComponent().getFactory().getHDLGenerator(comp.getComponent().getAttributeSet());
      if (worker != null) {
        var compName = comp.getComponent().getFactory().getHDLName(comp.getComponent().getAttributeSet());
        if (comp.isGatedInstance())  compName = compName.concat("_gated");
        final var CompId = "SubCircuits";
        var id = (compIds.containsKey(CompId)) ? compIds.get(CompId) : (long) 1;
        final var compMap = worker.getComponentMap(theNetList, id++, comp, compName);
        if (!compMap.isEmpty()) {
          if (isFirstLine) {
            contents.empty().addRemarkBlock("Here all sub-circuits are defined");
            isFirstLine = false;
          }
          compIds.put(CompId, id);
          contents.empty().add(compMap);
        }
      }
    }
    contents.empty();
    return contents;
  }

  @Override
  public Map<String, String> getPortMap(Netlist nets, Object theMapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (theMapInfo == null) return null;
    final var topLevel = theMapInfo instanceof MappableResourcesContainer;
    final var componentInfo = topLevel ? null : (netlistComponent) theMapInfo;
    var mapInfo = topLevel ? (MappableResourcesContainer) theMapInfo : null;
    final var Preamble = topLevel ? "s_" : "";
    final var sub = topLevel ? null : (SubcircuitFactory) componentInfo.getComponent().getFactory();
    final var myNetList = topLevel ? nets : sub.getSubcircuit().getNetList();

    /* First we instantiate the Clock tree busses when present */
    for (var i = 0; i < myNetList.numberOfClockTrees(); i++) {
      portMap.put(CLOCK_TREE_NAME + i, Preamble + CLOCK_TREE_NAME + i);
    }
    if (myNetList.requiresGlobalClockConnection()) {
      portMap.put(TickComponentHdlGeneratorFactory.FPGA_CLOCK, TickComponentHdlGeneratorFactory.FPGA_CLOCK);
    }
    if (myNetList.getNumberOfInputBubbles() > 0) {
      portMap.put(LOCAL_INPUT_BUBBLE_BUS_NAME,
          LineBuffer.format("{{1}}{{2}}", topLevel ? Preamble : LOCAL_INPUT_BUBBLE_BUS_NAME,
              topLevel ? LOCAL_INPUT_BUBBLE_BUS_NAME : getBubbleIndex(componentInfo, bubbleType.INPUT)));
    }
    if (myNetList.numberOfOutputBubbles() > 0) {
      portMap.put(LOCAL_OUTPUT_BUBBLE_BUS_NAME,
          LineBuffer.format("{{1}}{{2}}", topLevel ? Preamble : LOCAL_OUTPUT_BUBBLE_BUS_NAME,
              topLevel ? LOCAL_OUTPUT_BUBBLE_BUS_NAME : getBubbleIndex(componentInfo, bubbleType.OUTPUT)));
    }

    final var nrOfIOBubbles = myNetList.numberOfInOutBubbles();
    if (nrOfIOBubbles > 0) {
      if (topLevel) {
        final var vector = new StringBuilder();
        for (var i = nrOfIOBubbles - 1; i >= 0; i--) {
          /* first pass find the component which is connected to this io */
          var compPin = -1;
          MapComponent map = null;
          for (final var key : mapInfo.getMappableResources().keySet()) {
            final var comp = mapInfo.getMappableResources().get(key);
            if (comp.hasIos()) {
              final var id = comp.getIoBubblePinId(i);
              if (id >= 0) {
                compPin = id;
                map = comp;
                break;
              }
            }
          }
          if (map == null || compPin < 0) {
            // FIXME: hardcoded string
            Reporter.report.addError("BUG: did not find IOpin");
            continue;
          }
          if (!map.isMapped(compPin) || map.isOpenMapped(compPin)) {
            if (Hdl.isVhdl())
              portMap.put(LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}", LOCAL_INOUT_BUBBLE_BUS_NAME, i), "OPEN");
            else {
              if (vector.length() != 0) vector.append(",");
              vector.append("OPEN"); // still not found the correct method but this seems to work
            }
          } else {
            if (Hdl.isVhdl())
              portMap.put(LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}", LOCAL_INOUT_BUBBLE_BUS_NAME, i),
                  (map.isExternalInverted(compPin) ? "n_" : "") + map.getHdlString(compPin));
            else {
              if (vector.length() != 0) vector.append(",");
              vector
                  .append(map.isExternalInverted(compPin) ? "n_" : "")
                  .append(map.getHdlString(compPin));
            }
          }
        }
        if (Hdl.isVerilog())
          portMap.put(LOCAL_INOUT_BUBBLE_BUS_NAME, vector.toString());
      } else {
        portMap.put(LOCAL_INOUT_BUBBLE_BUS_NAME, LOCAL_INOUT_BUBBLE_BUS_NAME + getBubbleIndex(componentInfo, bubbleType.INOUT));
      }
    }

    final var nrOfInputPorts = myNetList.getNumberOfInputPorts();
    if (nrOfInputPorts > 0) {
      for (var i = 0; i < nrOfInputPorts; i++) {
        netlistComponent selected = myNetList.getInputPin(i);
        if (selected != null) {
          final var pinLabel = CorrectLabel.getCorrectLabel(selected.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
          if (topLevel) {
            portMap.put(pinLabel, Preamble + pinLabel);
          } else {
            final var endId = nets.getEndIndex(componentInfo, pinLabel, false);
            if (endId < 0) {
              // FIXME: hardcoded string
              Reporter.report.addFatalError(
                  String.format("INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '%s'", pinLabel));
            } else {
              portMap.putAll(Hdl.getNetMap(pinLabel, true, componentInfo, endId, nets));
            }
          }
        }
      }
    }

    final var nrOfInOutPorts = myNetList.numberOfInOutPorts();
    if (nrOfInOutPorts > 0) {
      for (var i = 0; i < nrOfInOutPorts; i++) {
        final var selected = myNetList.getInOutPin(i);
        if (selected != null) {
          final var pinLabel = CorrectLabel.getCorrectLabel(selected.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
          if (topLevel) {
            /* Do not exist yet in logisim */
            /* TODO: implement by going over each bit */
          } else {
            final var endId = nets.getEndIndex(componentInfo, pinLabel, false);
            if (endId < 0) {
              // FIXME: hardcoded string
              Reporter.report.addFatalError(
                      String.format("INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '%s'", pinLabel));
            } else {
              portMap.putAll(Hdl.getNetMap(pinLabel, true, componentInfo, endId, nets));
            }
          }
        }
      }
    }

    final var nrOfOutputPorts = myNetList.numberOfOutputPorts();
    if (nrOfOutputPorts > 0) {
      for (var i = 0; i < nrOfOutputPorts; i++) {
        final var selected = myNetList.getOutputPin(i);
        if (selected != null) {
          final var pinLabel = CorrectLabel.getCorrectLabel(selected.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
          if (topLevel) {
            portMap.put(pinLabel, Preamble + pinLabel);
          } else {
            final var endid = nets.getEndIndex(componentInfo, pinLabel, true);
            if (endid < 0) {
              // FIXME: hardcoded string
              Reporter.report.addFatalError(
                      String.format("INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '%s'", pinLabel));
            } else {
              portMap.putAll(Hdl.getNetMap(pinLabel, true, componentInfo, endid, nets));
            }
          }
        }
      }
    }
    return portMap;
  }

  private static Map<String, String> getSignalMap(String portName, netlistComponent comp, int endIndex, Netlist theNets) {
    final var signal = new HashMap<String, String>();
    if ((endIndex < 0) || (endIndex >= comp.nrOfEnds())) {
      // FIXME: hardcoded string
      Reporter.report.addFatalErrorFmt(
              "INTERNAL ERROR: Component tried to index non-existing SolderPoint: '%s'",
              comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      return signal;
    }
    final var connectionInformation = comp.getEnd(endIndex);
    final var isInputConnection = connectionInformation.isOutputEnd();
    final var nrOfBits = connectionInformation.getNrOfBits();
    if (nrOfBits == 1) {
      /* Here we have the easy case, just a single bit net */
      if (isInputConnection) {
        if (comp.isEndConnected(endIndex)) signal.put(Hdl.getNetName(comp, endIndex, true, theNets), portName);
      } else {
        if (comp.isEndConnected(endIndex)) {
          signal.put(portName, Hdl.getNetName(comp, endIndex, true, theNets));
        } else {
          // FIXME: hardcoded string
          Reporter.report.addSevereWarning("Found an unconnected output pin, tied the pin to ground!");
          signal.put(portName, Hdl.zeroBit());
        }
      }
    } else {
      /*
       * Here we have the more difficult case, it is a bus that needs to
       * be mapped
       */
      /* First we check if the bus has a connection */
      var connected = false;
      for (var i = 0; i < nrOfBits; i++) {
        if (connectionInformation.get((byte) i).getParentNet() != null) connected = true;
      }
      if (!connected) {
        /* Here is the easy case, the bus is unconnected */
        if (!isInputConnection) {
          // FIXME: hardcoded string
          Reporter.report.addSevereWarning("Found an unconnected output bus pin, tied all the pin bits to ground!");
          signal.put(portName, Hdl.getZeroVector(nrOfBits, true));
        }
      } else {
        /*
         * There are connections, we detect if it is a continues bus
         * connection
         */
        if (theNets.isContinuesBus(comp, endIndex)) {
          /* Another easy case, the continues bus connection */
          if (isInputConnection) {
            signal.put(Hdl.getBusNameContinues(comp, endIndex, theNets), portName);
          } else {
            signal.put(portName, Hdl.getBusNameContinues(comp, endIndex, theNets));
          }
        } else {
          /* The last case, we have to enumerate through each bit */
          for (var bit = 0; bit < nrOfBits; bit++) {
            final var bitConnection = LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}", portName, bit);
            final var solderPoint = connectionInformation.get((byte) bit);
            if (solderPoint.getParentNet() == null) {
              /* The net is not connected */
              if (isInputConnection) continue;
              // FIXME: hardcoded string
              Reporter.report.addSevereWarning(String.format("Found an unconnected output bus pin, tied bit %d to ground!", bit));
              signal.put(bitConnection, Hdl.zeroBit());
            } else {
              /*
               * The net is connected, we have to find out if the
               * connection is to a bus or to a normal net
               */
              final var connectedNet = solderPoint.getParentNet().getBitWidth() == 1
                    ? LineBuffer.formatHdl("{{1}}{{2}}", NET_NAME, theNets.getNetId(solderPoint.getParentNet()))
                    : LineBuffer.formatHdl("{{1}}{{2}}{{<}}{{3}}{{>}}", BUS_NAME,
                        theNets.getNetId(solderPoint.getParentNet()), solderPoint.getParentNetBitIndex());
              if (isInputConnection) {
                signal.put(connectedNet, bitConnection);
              } else {
                signal.put(bitConnection, connectedNet);
              }
            }
          }
        }
      }
    }
    return signal;
  }
}
