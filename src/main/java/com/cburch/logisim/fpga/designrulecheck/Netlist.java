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

package com.cburch.logisim.fpga.designrulecheck;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Probe;
import com.cburch.logisim.std.wiring.Tunnel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JProgressBar;

public class Netlist implements CircuitListener {

  @Override
  public void circuitChanged(CircuitEvent event) {
    int ev = event.getAction();
    if (event.getData() instanceof InstanceComponent) {
      InstanceComponent inst = (InstanceComponent) event.getData();
      if (event.getCircuit().equals(MyCircuit)) {
        switch (ev) {
          case CircuitEvent.ACTION_ADD:
            DRCStatus = DRC_REQUIRED;
            if (inst.getFactory() instanceof SubcircuitFactory) {
              SubcircuitFactory fac = (SubcircuitFactory) inst.getFactory();
              Circuit sub = fac.getSubcircuit();

              if (MySubCircuitMap.containsKey(sub)) {
                MySubCircuitMap.put(sub, MySubCircuitMap.get(sub) + 1);
              } else {
                MySubCircuitMap.put(sub, 1);
                sub.addCircuitListener(this);
              }
            }
            break;
          case CircuitEvent.ACTION_REMOVE:
            DRCStatus = DRC_REQUIRED;
            if (inst.getFactory() instanceof SubcircuitFactory) {
              SubcircuitFactory fac = (SubcircuitFactory) inst.getFactory();
              Circuit sub = fac.getSubcircuit();
              if (MySubCircuitMap.containsKey(sub)) {
                if (MySubCircuitMap.get(sub) == 1) {
                  MySubCircuitMap.remove(sub);
                  sub.removeCircuitListener(this);
                } else {
                  MySubCircuitMap.put(sub, MySubCircuitMap.get(sub) - 1);
                }
              }
            }
            break;
          case CircuitEvent.ACTION_CLEAR:
          case CircuitEvent.ACTION_INVALIDATE:
            DRCStatus = DRC_REQUIRED;
            break;
        }
      } else {
        if (inst.getFactory() instanceof Pin) {
          DRCStatus = DRC_REQUIRED;
        }
      }
    }
  }

  private static class SourceInfo {
    private final ConnectionPoint source;
    private final byte index;

    public SourceInfo(ConnectionPoint source, byte index) {
      this.source = source;
      this.index = index;
    }

    public Integer getIndex() {
      return (int) index;
    }

    public ConnectionPoint getSource() {
      return source;
    }
  }

  public static class NetInfo {

    private final Net TheNet;
    private final byte BitIndex;

    public NetInfo(Net ConcernedNet, byte Index) {
      TheNet = ConcernedNet;
      BitIndex = Index;
    }

    public Byte getIndex() {
      return BitIndex;
    }

    public Net getNet() {
      return TheNet;
    }
  }

  private String CircuitName;
  private final ArrayList<Net> MyNets = new ArrayList<>();
  private final Map<Circuit, Integer> MySubCircuitMap = new HashMap<>();
  private final ArrayList<NetlistComponent> MySubCircuits = new ArrayList<>();
  private final ArrayList<NetlistComponent> MyComponents = new ArrayList<>();
  private final ArrayList<NetlistComponent> MyClockGenerators = new ArrayList<>();
  private final ArrayList<NetlistComponent> MyInOutPorts = new ArrayList<>();
  private final ArrayList<NetlistComponent> MyInputPorts = new ArrayList<>();
  private final ArrayList<NetlistComponent> MyOutputPorts = new ArrayList<>();
  private final ArrayList<Component> MyComplexSplitters = new ArrayList<>();
  private Integer LocalNrOfInportBubles;
  private Integer LocalNrOfOutportBubles;
  private Integer LocalNrOfInOutBubles;
  private final ClockTreeFactory MyClockInformation = new ClockTreeFactory();
  private final Circuit MyCircuit;
  private int DRCStatus;
  private final Set<Wire> wires = new HashSet<>();
  private ArrayList<String> CurrentHierarchyLevel;
  public static final int DRC_REQUIRED = 4;
  public static final int DRC_PASSED = 0;
  public static final int ANNOTATE_REQUIRED = 1;
  public static final int DRC_ERROR = 2;

  public static final Color DRC_INSTANCE_MARK_COLOR = Color.RED;
  public static final Color DRC_LABEL_MARK_COLOR = Color.MAGENTA;
  public static final Color DRC_WIRE_MARK_COLOR = Color.RED;

  public Netlist(Circuit ThisCircuit) {
    MyCircuit = ThisCircuit;
    this.clear();
  }

  public void cleanClockTree(ClockSourceContainer ClockSources) {
    /* First pass, we cleanup all old information */
    MyClockInformation.clean();
    MyClockInformation.SetSourceContainer(ClockSources);
    /* Second pass, we go down the hierarchy */
    for (NetlistComponent sub : MySubCircuits) {
      SubcircuitFactory SubFact = (SubcircuitFactory) sub.GetComponent().getFactory();
      SubFact.getSubcircuit().getNetList().cleanClockTree(ClockSources);
    }
  }

  public void clear() {
    for (NetlistComponent subcirc : MySubCircuits) {
      SubcircuitFactory SubFact = (SubcircuitFactory) subcirc.GetComponent().getFactory();
      SubFact.getSubcircuit().getNetList().clear();
    }
    DRCStatus = DRC_REQUIRED;
    MyNets.clear();
    MySubCircuits.clear();
    MyComponents.clear();
    MyClockGenerators.clear();
    MyInputPorts.clear();
    MyInOutPorts.clear();
    MyOutputPorts.clear();
    MyComplexSplitters.clear();
    LocalNrOfInportBubles = 0;
    LocalNrOfOutportBubles = 0;
    LocalNrOfInOutBubles = 0;
    if (CurrentHierarchyLevel == null) {
      CurrentHierarchyLevel = new ArrayList<>();
    } else {
      CurrentHierarchyLevel.clear();
    }
  }

  public String getName() {
    if (MyCircuit != null) return MyCircuit.getName();
    else return "Unknown";
  }

  public void ConstructHierarchyTree(
      Set<String> ProcessedCircuits,
      ArrayList<String> HierarchyName,
      Integer GlobalInputID,
      Integer GlobalOutputID,
      Integer GlobalInOutID) {
    if (ProcessedCircuits == null) {
      ProcessedCircuits = new HashSet<>();
    }
    /*
     * The first step is to go down to the leaves and visit all involved
     * sub-circuits to construct the local bubble information and form the
     * Mappable components tree
     */
    LocalNrOfInportBubles = 0;
    LocalNrOfOutportBubles = 0;
    LocalNrOfInOutBubles = 0;
    for (NetlistComponent comp : MySubCircuits) {
      SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent().getFactory();
      ArrayList<String> MyHierarchyName = new ArrayList<>(HierarchyName);
      MyHierarchyName.add(
          CorrectLabel.getCorrectLabel(
              comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      boolean FirstTime = !ProcessedCircuits.contains(sub.getName());
      if (FirstTime) {
        ProcessedCircuits.add(sub.getName());
        sub.getSubcircuit()
            .getNetList()
            .ConstructHierarchyTree(
                ProcessedCircuits, MyHierarchyName, GlobalInputID, GlobalOutputID, GlobalInOutID);
      }
      int subInputBubbles = sub.getSubcircuit().getNetList().NumberOfInputBubbles();
      int subInOutBubbles = sub.getSubcircuit().getNetList().NumberOfInOutBubbles();
      int subOutputBubbles = sub.getSubcircuit().getNetList().NumberOfOutputBubbles();
      comp.SetLocalBubbleID(
          LocalNrOfInportBubles,
          subInputBubbles,
          LocalNrOfOutportBubles,
          subOutputBubbles,
          LocalNrOfInOutBubles,
          subInOutBubbles);
      LocalNrOfInportBubles += subInputBubbles;
      LocalNrOfInOutBubles += subInOutBubbles;
      LocalNrOfOutportBubles += subOutputBubbles;
      comp.AddGlobalBubbleID(
          MyHierarchyName,
          GlobalInputID,
          subInputBubbles,
          GlobalOutputID,
          subOutputBubbles,
          GlobalInOutID,
          subInOutBubbles);
      if (!FirstTime) {
        sub.getSubcircuit()
            .getNetList()
            .EnumerateGlobalBubbleTree(
                MyHierarchyName, GlobalInputID, GlobalOutputID, GlobalInOutID);
      }
      GlobalInputID += subInputBubbles;
      GlobalInOutID += subInOutBubbles;
      GlobalOutputID += subOutputBubbles;
    }
    /*
     * Here we processed all sub-circuits of the local hierarchy level, now
     * we have to process the IO components
     */
    for (NetlistComponent comp : MyComponents) {
      if (comp.GetMapInformationContainer() != null) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(HierarchyName);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        int subInputBubbles = comp.GetMapInformationContainer().GetNrOfInports();
        int subInOutBubbles = comp.GetMapInformationContainer().GetNrOfInOutports();
        int subOutputBubbles = comp.GetMapInformationContainer().GetNrOfOutports();
        comp.SetLocalBubbleID(
            LocalNrOfInportBubles,
            subInputBubbles,
            LocalNrOfOutportBubles,
            subOutputBubbles,
            LocalNrOfInOutBubles,
            subInOutBubbles);
        LocalNrOfInportBubles += subInputBubbles;
        LocalNrOfInOutBubles += subInOutBubbles;
        LocalNrOfOutportBubles += subOutputBubbles;
        comp.AddGlobalBubbleID(
            MyHierarchyName,
            GlobalInputID,
            subInputBubbles,
            GlobalOutputID,
            subOutputBubbles,
            GlobalInOutID,
            subInOutBubbles);
        GlobalInputID += subInputBubbles;
        GlobalInOutID += subInOutBubbles;
        GlobalOutputID += subOutputBubbles;
      }
    }
  }

  public int DesignRuleCheckResult(boolean IsTopLevel, ArrayList<String> Sheetnames) {
    ArrayList<String> CompName = new ArrayList<>();
    Map<String, Component> Labels = new HashMap<>();
    ArrayList<SimpleDRCContainer> drc = new ArrayList<>();
    int CommonDRCStatus = DRC_PASSED;
    /* First we go down the tree and get the DRC status of all sub-circuits */
    for (Circuit circ : MySubCircuitMap.keySet()) {
      CommonDRCStatus |= circ.getNetList().DesignRuleCheckResult(false, Sheetnames);
    }
    /* Check if we are okay */
    if (DRCStatus == DRC_PASSED) {
      return CommonDRCStatus;
    } else {
      /* There are changes, so we clean up the old information */
      clear();
      DRCStatus = DRC_PASSED; /*
			 * we mark already passed, if an error
			 * occurs the status is changed
			 */
    }
    /*
     * Check for duplicated sheet names, this is bad as we will have
     * multiple "different" components with the same name
     */
    if (MyCircuit.getName().isEmpty()) {
      /*
       * in the current implementation of logisim this should never
       * happen, but we leave it in
       */
      Reporter.Report.AddFatalError(S.get("EmptyNamedSheet"));
      DRCStatus |= DRC_ERROR;
    }
    if (Sheetnames.contains(MyCircuit.getName())) {
      /*
       * in the current implementation of logisim this should never
       * happen, but we leave it in
       */
      Reporter.Report.AddFatalError(S.fmt("MultipleSheetSameName", MyCircuit.getName()));
      DRCStatus |= DRC_ERROR;
    } else {
      Sheetnames.add(MyCircuit.getName());
    }
    /* Preparing stage */
    for (Component comp : MyCircuit.getNonWires()) {
      String ComponentName = comp.getFactory().getHDLName(comp.getAttributeSet());
      if (!CompName.contains(ComponentName)) {
        CompName.add(ComponentName);
      }
    }
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("HDL_noLabel"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_INSTANCE));
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("HDL_CompNameIsLabel"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_LABEL));
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("HDL_LabelInvalid"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_LABEL));
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("HDL_DuplicatedLabels"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_LABEL));
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("HDL_Tristate"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_INSTANCE));
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("HDL_unsupported"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_INSTANCE));
    for (Component comp : MyCircuit.getNonWires()) {
      /*
       * Here we check if the components are supported for the HDL
       * generation
       */
      if (!comp.getFactory().HDLSupportedComponent(comp.getAttributeSet())) {
        drc.get(5).AddMarkComponent(comp);
        DRCStatus |= DRC_ERROR;
      }
      /*
       * we check that all components that require a non zero label
       * (annotation) have a label set
       */
      if (comp.getFactory().RequiresNonZeroLabel()) {
        String Label =
            CorrectLabel.getCorrectLabel(comp.getAttributeSet().getValue(StdAttr.LABEL))
                .toUpperCase();
        String ComponentName = comp.getFactory().getHDLName(comp.getAttributeSet());
        if (Label.isEmpty()) {
          drc.get(0).AddMarkComponent(comp);
          DRCStatus |= ANNOTATE_REQUIRED;
        } else {
          if (CompName.contains(Label)) {
            drc.get(1).AddMarkComponent(comp);
            DRCStatus |= DRC_ERROR;
          }
          if (!CorrectLabel.IsCorrectLabel(Label)) {
            /* this should not happen anymore */
            drc.get(2).AddMarkComponent(comp);
            DRCStatus |= DRC_ERROR;
          }
          if (Labels.containsKey(Label)) {
            drc.get(3).AddMarkComponent(comp);
            drc.get(3).AddMarkComponent(Labels.get(Label));
            DRCStatus |= DRC_ERROR;
          } else {
            Labels.put(Label, comp);
          }
        }
        if (comp.getFactory() instanceof SubcircuitFactory) {
          /* Special care has to be taken for sub-circuits */
          if (Label.equals(ComponentName.toUpperCase())) {
            drc.get(1).AddMarkComponent(comp);
            DRCStatus |= DRC_ERROR;
          }
          if (!CorrectLabel.IsCorrectLabel(
              comp.getFactory().getName(),
              S.fmt("FoundBadComponent", comp.getFactory().getName(), MyCircuit.getName()))) {
            DRCStatus |= DRC_ERROR;
          }
          SubcircuitFactory sub = (SubcircuitFactory) comp.getFactory();
          LocalNrOfInportBubles =
              LocalNrOfInportBubles + sub.getSubcircuit().getNetList().NumberOfInputBubbles();
          LocalNrOfOutportBubles =
              LocalNrOfOutportBubles + sub.getSubcircuit().getNetList().NumberOfOutputBubbles();
          LocalNrOfInOutBubles =
              LocalNrOfInOutBubles + sub.getSubcircuit().getNetList().NumberOfInOutBubbles();
        }
      }
      /* Now we check that no tri-state are present */
      if (comp.getFactory().HasThreeStateDrivers(comp.getAttributeSet())) {
        drc.get(4).AddMarkComponent(comp);
        DRCStatus |= DRC_ERROR;
      }
    }
    for (SimpleDRCContainer simpleDRCContainer : drc)
      if (simpleDRCContainer.DRCInfoPresent())
        Reporter.Report.AddError(simpleDRCContainer);
    drc.clear();
    /* Here we have to quit as the netlist generation needs a clean tree */
    if ((DRCStatus | CommonDRCStatus) != DRC_PASSED) {
      return DRCStatus | CommonDRCStatus;
    }
    /*
     * Okay we now know for sure that all elements are supported, lets build
     * the net list
     */
    Reporter.Report.AddInfo(S.fmt("BuildingNetlistFor", MyCircuit.getName()));
    if (!this.GenerateNetlist()) {
      this.clear();
      DRCStatus = DRC_ERROR;
      /*
       * here we have to quit, as all the following steps depend on a
       * proper netlist
       */
      return DRCStatus | CommonDRCStatus;
    }
    if (NetlistHasShortCircuits()) {
      clear();
      DRCStatus = DRC_ERROR;
      return DRCStatus | CommonDRCStatus;
    }
    /* Check for connections without a source */
    NetlistHasSinksWithoutSource();
    /* Check for unconnected input pins on components and generate warnings */
    for (NetlistComponent comp : MyComponents) {
      boolean openInputs = false;
      for (int j = 0; j < comp.NrOfEnds(); j++) {
        if (comp.EndIsInput(j) && !comp.EndIsConnected(j)) openInputs = true;
      }
      if (openInputs&&!AppPreferences.SupressOpenPinWarnings.get()) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                MyCircuit,
                S.get("NetList_UnconnectedInputs"),
                SimpleDRCContainer.LEVEL_NORMAL,
                SimpleDRCContainer.MARK_INSTANCE);
        warn.AddMarkComponent(comp.GetComponent());
        Reporter.Report.AddWarning(warn);
      }
    }
    /* Check for unconnected input pins on subcircuits and generate warnings */
    for (NetlistComponent comp : MySubCircuits) {
      boolean openInputs = false;
      for (int j = 0; j < comp.NrOfEnds(); j++) {
        if (comp.EndIsInput(j) && !comp.EndIsConnected(j)) openInputs = true;
      }
      if (openInputs&&!AppPreferences.SupressOpenPinWarnings.get()) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                MyCircuit,
                S.get("NetList_UnconnectedInputs"),
                SimpleDRCContainer.LEVEL_SEVERE,
                SimpleDRCContainer.MARK_INSTANCE);
        warn.AddMarkComponent(comp.GetComponent());
        Reporter.Report.AddWarning(warn);
      }
    }
    /* Check for unconnected input pins in my circuit and generate warnings */
    for (NetlistComponent comp : MyInputPorts) {
      boolean openInputs = false;
      for (int j = 0; j < comp.NrOfEnds(); j++) {
        if (!comp.EndIsConnected(j)) openInputs = true;
      }
      if (openInputs&&!AppPreferences.SupressOpenPinWarnings.get()) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                MyCircuit,
                S.get("NetList_UnconnectedInput"),
                SimpleDRCContainer.LEVEL_NORMAL,
                SimpleDRCContainer.MARK_INSTANCE);
        warn.AddMarkComponent(comp.GetComponent());
        Reporter.Report.AddWarning(warn);
      }
    }
    /* Check for unconnected output pins in my circuit and generate warnings */
    for (NetlistComponent comp : MyOutputPorts) {
      boolean openOutputs = false;
      for (int j = 0; j < comp.NrOfEnds(); j++) {
        if (!comp.EndIsConnected(j)) openOutputs = true;
      }
      if (openOutputs&&!AppPreferences.SupressOpenPinWarnings.get()) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                MyCircuit,
                S.get("NetList_UnconnectedOutput"),
                SimpleDRCContainer.LEVEL_NORMAL,
                SimpleDRCContainer.MARK_INSTANCE);
        warn.AddMarkComponent(comp.GetComponent());
        Reporter.Report.AddWarning(warn);
      }
    }

    /* Only if we are on the top-level we are going to build the clock-tree */
    if (IsTopLevel) {
      if (!DetectClockTree()) {
        DRCStatus = DRC_ERROR;
        return DRCStatus | CommonDRCStatus;
      }
      ConstructHierarchyTree(null, new ArrayList<>(), 0, 0, 0);
      int ports =
          NumberOfInputPorts()
              + NumberOfOutputPorts()
              + LocalNrOfInportBubles
              + LocalNrOfOutportBubles
              + LocalNrOfInOutBubles;
      if (ports == 0) {
        Reporter.Report.AddFatalError(S.fmt("TopLevelNoIO", MyCircuit.getName()));
        DRCStatus = DRC_ERROR;
        return DRCStatus | CommonDRCStatus;
      }
      /* Check for gated clocks */
      if (!DetectGatedClocks()) {
        DRCStatus = DRC_ERROR;
        return DRCStatus | CommonDRCStatus;
      }
    }

    Reporter.Report.AddInfo(
        S.fmt("CircuitInfoString", MyCircuit.getName(), NumberOfNets(), NumberOfBusses()));
    Reporter.Report.AddInfo(S.fmt("DRCPassesString", MyCircuit.getName()));
    DRCStatus = DRC_PASSED;
    return DRCStatus | CommonDRCStatus;
  }

  private boolean DetectClockTree() {
    /*
     * First pass, we remove all information of previously detected
     * clock-trees
     */
    ClockSourceContainer ClockSources = MyClockInformation.GetSourceContainer();
    cleanClockTree(ClockSources);
    /* Second pass, we build the clock tree */
    ArrayList<Netlist> HierarchyNetlists = new ArrayList<>();
    HierarchyNetlists.add(this);
    return MarkClockSourceComponents(new ArrayList<>(), HierarchyNetlists, ClockSources);
  }

  /* Here all private handles are defined */
  private void EnumerateGlobalBubbleTree(
      ArrayList<String> HierarchyName, int StartInputID, int StartOutputID, int StartInOutID) {
    for (NetlistComponent comp : MySubCircuits) {
      SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent().getFactory();
      ArrayList<String> MyHierarchyName = new ArrayList<>(HierarchyName);
      MyHierarchyName.add(
          CorrectLabel.getCorrectLabel(
              comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      sub.getSubcircuit()
          .getNetList()
          .EnumerateGlobalBubbleTree(
              MyHierarchyName,
              StartInputID + comp.GetLocalBubbleInputStartId(),
              StartOutputID + comp.GetLocalBubbleOutputStartId(),
              StartInOutID + comp.GetLocalBubbleInOutStartId());
    }
    for (NetlistComponent comp : MyComponents) {
      if (comp.GetMapInformationContainer() != null) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(HierarchyName);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        int subInputBubbles = comp.GetMapInformationContainer().GetNrOfInports();
        int subInOutBubbles = comp.GetMapInformationContainer().GetNrOfInOutports();
        int subOutputBubbles = comp.GetMapInformationContainer().GetNrOfOutports();
        comp.AddGlobalBubbleID(
            MyHierarchyName,
            StartInputID + comp.GetLocalBubbleInputStartId(),
            subInputBubbles,
            StartOutputID + comp.GetLocalBubbleOutputStartId(),
            subOutputBubbles,
            StartInOutID,
            subInOutBubbles);
      }
    }
  }

  private Net FindConnectedNet(Location loc) {
    for (Net Current : MyNets) {
      if (Current.contains(loc)) {
        return Current;
      }
    }
    return null;
  }

  private boolean GenerateNetlist() {
    ArrayList<SimpleDRCContainer> drc = new ArrayList<>();
    boolean errors = false;
    CircuitName = MyCircuit.getName();
    JProgressBar progress = Reporter.Report.getProgressBar();
    int curMax = 0;
    int curVal = 0;
    String curStr = ""; 
    if (progress != null) {
      curMax = progress.getMaximum();
      curVal = progress.getValue();
      curStr = progress.getString();
      progress.setMaximum(7);
      progress.setString(S.fmt("NetListBuild", CircuitName,1));
    }

    wires.clear();
    wires.addAll(MyCircuit.getWires());
    /*
     * FIRST PASS: In this pass we take all wire segments and see if they
     * are connected to other segments. If they are connected we build a
     * net.
     */
    while (wires.size() != 0) {
      Net NewNet = new Net();
      GetNet(null, NewNet);
      if (!NewNet.isEmpty()) {
        MyNets.add(NewNet);
      }
    }
    /*
     * Here we start to detect direct input-output component connections,
     * read we detect "hidden" nets
     */
    Set<Component> components = MyCircuit.getNonWires();
    /* we Start with the creation of an outputs list */
    Set<Location> OutputsList = new HashSet<>();
    Set<Location> InputsList = new HashSet<>();
    Set<Component> TunnelList = new HashSet<>();
    MyComplexSplitters.clear();
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("NetList_IOError"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_INSTANCE));
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("NetList_BitwidthError"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_WIRE));
    for (Component com : components) {
      /*
       * We do not process the splitter and tunnel, they are processed
       * later on
       */
      boolean Ignore = false;

      /* In this case, the probe should not be synthetised:
       * We could set the Probe as non-HDL element. But If we set the Probe
       * as non HDL element, logisim will not allow user to download the design.
       *
       * In some case we need to use Logisim Simulation before running the design on the hardware.
       * During simulation, probes are very helpful to see signals values. And when simulation is ok,
       * the user does not want to delete all probes.
       * Thus, here we remove it form the netlist so it is transparent.
       */
      if (com.getFactory() instanceof Probe) {
        continue;
      }

      if (com.getFactory() instanceof SplitterFactory) {
        MyComplexSplitters.add(com);
        Ignore = true;
      }
      if (com.getFactory() instanceof Tunnel) {
        TunnelList.add(com);
        Ignore = true;
      }

      List<EndData> ends = com.getEnds();
      for (EndData end : ends) {
        if (!Ignore) {
          if (end.isInput() && end.isOutput()) {
            /* The IO Port can be either output or input */
            //		if (!(com.getFactory() instanceof PortIO)) {
            //			drc.get(0).AddMarkComponent(com);
            //		}
          } else if (end.isOutput()) {
            OutputsList.add(end.getLocation());
          } else {
            InputsList.add(end.getLocation());
          }
        }
        /* Here we are going to mark the bitwidths on the nets */
        int width = end.getWidth().getWidth();
        Location loc = end.getLocation();
        // Collection<Component> component_verify = MyCircuit.getAllContaining(loc);
        for (Net ThisNet : MyNets) {
          if (ThisNet.contains(loc)) {
            if (!ThisNet.setWidth(width)) {
              drc.get(1).AddMarkComponents(ThisNet.getWires());
            }
          }
        }
      }
    }
    for (SimpleDRCContainer simpleDRCContainer : drc) {
      if (simpleDRCContainer.DRCInfoPresent()) {
        errors = true;
        Reporter.Report.AddError(simpleDRCContainer);
      }
    }
    if (errors) {
      return false;
    }
    if (progress != null) {
      progress.setValue(1);
      progress.setString(S.fmt("NetListBuild", CircuitName,2));
    }
    /*
     * Now we check if an input pin is connected to an output and in case of
     * a Splitter if it is connected to either of them
     */
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("NetAdd_ComponentWidthMismatch"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_INSTANCE));
    Map<Location, Integer> Points = new HashMap<>();
    for (Component comp : components) {
      for (EndData end : comp.getEnds()) {
        Location loc = end.getLocation();
        if (Points.containsKey(loc)) {
          /* Found a connection already used */
          boolean newNet = true;
          for (Net net : MyNets) {
            if (net.contains(loc)) newNet = false;
          }
          if (newNet) {
            int BitWidth = Points.get(loc);
            if (BitWidth == end.getWidth().getWidth()) {
              MyNets.add(new Net(loc, BitWidth));
            } else {
              drc.get(0).AddMarkComponent(comp);
            }
          }
        } else Points.put(loc, end.getWidth().getWidth());
      }
    }
    if (drc.get(0).DRCInfoPresent()) {
      Reporter.Report.AddError(drc.get(0));
      return false;
    }

    if (progress != null) {
      progress.setValue(2);
      progress.setString(S.fmt("NetListBuild", CircuitName,3));
    }
    /*
     * Here we are going to process the tunnels and possible merging of the
     * tunneled nets
     */
    boolean TunnelsPresent = false;
    for (Component com : TunnelList) {
      List<EndData> ends = com.getEnds();
      for (EndData end : ends) {
        for (Net ThisNet : MyNets) {
          if (ThisNet.contains(end.getLocation())) {
            ThisNet.addTunnel(com.getAttributeSet().getValue(StdAttr.LABEL));
            TunnelsPresent = true;
          }
        }
      }
    }
    drc.clear();
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("NetMerge_BitWidthError"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_WIRE));
    if (TunnelsPresent) {
      Iterator<Net> NetIterator = MyNets.listIterator();
      while (NetIterator.hasNext()) {
        Net ThisNet = NetIterator.next();
        if (ThisNet.HasTunnel() && (MyNets.indexOf(ThisNet) < (MyNets.size() - 1))) {
          boolean merged = false;
          Iterator<Net> SearchIterator = MyNets.listIterator(MyNets.indexOf(ThisNet) + 1);
          while (SearchIterator.hasNext() && !merged) {
            Net SearchNet = SearchIterator.next();
            for (String name : ThisNet.TunnelNames()) {
              if (SearchNet.ContainsTunnel(name) && !merged) {
                merged = true;
                if (!SearchNet.merge(ThisNet)) {
                  drc.get(0).AddMarkComponents(SearchNet.getWires());
                  drc.get(0).AddMarkComponents(ThisNet.getWires());
                }
              }
            }
          }
          if (merged) {
            NetIterator.remove();
          }
        }
      }
    }
    if (drc.get(0).DRCInfoPresent()) {
      Reporter.Report.AddError(drc.get(0));
      return false;
    }
    if (progress != null) {
      progress.setValue(3);
      progress.setString(S.fmt("NetListBuild", CircuitName,4));
    }

    /* At this point all net segments are build. All tunnels have been removed. There is still the processing of
     * the splitters and the determination of the direction of the nets.
     */

    /* First we are going to check on duplicated splitters and remove them */
    Iterator<Component> MySplitIterator = MyComplexSplitters.listIterator();
    while (MySplitIterator.hasNext()) {
      Component ThisSplitter = MySplitIterator.next();
      if (MyComplexSplitters.indexOf(ThisSplitter) < (MyComplexSplitters.size() - 1)) {
        boolean FoundDuplicate = false;
        Iterator<Component> SearchIterator =
            MyComplexSplitters.listIterator(MyComplexSplitters.indexOf(ThisSplitter) + 1);
        while (SearchIterator.hasNext() && !FoundDuplicate) {
          Component SearchSplitter = SearchIterator.next();
          if (SearchSplitter.getLocation().equals(ThisSplitter.getLocation())) {
            FoundDuplicate = true;
            for (int i = 0; i < SearchSplitter.getEnds().size(); i++) {
              if (!SearchSplitter.getEnd(i)
                  .getLocation()
                  .equals(ThisSplitter.getEnd(i).getLocation())) FoundDuplicate = false;
            }
          }
        }
        if (FoundDuplicate) {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  MyCircuit,
                  S.get("NetList_duplicatedSplitter"),
                  SimpleDRCContainer.LEVEL_SEVERE,
                  SimpleDRCContainer.MARK_INSTANCE);
          warn.AddMarkComponent(ThisSplitter);
          Reporter.Report.AddWarning(warn);
          MySplitIterator.remove();
        }
      }
    }

    /* In this round we are going to detect the unconnected nets meaning those having a width of 0 and remove them */
    drc.clear();
    Iterator<Net> NetIterator = MyNets.listIterator();
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("NetList_emptynets"),
            SimpleDRCContainer.LEVEL_NORMAL,
            SimpleDRCContainer.MARK_WIRE));
    while (NetIterator.hasNext()) {
      Net wire = NetIterator.next();
      if (wire.BitWidth() == 0) {
        drc.get(0).AddMarkComponents(wire.getWires());
        NetIterator.remove();
      }
    }
    if (drc.get(0).DRCInfoPresent()) {
      Reporter.Report.AddWarning(drc.get(0));
    }
    MySplitIterator = MyComplexSplitters.iterator();
    /* We also check quickly the splitters and remove the ones where input-bus is output-bus. We mark those who are not
     * correctly connected and remove both versions from the set.
     */
    drc.clear();
    drc.add(
        new SimpleDRCContainer(
            MyCircuit,
            S.get("NetList_ShortCircuit"),
            SimpleDRCContainer.LEVEL_FATAL,
            SimpleDRCContainer.MARK_WIRE));
    errors = false;
    while (MySplitIterator.hasNext()) {
      Component mySplitter = MySplitIterator.next();
      int BusWidth = mySplitter.getEnd(0).getWidth().getWidth();
      List<EndData> myEnds = mySplitter.getEnds();
      int MaxFanoutWidth = 0;
      int index = -1;
      for (int i = 1; i < myEnds.size(); i++) {
        int width = mySplitter.getEnd(i).getWidth().getWidth();
        if (width > MaxFanoutWidth) {
          MaxFanoutWidth = width;
          index = i;
        }
      }
      /* stupid situation first: the splitters bus connection is a single fanout */
      if (BusWidth == MaxFanoutWidth) {
        Net busnet = null;
        Net connectedNet = null;
        Location BusLoc = mySplitter.getEnd(0).getLocation();
        Location ConnectedLoc = mySplitter.getEnd(index).getLocation();
        boolean issueWarning = false;
        /* here we search for the nets */
        for (Net CurrentNet : MyNets) {
          if (CurrentNet.contains(BusLoc)) {
            if (busnet != null) {
              Reporter.Report.AddFatalError(
                  "BUG: Multiple bus nets found for a single splitter\n ==> "
                      + this.getClass().getName().replaceAll("\\.", "/")
                      + ":"
                      + Thread.currentThread().getStackTrace()[2].getLineNumber()
                      + "\n");
              return false;
            } else {
              busnet = CurrentNet;
            }
          }
          if (CurrentNet.contains(ConnectedLoc)) {
            if (connectedNet != null) {
              Reporter.Report.AddFatalError(
                  "BUG: Multiple nets found for a single splitter split connection\n ==> "
                      + this.getClass().getName().replaceAll("\\.", "/")
                      + ":"
                      + Thread.currentThread().getStackTrace()[2].getLineNumber()
                      + "\n");
              return false;
            } else {
              connectedNet = CurrentNet;
            }
          }
        }
        if (connectedNet != null) {
          if (busnet != null) {
            /* we can merge both nets */
            if (!busnet.merge(connectedNet)) {
              Reporter.Report.AddFatalError(
                  "BUG: Splitter bus merge error\n ==> "
                      + this.getClass().getName().replaceAll("\\.", "/")
                      + ":"
                      + Thread.currentThread().getStackTrace()[2].getLineNumber()
                      + "\n");
              return false;
            } else {
              MyNets.remove(connectedNet);
            }
          } else {
            issueWarning = true;
          }
        } else {
          issueWarning = true;
        }
        if (issueWarning) {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  MyCircuit,
                  S.get("NetList_NoSplitterConnection"),
                  SimpleDRCContainer.LEVEL_SEVERE,
                  SimpleDRCContainer.MARK_INSTANCE);
          warn.AddMarkComponent(mySplitter);
          Reporter.Report.AddWarning(warn);
        }
        MySplitIterator.remove(); /* Does not exist anymore */
      }
    }

    if (progress != null) {
      progress.setValue(4);
      progress.setString(S.fmt("NetListBuild", CircuitName,5));
    }
    /*
     * Finally we have to process the splitters to determine the bus
     * hierarchy (if any)
     */
    /*
     * In this round we only process the evident splitters and remove them
     * from the list
     */
    for (Component com : MyComplexSplitters) {
      /*
       * Currently by definition end(0) is the combined end of the
       * splitter
       */
      List<EndData> ends = com.getEnds();
      EndData CombinedEnd = ends.get(0);
      int RootNet = -1;
      /* We search for the root net in the list of nets */
      for (int i = 0; i < MyNets.size() && RootNet < 0; i++) {
        if (MyNets.get(i).contains(CombinedEnd.getLocation())) {
          RootNet = i;
        }
      }
      if (RootNet < 0) {
        Reporter.Report.AddFatalError(
            "BUG: Splitter without a bus connection\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        this.clear();
        return false;
      }
      /*
       * Now we process all the other ends to find the child busses/nets
       * of this root bus
       */
      ArrayList<Integer> Connections = new ArrayList<>();
      for (int i = 1; i < ends.size(); i++) {
        EndData ThisEnd = ends.get(i);
        /* Find the connected net */
        int ConnectedNet = -1;
        for (int j = 0; j < MyNets.size() && ConnectedNet < 1; j++) {
          if (MyNets.get(j).contains(ThisEnd.getLocation())) {
            ConnectedNet = j;
          }
        }
        Connections.add(ConnectedNet);
      }
      boolean unconnectedEnds = false;
      boolean connectedUnknownEnds = false;
      SplitterAttributes sattrs = (SplitterAttributes) com.getAttributeSet();
      for (int i = 1; i < ends.size(); i++) {
        int ConnectedNet = Connections.get(i - 1);
        if (ConnectedNet >= 0) {
          /* Has this end a connection to the root bus? */
          connectedUnknownEnds |= sattrs.isNoConnect(i);
          /* There is a net connected to this splitter's end point */
          if (!MyNets.get(ConnectedNet).setParent(MyNets.get(RootNet))) {
            MyNets.get(ConnectedNet).ForceRootNet();
          }
          /* Here we have to process the inherited bits of the parent */
          byte[] BusBitConnection = ((Splitter) com).GetEndpoints();
          for (byte b = 0; b < BusBitConnection.length; b++) {
            if (BusBitConnection[b] == i) {
              MyNets.get(ConnectedNet).AddParrentBit(b);
            }
          }
        } else {
          unconnectedEnds = true;
        }
      }
      if (unconnectedEnds) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                MyCircuit,
                S.get("NetList_NoSplitterEndConnections"),
                SimpleDRCContainer.LEVEL_NORMAL,
                SimpleDRCContainer.MARK_INSTANCE);
        warn.AddMarkComponent(com);
        Reporter.Report.AddWarning(warn);
      }
      if (connectedUnknownEnds) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                MyCircuit,
                S.get("NetList_NoEndSplitterConnections"),
                SimpleDRCContainer.LEVEL_SEVERE,
                SimpleDRCContainer.MARK_INSTANCE);
         warn.AddMarkComponent(com);
         Reporter.Report.AddWarning(warn);
      }
    }
    if (progress != null) {
      progress.setValue(5);
      progress.setString(S.fmt("NetListBuild", CircuitName,6));
    }
    /*
     * Now the complete netlist is created, we have to check that each
     * net/bus entry has only 1 source and 1 or more sinks. If there exist
     * more than 1 source we have a short circuit! We keep track of the
     * sources and sinks at the root nets/buses
     */
    for (Net ThisNet : MyNets) {
      if (ThisNet.IsRootNet()) {
        ThisNet.InitializeSourceSinks();
      }
    }
    /*
     * We are going to iterate through all components and their respective
     * pins to see if they are connected to a net, and if yes if they
     * present a source or sink. We omit the splitter and tunnel as we
     * already processed those
     */

    for (Component comp : components) {
      if (comp.getFactory() instanceof SubcircuitFactory) {
        if (!ProcessSubcircuit(comp)) {
          this.clear();
          return false;
        }
      } else if ((comp.getFactory() instanceof Pin)
          || comp.getAttributeSet().containsAttribute(StdAttr.MAPINFO)
          || (comp.getFactory().getHDLGenerator(comp.getAttributeSet()) != null)) {
        if (!ProcessNormalComponent(comp)) {
          this.clear();
          return false;
        }
      }
    }
    if (progress != null) {
      progress.setValue(6);
      progress.setString(S.fmt("NetListBuild", CircuitName,7));
    }

    /*
     * Here we are going to process the complex splitters, note that in the
     * previous handling of the splitters we marked all nets connected to a
     * complex splitter with a forcerootnet annotation; we are going to
     * cycle trough all these nets
     */
    for (Net thisnet : MyNets) {
      if (thisnet.IsForcedRootNet()) {
        /* Cycle through all the bits of this net */
        for (int bit = 0; bit < thisnet.BitWidth(); bit++) {
          for (Component comp : MyComplexSplitters) {
            /*
             * Currently by definition end(0) is the combined end of
             * the splitter
             */
            List<EndData> ends = comp.getEnds();
            EndData CombinedEnd = ends.get(0);
            int ConnectedBus = -1;
            SplitterAttributes sattrs = (SplitterAttributes)comp.getAttributeSet();
            /* We search for the root net in the list of nets */
            for (int i = 0; i < MyNets.size() && ConnectedBus < 0; i++) {
              if (MyNets.get(i).contains(CombinedEnd.getLocation())) {
                ConnectedBus = i;
              }
            }
            if (ConnectedBus < 0) {
              /*
               * This should never happen as we already checked in
               * the first pass
               */
              Reporter.Report.AddFatalError(
                  "BUG: This is embarasing as this should never happen\n ==> "
                      + this.getClass().getName().replaceAll("\\.", "/")
                      + ":"
                      + Thread.currentThread().getStackTrace()[2].getLineNumber()
                      + "\n");
              this.clear();
              return false;
            }
            for (int endid = 1; endid < ends.size(); endid++) {
              /*
               * If this is an end that is not connected to the root bus
               * we can continue we already warned severly before.
               */
              if (sattrs.isNoConnect(endid)) continue;
              /*
               * we iterate through all bits to see if the current
               * net is connected to this splitter
               */
              if (thisnet.contains(ends.get(endid).getLocation())) {
                /*
                 * first we have to get the bitindices of the
                 * rootbus
                 */
                /*
                 * Here we have to process the inherited bits of
                 * the parent
                 */
                byte[] BusBitConnection = ((Splitter) comp).GetEndpoints();
                ArrayList<Byte> IndexBits = new ArrayList<>();
                for (byte b = 0; b < BusBitConnection.length; b++) {
                  if (BusBitConnection[b] == endid) {
                    IndexBits.add(b);
                  }
                }
                byte ConnectedBusIndex = IndexBits.get(bit);
                /* Figure out the rootbusid and rootbusindex */
                Net Rootbus = MyNets.get(ConnectedBus);
                while (!Rootbus.IsRootNet()) {
                  ConnectedBusIndex = Rootbus.getBit(ConnectedBusIndex);
                  Rootbus = Rootbus.getParent();
                }
                ConnectionPoint SolderPoint = new ConnectionPoint(comp);
                SolderPoint.SetParrentNet(Rootbus, ConnectedBusIndex);
                boolean IsSink = true;
                if (!thisnet.hasBitSource(bit)) {
                  if (HasHiddenSource(
                      thisnet,
                      (byte) 0,
                      Rootbus,
                      ConnectedBusIndex,
                      MyComplexSplitters,
                      new HashSet<>())) {
                    IsSink = false;
                  }
                }
                if (IsSink) {
                  thisnet.addSinkNet(bit, SolderPoint);
                } else {
                  thisnet.addSourceNet(bit, SolderPoint);
                }
              }
            }
          }
        }
      }
    }
    if (progress != null) {
      progress.setMaximum(curMax);
      progress.setValue(curVal);
      progress.setString(curStr);
    }
    /* So now we have all information we need! */
    return true;
  }

  public ArrayList<Component> GetAllClockSources() {
    return MyClockInformation.GetSourceContainer().getSources();
  }

  public ArrayList<Net> GetAllNets() {
    return MyNets;
  }

  public Circuit getCircuit() {
    return MyCircuit;
  }

  public String getCircuitName() {
    return CircuitName;
  }

  public int GetClockSourceId(ArrayList<String> HierarchyLevel, Net WhichNet, Byte Bitid) {
    return MyClockInformation.GetClockSourceId(HierarchyLevel, WhichNet, Bitid);
  }

  public int GetClockSourceId(Component comp) {
    return MyClockInformation.GetClockSourceId(comp);
  }

  public ArrayList<NetlistComponent> GetClockSources() {
    return MyClockGenerators;
  }

  public ArrayList<String> GetCurrentHierarchyLevel() {
    return CurrentHierarchyLevel;
  }

  public int GetEndIndex(NetlistComponent comp, String PinLabel, boolean IsOutputPort) {
    String label = CorrectLabel.getCorrectLabel(PinLabel);
    SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent().getFactory();
    for (int end = 0; end < comp.NrOfEnds(); end++) {
      if (comp.getEnd(end).IsOutputEnd() == IsOutputPort) {
        if (comp.getEnd(end).GetConnection((byte) 0).getChildsPortIndex()
            == sub.getSubcircuit().getNetList().GetPortInfo(label)) {
          return end;
        }
      }
    }
    return -1;
  }

  private ArrayList<ConnectionPoint> GetHiddenSinks(
      Net thisNet,
      Byte bitIndex,
      ArrayList<Component> SplitterList,
      Set<String> HandledNets,
      Boolean isSourceNet) {
    ArrayList<ConnectionPoint> result = new ArrayList<>();
    /*
     * to prevent deadlock situations we check if we already looked at this
     * net
     */
    String NetId = MyNets.indexOf(thisNet) + "-" + bitIndex;
    if (HandledNets.contains(NetId)) {
      return result;
    } else {
      HandledNets.add(NetId);
    }
    if (thisNet.hasBitSinks(bitIndex) && !isSourceNet && thisNet.IsRootNet()) {
      result.addAll(thisNet.GetBitSinks(bitIndex));
    }
    /* Check if we have a connection to another splitter */
    for (Component currentSplitter : SplitterList) {
      List<EndData> ends = currentSplitter.getEnds();
      SplitterAttributes sattrs = (SplitterAttributes)currentSplitter.getAttributeSet();
      for (byte end = 0; end < ends.size(); end++) {
    	/* prevent the search for ends that are not connected to the root bus */
    	if (end > 0 && sattrs.isNoConnect(end)) continue;
        if (thisNet.contains(ends.get(end).getLocation())) {
          /* Here we have to process the inherited bits of the parent */
          byte[] BusBitConnection = ((Splitter) currentSplitter).GetEndpoints();
          if (end == 0) {
            /* this is a main net, find the connected end */
            byte SplitterEnd = BusBitConnection[bitIndex];
            /* Find the corresponding Net index */
            Byte Netindex = 0;
            for (int index = 0; index < bitIndex; index++) {
              if (BusBitConnection[index] == SplitterEnd) {
                Netindex++;
              }
            }
            /* Find the connected Net */
            Net SlaveNet = null;
            for (Net thisnet : MyNets) {
              if (thisnet.contains(ends.get(SplitterEnd).getLocation())) {
                SlaveNet = thisnet;
              }
            }
            if (SlaveNet != null) {
              result.addAll(GetHiddenSinks(SlaveNet, Netindex, SplitterList, HandledNets, false));
            }
          } else {
            ArrayList<Byte> Rootindices = new ArrayList<>();
            for (byte b = 0; b < BusBitConnection.length; b++) {
              if (BusBitConnection[b] == end) {
                Rootindices.add(b);
              }
            }
            Net RootNet = null;
            for (Net thisnet : MyNets) {
              if (thisnet.contains(currentSplitter.getEnd(0).getLocation())) {
                RootNet = thisnet;
              }
            }
            if (RootNet != null) {
              result.addAll(GetHiddenSinks(
                        RootNet,
                        Rootindices.get(bitIndex),
                        SplitterList,
                        HandledNets,
                        false));
            }
          }
        }
      }
    }
    return result;
  }

  public NetlistComponent GetInOutPin(int index) {
    if ((index < 0) || (index >= MyInOutPorts.size())) {
      return null;
    }
    return MyInOutPorts.get(index);
  }

  public NetlistComponent GetInOutPort(int Index) {
    if ((Index < 0) || (Index >= MyInOutPorts.size())) {
      return null;
    }
    return MyInOutPorts.get(Index);
  }

  public NetlistComponent GetInputPin(int index) {
    if ((index < 0) || (index >= MyInputPorts.size())) {
      return null;
    }
    return MyInputPorts.get(index);
  }

  public NetlistComponent GetInputPort(int Index) {
    if ((Index < 0) || (Index >= MyInputPorts.size())) {
      return null;
    }
    return MyInputPorts.get(Index);
  }

  public Map<ArrayList<String>, NetlistComponent> GetMappableResources(
      ArrayList<String> Hierarchy, boolean toplevel) {
    Map<ArrayList<String>, NetlistComponent> Components =
        new HashMap<>();
    /* First we search through my sub-circuits and add those IO components */
    for (NetlistComponent comp : MySubCircuits) {
      SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent().getFactory();
      ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
      MyHierarchyName.add(
          CorrectLabel.getCorrectLabel(
              comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      Components.putAll(
          sub.getSubcircuit().getNetList().GetMappableResources(MyHierarchyName, false));
    }
    /* Now we search for all local IO components */
    for (NetlistComponent comp : MyComponents) {
      if (comp.GetMapInformationContainer() != null) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        Components.put(MyHierarchyName, comp);
      }
    }
    /* On the toplevel we have to add the pins */
    if (toplevel) {
      for (NetlistComponent comp : MyInputPorts) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        Components.put(MyHierarchyName, comp);
      }
      for (NetlistComponent comp : MyInOutPorts) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        Components.put(MyHierarchyName, comp);
      }
      for (NetlistComponent comp : MyOutputPorts) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        Components.put(MyHierarchyName, comp);
      }
    }
    return Components;
  }

  private void GetNet(Wire wire, Net ThisNet) {
    Iterator<Wire> MyIterator = wires.iterator();
    ArrayList<Wire> MatchedWires = new ArrayList<>();
    Wire CompWire = wire;
    while (MyIterator.hasNext()) {
      Wire ThisWire = MyIterator.next();
      if (CompWire == null) {
        CompWire = ThisWire;
        ThisNet.add(ThisWire);
        MyIterator.remove();
      } else if (ThisWire.sharesEnd(CompWire)) {
        MatchedWires.add(ThisWire);
        ThisNet.add(ThisWire);
        MyIterator.remove();
      }
    }
    for (Wire matched : MatchedWires) GetNet(matched, ThisNet);
    MatchedWires.clear();
  }

  public Integer GetNetId(Net selectedNet) {
    return MyNets.indexOf(selectedNet);
  }

  public ConnectionPoint GetNetlistConnectionForSubCircuit(
      String Label, int PortIndex, byte bitindex) {
    for (NetlistComponent search : MySubCircuits) {
      String CircuitLabel =
          CorrectLabel.getCorrectLabel(
              search.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (CircuitLabel.equals(Label)) {
        /* Found the component, let's search the ends */
        for (int i = 0; i < search.NrOfEnds(); i++) {
          ConnectionEnd ThisEnd = search.getEnd(i);
          if (ThisEnd.IsOutputEnd() && (bitindex < ThisEnd.NrOfBits())) {
            if (ThisEnd.GetConnection(bitindex).getChildsPortIndex() == PortIndex) {
              return ThisEnd.GetConnection(bitindex);
            }
          }
        }
      }
    }
    return null;
  }

  public ConnectionPoint GetNetlistConnectionForSubCircuitInput(
      String Label, int PortIndex, byte bitindex) {
    for (NetlistComponent search : MySubCircuits) {
      String CircuitLabel =
          CorrectLabel.getCorrectLabel(
              search.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (CircuitLabel.equals(Label)) {
        /* Found the component, let's search the ends */
        for (int i = 0; i < search.NrOfEnds(); i++) {
          ConnectionEnd ThisEnd = search.getEnd(i);
          if (!ThisEnd.IsOutputEnd() && (bitindex < ThisEnd.NrOfBits())) {
            if (ThisEnd.GetConnection(bitindex).getChildsPortIndex() == PortIndex) {
              return ThisEnd.GetConnection(bitindex);
            }
          }
        }
      }
    }
    return null;
  }

  public ArrayList<NetlistComponent> GetNormalComponents() {
    return MyComponents;
  }

  public NetlistComponent GetOutputPin(int index) {
    if ((index < 0) || (index >= MyOutputPorts.size())) {
      return null;
    }
    return MyOutputPorts.get(index);
  }

  public int GetPortInfo(String Label) {
    String Source = CorrectLabel.getCorrectLabel(Label);
    for (NetlistComponent Inport : MyInputPorts) {
      String Comp =
          CorrectLabel.getCorrectLabel(
              Inport.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (Comp.equals(Source)) {
        return MyInputPorts.indexOf(Inport);
      }
    }
    for (NetlistComponent InOutport : MyInOutPorts) {
      String Comp =
          CorrectLabel.getCorrectLabel(
              InOutport.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (Comp.equals(Source)) {
        return MyInOutPorts.indexOf(InOutport);
      }
    }
    for (NetlistComponent Outport : MyOutputPorts) {
      String Comp =
          CorrectLabel.getCorrectLabel(
              Outport.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (Comp.equals(Source)) {
        return MyOutputPorts.indexOf(Outport);
      }
    }
    return -1;
  }

  private Net GetRootNet(Net Child) {
    if (Child == null) {
      return null;
    }
    if (Child.IsRootNet()) {
      return Child;
    }
    Net RootNet = Child.getParent();
    while (!RootNet.IsRootNet()) {
      RootNet = RootNet.getParent();
    }
    return RootNet;
  }

  private byte GetRootNetIndex(Net Child, byte BitIndex) {
    if (Child == null) {
      return -1;
    }
    if ((BitIndex < 0) || (BitIndex > Child.BitWidth())) {
      return -1;
    }
    if (Child.IsRootNet()) {
      return BitIndex;
    }
    Net RootNet = Child.getParent();
    byte RootIndex = Child.getBit(BitIndex);
    while (!RootNet.IsRootNet()) {
      RootIndex = RootNet.getBit(RootIndex);
      RootNet = RootNet.getParent();
    }
    return RootIndex;
  }

  public Set<Splitter> getSplitters() {
    /* This may be cause bugs due to dual splitter on same location situations */
    Set<Splitter> SplitterList = new HashSet<>();
    for (Component comp : MyCircuit.getNonWires()) {
      if (comp.getFactory() instanceof SplitterFactory) {
        SplitterList.add((Splitter) comp);
      }
    }
    return SplitterList;
  }

  public ArrayList<NetlistComponent> GetSubCircuits() {
    return MySubCircuits;
  }

  private SourceInfo GetHiddenSource(
     Net sourceNet,
     Byte sourceBitIndex,
     Net thisNet,
     Byte bitIndex,
     List<Component> SplitterList,
     Set<String> HandledNets,
     Set<Wire> Segments) {
	/* If the source net not is null add it to the set of visited nets to
	 * prevent back-search on this net
	 */
	if (sourceNet != null) {
      String NetId = MyNets.indexOf(sourceNet) + "-" + sourceBitIndex;
      if (HandledNets.contains(NetId)) {
        return null;
      } else {
        HandledNets.add(NetId);
      }
	}
    /*
     * to prevent deadlock situations we check if we already looked at this
     * net
     */
    String NetId = MyNets.indexOf(thisNet) + "-" + bitIndex;
    if (HandledNets.contains(NetId)) {
      return null;
    } else {
      HandledNets.add(NetId);
      Segments.addAll(thisNet.getWires());
    }
    if (thisNet.hasBitSource(bitIndex)) {
      List<ConnectionPoint> sources = thisNet.GetBitSources(bitIndex);
      if (sources.size() != 1) {
        Reporter.Report.AddFatalError(
            "BUG: Found multiple sources\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return null;
      }
      return new SourceInfo(sources.get(0), bitIndex);
    }
    /* Check if we have a connection to another splitter */
    for (Component currentSplitter : SplitterList) {
      List<EndData> ends = currentSplitter.getEnds();
      for (byte end = 0; end < ends.size(); end++) {
        if (thisNet.contains(ends.get(end).getLocation())) {
          /* Here we have to process the inherited bits of the parent */
          byte[] BusBitConnection = ((Splitter) currentSplitter).GetEndpoints();
          if (end == 0) {
            /* this is a main net, find the connected end */
            byte SplitterEnd = BusBitConnection[bitIndex];
            /* Find the corresponding Net index */
            Byte Netindex = 0;
            for (int index = 0; index < bitIndex; index++) {
              if (BusBitConnection[index] == SplitterEnd) {
                Netindex++;
              }
            }
            /* Find the connected Net */
            Net SlaveNet = null;
            for (Net thisnet : MyNets) {
              if (thisnet.contains(ends.get(SplitterEnd).getLocation())) {
                SlaveNet = thisnet;
              }
            }
            if (SlaveNet != null) {
              SourceInfo ret =
                  GetHiddenSource(null, (byte) 0,
                      SlaveNet,
                      Netindex,
                      SplitterList,
                      HandledNets,
                      Segments);
              if (ret != null) return ret;
            }
          } else {
            ArrayList<Byte> Rootindices = new ArrayList<>();
            for (byte b = 0; b < BusBitConnection.length; b++) {
              if (BusBitConnection[b] == end) {
                Rootindices.add(b);
              }
            }
            Net RootNet = null;
            for (Net thisnet : MyNets) {
              if (thisnet.contains(currentSplitter.getEnd(0).getLocation())) {
                RootNet = thisnet;
              }
            }
            if (RootNet != null) {
              SourceInfo ret =
                  GetHiddenSource(null, (byte) 0,
                      RootNet,
                      Rootindices.get(bitIndex),
                      SplitterList,
                      HandledNets,
                      Segments);
              if (ret != null) return ret;
            }
          }
        }
      }
    }
    return null;
  }

  private boolean HasHiddenSource(
	  Net sourceNet,
	  Byte sourceBitIndex,
      Net thisNet,
      Byte bitIndex,
      List<Component> SplitterList,
      Set<String> HandledNets) {
	/* If the source net not is null add it to the set of visited nets to
	 * prevent back-search on this net
	 */
	if (sourceNet != null) {
      String NetId = MyNets.indexOf(sourceNet) + "-" + sourceBitIndex;
      if (HandledNets.contains(NetId)) {
        return false;
      } else {
        HandledNets.add(NetId);
      }
	}
    /*
     * to prevent deadlock situations we check if we already looked at this
     * net
     */
    String NetId = MyNets.indexOf(thisNet) + "-" + bitIndex;
    if (HandledNets.contains(NetId)) {
      return false;
    } else {
      HandledNets.add(NetId);
    }
    if (thisNet.hasBitSource(bitIndex)) {
      return true;
    }
    /* Check if we have a connection to another splitter */
    for (Component currentSplitter : SplitterList) {
      List<EndData> ends = currentSplitter.getEnds();
      for (byte end = 0; end < ends.size(); end++) {
        if (thisNet.contains(ends.get(end).getLocation())) {
          /* Here we have to process the inherited bits of the parent */
          byte[] BusBitConnection = ((Splitter) currentSplitter).GetEndpoints();
          if (end == 0) {
            /* this is a main net, find the connected end */
            byte SplitterEnd = BusBitConnection[bitIndex];
            /* Find the corresponding Net index */
            Byte Netindex = 0;
            for (int index = 0; index < bitIndex; index++) {
              if (BusBitConnection[index] == SplitterEnd) {
                Netindex++;
              }
            }
            /* Find the connected Net */
            Net SlaveNet = null;
            for (Net thisnet : MyNets) {
              if (thisnet.contains(ends.get(SplitterEnd).getLocation())) {
                SlaveNet = thisnet;
              }
            }
            if (SlaveNet != null) {
              if (HasHiddenSource(null,(byte) 0,
                  SlaveNet, Netindex, SplitterList, HandledNets)) {
                return true;
              }
            }
          } else {
            ArrayList<Byte> Rootindices = new ArrayList<>();
            for (byte b = 0; b < BusBitConnection.length; b++) {
              if (BusBitConnection[b] == end) {
                Rootindices.add(b);
              }
            }
            Net RootNet = null;
            for (Net thisnet : MyNets) {
              if (thisnet.contains(currentSplitter.getEnd(0).getLocation())) {
                RootNet = thisnet;
              }
            }
            if (RootNet != null) {
              if (HasHiddenSource(null,(byte) 0,
                  RootNet,
                  Rootindices.get(bitIndex),
                  SplitterList,
                  HandledNets)) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }

  public boolean IsContinuesBus(NetlistComponent comp, int EndIndex) {
    boolean ContinuesBus = true;
    if ((EndIndex < 0) || (EndIndex >= comp.NrOfEnds())) {
      return true;
    }
    ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
    int NrOfBits = ConnectionInformation.NrOfBits();
    if (NrOfBits == 1) {
      return true;
    }
    Net ConnectedNet = ConnectionInformation.GetConnection((byte) 0).GetParrentNet();
    byte ConnectedNetIndex = ConnectionInformation.GetConnection((byte) 0).GetParrentNetBitIndex();
    for (int i = 1; (i < NrOfBits) && ContinuesBus; i++) {
      if (ConnectedNet != ConnectionInformation.GetConnection((byte) i).GetParrentNet()) {
        /* This bit is connected to another bus */
        ContinuesBus = false;
      }
      if ((ConnectedNetIndex + 1)
          != ConnectionInformation.GetConnection((byte) i).GetParrentNetBitIndex()) {
        /* Connected to a none incremental position of the bus */
        ContinuesBus = false;
      } else {
        ConnectedNetIndex++;
      }
    }
    return ContinuesBus;
  }

  public boolean IsValid() {
    return DRCStatus == DRC_PASSED;
  }

  public void MarkClockNet(ArrayList<String> HierarchyNames, int clocksourceid, 
        ConnectionPoint connection, boolean isPinClockSource) {
    MyClockInformation.AddClockNet(HierarchyNames, clocksourceid, connection,isPinClockSource);
  }

  public boolean MarkClockSourceComponents(
      ArrayList<String> HierarchyNames,
      ArrayList<Netlist> HierarchyNetlists,
      ClockSourceContainer ClockSources) {
    /* First pass: we go down the hierarchy till the leaves */
    for (NetlistComponent sub : MySubCircuits) {
      SubcircuitFactory SubFact = (SubcircuitFactory) sub.GetComponent().getFactory();
      ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
      NewHierarchyNames.add(
          CorrectLabel.getCorrectLabel(
              sub.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
      NewHierarchyNetlists.add(SubFact.getSubcircuit().getNetList());
      if (!SubFact.getSubcircuit()
          .getNetList()
          .MarkClockSourceComponents(NewHierarchyNames, NewHierarchyNetlists, ClockSources)) {
        return false;
      }
    }
    /*
     * We see if some components require the Global fast FPGA
     * clock
     */
    for (Component comp : MyCircuit.getNonWires()) {
      if (comp.getFactory().RequiresGlobalClock()) {
        ClockSources.SetGloblaClockRequirement();
      }
    }
    /* Second pass: We mark all clock sources */
    for (NetlistComponent ClockSource : MyClockGenerators) {
      if (ClockSource.NrOfEnds() != 1) {
        Reporter.Report.AddFatalError(
            "BUG: Found a clock source with more than 1 connection\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return false;
      }
      ConnectionEnd ClockConnection = ClockSource.getEnd(0);
      if (ClockConnection.NrOfBits() != 1) {
        Reporter.Report.AddFatalError(
            "BUG: Found a clock source with a bus as output\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return false;
      }
      ConnectionPoint SolderPoint = ClockConnection.GetConnection((byte) 0);
      /* Check if the clock source is connected */
      if (SolderPoint.GetParrentNet() != null) {
        /* Third pass: add this clock to the list of ClockSources */
        int clockid = ClockSources.getClockId(ClockSource.GetComponent());
        /* Forth pass: Add this source as clock source to the tree */
        MyClockInformation.AddClockSource(HierarchyNames, clockid, SolderPoint);
        /* Fifth pass: trace the clock net all the way */
        if (!TraceClockNet(
            SolderPoint.GetParrentNet(),
            SolderPoint.GetParrentNetBitIndex(),
            clockid,
            false,
            HierarchyNames,
            HierarchyNetlists)) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean NetlistHasShortCircuits() {
    boolean ret = false;
    for (Net net : MyNets) {
      if (net.IsRootNet()) {
        if (net.hasShortCircuit()) {
          SimpleDRCContainer error =
              new SimpleDRCContainer(
                  MyCircuit,
                  S.get("NetList_ShortCircuit"),
                  SimpleDRCContainer.LEVEL_FATAL,
                  SimpleDRCContainer.MARK_WIRE);
          error.AddMarkComponents(net.getWires());
          Reporter.Report.AddError(error);
          ret = true;
        } else if (net.BitWidth() == 1 && net.GetSourceNets(0).size() > 1) {
          /* We have to check if the net is connected to multiple drivers */
          ArrayList<ConnectionPoint> sourceNets = net.GetSourceNets(0);
          HashMap<Component,Integer> sourceConnections = new HashMap<>();
          HashSet<Wire> segments = new HashSet<>(net.getWires());
          boolean foundShortCrcuit = false;
          SimpleDRCContainer error =
              new SimpleDRCContainer(
                  MyCircuit,
                  S.get("NetList_ShortCircuit"),
                  SimpleDRCContainer.LEVEL_FATAL,
                  SimpleDRCContainer.MARK_WIRE|SimpleDRCContainer.MARK_INSTANCE);
          for (ConnectionPoint sourceNet : sourceNets) {
            Net connectedNet = sourceNet.GetParrentNet();
            byte bitIndex = sourceNet.GetParrentNetBitIndex();
            if (HasHiddenSource(net, (byte) 0, connectedNet, bitIndex, MyComplexSplitters,
                new HashSet<>())) {
              SourceInfo source = GetHiddenSource(net, (byte) 0, connectedNet, bitIndex,
                  MyComplexSplitters, new HashSet<>(), segments);
              if (source == null) {
                /* this should never happen */
                return true;
              }
              Component comp = source.getSource().GetComp();
              for (Wire seg : segments)
                error.AddMarkComponent(seg);
              error.AddMarkComponent(comp);
              int index = source.getIndex();
              foundShortCrcuit |= (sourceConnections.containsKey(comp) &&
                  sourceConnections.get(comp) != index) ||
                  (sourceConnections.keySet().size() > 0);
              sourceConnections.put(comp, index);
            }
          }
          if (foundShortCrcuit) {
            ret = true;
            Reporter.Report.AddError(error);
          } else net.CleanupSourceNets(0);
        }
      }
    }
    return ret;
  }

  public boolean NetlistHasSinksWithoutSource() {
    /* First pass: we make a set with all sinks */
    Set<ConnectionPoint> MySinks = new HashSet<>();
    for (Net ThisNet : MyNets) {
      if (ThisNet.IsRootNet()) {
        MySinks.addAll(ThisNet.GetSinks());
      }
    }
    /* Second pass: we iterate along all the sources */
    for (Net ThisNet : MyNets) {
      if (ThisNet.IsRootNet()) {
        for (int i = 0; i < ThisNet.BitWidth(); i++) {
          if (ThisNet.hasBitSource(i)) {
            boolean HasSink = false;
            ArrayList<ConnectionPoint> Sinks = ThisNet.GetBitSinks(i);
            HasSink |= !Sinks.isEmpty();
            Sinks.forEach(MySinks::remove);
            ArrayList<ConnectionPoint> HiddenSinkNets =
                GetHiddenSinks(
                    ThisNet, (byte) i, MyComplexSplitters, new HashSet<>(), true);
            HasSink |= !HiddenSinkNets.isEmpty();
            HiddenSinkNets.forEach(MySinks::remove);
            if (!HasSink) {
              SimpleDRCContainer warn =
                  new SimpleDRCContainer(
                      MyCircuit,
                      S.get("NetList_SourceWithoutSink"),
                      SimpleDRCContainer.LEVEL_NORMAL,
                      SimpleDRCContainer.MARK_WIRE);
              warn.AddMarkComponents(ThisNet.getWires());
              Reporter.Report.AddWarning(warn);
            }
          }
        }
      }
    }
    if (MySinks.size() != 0) {
      for (ConnectionPoint Sink : MySinks) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                MyCircuit,
                S.get("NetList_UnsourcedSink"),
                SimpleDRCContainer.LEVEL_SEVERE,
                SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_WIRE);
        warn.AddMarkComponents(Sink.GetParrentNet().getWires());
        if (Sink.GetComp() != null) {
          warn.AddMarkComponent(Sink.GetComp());
        }
        Reporter.Report.AddWarning(warn);
      }
    }
    return false;
  }

  public int NumberOfBusses() {
    int nr_of_busses = 0;
    for (Net ThisNet : MyNets) {
      if (ThisNet.IsRootNet() && ThisNet.isBus()) {
        nr_of_busses++;
      }
    }
    return nr_of_busses;
  }

  public int NumberOfClockTrees() {
    return MyClockInformation.GetSourceContainer().getNrofSources();
  }

  public int NumberOfInOutBubbles() {
    return LocalNrOfInOutBubles;
  }

  public int NumberOfInOutPortBits() {
    int count = 0;
    for (NetlistComponent inp : MyInOutPorts) {
      count += inp.getEnd(0).NrOfBits();
    }
    return count;
  }

  public int NumberOfInOutPorts() {
    return MyInOutPorts.size();
  }

  public int NumberOfInputBubbles() {
    return LocalNrOfInportBubles;
  }

  public int NumberOfInputPortBits() {
    int count = 0;
    for (NetlistComponent inp : MyInputPorts) {
      count += inp.getEnd(0).NrOfBits();
    }
    return count;
  }

  public int NumberOfInputPorts() {
    return MyInputPorts.size();
  }

  public int NumberOfNets() {
    int nr_of_nets = 0;
    for (Net ThisNet : MyNets) {
      if (ThisNet.IsRootNet() && !ThisNet.isBus()) {
        nr_of_nets++;
      }
    }
    return nr_of_nets;
  }

  public int NumberOfOutputBubbles() {
    return LocalNrOfOutportBubles;
  }

  public int NumberOfOutputPortBits() {
    int count = 0;
    for (NetlistComponent outp : MyOutputPorts) {
      count += outp.getEnd(0).NrOfBits();
    }
    return count;
  }

  public int NumberOfOutputPorts() {
    return MyOutputPorts.size();
  }

  private boolean ProcessNormalComponent(Component comp) {
    NetlistComponent NormalComponent = new NetlistComponent(comp);
    for (EndData ThisPin : comp.getEnds()) {
      Net Connection = FindConnectedNet(ThisPin.getLocation());
      if (Connection != null) {
        int PinId = comp.getEnds().indexOf(ThisPin);
        boolean PinIsSink = ThisPin.isInput();
        ConnectionEnd ThisEnd = NormalComponent.getEnd(PinId);
        Net RootNet = GetRootNet(Connection);
        if (RootNet == null) {
          Reporter.Report.AddFatalError(
              "BUG: Unable to find a root net for a normal component\n ==> "
                  + this.getClass().getName().replaceAll("\\.", "/")
                  + ":"
                  + Thread.currentThread().getStackTrace()[2].getLineNumber()
                  + "\n");
          return false;
        }
        for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
          byte RootNetBitIndex = GetRootNetIndex(Connection, bitid);
          if (RootNetBitIndex < 0) {
            Reporter.Report.AddFatalError(
                "BUG:  Unable to find a root-net bit-index for a normal component\n ==> "
                    + this.getClass().getName().replaceAll("\\.", "/")
                    + ":"
                    + Thread.currentThread().getStackTrace()[2].getLineNumber()
                    + "\n");
            return false;
          }
          ConnectionPoint ThisSolderPoint = ThisEnd.GetConnection(bitid);
          ThisSolderPoint.SetParrentNet(RootNet, RootNetBitIndex);
          if (PinIsSink) {
            RootNet.addSink(RootNetBitIndex, ThisSolderPoint);
          } else {
            RootNet.addSource(RootNetBitIndex, ThisSolderPoint);
          }
        }
      }
    }
    if (comp.getFactory() instanceof Clock) {
      MyClockGenerators.add(NormalComponent);
    } else if (comp.getFactory() instanceof Pin) {
      if (comp.getEnd(0).isInput()) {
        MyOutputPorts.add(NormalComponent);
      } else {
        MyInputPorts.add(NormalComponent);
      }
    } else {
      MyComponents.add(NormalComponent);
    }
    return true;
  }

  private boolean ProcessSubcircuit(Component comp) {
    NetlistComponent Subcircuit = new NetlistComponent(comp);
    SubcircuitFactory sub = (SubcircuitFactory) comp.getFactory();
    Instance[] subPins = ((CircuitAttributes) comp.getAttributeSet()).getPinInstances();
    Netlist subNetlist = sub.getSubcircuit().getNetList();
    for (EndData ThisPin : comp.getEnds()) {
      Net Connection = FindConnectedNet(ThisPin.getLocation());
      int PinId = comp.getEnds().indexOf(ThisPin);
      int SubPortIndex = subNetlist.GetPortInfo(subPins[PinId].getAttributeValue(StdAttr.LABEL));
      if (SubPortIndex < 0) {
        Reporter.Report.AddFatalError(
            "BUG:  Unable to find pin in sub-circuit\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return false;
      }
      if (Connection != null) {
        boolean PinIsSink = ThisPin.isInput();
        Net RootNet = GetRootNet(Connection);
        if (RootNet == null) {
          Reporter.Report.AddFatalError(
              "BUG:  Unable to find a root net for sub-circuit\n ==> "
                  + this.getClass().getName().replaceAll("\\.", "/")
                  + ":"
                  + Thread.currentThread().getStackTrace()[2].getLineNumber()
                  + "\n");
          return false;
        }
        for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
          byte RootNetBitIndex = GetRootNetIndex(Connection, bitid);
          if (RootNetBitIndex < 0) {
            Reporter.Report.AddFatalError(
                "BUG:  Unable to find a root-net bit-index for sub-circuit\n ==> "
                    + this.getClass().getName().replaceAll("\\.", "/")
                    + ":"
                    + Thread.currentThread().getStackTrace()[2].getLineNumber()
                    + "\n");
            return false;
          }
          Subcircuit.getEnd(PinId).GetConnection(bitid).SetParrentNet(RootNet, RootNetBitIndex);
          if (PinIsSink) {
            RootNet.addSink(RootNetBitIndex, Subcircuit.getEnd(PinId).GetConnection(bitid));
          } else {
            RootNet.addSource(RootNetBitIndex, Subcircuit.getEnd(PinId).GetConnection(bitid));
          }
          /*
           * Special handling for sub-circuits; we have to find out
           * the connection to the corresponding net in the underlying
           * net-list; At this point the underlying net-lists have
           * already been generated.
           */
          Subcircuit.getEnd(PinId).GetConnection(bitid).setChildsPortIndex(SubPortIndex);
        }
      } else {
        for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
          Subcircuit.getEnd(PinId).GetConnection(bitid).setChildsPortIndex(SubPortIndex);
        }
      }
    }
    MySubCircuits.add(Subcircuit);
    return true;
  }

  public String projName() {
    return MyCircuit.getProjName();
  }

  public boolean RequiresGlobalClockConnection() {
    return MyClockInformation.GetSourceContainer().RequiresFPGAGlobalClock();
  }

  public void SetCurrentHierarchyLevel(ArrayList<String> Level) {
    CurrentHierarchyLevel.clear();
    CurrentHierarchyLevel.addAll(Level);
  }
  
  private boolean TraceDownSubcircuit(
		  ConnectionPoint p,
		  int ClockSourceId,
	      ArrayList<String> HierarchyNames,
	      ArrayList<Netlist> HierarchyNetlists) {
    if (p.getChildsPortIndex() < 0) {
      Reporter.Report.AddFatalError(
          "BUG: Subcircuit port is not annotated!\n ==> "
              + this.getClass().getName().replaceAll("\\.", "/")
              + ":"
              + Thread.currentThread().getStackTrace()[2].getLineNumber()
              + "\n");
      return false;
    }
    SubcircuitFactory sub = (SubcircuitFactory) p.GetComp().getFactory();
    NetlistComponent InputPort = sub.getSubcircuit().getNetList().GetInputPin(p.getChildsPortIndex());
    if (InputPort == null) {
      Reporter.Report.AddFatalError(
          "BUG: Unable to find Subcircuit input port!\n ==> "
              + this.getClass().getName().replaceAll("\\.", "/")
              + ":"
              + Thread.currentThread().getStackTrace()[2].getLineNumber()
              + "\n");
      return false;
    }
    NetlistComponent subCirc = getSubCirc(p.GetComp());
    if (subCirc == null) {
      Reporter.Report.AddFatalError(
          "BUG: Unable to find Subcircuit!\n ==> "
              + this.getClass().getName().replaceAll("\\.", "/")
              + ":"
              + Thread.currentThread().getStackTrace()[2].getLineNumber()
              + "\n");
      return false;
    }
    byte BitIndex = subCirc.GetConnectionBitIndex(p.GetParrentNet(), p.GetParrentNetBitIndex());
    if (BitIndex < 0) {
      Reporter.Report.AddFatalError(
          "BUG: Unable to find the bit index of a Subcircuit input port!\n ==> "
              + this.getClass().getName().replaceAll("\\.", "/")
              + ":"
              + Thread.currentThread().getStackTrace()[2].getLineNumber()
              + "\n");
      return false;
    }
    ConnectionPoint SubClockNet = InputPort.getEnd(0).GetConnection(BitIndex);
    if (SubClockNet.GetParrentNet() != null) {
      /* we have a connected pin */
      ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
      String Label = CorrectLabel.getCorrectLabel(
              subCirc.GetComponent().getAttributeSet().getValue(StdAttr.LABEL));
      NewHierarchyNames.add(Label);
      ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
      NewHierarchyNetlists.add(sub.getSubcircuit().getNetList());
      sub.getSubcircuit().getNetList()
          .MarkClockNet(NewHierarchyNames, ClockSourceId, SubClockNet,true);
      return sub.getSubcircuit()
          .getNetList()
          .TraceClockNet(
              SubClockNet.GetParrentNet(),
              SubClockNet.GetParrentNetBitIndex(),
              ClockSourceId,
              true,
              NewHierarchyNames,
              NewHierarchyNetlists);
    }
    return true;
  }

  public boolean TraceClockNet(
      Net ClockNet,
      byte ClockNetBitIndex,
      int ClockSourceId,
      boolean isPinSource,
      ArrayList<String> HierarchyNames,
      ArrayList<Netlist> HierarchyNetlists) {
 	    ArrayList<ConnectionPoint> HiddenComps = GetHiddenSinks(ClockNet, ClockNetBitIndex, MyComplexSplitters,
         new HashSet<>(), false);
 	    for (ConnectionPoint p : HiddenComps) {
 	    MarkClockNet(HierarchyNames, ClockSourceId, p, isPinSource);
      if (p.GetComp().getFactory() instanceof SubcircuitFactory)
        if (!TraceDownSubcircuit(p,ClockSourceId,HierarchyNames,HierarchyNetlists))
        	return false;
      /* On top level we do not have to go up */
      if (HierarchyNames.isEmpty()) continue;
      if (p.GetComp().getFactory() instanceof Pin) {
        NetlistComponent OutputPort = getOutPort(p.GetComp());
        if (OutputPort == null) {
          Reporter.Report.AddFatalError(
              "BUG: Could not find an output port!\n ==> "
                  + this.getClass().getName().replaceAll("\\.", "/")
                  + ":"
                  + Thread.currentThread().getStackTrace()[2].getLineNumber()
                  + "\n");
          return false;
        }
        byte bitindex = OutputPort.GetConnectionBitIndex(p.GetParrentNet(), p.GetParrentNetBitIndex());
        ConnectionPoint SubClockNet =
            HierarchyNetlists.get(HierarchyNetlists.size() - 2)
                .GetNetlistConnectionForSubCircuit(
                    HierarchyNames.get(HierarchyNames.size() - 1),
                    MyOutputPorts.indexOf(OutputPort),
                    bitindex);
        if (SubClockNet == null) {
          Reporter.Report.AddFatalError(
              "BUG: Could not find a sub-circuit connection in overlying hierarchy level!\n ==> "
                  + this.getClass().getName().replaceAll("\\.", "/")
                  + ":"
                  + Thread.currentThread().getStackTrace()[2].getLineNumber()
                  + "\n");
          return false;
        }
        if (SubClockNet.GetParrentNet() == null) {
        } else {
          ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
          NewHierarchyNames.remove(NewHierarchyNames.size() - 1);
          ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
          NewHierarchyNetlists.remove(NewHierarchyNetlists.size() - 1);
          HierarchyNetlists.get(HierarchyNetlists.size() - 2)
              .MarkClockNet(NewHierarchyNames, ClockSourceId, SubClockNet,true);
          if (!HierarchyNetlists.get(HierarchyNetlists.size() - 2)
              .TraceClockNet(
                  SubClockNet.GetParrentNet(),
                  SubClockNet.GetParrentNetBitIndex(),
                  ClockSourceId,
                  true,
                  NewHierarchyNames,
                  NewHierarchyNetlists)) {
            return false;
          }
        }
      }
	}
    return true;
  }
  
  private NetlistComponent getSubCirc(Component comp) {
    for (NetlistComponent current : MySubCircuits)
      if (current.GetComponent().equals(comp))
        return current;
    return null;
  }
  
  private NetlistComponent getOutPort(Component comp) {
    for (NetlistComponent current : MyOutputPorts)
      if (current.GetComponent().equals(comp))
        return current;
    return null;
  }

  private boolean DetectGatedClocks() {
    /* First Pass: We gather a complete information tree about components with clock inputs and their connected nets in
     * case it is not a clock net. The moment we call this function the clock tree has been marked already !*/
    ArrayList<Netlist> root = new ArrayList<>();
    boolean suppress = AppPreferences.SupressGatedClockWarnings.getBoolean();
    root.add(this);
    Map<String, Map<NetlistComponent, Circuit>> NotGatedSet =
        new HashMap<>();
    Map<String, Map<NetlistComponent, Circuit>> GatedSet =
        new HashMap<>();
    SetCurrentHierarchyLevel(new ArrayList<>());
    GetGatedClockComponents(root, null, NotGatedSet, GatedSet, new HashSet<>());
    for (String key : NotGatedSet.keySet()) {
      if (GatedSet.containsKey(key) && !suppress) {
        /* big Problem, we have a component that is used with and without gated clocks */
        Reporter.Report.AddSevereWarning(S.get("NetList_CircuitGatedNotGated"));
        Reporter.Report.AddWarningIncrement(S.get("NetList_TraceListBegin"));
        Map<NetlistComponent, Circuit> instances = NotGatedSet.get(key);
        for (NetlistComponent comp : instances.keySet()) {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  instances.get(comp),
                  S.get("NetList_CircuitNotGated"),
                  SimpleDRCContainer.LEVEL_NORMAL,
                  SimpleDRCContainer.MARK_INSTANCE,
                  true);
          warn.AddMarkComponent(comp.GetComponent());
          Reporter.Report.AddWarning(warn);
        }
        instances = GatedSet.get(key);
        for (NetlistComponent comp : instances.keySet()) {
          comp.SetIsGatedInstance();
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  instances.get(comp),
                  S.get("NetList_CircuitGated"),
                  SimpleDRCContainer.LEVEL_NORMAL,
                  SimpleDRCContainer.MARK_INSTANCE,
                  true);
          warn.AddMarkComponent(comp.GetComponent());
          Reporter.Report.AddWarning(warn);
        }
        Reporter.Report.AddWarningIncrement(S.get("NetList_TraceListEnd"));
      }
    }
    return true;
  }

  public void GetGatedClockComponents(
      ArrayList<Netlist> HierarchyNetlists,
      NetlistComponent SubCircuit,
      Map<String, Map<NetlistComponent, Circuit>> NotGatedSet,
      Map<String, Map<NetlistComponent, Circuit>> GatedSet,
      Set<NetlistComponent> WarnedComponents) {
    /* First pass: we go down the tree */
    for (NetlistComponent SubCirc : MySubCircuits) {
      SubcircuitFactory sub = (SubcircuitFactory) SubCirc.GetComponent().getFactory();
      ArrayList<String> NewHierarchyNames = new ArrayList<>(GetCurrentHierarchyLevel());
      NewHierarchyNames.add(
          CorrectLabel.getCorrectLabel(
              SubCirc.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
      NewHierarchyNetlists.add(sub.getSubcircuit().getNetList());
      sub.getSubcircuit().getNetList().SetCurrentHierarchyLevel(NewHierarchyNames);
      sub.getSubcircuit()
          .getNetList()
          .GetGatedClockComponents(
              NewHierarchyNetlists, SubCirc, NotGatedSet, GatedSet, WarnedComponents);
    }
    /* Second pass: we find all components with a clock input and see if they are connected to a clock */
    boolean GatedClock = false;
    List<SourceInfo> PinSources = new ArrayList<>();
    List<Set<Wire>> PinWires = new ArrayList<>();
    List<Set<NetlistComponent>> PinGatedComponents = new ArrayList<>();
    List<SourceInfo> NonPinSources = new ArrayList<>();
    List<Set<Wire>> NonPinWires = new ArrayList<>();
    List<Set<NetlistComponent>> NonPinGatedComponents = new ArrayList<>();
    for (NetlistComponent comp : MyComponents) {
      ComponentFactory fact = comp.GetComponent().getFactory();
      if (fact.CheckForGatedClocks(comp)) {
        int[] clockpins = fact.ClockPinIndex(comp);
        for (int clockpin : clockpins)
          GatedClock |=
              HasGatedClock(
                  comp,
                  clockpin,
                  PinSources,
                  PinWires,
                  PinGatedComponents,
                  NonPinSources,
                  NonPinWires,
                  NonPinGatedComponents,
                  WarnedComponents);
      }
    }
    /* We have two situations:
     * 1) The gated clock net is generated locally, in this case we can mark them and add the current system to the non-gated set as
     *    each instance will be equal at higher/lower levels.
     * 2) The gated clock nets are connected to a pin, in this case each instance of this circuit could be either gated or non-gated,
     *    we have to do something on the level higher and we mark this in the sets to be processed later.
     */

    String MyName = CorrectLabel.getCorrectLabel(CircuitName);
    if (HierarchyNetlists.size() > 1) {
      if (GatedClock && PinSources.isEmpty()) {
        GatedClock = false; /* we have only non-pin driven gated clocks */
        WarningForGatedClock(
            NonPinSources,
            NonPinGatedComponents,
            NonPinWires,
            WarnedComponents,
            HierarchyNetlists,
            S.get("NetList_GatedClock"));
      }

      if (GatedClock && !PinSources.isEmpty()&&!AppPreferences.SupressGatedClockWarnings.getBoolean()) {
        for (int i = 0; i < PinSources.size(); i++) {
          Reporter.Report.AddSevereWarning(S.get("NetList_GatedClock"));
          Reporter.Report.AddWarningIncrement(S.get("NetList_TraceListBegin"));
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  MyCircuit,
                  S.get("NetList_GatedClockSink"),
                  SimpleDRCContainer.LEVEL_NORMAL,
                  SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_WIRE,
                  true);
          warn.AddMarkComponents(PinWires.get(i));
          for (NetlistComponent comp : PinGatedComponents.get(i))
            warn.AddMarkComponent(comp.GetComponent());
          Reporter.Report.AddWarning(warn);
          WarningTraceForGatedClock(
              PinSources.get(i).getSource(),
              PinSources.get(i).getIndex(),
              HierarchyNetlists,
              CurrentHierarchyLevel);
          Reporter.Report.AddWarningIncrement(S.get("NetList_TraceListEnd"));
        }
      }

      /* we only mark if we are not at top-level */
      if (GatedClock) {
        if (GatedSet.containsKey(MyName))
          GatedSet.get(MyName)
              .put(SubCircuit, HierarchyNetlists.get(HierarchyNetlists.size() - 2).getCircuit());
        else {
          Map<NetlistComponent, Circuit> newList = new HashMap<>();
          newList.put(SubCircuit, HierarchyNetlists.get(HierarchyNetlists.size() - 2).getCircuit());
          GatedSet.put(MyName, newList);
        }
      } else {
        if (NotGatedSet.containsKey(MyName))
          NotGatedSet.get(MyName)
              .put(SubCircuit, HierarchyNetlists.get(HierarchyNetlists.size() - 2).getCircuit());
        else {
          Map<NetlistComponent, Circuit> newList = new HashMap<>();
          newList.put(SubCircuit, HierarchyNetlists.get(HierarchyNetlists.size() - 2).getCircuit());
          NotGatedSet.put(MyName, newList);
        }
      }
    } else {
      /* At toplevel we warn for all possible gated clocks */
      WarningForGatedClock(
          NonPinSources,
          NonPinGatedComponents,
          NonPinWires,
          WarnedComponents,
          HierarchyNetlists,
          S.get("NetList_GatedClock"));
      WarningForGatedClock(
          PinSources,
          PinGatedComponents,
          PinWires,
          WarnedComponents,
          HierarchyNetlists,
          S.get("NetList_PossibleGatedClock"));
    }
  }

  private boolean HasGatedClock(
      NetlistComponent comp,
      int ClockPinIndex,
      List<SourceInfo> PinSources,
      List<Set<Wire>> PinWires,
      List<Set<NetlistComponent>> PinGatedComponents,
      List<SourceInfo> NonPinSources,
      List<Set<Wire>> NonPinWires,
      List<Set<NetlistComponent>> NonPinGatedComponents,
      Set<NetlistComponent> WarnedComponents) {
    boolean GatedClock = false;
    String ClockNetName = AbstractHDLGeneratorFactory.GetClockNetName(comp, ClockPinIndex, this);
    if (ClockNetName.isEmpty()) {
      /* we search for the source in case it is connected otherwise we ignore */
      ConnectionPoint connection = comp.getEnd(ClockPinIndex).GetConnection((byte) 0);
      Net connectedNet = connection.GetParrentNet();
      byte connectedNetindex = connection.GetParrentNetBitIndex();
      if (connectedNet != null) {
        GatedClock = true;
        Set<Wire> Segments = new HashSet<>();
        SourceInfo source =
            GetHiddenSource(
              	  null,
              	  (byte) 0,
                  connectedNet,
                  connectedNetindex,
                  MyComplexSplitters,
                new HashSet<>(),
                  Segments);
        ConnectionPoint sourceCon = source.getSource();
        if (sourceCon.GetComp().getFactory() instanceof Pin) {
          int index = IndexOfEntry(PinSources, sourceCon, (int) connectedNetindex);
          if (index < 0) {
            PinSources.add(source);
            PinWires.add(Segments);
            Set<NetlistComponent> comps = new HashSet<>();
            comps.add(comp);
            comps.add(new NetlistComponent(sourceCon.GetComp()));
            PinGatedComponents.add(comps);
          } else {
            PinGatedComponents.get(index).add(comp);
          }
        } else {
            int index = IndexOfEntry(NonPinSources, sourceCon, (int) connectedNetindex);
            if (index < 0) {
              NonPinSources.add(source);
              NonPinWires.add(Segments);
              Set<NetlistComponent> comps = new HashSet<>();
              comps.add(comp);
              NonPinGatedComponents.add(comps);
            } else {
              NonPinGatedComponents.get(index).add(comp);
            }
        }
      } else {
        /* Add severe warning, we found a sequential element with an unconnected clock input */
        if (!WarnedComponents.contains(comp)) {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  MyCircuit,
                  S.get("NetList_NoClockConnection"),
                  SimpleDRCContainer.LEVEL_SEVERE,
                  SimpleDRCContainer.MARK_INSTANCE);
          warn.AddMarkComponent(comp.GetComponent());
          Reporter.Report.AddWarning(warn);
          WarnedComponents.add(comp);
        }
      }
    }
    return GatedClock;
  }

  private int IndexOfEntry(List<SourceInfo> SearchList, ConnectionPoint Connection, Integer index) {
    int result = -1;
    for (int i = 0; i < SearchList.size(); i++) {
      SourceInfo thisEntry = SearchList.get(i);
      if (thisEntry.getSource().equals(Connection) && thisEntry.getIndex().equals(index))
        result = i;
    }
    return result;
  }

  private void WarningTraceForGatedClock(
      ConnectionPoint Source,
      int index,
      ArrayList<Netlist> HierarchyNetlists,
      ArrayList<String> HierarchyNames) {

    Component comp = Source.GetComp();
    if (comp.getFactory() instanceof Pin) {
      if (HierarchyNames.isEmpty())
        /* we cannot go up at toplevel, so leave */
        return;
      int idx = -1;
      for (int i = 0; i < MyInputPorts.size(); i++) {
        if (MyInputPorts.get(i).GetComponent().equals(comp)) idx = i;
      }
      if (idx < 0) {
        Reporter.Report.AddFatalError(
            "BUG: Could not find port!\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return;
      }
      ConnectionPoint SubNet =
          HierarchyNetlists.get(HierarchyNetlists.size() - 2)
              .GetNetlistConnectionForSubCircuitInput(
                  HierarchyNames.get(HierarchyNames.size() - 1), idx, (byte) index);
      if (SubNet == null) {
        Reporter.Report.AddFatalError(
            "BUG: Could not find a sub-circuit connection in overlying hierarchy level!\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return;
      }
      if (SubNet.GetParrentNet() != null) {
        ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
        NewHierarchyNames.remove(NewHierarchyNames.size() - 1);
        ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
        NewHierarchyNetlists.remove(NewHierarchyNetlists.size() - 1);
        Netlist SubNetList = HierarchyNetlists.get(HierarchyNetlists.size() - 2);
        Net NewNet = SubNet.GetParrentNet();
        Byte NewNetIndex = SubNet.GetParrentNetBitIndex();
        Set<Wire> Segments = new HashSet<>();
        SourceInfo source =
            SubNetList.GetHiddenSource(
                null,
                (byte) 0,
                NewNet,
                NewNetIndex,
                SubNetList.MyComplexSplitters,
                new HashSet<>(),
                Segments);
        if (source == null) {
          Reporter.Report.AddFatalError(
              "BUG: Unable to find source in sub-circuit!\n ==> "
                  + this.getClass().getName().replaceAll("\\.", "/")
                  + ":"
                  + Thread.currentThread().getStackTrace()[2].getLineNumber()
                  + "\n");
          return;
        }
        ComponentFactory sfac = source.getSource().GetComp().getFactory();
        if (sfac instanceof Pin || sfac instanceof SubcircuitFactory) {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  SubNetList.getCircuit(),
                  S.get("NetList_GatedClockInt"),
                  SimpleDRCContainer.LEVEL_NORMAL,
                  SimpleDRCContainer.MARK_WIRE,
                  true);
          warn.AddMarkComponents(Segments);
          Reporter.Report.AddWarning(warn);
          SubNetList.WarningTraceForGatedClock(
              source.getSource(),
              source.getIndex(),
              NewHierarchyNetlists,
              NewHierarchyNames);
        } else {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  SubNetList.getCircuit(),
                  S.get("NetList_GatedClockSource"),
                  SimpleDRCContainer.LEVEL_NORMAL,
                  SimpleDRCContainer.MARK_WIRE,
                  true);
          warn.AddMarkComponents(Segments);
          Reporter.Report.AddWarning(warn);
        }
      }
    }
    if (comp.getFactory() instanceof SubcircuitFactory) {
      SubcircuitFactory sub = (SubcircuitFactory) comp.getFactory();
      if (Source.getChildsPortIndex() < 0) {
        Reporter.Report.AddFatalError(
            "BUG: Subcircuit port is not annotated!\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return;
      }
      NetlistComponent OutputPort =
          sub.getSubcircuit().getNetList().GetOutputPin(Source.getChildsPortIndex());
      if (OutputPort == null) {
        Reporter.Report.AddFatalError(
            "BUG: Unable to find Subcircuit output port!\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return;
      }
      Net ConnectedNet = Source.GetParrentNet();
      /* Find the correct subcircuit */
      NetlistComponent SubCirc = null;
      for (NetlistComponent subc : MySubCircuits) {
        if (subc.GetComponent().equals(Source.GetComp())) SubCirc = subc;
      }
      if (SubCirc == null) {
        Reporter.Report.AddFatalError(
            "BUG: Unable to find Subcircuit!\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return;
      }
      byte BitIndex = SubCirc.GetConnectionBitIndex(ConnectedNet, (byte) index);
      if (BitIndex < 0) {
        Reporter.Report.AddFatalError(
            "BUG: Unable to find the bit index of a Subcircuit output port!\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return;
      }
      ConnectionPoint SubNet = OutputPort.getEnd(0).GetConnection(BitIndex);
      if (SubNet.GetParrentNet() != null) {
        /* we have a connected pin */
        Netlist SubNetList = sub.getSubcircuit().getNetList();
        ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
        NewHierarchyNames.add(
            CorrectLabel.getCorrectLabel(
                SubCirc.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
        NewHierarchyNetlists.add(SubNetList);
        Net NewNet = SubNet.GetParrentNet();
        Byte NewNetIndex = SubNet.GetParrentNetBitIndex();
        Set<Wire> Segments = new HashSet<>();
        SourceInfo source =
            SubNetList.GetHiddenSource(
            	null,
            	(byte) 0,
                NewNet,
                NewNetIndex,
                SubNetList.MyComplexSplitters,
                new HashSet<>(),
                Segments);
        if (source == null) {
          Reporter.Report.AddFatalError(
              "BUG: Unable to find source in sub-circuit!\n ==> "
                  + this.getClass().getName().replaceAll("\\.", "/")
                  + ":"
                  + Thread.currentThread().getStackTrace()[2].getLineNumber()
                  + "\n");
          return;
        }
        ComponentFactory sfac = source.getSource().GetComp().getFactory();
        if (sfac instanceof Pin || sfac instanceof SubcircuitFactory) {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  SubNetList.getCircuit(),
                  S.get("NetList_GatedClockInt"),
                  SimpleDRCContainer.LEVEL_NORMAL,
                  SimpleDRCContainer.MARK_WIRE,
                  true);
          warn.AddMarkComponents(Segments);
          Reporter.Report.AddWarning(warn);
          SubNetList.WarningTraceForGatedClock(
              source.getSource(),
              source.getIndex(),
              NewHierarchyNetlists,
              NewHierarchyNames);
        } else {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  SubNetList.getCircuit(),
                  S.get("NetList_GatedClockSource"),
                  SimpleDRCContainer.LEVEL_NORMAL,
                  SimpleDRCContainer.MARK_WIRE,
                  true);
          warn.AddMarkComponents(Segments);
          Reporter.Report.AddWarning(warn);
        }
      }
    }
  }

  private void WarningForGatedClock(
      List<SourceInfo> Sources,
      List<Set<NetlistComponent>> Components,
      List<Set<Wire>> Wires,
      Set<NetlistComponent> WarnedComponents,
      ArrayList<Netlist> HierarchyNetlists,
      String Warning) {
	if (AppPreferences.SupressGatedClockWarnings.getBoolean()) return;
    for (int i = 0; i < Sources.size(); i++) {
      boolean AlreadyWarned = false;
      for (NetlistComponent comp : Components.get(i))
        AlreadyWarned |= WarnedComponents.contains(comp);
      if (!AlreadyWarned) {
        if (Sources.get(i).getSource().GetComp().getFactory() instanceof SubcircuitFactory) {
          Reporter.Report.AddSevereWarning(S.get("NetList_GatedClock"));
          Reporter.Report.AddWarningIncrement(S.get("NetList_TraceListBegin"));
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  MyCircuit,
                  S.get("NetList_GatedClockSink"),
                  SimpleDRCContainer.LEVEL_NORMAL,
                  SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_WIRE,
                  true);
          warn.AddMarkComponents(Wires.get(i));
          for (NetlistComponent comp : Components.get(i))
            warn.AddMarkComponent(comp.GetComponent());
          Reporter.Report.AddWarning(warn);
          WarningTraceForGatedClock(
              Sources.get(i).getSource(),
              Sources.get(i).getIndex(),
              HierarchyNetlists,
              CurrentHierarchyLevel);
          Reporter.Report.AddWarningIncrement(S.get("NetList_TraceListEnd"));
        } else {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                  MyCircuit,
                  Warning,
                  SimpleDRCContainer.LEVEL_SEVERE,
                  SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_WIRE);
          for (NetlistComponent comp : Components.get(i))
            warn.AddMarkComponent(comp.GetComponent());
          warn.AddMarkComponents(Wires.get(i));
          Reporter.Report.AddWarning(warn);
        }
        WarnedComponents.addAll(Components.get(i));
      }
    }
  }

  public static boolean IsFlipFlop(AttributeSet attrs) {
    if (attrs.containsAttribute(StdAttr.EDGE_TRIGGER)) return true;
    if (attrs.containsAttribute(StdAttr.TRIGGER)) {
      return ((attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING)
          || (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING));
    }
    return false;
  }
}
