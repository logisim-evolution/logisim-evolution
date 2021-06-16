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
import com.cburch.logisim.fpga.designrulecheck.ConnectionEnd;
import com.cburch.logisim.fpga.designrulecheck.ConnectionPoint;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Net;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
	      Set<String> HandledComponents,
	      String WorkingDir,
	      ArrayList<String> Hierarchy) {
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
       CircuitHDLGeneratorFactory Worker = (CircuitHDLGeneratorFactory)
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
            String line = "   "+
                          HDL.assignPreamble()+
                    OneLine +
                          HDL.assignOperator()+
                          BusName+
                          TheNets.GetNetId(Source.GetParentNet())+
                          HDL.BracketOpen()+
                          Source.GetParentNetBitIndex()+
                          HDL.BracketClose()+";";
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
            String line = "   "+HDL.assignPreamble()+ OneLine +";";
            if (!Contents.contains(line)) Contents.add(line);
          }
        }
      }
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetInOutList(Netlist MyNetList, AttributeSet attrs) {
    SortedMap<String, Integer> InOuts = new TreeMap<>();
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
        Inputs.put( CorrectLabel.getCorrectLabel( selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
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
    var contents = new ArrayList<String>();
    var isFirstLine = true;
    var temp = new StringBuilder();
    Map<String, Long> compIds = new HashMap<>();
    /* we start with the connection of the clock sources */
    for (NetlistComponent clockSource : theNetlist.GetClockSources()) {
      if (isFirstLine) {
        contents.add("");
        contents.addAll(MakeRemarkBlock("Here all clock generator connections are defined", 3));
        isFirstLine = false;
      }
      if (!clockSource.EndIsConnected(0)) {
        if (clockSource.GetComponent().getAttributeSet().getValue(StdAttr.LABEL).equals("sysclk")) {
          Reporter.Report.AddInfo(
              "Clock component found with no connection, skipping: '"
                  + clockSource.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
                  + "'");
        } else {
          Reporter.Report.AddWarning(
              "Clock component found with no connection, skipping: '"
                  + clockSource.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
                  + "'");
        }
        continue;
      }
      String ClockNet = GetClockNetName(clockSource, 0, theNetlist);
      if (ClockNet.isEmpty()) {
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
                + ClockNet
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
    ArrayList<String> Wiring = GetHDLWiring(theNetlist);
    if (!Wiring.isEmpty()) {
      contents.add("");
      contents.addAll(MakeRemarkBlock("Here all wiring is defined", 3));
      contents.addAll(Wiring);
    }
    /* Now we define all input signals; hence Input port -> Internal Net */
    isFirstLine = true;
    for (int i = 0; i < theNetlist.NumberOfInputPorts(); i++) {
      if (isFirstLine) {
        contents.add("");
        contents.addAll(MakeRemarkBlock("Here all input connections are defined", 3));
        isFirstLine = false;
      }
      NetlistComponent MyInput = theNetlist.GetInputPin(i);
        contents.add(
            GetSignalMap(
                CorrectLabel.getCorrectLabel(
                    MyInput.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
                MyInput,
                0,
                3,
                theNetlist));
    }
    /* Now we define all output signals; hence Internal Net -> Input port */
    isFirstLine = true;
    for (int i = 0; i < theNetlist.NumberOfOutputPorts(); i++) {
      if (isFirstLine) {
        contents.add("");
        contents.addAll(MakeRemarkBlock("Here all output connections are defined", 3));
        isFirstLine = false;
      }
      NetlistComponent MyOutput = theNetlist.GetOutputPin(i);
      contents.add(
          GetSignalMap(
              CorrectLabel.getCorrectLabel(
                  MyOutput.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              MyOutput,
              0,
              3,
              theNetlist));
    }
    /* Here all in-lined components are generated */
    isFirstLine = true;
    for (NetlistComponent comp : theNetlist.GetNormalComponents()) {
      var worker =
          comp.GetComponent().getFactory().getHDLGenerator(comp.GetComponent().getAttributeSet());
      if (worker != null) {
        if (worker.IsOnlyInlined()) {
          var inlinedName =
              comp.GetComponent().getFactory().getHDLName(comp.GetComponent().getAttributeSet());
          var InlinedId = worker.getComponentStringIdentifier();
          Long id = (compIds.containsKey(InlinedId)) ? compIds.get(InlinedId) : (long) 1;
          if (isFirstLine) {
            contents.add("");
            contents.addAll(MakeRemarkBlock("Here all in-lined components are defined", 3));
            isFirstLine = false;
          }
          contents.addAll(worker.GetInlinedCode(theNetlist, id++, comp, inlinedName));
          compIds.remove(InlinedId);
          compIds.put(InlinedId, id);
        }
      }
    }
    /* Here all "normal" components are generated */
    isFirstLine = true;
    for (NetlistComponent comp : theNetlist.GetNormalComponents()) {
      var Worker =
          comp.GetComponent().getFactory().getHDLGenerator(comp.GetComponent().getAttributeSet());
      if (Worker != null) {
        if (!Worker.IsOnlyInlined()) {
          var compName =
              comp.GetComponent().getFactory().getHDLName(comp.GetComponent().getAttributeSet());
          var compId = Worker.getComponentStringIdentifier();
          Long id;
          if (compIds.containsKey(compId)) {
            id = compIds.get(compId);
          } else {
            id = (long) 1;
          }
          if (isFirstLine) {
            contents.add("");
            contents.addAll(MakeRemarkBlock("Here all normal components are defined", 3));
            isFirstLine = false;
          }
          contents.addAll(Worker.GetComponentMap(theNetlist, id++, comp, null, compName));
          compIds.remove(compId);
          compIds.put(compId, id);
        }
      }
    }
    /* Finally we instantiate all sub-circuits */
    isFirstLine = true;
    for (NetlistComponent comp : theNetlist.GetSubCircuits()) {
      var worker =
          comp.GetComponent().getFactory().getHDLGenerator(comp.GetComponent().getAttributeSet());
      if (worker != null) {
        String compName =
            comp.GetComponent().getFactory().getHDLName(comp.GetComponent().getAttributeSet());
        if (comp.IsGatedInstance())  compName = compName.concat("_gated");
        String CompId = worker.getComponentStringIdentifier();
        Long id;
        if (compIds.containsKey(CompId)) {
          id = compIds.get(CompId);
        } else {
          id = (long) 1;
        }
        ArrayList<String> CompMap = worker.GetComponentMap(theNetlist, id++, comp, null, compName);
        if (!CompMap.isEmpty()) {
          if (isFirstLine) {
            contents.add("");
            contents.addAll(MakeRemarkBlock("Here all sub-circuits are defined", 3));
            isFirstLine = false;
          }
          compIds.remove(CompId);
          compIds.put(CompId, id);
          contents.addAll(CompMap);
        }
      }
    }
    contents.add("");
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist MyNetList, AttributeSet attrs) {
    SortedMap<String, Integer> outputs = new TreeMap<>();
    int OutputBubbles = MyNetList.NumberOfOutputBubbles();
    if (OutputBubbles > 0) {
      if (OutputBubbles > 1) {
        outputs.put(HDLGeneratorFactory.LocalOutputBubbleBusname, OutputBubbles);
      } else {
        outputs.put(HDLGeneratorFactory.LocalOutputBubbleBusname, 0);
      }
    }
    for (int i = 0; i < MyNetList.NumberOfOutputPorts(); i++) {
      NetlistComponent selected = MyNetList.GetOutputPin(i);
      if (selected != null) {
          outputs.put(
              CorrectLabel.getCorrectLabel(
                  selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              selected.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth());
      }
    }
    return outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (MapInfo == null) return null;
    boolean topLevel = MapInfo instanceof MappableResourcesContainer;
    var componentInfo = topLevel ? null : (NetlistComponent) MapInfo;
    var mapInfo = topLevel ? (MappableResourcesContainer) MapInfo : null;
    String Preamble = topLevel ? "s_" : "";
    var sub = topLevel ? null : (SubcircuitFactory) componentInfo.GetComponent().getFactory();
    var myNetList = topLevel ? nets : sub.getSubcircuit().getNetList();

      /* First we instantiate the Clock tree busses when present */
      for (int i = 0; i < myNetList.NumberOfClockTrees(); i++) {
        PortMap.put(ClockTreeName + i, Preamble + ClockTreeName + i);
      }
      if (myNetList.RequiresGlobalClockConnection()) {
        PortMap.put(
            TickComponentHDLGeneratorFactory.FPGAClock, TickComponentHDLGeneratorFactory.FPGAClock);
      }
      if (myNetList.NumberOfInputBubbles() > 0) {
        PortMap.put(
            HDLGeneratorFactory.LocalInputBubbleBusname, topLevel ? Preamble+HDLGeneratorFactory.LocalInputBubbleBusname :
            HDLGeneratorFactory.LocalInputBubbleBusname + GetBubbleIndex(componentInfo, 0));
      }
      if (myNetList.NumberOfOutputBubbles() > 0) {
        PortMap.put(
            HDLGeneratorFactory.LocalOutputBubbleBusname, topLevel ? Preamble+HDLGeneratorFactory.LocalOutputBubbleBusname :
            HDLGeneratorFactory.LocalOutputBubbleBusname + GetBubbleIndex(componentInfo, 1));
      }

      int nrOfIOBubbles = myNetList.NumberOfInOutBubbles();
      if (nrOfIOBubbles > 0) {
        if (topLevel) {
          StringBuilder vector = new StringBuilder();
          for (int i = nrOfIOBubbles-1 ; i >= 0 ; i--) {
            /* first pass find the component which is connected to this io */
            int compPin = -1;
            MapComponent map = null;
            for (ArrayList<String> key : mapInfo.getMappableResources().keySet()) {
              MapComponent comp = mapInfo.getMappableResources().get(key);
              if (comp.hasIOs()) {
                int id =  comp.getIOBublePinId(i);
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
              PortMap.put(HDLGeneratorFactory.LocalInOutBubbleBusname + "(" + i + ")", "OPEN");
            else {
                if (vector.length() != 0) vector.append(",");
                vector.append("OPEN"); // still not found the correct method but this seems to work
              }
            } else {
              if (HDL.isVHDL())
                PortMap.put(HDLGeneratorFactory.LocalInOutBubbleBusname+"("+i+")",
                  (map.isExternalInverted(compPin)?"n_":"")+map.getHdlString(compPin));
              else {
                if (vector.length() != 0) vector.append(",");
                vector.append(map.isExternalInverted(compPin) ? "n_" : "")
                    .append(map.getHdlString(compPin));
              }
            }
          }
          if (HDL.isVerilog())
            PortMap.put(HDLGeneratorFactory.LocalInOutBubbleBusname, vector.toString());
        } else {
          PortMap.put(HDLGeneratorFactory.LocalInOutBubbleBusname,
        		  HDLGeneratorFactory.LocalInOutBubbleBusname + GetBubbleIndex(componentInfo, 2));
        }
      }

      int nrOfInputPorts = myNetList.NumberOfInputPorts();
      if (nrOfInputPorts > 0) {
        for (int i = 0; i < nrOfInputPorts; i++) {
          NetlistComponent selected = myNetList.GetInputPin(i);
          if (selected != null) {
            String PinLabel =
                CorrectLabel.getCorrectLabel(
                    selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
            if (topLevel) {
              PortMap.put(PinLabel, Preamble+PinLabel);
            } else {
              int endid = nets.GetEndIndex(componentInfo, PinLabel, false);
              if (endid < 0) {
              Reporter.Report.AddFatalError(
                  "INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '"
                      + PinLabel
                      + "'");

              } else {
                PortMap.putAll(GetNetMap(PinLabel, true, componentInfo, endid, nets));
              }
            }
          }
        }
      }

      int nrOfInOutPorts = myNetList.NumberOfInOutPorts();
      if (nrOfInOutPorts > 0) {
        for (int i = 0; i < nrOfInOutPorts; i++) {
          NetlistComponent selected = myNetList.GetInOutPin(i);
          if (selected != null) {
            String PinLabel =
                CorrectLabel.getCorrectLabel(
                    selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
            if (topLevel) {
              /* Do not exist yet in logisim */
              /* TODO: implement by going over each bit */
            } else {
              int endid = nets.GetEndIndex(componentInfo, PinLabel, false);
              if (endid < 0) {
                Reporter.Report.AddFatalError(
                    "INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '"
                        + PinLabel
                        + "'");
              } else {
                PortMap.putAll(GetNetMap(PinLabel, true, componentInfo, endid, nets));
              }
            }
          }
        }
      }

      int nrOfOutputPorts = myNetList.NumberOfOutputPorts();
      if (nrOfOutputPorts > 0) {
        for (int i = 0; i < nrOfOutputPorts; i++) {
          NetlistComponent selected = myNetList.GetOutputPin(i);
          if (selected != null) {
            String PinLabel =
                CorrectLabel.getCorrectLabel(
                    selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
            if (topLevel) {
              PortMap.put(PinLabel, Preamble+PinLabel);
            } else {
              int endid = nets.GetEndIndex(componentInfo, PinLabel, true);
              if (endid < 0) {
                Reporter.Report.AddFatalError(
                    "INTERNAL ERROR! Could not find the end-index of a sub-circuit component: '"
                        + PinLabel
                        + "'");
              } else {
                PortMap.putAll(GetNetMap(PinLabel, true, componentInfo, endid, nets));
              }
            }
          }
        }
      }
    return PortMap;
  }

  private String GetSignalMap(
      String portName,
      NetlistComponent comp,
      int endIndex,
      int tabSize,
      Netlist TheNets) {
    var contents = new StringBuilder();
    var source = new StringBuilder();
    var destination = new StringBuilder();
    var tab = new StringBuilder();
    if ((endIndex < 0) || (endIndex >= comp.NrOfEnds())) {
      Reporter.Report.AddFatalError(
          "INTERNAL ERROR: Component tried to index non-existing SolderPoint: '"
              + comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
              + "'");
      return "";
    }
    tab.append(" ".repeat(tabSize));
    var connectionInformation = comp.getEnd(endIndex);
    boolean isOutput = connectionInformation.IsOutputEnd();
    int nrOfBits = connectionInformation.NrOfBits();
    if (nrOfBits == 1) {
      /* Here we have the easy case, just a single bit net */
      if (isOutput) {
        if (!comp.EndIsConnected(endIndex)) {
          return " ";
        }
        source.append(portName);
        destination.append(GetNetName(comp, endIndex, true, TheNets));
      } else {
        if (!comp.EndIsConnected(endIndex)) {
          Reporter.Report.AddSevereWarning(
              "Found an unconnected output pin, tied the pin to ground!");
        }
        source.append(GetNetName(comp, endIndex, true, TheNets));
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
      for (int i = 0; i < nrOfBits; i++) {
        if (connectionInformation.GetConnection((byte) i).GetParentNet() != null) {
          connected = true;
        }
      }
      if (!connected) {
        /* Here is the easy case, the bus is unconnected */
        if (isOutput) {
          return contents.toString();
        } else {
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
        if (TheNets.IsContinuesBus(comp, endIndex)) {
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
            ConnectionPoint SolderPoint = connectionInformation.GetConnection((byte) bit);
            if (SolderPoint.GetParentNet() == null) {
              /* The net is not connected */
              if (isOutput) {
                continue;
              } else {
                Reporter.Report.AddSevereWarning(
                    "Found an unconnected output bus pin, tied bit "
                        + bit
                        + " to ground!");
                source.append(HDL.GetZeroVector(1, true));
              }
            } else {
              /*
               * The net is connected, we have to find out if the
               * connection is to a bus or to a normal net
               */
              if (SolderPoint.GetParentNet().BitWidth() == 1) {
                /* The connection is to a Net */
                if (isOutput) {
                  destination.append(NetName).append(TheNets.GetNetId(SolderPoint.GetParentNet()));
                } else {
                  source.append(NetName).append(TheNets.GetNetId(SolderPoint.GetParentNet()));
                }
              } else {
                /* The connection is to an entry of a bus */
                if (isOutput) {
                  destination
                      .append(BusName)
                      .append(TheNets.GetNetId(SolderPoint.GetParentNet()))
                      .append(HDL.BracketOpen())
                      .append(SolderPoint.GetParentNetBitIndex())
                      .append(HDL.BracketClose());
                } else {
                  source
                      .append(BusName)
                      .append(TheNets.GetNetId(SolderPoint.GetParentNet()))
                      .append(HDL.BracketOpen())
                      .append(SolderPoint.GetParentNetBitIndex())
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
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> SignalMap = new TreeMap<>();

    /* First we define the nets */
    for (Net ThisNet : Nets.GetAllNets()) {
      if (!ThisNet.isBus() && ThisNet.IsRootNet()) {
        SignalMap.put(NetName + Nets.GetNetId(ThisNet), 1);
      }
    }
    /* now we define the busses */
    for (Net ThisNet : Nets.GetAllNets()) {
      if (ThisNet.isBus() && ThisNet.IsRootNet()) {
        int NrOfBits = ThisNet.BitWidth();
        SignalMap.put(BusName + Nets.GetNetId(ThisNet), NrOfBits);
      }
    }
    return SignalMap;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
