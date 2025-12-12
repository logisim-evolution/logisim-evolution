/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.circuit.appear.CircuitAppearance;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.FailException;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.std.memory.Rom;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Tunnel;
import com.cburch.logisim.tools.LibraryTools;
import com.cburch.logisim.tools.SetAttributeAction;
import com.cburch.logisim.util.AutoLabel;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.WeakHashMap;

public class Circuit {
  private class EndChangedTransaction extends CircuitTransaction {
    private final Component comp;
    private final Map<Location, EndData> toRemove;
    private final Map<Location, EndData> toAdd;

    EndChangedTransaction(
        Component comp, Map<Location, EndData> toRemove, Map<Location, EndData> toAdd) {
      this.comp = comp;
      this.toRemove = toRemove;
      this.toAdd = toAdd;
    }

    @Override
    protected Map<Circuit, Integer> getAccessedCircuits() {
      return Collections.singletonMap(Circuit.this, READ_WRITE);
    }

    @Override
    protected void run(CircuitMutator mutator) {
      for (final var loc : toRemove.keySet()) {
        final var removed = toRemove.get(loc);
        final var replaced = toAdd.remove(loc);
        if (replaced == null) {
          wires.remove(comp, removed);
        } else if (!replaced.equals(removed)) {
          wires.replace(comp, removed, replaced);
        }
      }
      for (final var end : toAdd.values()) {
        wires.add(comp, end);
      }
      ((CircuitMutatorImpl) mutator).markModified(Circuit.this);
    }
  }

  private class MyComponentListener implements ComponentListener {
    @Override
    public void componentInvalidated(ComponentEvent e) {
      fireEvent(CircuitEvent.ACTION_INVALIDATE, e.getSource());
    }

    @Override
    public void endChanged(ComponentEvent e) {
      locker.checkForWritePermission("ends changed", Circuit.this);
      isAnnotated = false;
      myNetList.clear();
      final var comp = e.getSource();
      final var toRemove = toMap(e.getOldData());
      final var toAdd = toMap(e.getData());
      final var xn = new EndChangedTransaction(comp, toRemove, toAdd);
      locker.execute(xn);
      fireEvent(CircuitEvent.ACTION_INVALIDATE, comp);
    }

    private HashMap<Location, EndData> toMap(Object val) {
      final var map = new HashMap<Location, EndData>();
      if (val instanceof List) {
        @SuppressWarnings("unchecked")
        final var valList = (List<EndData>) val;
        for (final var end : valList) {
          if (end != null) map.put(end.getLocation(), end);
        }
      } else if (val instanceof EndData end) {
        map.put(end.getLocation(), end);
      }
      return map;
    }

    @Override
    public void labelChanged(ComponentEvent e) {
      final var attrEvent = (AttributeEvent) e.getData();
      if (attrEvent.getSource() == null || attrEvent.getValue() == null) return;
      final var newLabel = (String) attrEvent.getValue();
      final var oldLabel = attrEvent.getOldValue() != null ? (String) attrEvent.getOldValue() : "";
      @SuppressWarnings("unchecked")
      Attribute<String> lattr = (Attribute<String>) attrEvent.getAttribute();
      if (!isCorrectLabel(getName(), newLabel, comps, attrEvent.getSource(), e.getSource().getFactory(), true)) {
        if (isCorrectLabel(
            getName(), oldLabel, comps, attrEvent.getSource(), e.getSource().getFactory(), false)) {
          attrEvent.getSource().setValue(lattr, oldLabel);
        } else {
          attrEvent.getSource().setValue(lattr, "");
        }
      }
    }
  }

  public static boolean isCorrectLabel(
      String circuitName,
      String name,
      Set<Component> components,
      AttributeSet me,
      ComponentFactory myFactory,
      Boolean showDialog) {
    if (myFactory instanceof Tunnel) return true;
    if (circuitName != null
        && !circuitName.isEmpty()
        && circuitName.equalsIgnoreCase(name)
        && myFactory instanceof Pin) {
      if (showDialog) {
        final var msg = S.get("ComponentLabelEqualCircuitName");
        OptionPane.showMessageDialog(null, "\"" + name + "\" : " + msg);
      }
      return false;
    }
    return !(isExistingLabel(name, me, components, showDialog)
        || isComponentName(name, components, showDialog));
  }

  private static boolean isComponentName(String name, Set<Component> comps, Boolean showDialog) {
    if (name.isEmpty()) return false;
    for (final var comp : comps) {
      if (comp.getFactory().getName().equalsIgnoreCase(name)) {
        if (showDialog) {
          final var msg = S.get("ComponentLabelNameError");
          OptionPane.showMessageDialog(null, "\"" + name + "\" : " + msg);
        }
        return true;
      }
    }
    // we do not have to check the wires as (1) Wire is a reserved keyword,
    // and (2) they cannot have a label
    return false;
  }

  private static boolean isExistingLabel(String name, AttributeSet me, Set<Component> comps, Boolean showDialog) {
    if (name.isEmpty()) return false;
    for (final var comp : comps) {
      if (!comp.getAttributeSet().equals(me) && !(comp.getFactory() instanceof Tunnel)) {
        final var Label =
            (comp.getAttributeSet().containsAttribute(StdAttr.LABEL))
                ? comp.getAttributeSet().getValue(StdAttr.LABEL)
                : "";
        if (Label.equalsIgnoreCase(name)) {
          if (showDialog) {
            final var msg = S.get("UsedLabelNameError");
            OptionPane.showMessageDialog(null, "\"" + name + "\" : " + msg);
          }
          return true;
        }
      }
    }
    // we do not have to check the wires as (1) Wire is a reserved keyword,
    // and (2) they cannot have a label
    return false;
  }

  //
  // helper methods for other classes in package
  //
  public static boolean isInput(Component comp) {
    return comp.getEnd(0).getType() != EndData.INPUT_ONLY;
  }

  private static final int maxTimeoutTestBenchSec = 60000;
  private final MyComponentListener myComponentListener = new MyComponentListener();
  private final CircuitAppearance appearance;
  private final AttributeSet staticAttrs;
  private final SubcircuitFactory subcircuitFactory;
  private final EventSourceWeakSupport<CircuitListener> listeners = new EventSourceWeakSupport<>();
  private LinkedHashSet<Component> comps = new LinkedHashSet<>(); // doesn't include wires
  CircuitWires wires = new CircuitWires();
  private final List<Component> clocks = new ArrayList<>();
  private final CircuitLocker locker;

  private final WeakHashMap<Component, Circuit> circuitsUsingThis;
  private final Netlist myNetList;
  private final Map<String, MappableResourcesContainer> myMappableResources;
  private final Map<String, Map<String, CircuitMapInfo>> loadedMaps;
  private boolean isAnnotated;
  private Project proj;
  private final SocSimulationManager socSim = new SocSimulationManager();

  private final LogisimFile logiFile;

  public Circuit(String name, LogisimFile file, Project proj) {
    staticAttrs = CircuitAttributes.createBaseAttrs(this, name);
    appearance = new CircuitAppearance(this);
    subcircuitFactory = new SubcircuitFactory(this);
    locker = new CircuitLocker();
    circuitsUsingThis = new WeakHashMap<>();
    myNetList = new Netlist(this);
    myMappableResources = new HashMap<>();
    loadedMaps = new HashMap<>();
    isAnnotated = false;
    logiFile = file;
    staticAttrs.setValue(
        CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE,
        AppPreferences.NAMED_CIRCUIT_BOXES_FIXED_SIZE.getBoolean());
    this.proj = proj;
  }

  public void setProject(Project proj) {
    this.proj = proj;
  }

  public Project getProject() {
    return proj;
  }

  public SocSimulationManager getSocSimulationManager() {
    return socSim;
  }

  //
  // Listener methods
  //
  public void addCircuitListener(CircuitListener what) {
    listeners.add(what);
  }

  public void recalcDefaultShape() {
    if (appearance.isDefaultAppearance()) {
      appearance.recomputeDefaultAppearance();
    }
  }

  private static String getAnnotationName(Component comp) {
    String componentName;
    /* Pins are treated specially */
    if (comp.getFactory() instanceof Pin) {
      if (comp.getEnd(0).isOutput()) {
        if (comp.getEnd(0).getWidth().getWidth() > 1) {
          componentName = "Input_bus";
        } else {
          componentName = "Input";
        }
      } else {
        if (comp.getEnd(0).getWidth().getWidth() > 1) {
          componentName = "Output_bus";
        } else {
          componentName = "Output";
        }
      }
    } else {
      componentName = comp.getFactory().getHDLName(comp.getAttributeSet());
    }
    return componentName;
  }

  public void annotate(Project proj, boolean clearExistingLabels, boolean insideLibrary) {
    if (this.proj == null) this.proj = proj;
    this.annotate(clearExistingLabels, insideLibrary);
  }

  public void annotate(boolean clearExistingLabels, boolean insideLibrary) {
    /* If I am already completely annotated, return */
    if (isAnnotated) {
      // FIXME: hardcoded string
      Reporter.report.addInfo("Nothing to do!");
      return;
    }
    final var comps = new TreeSet<Component>(Location.CompareVertical);
    final var labelers = new HashMap<String, AutoLabel>();
    final var labelNames = new LinkedHashSet<String>();
    final var subCircuits = new LinkedHashSet<String>();
    for (final var comp : getNonWires()) {
      if (comp.getFactory() instanceof Tunnel) continue;
      /* we are directly going to remove duplicated labels */
      final var attrs = comp.getAttributeSet();
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        final var label = attrs.getValue(StdAttr.LABEL);
        if (!label.isEmpty()) {
          if (labelNames.contains(label.toUpperCase())) {
            final var act = new SetAttributeAction(this, S.getter("changeComponentAttributesAction"));
            act.set(comp, StdAttr.LABEL, "");
            proj.doAction(act);
            // FIXME: hardcoded string
            Reporter.report.addSevereWarning("Removed duplicated label " + this.getName() + "/" + label);
          } else {
            labelNames.add(label.toUpperCase());
          }
        }
      }
      /* now we only process those that require a label */
      if (comp.getFactory().requiresNonZeroLabel()) {
        if (clearExistingLabels) {
          /* in case of label cleaning, we clear first the old label */
          // FIXME: hardcoded string
          Reporter.report.addInfo("Cleared " + this.getName() + "/" + comp.getAttributeSet().getValue(StdAttr.LABEL));
          final var act = new SetAttributeAction(this, S.getter("changeComponentAttributesAction"));
          act.set(comp, StdAttr.LABEL, "");
          proj.doAction(act);
        }
        if (comp.getAttributeSet().getValue(StdAttr.LABEL).isEmpty()) {
          comps.add(comp);
          final var componentName = getAnnotationName(comp);
          if (!labelers.containsKey(componentName)) {
            labelers.put(componentName, new AutoLabel(componentName + "_0", this));
          }
        }
      }
      /* if the current component is a sub-circuit, recurse into it */
      if (comp.getFactory() instanceof SubcircuitFactory sub) {
        subCircuits.add(sub.getName());
      }
    }
    /* Now Annotate */
    var sizeMightHaveChanged = false;
    for (final var comp : comps) {
      final var componentName = getAnnotationName(comp);
      if (!labelers.containsKey(componentName) || !labelers.get(componentName).hasNext(this)) {
        // This should never happen!
        // FIXME: hardcoded string
        Reporter.report.addFatalError(
            "Annotate internal Error: Either there exists duplicate labels or the label syntax is incorrect!\nPlease try annotation on labeled components also\n");
        return;
      } else {
        final var newLabel = labelers.get(componentName).getNext(this, comp.getFactory());
        final var act = new SetAttributeAction(this, S.getter("changeComponentAttributesAction"));
        act.set(comp, StdAttr.LABEL, newLabel);
        proj.doAction(act);
        Reporter.report.addInfo("Labeled " + this.getName() + "/" + newLabel);
        if (comp.getFactory() instanceof Pin) {
          sizeMightHaveChanged = true;
        }
      }
    }
    if (!comps.isEmpty() && insideLibrary) {
      // FIXME: hardcoded string
      Reporter.report.addSevereWarning(
          "Annotated the circuit \""
              + this.getName()
              + "\" which is inside a library these changes will not be saved!");
    }
    if (sizeMightHaveChanged)
      // FIXME: hardcoded string
      Reporter.report.addSevereWarning(
          "Annotated one ore more pins in circuit \""
              + this.getName()
              + "\" this might have changed it's boxsize and might have impacted it's connections in circuits using this one!");
    isAnnotated = true;
    /* Now annotate all circuits below me */
    for (final var subs : subCircuits) {
      final var circ = LibraryTools.getCircuitFromLibs(proj.getLogisimFile(), subs.toUpperCase());
      final var inLibrary = !proj.getLogisimFile().getCircuits().contains(circ);
      circ.annotate(proj, clearExistingLabels, inLibrary);
    }
  }

  //
  // Annotation module for all components that require a non-zero-length label
  public void clearAnnotationLevel() {
    isAnnotated = false;
    myNetList.clear();
    for (final var comp : this.getNonWires()) {
      if (comp.getFactory() instanceof SubcircuitFactory sub) {
        sub.getSubcircuit().clearAnnotationLevel();
      }
    }
  }

  public boolean contains(Component c) {
    return comps.contains(c) || wires.getWires().contains(c);
  }

  /* The function will tick. Then once the tick was propagated
   * in the circuit, the output value are going to be checked.
   * The pin[0] is indicating when the simulation is done.
   * Once the Simulation is done (pin[0] to 1) the value of pin[1]
   * will be checked and if the value of pin[1] is 1 the function return true.
   * It will return zero otherwise  */
  public boolean doTestBench(Project project, Instance[] pin, Value[] val) {
    final var state = project.getCircuitState();
    /* This is introduced in order to not block in case both the signal never happend*/
    final var pinsState = new InstanceState[pin.length];
    final var vPins = new Value[pin.length];
    state.reset();

    final var ts = new TimeoutSimulation();
    final var timer = new Timer();
    timer.schedule(ts, maxTimeoutTestBenchSec);

    while (true) {
      var i = 0;
      project.getSimulator().tick(1);
      Thread.yield();

      for (final var pinStatus : pin) {
        pinsState[i] = state.getInstanceState(pinStatus);
        vPins[i] = Pin.FACTORY.getValue(pinsState[i]);
        i++;
      }

      if (val[0].compatible(vPins[0])) {
        if (vPins[0].equals(Value.TRUE)) {
          return (val[1].compatible(vPins[1]) && vPins[1].equals(Value.TRUE));
        }
      }

      if (ts.isTimeOut()) {
        return false;
      }
    }
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   *
   * @deprecated Use {@link #doTestVector(Project, Instance[], Value[], boolean, TestVector, int)} instead
   */
  @Deprecated
  public void doTestVector(Project project, Instance[] pin, Value[] val) throws TestException {
    doTestVector(project, pin, val, true, null, -1);
  }

  /**
   * Execute a test vector with optional reset control and special value handling.
   *
   * @param project The project containing the circuit
   * @param pin Array of pin instances
   * @param val Array of values to drive/expect
   * @param resetState If true, reset circuit state before test; if false, maintain state
   * @param vector The test vector (can be null for backward compatibility)
   * @param rowIndex The row index in the test vector (used for don't-care/floating checks)
   * @throws TestException if test fails
   */
  public void doTestVector(Project project, Instance[] pin, Value[] val, boolean resetState, 
      com.cburch.logisim.data.TestVector vector, int rowIndex) throws TestException {
    final var state = project.getCircuitState();
    if (resetState) {
      state.reset();
    }

    for (var i = 0; i < pin.length; ++i) {
      if (Pin.FACTORY.isInputPin(pin[i])) {
        final var pinState = state.getInstanceState(pin[i]);
        // Handle floating input - drive UNKNOWN if value is marked as floating
        Value driveValue = val[i];
        if (vector != null && rowIndex >= 0 && vector.isFloating(rowIndex, i)) {
          driveValue = Value.UNKNOWN;
        }
        Pin.FACTORY.driveInputPin(pinState, driveValue);
        // Mark the pin component as dirty so it gets processed during propagation
        state.markComponentAsDirty(pin[i].getComponent());
      }
    }

    final var prop = state.getPropagator();

    // Propagate until stable
    
    try {
      prop.propagate();
    } catch (Throwable thr) {
      // propagate() might fail if not on simulation thread
      // This shouldn't happen in normal operation, but handle gracefully
      throw new TestException("propagation failed: " + thr.getMessage());
    }

    if (prop.isOscillating()) throw new TestException("oscillation detected");

    FailException err = null;

    for (var i = 0; i < pin.length; i++) {
      final var pinState = state.getInstanceState(pin[i]);
      if (Pin.FACTORY.isInputPin(pin[i])) continue;

      final var v = Pin.FACTORY.getValue(pinState);
      
      // Check for don't care - always pass
      if (vector != null && rowIndex >= 0 && vector.isDontCare(rowIndex, i)) {
        continue; // Skip comparison for don't care values
      }
      
      // Check for floating - expect UNKNOWN
      if (vector != null && rowIndex >= 0 && vector.isFloating(rowIndex, i)) {
        if (!Value.UNKNOWN.equals(v)) {
          if (err == null) {
            err = new FailException(i, pinState.getAttributeValue(StdAttr.LABEL), Value.UNKNOWN, v);
          } else {
            err.add(new FailException(i, pinState.getAttributeValue(StdAttr.LABEL), Value.UNKNOWN, v));
          }
        }
        continue;
      }
      
      // Normal value comparison
      if (!val[i].compatible(v)) {
        if (err == null) {
          err = new FailException(i, pinState.getAttributeValue(StdAttr.LABEL), val[i], v);
        } else {
          err.add(new FailException(i, pinState.getAttributeValue(StdAttr.LABEL), val[i], v));
        }
      }
    }

    if (err != null) {
      throw err;
    }
  }

  //
  // Graphics methods
  //
  public void draw(ComponentDrawContext context, Collection<Component> hidden) {
    final var g = context.getGraphics();
    var gCopy = g.create();
    context.setGraphics(gCopy);
    wires.draw(context, hidden);

    if (CollectionUtil.isNullOrEmpty(hidden)) {
      for (final var c : comps) {
        final var gNew = g.create();
        context.setGraphics(gNew);
        gCopy.dispose();
        gCopy = gNew;

        c.draw(context);
      }
    } else {
      for (final var c : comps) {
        if (!hidden.contains(c)) {
          final var gNew = g.create();
          context.setGraphics(gNew);
          gCopy.dispose();
          gCopy = gNew;

          try {
            c.draw(context);
          } catch (RuntimeException e) {
            // this is a JAR developer error - display it and move on
            e.printStackTrace();
          }
        }
      }
    }
    context.setGraphics(g);
    gCopy.dispose();
  }

  private void fireEvent(CircuitEvent event) {
    for (final var l : listeners) {
      l.circuitChanged(event);
    }
  }

  void fireEvent(int action, Object data) {
    fireEvent(new CircuitEvent(action, this, data));
  }

  public void displayChanged() {
    fireEvent(CircuitEvent.ACTION_DISPLAY_CHANGE, null);
  }

  public Collection<Component> getAllContaining(Location pt) {
    final var ret = new LinkedHashSet<Component>();
    for (final var comp : getComponents()) {
      if (comp.contains(pt)) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getAllContaining(Location pt, Graphics g) {
    final var ret = new LinkedHashSet<Component>();
    for (final var comp : getComponents()) {
      if (comp.contains(pt, g)) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getAllWithin(Bounds bds) {
    final var ret = new LinkedHashSet<Component>();
    for (final var comp : getComponents()) {
      if (bds.contains(comp.getBounds())) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getAllWithin(Bounds bds, Graphics g) {
    final var ret = new LinkedHashSet<Component>();
    for (final var comp : getComponents()) {
      if (bds.contains(comp.getBounds(g))) ret.add(comp);
    }
    return ret;
  }

  public CircuitAppearance getAppearance() {
    return appearance;
  }

  public Bounds getBounds() {
    final var wireBounds = wires.getWireBounds();
    final var it = comps.iterator();
    if (!it.hasNext()) return wireBounds;
    final var first = it.next();
    final var firstBounds = first.getBounds();
    var xMin = firstBounds.getX();
    var yMin = firstBounds.getY();
    var xMax = xMin + firstBounds.getWidth();
    var yMax = yMin + firstBounds.getHeight();
    while (it.hasNext()) {
      Component c = it.next();
      Bounds bds = c.getBounds();
      int x0 = bds.getX();
      int x1 = x0 + bds.getWidth();
      int y0 = bds.getY();
      int y1 = y0 + bds.getHeight();
      if (x0 < xMin) xMin = x0;
      if (x1 > xMax) xMax = x1;
      if (y0 < yMin) yMin = y0;
      if (y1 > yMax) yMax = y1;
    }
    final var compBounds = Bounds.create(xMin, yMin, xMax - xMin, yMax - yMin);
    return (wireBounds.getWidth() == 0 || wireBounds.getHeight() == 0)
        ? compBounds
        : compBounds.add(wireBounds);
  }

  public Bounds getBounds(Graphics g) {
    final var ret = wires.getWireBounds();
    var xMin = ret.getX();
    var yMin = ret.getY();
    var xMax = xMin + ret.getWidth();
    var yMax = yMin + ret.getHeight();
    if (ret == Bounds.EMPTY_BOUNDS) {
      xMin = Integer.MAX_VALUE;
      yMin = Integer.MAX_VALUE;
      xMax = Integer.MIN_VALUE;
      yMax = Integer.MIN_VALUE;
    }
    for (final var comp : comps) {
      final var bds = comp.getBounds(g);
      if (bds != null && bds != Bounds.EMPTY_BOUNDS) {
        final var x0 = bds.getX();
        final var x1 = x0 + bds.getWidth();
        final var y0 = bds.getY();
        final var y1 = y0 + bds.getHeight();
        if (x0 < xMin) xMin = x0;
        if (x1 > xMax) xMax = x1;
        if (y0 < yMin) yMin = y0;
        if (y1 > yMax) yMax = y1;
      }
    }
    if (xMin > xMax || yMin > yMax) return Bounds.EMPTY_BOUNDS;
    return Bounds.create(xMin, yMin, xMax - xMin, yMax - yMin);
  }

  public Collection<Circuit> getCircuitsUsingThis() {
    return circuitsUsingThis.values();
  }

  public void removeComponent(Component c) {
    circuitsUsingThis.remove(c);
  }

  public List<Component> getClocks() {
    return clocks;
  }

  public Set<Component> getComponents() {
    return CollectionUtil.createUnmodifiableSetUnion(comps, wires.getWires());
  }

  public Collection<? extends Component> getComponents(Location loc) {
    return wires.points.getComponents(loc);
  }

  public Component getExclusive(Location loc) {
    return wires.points.getExclusive(loc);
  }

  public CircuitLocker getLocker() {
    return locker;
  }

  //
  // access methods
  //
  public String getName() {
    return staticAttrs.getValue(CircuitAttributes.NAME_ATTR);
  }

  public Netlist getNetList() {
    return myNetList;
  }

  public void addLoadedMap(String boardName, Map<String, CircuitMapInfo> map) {
    loadedMaps.put(boardName, map);
  }

  public Set<String> getBoardMapNamestoSave() {
    final var ret = new HashSet<String>();
    ret.addAll(loadedMaps.keySet());
    ret.addAll(myMappableResources.keySet());
    return ret;
  }

  public Map<String, CircuitMapInfo> getMapInfo(String boardName) {
    if (myMappableResources.containsKey(boardName))
      return myMappableResources.get(boardName).getCircuitMap();
    if (loadedMaps.containsKey(boardName))
      return loadedMaps.get(boardName);
    return new HashMap<>();
  }

  public void setBoardMap(String boardName, MappableResourcesContainer map) {
    if (loadedMaps.containsKey(boardName)) {
      for (final var key : loadedMaps.get(boardName).keySet()) {
        final var cmap = loadedMaps.get(boardName).get(key);
        map.tryMap(key, cmap);
      }
      loadedMaps.remove(boardName);
    }
    myMappableResources.put(boardName, map);
  }

  public MappableResourcesContainer getBoardMap(String boardName) {
    if (myMappableResources.containsKey(boardName))
      return myMappableResources.get(boardName);
    return null;
  }

  public Set<String> getMapableBoards() {
    return myMappableResources.keySet();
  }

  public Set<Component> getNonWires() {
    return comps;
  }

  public Collection<? extends Component> getNonWires(Location loc) {
    return wires.points.getNonWires(loc);
  }

  public String getProjName() {
    return logiFile == null ? "" : logiFile.getName();
  }

  public Collection<? extends Component> getSplitCauses(Location loc) {
    return wires.points.getSplitCauses(loc);
  }

  public Set<Location> getAllLocations() {
    return wires.points.getAllLocations();
  }

  public AttributeSet getStaticAttributes() {
    return staticAttrs;
  }

  public SubcircuitFactory getSubcircuitFactory() {
    return subcircuitFactory;
  }

  public BitWidth getWidth(Location p) {
    return wires.getWidth(p);
  }

  public Set<WidthIncompatibilityData> getWidthIncompatibilityData() {
    return wires.getWidthIncompatibilityData();
  }

  public Set<Wire> getWires() {
    return wires.getWires();
  }

  public Collection<Wire> getWires(Location loc) {
    return wires.points.getWires(loc);
  }

  public WireSet getWireSet(Wire start) {
    return wires.getWireSet(start);
  }

  public boolean hasConflict(Component comp) {
    return wires.points.hasConflict(comp) || isDoubleMapped(comp);
  }

  private boolean isDoubleMapped(Component comp) {
    final var loc = comp.getLocation();
    final var existing = wires.points.getNonWires(loc);
    for (final var existingComp : existing) {
      if (existingComp.getFactory().equals(comp.getFactory())) {
        /* we make an exception for the pin in case we have an input placed on an output */
        if (comp.getFactory() instanceof Pin) {
          final var dir1 = comp.getAttributeSet().getValue(Pin.ATTR_TYPE);
          final var dir2 = existingComp.getAttributeSet().getValue(Pin.ATTR_TYPE);
          if (dir1.equals(dir2)) return true;
        } else {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isConnected(Location loc, Component ignore) {
    for (final var o : wires.points.getComponents(loc)) {
      if (o != ignore) return true;
    }
    return false;
  }

  void mutatorAdd(Component c) {
    locker.checkForWritePermission("add", this);

    isAnnotated = false;
    myNetList.clear();
    if (c instanceof Wire wire) {
      if (wire.getEnd0().equals(wire.getEnd1())) return;
      var added = wires.add(wire);
      if (!added) return;
    } else {
      // add it into the circuit
      var added = comps.add(c);
      if (!added) return;
      socSim.registerComponent(c);
      // Here we check for duplicated labels and clear the label
      // if it already exists in the circuit
      if (c.getAttributeSet().containsAttribute(StdAttr.LABEL)
          && !(c.getFactory() instanceof Tunnel)) {
        final var labels = new HashSet<String>();
        for (final var comp : comps) {
          if (comp.equals(c) || comp.getFactory() instanceof Tunnel) continue;
          if (comp.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
            final var label = comp.getAttributeSet().getValue(StdAttr.LABEL);
            if (StringUtil.isNotEmpty(label)) labels.add(label.toUpperCase());
          }
        }
        /* we also have to check for the entity name */
        if (getName() != null && !getName().isEmpty()) labels.add(getName());
        final var label = c.getAttributeSet().getValue(StdAttr.LABEL);
        if (StringUtil.isNotEmpty(label) && labels.contains(label.toUpperCase())) {
          c.getAttributeSet().setValue(StdAttr.LABEL, "");
        }
      }
      wires.add(c);
      final var factory = c.getFactory();
      if (factory instanceof Clock) {
        clocks.add(c);
      } else if (factory instanceof Rom) {
        Rom.closeHexFrame(c);
      } else if (factory instanceof SubcircuitFactory subFactory) {
        final var subcirc = subFactory;
        subcirc.getSubcircuit().circuitsUsingThis.put(c, this);
      } else if (factory instanceof VhdlEntity vhdlEntity) {
        final var vhdl = vhdlEntity;
        vhdl.addCircuitUsing(c, this);
      }
      c.addComponentListener(myComponentListener);
    }
    removeWrongLabels(c.getFactory().getName());
    fireEvent(CircuitEvent.ACTION_ADD, c);
  }

  public void mutatorClear() {
    locker.checkForWritePermission("clear", this);

    final var oldComps = comps;
    comps = new LinkedHashSet<>();
    wires = new CircuitWires();
    clocks.clear();
    myNetList.clear();
    isAnnotated = false;
    for (final var comp : oldComps) {
      socSim.removeComponent(comp);
      final var factory = comp.getFactory();
      factory.removeComponent(this, comp, proj.getCircuitState(this));
    }
    fireEvent(CircuitEvent.ACTION_CLEAR, oldComps);
  }

  void mutatorRemove(Component c) {
    locker.checkForWritePermission("remove", this);

    isAnnotated = false;
    myNetList.clear();
    if (c instanceof Wire) {
      wires.remove(c);
    } else {
      wires.remove(c);
      comps.remove(c);
      socSim.removeComponent(c);
      final var factory = c.getFactory();
      factory.removeComponent(this, c, proj.getCircuitState(this));
      if (factory instanceof Clock) {
        clocks.remove(c);
      } else if (factory instanceof DynamicElementProvider) {
        DynamicElementProvider.removeDynamicElements(this, c);
      }
      c.removeComponentListener(myComponentListener);
    }
    fireEvent(CircuitEvent.ACTION_REMOVE, c);
  }

  private void removeWrongLabels(String label) {
    var changed = false;
    for (final var comp : comps) {
      final var attrs = comp.getAttributeSet();
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        final var compLabel = attrs.getValue(StdAttr.LABEL);
        if (label.equalsIgnoreCase(compLabel)) {
          attrs.setValue(StdAttr.LABEL, "");
          changed = true;
        }
      }
    }
    // we do not have to check the wires as (1) Wire is a reserved keyword,
    // and (2) they cannot have a label
    if (changed)
      OptionPane.showMessageDialog(
          null, "\"" + label + "\" : " + S.get("ComponentLabelCollisionError"));
  }

  public void removeCircuitListener(CircuitListener what) {
    listeners.remove(what);
  }

  //
  // action methods
  //
  public void setName(String name) {
    staticAttrs.setValue(CircuitAttributes.NAME_ATTR, name);
  }

  @Override
  public String toString() {
    return staticAttrs.getValue(CircuitAttributes.NAME_ATTR);
  }

  public static class TimeoutSimulation extends TimerTask {

    /* Make it atomic */
    private volatile boolean timedOut;

    public TimeoutSimulation() {
      timedOut = false;
    }

    public boolean isTimeOut() {
      return timedOut;
    }

    @Override
    public void run() {
      timedOut = true;
    }
  }

  public double getTickFrequency() {
    return staticAttrs.getValue(CircuitAttributes.SIMULATION_FREQUENCY);
  }

  public void setTickFrequency(double value) {
    final var currentTickFrequency = staticAttrs.getValue(CircuitAttributes.SIMULATION_FREQUENCY);
    if (value != currentTickFrequency) {
      staticAttrs.setValue(CircuitAttributes.SIMULATION_FREQUENCY, value);
      if ((proj != null) && (currentTickFrequency > 0)) proj.setForcedDirty();
    }
  }

  public double getDownloadFrequency() {
    return staticAttrs.getValue(CircuitAttributes.DOWNLOAD_FREQUENCY);
  }

  public void setDownloadFrequency(double value) {
    if (value != staticAttrs.getValue(CircuitAttributes.DOWNLOAD_FREQUENCY)) {
      staticAttrs.setValue(CircuitAttributes.DOWNLOAD_FREQUENCY, value);
      if (proj != null) proj.setForcedDirty();
    }
  }

  public String getDownloadBoard() {
    return staticAttrs.getValue(CircuitAttributes.DOWNLOAD_BOARD);
  }

  public void setDownloadBoard(String board) {
    if (!board.equals(staticAttrs.getValue(CircuitAttributes.DOWNLOAD_BOARD))) {
      staticAttrs.setValue(CircuitAttributes.DOWNLOAD_BOARD, board);
      if (proj != null) proj.setForcedDirty();
    }
  }
}
