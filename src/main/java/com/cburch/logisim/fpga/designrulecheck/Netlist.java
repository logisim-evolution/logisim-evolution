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

public class Netlist implements CircuitListener {

  @Override
  public void circuitChanged(CircuitEvent event) {
    final var ev = event.getAction();
    if (event.getData() instanceof InstanceComponent) {
      final var inst = (InstanceComponent) event.getData();
      if (event.getCircuit().equals(myCircuit)) {
        switch (ev) {
          case CircuitEvent.ACTION_ADD:
            drcStatus = DRC_REQUIRED;
            if (inst.getFactory() instanceof SubcircuitFactory) {
              final var fac = (SubcircuitFactory) inst.getFactory();
              final var sub = fac.getSubcircuit();

              if (MySubCircuitMap.containsKey(sub)) {
                MySubCircuitMap.put(sub, MySubCircuitMap.get(sub) + 1);
              } else {
                MySubCircuitMap.put(sub, 1);
                sub.addCircuitListener(this);
              }
            }
            break;
          case CircuitEvent.ACTION_REMOVE:
            drcStatus = DRC_REQUIRED;
            if (inst.getFactory() instanceof SubcircuitFactory) {
              final var fac = (SubcircuitFactory) inst.getFactory();
              final var sub = fac.getSubcircuit();
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
            drcStatus = DRC_REQUIRED;
            break;
        }
      } else {
        if (inst.getFactory() instanceof Pin) {
          drcStatus = DRC_REQUIRED;
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

    private final Net theNet;
    private final byte bitIndex;

    public NetInfo(Net concernedNet, byte index) {
      theNet = concernedNet;
      bitIndex = index;
    }

    public Byte getIndex() {
      return bitIndex;
    }

    public Net getNet() {
      return theNet;
    }
  }

  private String circuitName;
  private final ArrayList<Net> myNets = new ArrayList<>();
  private final Map<Circuit, Integer> MySubCircuitMap = new HashMap<>();
  private final ArrayList<NetlistComponent> mySubCircuits = new ArrayList<>();
  private final ArrayList<NetlistComponent> myComponents = new ArrayList<>();
  private final ArrayList<NetlistComponent> myClockGenerators = new ArrayList<>();
  private final ArrayList<NetlistComponent> myInOutPorts = new ArrayList<>();
  private final ArrayList<NetlistComponent> myInputPorts = new ArrayList<>();
  private final ArrayList<NetlistComponent> myOutputPorts = new ArrayList<>();
  private final ArrayList<Component> myComplexSplitters = new ArrayList<>();
  private Integer localNrOfInportBubles;
  private Integer localNrOfOutportBubles;
  private Integer localNrOfInOutBubles;
  private final ClockTreeFactory myClockInformation = new ClockTreeFactory();
  private final Circuit myCircuit;
  private int drcStatus;
  private final Set<Wire> wires = new HashSet<>();
  private ArrayList<String> currentHierarchyLevel;
  public static final int DRC_REQUIRED = 4;
  public static final int DRC_PASSED = 0;
  public static final int ANNOTATE_REQUIRED = 1;
  public static final int DRC_ERROR = 2;

  public static final Color DRC_INSTANCE_MARK_COLOR = Color.RED;
  public static final Color DRC_LABEL_MARK_COLOR = Color.MAGENTA;
  public static final Color DRC_WIRE_MARK_COLOR = Color.RED;

  public Netlist(Circuit ThisCircuit) {
    myCircuit = ThisCircuit;
    this.clear();
  }

  public void cleanClockTree(ClockSourceContainer ClockSources) {
    /* First pass, we cleanup all old information */
    myClockInformation.clean();
    myClockInformation.setSourceContainer(ClockSources);
    /* Second pass, we go down the hierarchy */
    for (final var sub : mySubCircuits) {
      final var subFact = (SubcircuitFactory) sub.getComponent().getFactory();
      subFact.getSubcircuit().getNetList().cleanClockTree(ClockSources);
    }
  }

  public void clear() {
    for (final var subcirc : mySubCircuits) {
      final var subFact = (SubcircuitFactory) subcirc.getComponent().getFactory();
      subFact.getSubcircuit().getNetList().clear();
    }
    drcStatus = DRC_REQUIRED;
    myNets.clear();
    mySubCircuits.clear();
    myComponents.clear();
    myClockGenerators.clear();
    myInputPorts.clear();
    myInOutPorts.clear();
    myOutputPorts.clear();
    myComplexSplitters.clear();
    localNrOfInportBubles = 0;
    localNrOfOutportBubles = 0;
    localNrOfInOutBubles = 0;
    if (currentHierarchyLevel == null) {
      currentHierarchyLevel = new ArrayList<>();
    } else {
      currentHierarchyLevel.clear();
    }
  }

  public String getName() {
    if (myCircuit != null) return myCircuit.getName();
    else return "Unknown";
  }

  public void constructHierarchyTree(
      Set<String> processedCircuits,
      ArrayList<String> hierarchyName,
      Integer globalInputId,
      Integer globalOutputId,
      Integer globalInOutId) {
    if (processedCircuits == null) {
      processedCircuits = new HashSet<>();
    }
    /*
     * The first step is to go down to the leaves and visit all involved
     * sub-circuits to construct the local bubble information and form the
     * Mappable components tree
     */
    localNrOfInportBubles = 0;
    localNrOfOutportBubles = 0;
    localNrOfInOutBubles = 0;
    for (final var comp : mySubCircuits) {
      final var sub = (SubcircuitFactory) comp.getComponent().getFactory();
      final var myHierarchyName = new ArrayList<String>(hierarchyName);
      myHierarchyName.add(
          CorrectLabel.getCorrectLabel(
              comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      final var firstTime = !processedCircuits.contains(sub.getName());
      if (firstTime) {
        processedCircuits.add(sub.getName());
        sub.getSubcircuit()
            .getNetList()
            .constructHierarchyTree(
                processedCircuits, myHierarchyName, globalInputId, globalOutputId, globalInOutId);
      }
      final var subInputBubbles = sub.getSubcircuit().getNetList().NumberOfInputBubbles();
      final var subInOutBubbles = sub.getSubcircuit().getNetList().NumberOfInOutBubbles();
      final var subOutputBubbles = sub.getSubcircuit().getNetList().NumberOfOutputBubbles();
      comp.setLocalBubbleID(
              localNrOfInportBubles,
          subInputBubbles,
              localNrOfOutportBubles,
          subOutputBubbles,
              localNrOfInOutBubles,
          subInOutBubbles);
      localNrOfInportBubles += subInputBubbles;
      localNrOfInOutBubles += subInOutBubbles;
      localNrOfOutportBubles += subOutputBubbles;
      comp.addGlobalBubbleId(
          myHierarchyName,
          globalInputId,
          subInputBubbles,
          globalOutputId,
          subOutputBubbles,
          globalInOutId,
          subInOutBubbles);
      if (!firstTime) {
        sub.getSubcircuit()
            .getNetList()
            .enumerateGlobalBubbleTree(
                myHierarchyName, globalInputId, globalOutputId, globalInOutId);
      }
      globalInputId += subInputBubbles;
      globalInOutId += subInOutBubbles;
      globalOutputId += subOutputBubbles;
    }
    /*
     * Here we processed all sub-circuits of the local hierarchy level, now
     * we have to process the IO components
     */
    for (NetlistComponent comp : myComponents) {
      if (comp.getMapInformationContainer() != null) {
        final var myHierarchyName = new ArrayList<String>(hierarchyName);
        myHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        final var subInputBubbles = comp.getMapInformationContainer().GetNrOfInports();
        final var subInOutBubbles = comp.getMapInformationContainer().GetNrOfInOutports();
        final var subOutputBubbles = comp.getMapInformationContainer().GetNrOfOutports();
        comp.setLocalBubbleID(
            localNrOfInportBubles,
            subInputBubbles,
            localNrOfOutportBubles,
            subOutputBubbles,
            localNrOfInOutBubles,
            subInOutBubbles);
        localNrOfInportBubles += subInputBubbles;
        localNrOfInOutBubles += subInOutBubbles;
        localNrOfOutportBubles += subOutputBubbles;
        comp.addGlobalBubbleId(
            myHierarchyName,
            globalInputId,
            subInputBubbles,
            globalOutputId,
            subOutputBubbles,
            globalInOutId,
            subInOutBubbles);
        globalInputId += subInputBubbles;
        globalInOutId += subInOutBubbles;
        globalOutputId += subOutputBubbles;
      }
    }
  }

  public int designRuleCheckResult(boolean isTopLevel, ArrayList<String> sheetNames) {
    ArrayList<String> CompName = new ArrayList<>();
    Map<String, Component> Labels = new HashMap<>();
    ArrayList<SimpleDRCContainer> drc = new ArrayList<>();
    int CommonDRCStatus = DRC_PASSED;
    /* First we go down the tree and get the DRC status of all sub-circuits */
    for (Circuit circ : MySubCircuitMap.keySet()) {
      CommonDRCStatus |= circ.getNetList().designRuleCheckResult(false, sheetNames);
    }
    // Check if we are okay
    if (drcStatus == DRC_PASSED) {
      return CommonDRCStatus;
    } else {
      // There are changes, so we clean up the old information
      clear();
      drcStatus = DRC_PASSED;
      // we mark already passed, if an error * occurs the status is changed
    }
    /*
     * Check for duplicated sheet names, this is bad as we will have
     * multiple "different" components with the same name
     */
    if (myCircuit.getName().isEmpty()) {
      /*
       * in the current implementation of logisim this should never
       * happen, but we leave it in
       */
      Reporter.Report.AddFatalError(S.get("EmptyNamedSheet"));
      drcStatus |= DRC_ERROR;
    }
    if (sheetNames.contains(myCircuit.getName())) {
      /*
       * in the current implementation of logisim this should never
       * happen, but we leave it in
       */
      Reporter.Report.AddFatalError(S.get("MultipleSheetSameName", myCircuit.getName()));
      drcStatus |= DRC_ERROR;
    } else {
      sheetNames.add(myCircuit.getName());
    }
    /* Preparing stage */
    for (Component comp : myCircuit.getNonWires()) {
      String ComponentName = comp.getFactory().getHDLName(comp.getAttributeSet());
      if (!CompName.contains(ComponentName)) {
        CompName.add(ComponentName);
      }
    }
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("HDL_noLabel"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_INSTANCE));
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("HDL_CompNameIsLabel"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_LABEL));
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("HDL_LabelInvalid"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_LABEL));
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("HDL_DuplicatedLabels"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_LABEL));
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("HDL_Tristate"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_INSTANCE));
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("HDL_unsupported"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_INSTANCE));
    for (Component comp : myCircuit.getNonWires()) {
      /*
       * Here we check if the components are supported for the HDL
       * generation
       */
      if (!comp.getFactory().HDLSupportedComponent(comp.getAttributeSet())) {
        drc.get(5).addMarkComponent(comp);
        drcStatus |= DRC_ERROR;
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
          drc.get(0).addMarkComponent(comp);
          drcStatus |= ANNOTATE_REQUIRED;
        } else {
          if (CompName.contains(Label)) {
            drc.get(1).addMarkComponent(comp);
            drcStatus |= DRC_ERROR;
          }
          if (!CorrectLabel.isCorrectLabel(Label)) {
            /* this should not happen anymore */
            drc.get(2).addMarkComponent(comp);
            drcStatus |= DRC_ERROR;
          }
          if (Labels.containsKey(Label)) {
            drc.get(3).addMarkComponent(comp);
            drc.get(3).addMarkComponent(Labels.get(Label));
            drcStatus |= DRC_ERROR;
          } else {
            Labels.put(Label, comp);
          }
        }
        if (comp.getFactory() instanceof SubcircuitFactory) {
          /* Special care has to be taken for sub-circuits */
          if (Label.equals(ComponentName.toUpperCase())) {
            drc.get(1).addMarkComponent(comp);
            drcStatus |= DRC_ERROR;
          }
          if (!CorrectLabel.isCorrectLabel(
              comp.getFactory().getName(),
              S.get("FoundBadComponent", comp.getFactory().getName(), myCircuit.getName()))) {
            drcStatus |= DRC_ERROR;
          }
          SubcircuitFactory sub = (SubcircuitFactory) comp.getFactory();
          localNrOfInportBubles =
              localNrOfInportBubles + sub.getSubcircuit().getNetList().NumberOfInputBubbles();
          localNrOfOutportBubles =
              localNrOfOutportBubles + sub.getSubcircuit().getNetList().NumberOfOutputBubbles();
          localNrOfInOutBubles =
              localNrOfInOutBubles + sub.getSubcircuit().getNetList().NumberOfInOutBubbles();
        }
      }
      /* Now we check that no tri-state are present */
      if (comp.getFactory().HasThreeStateDrivers(comp.getAttributeSet())) {
        drc.get(4).addMarkComponent(comp);
        drcStatus |= DRC_ERROR;
      }
    }
    for (SimpleDRCContainer simpleDRCContainer : drc)
      if (simpleDRCContainer.isDrcInfoPresent())
        Reporter.Report.AddError(simpleDRCContainer);
    drc.clear();
    /* Here we have to quit as the netlist generation needs a clean tree */
    if ((drcStatus | CommonDRCStatus) != DRC_PASSED) {
      return drcStatus | CommonDRCStatus;
    }
    /*
     * Okay we now know for sure that all elements are supported, lets build
     * the net list
     */
    Reporter.Report.AddInfo(S.get("BuildingNetlistFor", myCircuit.getName()));
    if (!this.generateNetlist()) {
      this.clear();
      drcStatus = DRC_ERROR;
      /*
       * here we have to quit, as all the following steps depend on a
       * proper netlist
       */
      return drcStatus | CommonDRCStatus;
    }
    if (NetlistHasShortCircuits()) {
      clear();
      drcStatus = DRC_ERROR;
      return drcStatus | CommonDRCStatus;
    }
    /* Check for connections without a source */
    NetlistHasSinksWithoutSource();
    /* Check for unconnected input pins on components and generate warnings */
    for (NetlistComponent comp : myComponents) {
      boolean openInputs = false;
      for (int j = 0; j < comp.nrOfEnds(); j++) {
        if (comp.isEndInput(j) && !comp.isEndConnected(j)) openInputs = true;
      }
      if (openInputs && !AppPreferences.SupressOpenPinWarnings.get()) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                    myCircuit,
                    S.get("NetList_UnconnectedInputs"),
                    SimpleDRCContainer.LEVEL_NORMAL,
                    SimpleDRCContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp.getComponent());
        Reporter.Report.AddWarning(warn);
      }
    }
    /* Check for unconnected input pins on subcircuits and generate warnings */
    for (NetlistComponent comp : mySubCircuits) {
      boolean openInputs = false;
      for (int j = 0; j < comp.nrOfEnds(); j++) {
        if (comp.isEndInput(j) && !comp.isEndConnected(j)) openInputs = true;
      }
      if (openInputs && !AppPreferences.SupressOpenPinWarnings.get()) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                    myCircuit,
                    S.get("NetList_UnconnectedInputs"),
                    SimpleDRCContainer.LEVEL_SEVERE,
                    SimpleDRCContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp.getComponent());
        Reporter.Report.AddWarning(warn);
      }
    }
    /* Check for unconnected input pins in my circuit and generate warnings */
    for (NetlistComponent comp : myInputPorts) {
      boolean openInputs = false;
      for (int j = 0; j < comp.nrOfEnds(); j++) {
        if (!comp.isEndConnected(j)) openInputs = true;
      }
      if (openInputs && !AppPreferences.SupressOpenPinWarnings.get()) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                    myCircuit,
                    S.get("NetList_UnconnectedInput"),
                    SimpleDRCContainer.LEVEL_NORMAL,
                    SimpleDRCContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp.getComponent());
        Reporter.Report.AddWarning(warn);
      }
    }
    /* Check for unconnected output pins in my circuit and generate warnings */
    for (NetlistComponent comp : myOutputPorts) {
      boolean openOutputs = false;
      for (int j = 0; j < comp.nrOfEnds(); j++) {
        if (!comp.isEndConnected(j)) openOutputs = true;
      }
      if (openOutputs && !AppPreferences.SupressOpenPinWarnings.get()) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                    myCircuit,
                    S.get("NetList_UnconnectedOutput"),
                    SimpleDRCContainer.LEVEL_NORMAL,
                    SimpleDRCContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp.getComponent());
        Reporter.Report.AddWarning(warn);
      }
    }

    /* Only if we are on the top-level we are going to build the clock-tree */
    if (isTopLevel) {
      if (!detectClockTree()) {
        drcStatus = DRC_ERROR;
        return drcStatus | CommonDRCStatus;
      }
      constructHierarchyTree(null, new ArrayList<>(), 0, 0, 0);
      int ports =
          NumberOfInputPorts()
              + NumberOfOutputPorts()
              + localNrOfInportBubles
              + localNrOfOutportBubles
              + localNrOfInOutBubles;
      if (ports == 0) {
        Reporter.Report.AddFatalError(S.get("TopLevelNoIO", myCircuit.getName()));
        drcStatus = DRC_ERROR;
        return drcStatus | CommonDRCStatus;
      }
      /* Check for gated clocks */
      if (!detectGatedClocks()) {
        drcStatus = DRC_ERROR;
        return drcStatus | CommonDRCStatus;
      }
    }

    Reporter.Report.AddInfo(S.get("CircuitInfoString", myCircuit.getName(), numberOfNets(), numberOfBusses()));
    Reporter.Report.AddInfo(S.get("DRCPassesString", myCircuit.getName()));
    drcStatus = DRC_PASSED;
    return drcStatus | CommonDRCStatus;
  }

  private boolean detectClockTree() {
    /*
     * First pass, we remove all information of previously detected
     * clock-trees
     */
    final var clockSources = myClockInformation.getSourceContainer();
    cleanClockTree(clockSources);
    /* Second pass, we build the clock tree */
    final var hierarchyNetlists = new ArrayList<Netlist>();
    hierarchyNetlists.add(this);
    return markClockSourceComponents(new ArrayList<>(), hierarchyNetlists, clockSources);
  }

  /* Here all private handles are defined */
  private void enumerateGlobalBubbleTree(ArrayList<String> hierarchyname, int startInputID, int startOutputID, int startInOutID) {
    for (final var comp : mySubCircuits) {
      final var sub = (SubcircuitFactory) comp.getComponent().getFactory();
      final var myHierarchyName = new ArrayList<String>(hierarchyname);
      myHierarchyName.add(
          CorrectLabel.getCorrectLabel(
              comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      sub.getSubcircuit()
          .getNetList()
          .enumerateGlobalBubbleTree(
              myHierarchyName,
              startInputID + comp.getLocalBubbleInputStartId(),
              startOutputID + comp.getLocalBubbleOutputStartId(),
              startInOutID + comp.getLocalBubbleInOutStartId());
    }
    for (final var comp : myComponents) {
      if (comp.getMapInformationContainer() != null) {
        final var myHierarchyName = new ArrayList<String>(hierarchyname);
        myHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        int subInputBubbles = comp.getMapInformationContainer().GetNrOfInports();
        int subInOutBubbles = comp.getMapInformationContainer().GetNrOfInOutports();
        int subOutputBubbles = comp.getMapInformationContainer().GetNrOfOutports();
        comp.addGlobalBubbleId(
            myHierarchyName,
            startInputID + comp.getLocalBubbleInputStartId(),
            subInputBubbles,
            startOutputID + comp.getLocalBubbleOutputStartId(),
            subOutputBubbles,
            startInOutID,
            subInOutBubbles);
      }
    }
  }

  private Net findConnectedNet(Location loc) {
    for (final var current : myNets) {
      if (current.contains(loc)) {
        return current;
      }
    }
    return null;
  }

  private boolean generateNetlist() {
    final var drc = new ArrayList<SimpleDRCContainer>();
    var errors = false;
    circuitName = myCircuit.getName();
    final var progress = Reporter.Report.getProgressBar();
    var curMax = 0;
    var curVal = 0;
    var curStr = "";
    if (progress != null) {
      curMax = progress.getMaximum();
      curVal = progress.getValue();
      curStr = progress.getString();
      progress.setMaximum(7);
      progress.setString(S.get("NetListBuild", circuitName, 1));
    }

    wires.clear();
    wires.addAll(myCircuit.getWires());
    /*
     * FIRST PASS: In this pass we take all wire segments and see if they
     * are connected to other segments. If they are connected we build a
     * net.
     */
    while (wires.size() != 0) {
      final var newNet = new Net();
      getNet(null, newNet);
      if (!newNet.isEmpty()) {
        myNets.add(newNet);
      }
    }
    /*
     * Here we start to detect direct input-output component connections,
     * read we detect "hidden" nets
     */
    final var components = myCircuit.getNonWires();
    /* we Start with the creation of an outputs list */
    final var outputsList = new HashSet<Location>();
    final var inputsList = new HashSet<Location>();
    final var tunnelList = new HashSet<Component>();
    myComplexSplitters.clear();
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("NetList_IOError"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_INSTANCE));
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("NetList_BitwidthError"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_WIRE));
    for (final var comp : components) {
      /*
       * We do not process the splitter and tunnel, they are processed
       * later on
       */
      boolean ignore = false;

      /* In this case, the probe should not be synthetised:
       * We could set the Probe as non-HDL element. But If we set the Probe
       * as non HDL element, logisim will not allow user to download the design.
       *
       * In some case we need to use Logisim Simulation before running the design on the hardware.
       * During simulation, probes are very helpful to see signals values. And when simulation is
       * ok, the user does not want to delete all probes.
       * Thus, here we remove it form the netlist so it is transparent.
       */
      if (comp.getFactory() instanceof Probe) {
        continue;
      }

      if (comp.getFactory() instanceof SplitterFactory) {
        myComplexSplitters.add(comp);
        ignore = true;
      }
      if (comp.getFactory() instanceof Tunnel) {
        tunnelList.add(comp);
        ignore = true;
      }

      final var ends = comp.getEnds();
      for (final var end : ends) {
        if (!ignore) {
          if (end.isInput() && end.isOutput()) {
            /* The IO Port can be either output or input */
          //            if (!(com.getFactory() instanceof PortIO)) {
          //              drc.get(0).AddMarkComponent(com);
          //            }
          } else if (end.isOutput()) {
            outputsList.add(end.getLocation());
          } else {
            inputsList.add(end.getLocation());
          }
        }
        /* Here we are going to mark the bitwidths on the nets */
        final var width = end.getWidth().getWidth();
        final var loc = end.getLocation();
        // Collection<Component> component_verify = MyCircuit.getAllContaining(loc);
        for (final var thisNet : myNets) {
          if (thisNet.contains(loc)) {
            if (!thisNet.setWidth(width)) {
              drc.get(1).addMarkComponents(thisNet.getWires());
            }
          }
        }
      }
    }
    for (final var simpleDRCContainer : drc) {
      if (simpleDRCContainer.isDrcInfoPresent()) {
        errors = true;
        Reporter.Report.AddError(simpleDRCContainer);
      }
    }
    if (errors) {
      return false;
    }
    if (progress != null) {
      progress.setValue(1);
      progress.setString(S.get("NetListBuild", circuitName, 2));
    }
    /*
     * Now we check if an input pin is connected to an output and in case of
     * a Splitter if it is connected to either of them
     */
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("NetAdd_ComponentWidthMismatch"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_INSTANCE));
    final var points = new HashMap<Location, Integer>();
    for (final var comp : components) {
      for (final var end : comp.getEnds()) {
        final var loc = end.getLocation();
        if (points.containsKey(loc)) {
          /* Found a connection already used */
          var newNet = true;
          for (final var net : myNets) {
            if (net.contains(loc)) newNet = false;
          }
          if (newNet) {
            final var bitWidth = points.get(loc);
            if (bitWidth == end.getWidth().getWidth()) {
              myNets.add(new Net(loc, bitWidth));
            } else {
              drc.get(0).addMarkComponent(comp);
            }
          }
        } else points.put(loc, end.getWidth().getWidth());
      }
    }
    if (drc.get(0).isDrcInfoPresent()) {
      Reporter.Report.AddError(drc.get(0));
      return false;
    }

    if (progress != null) {
      progress.setValue(2);
      progress.setString(S.get("NetListBuild", circuitName, 3));
    }
    /*
     * Here we are going to process the tunnels and possible merging of the
     * tunneled nets
     */
    var TunnelsPresent = false;
    for (final var comp : tunnelList) {
      List<EndData> ends = comp.getEnds();
      for (EndData end : ends) {
        for (Net ThisNet : myNets) {
          if (ThisNet.contains(end.getLocation())) {
            ThisNet.addTunnel(comp.getAttributeSet().getValue(StdAttr.LABEL));
            TunnelsPresent = true;
          }
        }
      }
    }
    drc.clear();
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("NetMerge_BitWidthError"),
                SimpleDRCContainer.LEVEL_FATAL,
                SimpleDRCContainer.MARK_WIRE));
    if (TunnelsPresent) {
      Iterator<Net> NetIterator = myNets.listIterator();
      while (NetIterator.hasNext()) {
        Net ThisNet = NetIterator.next();
        if (ThisNet.hasTunnel() && (myNets.indexOf(ThisNet) < (myNets.size() - 1))) {
          boolean merged = false;
          Iterator<Net> SearchIterator = myNets.listIterator(myNets.indexOf(ThisNet) + 1);
          while (SearchIterator.hasNext() && !merged) {
            Net SearchNet = SearchIterator.next();
            for (String name : ThisNet.getTunnelNames()) {
              if (SearchNet.ContainsTunnel(name) && !merged) {
                merged = true;
                if (!SearchNet.merge(ThisNet)) {
                  drc.get(0).addMarkComponents(SearchNet.getWires());
                  drc.get(0).addMarkComponents(ThisNet.getWires());
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
    if (drc.get(0).isDrcInfoPresent()) {
      Reporter.Report.AddError(drc.get(0));
      return false;
    }
    if (progress != null) {
      progress.setValue(3);
      progress.setString(S.get("NetListBuild", circuitName, 4));
    }

    /* At this point all net segments are build. All tunnels have been removed.
     * There is still the processing of the splitters and the determination of
     * the direction of the nets.
     */

    /* First we are going to check on duplicated splitters and remove them */
    Iterator<Component> MySplitIterator = myComplexSplitters.listIterator();
    while (MySplitIterator.hasNext()) {
      Component ThisSplitter = MySplitIterator.next();
      if (myComplexSplitters.indexOf(ThisSplitter) < (myComplexSplitters.size() - 1)) {
        boolean FoundDuplicate = false;
        Iterator<Component> SearchIterator =
            myComplexSplitters.listIterator(myComplexSplitters.indexOf(ThisSplitter) + 1);
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
                      myCircuit,
                      S.get("NetList_duplicatedSplitter"),
                      SimpleDRCContainer.LEVEL_SEVERE,
                      SimpleDRCContainer.MARK_INSTANCE);
          warn.addMarkComponent(ThisSplitter);
          Reporter.Report.AddWarning(warn);
          MySplitIterator.remove();
        }
      }
    }

    // In this round we are going to detect the unconnected nets meaning those having a width of 0
    // and remove them
    drc.clear();
    Iterator<Net> NetIterator = myNets.listIterator();
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
                S.get("NetList_emptynets"),
                SimpleDRCContainer.LEVEL_NORMAL,
                SimpleDRCContainer.MARK_WIRE));
    while (NetIterator.hasNext()) {
      Net wire = NetIterator.next();
      if (wire.bitWidth() == 0) {
        drc.get(0).addMarkComponents(wire.getWires());
        NetIterator.remove();
      }
    }
    if (drc.get(0).isDrcInfoPresent()) {
      Reporter.Report.AddWarning(drc.get(0));
    }
    MySplitIterator = myComplexSplitters.iterator();
    // We also check quickly the splitters and remove the ones where input-bus is output-bus. We
    // mark those who are not correctly connected and remove both versions from the set.
    drc.clear();
    drc.add(
        new SimpleDRCContainer(
                myCircuit,
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
        for (Net CurrentNet : myNets) {
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
              myNets.remove(connectedNet);
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
                      myCircuit,
                      S.get("NetList_NoSplitterConnection"),
                      SimpleDRCContainer.LEVEL_SEVERE,
                      SimpleDRCContainer.MARK_INSTANCE);
          warn.addMarkComponent(mySplitter);
          Reporter.Report.AddWarning(warn);
        }
        MySplitIterator.remove(); /* Does not exist anymore */
      }
    }

    if (progress != null) {
      progress.setValue(4);
      progress.setString(S.get("NetListBuild", circuitName, 5));
    }
    /*
     * Finally we have to process the splitters to determine the bus
     * hierarchy (if any)
     */
    /*
     * In this round we only process the evident splitters and remove them
     * from the list
     */
    for (Component com : myComplexSplitters) {
      /*
       * Currently by definition end(0) is the combined end of the
       * splitter
       */
      List<EndData> ends = com.getEnds();
      EndData CombinedEnd = ends.get(0);
      int RootNet = -1;
      /* We search for the root net in the list of nets */
      for (int i = 0; i < myNets.size() && RootNet < 0; i++) {
        if (myNets.get(i).contains(CombinedEnd.getLocation())) {
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
        for (int j = 0; j < myNets.size() && ConnectedNet < 1; j++) {
          if (myNets.get(j).contains(ThisEnd.getLocation())) {
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
          if (!myNets.get(ConnectedNet).setParent(myNets.get(RootNet))) {
            myNets.get(ConnectedNet).ForceRootNet();
          }
          /* Here we have to process the inherited bits of the parent */
          byte[] BusBitConnection = ((Splitter) com).GetEndpoints();
          for (byte b = 0; b < BusBitConnection.length; b++) {
            if (BusBitConnection[b] == i) {
              myNets.get(ConnectedNet).AddParentBit(b);
            }
          }
        } else {
          unconnectedEnds = true;
        }
      }
      if (unconnectedEnds) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                    myCircuit,
                    S.get("NetList_NoSplitterEndConnections"),
                    SimpleDRCContainer.LEVEL_NORMAL,
                    SimpleDRCContainer.MARK_INSTANCE);
        warn.addMarkComponent(com);
        Reporter.Report.AddWarning(warn);
      }
      if (connectedUnknownEnds) {
        SimpleDRCContainer warn =
            new SimpleDRCContainer(
                    myCircuit,
                    S.get("NetList_NoEndSplitterConnections"),
                    SimpleDRCContainer.LEVEL_SEVERE,
                    SimpleDRCContainer.MARK_INSTANCE);
        warn.addMarkComponent(com);
        Reporter.Report.AddWarning(warn);
      }
    }
    if (progress != null) {
      progress.setValue(5);
      progress.setString(S.get("NetListBuild", circuitName, 6));
    }
    /*
     * Now the complete netlist is created, we have to check that each
     * net/bus entry has only 1 source and 1 or more sinks. If there exist
     * more than 1 source we have a short circuit! We keep track of the
     * sources and sinks at the root nets/buses
     */
    for (Net ThisNet : myNets) {
      if (ThisNet.isRootNet()) {
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
      progress.setString(S.get("NetListBuild", circuitName, 7));
    }

    /*
     * Here we are going to process the complex splitters, note that in the
     * previous handling of the splitters we marked all nets connected to a
     * complex splitter with a forcerootnet annotation; we are going to
     * cycle trough all these nets
     */
    for (Net thisnet : myNets) {
      if (thisnet.isForcedRootNet()) {
        /* Cycle through all the bits of this net */
        for (int bit = 0; bit < thisnet.bitWidth(); bit++) {
          for (Component comp : myComplexSplitters) {
            /*
             * Currently by definition end(0) is the combined end of
             * the splitter
             */
            List<EndData> ends = comp.getEnds();
            EndData CombinedEnd = ends.get(0);
            int ConnectedBus = -1;
            SplitterAttributes sattrs = (SplitterAttributes) comp.getAttributeSet();
            /* We search for the root net in the list of nets */
            for (int i = 0; i < myNets.size() && ConnectedBus < 0; i++) {
              if (myNets.get(i).contains(CombinedEnd.getLocation())) {
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
                Net Rootbus = myNets.get(ConnectedBus);
                while (!Rootbus.isRootNet()) {
                  ConnectedBusIndex = Rootbus.getBit(ConnectedBusIndex);
                  Rootbus = Rootbus.getParent();
                }
                ConnectionPoint SolderPoint = new ConnectionPoint(comp);
                SolderPoint.setParentNet(Rootbus, ConnectedBusIndex);
                boolean IsSink = true;
                if (!thisnet.hasBitSource(bit)) {
                  if (HasHiddenSource(
                      thisnet,
                      (byte) bit,
                      Rootbus,
                      ConnectedBusIndex,
                          myComplexSplitters,
                      new HashSet<String>(),
                      comp)) {
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
    return myClockInformation.getSourceContainer().getSources();
  }

  public ArrayList<Net> GetAllNets() {
    return myNets;
  }

  public Circuit getCircuit() {
    return myCircuit;
  }

  public String getCircuitName() {
    return circuitName;
  }

  public int GetClockSourceId(ArrayList<String> HierarchyLevel, Net WhichNet, Byte Bitid) {
    return myClockInformation.getClockSourceId(HierarchyLevel, WhichNet, Bitid);
  }

  public int GetClockSourceId(Component comp) {
    return myClockInformation.getClockSourceId(comp);
  }

  public ArrayList<NetlistComponent> GetClockSources() {
    return myClockGenerators;
  }

  public ArrayList<String> GetCurrentHierarchyLevel() {
    return currentHierarchyLevel;
  }

  public int GetEndIndex(NetlistComponent comp, String PinLabel, boolean IsOutputPort) {
    String label = CorrectLabel.getCorrectLabel(PinLabel);
    SubcircuitFactory sub = (SubcircuitFactory) comp.getComponent().getFactory();
    for (int end = 0; end < comp.nrOfEnds(); end++) {
      if (comp.getEnd(end).isOutputEnd() == IsOutputPort) {
        if (comp.getEnd(end).get((byte) 0).getChildsPortIndex()
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
    String NetId = myNets.indexOf(thisNet) + "-" + bitIndex;
    if (HandledNets.contains(NetId)) {
      return result;
    } else {
      HandledNets.add(NetId);
    }
    if (thisNet.hasBitSinks(bitIndex) && !isSourceNet && thisNet.isRootNet()) {
      result.addAll(thisNet.getBitSinks(bitIndex));
    }
    /* Check if we have a connection to another splitter */
    for (Component currentSplitter : SplitterList) {
      List<EndData> ends = currentSplitter.getEnds();
      SplitterAttributes sattrs = (SplitterAttributes) currentSplitter.getAttributeSet();
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
            for (Net thisnet : myNets) {
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
            for (Net thisnet : myNets) {
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
    if ((index < 0) || (index >= myInOutPorts.size())) {
      return null;
    }
    return myInOutPorts.get(index);
  }

  public NetlistComponent GetInOutPort(int Index) {
    if ((Index < 0) || (Index >= myInOutPorts.size())) {
      return null;
    }
    return myInOutPorts.get(Index);
  }

  public NetlistComponent GetInputPin(int index) {
    if ((index < 0) || (index >= myInputPorts.size())) {
      return null;
    }
    return myInputPorts.get(index);
  }

  public NetlistComponent GetInputPort(int Index) {
    if ((Index < 0) || (Index >= myInputPorts.size())) {
      return null;
    }
    return myInputPorts.get(Index);
  }

  public Map<ArrayList<String>, NetlistComponent> GetMappableResources(
      ArrayList<String> Hierarchy, boolean toplevel) {
    Map<ArrayList<String>, NetlistComponent> Components =
        new HashMap<>();
    /* First we search through my sub-circuits and add those IO components */
    for (NetlistComponent comp : mySubCircuits) {
      SubcircuitFactory sub = (SubcircuitFactory) comp.getComponent().getFactory();
      ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
      MyHierarchyName.add(
          CorrectLabel.getCorrectLabel(
              comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      Components.putAll(
          sub.getSubcircuit().getNetList().GetMappableResources(MyHierarchyName, false));
    }
    /* Now we search for all local IO components */
    for (NetlistComponent comp : myComponents) {
      if (comp.getMapInformationContainer() != null) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        Components.put(MyHierarchyName, comp);
      }
    }
    /* On the toplevel we have to add the pins */
    if (toplevel) {
      for (NetlistComponent comp : myInputPorts) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        Components.put(MyHierarchyName, comp);
      }
      for (NetlistComponent comp : myInOutPorts) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        Components.put(MyHierarchyName, comp);
      }
      for (NetlistComponent comp : myOutputPorts) {
        ArrayList<String> MyHierarchyName = new ArrayList<>(Hierarchy);
        MyHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        Components.put(MyHierarchyName, comp);
      }
    }
    return Components;
  }

  private void getNet(Wire wire, Net ThisNet) {
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
    for (Wire matched : MatchedWires) getNet(matched, ThisNet);
    MatchedWires.clear();
  }

  public Integer GetNetId(Net selectedNet) {
    return myNets.indexOf(selectedNet);
  }

  public ConnectionPoint GetNetlistConnectionForSubCircuit(
      String Label, int PortIndex, byte bitindex) {
    for (NetlistComponent search : mySubCircuits) {
      String CircuitLabel =
          CorrectLabel.getCorrectLabel(
              search.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (CircuitLabel.equals(Label)) {
        /* Found the component, let's search the ends */
        for (int i = 0; i < search.nrOfEnds(); i++) {
          ConnectionEnd ThisEnd = search.getEnd(i);
          if (ThisEnd.isOutputEnd() && (bitindex < ThisEnd.nrOfBits())) {
            if (ThisEnd.get(bitindex).getChildsPortIndex() == PortIndex) {
              return ThisEnd.get(bitindex);
            }
          }
        }
      }
    }
    return null;
  }

  public ConnectionPoint GetNetlistConnectionForSubCircuitInput(
      String Label, int PortIndex, byte bitindex) {
    for (NetlistComponent search : mySubCircuits) {
      String CircuitLabel =
          CorrectLabel.getCorrectLabel(
              search.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (CircuitLabel.equals(Label)) {
        /* Found the component, let's search the ends */
        for (int i = 0; i < search.nrOfEnds(); i++) {
          ConnectionEnd ThisEnd = search.getEnd(i);
          if (!ThisEnd.isOutputEnd() && (bitindex < ThisEnd.nrOfBits())) {
            if (ThisEnd.get(bitindex).getChildsPortIndex() == PortIndex) {
              return ThisEnd.get(bitindex);
            }
          }
        }
      }
    }
    return null;
  }

  public ArrayList<NetlistComponent> GetNormalComponents() {
    return myComponents;
  }

  public NetlistComponent GetOutputPin(int index) {
    if ((index < 0) || (index >= myOutputPorts.size())) {
      return null;
    }
    return myOutputPorts.get(index);
  }

  public int GetPortInfo(String Label) {
    String Source = CorrectLabel.getCorrectLabel(Label);
    for (NetlistComponent Inport : myInputPorts) {
      String Comp =
          CorrectLabel.getCorrectLabel(
              Inport.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (Comp.equals(Source)) {
        return myInputPorts.indexOf(Inport);
      }
    }
    for (NetlistComponent InOutport : myInOutPorts) {
      String Comp =
          CorrectLabel.getCorrectLabel(
              InOutport.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (Comp.equals(Source)) {
        return myInOutPorts.indexOf(InOutport);
      }
    }
    for (NetlistComponent Outport : myOutputPorts) {
      String Comp =
          CorrectLabel.getCorrectLabel(
              Outport.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (Comp.equals(Source)) {
        return myOutputPorts.indexOf(Outport);
      }
    }
    return -1;
  }

  private Net GetRootNet(Net Child) {
    if (Child == null) {
      return null;
    }
    if (Child.isRootNet()) {
      return Child;
    }
    Net RootNet = Child.getParent();
    while (!RootNet.isRootNet()) {
      RootNet = RootNet.getParent();
    }
    return RootNet;
  }

  private byte GetRootNetIndex(Net Child, byte BitIndex) {
    if (Child == null) {
      return -1;
    }
    if ((BitIndex < 0) || (BitIndex > Child.bitWidth())) {
      return -1;
    }
    if (Child.isRootNet()) {
      return BitIndex;
    }
    Net RootNet = Child.getParent();
    byte RootIndex = Child.getBit(BitIndex);
    while (!RootNet.isRootNet()) {
      RootIndex = RootNet.getBit(RootIndex);
      RootNet = RootNet.getParent();
    }
    return RootIndex;
  }

  public Set<Splitter> getSplitters() {
    /* This may be cause bugs due to dual splitter on same location situations */
    Set<Splitter> SplitterList = new HashSet<>();
    for (Component comp : myCircuit.getNonWires()) {
      if (comp.getFactory() instanceof SplitterFactory) {
        SplitterList.add((Splitter) comp);
      }
    }
    return SplitterList;
  }

  public ArrayList<NetlistComponent> GetSubCircuits() {
    return mySubCircuits;
  }

  private SourceInfo GetHiddenSource(
      Net sourceNet,
      Byte sourceBitIndex,
      Net thisNet,
      Byte bitIndex,
      List<Component> SplitterList,
      Set<String> HandledNets,
      Set<Wire> Segments,
      Component ignoreSplitter) {
    /* If the source net not is null add it to the set of visited nets to
     * prevent back-search on this net
     */
    if (sourceNet != null) {
      String NetId = myNets.indexOf(sourceNet) + "-" + sourceBitIndex;
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
    String NetId = myNets.indexOf(thisNet) + "-" + bitIndex;
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
      if (currentSplitter.equals(ignoreSplitter)) continue;
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
            for (Net thisnet : myNets) {
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
                      Segments,
                      currentSplitter);
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
            for (Net thisnet : myNets) {
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
                      Segments,
                      currentSplitter);
              if (ret != null) return ret;
            }
          }
        }
      }
    }
    return null;
  }

  private boolean HasHiddenSource(
      Net fannoutNet,
      Byte fannoutBitIndex,
      Net combinedNet,
      Byte combinedBitIndex,
      List<Component> splitterList,
      Set<String> handledNets,
      Component ignoreSplitter) {
    /* If the fannout net not is null add it to the set of visited nets to
     * prevent back-search on this net
     */
    if (fannoutNet != null) {
      final var NetId = myNets.indexOf(fannoutNet) + "-" + fannoutBitIndex;
      if (handledNets.contains(NetId)) {
        return false;
      } else {
        handledNets.add(NetId);
      }
    }
    /*
     * to prevent deadlock situations we check if we already looked at this
     * net
     */
    final var NetId = myNets.indexOf(combinedNet) + "-" + combinedBitIndex;
    if (handledNets.contains(NetId)) {
      return false;
    } else {
      handledNets.add(NetId);
    }
    if (combinedNet.hasBitSource(combinedBitIndex)) {
      return true;
    }
    /* Check if we have a connection to another splitter */
    for (var currentSplitter : splitterList) {
      if (currentSplitter.equals(ignoreSplitter)) continue;
      final var ends = currentSplitter.getEnds();
      for (var end = 0; end < ends.size(); end++) {
        if (combinedNet.contains(ends.get(end).getLocation())) {
          /* Here we have to process the inherited bits of the parent */
          byte[] BusBitConnection = ((Splitter) currentSplitter).GetEndpoints();
          if (end == 0) {
            /* this is a main net, find the connected end */
            var SplitterEnd = BusBitConnection[combinedBitIndex];
            /* Find the corresponding Net index */
            Byte Netindex = 0;
            for (var index = 0; index < combinedBitIndex; index++) {
              if (BusBitConnection[index] == SplitterEnd) {
                Netindex++;
              }
            }
            /* Find the connected Net */
            Net SlaveNet = null;
            for (Net thisnet : myNets) {
              if (thisnet.contains(ends.get(SplitterEnd).getLocation())) {
                SlaveNet = thisnet;
              }
            }
            if (SlaveNet != null) {
              if (HasHiddenSource(null, (byte) 0, SlaveNet, Netindex, splitterList, handledNets, currentSplitter)) {
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
            for (Net thisnet : myNets) {
              if (thisnet.contains(currentSplitter.getEnd(0).getLocation())) {
                RootNet = thisnet;
              }
            }
            if (RootNet != null) {
              if (HasHiddenSource(
                  null, (byte) 0, RootNet, Rootindices.get(combinedBitIndex), splitterList, handledNets, currentSplitter)) {
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
    if ((EndIndex < 0) || (EndIndex >= comp.nrOfEnds())) {
      return true;
    }
    ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
    int NrOfBits = ConnectionInformation.nrOfBits();
    if (NrOfBits == 1) {
      return true;
    }
    Net ConnectedNet = ConnectionInformation.get((byte) 0).getParentNet();
    byte ConnectedNetIndex = ConnectionInformation.get((byte) 0).getParentNetBitIndex();
    for (int i = 1; (i < NrOfBits) && ContinuesBus; i++) {
      if (ConnectedNet != ConnectionInformation.get((byte) i).getParentNet()) {
        /* This bit is connected to another bus */
        ContinuesBus = false;
      }
      if ((ConnectedNetIndex + 1)
          != ConnectionInformation.get((byte) i).getParentNetBitIndex()) {
        /* Connected to a none incremental position of the bus */
        ContinuesBus = false;
      } else {
        ConnectedNetIndex++;
      }
    }
    return ContinuesBus;
  }

  public boolean IsValid() {
    return drcStatus == DRC_PASSED;
  }

  public void MarkClockNet(ArrayList<String> HierarchyNames, int clocksourceid,
        ConnectionPoint connection, boolean isPinClockSource) {
    myClockInformation.addClockNet(HierarchyNames, clocksourceid, connection, isPinClockSource);
  }

  public boolean markClockSourceComponents(
      ArrayList<String> HierarchyNames,
      ArrayList<Netlist> HierarchyNetlists,
      ClockSourceContainer ClockSources) {
    /* First pass: we go down the hierarchy till the leaves */
    for (NetlistComponent sub : mySubCircuits) {
      SubcircuitFactory SubFact = (SubcircuitFactory) sub.getComponent().getFactory();
      ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
      NewHierarchyNames.add(
          CorrectLabel.getCorrectLabel(
              sub.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
      NewHierarchyNetlists.add(SubFact.getSubcircuit().getNetList());
      if (!SubFact.getSubcircuit()
          .getNetList()
          .markClockSourceComponents(NewHierarchyNames, NewHierarchyNetlists, ClockSources)) {
        return false;
      }
    }
    /*
     * We see if some components require the Global fast FPGA
     * clock
     */
    for (Component comp : myCircuit.getNonWires()) {
      if (comp.getFactory().RequiresGlobalClock()) {
        ClockSources.setRequiresFpgaGlobalClock();
      }
    }
    /* Second pass: We mark all clock sources */
    for (NetlistComponent ClockSource : myClockGenerators) {
      if (ClockSource.nrOfEnds() != 1) {
        Reporter.Report.AddFatalError(
            "BUG: Found a clock source with more than 1 connection\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return false;
      }
      ConnectionEnd ClockConnection = ClockSource.getEnd(0);
      if (ClockConnection.nrOfBits() != 1) {
        Reporter.Report.AddFatalError(
            "BUG: Found a clock source with a bus as output\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return false;
      }
      ConnectionPoint SolderPoint = ClockConnection.get((byte) 0);
      /* Check if the clock source is connected */
      if (SolderPoint.getParentNet() != null) {
        /* Third pass: add this clock to the list of ClockSources */
        int clockid = ClockSources.getClockId(ClockSource.getComponent());
        /* Forth pass: Add this source as clock source to the tree */
        myClockInformation.AddClockSource(HierarchyNames, clockid, SolderPoint);
        /* Fifth pass: trace the clock net all the way */
        if (!TraceClockNet(
            SolderPoint.getParentNet(),
            SolderPoint.getParentNetBitIndex(),
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
    for (Net net : myNets) {
      if (net.isRootNet()) {
        if (net.hasShortCircuit()) {
          SimpleDRCContainer error =
              new SimpleDRCContainer(
                      myCircuit,
                      S.get("NetList_ShortCircuit"),
                      SimpleDRCContainer.LEVEL_FATAL,
                      SimpleDRCContainer.MARK_WIRE);
          error.addMarkComponents(net.getWires());
          Reporter.Report.AddError(error);
          ret = true;
        } else if (net.bitWidth() == 1 && net.getSourceNets(0).size() > 1) {
          /* We have to check if the net is connected to multiple drivers */
          ArrayList<ConnectionPoint> sourceNets = net.getSourceNets(0);
          HashMap<Component, Integer> sourceConnections = new HashMap<>();
          HashSet<Wire> segments = new HashSet<>(net.getWires());
          boolean foundShortCrcuit = false;
          SimpleDRCContainer error =
              new SimpleDRCContainer(
                      myCircuit,
                      S.get("NetList_ShortCircuit"),
                      SimpleDRCContainer.LEVEL_FATAL,
                      SimpleDRCContainer.MARK_WIRE | SimpleDRCContainer.MARK_INSTANCE);
          for (ConnectionPoint sourceNet : sourceNets) {
            Net connectedNet = sourceNet.getParentNet();
            byte bitIndex = sourceNet.getParentNetBitIndex();
            if (HasHiddenSource(net, (byte) 0, connectedNet, bitIndex, myComplexSplitters,
                new HashSet<>(), null)) {
              SourceInfo source = GetHiddenSource(net, (byte) 0, connectedNet, bitIndex,
                      myComplexSplitters, new HashSet<>(), segments, null);
              if (source == null) {
                /* this should never happen */
                return true;
              }
              Component comp = source.getSource().getComp();
              for (Wire seg : segments)
                error.addMarkComponent(seg);
              error.addMarkComponent(comp);
              int index = source.getIndex();
              foundShortCrcuit |=
                  (sourceConnections.containsKey(comp) && sourceConnections.get(comp) != index)
                      || (sourceConnections.keySet().size() > 0);
              sourceConnections.put(comp, index);
            }
          }
          if (foundShortCrcuit) {
            ret = true;
            Reporter.Report.AddError(error);
          } else net.cleanupSourceNets(0);
        }
      }
    }
    return ret;
  }

  public boolean NetlistHasSinksWithoutSource() {
    /* First pass: we make a set with all sinks */
    Set<ConnectionPoint> MySinks = new HashSet<>();
    for (Net ThisNet : myNets) {
      if (ThisNet.isRootNet()) {
        MySinks.addAll(ThisNet.getSinks());
      }
    }
    /* Second pass: we iterate along all the sources */
    for (Net ThisNet : myNets) {
      if (ThisNet.isRootNet()) {
        for (int i = 0; i < ThisNet.bitWidth(); i++) {
          if (ThisNet.hasBitSource(i)) {
            boolean HasSink = false;
            ArrayList<ConnectionPoint> Sinks = ThisNet.getBitSinks(i);
            HasSink |= !Sinks.isEmpty();
            Sinks.forEach(MySinks::remove);
            ArrayList<ConnectionPoint> HiddenSinkNets =
                GetHiddenSinks(
                    ThisNet, (byte) i, myComplexSplitters, new HashSet<>(), true);
            HasSink |= !HiddenSinkNets.isEmpty();
            HiddenSinkNets.forEach(MySinks::remove);
            if (!HasSink) {
              SimpleDRCContainer warn =
                  new SimpleDRCContainer(
                          myCircuit,
                          S.get("NetList_SourceWithoutSink"),
                          SimpleDRCContainer.LEVEL_NORMAL,
                          SimpleDRCContainer.MARK_WIRE);
              warn.addMarkComponents(ThisNet.getWires());
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
                    myCircuit,
                    S.get("NetList_UnsourcedSink"),
                    SimpleDRCContainer.LEVEL_SEVERE,
                    SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_WIRE);
        warn.addMarkComponents(Sink.getParentNet().getWires());
        if (Sink.getComp() != null) {
          warn.addMarkComponent(Sink.getComp());
        }
        Reporter.Report.AddWarning(warn);
      }
    }
    return false;
  }

  public int numberOfBusses() {
    int nr_of_busses = 0;
    for (Net ThisNet : myNets) {
      if (ThisNet.isRootNet() && ThisNet.isBus()) {
        nr_of_busses++;
      }
    }
    return nr_of_busses;
  }

  public int NumberOfClockTrees() {
    return myClockInformation.getSourceContainer().getNrofSources();
  }

  public int NumberOfInOutBubbles() {
    return localNrOfInOutBubles;
  }

  public int NumberOfInOutPortBits() {
    int count = 0;
    for (NetlistComponent inp : myInOutPorts) {
      count += inp.getEnd(0).nrOfBits();
    }
    return count;
  }

  public int NumberOfInOutPorts() {
    return myInOutPorts.size();
  }

  public int NumberOfInputBubbles() {
    return localNrOfInportBubles;
  }

  public int NumberOfInputPortBits() {
    int count = 0;
    for (NetlistComponent inp : myInputPorts) {
      count += inp.getEnd(0).nrOfBits();
    }
    return count;
  }

  public int NumberOfInputPorts() {
    return myInputPorts.size();
  }

  public int numberOfNets() {
    int nrOfNets = 0;
    for (final var thisNet : myNets) {
      if (thisNet.isRootNet() && !thisNet.isBus()) {
        nrOfNets++;
      }
    }
    return nrOfNets;
  }

  public int NumberOfOutputBubbles() {
    return localNrOfOutportBubles;
  }

  public int NumberOfOutputPortBits() {
    int count = 0;
    for (NetlistComponent outp : myOutputPorts) {
      count += outp.getEnd(0).nrOfBits();
    }
    return count;
  }

  public int NumberOfOutputPorts() {
    return myOutputPorts.size();
  }

  private boolean ProcessNormalComponent(Component comp) {
    NetlistComponent NormalComponent = new NetlistComponent(comp);
    for (EndData ThisPin : comp.getEnds()) {
      Net Connection = findConnectedNet(ThisPin.getLocation());
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
          ConnectionPoint ThisSolderPoint = ThisEnd.get(bitid);
          ThisSolderPoint.setParentNet(RootNet, RootNetBitIndex);
          if (PinIsSink) {
            RootNet.addSink(RootNetBitIndex, ThisSolderPoint);
          } else {
            RootNet.addSource(RootNetBitIndex, ThisSolderPoint);
          }
        }
      }
    }
    if (comp.getFactory() instanceof Clock) {
      myClockGenerators.add(NormalComponent);
    } else if (comp.getFactory() instanceof Pin) {
      if (comp.getEnd(0).isInput()) {
        myOutputPorts.add(NormalComponent);
      } else {
        myInputPorts.add(NormalComponent);
      }
    } else {
      myComponents.add(NormalComponent);
    }
    return true;
  }

  private boolean ProcessSubcircuit(Component comp) {
    NetlistComponent Subcircuit = new NetlistComponent(comp);
    SubcircuitFactory sub = (SubcircuitFactory) comp.getFactory();
    Instance[] subPins = ((CircuitAttributes) comp.getAttributeSet()).getPinInstances();
    Netlist subNetlist = sub.getSubcircuit().getNetList();
    for (EndData ThisPin : comp.getEnds()) {
      Net Connection = findConnectedNet(ThisPin.getLocation());
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
          Subcircuit.getEnd(PinId).get(bitid).setParentNet(RootNet, RootNetBitIndex);
          if (PinIsSink) {
            RootNet.addSink(RootNetBitIndex, Subcircuit.getEnd(PinId).get(bitid));
          } else {
            RootNet.addSource(RootNetBitIndex, Subcircuit.getEnd(PinId).get(bitid));
          }
          /*
           * Special handling for sub-circuits; we have to find out
           * the connection to the corresponding net in the underlying
           * net-list; At this point the underlying net-lists have
           * already been generated.
           */
          Subcircuit.getEnd(PinId).get(bitid).setChildsPortIndex(SubPortIndex);
        }
      } else {
        for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
          Subcircuit.getEnd(PinId).get(bitid).setChildsPortIndex(SubPortIndex);
        }
      }
    }
    mySubCircuits.add(Subcircuit);
    return true;
  }

  public String projName() {
    return myCircuit.getProjName();
  }

  public boolean RequiresGlobalClockConnection() {
    return myClockInformation.getSourceContainer().getRequiresFpgaGlobalClock();
  }

  public void SetCurrentHierarchyLevel(ArrayList<String> Level) {
    currentHierarchyLevel.clear();
    currentHierarchyLevel.addAll(Level);
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
    SubcircuitFactory sub = (SubcircuitFactory) p.getComp().getFactory();
    NetlistComponent InputPort =
        sub.getSubcircuit().getNetList().GetInputPin(p.getChildsPortIndex());
    if (InputPort == null) {
      Reporter.Report.AddFatalError(
          "BUG: Unable to find Subcircuit input port!\n ==> "
              + this.getClass().getName().replaceAll("\\.", "/")
              + ":"
              + Thread.currentThread().getStackTrace()[2].getLineNumber()
              + "\n");
      return false;
    }
    NetlistComponent subCirc = getSubCirc(p.getComp());
    if (subCirc == null) {
      Reporter.Report.AddFatalError(
          "BUG: Unable to find Subcircuit!\n ==> "
              + this.getClass().getName().replaceAll("\\.", "/")
              + ":"
              + Thread.currentThread().getStackTrace()[2].getLineNumber()
              + "\n");
      return false;
    }
    byte BitIndex = subCirc.getConnectionBitIndex(p.getParentNet(), p.getParentNetBitIndex());
    if (BitIndex < 0) {
      Reporter.Report.AddFatalError(
          "BUG: Unable to find the bit index of a Subcircuit input port!\n ==> "
              + this.getClass().getName().replaceAll("\\.", "/")
              + ":"
              + Thread.currentThread().getStackTrace()[2].getLineNumber()
              + "\n");
      return false;
    }
    ConnectionPoint SubClockNet = InputPort.getEnd(0).get(BitIndex);
    if (SubClockNet.getParentNet() != null) {
      /* we have a connected pin */
      ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
      String Label = CorrectLabel.getCorrectLabel(
              subCirc.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      NewHierarchyNames.add(Label);
      ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
      NewHierarchyNetlists.add(sub.getSubcircuit().getNetList());
      sub.getSubcircuit()
          .getNetList()
          .MarkClockNet(NewHierarchyNames, ClockSourceId, SubClockNet, true);
      return sub.getSubcircuit()
          .getNetList()
          .TraceClockNet(
              SubClockNet.getParentNet(),
              SubClockNet.getParentNetBitIndex(),
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
    ArrayList<ConnectionPoint> HiddenComps =
        GetHiddenSinks(ClockNet, ClockNetBitIndex, myComplexSplitters, new HashSet<>(), false);
    for (ConnectionPoint p : HiddenComps) {
      MarkClockNet(HierarchyNames, ClockSourceId, p, isPinSource);
      if (p.getComp().getFactory() instanceof SubcircuitFactory)
        if (!TraceDownSubcircuit(p, ClockSourceId, HierarchyNames, HierarchyNetlists)) return false;
      /* On top level we do not have to go up */
      if (HierarchyNames.isEmpty()) continue;
      if (p.getComp().getFactory() instanceof Pin) {
        NetlistComponent OutputPort = getOutPort(p.getComp());
        if (OutputPort == null) {
          Reporter.Report.AddFatalError(
              "BUG: Could not find an output port!\n ==> "
                  + this.getClass().getName().replaceAll("\\.", "/")
                  + ":"
                  + Thread.currentThread().getStackTrace()[2].getLineNumber()
                  + "\n");
          return false;
        }
        byte bitindex =
            OutputPort.getConnectionBitIndex(p.getParentNet(), p.getParentNetBitIndex());
        ConnectionPoint SubClockNet =
            HierarchyNetlists.get(HierarchyNetlists.size() - 2)
                .GetNetlistConnectionForSubCircuit(
                    HierarchyNames.get(HierarchyNames.size() - 1),
                    myOutputPorts.indexOf(OutputPort),
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
        if (SubClockNet.getParentNet() == null) {
        } else {
          ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
          NewHierarchyNames.remove(NewHierarchyNames.size() - 1);
          ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
          NewHierarchyNetlists.remove(NewHierarchyNetlists.size() - 1);
          HierarchyNetlists.get(HierarchyNetlists.size() - 2)
              .MarkClockNet(NewHierarchyNames, ClockSourceId, SubClockNet, true);
          if (!HierarchyNetlists.get(HierarchyNetlists.size() - 2)
              .TraceClockNet(
                  SubClockNet.getParentNet(),
                  SubClockNet.getParentNetBitIndex(),
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
    for (NetlistComponent current : mySubCircuits)
      if (current.getComponent().equals(comp))
        return current;
    return null;
  }

  private NetlistComponent getOutPort(Component comp) {
    for (NetlistComponent current : myOutputPorts)
      if (current.getComponent().equals(comp))
        return current;
    return null;
  }

  private boolean detectGatedClocks() {
    // First Pass: We gather a complete information tree about components with clock inputs and
    // their connected nets in    case it is not a clock net. The moment we call this function the
    // clock tree has been marked already!
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
          warn.addMarkComponent(comp.getComponent());
          Reporter.Report.AddWarning(warn);
        }
        instances = GatedSet.get(key);
        for (NetlistComponent comp : instances.keySet()) {
          comp.setIsGatedInstance();
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                      instances.get(comp),
                      S.get("NetList_CircuitGated"),
                      SimpleDRCContainer.LEVEL_NORMAL,
                      SimpleDRCContainer.MARK_INSTANCE,
                      true);
          warn.addMarkComponent(comp.getComponent());
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
    for (NetlistComponent SubCirc : mySubCircuits) {
      SubcircuitFactory sub = (SubcircuitFactory) SubCirc.getComponent().getFactory();
      ArrayList<String> NewHierarchyNames = new ArrayList<>(GetCurrentHierarchyLevel());
      NewHierarchyNames.add(
          CorrectLabel.getCorrectLabel(
              SubCirc.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
      NewHierarchyNetlists.add(sub.getSubcircuit().getNetList());
      sub.getSubcircuit().getNetList().SetCurrentHierarchyLevel(NewHierarchyNames);
      sub.getSubcircuit()
          .getNetList()
          .GetGatedClockComponents(
              NewHierarchyNetlists, SubCirc, NotGatedSet, GatedSet, WarnedComponents);
    }
    // Second pass: we find all components with a clock input and see if they are connected to a
    // clock
    boolean GatedClock = false;
    List<SourceInfo> PinSources = new ArrayList<>();
    List<Set<Wire>> PinWires = new ArrayList<>();
    List<Set<NetlistComponent>> PinGatedComponents = new ArrayList<>();
    List<SourceInfo> NonPinSources = new ArrayList<>();
    List<Set<Wire>> NonPinWires = new ArrayList<>();
    List<Set<NetlistComponent>> NonPinGatedComponents = new ArrayList<>();
    for (NetlistComponent comp : myComponents) {
      ComponentFactory fact = comp.getComponent().getFactory();
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
     * 1) The gated clock net is generated locally, in this case we can mark them and add the
     *    current system to the non-gated set as
     *    each instance will be equal at higher/lower levels.
     * 2) The gated clock nets are connected to a pin, in this case each instance of this circuit
     *    could be either gated or non-gated,
     *    we have to do something on the level higher and we mark this in the sets to be
     *    processed later.
     */

    String MyName = CorrectLabel.getCorrectLabel(circuitName);
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

      if (GatedClock
          && !PinSources.isEmpty()
          && !AppPreferences.SupressGatedClockWarnings.getBoolean()) {
        for (int i = 0; i < PinSources.size(); i++) {
          Reporter.Report.AddSevereWarning(S.get("NetList_GatedClock"));
          Reporter.Report.AddWarningIncrement(S.get("NetList_TraceListBegin"));
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                      myCircuit,
                      S.get("NetList_GatedClockSink"),
                      SimpleDRCContainer.LEVEL_NORMAL,
                      SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(PinWires.get(i));
          for (NetlistComponent comp : PinGatedComponents.get(i))
            warn.addMarkComponent(comp.getComponent());
          Reporter.Report.AddWarning(warn);
          WarningTraceForGatedClock(
              PinSources.get(i).getSource(),
              PinSources.get(i).getIndex(),
              HierarchyNetlists,
                  currentHierarchyLevel);
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
      ConnectionPoint connection = comp.getEnd(ClockPinIndex).get((byte) 0);
      Net connectedNet = connection.getParentNet();
      byte connectedNetindex = connection.getParentNetBitIndex();
      if (connectedNet != null) {
        GatedClock = true;
        Set<Wire> Segments = new HashSet<>();
        SourceInfo source =
            GetHiddenSource(
                null,
                (byte) 0,
                connectedNet,
                connectedNetindex,
                    myComplexSplitters,
                new HashSet<>(),
                Segments,
                null);
        ConnectionPoint sourceCon = source.getSource();
        if (sourceCon.getComp().getFactory() instanceof Pin) {
          int index = IndexOfEntry(PinSources, sourceCon, (int) connectedNetindex);
          if (index < 0) {
            PinSources.add(source);
            PinWires.add(Segments);
            Set<NetlistComponent> comps = new HashSet<>();
            comps.add(comp);
            comps.add(new NetlistComponent(sourceCon.getComp()));
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
                      myCircuit,
                      S.get("NetList_NoClockConnection"),
                      SimpleDRCContainer.LEVEL_SEVERE,
                      SimpleDRCContainer.MARK_INSTANCE);
          warn.addMarkComponent(comp.getComponent());
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

    Component comp = Source.getComp();
    if (comp.getFactory() instanceof Pin) {
      if (HierarchyNames.isEmpty())
        /* we cannot go up at toplevel, so leave */
        return;
      int idx = -1;
      for (int i = 0; i < myInputPorts.size(); i++) {
        if (myInputPorts.get(i).getComponent().equals(comp)) idx = i;
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
      if (SubNet.getParentNet() != null) {
        ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
        NewHierarchyNames.remove(NewHierarchyNames.size() - 1);
        ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
        NewHierarchyNetlists.remove(NewHierarchyNetlists.size() - 1);
        Netlist SubNetList = HierarchyNetlists.get(HierarchyNetlists.size() - 2);
        Net NewNet = SubNet.getParentNet();
        Byte NewNetIndex = SubNet.getParentNetBitIndex();
        Set<Wire> Segments = new HashSet<>();
        SourceInfo source =
            SubNetList.GetHiddenSource(
                null,
                (byte) 0,
                NewNet,
                NewNetIndex,
                SubNetList.myComplexSplitters,
                new HashSet<>(),
                Segments,
                null);
        if (source == null) {
          Reporter.Report.AddFatalError(
              "BUG: Unable to find source in sub-circuit!\n ==> "
                  + this.getClass().getName().replaceAll("\\.", "/")
                  + ":"
                  + Thread.currentThread().getStackTrace()[2].getLineNumber()
                  + "\n");
          return;
        }
        ComponentFactory sfac = source.getSource().getComp().getFactory();
        if (sfac instanceof Pin || sfac instanceof SubcircuitFactory) {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                      SubNetList.getCircuit(),
                      S.get("NetList_GatedClockInt"),
                      SimpleDRCContainer.LEVEL_NORMAL,
                      SimpleDRCContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(Segments);
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
          warn.addMarkComponents(Segments);
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
      Net ConnectedNet = Source.getParentNet();
      /* Find the correct subcircuit */
      NetlistComponent SubCirc = null;
      for (NetlistComponent subc : mySubCircuits) {
        if (subc.getComponent().equals(Source.getComp())) SubCirc = subc;
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
      byte BitIndex = SubCirc.getConnectionBitIndex(ConnectedNet, (byte) index);
      if (BitIndex < 0) {
        Reporter.Report.AddFatalError(
            "BUG: Unable to find the bit index of a Subcircuit output port!\n ==> "
                + this.getClass().getName().replaceAll("\\.", "/")
                + ":"
                + Thread.currentThread().getStackTrace()[2].getLineNumber()
                + "\n");
        return;
      }
      ConnectionPoint SubNet = OutputPort.getEnd(0).get(BitIndex);
      if (SubNet.getParentNet() != null) {
        /* we have a connected pin */
        Netlist SubNetList = sub.getSubcircuit().getNetList();
        ArrayList<String> NewHierarchyNames = new ArrayList<>(HierarchyNames);
        NewHierarchyNames.add(
            CorrectLabel.getCorrectLabel(
                SubCirc.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        ArrayList<Netlist> NewHierarchyNetlists = new ArrayList<>(HierarchyNetlists);
        NewHierarchyNetlists.add(SubNetList);
        Net NewNet = SubNet.getParentNet();
        Byte NewNetIndex = SubNet.getParentNetBitIndex();
        Set<Wire> Segments = new HashSet<>();
        SourceInfo source =
            SubNetList.GetHiddenSource(
                null,
                (byte) 0,
                NewNet,
                NewNetIndex,
                SubNetList.myComplexSplitters,
                new HashSet<>(),
                Segments,
                null);
        if (source == null) {
          Reporter.Report.AddFatalError(
              "BUG: Unable to find source in sub-circuit!\n ==> "
                  + this.getClass().getName().replaceAll("\\.", "/")
                  + ":"
                  + Thread.currentThread().getStackTrace()[2].getLineNumber()
                  + "\n");
          return;
        }
        ComponentFactory sfac = source.getSource().getComp().getFactory();
        if (sfac instanceof Pin || sfac instanceof SubcircuitFactory) {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                      SubNetList.getCircuit(),
                      S.get("NetList_GatedClockInt"),
                      SimpleDRCContainer.LEVEL_NORMAL,
                      SimpleDRCContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(Segments);
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
          warn.addMarkComponents(Segments);
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
        if (Sources.get(i).getSource().getComp().getFactory() instanceof SubcircuitFactory) {
          Reporter.Report.AddSevereWarning(S.get("NetList_GatedClock"));
          Reporter.Report.AddWarningIncrement(S.get("NetList_TraceListBegin"));
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                      myCircuit,
                      S.get("NetList_GatedClockSink"),
                      SimpleDRCContainer.LEVEL_NORMAL,
                      SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(Wires.get(i));
          for (NetlistComponent comp : Components.get(i))
            warn.addMarkComponent(comp.getComponent());
          Reporter.Report.AddWarning(warn);
          WarningTraceForGatedClock(
              Sources.get(i).getSource(),
              Sources.get(i).getIndex(),
              HierarchyNetlists,
                  currentHierarchyLevel);
          Reporter.Report.AddWarningIncrement(S.get("NetList_TraceListEnd"));
        } else {
          SimpleDRCContainer warn =
              new SimpleDRCContainer(
                      myCircuit,
                      Warning,
                      SimpleDRCContainer.LEVEL_SEVERE,
                      SimpleDRCContainer.MARK_INSTANCE | SimpleDRCContainer.MARK_WIRE);
          for (NetlistComponent comp : Components.get(i))
            warn.addMarkComponent(comp.getComponent());
          warn.addMarkComponents(Wires.get(i));
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
