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
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.ConnectionPoint;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Net;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class CircuitHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private final Circuit MyCircuit;

  public CircuitHDLGeneratorFactory(Circuit source) {
    MyCircuit = source;
  }

  @Override
  public boolean GenerateAllHDLDescriptions(
      Set<String> HandledComponents, String WorkingDir, ArrayList<String> Hierarchy) {
    return GenerateAllHDLDescriptions(HandledComponents, WorkingDir, Hierarchy, false);
  }

  public boolean GenerateAllHDLDescriptions(
      Set<String> HandledComponents,
      String WorkingDir,
      ArrayList<String> Hierarchy,
      boolean gatedInstance) {
    if (MyCircuit == null) {
      return false;
    }
    if (Hierarchy == null) {
      Hierarchy = new ArrayList<>();
    }
    Netlist MyNetList = MyCircuit.getNetList();
    if (MyNetList == null) {
      return false;
    }
    String WorkPath = WorkingDir;
    if (!WorkPath.endsWith(File.separator)) {
      WorkPath += File.separator;
    }
    MyNetList.setCurrentHierarchyLevel(Hierarchy);
    /* First we handle the normal components */
    for (NetlistComponent ThisComponent : MyNetList.getNormalComponents()) {
      String ComponentName =
          ThisComponent.getComponent()
              .getFactory()
              .getHDLName(ThisComponent.getComponent().getAttributeSet());
      if (!HandledComponents.contains(ComponentName)) {
        HDLGeneratorFactory Worker =
            ThisComponent.getComponent()
                .getFactory()
                .getHDLGenerator(ThisComponent.getComponent().getAttributeSet());
        if (Worker == null) {
          Reporter.Report.AddFatalError(
              "INTERNAL ERROR: Cannot find the VHDL generator factory for component "
                  + ComponentName);
          return false;
        }
        if (!Worker.IsOnlyInlined()) {
          if (!WriteEntity(
              WorkPath + Worker.GetRelativeDirectory(),
              Worker.GetEntity(
                  MyNetList,
                  ThisComponent.getComponent().getAttributeSet(),
                  ComponentName),
              ComponentName)) {
            return false;
          }
          if (!WriteArchitecture(
              WorkPath + Worker.GetRelativeDirectory(),
              Worker.GetArchitecture(
                  MyNetList,
                  ThisComponent.getComponent().getAttributeSet(),
                  ComponentName),
              ComponentName)) {
            return false;
          }
        }
        HandledComponents.add(ComponentName);
      }
    }
    /* Now we go down the hierarchy to get all other components */
    for (NetlistComponent ThisCircuit : MyNetList.getSubCircuits()) {
      CircuitHDLGeneratorFactory Worker =
          (CircuitHDLGeneratorFactory)
              ThisCircuit.getComponent()
                  .getFactory()
                  .getHDLGenerator(ThisCircuit.getComponent().getAttributeSet());
      if (Worker == null) {
        Reporter.Report.AddFatalError(
            "INTERNAL ERROR: Unable to get a subcircuit VHDL generator for '"
                + ThisCircuit.getComponent().getFactory().getName()
                + "'");
        return false;
      }
      Hierarchy.add(
          CorrectLabel.getCorrectLabel(
              ThisCircuit.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      if (!Worker.GenerateAllHDLDescriptions(
          HandledComponents, WorkingDir, Hierarchy, ThisCircuit.isGatedInstance())) {
        return false;
      }
      Hierarchy.remove(Hierarchy.size() - 1);
    }
    /* I also have to generate myself */
    String ComponentName = CorrectLabel.getCorrectLabel(MyCircuit.getName());
    if (gatedInstance) ComponentName = ComponentName.concat("_gated");
    if (!HandledComponents.contains(ComponentName)) {
      if (!WriteEntity(WorkPath + GetRelativeDirectory(),
          GetEntity(MyNetList, null, ComponentName), ComponentName)) 
        return false;
      if (!WriteArchitecture(WorkPath + GetRelativeDirectory(),
          GetArchitecture(MyNetList, null, ComponentName), ComponentName)) 
        return false;
    }
    HandledComponents.add(ComponentName);
    return true;
  }

  /* here the private handles are defined */
  private String GetBubbleIndex(NetlistComponent comp, int type) {
    switch (type) {
      case 0:
        return HDL.BracketOpen()
            + comp.getLocalBubbleInputEndId()
            + HDL.vectorLoopId()
            + comp.getLocalBubbleInputStartId()
            + HDL.BracketClose();
      case 1:
        return HDL.BracketOpen()
            + comp.getLocalBubbleOutputEndId()
            + HDL.vectorLoopId()
            + comp.getLocalBubbleOutputStartId()
            + HDL.BracketClose();
      case 2:
        return HDL.BracketOpen()
            + comp.getLocalBubbleInOutEndId()
            + HDL.vectorLoopId()
            + comp.getLocalBubbleInOutStartId()
            + HDL.BracketClose();
    }
    return "";
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Components = new ArrayList<>();
    Set<String> InstantiatedComponents = new HashSet<>();
    for (NetlistComponent Gate : TheNetlist.getNormalComponents()) {
      String CompName =
          Gate.getComponent().getFactory().getHDLName(Gate.getComponent().getAttributeSet());
      if (!InstantiatedComponents.contains(CompName)) {
        InstantiatedComponents.add(CompName);
        HDLGeneratorFactory Worker =
            Gate.getComponent()
                .getFactory()
                .getHDLGenerator(Gate.getComponent().getAttributeSet());
        if (Worker != null) {
          if (!Worker.IsOnlyInlined()) {
            Components.addAll(
                Worker.GetComponentInstantiation(
                    TheNetlist,
                    Gate.getComponent().getAttributeSet(),
                    CompName));
          }
        }
      }
    }
    InstantiatedComponents.clear();
    for (NetlistComponent Gate : TheNetlist.getSubCircuits()) {
      String CompName =
          Gate.getComponent().getFactory().getHDLName(Gate.getComponent().getAttributeSet());
      if (Gate.isGatedInstance()) CompName = CompName.concat("_gated");
      if (!InstantiatedComponents.contains(CompName)) {
        InstantiatedComponents.add(CompName);
        HDLGeneratorFactory Worker =
            Gate.getComponent()
                .getFactory()
                .getHDLGenerator(Gate.getComponent().getAttributeSet());
        SubcircuitFactory sub = (SubcircuitFactory) Gate.getComponent().getFactory();
        if (Worker != null) {
          Components.addAll(
              Worker.GetComponentInstantiation(
                  sub.getSubcircuit().getNetList(),
                  Gate.getComponent().getAttributeSet(),
                  CompName));
        }
      }
    }
    return Components;
  }

  @Override
  public String getComponentStringIdentifier() {
    return CorrectLabel.getCorrectLabel(MyCircuit.getName());
  }

  public ArrayList<String> GetHDLWiring(Netlist TheNets) {
    final var Contents = (new LineBuffer()).addHdlPairs();
    final StringBuilder OneLine = new StringBuilder();
    /* we cycle through all nets with a forcedrootnet annotation */
    for (Net ThisNet : TheNets.getAllNets()) {
      if (ThisNet.isForcedRootNet()) {
        /* now we cycle through all the bits */
        for (int bit = 0; bit < ThisNet.getBitWidth(); bit++) {
          /* First we perform all source connections */
          for (ConnectionPoint Source : ThisNet.getSourceNets(bit)) {
            OneLine.setLength(0);
            if (ThisNet.isBus()) {
              OneLine.append(BusName)
                  .append(TheNets.getNetId(ThisNet))
                  .append(HDL.BracketOpen())
                  .append(bit)
                  .append(HDL.BracketClose());
            } else {
              OneLine.append(NetName).append(TheNets.getNetId(ThisNet));
            }
            while (OneLine.length() < SallignmentSize) OneLine.append(" ");

            Contents.addUnique(LineBuffer.format("   {{assign}} {{1}} {{=}} {{2}}{{3}}{{bracketOpen}}{{4}}{{bracketClose}};",
                OneLine, BusName, TheNets.getNetId(Source.getParentNet()), Source.getParentNetBitIndex()));
          }
          /* Next we perform all sink connections */
          for (ConnectionPoint Source : ThisNet.getSinkNets(bit)) {
            OneLine.setLength(0);
            OneLine.append(BusName)
                .append(TheNets.getNetId(Source.getParentNet()))
                .append(HDL.BracketOpen())
                .append(Source.getParentNetBitIndex())
                .append(HDL.BracketClose());
            while (OneLine.length() < SallignmentSize) OneLine.append(" ");
            OneLine.append(HDL.assignOperator());
            if (ThisNet.isBus()) {
              OneLine.append(BusName)
                  .append(TheNets.getNetId(ThisNet))
                  .append(HDL.BracketOpen())
                  .append(bit)
                  .append(HDL.BracketClose());
            } else {
              OneLine.append(NetName).append(TheNets.getNetId(ThisNet));
            }
            Contents.addUnique(LineBuffer.format("   {{1}}{{2}};", HDL.assignPreamble(), OneLine));
          }
        }
      }
    }
    return Contents.get();
  }

  @Override
  public SortedMap<String, Integer> GetInOutList(Netlist MyNetList, AttributeSet attrs) {
    final var InOuts = new TreeMap<String, Integer>();
    int InOutBubbles = MyNetList.numberOfInOutBubbles();
    if (InOutBubbles > 0) {
      if (InOutBubbles > 1) {
        InOuts.put(HDLGeneratorFactory.LocalInOutBubbleBusname, InOutBubbles);
      } else {
        InOuts.put(HDLGeneratorFactory.LocalInOutBubbleBusname, 0);
      }
    }
    return InOuts;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist MyNetList, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    for (int i = 0; i < MyNetList.numberOfClockTrees(); i++) {
      Inputs.put(ClockTreeName + i, ClockHDLGeneratorFactory.NR_OF_CLOCK_BITS);
    }
    if (MyNetList.requiresGlobalClockConnection()) {
      Inputs.put(TickComponentHDLGeneratorFactory.FPGA_CLOCK, 1);
    }
    int InputBubbles = MyNetList.getNumberOfInputBubbles();
    if (InputBubbles > 0) {
      if (InputBubbles > 1) {
        Inputs.put(HDLGeneratorFactory.LocalInputBubbleBusname, InputBubbles);
      } else {
        Inputs.put(HDLGeneratorFactory.LocalInputBubbleBusname, 0);
      }
    }
    for (int i = 0; i < MyNetList.getNumberOfInputPorts(); i++) {
      NetlistComponent selected = MyNetList.getInputPin(i);
      if (selected != null) {
        Inputs.put(
            CorrectLabel.getCorrectLabel(
                selected.getComponent().getAttributeSet().getValue(StdAttr.LABEL)),
            selected.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth());
      }
    }
    return Inputs;
  }

  @Override
  public String GetInstanceIdentifier(NetlistComponent componentInfo, Long componentId) {
    if (componentInfo != null) {
      String compId =
          CorrectLabel.getCorrectLabel(
              componentInfo.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (!compId.isEmpty()) {
        return compId;
      }
    }
    return getComponentStringIdentifier() + "_" + componentId.toString();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = (new LineBuffer()).addHdlPairs();
    var isFirstLine = true;
    final var temp = new StringBuilder();
    final var compIds = new HashMap<String, Long>();
    /* we start with the connection of the clock sources */
    for (final var clockSource : theNetlist.getClockSources()) {
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
          Reporter.Report.AddInfo(msg);
        } else {
          Reporter.Report.AddWarning(msg);
        }
        continue;
      }
      final var clockNet = GetClockNetName(clockSource, 0, theNetlist);
      if (clockNet.isEmpty()) {
        // FIXME: hardcoded string
        Reporter.Report.AddFatalError("INTERNAL ERROR: Cannot find clocknet!");
      }
      String ConnectedNet = GetNetName(clockSource, 0, true, theNetlist);
      temp.setLength(0);
      temp.append(ConnectedNet);
      // Padding
      while (temp.length() < SallignmentSize) {
        temp.append(" ");
      }
      if (!theNetlist.requiresGlobalClockConnection()) {
        contents.add("   {{assign}} {{1}} {{=}} {{2}}{{bracketOpen}}{{3}}{{bracketClose}};", temp, clockNet, ClockHDLGeneratorFactory.DERIVED_CLOCK_INDEX);
      } else {
        contents.add("   {{assign}} {{1}} {{=}} {{2}};", temp, TickComponentHDLGeneratorFactory.FPGA_CLOCK);
      }
    }
    /* Here we define all wiring; hence all complex splitter connections */
    final var wiring = GetHDLWiring(theNetlist);
    if (!wiring.isEmpty()) {
      contents.add("");
      contents.addRemarkBlock("Here all wiring is defined");
      contents.add(wiring);
    }
    /* Now we define all input signals; hence Input port -> Internal Net */
    isFirstLine = true;
    for (var i = 0; i < theNetlist.getNumberOfInputPorts(); i++) {
      if (isFirstLine) {
        contents.add("").addRemarkBlock("Here all input connections are defined");
        isFirstLine = false;
      }
      final var myInput = theNetlist.getInputPin(i);
      contents.add(
          getSignalMap(
              CorrectLabel.getCorrectLabel(myInput.getComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              myInput,
              0,
              3,
              theNetlist));
    }
    /* Now we define all output signals; hence Internal Net -> Input port */
    isFirstLine = true;
    for (var i = 0; i < theNetlist.numberOfOutputPorts(); i++) {
      if (isFirstLine) {
        contents.add("");
        contents.addRemarkBlock("Here all output connections are defined");
        isFirstLine = false;
      }
      NetlistComponent MyOutput = theNetlist.getOutputPin(i);
      contents.add(
          getSignalMap(
              CorrectLabel.getCorrectLabel(MyOutput.getComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              MyOutput,
              0,
              3,
              theNetlist));
    }
    /* Here all in-lined components are generated */
    isFirstLine = true;
    for (final var comp : theNetlist.getNormalComponents()) {
      var worker = comp.getComponent().getFactory().getHDLGenerator(comp.getComponent().getAttributeSet());
      if (worker != null) {
        if (worker.IsOnlyInlined()) {
          var inlinedName = comp.getComponent().getFactory().getHDLName(comp.getComponent().getAttributeSet());
          var InlinedId = worker.getComponentStringIdentifier();
          var id = (compIds.containsKey(InlinedId)) ? compIds.get(InlinedId) : (long) 1;
          if (isFirstLine) {
            contents.add("");
            contents.addRemarkBlock("Here all in-lined components are defined");
            isFirstLine = false;
          }
          contents.add(worker.GetInlinedCode(theNetlist, id++, comp, inlinedName));
          compIds.remove(InlinedId);
          compIds.put(InlinedId, id);
        }
      }
    }
    /* Here all "normal" components are generated */
    isFirstLine = true;
    for (final var comp : theNetlist.getNormalComponents()) {
      var worker = comp.getComponent().getFactory().getHDLGenerator(comp.getComponent().getAttributeSet());
      if (worker != null) {
        if (!worker.IsOnlyInlined()) {
          var compName = comp.getComponent().getFactory().getHDLName(comp.getComponent().getAttributeSet());
          var compId = worker.getComponentStringIdentifier();
          var id = (compIds.containsKey(compId)) ? compIds.get(compId) : (long) 1;
          if (isFirstLine) {
            contents.add("").addRemarkBlock("Here all normal components are defined");
            isFirstLine = false;
          }
          contents.add(worker.GetComponentMap(theNetlist, id++, comp, null, compName));
          compIds.remove(compId);
          compIds.put(compId, id);
        }
      }
    }
    /* Finally we instantiate all sub-circuits */
    isFirstLine = true;
    for (final var comp : theNetlist.getSubCircuits()) {
      final var worker = comp.getComponent().getFactory().getHDLGenerator(comp.getComponent().getAttributeSet());
      if (worker != null) {
        var compName = comp.getComponent().getFactory().getHDLName(comp.getComponent().getAttributeSet());
        if (comp.isGatedInstance())  compName = compName.concat("_gated");
        final var CompId = worker.getComponentStringIdentifier();
        var id = (compIds.containsKey(CompId)) ? compIds.get(CompId) : (long) 1;
        final var compMap = worker.GetComponentMap(theNetlist, id++, comp, null, compName);
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
  public SortedMap<String, Integer> GetOutputList(Netlist myNetList, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    final var outputBubbles = myNetList.numberOfOutputBubbles();
    if (outputBubbles > 0) {
      outputs.put(HDLGeneratorFactory.LocalOutputBubbleBusname, (outputBubbles == 1) ? 0 : outputBubbles);
    }

    for (var i = 0; i < myNetList.numberOfOutputPorts(); i++) {
      final var selected = myNetList.getOutputPin(i);
      if (selected != null) {
        outputs.put(
            CorrectLabel.getCorrectLabel(selected.getComponent().getAttributeSet().getValue(StdAttr.LABEL)),
            selected.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth());
      }
    }
    return outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object MapInfo) {
    final var PortMap = new TreeMap<String, String>();
    if (MapInfo == null) return null;
    final var topLevel = MapInfo instanceof MappableResourcesContainer;
    final var componentInfo = topLevel ? null : (NetlistComponent) MapInfo;
    var mapInfo = topLevel ? (MappableResourcesContainer) MapInfo : null;
    final var Preamble = topLevel ? "s_" : "";
    final var sub = topLevel ? null : (SubcircuitFactory) componentInfo.getComponent().getFactory();
    final var myNetList = topLevel ? nets : sub.getSubcircuit().getNetList();

    /* First we instantiate the Clock tree busses when present */
    for (var i = 0; i < myNetList.numberOfClockTrees(); i++) {
      PortMap.put(ClockTreeName + i, Preamble + ClockTreeName + i);
    }
    if (myNetList.requiresGlobalClockConnection()) {
      PortMap.put(TickComponentHDLGeneratorFactory.FPGA_CLOCK, TickComponentHDLGeneratorFactory.FPGA_CLOCK);
    }
    if (myNetList.getNumberOfInputBubbles() > 0) {
      PortMap.put(
          HDLGeneratorFactory.LocalInputBubbleBusname,
          topLevel
              ? Preamble + HDLGeneratorFactory.LocalInputBubbleBusname
              : HDLGeneratorFactory.LocalInputBubbleBusname + GetBubbleIndex(componentInfo, 0));
    }
    if (myNetList.numberOfOutputBubbles() > 0) {
      PortMap.put(
          HDLGeneratorFactory.LocalOutputBubbleBusname,
          topLevel
              ? Preamble + HDLGeneratorFactory.LocalOutputBubbleBusname
              : HDLGeneratorFactory.LocalOutputBubbleBusname + GetBubbleIndex(componentInfo, 1));
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
            if (comp.hasIOs()) {
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
            Reporter.Report.AddError("BUG: did not find IOpin");
            continue;
          }
          if (!map.isMapped(compPin) || map.IsOpenMapped(compPin)) {
            if (HDL.isVHDL())
              PortMap.put(HDLGeneratorFactory.LocalInOutBubbleBusname + "(" + i + ")", "OPEN");
            else {
              if (vector.length() != 0) vector.append(",");
              vector.append("OPEN"); // still not found the correct method but this seems to work
            }
          } else {
            if (HDL.isVHDL())
              PortMap.put(
                  HDLGeneratorFactory.LocalInOutBubbleBusname + "(" + i + ")",
                  (map.isExternalInverted(compPin) ? "n_" : "") + map.getHdlString(compPin));
            else {
              if (vector.length() != 0) vector.append(",");
              vector
                  .append(map.isExternalInverted(compPin) ? "n_" : "")
                  .append(map.getHdlString(compPin));
            }
          }
        }
        if (HDL.isVerilog())
          PortMap.put(HDLGeneratorFactory.LocalInOutBubbleBusname, vector.toString());
      } else {
        PortMap.put(
            HDLGeneratorFactory.LocalInOutBubbleBusname,
            HDLGeneratorFactory.LocalInOutBubbleBusname + GetBubbleIndex(componentInfo, 2));
      }
    }

    final var nrOfInputPorts = myNetList.getNumberOfInputPorts();
    if (nrOfInputPorts > 0) {
      for (var i = 0; i < nrOfInputPorts; i++) {
        NetlistComponent selected = myNetList.getInputPin(i);
        if (selected != null) {
          final var pinLabel = CorrectLabel.getCorrectLabel(selected.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
          if (topLevel) {
            PortMap.put(pinLabel, Preamble + pinLabel);
          } else {
            final var endId = nets.getEndIndex(componentInfo, pinLabel, false);
            if (endId < 0) {
              // FIXME: hardcoded string
              Reporter.Report.AddFatalError(
                  String.format("INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '%s'", pinLabel));
            } else {
              PortMap.putAll(GetNetMap(pinLabel, true, componentInfo, endId, nets));
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
              Reporter.Report.AddFatalError(
                      String.format("INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '%s'", pinLabel));
            } else {
              PortMap.putAll(GetNetMap(pinLabel, true, componentInfo, endId, nets));
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
            PortMap.put(pinLabel, Preamble + pinLabel);
          } else {
            final var endid = nets.getEndIndex(componentInfo, pinLabel, true);
            if (endid < 0) {
              // FIXME: hardcoded string
              Reporter.Report.AddFatalError(
                      String.format("INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '%s'", pinLabel));
            } else {
              PortMap.putAll(GetNetMap(pinLabel, true, componentInfo, endid, nets));
            }
          }
        }
      }
    }
    return PortMap;
  }

  private String getSignalMap(String portName, NetlistComponent comp, int endIndex, int tabSize, Netlist TheNets) {
    final var contents = new StringBuilder();
    final var source = new StringBuilder();
    final var destination = new StringBuilder();
    final var tab = new StringBuilder();
    if ((endIndex < 0) || (endIndex >= comp.nrOfEnds())) {
      // FIXME: hardcoded string
      Reporter.Report.AddFatalError(
          String.format(
              "INTERNAL ERROR: Component tried to index non-existing SolderPoint: '%s'",
              comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
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
        destination.append(GetNetName(comp, endIndex, true, TheNets));
      } else {
        if (!comp.isEndConnected(endIndex)) {
          // FIXME: hardcoded string
          Reporter.Report.AddSevereWarning(
              "Found an unconnected output pin, tied the pin to ground!");
        }
        source.append(GetNetName(comp, endIndex, true, TheNets));
        destination.append(portName);
        if (!comp.isEndConnected(endIndex)) return contents.toString();
      }
      while (destination.length() < SallignmentSize) destination.append(" ");
      contents
          .append(tab)
          .append(HDL.assignPreamble())
          .append(destination)
          .append(HDL.assignOperator())
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
        Reporter.Report.AddSevereWarning("Found an unconnected output bus pin, tied all the pin bits to ground!");
        destination.append(portName);
        while (destination.length() < SallignmentSize) destination.append(" ");
        contents
            .append(tab)
            .append(HDL.assignPreamble())
            .append(destination)
            .append(HDL.assignOperator())
            .append(HDL.GetZeroVector(nrOfBits, true))
            .append(";");
      } else {
        /*
         * There are connections, we detect if it is a continues bus
         * connection
         */
        if (TheNets.isContinuesBus(comp, endIndex)) {
          destination.setLength(0);
          source.setLength(0);
          /* Another easy case, the continues bus connection */
          if (isOutput) {
            source.append(portName);
            destination.append(GetBusNameContinues(comp, endIndex, TheNets));
          } else {
            destination.append(portName);
            source.append(GetBusNameContinues(comp, endIndex, TheNets));
          }
          while (destination.length() < SallignmentSize) destination.append(" ");
          contents
              .append(tab)
              .append(HDL.assignPreamble())
              .append(destination)
              .append(HDL.assignOperator())
              .append(source)
              .append(";");
        } else {
          /* The last case, we have to enumerate through each bit */
          for (int bit = 0; bit < nrOfBits; bit++) {
            source.setLength(0);
            destination.setLength(0);
            if (isOutput) {
              source
                  .append(portName)
                  .append(HDL.BracketOpen())
                  .append(bit)
                  .append(HDL.BracketClose());
            } else {
              destination
                  .append(portName)
                  .append(HDL.BracketOpen())
                  .append(bit)
                  .append(HDL.BracketClose());
            }
            final var solderPoint = connectionInformation.get((byte) bit);
            if (solderPoint.getParentNet() == null) {
              /* The net is not connected */
              if (isOutput) continue;
              // FIXME: hardcoded string
              Reporter.Report.AddSevereWarning(String.format("Found an unconnected output bus pin, tied bit %d to ground!", bit));
              source.append(HDL.GetZeroVector(1, true));
            } else {
              /*
               * The net is connected, we have to find out if the
               * connection is to a bus or to a normal net
               */
              if (solderPoint.getParentNet().getBitWidth() == 1) {
                /* The connection is to a Net */
                if (isOutput) {
                  destination.append(NetName).append(TheNets.getNetId(solderPoint.getParentNet()));
                } else {
                  source.append(NetName).append(TheNets.getNetId(solderPoint.getParentNet()));
                }
              } else {
                /* The connection is to an entry of a bus */
                if (isOutput) {
                  destination
                      .append(BusName)
                      .append(TheNets.getNetId(solderPoint.getParentNet()))
                      .append(HDL.BracketOpen())
                      .append(solderPoint.getParentNetBitIndex())
                      .append(HDL.BracketClose());
                } else {
                  source
                      .append(BusName)
                      .append(TheNets.getNetId(solderPoint.getParentNet()))
                      .append(HDL.BracketOpen())
                      .append(solderPoint.getParentNetBitIndex())
                      .append(HDL.BracketClose());
                }
              }
            }
            while (destination.length() < SallignmentSize) destination.append(" ");
            if (bit != 0) contents.append("\n");
            contents
                .append(tab)
                .append(HDL.assignPreamble())
                .append(destination)
                .append(HDL.assignOperator())
                .append(source)
                .append(";");
          }
        }
      }
    }
    return contents.toString();
  }

  @Override
  public String GetSubDir() {
    return "circuit";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist nets) {
    final var signalMap = new TreeMap<String, Integer>();

    /* First we define the nets */
    for (final var thisNet : nets.getAllNets()) {
      if (!thisNet.isBus() && thisNet.isRootNet()) {
        signalMap.put(NetName + nets.getNetId(thisNet), 1);
      }
    }
    /* now we define the busses */
    for (final var thisNet : nets.getAllNets()) {
      if (thisNet.isBus() && thisNet.isRootNet()) {
        final var nrOfBits = thisNet.getBitWidth();
        signalMap.put(BusName + nets.getNetId(thisNet), nrOfBits);
      }
    }
    return signalMap;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
