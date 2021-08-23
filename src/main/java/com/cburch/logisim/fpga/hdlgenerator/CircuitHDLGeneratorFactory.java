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
import com.cburch.logisim.circuit.CircuitAttributes;
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
import com.cburch.logisim.util.ContentBuilder;
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
    MyNetList.SetCurrentHierarchyLevel(Hierarchy);
    /* First we handle the normal components */
    for (NetlistComponent ThisComponent : MyNetList.GetNormalComponents()) {
      String ComponentName =
          ThisComponent.GetComponent()
              .getFactory()
              .getHDLName(ThisComponent.GetComponent().getAttributeSet());
      if (!HandledComponents.contains(ComponentName)) {
        HDLGeneratorFactory Worker =
            ThisComponent.GetComponent()
                .getFactory()
                .getHDLGenerator(ThisComponent.GetComponent().getAttributeSet());
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
                  ThisComponent.GetComponent().getAttributeSet(),
                  ComponentName),
              ComponentName)) {
            return false;
          }
          if (!WriteArchitecture(
              WorkPath + Worker.GetRelativeDirectory(),
              Worker.GetArchitecture(
                  MyNetList,
                  ThisComponent.GetComponent().getAttributeSet(),
                  ComponentName),
              ComponentName)) {
            return false;
          }
        }
        HandledComponents.add(ComponentName);
      }
    }
    /* Now we go down the hierarchy to get all other components */
    for (NetlistComponent ThisCircuit : MyNetList.GetSubCircuits()) {
      CircuitHDLGeneratorFactory Worker =
          (CircuitHDLGeneratorFactory)
              ThisCircuit.GetComponent()
                  .getFactory()
                  .getHDLGenerator(ThisCircuit.GetComponent().getAttributeSet());
      if (Worker == null) {
        Reporter.Report.AddFatalError(
            "INTERNAL ERROR: Unable to get a subcircuit VHDL generator for '"
                + ThisCircuit.GetComponent().getFactory().getName()
                + "'");
        return false;
      }
      Hierarchy.add(
          CorrectLabel.getCorrectLabel(
              ThisCircuit.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      if (!Worker.GenerateAllHDLDescriptions(
          HandledComponents, WorkingDir, Hierarchy, ThisCircuit.IsGatedInstance())) {
        return false;
      }
      Hierarchy.remove(Hierarchy.size() - 1);
    }
    /* I also have to generate myself */
    String ComponentName = CorrectLabel.getCorrectLabel(MyCircuit.getName());
    if (gatedInstance) ComponentName = ComponentName.concat("_gated");
    if (!HandledComponents.contains(ComponentName)) {
      if (!WriteEntity(
          WorkPath + GetRelativeDirectory(),
          GetEntity(MyNetList, null, ComponentName),
          ComponentName)) {
        return false;
      }

      // is the current circuit an 'empty vhdl box' ?
      String ArchName =
          MyCircuit.getStaticAttributes().getValue(CircuitAttributes.CIRCUIT_VHDL_PATH);

      if (!ArchName.isEmpty()) {
        if (!FileWriter.CopyArchitecture(
            ArchName, WorkPath + GetRelativeDirectory(), ComponentName)) {
          return false;
        }
      } else {
        if (!WriteArchitecture(
            WorkPath + GetRelativeDirectory(),
            GetArchitecture(MyNetList, null, ComponentName),
            ComponentName)) {
          return false;
        }
      }
      HandledComponents.add(ComponentName);
    }
    return true;
  }

  /* here the private handles are defined */
  private String GetBubbleIndex(NetlistComponent comp, int type) {
    switch (type) {
      case 0 : return HDL.BracketOpen()
                      + comp.GetLocalBubbleInputEndId()
                      + HDL.vectorLoopId()
                      + comp.GetLocalBubbleInputStartId()
                      + HDL.BracketClose();
      case 1 : return HDL.BracketOpen()
                      + comp.GetLocalBubbleOutputEndId()
                      + HDL.vectorLoopId()
                      + comp.GetLocalBubbleOutputStartId()
                      + HDL.BracketClose();
      case 2 : return HDL.BracketOpen()
                      + comp.GetLocalBubbleInOutEndId()
                      + HDL.vectorLoopId()
                      + comp.GetLocalBubbleInOutStartId()
                      + HDL.BracketClose();
    }
    return "";
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Components = new ArrayList<>();
    Set<String> InstantiatedComponents = new HashSet<>();
    for (NetlistComponent Gate : TheNetlist.GetNormalComponents()) {
      String CompName =
          Gate.GetComponent().getFactory().getHDLName(Gate.GetComponent().getAttributeSet());
      if (!InstantiatedComponents.contains(CompName)) {
        InstantiatedComponents.add(CompName);
        HDLGeneratorFactory Worker =
            Gate.GetComponent()
                .getFactory()
                .getHDLGenerator(Gate.GetComponent().getAttributeSet());
        if (Worker != null) {
          if (!Worker.IsOnlyInlined()) {
            Components.addAll(
                Worker.GetComponentInstantiation(
                    TheNetlist,
                    Gate.GetComponent().getAttributeSet(),
                    CompName));
          }
        }
      }
    }
    InstantiatedComponents.clear();
    for (NetlistComponent Gate : TheNetlist.GetSubCircuits()) {
      String CompName =
          Gate.GetComponent().getFactory().getHDLName(Gate.GetComponent().getAttributeSet());
      if (Gate.IsGatedInstance()) CompName = CompName.concat("_gated");
      if (!InstantiatedComponents.contains(CompName)) {
        InstantiatedComponents.add(CompName);
        HDLGeneratorFactory Worker =
            Gate.GetComponent()
                .getFactory()
                .getHDLGenerator(Gate.GetComponent().getAttributeSet());
        SubcircuitFactory sub = (SubcircuitFactory) Gate.GetComponent().getFactory();
        if (Worker != null) {
          Components.addAll(
              Worker.GetComponentInstantiation(
                  sub.getSubcircuit().getNetList(),
                  Gate.GetComponent().getAttributeSet(),
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
    ArrayList<String> Contents = new ArrayList<>();
    StringBuilder OneLine = new StringBuilder();
    /* we cycle through all nets with a forcedrootnet annotation */
    for (Net ThisNet : TheNets.GetAllNets()) {
      if (ThisNet.IsForcedRootNet()) {
        /* now we cycle through all the bits */
        for (int bit = 0; bit < ThisNet.BitWidth(); bit++) {
          /* First we perform all source connections */
          for (ConnectionPoint Source : ThisNet.GetSourceNets(bit)) {
            OneLine.setLength(0);
            if (ThisNet.isBus()) {
              OneLine.append(BusName)
                  .append(TheNets.GetNetId(ThisNet))
                  .append(HDL.BracketOpen())
                  .append(bit)
                  .append(HDL.BracketClose());
            } else {
              OneLine.append(NetName).append(TheNets.GetNetId(ThisNet));
            }
            while (OneLine.length() < SallignmentSize) {
              OneLine.append(" ");
            }
            String line =
                "   "
                    + HDL.assignPreamble()
                    + OneLine
                    + HDL.assignOperator()
                    + BusName
                    + TheNets.GetNetId(Source.GetParentNet())
                    + HDL.BracketOpen()
                    + Source.GetParentNetBitIndex()
                    + HDL.BracketClose()
                    + ";";
            if (!Contents.contains(line)) Contents.add(line);
          }
          /* Next we perform all sink connections */
          for (ConnectionPoint Source : ThisNet.GetSinkNets(bit)) {
            OneLine.setLength(0);
            OneLine.append(BusName)
                .append(TheNets.GetNetId(Source.GetParentNet()))
                .append(HDL.BracketOpen())
                .append(Source.GetParentNetBitIndex())
                .append(HDL.BracketClose());
            while (OneLine.length() < SallignmentSize) {
              OneLine.append(" ");
            }
            OneLine.append(HDL.assignOperator());
            if (ThisNet.isBus()) {
              OneLine.append(BusName)
                  .append(TheNets.GetNetId(ThisNet))
                  .append(HDL.BracketOpen())
                  .append(bit)
                  .append(HDL.BracketClose());
            } else {
              OneLine.append(NetName).append(TheNets.GetNetId(ThisNet));
            }
            String line = "   " + HDL.assignPreamble() + OneLine + ";";
            if (!Contents.contains(line)) Contents.add(line);
          }
        }
      }
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetInOutList(Netlist MyNetList, AttributeSet attrs) {
    final var InOuts = new TreeMap<String, Integer>();
    int InOutBubbles = MyNetList.NumberOfInOutBubbles();
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
    for (int i = 0; i < MyNetList.NumberOfClockTrees(); i++) {
      Inputs.put(ClockTreeName + i, ClockHDLGeneratorFactory.NrOfClockBits);
    }
    if (MyNetList.RequiresGlobalClockConnection()) {
      Inputs.put(TickComponentHDLGeneratorFactory.FPGAClock, 1);
    }
    int InputBubbles = MyNetList.NumberOfInputBubbles();
    if (InputBubbles > 0) {
      if (InputBubbles > 1) {
        Inputs.put(HDLGeneratorFactory.LocalInputBubbleBusname, InputBubbles);
      } else {
        Inputs.put(HDLGeneratorFactory.LocalInputBubbleBusname, 0);
      }
    }
    for (int i = 0; i < MyNetList.NumberOfInputPorts(); i++) {
      NetlistComponent selected = MyNetList.GetInputPin(i);
      if (selected != null) {
        Inputs.put(
            CorrectLabel.getCorrectLabel(
                selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
            selected.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth());
      }
    }
    return Inputs;
  }

  @Override
  public String GetInstanceIdentifier(NetlistComponent componentInfo, Long componentId) {
    if (componentInfo != null) {
      String compId =
          CorrectLabel.getCorrectLabel(
              componentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (!compId.isEmpty()) {
        return compId;
      }
    }
    return getComponentStringIdentifier() + "_" + componentId.toString();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
//    var contents = new ArrayList<String>();
    var contents = new ContentBuilder();
    var isFirstLine = true;
    var temp = new StringBuilder();
    final var compIds = new HashMap<String, Long>();
    /* we start with the connection of the clock sources */
    for (final var clockSource : theNetlist.GetClockSources()) {
      if (isFirstLine) {
        contents.add("");
        contents.addRemarkBlock("Here all clock generator connections are defined");
        isFirstLine = false;
      }
      if (!clockSource.EndIsConnected(0)) {
        // FIXME: hardcoded string
        final var msg = String.format("Clock component found with no connection, skipping: '%s'",
                clockSource.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
        if (clockSource.GetComponent().getAttributeSet().getValue(StdAttr.LABEL).equals("sysclk")) {
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
      while (temp.length() < SallignmentSize) {
        temp.append(" ");
      }
      if (!theNetlist.RequiresGlobalClockConnection()) {
        contents.add(
            "   "
                + HDL.assignPreamble()
                + temp
                + HDL.assignOperator()
                + clockNet
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.DerivedClockIndex
                + HDL.BracketClose()
                + ";");
      } else {
        contents.add(
            "   "
                + HDL.assignPreamble()
                + temp
                + HDL.assignOperator()
                + TickComponentHDLGeneratorFactory.FPGAClock
                + ";");
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
    for (var i = 0; i < theNetlist.NumberOfInputPorts(); i++) {
      if (isFirstLine) {
        contents.add("");
        contents.add(MakeRemarkBlock("Here all input connections are defined", 3));
        isFirstLine = false;
      }
      final var myInput = theNetlist.GetInputPin(i);
      contents.add(
          getSignalMap(
              CorrectLabel.getCorrectLabel(
                  myInput.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              myInput,
              0,
              3,
              theNetlist));
    }
    /* Now we define all output signals; hence Internal Net -> Input port */
    isFirstLine = true;
    for (var i = 0; i < theNetlist.NumberOfOutputPorts(); i++) {
      if (isFirstLine) {
        contents.add("");
        contents.addRemarkBlock("Here all output connections are defined");
        isFirstLine = false;
      }
      NetlistComponent MyOutput = theNetlist.GetOutputPin(i);
      contents.add(
          getSignalMap(
              CorrectLabel.getCorrectLabel(
                  MyOutput.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              MyOutput,
              0,
              3,
              theNetlist));
    }
    /* Here all in-lined components are generated */
    isFirstLine = true;
    for (final var comp : theNetlist.GetNormalComponents()) {
      var worker = comp.GetComponent().getFactory().getHDLGenerator(comp.GetComponent().getAttributeSet());
      if (worker != null) {
        if (worker.IsOnlyInlined()) {
          var inlinedName = comp.GetComponent().getFactory().getHDLName(comp.GetComponent().getAttributeSet());
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
    for (final var comp : theNetlist.GetNormalComponents()) {
      var worker = comp.GetComponent().getFactory().getHDLGenerator(comp.GetComponent().getAttributeSet());
      if (worker != null) {
        if (!worker.IsOnlyInlined()) {
          var compName = comp.GetComponent().getFactory().getHDLName(comp.GetComponent().getAttributeSet());
          var compId = worker.getComponentStringIdentifier();
          var id = (compIds.containsKey(compId)) ? compIds.get(compId) : (long) 1;
          if (isFirstLine) {
            contents.add("");
            contents.add(MakeRemarkBlock("Here all normal components are defined", 3));
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
    for (final var comp : theNetlist.GetSubCircuits()) {
      final var worker = comp.GetComponent().getFactory().getHDLGenerator(comp.GetComponent().getAttributeSet());
      if (worker != null) {
        var compName = comp.GetComponent().getFactory().getHDLName(comp.GetComponent().getAttributeSet());
        if (comp.IsGatedInstance())  compName = compName.concat("_gated");
        var compId = worker.getComponentStringIdentifier();
        var id = (compIds.containsKey(compId)) ? compIds.get(compId) : (long) 1;
        final var compMap = worker.GetComponentMap(theNetlist, id++, comp, null, compName);
        if (!compMap.isEmpty()) {
          if (isFirstLine) {
            contents.add("");
            contents.addRemarkBlock("Here all sub-circuits are defined");
            isFirstLine = false;
          }
          compIds.remove(compId);
          compIds.put(compId, id);
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
    final var outputBubbles = myNetList.NumberOfOutputBubbles();
    if (outputBubbles > 0) {
      outputs.put(HDLGeneratorFactory.LocalOutputBubbleBusname, (outputBubbles == 1) ? 0 : outputBubbles);
    }

    for (var i = 0; i < myNetList.NumberOfOutputPorts(); i++) {
      final var selected = myNetList.GetOutputPin(i);
      if (selected != null) {
        outputs.put(
            CorrectLabel.getCorrectLabel(selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
            selected.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth());
      }
    }
    return outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object MapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (MapInfo == null) return null;
    final var topLevel = MapInfo instanceof MappableResourcesContainer;
    final var componentInfo = topLevel ? null : (NetlistComponent) MapInfo;
    var mapInfo = topLevel ? (MappableResourcesContainer) MapInfo : null;
    final var preamble = topLevel ? "s_" : "";
    final var sub = topLevel ? null : (SubcircuitFactory) componentInfo.GetComponent().getFactory();
    final var myNetList = topLevel ? nets : sub.getSubcircuit().getNetList();

    /* First we instantiate the Clock tree busses when present */
    for (var i = 0; i < myNetList.NumberOfClockTrees(); i++) {
      portMap.put(ClockTreeName + i, preamble + ClockTreeName + i);
    }
    if (myNetList.RequiresGlobalClockConnection()) {
      portMap.put(TickComponentHDLGeneratorFactory.FPGAClock, TickComponentHDLGeneratorFactory.FPGAClock);
    }
    if (myNetList.NumberOfInputBubbles() > 0) {
      portMap.put(
          HDLGeneratorFactory.LocalInputBubbleBusname,
          topLevel
              ? preamble + HDLGeneratorFactory.LocalInputBubbleBusname
              : HDLGeneratorFactory.LocalInputBubbleBusname + GetBubbleIndex(componentInfo, 0));
    }
    if (myNetList.NumberOfOutputBubbles() > 0) {
      portMap.put(
          HDLGeneratorFactory.LocalOutputBubbleBusname,
          topLevel
              ? preamble + HDLGeneratorFactory.LocalOutputBubbleBusname
              : HDLGeneratorFactory.LocalOutputBubbleBusname + GetBubbleIndex(componentInfo, 1));
    }

    final var nrOfIOBubbles = myNetList.NumberOfInOutBubbles();
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
              final var id = comp.getIOBublePinId(i);
              if (id >= 0) {
                compPin = id;
                map = comp;
                break;
              }
            }
          }
          if (map == null || compPin < 0) {
            Reporter.Report.AddError("BUG: did not find IOpin");
            continue;
          }
          if (!map.isMapped(compPin) || map.IsOpenMapped(compPin)) {
            if (HDL.isVHDL())
              portMap.put(HDLGeneratorFactory.LocalInOutBubbleBusname + "(" + i + ")", "OPEN");
            else {
              if (vector.length() != 0) vector.append(",");
              vector.append("OPEN"); // still not found the correct method but this seems to work
            }
          } else {
            if (HDL.isVHDL())
              portMap.put(
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
          portMap.put(HDLGeneratorFactory.LocalInOutBubbleBusname, vector.toString());
      } else {
        portMap.put(
            HDLGeneratorFactory.LocalInOutBubbleBusname,
            HDLGeneratorFactory.LocalInOutBubbleBusname + GetBubbleIndex(componentInfo, 2));
      }
    }

    final var nrOfInputPorts = myNetList.NumberOfInputPorts();
    if (nrOfInputPorts > 0) {
      for (var i = 0; i < nrOfInputPorts; i++) {
        NetlistComponent selected = myNetList.GetInputPin(i);
        if (selected != null) {
          final var pinLabel = CorrectLabel.getCorrectLabel(selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
          if (topLevel) {
            portMap.put(pinLabel, preamble + pinLabel);
          } else {
            final var endId = nets.GetEndIndex(componentInfo, pinLabel, false);
            if (endId < 0) {
              // FIXME: hardcoded string
              Reporter.Report.AddFatalError(
                  String.format("INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '%s'", pinLabel));
            } else {
              portMap.putAll(GetNetMap(pinLabel, true, componentInfo, endId, nets));
            }
          }
        }
      }
    }

    final var nrOfInOutPorts = myNetList.NumberOfInOutPorts();
    if (nrOfInOutPorts > 0) {
      for (var i = 0; i < nrOfInOutPorts; i++) {
        final var selected = myNetList.GetInOutPin(i);
        if (selected != null) {
          final var pinLabel = CorrectLabel.getCorrectLabel(selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
          if (topLevel) {
            /* Do not exist yet in logisim */
            /* TODO: implement by going over each bit */
          } else {
            final var endId = nets.GetEndIndex(componentInfo, pinLabel, false);
            if (endId < 0) {
              // FIXME: hardcoded string
              Reporter.Report.AddFatalError(
                      String.format("INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '%s'", pinLabel));
            } else {
              portMap.putAll(GetNetMap(pinLabel, true, componentInfo, endId, nets));
            }
          }
        }
      }
    }

    final var nrOfOutputPorts = myNetList.NumberOfOutputPorts();
    if (nrOfOutputPorts > 0) {
      for (var i = 0; i < nrOfOutputPorts; i++) {
        final var selected = myNetList.GetOutputPin(i);
        if (selected != null) {
          final var pinLabel = CorrectLabel.getCorrectLabel(selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
          if (topLevel) {
            portMap.put(pinLabel, preamble + pinLabel);
          } else {
            int endid = nets.GetEndIndex(componentInfo, pinLabel, true);
            if (endid < 0) {
              // FIXME: hardcoded string
              Reporter.Report.AddFatalError(
                      String.format("INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '%s'", pinLabel));
            } else {
              portMap.putAll(GetNetMap(pinLabel, true, componentInfo, endid, nets));
            }
          }
        }
      }
    }
    return portMap;
  }

  private String getSignalMap(String portName, NetlistComponent comp, int endIndex, int tabSize, Netlist theNets) {
    final var contents = new StringBuilder();
    final var source = new StringBuilder();
    final var destination = new StringBuilder();
    final var tab = new StringBuilder();
    if ((endIndex < 0) || (endIndex >= comp.NrOfEnds())) {
      // FIXME: hardcoded string
      Reporter.Report.AddFatalError(
          String.format(
              "INTERNAL ERROR: Component tried to index non-existing SolderPoint: '%s'",
              comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      return "";
    }
    tab.append(" ".repeat(tabSize));
    final var connectionInformation = comp.getEnd(endIndex);
    final var isOutput = connectionInformation.IsOutputEnd();
    final var nrOfBits = connectionInformation.NrOfBits();
    if (nrOfBits == 1) {
      /* Here we have the easy case, just a single bit net */
      if (isOutput) {
        if (!comp.EndIsConnected(endIndex)) {
          return " ";
        }
        source.append(portName);
        destination.append(GetNetName(comp, endIndex, true, theNets));
      } else {
        if (!comp.EndIsConnected(endIndex)) {
          // FIXME: hardcoded string
          Reporter.Report.AddSevereWarning(
              "Found an unconnected output pin, tied the pin to ground!");
        }
        source.append(GetNetName(comp, endIndex, true, theNets));
        destination.append(portName);
        if (!comp.EndIsConnected(endIndex)) {
          return contents.toString();
        }
      }
      while (destination.length() < SallignmentSize) {
        destination.append(" ");
      }
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
        if (connectionInformation.GetConnection((byte) i).GetParentNet() != null) {
          connected = true;
        }
      }
      if (!connected) {
        /* Here is the easy case, the bus is unconnected */
        if (isOutput) {
          return contents.toString();
        } else {
          // FIXME: hardcoded string
          Reporter.Report.AddSevereWarning(
              "Found an unconnected output bus pin, tied all the pin bits to ground!");
        }
        destination.append(portName);
        while (destination.length() < SallignmentSize) {
          destination.append(" ");
        }
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
        if (theNets.IsContinuesBus(comp, endIndex)) {
          destination.setLength(0);
          source.setLength(0);
          /* Another easy case, the continues bus connection */
          if (isOutput) {
            source.append(portName);
            destination.append(GetBusNameContinues(comp, endIndex, theNets));
          } else {
            destination.append(portName);
            source.append(GetBusNameContinues(comp, endIndex, theNets));
          }
          while (destination.length() < SallignmentSize) {
            destination.append(" ");
          }
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
            final var solderPoint = connectionInformation.GetConnection((byte) bit);
            if (solderPoint.GetParentNet() == null) {
              /* The net is not connected */
              if (isOutput) {
                continue;
              } else {
                // FIXME: hardcoded string
                Reporter.Report.AddSevereWarning(
                        String.format("Found an unconnected output bus pin, tied bit %d to ground!", bit));
                source.append(HDL.GetZeroVector(1, true));
              }
            } else {
              /*
               * The net is connected, we have to find out if the
               * connection is to a bus or to a normal net
               */
              if (solderPoint.GetParentNet().BitWidth() == 1) {
                /* The connection is to a Net */
                if (isOutput) {
                  destination.append(NetName).append(theNets.GetNetId(solderPoint.GetParentNet()));
                } else {
                  source.append(NetName).append(theNets.GetNetId(solderPoint.GetParentNet()));
                }
              } else {
                /* The connection is to an entry of a bus */
                if (isOutput) {
                  destination
                      .append(BusName)
                      .append(theNets.GetNetId(solderPoint.GetParentNet()))
                      .append(HDL.BracketOpen())
                      .append(solderPoint.GetParentNetBitIndex())
                      .append(HDL.BracketClose());
                } else {
                  source
                      .append(BusName)
                      .append(theNets.GetNetId(solderPoint.GetParentNet()))
                      .append(HDL.BracketOpen())
                      .append(solderPoint.GetParentNetBitIndex())
                      .append(HDL.BracketClose());
                }
              }
            }
            while (destination.length() < SallignmentSize) {
              destination.append(" ");
            }
            if (bit != 0) {
              contents.append("\n");
            }
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
    for (final var thisNet : nets.GetAllNets()) {
      if (!thisNet.isBus() && thisNet.IsRootNet()) {
        signalMap.put(NetName + nets.GetNetId(thisNet), 1);
      }
    }
    /* now we define the busses */
    for (final var thisNet : nets.GetAllNets()) {
      if (thisNet.isBus() && thisNet.IsRootNet()) {
        final var nrOfBits = thisNet.BitWidth();
        signalMap.put(BusName + nets.GetNetId(thisNet), nrOfBits);
      }
    }
    return signalMap;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
