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
              OneLine.append(
                  BusName
                      + TheNets.GetNetId(ThisNet)
                      + HDL.BracketOpen()
                      + bit
                      + HDL.BracketClose());
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
            OneLine.append(
                BusName
                    + TheNets.GetNetId(Source.GetParentNet())
                    + HDL.BracketOpen()
                    + Source.GetParentNetBitIndex()
                    + HDL.BracketClose());
            while (OneLine.length() < SallignmentSize) {
              OneLine.append(" ");
            }
            OneLine.append(HDL.assignOperator());
            if (ThisNet.isBus()) {
              OneLine.append(
                  BusName
                      + TheNets.GetNetId(ThisNet)
                      + HDL.BracketOpen()
                      + bit
                      + HDL.BracketClose());
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
  public String GetInstanceIdentifier(NetlistComponent ComponentInfo, Long ComponentId) {
    if (ComponentInfo != null) {
      String CompId =
          CorrectLabel.getCorrectLabel(
              ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (!CompId.isEmpty()) {
        return CompId;
      }
    }
    return getComponentStringIdentifier() + "_" + ComponentId.toString();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    boolean FirstLine = true;
    StringBuilder Temp = new StringBuilder();
    Map<String, Long> CompIds = new HashMap<>();
    /* we start with the connection of the clock sources */
    for (NetlistComponent ClockSource : TheNetlist.GetClockSources()) {
      if (FirstLine) {
        Contents.add("");
        Contents.addAll(MakeRemarkBlock("Here all clock generator connections are defined", 3));
        FirstLine = false;
      }
      if (!ClockSource.EndIsConnected(0)) {
        if (ClockSource.GetComponent().getAttributeSet().getValue(StdAttr.LABEL).equals("sysclk")) {
          Reporter.Report.AddInfo(
              "Clock component found with no connection, skipping: '"
                  + ClockSource.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
                  + "'");
        } else {
          Reporter.Report.AddWarning(
              "Clock component found with no connection, skipping: '"
                  + ClockSource.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
                  + "'");
        }
        continue;
      }
      String ClockNet = GetClockNetName(ClockSource, 0, TheNetlist);
      if (ClockNet.isEmpty()) {
        Reporter.Report.AddFatalError("INTERNAL ERROR: Cannot find clocknet!");
      }
      String ConnectedNet = GetNetName(ClockSource, 0, true, TheNetlist);
      Temp.setLength(0);
      Temp.append(ConnectedNet);
      while (Temp.length() < SallignmentSize) {
        Temp.append(" ");
      }
      if (!TheNetlist.RequiresGlobalClockConnection()) {
        Contents.add(
            "   "
                + HDL.assignPreamble()
                + Temp
                + HDL.assignOperator()
                + ClockNet
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.DerivedClockIndex
                + HDL.BracketClose()
                + ";");
      } else {
        Contents.add(
            "   "
                + HDL.assignPreamble()
                + Temp
                + HDL.assignOperator()
                + TickComponentHDLGeneratorFactory.FPGAClock
                + ";");
      }
    }
    /* Here we define all wiring; hence all complex splitter connections */
    ArrayList<String> Wiring = GetHDLWiring(TheNetlist);
    if (!Wiring.isEmpty()) {
      Contents.add("");
      Contents.addAll(MakeRemarkBlock("Here all wiring is defined", 3));
      Contents.addAll(Wiring);
    }
    /* Now we define all input signals; hence Input port -> Internal Net */
    FirstLine = true;
    for (int i = 0; i < TheNetlist.NumberOfInputPorts(); i++) {
      if (FirstLine) {
        Contents.add("");
        Contents.addAll(MakeRemarkBlock("Here all input connections are defined", 3));
        FirstLine = false;
      }
      NetlistComponent MyInput = TheNetlist.GetInputPin(i);
        Contents.add(
            GetSignalMap(
                CorrectLabel.getCorrectLabel(
                    MyInput.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
                MyInput,
                0,
                3,
                TheNetlist));
    }
    /* Now we define all output signals; hence Internal Net -> Input port */
    FirstLine = true;
    for (int i = 0; i < TheNetlist.NumberOfOutputPorts(); i++) {
      if (FirstLine) {
        Contents.add("");
        Contents.addAll(MakeRemarkBlock("Here all output connections are defined", 3));
        FirstLine = false;
      }
      NetlistComponent MyOutput = TheNetlist.GetOutputPin(i);
      Contents.add(
          GetSignalMap(
              CorrectLabel.getCorrectLabel(
                  MyOutput.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              MyOutput,
              0,
              3,
              TheNetlist));
    }
    /* Here all in-lined components are generated */
    FirstLine = true;
    for (NetlistComponent comp : TheNetlist.GetNormalComponents()) {
      HDLGeneratorFactory Worker =
          comp.GetComponent()
              .getFactory()
              .getHDLGenerator(comp.GetComponent().getAttributeSet());
      if (Worker != null) {
        if (Worker.IsOnlyInlined()) {
          String InlinedName =
              comp.GetComponent().getFactory().getHDLName(comp.GetComponent().getAttributeSet());
          String InlinedId = Worker.getComponentStringIdentifier();
          Long id;
          if (CompIds.containsKey(InlinedId)) {
            id = CompIds.get(InlinedId);
          } else {
            id = (long) 1;
          }
          if (FirstLine) {
            Contents.add("");
            Contents.addAll(
                MakeRemarkBlock("Here all in-lined components are defined", 3));
            FirstLine = false;
          }
          Contents.addAll(
              Worker.GetInlinedCode(TheNetlist, id++, comp, InlinedName));
          CompIds.remove(InlinedId);
          CompIds.put(InlinedId, id);
        }
      }
    }
    /* Here all "normal" components are generated */
    FirstLine = true;
    for (NetlistComponent comp : TheNetlist.GetNormalComponents()) {
      HDLGeneratorFactory Worker =
          comp.GetComponent()
              .getFactory()
              .getHDLGenerator(comp.GetComponent().getAttributeSet());
      if (Worker != null) {
        if (!Worker.IsOnlyInlined()) {
          String CompName = comp.GetComponent().getFactory().getHDLName(comp.GetComponent().getAttributeSet());
          String CompId = Worker.getComponentStringIdentifier();
          Long id;
          if (CompIds.containsKey(CompId)) {
            id = CompIds.get(CompId);
          } else {
            id = (long) 1;
          }
          if (FirstLine) {
            Contents.add("");
            Contents.addAll(MakeRemarkBlock("Here all normal components are defined", 3));
            FirstLine = false;
          }
          Contents.addAll(Worker.GetComponentMap(TheNetlist, id++, comp, null, CompName));
          CompIds.remove(CompId);
          CompIds.put(CompId, id);
        }
      }
    }
    /* Finally we instantiate all sub-circuits */
    FirstLine = true;
    for (NetlistComponent comp : TheNetlist.GetSubCircuits()) {
      HDLGeneratorFactory Worker =
          comp.GetComponent().getFactory().getHDLGenerator(comp.GetComponent().getAttributeSet());
      if (Worker != null) {
        String CompName =
            comp.GetComponent().getFactory().getHDLName(comp.GetComponent().getAttributeSet());
        if (comp.IsGatedInstance())  CompName = CompName.concat("_gated");
        String CompId = Worker.getComponentStringIdentifier();
        Long id;
        if (CompIds.containsKey(CompId)) {
          id = CompIds.get(CompId);
        } else {
          id = (long) 1;
        }
        ArrayList<String> CompMap = Worker.GetComponentMap(TheNetlist, id++, comp, null, CompName);
        if (!CompMap.isEmpty()) {
          if (FirstLine) {
            Contents.add("");
            Contents.addAll(MakeRemarkBlock("Here all sub-circuits are defined", 3));
            FirstLine = false;
          }
          CompIds.remove(CompId);
          CompIds.put(CompId, id);
          Contents.addAll(CompMap);
        }
      }
    }
    Contents.add("");
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist MyNetList, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    int OutputBubbles = MyNetList.NumberOfOutputBubbles();
    if (OutputBubbles > 0) {
      if (OutputBubbles > 1) {
        Outputs.put(HDLGeneratorFactory.LocalOutputBubbleBusname, OutputBubbles);
      } else {
        Outputs.put(HDLGeneratorFactory.LocalOutputBubbleBusname, 0);
      }
    }
    for (int i = 0; i < MyNetList.NumberOfOutputPorts(); i++) {
      NetlistComponent selected = MyNetList.GetOutputPin(i);
      if (selected != null) {
          Outputs.put(
              CorrectLabel.getCorrectLabel(
                  selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)),
              selected.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth());
      }
    }
    return Outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (MapInfo == null) return null;
    boolean topLevel = MapInfo instanceof MappableResourcesContainer;
    NetlistComponent ComponentInfo = topLevel ? null : (NetlistComponent) MapInfo;
    MappableResourcesContainer mapInfo = topLevel ? (MappableResourcesContainer) MapInfo : null;
    String Preamble = topLevel ? "s_" : "";
      SubcircuitFactory sub = topLevel ? null : (SubcircuitFactory) ComponentInfo.GetComponent().getFactory();
      Netlist MyNetList = topLevel ? Nets : sub.getSubcircuit().getNetList();
      int NrOfClockTrees = MyNetList.NumberOfClockTrees();
      int NrOfInputBubbles = MyNetList.NumberOfInputBubbles();
      int NrOfOutputBubbles = MyNetList.NumberOfOutputBubbles();
      int NrOfIOBubbles = MyNetList.NumberOfInOutBubbles();
      int NrOfInputPorts = MyNetList.NumberOfInputPorts();
      int NrOfInOutPorts = MyNetList.NumberOfInOutPorts();
      int NrOfOutputPorts = MyNetList.NumberOfOutputPorts();
      /* First we instantiate the Clock tree busses when present */
      for (int i = 0; i < NrOfClockTrees; i++) {
        PortMap.put(ClockTreeName + i, Preamble + ClockTreeName + i);
      }
      if (MyNetList.RequiresGlobalClockConnection()) {
        PortMap.put(
            TickComponentHDLGeneratorFactory.FPGAClock, TickComponentHDLGeneratorFactory.FPGAClock);
      }
      if (NrOfInputBubbles > 0) {
        PortMap.put(
            HDLGeneratorFactory.LocalInputBubbleBusname, topLevel ? Preamble+HDLGeneratorFactory.LocalInputBubbleBusname :
            HDLGeneratorFactory.LocalInputBubbleBusname + GetBubbleIndex(ComponentInfo, 0));
      }
      if (NrOfOutputBubbles > 0) {
        PortMap.put(
            HDLGeneratorFactory.LocalOutputBubbleBusname, topLevel ? Preamble+HDLGeneratorFactory.LocalOutputBubbleBusname :
            HDLGeneratorFactory.LocalOutputBubbleBusname + GetBubbleIndex(ComponentInfo, 1));
      }
      if (NrOfIOBubbles > 0) {
        if (topLevel) {
          StringBuilder vector = new StringBuilder();
          for (int i = NrOfIOBubbles-1 ; i >= 0 ; i--) {
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
                PortMap.put(HDLGeneratorFactory.LocalInOutBubbleBusname+"("+i+")","OPEN");
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
        		  HDLGeneratorFactory.LocalInOutBubbleBusname + GetBubbleIndex(ComponentInfo, 2));
        }
      }
      if (NrOfInputPorts > 0) {
        for (int i = 0; i < NrOfInputPorts; i++) {
          NetlistComponent selected = MyNetList.GetInputPin(i);
          if (selected != null) {
            String PinLabel =
                CorrectLabel.getCorrectLabel(
                    selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
            if (topLevel) {
              PortMap.put(PinLabel, Preamble+PinLabel);
            } else {
              int endid = Nets.GetEndIndex(ComponentInfo, PinLabel, false);
              if (endid < 0) {
                Reporter.Report.AddFatalError("INTERNAL ERROR! Could not find the end-index of a sub-circuit component : '"
                      + PinLabel
                      + "'");

              } else {
                PortMap.putAll(GetNetMap(PinLabel, true, ComponentInfo, endid, Nets));
              }
            }
          }
        }
      }
      if (NrOfInOutPorts > 0) {
        for (int i = 0; i < NrOfInOutPorts; i++) {
          NetlistComponent selected = MyNetList.GetInOutPin(i);
          if (selected != null) {
            String PinLabel =
                CorrectLabel.getCorrectLabel(
                    selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
            if (topLevel) {
              /* Do not exist yet in logisim */
              /* TODO: implement by going over each bit */
            } else {
              int endid = Nets.GetEndIndex(ComponentInfo, PinLabel, false);
              if (endid < 0) {
                Reporter.Report.AddFatalError(
                    "INTERNAL ERROR! Could not find the end-index of a sub-circuit component : '"
                        + PinLabel
                        + "'");
              } else {
                PortMap.putAll(GetNetMap(PinLabel, true, ComponentInfo, endid, Nets));
              }
            }
          }
        }
      }
      if (NrOfOutputPorts > 0) {
        for (int i = 0; i < NrOfOutputPorts; i++) {
          NetlistComponent selected = MyNetList.GetOutputPin(i);
          if (selected != null) {
            String PinLabel =
                CorrectLabel.getCorrectLabel(
                    selected.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
            if (topLevel) {
              PortMap.put(PinLabel, Preamble+PinLabel);
            } else {
              int endid = Nets.GetEndIndex(ComponentInfo, PinLabel, true);
              if (endid < 0) {
                Reporter.Report.AddFatalError(
                    "INTERNAL ERROR! Could not find the end-index of a sub-circuit component : '"
                        + PinLabel
                        + "'");
              } else {
                PortMap.putAll(GetNetMap(PinLabel, true, ComponentInfo, endid, Nets));
              }
            }
          }
        }
      }
    return PortMap;
  }

  private String GetSignalMap(
      String PortName,
      NetlistComponent comp,
      int EndIndex,
      int TabSize,
      Netlist TheNets) {
    StringBuilder Contents = new StringBuilder();
    StringBuilder Source = new StringBuilder();
    StringBuilder Destination = new StringBuilder();
    StringBuilder Tab = new StringBuilder();
    if ((EndIndex < 0) || (EndIndex >= comp.NrOfEnds())) {
      Reporter.Report.AddFatalError(
          "INTERNAL ERROR: Component tried to index non-existing SolderPoint: '"
              + comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
              + "'");
      return "";
    }
    Tab.append(" ".repeat(TabSize));
    ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
    boolean IsOutput = ConnectionInformation.IsOutputEnd();
    int NrOfBits = ConnectionInformation.NrOfBits();
    if (NrOfBits == 1) {
      /* Here we have the easy case, just a single bit net */
      if (IsOutput) {
        if (!comp.EndIsConnected(EndIndex)) {
          return " ";
        }
        Source.append(PortName);
        Destination.append(GetNetName(comp, EndIndex, true, TheNets));
      } else {
        if (!comp.EndIsConnected(EndIndex)) {
          Reporter.Report.AddSevereWarning("Found an unconnected output pin, tied the pin to ground!");
        }
        Source.append(GetNetName(comp, EndIndex, true, TheNets));
        Destination.append(PortName);
        if (!comp.EndIsConnected(EndIndex)) {
          return Contents.toString();
        }
      }
      while (Destination.length() < SallignmentSize) {
        Destination.append(" ");
      }
      Contents.append(Tab + HDL.assignPreamble() + Destination + HDL.assignOperator() + Source + ";");
    } else {
      /*
       * Here we have the more difficult case, it is a bus that needs to
       * be mapped
       */
      /* First we check if the bus has a connection */
      boolean Connected = false;
      for (int i = 0; i < NrOfBits; i++) {
        if (ConnectionInformation.GetConnection((byte) i).GetParentNet() != null) {
          Connected = true;
        }
      }
      if (!Connected) {
        /* Here is the easy case, the bus is unconnected */
        if (IsOutput) {
          return Contents.toString();
        } else {
          Reporter.Report.AddSevereWarning(
              "Found an unconnected output bus pin, tied all the pin bits to ground!");
        }
        Destination.append(PortName);
        while (Destination.length() < SallignmentSize) {
          Destination.append(" ");
        }
        Contents.append(
            Tab
                + HDL.assignPreamble()
                + Destination
                + HDL.assignOperator()
                + HDL.GetZeroVector(NrOfBits, true)
                + ";");
      } else {
        /*
         * There are connections, we detect if it is a continues bus
         * connection
         */
        if (TheNets.IsContinuesBus(comp, EndIndex)) {
          Destination.setLength(0);
          Source.setLength(0);
          /* Another easy case, the continues bus connection */
          if (IsOutput) {
            Source.append(PortName);
            Destination.append(GetBusNameContinues(comp, EndIndex, TheNets));
          } else {
            Destination.append(PortName);
            Source.append(GetBusNameContinues(comp, EndIndex, TheNets));
          }
          while (Destination.length() < SallignmentSize) {
            Destination.append(" ");
          }
          Contents.append(
              Tab + HDL.assignPreamble() + Destination + HDL.assignOperator() + Source + ";");
        } else {
          /* The last case, we have to enumerate through each bit */
          for (int bit = 0; bit < NrOfBits; bit++) {
            Source.setLength(0);
            Destination.setLength(0);
            if (IsOutput) {
              Source.append(PortName + HDL.BracketOpen() + bit + HDL.BracketClose());
            } else {
              Destination.append(PortName + HDL.BracketOpen() + bit + HDL.BracketClose());
            }
            ConnectionPoint SolderPoint = ConnectionInformation.GetConnection((byte) bit);
            if (SolderPoint.GetParentNet() == null) {
              /* The net is not connected */
              if (IsOutput) {
                continue;
              } else {
                Reporter.Report.AddSevereWarning(
                    "Found an unconnected output bus pin, tied bit "
                        + bit
                        + " to ground!");
                Source.append(HDL.GetZeroVector(1, true));
              }
            } else {
              /*
               * The net is connected, we have to find out if the
               * connection is to a bus or to a normal net
               */
              if (SolderPoint.GetParentNet().BitWidth() == 1) {
                /* The connection is to a Net */
                if (IsOutput) {
                  Destination.append(NetName).append(TheNets.GetNetId(SolderPoint.GetParentNet()));
                } else {
                  Source.append(NetName).append(TheNets.GetNetId(SolderPoint.GetParentNet()));
                }
              } else {
                /* The connection is to an entry of a bus */
                if (IsOutput) {
                  Destination.append(
                      BusName
                          + TheNets.GetNetId(SolderPoint.GetParentNet())
                          + HDL.BracketOpen()
                          + SolderPoint.GetParentNetBitIndex()
                          + HDL.BracketClose());
                } else {
                  Source.append(
                      BusName
                          + TheNets.GetNetId(SolderPoint.GetParentNet())
                          + HDL.BracketOpen()
                          + SolderPoint.GetParentNetBitIndex()
                          + HDL.BracketClose());
                }
              }
            }
            while (Destination.length() < SallignmentSize) {
              Destination.append(" ");
            }
            if (bit != 0) {
              Contents.append("\n");
            }
            Contents.append(
                Tab + HDL.assignPreamble() + Destination + HDL.assignOperator() + Source + ";");
          }
        }
      }
    }
    return Contents.toString();
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
