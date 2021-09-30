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
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class CircuitHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  private final Circuit myCircuit;

  public CircuitHDLGeneratorFactory(Circuit source) {
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
      myPorts.add(Port.INPUT, String.format("%s%d", CLOCK_TREE_NAME, clock), ClockHDLGeneratorFactory.NR_OF_CLOCK_BITS, 0);
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
    if (outputBubbles > 0)
      myPorts.add(Port.OUTPUT, LOCAL_OUTPUT_BUBBLE_BUS_NAME, outputBubbles > 1 ? outputBubbles : 0, 0);
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
          (CircuitHDLGeneratorFactory)
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
  private String GetBubbleIndex(netlistComponent comp, int type) {
    return switch (type) {
      case 0 -> Hdl.bracketOpen()
            + comp.getLocalBubbleInputEndId()
            + Hdl.vectorLoopId()
            + comp.getLocalBubbleInputStartId()
            + Hdl.bracketClose();
      case 1 -> Hdl.bracketOpen()
            + comp.getLocalBubbleOutputEndId()
            + Hdl.vectorLoopId()
            + comp.getLocalBubbleOutputStartId()
            + Hdl.bracketClose();
      case 2 -> Hdl.bracketOpen()
            + comp.getLocalBubbleInOutEndId()
            + Hdl.vectorLoopId()
            + comp.getLocalBubbleInOutStartId()
            + Hdl.bracketClose();
      default -> "";
    };
  }

  @Override
  public List<String> getComponentDeclarationSection(Netlist theNetlist, AttributeSet attrs) {
    final var components = new ArrayList<String>();
    final var instantiatedComponents = new HashSet<String>();
    for (final var gate : theNetlist.getNormalComponents()) {
      final var compName =
          gate.getComponent().getFactory().getHDLName(gate.getComponent().getAttributeSet());
      if (!instantiatedComponents.contains(compName)) {
        instantiatedComponents.add(compName);
        final var worker =
            gate.getComponent()
                .getFactory()
                .getHDLGenerator(gate.getComponent().getAttributeSet());
        if (worker != null) {
          if (!worker.isOnlyInlined()) {
            components.addAll(
                worker.getComponentInstantiation(
                    theNetlist,
                    gate.getComponent().getAttributeSet(),
                    compName));
          }
        }
      }
    }
    instantiatedComponents.clear();
    for (final var gate : theNetlist.getSubCircuits()) {
      var compName =
          gate.getComponent().getFactory().getHDLName(gate.getComponent().getAttributeSet());
      if (gate.isGatedInstance()) compName = compName.concat("_gated");
      if (!instantiatedComponents.contains(compName)) {
        instantiatedComponents.add(compName);
        final var worker =
            gate.getComponent()
                .getFactory()
                .getHDLGenerator(gate.getComponent().getAttributeSet());
        SubcircuitFactory sub = (SubcircuitFactory) gate.getComponent().getFactory();
        if (worker != null) {
          components.addAll(
              worker.getComponentInstantiation(
                  sub.getSubcircuit().getNetList(),
                  gate.getComponent().getAttributeSet(),
                  compName));
        }
      }
    }
    return components;
  }

  public List<String> GetHDLWiring(Netlist theNets) {
    final var contents = LineBuffer.getHdlBuffer();
    final var oneLine = new StringBuilder();
    /* we cycle through all nets with a forcedrootnet annotation */
    for (final var thisNet : theNets.getAllNets()) {
      if (thisNet.isForcedRootNet()) {
        /* now we cycle through all the bits */
        for (var bit = 0; bit < thisNet.getBitWidth(); bit++) {
          /* First we perform all source connections */
          for (final var source : thisNet.getSourceNets(bit)) {
            oneLine.setLength(0);
            if (thisNet.isBus()) {
              oneLine.append(BUS_NAME)
                  .append(theNets.getNetId(thisNet))
                  .append(Hdl.bracketOpen())
                  .append(bit)
                  .append(Hdl.bracketClose());
            } else {
              oneLine.append(NET_NAME).append(theNets.getNetId(thisNet));
            }
            while (oneLine.length() < SIGNAL_ALLIGNMENT_SIZE) oneLine.append(" ");

            contents.addUnique(LineBuffer.format("   {{assign}} {{1}} {{=}} {{2}}{{3}}{{<}}{{4}}{{>}};",
                oneLine, BUS_NAME, theNets.getNetId(source.getParentNet()), source.getParentNetBitIndex()));
          }
          /* Next we perform all sink connections */
          for (final var source : thisNet.getSinkNets(bit)) {
            oneLine.setLength(0);
            oneLine.append(BUS_NAME)
                .append(theNets.getNetId(source.getParentNet()))
                .append(Hdl.bracketOpen())
                .append(source.getParentNetBitIndex())
                .append(Hdl.bracketClose());
            while (oneLine.length() < SIGNAL_ALLIGNMENT_SIZE) oneLine.append(" ");
            oneLine.append(Hdl.assignOperator());
            if (thisNet.isBus()) {
              oneLine.append(BUS_NAME)
                  .append(theNets.getNetId(thisNet))
                  .append(Hdl.bracketOpen())
                  .append(bit)
                  .append(Hdl.bracketClose());
            } else {
              oneLine.append(NET_NAME).append(theNets.getNetId(thisNet));
            }
            contents.addUnique(LineBuffer.format("   {{1}}{{2}};", Hdl.assignPreamble(), oneLine));
          }
        }
      }
    }
    return contents.get();
  }

  @Override
  public List<String> getModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    var isFirstLine = true;
    final var temp = new StringBuilder();
    final var compIds = new HashMap<String, Long>();
    /* we start with the connection of the clock sources */
    for (final var clockSource : theNetList.getClockSources()) {
      if (isFirstLine) {
        contents.add("");
        contents.addRemarkBlock("Here all clock generator connections are defined");
        isFirstLine = false;
      }
      if (!clockSource.isEndConnected(0)) {
        // FIXME: hardcoded string
        final var msg = String.format("Clock component found with no connection, skipping: '%s'",
                clockSource.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
        if (clockSource.getComponent().getAttributeSet().getValue(StdAttr.LABEL).equals("sysclk")) {
          Reporter.report.addInfo(msg);
        } else {
          Reporter.report.addWarning(msg);
        }
        continue;
      }
      final var clockNet = Hdl.getClockNetName(clockSource, 0, theNetList);
      if (clockNet.isEmpty()) {
        // FIXME: hardcoded string
        Reporter.report.addFatalError("INTERNAL ERROR: Cannot find clocknet!");
      }
      final var connectedNet = Hdl.getNetName(clockSource, 0, true, theNetList);
      temp.setLength(0);
      temp.append(connectedNet);
      // Padding
      while (temp.length() < SIGNAL_ALLIGNMENT_SIZE) {
        temp.append(" ");
      }
      if (!theNetList.requiresGlobalClockConnection()) {
        contents.add("   {{assign}} {{1}} {{=}} {{2}}{{<}}{{3}}{{>}};", temp, clockNet, ClockHDLGeneratorFactory.DERIVED_CLOCK_INDEX);
      } else {
        contents.add("   {{assign}} {{1}} {{=}} {{2}};", temp, TickComponentHdlGeneratorFactory.FPGA_CLOCK);
      }
    }
    /* Here we define all wiring; hence all complex splitter connections */
    final var wiring = GetHDLWiring(theNetList);
    if (!wiring.isEmpty()) {
      contents.add("");
      contents.addRemarkBlock("Here all wiring is defined");
      contents.add(wiring);
    }
    /* Now we define all input signals; hence Input port -> Internal Net */
    isFirstLine = true;
    for (var i = 0; i < theNetList.getNumberOfInputPorts(); i++) {
      if (isFirstLine) {
        contents.add("").addRemarkBlock("Here all input connections are defined");
        isFirstLine = false;
      }
      final var myInput = theNetList.getInputPin(i);
      contents.add(
          getSignalMap(
              CorrectLabel.getCorrectLabel(myInput.getComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              myInput,
              0,
              3,
              theNetList));
    }
    /* Now we define all output signals; hence Internal Net -> Input port */
    isFirstLine = true;
    for (var i = 0; i < theNetList.numberOfOutputPorts(); i++) {
      if (isFirstLine) {
        contents.add("");
        contents.addRemarkBlock("Here all output connections are defined");
        isFirstLine = false;
      }
      netlistComponent myOutput = theNetList.getOutputPin(i);
      contents.add(
          getSignalMap(
              CorrectLabel.getCorrectLabel(myOutput.getComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              myOutput,
              0,
              3,
              theNetList));
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
          contents.add(worker.getInlinedCode(theNetList, id++, comp, inlinedName));
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
            contents.add("").addRemarkBlock("Here all normal components are defined");
            isFirstLine = false;
          }
          contents.add(worker.getComponentMap(theNetList, id++, comp, compName));
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
            contents.add("").addRemarkBlock("Here all sub-circuits are defined");
            isFirstLine = false;
          }
          compIds.remove(CompId);
          compIds.put(CompId, id);
          contents.add(compMap);
        }
      }
    }
    contents.add("");
    return contents.get();
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
      // FIXME: remove + by concatination.
      portMap.put(LOCAL_INPUT_BUBBLE_BUS_NAME,
          topLevel ? Preamble + LOCAL_INPUT_BUBBLE_BUS_NAME : LOCAL_INPUT_BUBBLE_BUS_NAME + GetBubbleIndex(componentInfo, 0));
    }
    if (myNetList.numberOfOutputBubbles() > 0) {
      // FIXME: remove + by concatination.
      portMap.put(LOCAL_OUTPUT_BUBBLE_BUS_NAME,
          topLevel ? Preamble + LOCAL_OUTPUT_BUBBLE_BUS_NAME : LOCAL_OUTPUT_BUBBLE_BUS_NAME + GetBubbleIndex(componentInfo, 1));
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
            // FIXME: rewrite using LineBuffer
            if (Hdl.isVhdl())
              portMap.put(LOCAL_INOUT_BUBBLE_BUS_NAME + "(" + i + ")", "OPEN");
            else {
              if (vector.length() != 0) vector.append(",");
              vector.append("OPEN"); // still not found the correct method but this seems to work
            }
          } else {
            if (Hdl.isVhdl())
              portMap.put(
                  LOCAL_INOUT_BUBBLE_BUS_NAME + "(" + i + ")",
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
        portMap.put(LOCAL_INOUT_BUBBLE_BUS_NAME, LOCAL_INOUT_BUBBLE_BUS_NAME + GetBubbleIndex(componentInfo, 2));
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

  private String getSignalMap(String portName, netlistComponent comp, int endIndex, int tabSize, Netlist theNets) {
    final var contents = new StringBuilder();
    final var source = new StringBuilder();
    final var destination = new StringBuilder();
    final var tab = new StringBuilder();
    if ((endIndex < 0) || (endIndex >= comp.nrOfEnds())) {
      // FIXME: hardcoded string
      Reporter.report.addFatalErrorFmt(
              "INTERNAL ERROR: Component tried to index non-existing SolderPoint: '%s'",
              comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      return "";
    }
    tab.append(" ".repeat(tabSize));
    final var connectionInformation = comp.getEnd(endIndex);
    final var isOutput = connectionInformation.isOutputEnd();
    final var nrOfBits = connectionInformation.getNrOfBits();
    if (nrOfBits == 1) {
      /* Here we have the easy case, just a single bit net */
      if (isOutput) {
        if (!comp.isEndConnected(endIndex)) return " ";
        source.append(portName);
        destination.append(Hdl.getNetName(comp, endIndex, true, theNets));
      } else {
        if (!comp.isEndConnected(endIndex)) {
          // FIXME: hardcoded string
          Reporter.report.addSevereWarning(
              "Found an unconnected output pin, tied the pin to ground!");
        }
        source.append(Hdl.getNetName(comp, endIndex, true, theNets));
        destination.append(portName);
        if (!comp.isEndConnected(endIndex)) return contents.toString();
      }
      while (destination.length() < SIGNAL_ALLIGNMENT_SIZE) destination.append(" ");
      contents
          .append(tab)
          .append(Hdl.assignPreamble())
          .append(destination)
          .append(Hdl.assignOperator())
          .append(source)
          .append(";");
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
        if (isOutput) return contents.toString();
        // FIXME: hardcoded string
        Reporter.report.addSevereWarning("Found an unconnected output bus pin, tied all the pin bits to ground!");
        destination.append(portName);
        while (destination.length() < SIGNAL_ALLIGNMENT_SIZE) destination.append(" ");
        contents
            .append(tab)
            .append(Hdl.assignPreamble())
            .append(destination)
            .append(Hdl.assignOperator())
            .append(Hdl.getZeroVector(nrOfBits, true))
            .append(";");
      } else {
        /*
         * There are connections, we detect if it is a continues bus
         * connection
         */
        if (theNets.isContinuesBus(comp, endIndex)) {
          destination.setLength(0);
          source.setLength(0);
          /* Another easy case, the continues bus connection */
          if (isOutput) {
            source.append(portName);
            destination.append(Hdl.getBusNameContinues(comp, endIndex, theNets));
          } else {
            destination.append(portName);
            source.append(Hdl.getBusNameContinues(comp, endIndex, theNets));
          }
          while (destination.length() < SIGNAL_ALLIGNMENT_SIZE) destination.append(" ");
          contents
              .append(tab)
              .append(Hdl.assignPreamble())
              .append(destination)
              .append(Hdl.assignOperator())
              .append(source)
              .append(";");
        } else {
          /* The last case, we have to enumerate through each bit */
          for (var bit = 0; bit < nrOfBits; bit++) {
            source.setLength(0);
            destination.setLength(0);
            if (isOutput) {
              source
                  .append(portName)
                  .append(Hdl.bracketOpen())
                  .append(bit)
                  .append(Hdl.bracketClose());
            } else {
              destination
                  .append(portName)
                  .append(Hdl.bracketOpen())
                  .append(bit)
                  .append(Hdl.bracketClose());
            }
            final var solderPoint = connectionInformation.get((byte) bit);
            if (solderPoint.getParentNet() == null) {
              /* The net is not connected */
              if (isOutput) continue;
              // FIXME: hardcoded string
              Reporter.report.addSevereWarning(String.format("Found an unconnected output bus pin, tied bit %d to ground!", bit));
              source.append(Hdl.getZeroVector(1, true));
            } else {
              /*
               * The net is connected, we have to find out if the
               * connection is to a bus or to a normal net
               */
              if (solderPoint.getParentNet().getBitWidth() == 1) {
                /* The connection is to a Net */
                if (isOutput) {
                  destination.append(NET_NAME).append(theNets.getNetId(solderPoint.getParentNet()));
                } else {
                  source.append(NET_NAME).append(theNets.getNetId(solderPoint.getParentNet()));
                }
              } else {
                /* The connection is to an entry of a bus */
                if (isOutput) {
                  destination
                      .append(BUS_NAME)
                      .append(theNets.getNetId(solderPoint.getParentNet()))
                      .append(Hdl.bracketOpen())
                      .append(solderPoint.getParentNetBitIndex())
                      .append(Hdl.bracketClose());
                } else {
                  source
                      .append(BUS_NAME)
                      .append(theNets.getNetId(solderPoint.getParentNet()))
                      .append(Hdl.bracketOpen())
                      .append(solderPoint.getParentNetBitIndex())
                      .append(Hdl.bracketClose());
                }
              }
            }
            while (destination.length() < SIGNAL_ALLIGNMENT_SIZE) destination.append(" ");
            if (bit != 0) contents.append("\n");
            contents
                .append(tab)
                .append(Hdl.assignPreamble())
                .append(destination)
                .append(Hdl.assignOperator())
                .append(source)
                .append(";");
          }
        }
      }
    }
    return contents.toString();
  }
}
