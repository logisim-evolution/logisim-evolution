/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
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

public class Netlist {

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
  private final ArrayList<netlistComponent> mySubCircuits = new ArrayList<>();
  private final ArrayList<netlistComponent> myComponents = new ArrayList<>();
  private final ArrayList<netlistComponent> myClockGenerators = new ArrayList<>();
  private final ArrayList<netlistComponent> myInOutPorts = new ArrayList<>();
  private final ArrayList<netlistComponent> myInputPorts = new ArrayList<>();
  private final ArrayList<netlistComponent> myOutputPorts = new ArrayList<>();
  private final ArrayList<Component> mySplitters = new ArrayList<>();
  private Integer localNrOfInportBubbles;
  private Integer localNrOfOutportBubbles;
  private Integer localNrOfInOutBubbles;
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
    clear();
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
    mySplitters.clear();
    localNrOfInportBubbles = 0;
    localNrOfOutportBubbles = 0;
    localNrOfInOutBubbles = 0;
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

  /**
   *
   * @param circuits Processed circuits
   * @param name Hierarchy name.
   * @param gInputId Global input Id.
   * @param gOutputId Global output Id.
   * @param gInOutId Global In/Out Id
   */
  public void constructHierarchyTree(Set<String> circuits, ArrayList<String> name, Integer gInputId, Integer gOutputId, Integer gInOutId) {
    if (circuits == null) {
      circuits = new HashSet<>();
    }
    /*
     * The first step is to go down to the leaves and visit all involved
     * sub-circuits to construct the local bubble information and form the
     * Mappable components tree
     */
    localNrOfInportBubbles = 0;
    localNrOfOutportBubbles = 0;
    localNrOfInOutBubbles = 0;
    for (final var comp : mySubCircuits) {
      final var subFactory = (SubcircuitFactory) comp.getComponent().getFactory();
      final var names = new ArrayList<String>(name);
      names.add(
          CorrectLabel.getCorrectLabel(
              comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      final var firstTime = !circuits.contains(subFactory.getName());
      if (firstTime) {
        circuits.add(subFactory.getName());
        subFactory.getSubcircuit()
            .getNetList()
            .constructHierarchyTree(circuits, names, gInputId, gOutputId, gInOutId);
      }
      final var subInputBubbles = subFactory.getSubcircuit().getNetList().getNumberOfInputBubbles();
      final var subInOutBubbles = subFactory.getSubcircuit().getNetList().numberOfInOutBubbles();
      final var subOutputBubbles = subFactory.getSubcircuit().getNetList().numberOfOutputBubbles();
      comp.setLocalBubbleID(localNrOfInportBubbles, subInputBubbles, localNrOfOutportBubbles, subOutputBubbles, localNrOfInOutBubbles, subInOutBubbles);
      localNrOfInportBubbles += subInputBubbles;
      localNrOfInOutBubbles += subInOutBubbles;
      localNrOfOutportBubbles += subOutputBubbles;
      comp.addGlobalBubbleId(names, gInputId, subInputBubbles, gOutputId, subOutputBubbles, gInOutId, subInOutBubbles);
      if (!firstTime) {
        subFactory.getSubcircuit()
            .getNetList()
            .enumerateGlobalBubbleTree(names, gInputId, gOutputId, gInOutId);
      }
      gInputId += subInputBubbles;
      gInOutId += subInOutBubbles;
      gOutputId += subOutputBubbles;
    }
    /*
     * Here we processed all sub-circuits of the local hierarchy level, now
     * we have to process the IO components
     */
    for (final var comp : myComponents) {
      if (comp.getMapInformationContainer() != null) {
        final var myHierarchyName = new ArrayList<String>(name);
        myHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        final var subInputBubbles = comp.getMapInformationContainer().getNrOfInPorts();
        final var subInOutBubbles = comp.getMapInformationContainer().getNrOfInOutPorts();
        final var subOutputBubbles = comp.getMapInformationContainer().getNrOfOutPorts();
        comp.setLocalBubbleID(localNrOfInportBubbles, subInputBubbles, localNrOfOutportBubbles, subOutputBubbles, localNrOfInOutBubbles, subInOutBubbles);
        localNrOfInportBubbles += subInputBubbles;
        localNrOfInOutBubbles += subInOutBubbles;
        localNrOfOutportBubbles += subOutputBubbles;
        comp.addGlobalBubbleId(myHierarchyName, gInputId, subInputBubbles, gOutputId, subOutputBubbles, gInOutId, subInOutBubbles);
        gInputId += subInputBubbles;
        gInOutId += subInOutBubbles;
        gOutputId += subOutputBubbles;
      }
    }
  }

  public int designRuleCheckResult(boolean isTopLevel, ArrayList<String> sheetNames) {
    final var compNames = new ArrayList<String>();
    final var labels = new HashMap<String, Component>();
    final var drc = new ArrayList<SimpleDrcContainer>();

    // if we are the toplevel component we clear the complete netlist
    if (isTopLevel) clear();

    // if we already have good drc results we can leave
    if (drcStatus == DRC_PASSED) return DRC_PASSED;

    // we mark already passed, if an error * occurs the status is changed
    drcStatus = DRC_PASSED;

    // Check for duplicated sheet names, this is bad as we will have
    // multiple "different" components with the same name
    if (myCircuit.getName().isEmpty()) {
      // in the current implementation of logisim this should never
      // happen, but we leave it in
      Reporter.report.addFatalError(S.get("EmptyNamedSheet"));
      drcStatus |= DRC_ERROR;
    }
    if (sheetNames.contains(myCircuit.getName())) {
      // in the current implementation of logisim this should never
      // happen, but we leave it in
      Reporter.report.addFatalError(S.get("MultipleSheetSameName", myCircuit.getName()));
      drcStatus |= DRC_ERROR;
    } else {
      sheetNames.add(myCircuit.getName());
    }
    // we have to go down the tree to build first all subcircuits
    final var handledCircuits = new ArrayList<Circuit>();
    for (final var comp : myCircuit.getNonWires()) {
      if (comp.getFactory() instanceof SubcircuitFactory factory) {
        final var subCircuit = factory.getSubcircuit();
        if (handledCircuits.contains(subCircuit)) continue;
        handledCircuits.add(subCircuit);
        if (subCircuit.getNetList().designRuleCheckResult(false, sheetNames) != DRC_PASSED) {
          drcStatus = DRC_REQUIRED;
          return DRC_ERROR;
        }
      }
    }

    // Preparing stage
    for (final var comp : myCircuit.getNonWires()) {
      final var compName = comp.getFactory().getHDLName(comp.getAttributeSet());
      if (!compNames.contains(compName)) compNames.add(compName);
    }

    drc.add(
        new SimpleDrcContainer(
            myCircuit,
            S.get("HDL_noLabel"),
            SimpleDrcContainer.LEVEL_FATAL,
            SimpleDrcContainer.MARK_INSTANCE));
    drc.add(
        new SimpleDrcContainer(
            myCircuit,
            S.get("HDL_CompNameIsLabel"),
            SimpleDrcContainer.LEVEL_FATAL,
            SimpleDrcContainer.MARK_INSTANCE | SimpleDrcContainer.MARK_LABEL));
    drc.add(
        new SimpleDrcContainer(
            myCircuit,
            S.get("HDL_LabelInvalid"),
            SimpleDrcContainer.LEVEL_FATAL,
            SimpleDrcContainer.MARK_INSTANCE | SimpleDrcContainer.MARK_LABEL));
    drc.add(
        new SimpleDrcContainer(
            myCircuit,
            S.get("HDL_DuplicatedLabels"),
            SimpleDrcContainer.LEVEL_FATAL,
            SimpleDrcContainer.MARK_INSTANCE | SimpleDrcContainer.MARK_LABEL));
    drc.add(
        new SimpleDrcContainer(
            myCircuit,
            S.get("HDL_Tristate"),
            SimpleDrcContainer.LEVEL_FATAL,
            SimpleDrcContainer.MARK_INSTANCE));
    drc.add(
        new SimpleDrcContainer(
            myCircuit,
            S.get("HDL_unsupported"),
            SimpleDrcContainer.LEVEL_FATAL,
            SimpleDrcContainer.MARK_INSTANCE));

    for (final var comp : myCircuit.getNonWires()) {
      // Here we check if the components are supported for the HDL generation
      if (!comp.getFactory().isHDLSupportedComponent(comp.getAttributeSet())) {
        drc.get(5).addMarkComponent(comp);
        drcStatus |= DRC_ERROR;
      }
      // we check that all components that require a non zero label (annotation) have a label set
      if (comp.getFactory().requiresNonZeroLabel()) {
        final var label = CorrectLabel.getCorrectLabel(comp.getAttributeSet().getValue(StdAttr.LABEL)).toUpperCase();
        final var componentName = comp.getFactory().getHDLName(comp.getAttributeSet());
        if (label.isEmpty()) {
          drc.get(0).addMarkComponent(comp);
          drcStatus |= ANNOTATE_REQUIRED;
        } else {
          if (compNames.contains(label)) {
            drc.get(1).addMarkComponent(comp);
            drcStatus |= DRC_ERROR;
          }
          if (!CorrectLabel.isCorrectLabel(label)) {
            /* this should not happen anymore */
            drc.get(2).addMarkComponent(comp);
            drcStatus |= DRC_ERROR;
          }
          if (labels.containsKey(label)) {
            drc.get(3).addMarkComponent(comp);
            drc.get(3).addMarkComponent(labels.get(label));
            drcStatus |= DRC_ERROR;
          } else {
            labels.put(label, comp);
          }
        }
        if (comp.getFactory() instanceof SubcircuitFactory) {
          // Special care has to be taken for sub-circuits
          if (label.equals(componentName.toUpperCase())) {
            drc.get(1).addMarkComponent(comp);
            drcStatus |= DRC_ERROR;
          }
          if (!CorrectLabel.isCorrectLabel(
              comp.getFactory().getName(),
              S.get("FoundBadComponent", comp.getFactory().getName(), myCircuit.getName()))) {
            drcStatus |= DRC_ERROR;
          }
          final var sub = (SubcircuitFactory) comp.getFactory();
          localNrOfInportBubbles += sub.getSubcircuit().getNetList().getNumberOfInputBubbles();
          localNrOfOutportBubbles += sub.getSubcircuit().getNetList().numberOfOutputBubbles();
          localNrOfInOutBubbles += sub.getSubcircuit().getNetList().numberOfInOutBubbles();
        }
      }
      /* Now we check that no tri-state are present */
      if (comp.getFactory().hasThreeStateDrivers(comp.getAttributeSet())) {
        drc.get(4).addMarkComponent(comp);
        drcStatus |= DRC_ERROR;
      }
    }
    for (final var simpleDRCContainer : drc) {
      if (simpleDRCContainer.isDrcInfoPresent()) Reporter.report.addError(simpleDRCContainer);
    }
    drc.clear();
    /* Here we have to quit as the netlist generation needs a clean tree */
    if (drcStatus != DRC_PASSED) return drcStatus;

    /*
     * Okay we now know for sure that all elements are supported, lets build
     * the net list
     */
    Reporter.report.addInfo(S.get("BuildingNetlistFor", myCircuit.getName()));
    if (!this.generateNetlist()) {
      this.clear();
      drcStatus = DRC_ERROR;
      /*
       * here we have to quit, as all the following steps depend on a
       * proper netlist
       */
      return drcStatus;
    }

    if (netlistHasShortCircuits()) {
      clear();
      drcStatus = DRC_ERROR;
      return drcStatus;
    }

    /* Check for connections without a source */
    netlistHasSinksWithoutSource();
    /* Check for unconnected input pins on components and generate warnings */
    for (final var comp : myComponents) {
      var openInputs = false;
      for (var j = 0; j < comp.nrOfEnds(); j++) {
        if (comp.isEndInput(j) && !comp.isEndConnected(j)) openInputs = true;
      }
      if (openInputs && !AppPreferences.SupressOpenPinWarnings.get()) {
        final var warn =
            new SimpleDrcContainer(
                    myCircuit,
                    S.get("NetList_UnconnectedInputs"),
                    SimpleDrcContainer.LEVEL_NORMAL,
                    SimpleDrcContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp.getComponent());
        Reporter.report.addWarning(warn);
      }
    }
    /* Check for unconnected input pins on subcircuits and generate warnings */
    for (final var comp : mySubCircuits) {
      var openInputs = false;
      for (var j = 0; j < comp.nrOfEnds(); j++) {
        if (comp.isEndInput(j) && !comp.isEndConnected(j)) openInputs = true;
      }
      if (openInputs && !AppPreferences.SupressOpenPinWarnings.get()) {
        final var warn =
            new SimpleDrcContainer(
                    myCircuit,
                    S.get("NetList_UnconnectedInputs"),
                    SimpleDrcContainer.LEVEL_SEVERE,
                    SimpleDrcContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp.getComponent());
        Reporter.report.addWarning(warn);
      }
    }
    /* Check for unconnected input pins in my circuit and generate warnings */
    for (final var comp : myInputPorts) {
      var openInputs = false;
      for (var j = 0; j < comp.nrOfEnds(); j++) {
        if (!comp.isEndConnected(j)) openInputs = true;
      }
      if (openInputs && !AppPreferences.SupressOpenPinWarnings.get()) {
        final var warn =
            new SimpleDrcContainer(
                    myCircuit,
                    S.get("NetList_UnconnectedInput"),
                    SimpleDrcContainer.LEVEL_NORMAL,
                    SimpleDrcContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp.getComponent());
        Reporter.report.addWarning(warn);
      }
    }
    /* Check for unconnected output pins in my circuit and generate warnings */
    for (final var comp : myOutputPorts) {
      var openOutputs = false;
      for (var j = 0; j < comp.nrOfEnds(); j++) {
        if (!comp.isEndConnected(j)) openOutputs = true;
      }
      if (openOutputs && !AppPreferences.SupressOpenPinWarnings.get()) {
        final var warn =
            new SimpleDrcContainer(
                    myCircuit,
                    S.get("NetList_UnconnectedOutput"),
                    SimpleDrcContainer.LEVEL_NORMAL,
                    SimpleDrcContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp.getComponent());
        Reporter.report.addWarning(warn);
      }
    }

    /* Only if we are on the top-level we are going to build the clock-tree */
    if (isTopLevel) {
      if (!detectClockTree()) {
        drcStatus = DRC_ERROR;
        return drcStatus;
      }
      constructHierarchyTree(null, new ArrayList<>(), 0, 0, 0);
      var ports =
          getNumberOfInputPorts()
              + numberOfOutputPorts()
              + localNrOfInportBubbles
              + localNrOfOutportBubbles
              + localNrOfInOutBubbles;
      if (ports == 0) {
        Reporter.report.addFatalError(S.get("TopLevelNoIO", myCircuit.getName()));
        drcStatus = DRC_ERROR;
        return drcStatus;
      }
      /* Check for gated clocks */
      if (!detectGatedClocks()) {
        drcStatus = DRC_ERROR;
        return drcStatus;
      }
    }

    Reporter.report.addInfo(S.get("CircuitInfoString", myCircuit.getName(), numberOfNets(), numberOfBusses()));
    Reporter.report.addInfo(S.get("DRCPassesString", myCircuit.getName()));
    drcStatus = DRC_PASSED;
    return drcStatus;
  }

  private boolean detectClockTree() {
    // First pass, we remove all information of previously detected clock-trees.
    final var clockSources = myClockInformation.getSourceContainer();
    cleanClockTree(clockSources);
    // Second pass, we build the clock tree
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
        int subInputBubbles = comp.getMapInformationContainer().getNrOfInPorts();
        int subInOutBubbles = comp.getMapInformationContainer().getNrOfInOutPorts();
        int subOutputBubbles = comp.getMapInformationContainer().getNrOfOutPorts();
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
      if (current.contains(loc)) return current;
    }
    return null;
  }

  private boolean generateNetlist() {
    final var drc = new ArrayList<SimpleDrcContainer>();
    var errors = false;
    circuitName = myCircuit.getName();
    final var progress = Reporter.report.getProgressBar();
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
    // FIRST PASS: In this pass we take all wire segments and see if they
    // are connected to other segments. If they are connected we build a net.
    while (wires.size() != 0) {
      final var newNet = new Net();
      getNet(null, newNet);
      if (!newNet.isEmpty()) myNets.add(newNet);
    }
    // Here we start to detect direct input-output component connections, read we detect "hidden"
    // nets
    final var components = myCircuit.getNonWires();
    /* we Start with the creation of an outputs list */
    final var outputsList = new HashSet<Location>();
    final var inputsList = new HashSet<Location>();
    final var tunnelList = new HashSet<Component>();
    mySplitters.clear();
    drc.add(
        new SimpleDrcContainer(
            myCircuit,
            S.get("NetList_IOError"),
            SimpleDrcContainer.LEVEL_FATAL,
            SimpleDrcContainer.MARK_INSTANCE));
    drc.add(
        new SimpleDrcContainer(
            myCircuit,
            S.get("NetList_BitwidthError"),
            SimpleDrcContainer.LEVEL_FATAL,
            SimpleDrcContainer.MARK_WIRE));

    for (final var comp : components) {
      // We do not process the splitter and tunnel, they are processed later on
      var ignore = false;

      // In this case, the probe should not be synthetised:
      // We could set the Probe as non-HDL element. But If we set the Probe
      // as non HDL element, logisim will not allow user to download the design.
      //
      // In some case we need to use Logisim Simulation before running the design on the hardware.
      // During simulation, probes are very helpful to see signals values. And when simulation is
      // ok, the user does not want to delete all probes.
      // Thus, here we remove it form the netlist so it is transparent.
      if (comp.getFactory() instanceof Probe) continue;
      if (comp.getFactory() instanceof SplitterFactory) {
        mySplitters.add(comp);
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
        for (final var thisNet : myNets) {
          if (thisNet.contains(loc) && !thisNet.setWidth(width)) drc.get(1).addMarkComponents(thisNet.getWires());
        }
      }
    }
    for (final var simpleDRCContainer : drc) {
      if (simpleDRCContainer.isDrcInfoPresent()) {
        errors = true;
        Reporter.report.addError(simpleDRCContainer);
      }
    }
    if (errors) return false;
    if (progress != null) {
      progress.setValue(1);
      progress.setString(S.get("NetListBuild", circuitName, 2));
    }
    // Now we check if an input pin is connected to an output and in case of
    // a Splitter if it is connected to either of them
    drc.add(
        new SimpleDrcContainer(
            myCircuit,
            S.get("NetAdd_ComponentWidthMismatch"),
            SimpleDrcContainer.LEVEL_FATAL,
            SimpleDrcContainer.MARK_INSTANCE));
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
        } else {
          points.put(loc, end.getWidth().getWidth());
        }
      }
    }
    if (drc.get(0).isDrcInfoPresent()) {
      Reporter.report.addError(drc.get(0));
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
    var areTunnelsPresent = false;
    for (final var comp : tunnelList) {
      final var ends = comp.getEnds();
      for (final var end : ends) {
        for (final var thisNet : myNets) {
          if (thisNet.contains(end.getLocation())) {
            thisNet.addTunnel(comp.getAttributeSet().getValue(StdAttr.LABEL));
            areTunnelsPresent = true;
          }
        }
      }
    }
    drc.clear();
    drc.add(new SimpleDrcContainer(myCircuit, S.get("NetMerge_BitWidthError"), SimpleDrcContainer.LEVEL_FATAL, SimpleDrcContainer.MARK_WIRE));
    if (areTunnelsPresent) {
      final var netIterator = myNets.listIterator();
      while (netIterator.hasNext()) {
        final var thisNet = netIterator.next();
        if (thisNet.hasTunnel() && (myNets.indexOf(thisNet) < (myNets.size() - 1))) {
          var merged = false;
          final var searchIterator = myNets.listIterator(myNets.indexOf(thisNet) + 1);
          while (searchIterator.hasNext() && !merged) {
            final var searchNet = searchIterator.next();
            for (final var name : thisNet.getTunnelNames()) {
              if (searchNet.containsTunnel(name) && !merged) {
                merged = true;
                if (!searchNet.merge(thisNet)) {
                  drc.get(0).addMarkComponents(searchNet.getWires());
                  drc.get(0).addMarkComponents(thisNet.getWires());
                }
              }
            }
          }
          if (merged) netIterator.remove();
        }
      }
    }
    if (drc.get(0).isDrcInfoPresent()) {
      Reporter.report.addError(drc.get(0));
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
    Iterator<Component> mySplitIter = mySplitters.listIterator();
    while (mySplitIter.hasNext()) {
      final var thisSplitter = mySplitIter.next();
      if (mySplitters.indexOf(thisSplitter) < (mySplitters.size() - 1)) {
        var dupeFound = false;
        final var searchIter = mySplitters.listIterator(mySplitters.indexOf(thisSplitter) + 1);
        while (searchIter.hasNext() && !dupeFound) {
          final var SearchSplitter = searchIter.next();
          if (SearchSplitter.getLocation().equals(thisSplitter.getLocation())) {
            dupeFound = true;
            for (var i = 0; i < SearchSplitter.getEnds().size(); i++) {
              if (!SearchSplitter.getEnd(i)
                  .getLocation()
                  .equals(thisSplitter.getEnd(i).getLocation())) dupeFound = false;
            }
          }
        }
        if (dupeFound) {
          final var warn =
              new SimpleDrcContainer(
                      myCircuit,
                      S.get("NetList_duplicatedSplitter"),
                      SimpleDrcContainer.LEVEL_SEVERE,
                      SimpleDrcContainer.MARK_INSTANCE);
          warn.addMarkComponent(thisSplitter);
          Reporter.report.addWarning(warn);
          mySplitIter.remove();
        }
      }
    }

    // In this round we are going to detect the unconnected nets meaning those having a width of 0
    // and remove them
    drc.clear();
    final Iterator<Net> netIterator = myNets.listIterator();
    drc.add(
        new SimpleDrcContainer(
                myCircuit,
                S.get("NetList_emptynets"),
                SimpleDrcContainer.LEVEL_NORMAL,
                SimpleDrcContainer.MARK_WIRE));
    while (netIterator.hasNext()) {
      final var wire = netIterator.next();
      if (wire.getBitWidth() == 0) {
        drc.get(0).addMarkComponents(wire.getWires());
        netIterator.remove();
      }
    }
    if (drc.get(0).isDrcInfoPresent()) {
      Reporter.report.addWarning(drc.get(0));
    }
    mySplitIter = mySplitters.iterator();
    // We also check quickly the splitters and remove the ones where input-bus is output-bus. We
    // mark those who are not correctly connected and remove both versions from the set.
    drc.clear();
    drc.add(
        new SimpleDrcContainer(
                myCircuit,
                S.get("NetList_ShortCircuit"),
                SimpleDrcContainer.LEVEL_FATAL,
                SimpleDrcContainer.MARK_WIRE));
    errors = false;
    while (mySplitIter.hasNext()) {
      final var mySplitter = mySplitIter.next();
      var busWidth = mySplitter.getEnd(0).getWidth().getWidth();
      final var myEnds = mySplitter.getEnds();
      var maxFanoutWidth = 0;
      var index = -1;
      for (var i = 1; i < myEnds.size(); i++) {
        var width = mySplitter.getEnd(i).getWidth().getWidth();
        if (width > maxFanoutWidth) {
          maxFanoutWidth = width;
          index = i;
        }
      }
      /* stupid situation first: the splitters bus connection is a single fanout */
      if (busWidth == maxFanoutWidth) {
        Net busnet = null;
        Net connectedNet = null;
        final var busLoc = mySplitter.getEnd(0).getLocation();
        final var connectedLoc = mySplitter.getEnd(index).getLocation();
        var issueWarning = false;
        /* here we search for the nets */
        for (final var currentNet : myNets) {
          if (currentNet.contains(busLoc)) {
            if (busnet != null) {
              Reporter.report.addFatalErrorFmt(
                  "BUG: Multiple bus nets found for a single splitter\n ==> %s:%d\n",
                  this.getClass().getName().replace(".", "/"),
                  Thread.currentThread().getStackTrace()[2].getLineNumber());
              return false;
            } else {
              busnet = currentNet;
            }
          }
          if (currentNet.contains(connectedLoc)) {
            if (connectedNet != null) {
              Reporter.report.addFatalErrorFmt(
                  "BUG: Multiple nets found for a single splitter split connection\n ==> %s:%d\n",
                  this.getClass().getName().replace(".", "/"),
                  Thread.currentThread().getStackTrace()[2].getLineNumber());
              return false;
            } else {
              connectedNet = currentNet;
            }
          }
        }
        if (connectedNet != null) {
          if (busnet != null) {
            /* we can merge both nets */
            if (!busnet.merge(connectedNet)) {
              Reporter.report.addFatalErrorFmt(
                  "BUG: Splitter bus merge error\n ==> %s:%d\n",
                  this.getClass().getName().replace(".", "/"),
                  Thread.currentThread().getStackTrace()[2].getLineNumber());
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
          final var warn =
              new SimpleDrcContainer(
                      myCircuit,
                      S.get("NetList_NoSplitterConnection"),
                      SimpleDrcContainer.LEVEL_SEVERE,
                      SimpleDrcContainer.MARK_INSTANCE);
          warn.addMarkComponent(mySplitter);
          Reporter.report.addWarning(warn);
        }
        mySplitIter.remove(); /* Does not exist anymore */
      }
    }

    if (progress != null) {
      progress.setValue(4);
      progress.setString(S.get("NetListBuild", circuitName, 5));
    }

    // Finally we have to process the splitters to determine the bus
    // hierarchy (if any).
    //
    // In this round we only process the evident splitters and remove them
    // from the list.
    for (final var comp : mySplitters) {
      // Currently by definition end(0) is the combined end of the splitter
      final var ends = comp.getEnds();
      final var combinedEnd = ends.get(0);
      var rootNet = -1;
      /* We search for the root net in the list of nets */
      for (var i = 0; i < myNets.size() && rootNet < 0; i++) {
        if (myNets.get(i).contains(combinedEnd.getLocation())) {
          rootNet = i;
          // FIXME: shouldn't we `break` once we found it?
        }
      }
      if (rootNet < 0) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Splitter without a bus connection\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        this.clear();
        return false;
      }
      // Now we process all the other ends to find the child busses/nets
      // of this root bus
      final var connections = new ArrayList<Integer>();
      for (var i = 1; i < ends.size(); i++) {
        final var thisEnd = ends.get(i);
        /* Find the connected net */
        var connectedNet = -1;
        for (var j = 0; j < myNets.size() && connectedNet < 1; j++) {
          if (myNets.get(j).contains(thisEnd.getLocation())) {
            connectedNet = j;
          }
        }
        connections.add(connectedNet);
      }
      var unconnectedEnds = false;
      var connectedUnknownEnds = false;
      final var sattrs = (SplitterAttributes) comp.getAttributeSet();
      for (var i = 1; i < ends.size(); i++) {
        var connectedNet = connections.get(i - 1);
        if (connectedNet >= 0) {
          /* Has this end a connection to the root bus? */
          connectedUnknownEnds |= sattrs.isNoConnect(i);
          /* There is a net connected to this splitter's end point */
          if (!myNets.get(connectedNet).setParent(myNets.get(rootNet))) {
            myNets.get(connectedNet).forceRootNet();
          }
          /* Here we have to process the inherited bits of the parent */
          final var busBitConnection = ((Splitter) comp).getEndpoints();
          for (byte b = 0; b < busBitConnection.length; b++) {
            if (busBitConnection[b] == i) {
              myNets.get(connectedNet).addParentBit(b);
            }
          }
        } else {
          unconnectedEnds = true;
        }
      }
      if (unconnectedEnds) {
        final var warn =
            new SimpleDrcContainer(
                    myCircuit,
                    S.get("NetList_NoSplitterEndConnections"),
                    SimpleDrcContainer.LEVEL_NORMAL,
                    SimpleDrcContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp);
        Reporter.report.addWarning(warn);
      }
      if (connectedUnknownEnds) {
        final var warn =
            new SimpleDrcContainer(
                    myCircuit,
                    S.get("NetList_NoEndSplitterConnections"),
                    SimpleDrcContainer.LEVEL_SEVERE,
                    SimpleDrcContainer.MARK_INSTANCE);
        warn.addMarkComponent(comp);
        Reporter.report.addWarning(warn);
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
    for (final var thisNet : myNets) {
      if (thisNet.isRootNet()) {
        thisNet.initializeSourceSinks();
      }
    }
    /*
     * We are going to iterate through all components and their respective
     * pins to see if they are connected to a net, and if yes if they
     * present a source or sink. We omit the splitter and tunnel as we
     * already processed those
     */

    for (final var comp : components) {
      if (comp.getFactory() instanceof SubcircuitFactory) {
        if (!processSubcircuit(comp)) {
          this.clear();
          return false;
        }
      } else if ((comp.getFactory() instanceof Pin)
          || comp.getAttributeSet().containsAttribute(StdAttr.MAPINFO)
          || (comp.getFactory().getHDLGenerator(comp.getAttributeSet()) != null)) {
        if (!processNormalComponent(comp)) {
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
    for (final var thisNet : myNets) {
      if (thisNet.isForcedRootNet()) {
        /* Cycle through all the bits of this net */
        for (var bit = 0; bit < thisNet.getBitWidth(); bit++) {
          for (final var comp : mySplitters) {
            // Currently by definition end(0) is the combined end of the splitter
            final var ends = comp.getEnds();
            final var combinedEnd = ends.get(0);
            var connectedBus = -1;
            final var sattrs = (SplitterAttributes) comp.getAttributeSet();
            /* We search for the root net in the list of nets */
            for (var i = 0; i < myNets.size() && connectedBus < 0; i++) {
              if (myNets.get(i).contains(combinedEnd.getLocation())) connectedBus = i;
            }
            if (connectedBus < 0) {
              // This should never happen as we already checked in the first pass.
              Reporter.report.addFatalErrorFmt(
                  "BUG: This is embarasing as this should never happen\n ==> %s:%d\n",
                  this.getClass().getName().replace(".", "/"),
                  Thread.currentThread().getStackTrace()[2].getLineNumber());
              this.clear();
              return false;
            }
            for (int endId = 1; endId < ends.size(); endId++) {
              //If this is an end that is not connected to the root bus
              //we can continue we already warned severly before.
              if (sattrs.isNoConnect(endId)) continue;
              // we iterate through all bits to see if the current net is connected to this splitter
              if (thisNet.contains(ends.get(endId).getLocation())) {
                // first we have to get the bitindices of the rootbus
                // Here we have to process the inherited bits of the parent
                final var busBitConnection = ((Splitter) comp).getEndpoints();
                final var indexBits = new ArrayList<Byte>();
                for (byte b = 0; b < busBitConnection.length; b++) {
                  if (busBitConnection[b] == endId) indexBits.add(b);
                }
                byte connectedBusIndex = indexBits.get(bit);
                // Figure out the rootbusid and rootbusindex
                var rootBus = myNets.get(connectedBus);
                while (!rootBus.isRootNet()) {
                  connectedBusIndex = rootBus.getBit(connectedBusIndex);
                  rootBus = rootBus.getParent();
                }
                final var solderPoint = new ConnectionPoint(comp);
                solderPoint.setParentNet(rootBus, connectedBusIndex);
                var isSink = true;
                if (!thisNet.hasBitSource(bit)) {
                  if (hasHiddenSource(thisNet, (byte) bit, rootBus, connectedBusIndex, mySplitters, new HashSet<String>(), comp)) {
                    isSink = false;
                  }
                }
                if (isSink) {
                  thisNet.addSinkNet(bit, solderPoint);
                } else {
                  thisNet.addSourceNet(bit, solderPoint);
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
    return true;
  }

  public List<Component> getAllClockSources() {
    return myClockInformation.getSourceContainer().getSources();
  }

  public List<Net> getAllNets() {
    return myNets;
  }

  public Circuit getCircuit() {
    return myCircuit;
  }

  public String getCircuitName() {
    return circuitName;
  }

  public int getClockSourceId(List<String> hierarchyLevel, Net whichNet, Byte bitId) {
    return myClockInformation.getClockSourceId(hierarchyLevel, whichNet, bitId);
  }

  public int getClockSourceId(Component comp) {
    return myClockInformation.getClockSourceId(comp);
  }

  public List<netlistComponent> getClockSources() {
    return myClockGenerators;
  }

  public List<String> getCurrentHierarchyLevel() {
    return currentHierarchyLevel;
  }

  public int getEndIndex(netlistComponent comp, String pinLabel, boolean isOutputPort) {
    final var label = CorrectLabel.getCorrectLabel(pinLabel);
    final var subFactory = (SubcircuitFactory) comp.getComponent().getFactory();
    for (var end = 0; end < comp.nrOfEnds(); end++) {
      if ((comp.getEnd(end).isOutputEnd() == isOutputPort)
          && (comp.getEnd(end).get((byte) 0).getChildsPortIndex() == subFactory.getSubcircuit().getNetList().getPortInfo(label))) {
        return end;
      }
    }
    return -1;
  }

  private List<ConnectionPoint> getHiddenSinks(Net thisNet, Byte bitIndex, List<Component> splitters, Set<String> handledNets, Boolean isSourceNet) {
    final var result = new ArrayList<ConnectionPoint>();
    // to prevent deadlock situations we check if we already looked at this net
    final var netId = myNets.indexOf(thisNet) + "-" + bitIndex;
    if (handledNets.contains(netId)) return result;
    handledNets.add(netId);

    if (thisNet.hasBitSinks(bitIndex) && !isSourceNet && thisNet.isRootNet()) {
      result.addAll(thisNet.getBitSinks(bitIndex));
    }
    // Check if we have a connection to another splitter
    for (final var currentSplitter : splitters) {
      final var ends = currentSplitter.getEnds();
      final var splitterAttrs = (SplitterAttributes) currentSplitter.getAttributeSet();
      for (byte end = 0; end < ends.size(); end++) {
        /* prevent the search for ends that are not connected to the root bus */
        if (end > 0 && splitterAttrs.isNoConnect(end)) continue;
        if (thisNet.contains(ends.get(end).getLocation())) {
          // Here we have to process the inherited bits of the parent.
          final var busBitConnection = ((Splitter) currentSplitter).getEndpoints();
          if (end == 0) {
            // This is a main net, find the connected end.
            final var splitterEnd = busBitConnection[bitIndex];
            /* Find the corresponding Net index */
            Byte netIndex = 0;
            for (var index = 0; index < bitIndex; index++) {
              if (busBitConnection[index] == splitterEnd) netIndex++;
            }
            // Find the connected Net
            Net slaveNet = null;
            for (final var thisnet : myNets) {
              if (thisnet.contains(ends.get(splitterEnd).getLocation())) slaveNet = thisnet;
            }
            if (slaveNet != null)
              result.addAll(getHiddenSinks(slaveNet, netIndex, splitters, handledNets, false));
          } else {
            final var rootIndices = new ArrayList<Byte>();
            for (byte b = 0; b < busBitConnection.length; b++) {
              if (busBitConnection[b] == end) rootIndices.add(b);
            }
            Net rootNet = null;
            for (final var thisnet : myNets) {
              if (thisnet.contains(currentSplitter.getEnd(0).getLocation())) rootNet = thisnet;
            }
            if (rootNet != null)
              result.addAll(getHiddenSinks(rootNet, rootIndices.get(bitIndex), splitters, handledNets, false));
          }
        }
      }
    }
    return result;
  }

  public netlistComponent getInOutPin(int index) {
    return ((index < 0) || (index >= myInOutPorts.size())) ? null : myInOutPorts.get(index);
  }

  public netlistComponent getInOutPort(int Index) {
    return ((Index < 0) || (Index >= myInOutPorts.size())) ? null : myInOutPorts.get(Index);
  }

  public netlistComponent getInputPin(int index) {
    return ((index < 0) || (index >= myInputPorts.size())) ? null : myInputPorts.get(index);
  }

  public netlistComponent getInputPort(int Index) {
    return ((Index < 0) || (Index >= myInputPorts.size())) ? null : myInputPorts.get(Index);
  }

  public Map<ArrayList<String>, netlistComponent> getMappableResources(List<String> hierarchy, boolean toplevel) {
    final var components = new HashMap<ArrayList<String>, netlistComponent>();
    /* First we search through my sub-circuits and add those IO components */
    for (final var comp : mySubCircuits) {
      final var sub = (SubcircuitFactory) comp.getComponent().getFactory();
      final var MyHierarchyName = new ArrayList<>(hierarchy);
      MyHierarchyName.add(
          CorrectLabel.getCorrectLabel(
              comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      components.putAll(
          sub.getSubcircuit().getNetList().getMappableResources(MyHierarchyName, false));
    }
    /* Now we search for all local IO components */
    for (final var comp : myComponents) {
      if (comp.getMapInformationContainer() != null) {
        final var myHierarchyName = new ArrayList<>(hierarchy);
        myHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        components.put(myHierarchyName, comp);
      }
    }
    /* On the toplevel we have to add the pins */
    if (toplevel) {
      for (final var comp : myInputPorts) {
        final var myHierarchyName = new ArrayList<>(hierarchy);
        myHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        components.put(myHierarchyName, comp);
      }
      for (final var comp : myInOutPorts) {
        final var myHierarchyName = new ArrayList<>(hierarchy);
        myHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        components.put(myHierarchyName, comp);
      }
      for (final var comp : myOutputPorts) {
        final var myHierarchyName = new ArrayList<>(hierarchy);
        myHierarchyName.add(
            CorrectLabel.getCorrectLabel(
                comp.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        components.put(myHierarchyName, comp);
      }
    }
    return components;
  }

  private void getNet(Wire wire, Net thisNet) {
    final var myIterator = wires.iterator();
    final var matchedWires = new ArrayList<Wire>();
    var compWire = wire;
    while (myIterator.hasNext()) {
      final var thisWire = myIterator.next();
      if (compWire == null) {
        compWire = thisWire;
        thisNet.add(thisWire);
        myIterator.remove();
      } else if (thisWire.sharesEnd(compWire)) {
        matchedWires.add(thisWire);
        thisNet.add(thisWire);
        myIterator.remove();
      }
    }
    for (final var matched : matchedWires) getNet(matched, thisNet);
    matchedWires.clear();
  }

  public Integer getNetId(Net selectedNet) {
    return myNets.indexOf(selectedNet);
  }

  public ConnectionPoint getNetlistConnectionForSubCircuit(String label, int PortIndex, byte bitindex) {
    for (final var search : mySubCircuits) {
      final var circuitLabel = CorrectLabel.getCorrectLabel(search.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (circuitLabel.equals(label)) {
        // Found the component, let's search the ends
        for (var i = 0; i < search.nrOfEnds(); i++) {
          final var thisEnd = search.getEnd(i);
          if (thisEnd.isOutputEnd() && (bitindex < thisEnd.getNrOfBits())) {
            if (thisEnd.get(bitindex).getChildsPortIndex() == PortIndex)
              return thisEnd.get(bitindex);
          }
        }
      }
    }
    return null;
  }

  public ConnectionPoint getNetlistConnectionForSubCircuitInput(String label, int portIndex, byte bitIndex) {
    for (final var search : mySubCircuits) {
      final var circuitLabel = CorrectLabel.getCorrectLabel(search.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (circuitLabel.equals(label)) {
        // Found the component, let's search the ends.
        for (var i = 0; i < search.nrOfEnds(); i++) {
          final var thisEnd = search.getEnd(i);
          if (!thisEnd.isOutputEnd() && (bitIndex < thisEnd.getNrOfBits())) {
            if (thisEnd.get(bitIndex).getChildsPortIndex() == portIndex)
              return thisEnd.get(bitIndex);
          }
        }
      }
    }
    return null;
  }

  public List<netlistComponent> getNormalComponents() {
    return myComponents;
  }

  public netlistComponent getOutputPin(int index) {
    return ((index < 0) || (index >= myOutputPorts.size())) ? null : myOutputPorts.get(index);
  }

  public int getPortInfo(String label) {
    final var source = CorrectLabel.getCorrectLabel(label);
    for (final var inPort : myInputPorts) {
      final var comp = CorrectLabel.getCorrectLabel(inPort.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (comp.equals(source)) return myInputPorts.indexOf(inPort);
    }
    for (final var inOutPort : myInOutPorts) {
      final var comp = CorrectLabel.getCorrectLabel(inOutPort.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (comp.equals(source)) return myInOutPorts.indexOf(inOutPort);
    }
    for (final var outPort : myOutputPorts) {
      final var comp = CorrectLabel.getCorrectLabel(outPort.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      if (comp.equals(source)) return myOutputPorts.indexOf(outPort);
    }
    return -1;
  }

  private Net getRootNet(Net child) {
    if (child == null) return null;
    if (child.isRootNet()) return child;
    var rootNet = child.getParent();
    while (!rootNet.isRootNet()) rootNet = rootNet.getParent();
    return rootNet;
  }

  private byte getRootNetIndex(Net child, byte bitIndex) {
    if ((child == null) || ((bitIndex < 0) || (bitIndex > child.getBitWidth()))) return -1;
    if (child.isRootNet()) return bitIndex;
    var rootNet = child.getParent();
    var rootIndex = child.getBit(bitIndex);
    while (!rootNet.isRootNet()) {
      rootIndex = rootNet.getBit(rootIndex);
      rootNet = rootNet.getParent();
    }
    return rootIndex;
  }

  public Set<Splitter> getSplitters() {
    /* This may be cause bugs due to dual splitter on same location situations */
    final var splitters = new HashSet<Splitter>();
    for (final var comp : myCircuit.getNonWires()) {
      if (comp.getFactory() instanceof SplitterFactory) splitters.add((Splitter) comp);
    }
    return splitters;
  }

  public ArrayList<netlistComponent> getSubCircuits() {
    return mySubCircuits;
  }

  private SourceInfo getHiddenSource(
      Net srcNet,
      Byte srcBitIndex,
      Net thisNet,
      Byte bitIndex,
      List<Component> splitters,
      Set<String> handledNets,
      Set<Wire> segments,
      Component splitterToIgnore) {
    // If the source net not is null add it to the set of visited nets to prevent back-search on
    // this net
    if (srcNet != null) {
      final var netId = myNets.indexOf(srcNet) + "-" + srcBitIndex;
      if (handledNets.contains(netId)) return null;
      handledNets.add(netId);
    }
    // to prevent deadlock situations we check if we already looked at this net
    final var netId = myNets.indexOf(thisNet) + "-" + bitIndex;
    if (handledNets.contains(netId)) return null;
    handledNets.add(netId);
    segments.addAll(thisNet.getWires());

    if (thisNet.hasBitSource(bitIndex)) {
      List<ConnectionPoint> sources = thisNet.getBitSources(bitIndex);
      if (sources.size() != 1) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Found multiple sources\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return null;
      }
      return new SourceInfo(sources.get(0), bitIndex);
    }
    /* Check if we have a connection to another splitter */
    for (final var splitter : splitters) {
      if (splitter.equals(splitterToIgnore)) continue;
      final var ends = splitter.getEnds();
      for (var end = 0; end < ends.size(); end++) {
        if (thisNet.contains(ends.get(end).getLocation())) {
          /* Here we have to process the inherited bits of the parent */
          final var busBitConnection = ((Splitter) splitter).getEndpoints();
          if (end == 0) {
            /* this is a main net, find the connected end */
            final var splitterEnd = busBitConnection[bitIndex];
            /* Find the corresponding Net index */
            Byte netIndex = 0;
            for (var index = 0; index < bitIndex; index++) {
              if (busBitConnection[index] == splitterEnd) netIndex++;
            }
            /* Find the connected Net */
            Net slaveNet = null;
            for (final var thisnet : myNets) {
              if (thisnet.contains(ends.get(splitterEnd).getLocation())) slaveNet = thisnet;
            }
            if (slaveNet != null) {
              final var ret = getHiddenSource(null, (byte) 0, slaveNet, netIndex, splitters, handledNets, segments, splitter);
              if (ret != null) return ret;
            }
          } else {
            final var rootIndices = new ArrayList<Byte>();
            for (byte b = 0; b < busBitConnection.length; b++) {
              if (busBitConnection[b] == end) rootIndices.add(b);
            }
            Net rootNet = null;
            for (final var thisnet : myNets) {
              if (thisnet.contains(splitter.getEnd(0).getLocation())) rootNet = thisnet;
            }
            if (rootNet != null) {
              final var ret = getHiddenSource(null, (byte) 0, rootNet, rootIndices.get(bitIndex), splitters, handledNets, segments, splitter);
              if (ret != null) return ret;
            }
          }
        }
      }
    }
    return null;
  }

  private boolean hasHiddenSource(
      Net fannoutNet,
      Byte fannoutBitIndex,
      Net combinedNet,
      Byte combinedBitIndex,
      List<Component> splitterList,
      Set<String> handledNets,
      Component ignoreSplitter) {
    // If the fannout net not is null add it to the set of visited nets to prevent back-search on
    // this net
    if (fannoutNet != null) {
      final var netId = myNets.indexOf(fannoutNet) + "-" + fannoutBitIndex;
      if (handledNets.contains(netId)) return false;
      handledNets.add(netId);
    }
    // to prevent deadlock situations we check if we already looked at this net
    final var netId = myNets.indexOf(combinedNet) + "-" + combinedBitIndex;
    if (handledNets.contains(netId)) return false;
    handledNets.add(netId);
    if (combinedNet.hasBitSource(combinedBitIndex)) return true;
    /* Check if we have a connection to another splitter */
    for (var currentSplitter : splitterList) {
      if (currentSplitter.equals(ignoreSplitter)) continue;
      final var ends = currentSplitter.getEnds();
      for (var end = 0; end < ends.size(); end++) {
        if (combinedNet.contains(ends.get(end).getLocation())) {
          /* Here we have to process the inherited bits of the parent */
          final var busBitConnection = ((Splitter) currentSplitter).getEndpoints();
          if (end == 0) {
            // This is a main net, find the connected end.
            var splitterEnd = busBitConnection[combinedBitIndex];
            /* Find the corresponding Net index */
            Byte netIndex = 0;
            for (var index = 0; index < combinedBitIndex; index++) {
              if (busBitConnection[index] == splitterEnd) netIndex++;
            }
            // Find the connected Net
            Net slaveNet = null;
            for (final var thisnet : myNets) {
              if (thisnet.contains(ends.get(splitterEnd).getLocation())) slaveNet = thisnet;
            }
            if (slaveNet != null && hasHiddenSource(null, (byte) 0, slaveNet, netIndex, splitterList, handledNets, currentSplitter))
              return true;
          } else {
            final var rootIndices = new ArrayList<Byte>();
            for (byte b = 0; b < busBitConnection.length; b++) {
              if (busBitConnection[b] == end) rootIndices.add(b);
            }
            Net rootNet = null;
            for (final var thisnet : myNets) {
              if (thisnet.contains(currentSplitter.getEnd(0).getLocation())) rootNet = thisnet;
            }
            if (rootNet != null
                && hasHiddenSource(null, (byte) 0, rootNet, rootIndices.get(combinedBitIndex), splitterList, handledNets, currentSplitter)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  // FIXME: This method name is very unfortunate.
  public boolean isContinuesBus(netlistComponent comp, int endIndex) {
    var continuesBus = true;
    if ((endIndex < 0) || (endIndex >= comp.nrOfEnds())) return true;

    final var connInfo = comp.getEnd(endIndex);
    final var nrOfBits = connInfo.getNrOfBits();
    if (nrOfBits == 1) return true;
    final var connectedNet = connInfo.get((byte) 0).getParentNet();
    var connectedNetIndex = connInfo.get((byte) 0).getParentNetBitIndex();
    for (var i = 1; (i < nrOfBits) && continuesBus; i++) {
      if (connectedNet != connInfo.get((byte) i).getParentNet())
        continuesBus = false; // This bit is connected to another bus
      if ((connectedNetIndex + 1) != connInfo.get((byte) i).getParentNetBitIndex()) {
        continuesBus = false; // Connected to a none incremental position of the bus
      } else {
        connectedNetIndex++;
      }
    }
    return continuesBus;
  }

  public boolean isValid() {
    return drcStatus == DRC_PASSED;
  }

  public void markClockNet(List<String> hierarchyNames, int clockSourceId, ConnectionPoint connection, boolean isPinClockSource) {
    myClockInformation.addClockNet(hierarchyNames, clockSourceId, connection, isPinClockSource);
  }

  public boolean markClockSourceComponents(List<String> hierarchyNames, List<Netlist> hierarchyNetlists, ClockSourceContainer clockSources) {
    //First pass: we go down the hierarchy till the leaves
    for (final var sub : mySubCircuits) {
      final var subFact = (SubcircuitFactory) sub.getComponent().getFactory();
      final var newHierarchyNames = new ArrayList<>(hierarchyNames);
      newHierarchyNames.add(
          CorrectLabel.getCorrectLabel(
              sub.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      final var newHierarchyNetlists = new ArrayList<>(hierarchyNetlists);
      newHierarchyNetlists.add(subFact.getSubcircuit().getNetList());
      if (!subFact.getSubcircuit()
          .getNetList()
          .markClockSourceComponents(newHierarchyNames, newHierarchyNetlists, clockSources)) {
        return false;
      }
    }
    // We see if some components require the Global fast FPGA clock
    for (final var comp : myCircuit.getNonWires()) {
      if (comp.getFactory().requiresGlobalClock()) clockSources.setRequiresFpgaGlobalClock();
    }
    /* Second pass: We mark all clock sources */
    for (final var clockSource : myClockGenerators) {
      if (clockSource.nrOfEnds() != 1) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Found a clock source with more than 1 connection\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return false;
      }
      final var clockConnection = clockSource.getEnd(0);
      if (clockConnection.getNrOfBits() != 1) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Found a clock source with a bus as output\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return false;
      }
      final var solderPoint = clockConnection.get((byte) 0);
      /* Check if the clock source is connected */
      if (solderPoint.getParentNet() != null) {
        /* Third pass: add this clock to the list of ClockSources */
        final var clockid = clockSources.getClockId(clockSource.getComponent());
        /* Forth pass: Add this source as clock source to the tree */
        myClockInformation.addClockSource(hierarchyNames, clockid, solderPoint);
        /* Fifth pass: trace the clock net all the way */
        if (!traceClockNet(
            solderPoint.getParentNet(),
            solderPoint.getParentNetBitIndex(),
            clockid,
            false,
            hierarchyNames,
            hierarchyNetlists)) {
          return false;
        }
      }
    }

    return true;
  }

  public boolean netlistHasShortCircuits() {
    var ret = false;
    for (final var net : myNets) {
      if (net.isRootNet()) {
        if (net.hasShortCircuit()) {
          final var error =
              new SimpleDrcContainer(
                  myCircuit,
                  S.get("NetList_ShortCircuit"),
                  SimpleDrcContainer.LEVEL_FATAL,
                  SimpleDrcContainer.MARK_WIRE);
          error.addMarkComponents(net.getWires());
          Reporter.report.addError(error);
          ret = true;
        } else if (net.getBitWidth() == 1 && net.getSourceNets(0).size() > 1) {
          // We have to check if the net is connected to multiple drivers
          final var sourceNets = net.getSourceNets(0);
          final var sourceConnections = new HashMap<Component, Integer>();
          final var segments = new HashSet<Wire>(net.getWires());
          var foundShortCrcuit = false;
          final var error = new SimpleDrcContainer(myCircuit, S.get("NetList_ShortCircuit"), SimpleDrcContainer.LEVEL_FATAL, SimpleDrcContainer.MARK_WIRE | SimpleDrcContainer.MARK_INSTANCE);
          for (ConnectionPoint sourceNet : sourceNets) {
            final var connectedNet = sourceNet.getParentNet();
            final byte bitIndex = sourceNet.getParentNetBitIndex();
            if (hasHiddenSource(net, (byte) 0, connectedNet, bitIndex, mySplitters, new HashSet<>(), null)) {
              final var source = getHiddenSource(net, (byte) 0, connectedNet, bitIndex, mySplitters, new HashSet<>(), segments, null);
              if (source == null) return true; // this should never happen
              final var comp = source.getSource().getComp();
              for (final var seg : segments) error.addMarkComponent(seg);
              error.addMarkComponent(comp);
              final var index = source.getIndex();
              foundShortCrcuit |= (sourceConnections.containsKey(comp) && sourceConnections.get(comp) != index) || (sourceConnections.keySet().size() > 0);
              sourceConnections.put(comp, index);
            }
          }
          if (foundShortCrcuit) {
            ret = true;
            Reporter.report.addError(error);
          } else {
            net.cleanupSourceNets(0);
          }
        }
      }
    }
    return ret;
  }

  public boolean netlistHasSinksWithoutSource() {
    /* First pass: we make a set with all sinks */
    final var mySinks = new HashSet<ConnectionPoint>();
    for (final var thisNet : myNets) {
      if (thisNet.isRootNet()) mySinks.addAll(thisNet.getSinks());
    }
    /* Second pass: we iterate along all the sources */
    for (final var thisNet : myNets) {
      if (thisNet.isRootNet()) {
        for (var i = 0; i < thisNet.getBitWidth(); i++) {
          if (thisNet.hasBitSource(i)) {
            var hasSink = false;
            final var sinks = thisNet.getBitSinks(i);
            hasSink |= !sinks.isEmpty();
            sinks.forEach(mySinks::remove);
            final var hiddenSinkNets = getHiddenSinks(thisNet, (byte) i, mySplitters, new HashSet<>(), true);
            hasSink |= !hiddenSinkNets.isEmpty();
            hiddenSinkNets.forEach(mySinks::remove);
            if (!hasSink) {
              final var warn =
                  new SimpleDrcContainer(
                      myCircuit,
                      S.get("NetList_SourceWithoutSink"),
                      SimpleDrcContainer.LEVEL_NORMAL,
                      SimpleDrcContainer.MARK_WIRE);
              warn.addMarkComponents(thisNet.getWires());
              Reporter.report.addWarning(warn);
            }
          }
        }
      }
    }
    if (mySinks.size() != 0) {
      for (final var sink : mySinks) {
        final var warn =
            new SimpleDrcContainer(
                    myCircuit,
                    S.get("NetList_UnsourcedSink"),
                    SimpleDrcContainer.LEVEL_SEVERE,
                    SimpleDrcContainer.MARK_INSTANCE | SimpleDrcContainer.MARK_WIRE);
        warn.addMarkComponents(sink.getParentNet().getWires());
        if (sink.getComp() != null) warn.addMarkComponent(sink.getComp());
        Reporter.report.addWarning(warn);
      }
    }
    return false;
  }

  public int numberOfBusses() {
    var nrOfBusses = 0;
    for (final var thisNet : myNets) {
      if (thisNet.isRootNet() && thisNet.isBus()) nrOfBusses++;
    }
    return nrOfBusses;
  }

  public int numberOfClockTrees() {
    return myClockInformation.getSourceContainer().getNrofSources();
  }

  public int numberOfInOutBubbles() {
    return localNrOfInOutBubbles;
  }

  public int numberOfInOutPortBits() {
    var count = 0;
    for (final var inp : myInOutPorts) count += inp.getEnd(0).getNrOfBits();
    return count;
  }

  public int numberOfInOutPorts() {
    return myInOutPorts.size();
  }

  public int getNumberOfInputBubbles() {
    return localNrOfInportBubbles;
  }

  public int getNumberOfInputPortBits() {
    var count = 0;
    for (final var inPort : myInputPorts) count += inPort.getEnd(0).getNrOfBits();
    return count;
  }

  public int getNumberOfInputPorts() {
    return myInputPorts.size();
  }

  public int numberOfNets() {
    var nrOfNets = 0;
    for (final var thisNet : myNets) {
      if (thisNet.isRootNet() && !thisNet.isBus()) nrOfNets++;
    }
    return nrOfNets;
  }

  public int numberOfOutputBubbles() {
    return localNrOfOutportBubbles;
  }

  public int numberOfOutputPortBits() {
    var count = 0;
    for (final var outPort : myOutputPorts) count += outPort.getEnd(0).getNrOfBits();
    return count;
  }

  public int numberOfOutputPorts() {
    return myOutputPorts.size();
  }

  private boolean processNormalComponent(Component comp) {
    final var normalComponent = new netlistComponent(comp);
    for (final var thisPin : comp.getEnds()) {
      final var connection = findConnectedNet(thisPin.getLocation());
      if (connection != null) {
        final var pinId = comp.getEnds().indexOf(thisPin);
        final var pinIsSink = thisPin.isInput();
        final var thisEnd = normalComponent.getEnd(pinId);
        final var rootNet = getRootNet(connection);
        if (rootNet == null) {
          Reporter.report.addFatalErrorFmt(
              "BUG: Unable to find a root net for a normal component\n ==> %s:%d\n",
              this.getClass().getName().replace(".", "/"),
              Thread.currentThread().getStackTrace()[2].getLineNumber());
          return false;
        }
        for (var bitid = 0; bitid < thisPin.getWidth().getWidth(); bitid++) {
          final var rootNetBitIndex = getRootNetIndex(connection, (byte) bitid);
          if (rootNetBitIndex < 0) {
            Reporter.report.addFatalErrorFmt(
                // FIXME: Some "BUG:" have 2 spaces, other just 1. Is this intentional or all can have 1 (multiple places)?
                "BUG:  Unable to find a root-net bit-index for a normal component\n ==> %s:%d\n",
                this.getClass().getName().replace(".", "/"),
                Thread.currentThread().getStackTrace()[2].getLineNumber());
            return false;
          }
          final var thisSolderPoint = thisEnd.get((byte) bitid);
          thisSolderPoint.setParentNet(rootNet, rootNetBitIndex);
          if (pinIsSink) {
            rootNet.addSink(rootNetBitIndex, thisSolderPoint);
          } else {
            rootNet.addSource(rootNetBitIndex, thisSolderPoint);
          }
        }
      }
    }
    if (comp.getFactory() instanceof Clock) {
      myClockGenerators.add(normalComponent);
    } else if (comp.getFactory() instanceof Pin) {
      if (comp.getEnd(0).isInput()) {
        myOutputPorts.add(normalComponent);
      } else {
        myInputPorts.add(normalComponent);
      }
    } else {
      myComponents.add(normalComponent);
    }
    return true;
  }

  private boolean processSubcircuit(Component comp) {
    final var subCircuit = new netlistComponent(comp);
    final var subFactory = (SubcircuitFactory) comp.getFactory();
    final var subPins = ((CircuitAttributes) comp.getAttributeSet()).getPinInstances();
    final var subNetlist = subFactory.getSubcircuit().getNetList();
    for (final var thisPin : comp.getEnds()) {
      final var connection = findConnectedNet(thisPin.getLocation());
      final var pinId = comp.getEnds().indexOf(thisPin);
      final var subPortIndex = subNetlist.getPortInfo(subPins[pinId].getAttributeValue(StdAttr.LABEL));
      if (subPortIndex < 0) {
        Reporter.report.addFatalErrorFmt(
            "BUG:  Unable to find pin in sub-circuit\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return false;
      }
      if (connection != null) {
        var pinIsSink = thisPin.isInput();
        final var rootNet = getRootNet(connection);
        if (rootNet == null) {
          Reporter.report.addFatalErrorFmt(
              "BUG:  Unable to find a root net for sub-circuit\n ==> %s:%d\n",
              this.getClass().getName().replace(".", "/"),
              Thread.currentThread().getStackTrace()[2].getLineNumber());
          return false;
        }
        for (byte bitid = 0; bitid < thisPin.getWidth().getWidth(); bitid++) {
          final var rootNetBitIndex = getRootNetIndex(connection, bitid);
          if (rootNetBitIndex < 0) {
            Reporter.report.addFatalErrorFmt(
                "BUG:  Unable to find a root-net bit-index for sub-circuit\n ==> %s:%d\n",
                this.getClass().getName().replace(".", "/"),
                Thread.currentThread().getStackTrace()[2].getLineNumber());
            return false;
          }
          subCircuit.getEnd(pinId).get(bitid).setParentNet(rootNet, rootNetBitIndex);
          if (pinIsSink) {
            rootNet.addSink(rootNetBitIndex, subCircuit.getEnd(pinId).get(bitid));
          } else {
            rootNet.addSource(rootNetBitIndex, subCircuit.getEnd(pinId).get(bitid));
          }
          // Special handling for sub-circuits; we have to find out  the connection to the
          // corresponding net in the underlying net-list; At this point the underlying net-lists
          // have already been generated.
          subCircuit.getEnd(pinId).get(bitid).setChildsPortIndex(subPortIndex);
        }
      } else {
        for (byte bitid = 0; bitid < thisPin.getWidth().getWidth(); bitid++)
          subCircuit.getEnd(pinId).get(bitid).setChildsPortIndex(subPortIndex);
      }
    }
    mySubCircuits.add(subCircuit);
    return true;
  }

  public String projName() {
    return myCircuit.getProjName();
  }

  public boolean requiresGlobalClockConnection() {
    return myClockInformation.getSourceContainer().getRequiresFpgaGlobalClock();
  }

  public void setCurrentHierarchyLevel(List<String> level) {
    currentHierarchyLevel.clear();
    currentHierarchyLevel.addAll(level);
  }

  private boolean traceDownSubcircuit(ConnectionPoint point, int clockSourceId, List<String> hierarchyNames, List<Netlist> hierarchyNetlists) {
    if (point.getChildsPortIndex() < 0) {
      Reporter.report.addFatalErrorFmt(
          "BUG: Subcircuit port is not annotated!\n ==> %s:%d\n",
          this.getClass().getName().replace(".", "/"),
          Thread.currentThread().getStackTrace()[2].getLineNumber());
      return false;
    }
    final var sub = (SubcircuitFactory) point.getComp().getFactory();
    final var inputPort = sub.getSubcircuit().getNetList().getInputPin(point.getChildsPortIndex());
    if (inputPort == null) {
      Reporter.report.addFatalErrorFmt(
          "BUG: Unable to find Subcircuit input port!\n ==> %s:%d\n",
          this.getClass().getName().replace(".", "/"),
          Thread.currentThread().getStackTrace()[2].getLineNumber());
      return false;
    }
    final var subCirc = getSubCirc(point.getComp());
    if (subCirc == null) {
      Reporter.report.addFatalErrorFmt(
          "BUG: Unable to find Subcircuit!\n ==> %s:%d\n",
          this.getClass().getName().replace(".", "/"),
          Thread.currentThread().getStackTrace()[2].getLineNumber());
      return false;
    }
    final var bitindex = subCirc.getConnectionBitIndex(point.getParentNet(), point.getParentNetBitIndex());
    if (bitindex < 0) {
      Reporter.report.addFatalErrorFmt(
          "BUG: Unable to find the bit index of a Subcircuit input port!\n ==> %s:%d\n",
          this.getClass().getName().replace(".", "/"),
          Thread.currentThread().getStackTrace()[2].getLineNumber());
      return false;
    }
    final var subClockNet = inputPort.getEnd(0).get(bitindex);
    if (subClockNet.getParentNet() != null) {
      /* we have a connected pin */
      final var newHierarchyNames = new ArrayList<>(hierarchyNames);
      final var label =
          CorrectLabel.getCorrectLabel(
              subCirc.getComponent().getAttributeSet().getValue(StdAttr.LABEL));
      newHierarchyNames.add(label);
      final var newHierarchyNetlists = new ArrayList<>(hierarchyNetlists);
      newHierarchyNetlists.add(sub.getSubcircuit().getNetList());
      sub.getSubcircuit()
          .getNetList()
          .markClockNet(newHierarchyNames, clockSourceId, subClockNet, true);
      return sub.getSubcircuit()
          .getNetList()
          .traceClockNet(
              subClockNet.getParentNet(),
              subClockNet.getParentNetBitIndex(),
              clockSourceId,
              true,
              newHierarchyNames,
              newHierarchyNetlists);
    }
    return true;
  }

  public boolean traceClockNet(Net clockNet, byte clockNetBitIndex, int clockSourceId, boolean isPinSource, List<String> hierarchyNames, List<Netlist> hierarchyNetlists) {
    final var hiddenComps = getHiddenSinks(clockNet, clockNetBitIndex, mySplitters, new HashSet<>(), false);
    for (final var point : hiddenComps) {
      markClockNet(hierarchyNames, clockSourceId, point, isPinSource);
      if (point.getComp().getFactory() instanceof SubcircuitFactory)
        if (!traceDownSubcircuit(point, clockSourceId, hierarchyNames, hierarchyNetlists)) return false;
      /* On top level we do not have to go up */
      if (hierarchyNames.isEmpty()) continue;
      if (point.getComp().getFactory() instanceof Pin) {
        final var outputPort = getOutPort(point.getComp());
        if (outputPort == null) {
          Reporter.report.addFatalErrorFmt(
              "BUG: Could not find an output port!\n ==> %s:%d\n",
              this.getClass().getName().replace(".", "/"),
              Thread.currentThread().getStackTrace()[2].getLineNumber());
          return false;
        }
        final var bitIndex = outputPort.getConnectionBitIndex(point.getParentNet(), point.getParentNetBitIndex());
        final var subClockNet =
            hierarchyNetlists.get(hierarchyNetlists.size() - 2)
                .getNetlistConnectionForSubCircuit(
                    hierarchyNames.get(hierarchyNames.size() - 1),
                    myOutputPorts.indexOf(outputPort),
                    bitIndex);
        if (subClockNet == null) {
          Reporter.report.addFatalErrorFmt(
              "BUG: Could not find a sub-circuit connection in overlying hierarchy level!\n ==> %s:%d\n",
              this.getClass().getName().replace(".", "/"),
              Thread.currentThread().getStackTrace()[2].getLineNumber());
          return false;
        }
        if (subClockNet.getParentNet() == null) {
        } else {
          final var newHierarchyNames = new ArrayList<String>(hierarchyNames);
          newHierarchyNames.remove(newHierarchyNames.size() - 1);
          final var newHierarchyNetlists = new ArrayList<>(hierarchyNetlists);
          newHierarchyNetlists.remove(newHierarchyNetlists.size() - 1);
          hierarchyNetlists.get(hierarchyNetlists.size() - 2).markClockNet(newHierarchyNames, clockSourceId, subClockNet, true);
          if (!hierarchyNetlists.get(hierarchyNetlists.size() - 2)
              .traceClockNet(
                  subClockNet.getParentNet(),
                  subClockNet.getParentNetBitIndex(),
                  clockSourceId,
                  true,
                  newHierarchyNames,
                  newHierarchyNetlists)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private netlistComponent getSubCirc(Component comp) {
    for (final var current : mySubCircuits) {
      if (current.getComponent().equals(comp)) return current;
    }
    return null;
  }

  private netlistComponent getOutPort(Component comp) {
    for (final var current : myOutputPorts) {
      if (current.getComponent().equals(comp)) return current;
    }
    return null;
  }

  private boolean detectGatedClocks() {
    // First Pass: We gather a complete information tree about components with clock inputs and
    // their connected nets in    case it is not a clock net. The moment we call this function the
    // clock tree has been marked already!
    final var root = new ArrayList<Netlist>();
    var suppress = AppPreferences.SupressGatedClockWarnings.getBoolean();
    root.add(this);
    final var notGatedSet = new HashMap<String, Map<netlistComponent, Circuit>>();
    final var gatedSet = new HashMap<String, Map<netlistComponent, Circuit>>();
    setCurrentHierarchyLevel(new ArrayList<>());
    getGatedClockComponents(root, null, notGatedSet, gatedSet, new HashSet<>());
    for (final var key : notGatedSet.keySet()) {
      if (gatedSet.containsKey(key) && !suppress) {
        /* big Problem, we have a component that is used with and without gated clocks */
        Reporter.report.addSevereWarning(S.get("NetList_CircuitGatedNotGated"));
        Reporter.report.addWarningIncrement(S.get("NetList_TraceListBegin"));
        Map<netlistComponent, Circuit> instances = notGatedSet.get(key);
        for (final var comp : instances.keySet()) {
          final var warn =
              new SimpleDrcContainer(
                  instances.get(comp),
                  S.get("NetList_CircuitNotGated"),
                  SimpleDrcContainer.LEVEL_NORMAL,
                  SimpleDrcContainer.MARK_INSTANCE,
                  true);
          warn.addMarkComponent(comp.getComponent());
          Reporter.report.addWarning(warn);
        }
        instances = gatedSet.get(key);
        for (final var comp : instances.keySet()) {
          comp.setIsGatedInstance();
          final var warn =
              new SimpleDrcContainer(
                      instances.get(comp),
                      S.get("NetList_CircuitGated"),
                      SimpleDrcContainer.LEVEL_NORMAL,
                      SimpleDrcContainer.MARK_INSTANCE,
                      true);
          warn.addMarkComponent(comp.getComponent());
          Reporter.report.addWarning(warn);
        }
        Reporter.report.addWarningIncrement(S.get("NetList_TraceListEnd"));
      }
    }
    return true;
  }

  public void getGatedClockComponents(
      List<Netlist> hierarchyNetlists,
      netlistComponent subCircuit,
      Map<String, Map<netlistComponent, Circuit>> notGatedSet,
      Map<String, Map<netlistComponent, Circuit>> gatedSet,
      Set<netlistComponent> warnedComponents) {
    /* First pass: we go down the tree */
    for (final var subCirc : mySubCircuits) {
      final var sub = (SubcircuitFactory) subCirc.getComponent().getFactory();
      final var newHierarchyNames = new ArrayList<>(getCurrentHierarchyLevel());
      newHierarchyNames.add(
          CorrectLabel.getCorrectLabel(
              subCirc.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
      final var newHierarchyNetlists = new ArrayList<>(hierarchyNetlists);
      newHierarchyNetlists.add(sub.getSubcircuit().getNetList());
      sub.getSubcircuit().getNetList().setCurrentHierarchyLevel(newHierarchyNames);
      sub.getSubcircuit()
          .getNetList()
          .getGatedClockComponents(newHierarchyNetlists, subCirc, notGatedSet, gatedSet, warnedComponents);
    }
    // Second pass: we find all components with a clock input and see if they are
    // connected to a clock.
    var gatedClock = false;
    final var pinSources = new ArrayList<SourceInfo>();
    final var pinWires = new ArrayList<Set<Wire>>();
    final var pinGatedComponents = new ArrayList<Set<netlistComponent>>();
    final var nonPinSources = new ArrayList<SourceInfo>();
    final var nonPinWires = new ArrayList<Set<Wire>>();
    final var nonPinGatedComponents = new ArrayList<Set<netlistComponent>>();
    for (final var comp : myComponents) {
      final var fact = comp.getComponent().getFactory();
      if (fact.checkForGatedClocks(comp)) {
        final var clockPins = fact.clockPinIndex(comp);
        for (final var clockPin : clockPins)
          gatedClock |=
              hasGatedClock(
                  comp,
                  clockPin,
                  pinSources,
                  pinWires,
                  pinGatedComponents,
                  nonPinSources,
                  nonPinWires,
                  nonPinGatedComponents,
                  warnedComponents);
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

    final var myName = CorrectLabel.getCorrectLabel(circuitName);
    if (hierarchyNetlists.size() > 1) {
      if (gatedClock && pinSources.isEmpty()) {
        gatedClock = false; // we have only non-pin driven gated clocks
        warningForGatedClock(
            nonPinSources,
            nonPinGatedComponents,
            nonPinWires,
            warnedComponents,
            hierarchyNetlists,
            S.get("NetList_GatedClock"));
      }

      if (gatedClock && !pinSources.isEmpty() && !AppPreferences.SupressGatedClockWarnings.getBoolean()) {
        for (var i = 0; i < pinSources.size(); i++) {
          Reporter.report.addSevereWarning(S.get("NetList_GatedClock"));
          Reporter.report.addWarningIncrement(S.get("NetList_TraceListBegin"));
          final var warn =
              new SimpleDrcContainer(
                      myCircuit,
                      S.get("NetList_GatedClockSink"),
                      SimpleDrcContainer.LEVEL_NORMAL,
                      SimpleDrcContainer.MARK_INSTANCE | SimpleDrcContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(pinWires.get(i));
          for (final var comp : pinGatedComponents.get(i)) warn.addMarkComponent(comp.getComponent());
          Reporter.report.addWarning(warn);
          warningTraceForGatedClock(
              pinSources.get(i).getSource(),
              pinSources.get(i).getIndex(),
              hierarchyNetlists,
              currentHierarchyLevel);
          Reporter.report.addWarningIncrement(S.get("NetList_TraceListEnd"));
        }
      }

      /* we only mark if we are not at top-level */
      if (gatedClock) {
        if (gatedSet.containsKey(myName))
          gatedSet.get(myName).put(subCircuit, hierarchyNetlists.get(hierarchyNetlists.size() - 2).getCircuit());
        else {
          final var newList = new HashMap<netlistComponent, Circuit>();
          newList.put(subCircuit, hierarchyNetlists.get(hierarchyNetlists.size() - 2).getCircuit());
          gatedSet.put(myName, newList);
        }
      } else {
        if (notGatedSet.containsKey(myName))
          notGatedSet.get(myName).put(subCircuit, hierarchyNetlists.get(hierarchyNetlists.size() - 2).getCircuit());
        else {
          final var newList = new HashMap<netlistComponent, Circuit>();
          newList.put(subCircuit, hierarchyNetlists.get(hierarchyNetlists.size() - 2).getCircuit());
          notGatedSet.put(myName, newList);
        }
      }
    } else {
      /* At toplevel we warn for all possible gated clocks */
      warningForGatedClock(nonPinSources, nonPinGatedComponents, nonPinWires, warnedComponents, hierarchyNetlists, S.get("NetList_GatedClock"));
      warningForGatedClock(pinSources, pinGatedComponents, pinWires, warnedComponents, hierarchyNetlists, S.get("NetList_PossibleGatedClock"));
    }
  }

  private boolean hasGatedClock(
      netlistComponent comp,
      int clockPinIndex,
      List<SourceInfo> pinSources,
      List<Set<Wire>> pinWires,
      List<Set<netlistComponent>> pinGatedComponents,
      List<SourceInfo> nonPinSources,
      List<Set<Wire>> nonPinWires,
      List<Set<netlistComponent>> nonPinGatedComponents,
      Set<netlistComponent> warnedComponents) {
    var isGatedClock = false;
    final var clockNetName = Hdl.getClockNetName(comp, clockPinIndex, this);
    if (clockNetName.isEmpty()) {
      /* we search for the source in case it is connected otherwise we ignore */
      final var connection = comp.getEnd(clockPinIndex).get((byte) 0);
      final var connectedNet = connection.getParentNet();
      final var connectedNetindex = connection.getParentNetBitIndex();
      if (connectedNet != null) {
        isGatedClock = true;
        final var segments = new HashSet<Wire>();
        final var source = getHiddenSource(null, (byte) 0, connectedNet, connectedNetindex, mySplitters, new HashSet<>(), segments, null);
        final var sourceCon = source.getSource();
        if (sourceCon.getComp().getFactory() instanceof Pin) {
          var index = getEntryIndex(pinSources, sourceCon, (int) connectedNetindex);
          if (index < 0) {
            pinSources.add(source);
            pinWires.add(segments);
            final var comps = new HashSet<netlistComponent>();
            comps.add(comp);
            comps.add(new netlistComponent(sourceCon.getComp()));
            pinGatedComponents.add(comps);
          } else {
            pinGatedComponents.get(index).add(comp);
          }
        } else {
          int index = getEntryIndex(nonPinSources, sourceCon, (int) connectedNetindex);
          if (index < 0) {
            nonPinSources.add(source);
            nonPinWires.add(segments);
            final var comps = new HashSet<netlistComponent>();
            comps.add(comp);
            nonPinGatedComponents.add(comps);
          } else {
            nonPinGatedComponents.get(index).add(comp);
          }
        }
      } else {
        /* Add severe warning, we found a sequential element with an unconnected clock input */
        if (!warnedComponents.contains(comp)) {
          final var warn =
              new SimpleDrcContainer(
                  myCircuit,
                  S.get("NetList_NoClockConnection"),
                  SimpleDrcContainer.LEVEL_SEVERE,
                  SimpleDrcContainer.MARK_INSTANCE);
          warn.addMarkComponent(comp.getComponent());
          Reporter.report.addWarning(warn);
          warnedComponents.add(comp);
        }
      }
    }
    return isGatedClock;
  }

  private int getEntryIndex(List<SourceInfo> searchList, ConnectionPoint connection, Integer index) {
    var result = -1;
    for (var i = 0; i < searchList.size(); i++) {
      final var thisEntry = searchList.get(i);
      if (thisEntry.getSource().equals(connection) && thisEntry.getIndex().equals(index))
        result = i;
    }
    return result;
  }

  private void warningTraceForGatedClock(ConnectionPoint sourcePoint, int index, List<Netlist> hierarchyNetlists, List<String> hierarchyNames) {
    final var comp = sourcePoint.getComp();
    if (comp.getFactory() instanceof Pin) {
      if (hierarchyNames.isEmpty())
        /* we cannot go up at toplevel, so leave */
        return;
      var idx = -1;
      for (var i = 0; i < myInputPorts.size(); i++) {
        if (myInputPorts.get(i).getComponent().equals(comp)) idx = i;
      }
      if (idx < 0) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Could not find port!\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return;
      }
      final var subNet =
          hierarchyNetlists
              .get(hierarchyNetlists.size() - 2)
              .getNetlistConnectionForSubCircuitInput(
                  hierarchyNames.get(hierarchyNames.size() - 1), idx, (byte) index);
      if (subNet == null) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Could not find a sub-circuit connection in overlying hierarchy level!\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return;
      }
      if (subNet.getParentNet() != null) {
        final var newHierarchyNames = new ArrayList<>(hierarchyNames);
        newHierarchyNames.remove(newHierarchyNames.size() - 1);
        final var newHierarchyNetlists = new ArrayList<>(hierarchyNetlists);
        newHierarchyNetlists.remove(newHierarchyNetlists.size() - 1);
        final var subNetList = hierarchyNetlists.get(hierarchyNetlists.size() - 2);
        final var newNet = subNet.getParentNet();
        final var newNetIndex = subNet.getParentNetBitIndex();
        final var segments = new HashSet<Wire>();
        final var source = subNetList.getHiddenSource(null, (byte) 0, newNet, newNetIndex, subNetList.mySplitters, new HashSet<>(), segments, null);
        if (source == null) {
          Reporter.report.addFatalErrorFmt(
              "BUG: Unable to find source in sub-circuit!\n ==> %s:%d\n",
              this.getClass().getName().replace(".", "/"),
              Thread.currentThread().getStackTrace()[2].getLineNumber());
          return;
        }
        final var sfac = source.getSource().getComp().getFactory();
        if (sfac instanceof Pin || sfac instanceof SubcircuitFactory) {
          final var warn =
              new SimpleDrcContainer(
                      subNetList.getCircuit(),
                      S.get("NetList_GatedClockInt"),
                      SimpleDrcContainer.LEVEL_NORMAL,
                      SimpleDrcContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(segments);
          Reporter.report.addWarning(warn);
          subNetList.warningTraceForGatedClock(
              source.getSource(),
              source.getIndex(),
              newHierarchyNetlists,
              newHierarchyNames);
        } else {
          final var warn =
              new SimpleDrcContainer(
                      subNetList.getCircuit(),
                      S.get("NetList_GatedClockSource"),
                      SimpleDrcContainer.LEVEL_NORMAL,
                      SimpleDrcContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(segments);
          Reporter.report.addWarning(warn);
        }
      }
    }
    if (comp.getFactory() instanceof SubcircuitFactory) {
      final var sub = (SubcircuitFactory) comp.getFactory();
      if (sourcePoint.getChildsPortIndex() < 0) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Subcircuit port is not annotated!\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return;
      }
      final var outputPort = sub.getSubcircuit().getNetList().getOutputPin(sourcePoint.getChildsPortIndex());
      if (outputPort == null) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Unable to find Subcircuit output port!\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return;
      }
      final var connectedNet = sourcePoint.getParentNet();
      /* Find the correct subcircuit */
      netlistComponent subCirc = null;
      for (final var circ : mySubCircuits) {
        if (circ.getComponent().equals(sourcePoint.getComp())) subCirc = circ;
      }
      if (subCirc == null) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Unable to find Subcircuit!\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return;
      }

      final var bitIndex = subCirc.getConnectionBitIndex(connectedNet, (byte) index);
      if (bitIndex < 0) {
        Reporter.report.addFatalErrorFmt(
            "BUG: Unable to find the bit index of a Subcircuit output port!\n ==> %s:%d\n",
            this.getClass().getName().replace(".", "/"),
            Thread.currentThread().getStackTrace()[2].getLineNumber());
        return;
      }
      final var subNet = outputPort.getEnd(0).get(bitIndex);
      if (subNet.getParentNet() != null) {
        /* we have a connected pin */
        final var subNetList = sub.getSubcircuit().getNetList();
        final var newHierarchyNames = new ArrayList<>(hierarchyNames);
        newHierarchyNames.add(
            CorrectLabel.getCorrectLabel(
                subCirc.getComponent().getAttributeSet().getValue(StdAttr.LABEL)));
        final var newHierarchyNetlists = new ArrayList<>(hierarchyNetlists);
        newHierarchyNetlists.add(subNetList);
        final var newNet = subNet.getParentNet();
        final var newNetIndex = subNet.getParentNetBitIndex();
        final var segments = new HashSet<Wire>();
        final var source =
            subNetList.getHiddenSource(
                null,
                (byte) 0,
                newNet,
                newNetIndex,
                subNetList.mySplitters,
                new HashSet<>(),
                segments,
                null);
        if (source == null) {
          Reporter.report.addFatalErrorFmt(
              "BUG: Unable to find source in sub-circuit!\n ==> %s:%d\n",
              this.getClass().getName().replace(".", "/"),
              Thread.currentThread().getStackTrace()[2].getLineNumber());
          return;
        }
        final var sfac = source.getSource().getComp().getFactory();
        if (sfac instanceof Pin || sfac instanceof SubcircuitFactory) {
          final var warn =
              new SimpleDrcContainer(
                      subNetList.getCircuit(),
                      S.get("NetList_GatedClockInt"),
                      SimpleDrcContainer.LEVEL_NORMAL,
                      SimpleDrcContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(segments);
          Reporter.report.addWarning(warn);
          subNetList.warningTraceForGatedClock(
              source.getSource(),
              source.getIndex(),
              newHierarchyNetlists,
              newHierarchyNames);
        } else {
          final var warn =
              new SimpleDrcContainer(
                      subNetList.getCircuit(),
                      S.get("NetList_GatedClockSource"),
                      SimpleDrcContainer.LEVEL_NORMAL,
                      SimpleDrcContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(segments);
          Reporter.report.addWarning(warn);
        }
      }
    }
  }

  private void warningForGatedClock(
      List<SourceInfo> sources,
      List<Set<netlistComponent>> components,
      List<Set<Wire>> wires,
      Set<netlistComponent> warnedComponents,
      List<Netlist> hierarchyNetlists,
      String warning) {
    if (AppPreferences.SupressGatedClockWarnings.getBoolean()) return;
    for (var i = 0; i < sources.size(); i++) {
      var alreadyWarned = false;
      for (final var comp : components.get(i))
        alreadyWarned |= warnedComponents.contains(comp);
      if (!alreadyWarned) {
        if (sources.get(i).getSource().getComp().getFactory() instanceof SubcircuitFactory) {
          Reporter.report.addSevereWarning(S.get("NetList_GatedClock"));
          Reporter.report.addWarningIncrement(S.get("NetList_TraceListBegin"));
          final var warn =
              new SimpleDrcContainer(
                      myCircuit,
                      S.get("NetList_GatedClockSink"),
                      SimpleDrcContainer.LEVEL_NORMAL,
                      SimpleDrcContainer.MARK_INSTANCE | SimpleDrcContainer.MARK_WIRE,
                      true);
          warn.addMarkComponents(wires.get(i));
          for (final var comp : components.get(i)) warn.addMarkComponent(comp.getComponent());
          Reporter.report.addWarning(warn);
          warningTraceForGatedClock(
              sources.get(i).getSource(),
              sources.get(i).getIndex(),
              hierarchyNetlists,
              currentHierarchyLevel);
          Reporter.report.addWarningIncrement(S.get("NetList_TraceListEnd"));
        } else {
          final var warn =
              new SimpleDrcContainer(
                  myCircuit,
                  warning,
                  SimpleDrcContainer.LEVEL_SEVERE,
                  SimpleDrcContainer.MARK_INSTANCE | SimpleDrcContainer.MARK_WIRE);
          for (final var comp : components.get(i))
            warn.addMarkComponent(comp.getComponent());
          warn.addMarkComponents(wires.get(i));
          Reporter.report.addWarning(warn);
        }
        warnedComponents.addAll(components.get(i));
      }
    }
  }

  public static boolean isFlipFlop(AttributeSet attrs) {
    if (attrs.containsAttribute(StdAttr.EDGE_TRIGGER)) return true;
    if (attrs.containsAttribute(StdAttr.TRIGGER))
      return ((attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING)
          || (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING));
    return false;
  }
}
