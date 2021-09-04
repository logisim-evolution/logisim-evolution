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
import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class Circuit {
  private class EndChangedTransaction extends CircuitTransaction {
    private final Component comp;
    private final Map<Location, EndData> toRemove;
    private final Map<Location, EndData> toAdd;

    EndChangedTransaction(Component comp, Map<Location, EndData> toRemove, Map<Location, EndData> toAdd) {
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
      for (val loc : toRemove.keySet()) {
        val removed = toRemove.get(loc);
        val replaced = toAdd.remove(loc);
        if (replaced == null) {
          wires.remove(comp, removed);
        } else if (!replaced.equals(removed)) {
          wires.replace(comp, removed, replaced);
        }
      }
      for (val end : toAdd.values()) {
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
      netList.clear();
      val comp = e.getSource();
      val toRemove = toMap(e.getOldData());
      val toAdd = toMap(e.getData());
      val xn = new EndChangedTransaction(comp, toRemove, toAdd);
      locker.execute(xn);
      fireEvent(CircuitEvent.ACTION_INVALIDATE, comp);
    }

    private HashMap<Location, EndData> toMap(Object val) {
      val map = new HashMap<Location, EndData>();
      if (val instanceof List) {
        @SuppressWarnings("unchecked")
        val valList = (List<EndData>) val;
        for (val end : valList) {
          if (end != null) {
            map.put(end.getLocation(), end);
          }
        }
      } else if (val instanceof EndData) {
        val end = (EndData) val;
        map.put(end.getLocation(), end);
      }
      return map;
    }

    @Override
    public void LabelChanged(ComponentEvent e) {
      val attre = (AttributeEvent) e.getData();
      if (attre.getSource() == null || attre.getValue() == null) {
        return;
      }
      val newLabel = (String) attre.getValue();
      val oldLabel = attre.getOldValue() != null ? (String) attre.getOldValue() : "";
      @SuppressWarnings("unchecked")
      Attribute<String> lattr = (Attribute<String>) attre.getAttribute();
      if (!IsCorrectLabel(
          getName(), newLabel, comps, attre.getSource(), e.getSource().getFactory(), true)) {
        if (IsCorrectLabel(
            getName(), oldLabel, comps, attre.getSource(), e.getSource().getFactory(), false))
          attre.getSource().setValue(lattr, oldLabel);
        else attre.getSource().setValue(lattr, "");
      }
    }
  }

  public static boolean IsCorrectLabel(
      String CircuitName,
      String Name,
      Set<Component> components,
      AttributeSet me,
      ComponentFactory myFactory,
      Boolean ShowDialog) {
    if (myFactory instanceof Tunnel) return true;
    if (CircuitName != null
        && !CircuitName.isEmpty()
        && CircuitName.equalsIgnoreCase(Name)
        && myFactory instanceof Pin) {
      if (ShowDialog) {
        String msg = S.get("ComponentLabelEqualCircuitName");
        OptionPane.showMessageDialog(null, "\"" + Name + "\" : " + msg);
      }
      return false;
    }
    return !(IsExistingLabel(Name, me, components, ShowDialog)
        || IsComponentName(Name, components, ShowDialog));
  }

  private static boolean IsComponentName(String name, Set<Component> comps, Boolean showDialog) {
    if (name.isEmpty()) return false;
    for (val comp : comps) {
      if (comp.getFactory().getName().equalsIgnoreCase(name)) {
        if (showDialog) {
          val msg = S.get("ComponentLabelNameError");
          OptionPane.showMessageDialog(null, "\"" + name + "\" : " + msg);
        }
        return true;
      }
    }
    // we do not have to check the wires as (1) Wire is a reserved keyword,
    // and (2) they cannot have a label
    return false;
  }

  private static boolean IsExistingLabel(String name, AttributeSet me, Set<Component> comps, Boolean showDialog) {
    if (name.isEmpty()) return false;
    for (val comp : comps) {
      if (!comp.getAttributeSet().equals(me) && !(comp.getFactory() instanceof Tunnel)) {
        val Label =
            (comp.getAttributeSet().containsAttribute(StdAttr.LABEL))
                ? comp.getAttributeSet().getValue(StdAttr.LABEL)
                : "";
        if (Label.equalsIgnoreCase(name)) {
          if (showDialog) {
            val msg = S.get("UsedLabelNameError");
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

  private static final int MAX_TIMEOUT_TEST_BENCH_SEC = 60000;
  private final MyComponentListener myComponentListener = new MyComponentListener();
  @Getter private final CircuitAppearance appearance;
  @Getter private final AttributeSet staticAttributes;
  @Getter private final SubcircuitFactory subcircuitFactory;
  private final EventSourceWeakSupport<CircuitListener> listeners = new EventSourceWeakSupport<>();
  private LinkedHashSet<Component> comps = new LinkedHashSet<>(); // doesn't
  // include
  // wires
  CircuitWires wires = new CircuitWires();
  @Getter private final ArrayList<Component> clocks = new ArrayList<>();
  @Getter private final CircuitLocker locker;

  private final WeakHashMap<Component, Circuit> circuitsUsingThis;
  @Getter private final Netlist netList;
  private final HashMap<String, MappableResourcesContainer> myMappableResources;
  private final HashMap<String, HashMap<String, CircuitMapInfo>> loadedMaps;
  private boolean isAnnotated;
  @Getter @Setter private Project project;
  private final SocSimulationManager socSim = new SocSimulationManager();

  private final LogisimFile logiFile;

  public Circuit(String name, LogisimFile file, Project proj) {
    staticAttributes = CircuitAttributes.createBaseAttrs(this, name);
    appearance = new CircuitAppearance(this);
    subcircuitFactory = new SubcircuitFactory(this);
    locker = new CircuitLocker();
    circuitsUsingThis = new WeakHashMap<>();
    netList = new Netlist(this);
    myMappableResources = new HashMap<>();
    loadedMaps = new HashMap<>();
    isAnnotated = false;
    logiFile = file;
    staticAttributes.setValue(
        CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE,
        AppPreferences.NAMED_CIRCUIT_BOXES_FIXED_SIZE.getBoolean());
    this.project = proj;
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

  public void RecalcDefaultShape() {
    if (appearance.isDefaultAppearance()) {
      appearance.recomputeDefaultAppearance();
    }
  }

  private static String GetAnnotationName(Component comp) {
    val componentName =
        (comp.getFactory() instanceof Pin)
            // Pins are treated specially
            ? (comp.getEnd(0).isOutput())
                ? (comp.getEnd(0).getWidth().getWidth() > 1) ? "Input_bus" : "Input"
                : (comp.getEnd(0).getWidth().getWidth() > 1) ? "Output_bus" : "Output"
            : comp.getFactory().getHDLName(comp.getAttributeSet());
    return componentName;
  }

  public void Annotate(Project proj, boolean clearExistingLabels, boolean insideLibrary) {
    if (this.project == null) this.project = proj;
    this.Annotate(clearExistingLabels, insideLibrary);
  }

  public void Annotate(boolean clearExistingLabels, boolean insideLibrary) {
    /* If I am already completely annotated, return */
    if (isAnnotated) {
      Reporter.Report.AddInfo("Nothing to do!");
      return;
    }
    val comps = new TreeSet<Component>(Location.CompareVertical);
    val lablers = new HashMap<String, AutoLabel>();
    val labelNames = new LinkedHashSet<String>();
    val subCircuits = new LinkedHashSet<String>();
    for (val comp : getNonWires()) {
      if (comp.getFactory() instanceof Tunnel) continue;
      /* we are directly going to remove duplicated labels */
      val attrs = comp.getAttributeSet();
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        val label = attrs.getValue(StdAttr.LABEL);
        if (!label.isEmpty()) {
          if (labelNames.contains(label.toUpperCase())) {
            val act = new SetAttributeAction(this, S.getter("changeComponentAttributesAction"));
            act.set(comp, StdAttr.LABEL, "");
            project.doAction(act);
            Reporter.Report.AddSevereWarningFmt("Removed duplicated label %s/%s", this.getName(), label);
          } else {
            labelNames.add(label.toUpperCase());
          }
        }
      }
      /* now we only process those that require a label */
      if (comp.getFactory().RequiresNonZeroLabel()) {
        if (clearExistingLabels) {
          /* in case of label cleaning, we clear first the old label */
          Reporter.Report.AddInfoFmt("Cleared %s/%s", this.getName(), comp.getAttributeSet().getValue(StdAttr.LABEL));
          val act = new SetAttributeAction(this, S.getter("changeComponentAttributesAction"));
          act.set(comp, StdAttr.LABEL, "");
          project.doAction(act);
        }
        if (comp.getAttributeSet().getValue(StdAttr.LABEL).isEmpty()) {
          comps.add(comp);
          val componentName = GetAnnotationName(comp);
          if (!lablers.containsKey(componentName)) {
            lablers.put(componentName, new AutoLabel(componentName + "_0", this));
          }
        }
      }
      /* if the current component is a sub-circuit, recurse into it */
      if (comp.getFactory() instanceof SubcircuitFactory) {
        val sub = (SubcircuitFactory) comp.getFactory();
        subCircuits.add(sub.getName());
      }
    }
    /* Now Annotate */
    var sizeMightHaveChanged = false;
    for (val comp : comps) {
      val componentName = GetAnnotationName(comp);
      if (!lablers.containsKey(componentName) || !lablers.get(componentName).hasNext(this)) {
        /* This should never happen! */
        Reporter.Report.AddFatalError(
            "Annotate internal Error: "
                + "Either there exists duplicate labels or the label syntax is incorrect! "
                + "Please try annotation on labeled components also.");
        return;
      } else {
        val newLabel = lablers.get(componentName).getNext(this, comp.getFactory());
        val act = new SetAttributeAction(this, S.getter("changeComponentAttributesAction"));
        act.set(comp, StdAttr.LABEL, newLabel);
        project.doAction(act);
        Reporter.Report.AddInfoFmt("Labeled %s/%s", this.getName(), newLabel);
        if (comp.getFactory() instanceof Pin) {
          sizeMightHaveChanged = true;
        }
      }
    }
    if (!comps.isEmpty() & insideLibrary) {
      Reporter.Report.AddSevereWarningFmt(
          "Annotated the circuit \"%s\" which is inside a library these changes will not be saved!",
          this.getName());
    }
    if (sizeMightHaveChanged)
      Reporter.Report.AddSevereWarningFmt(
          "Annotated one ore more pins in circuit \"%s\" this might have changed it's boxsize "
              + "and might have impacted it's connections in circuits using this one!",
          this.getName());
    isAnnotated = true;
    /* Now annotate all circuits below me */
    for (val subs : subCircuits) {
      val circ = LibraryTools.getCircuitFromLibs(project.getLogisimFile(), subs.toUpperCase());
      val inLibrary = !project.getLogisimFile().getCircuits().contains(circ);
      circ.Annotate(project, clearExistingLabels, inLibrary);
    }
  }

  //
  // Annotation module for all components that require a non-zero-length label
  public void ClearAnnotationLevel() {
    isAnnotated = false;
    netList.clear();
    for (val comp : this.getNonWires()) {
      if (comp.getFactory() instanceof SubcircuitFactory) {
        val sub = (SubcircuitFactory) comp.getFactory();
        sub.getSubcircuit().ClearAnnotationLevel();
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
    val state = project.getCircuitState();
    /* This is introduced in order to not block in case both the signal never happend*/
    val pinsState = new InstanceState[pin.length];
    val vPins = new Value[pin.length];
    state.reset();

    val ts = new TimeoutSimulation();
    val timer = new Timer();
    timer.schedule(ts, MAX_TIMEOUT_TEST_BENCH_SEC);

    while (true) {
      var i = 0;
      project.getSimulator().tick(1);
      Thread.yield();

      for (val pinStatus : pin) {
        pinsState[i] = state.getInstanceState(pinStatus);
        vPins[i] = Pin.FACTORY.getValue(pinsState[i]);
        i++;
      }

      if (val[0].compatible(vPins[0])) {
        if (vPins[0].equals(Value.TRUE)) {
          return (val[1].compatible(vPins[1]) && vPins[1].equals(Value.TRUE));
        }
      }

      if (ts.getTimeout()) {
        return false;
      }
    }
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  public void doTestVector(Project project, Instance[] pin, Value[] val) throws TestException {
    val state = project.getCircuitState();
    state.reset();

    for (var i = 0; i < pin.length; ++i) {
      if (Pin.FACTORY.isInputPin(pin[i])) {
        val pinState = state.getInstanceState(pin[i]);
        Pin.FACTORY.setValue(pinState, val[i]);
      }
    }

    val prop = state.getPropagator();

    try {
      prop.propagate();
    } catch (Throwable thr) {
      thr.printStackTrace();
    }

    if (prop.isOscillating()) throw new TestException("oscillation detected");

    FailException err = null;

    for (var i = 0; i < pin.length; i++) {
      val pinState = state.getInstanceState(pin[i]);
      if (Pin.FACTORY.isInputPin(pin[i])) continue;

      val v = Pin.FACTORY.getValue(pinState);
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
    val g = context.getGraphics();
    var gCopy = g.create();
    context.setGraphics(gCopy);
    wires.draw(context, hidden);

    if (hidden == null || hidden.size() == 0) {
      for (val c : comps) {
        val gNew = g.create();
        context.setGraphics(gNew);
        gCopy.dispose();
        gCopy = gNew;

        c.draw(context);
      }
    } else {
      for (val c : comps) {
        if (!hidden.contains(c)) {
          val gNew = g.create();
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
    for (val l : listeners) {
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
    val ret = new LinkedHashSet<Component>();
    for (val comp : getComponents()) {
      if (comp.contains(pt)) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getAllContaining(Location pt, Graphics g) {
    val ret = new LinkedHashSet<Component>();
    for (val comp : getComponents()) {
      if (comp.contains(pt, g)) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getAllWithin(Bounds bds) {
    val ret = new LinkedHashSet<Component>();
    for (val comp : getComponents()) {
      if (bds.contains(comp.getBounds())) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getAllWithin(Bounds bds, Graphics g) {
    val ret = new LinkedHashSet<Component>();
    for (val comp : getComponents()) {
      if (bds.contains(comp.getBounds(g))) ret.add(comp);
    }
    return ret;
  }

  public Bounds getBounds() {
    val wireBounds = wires.getWireBounds();
    val it = comps.iterator();
    if (!it.hasNext()) return wireBounds;
    val first = it.next();
    val firstBounds = first.getBounds();
    var xMin = firstBounds.getX();
    var yMin = firstBounds.getY();
    var xMax = xMin + firstBounds.getWidth();
    var yMax = yMin + firstBounds.getHeight();
    while (it.hasNext()) {
      Component c = it.next();
      Bounds bds = c.getBounds();
      val x0 = bds.getX();
      val x1 = x0 + bds.getWidth();
      val y0 = bds.getY();
      val y1 = y0 + bds.getHeight();
      if (x0 < xMin) xMin = x0;
      if (x1 > xMax) xMax = x1;
      if (y0 < yMin) yMin = y0;
      if (y1 > yMax) yMax = y1;
    }
    val compBounds = Bounds.create(xMin, yMin, xMax - xMin, yMax - yMin);
    return (wireBounds.getWidth() == 0 || wireBounds.getHeight() == 0)
        ? compBounds
        : compBounds.add(wireBounds);
  }

  public Bounds getBounds(Graphics g) {
    val ret = wires.getWireBounds();
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
    for (val comp : comps) {
      val bds = comp.getBounds(g);
      if (bds != null && bds != Bounds.EMPTY_BOUNDS) {
        val x0 = bds.getX();
        val x1 = x0 + bds.getWidth();
        val y0 = bds.getY();
        val y1 = y0 + bds.getHeight();
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

  public Set<Component> getComponents() {
    return CollectionUtil.createUnmodifiableSetUnion(comps, wires.getWires());
  }

  public Collection<? extends Component> getComponents(Location loc) {
    return wires.points.getComponents(loc);
  }

  public Component getExclusive(Location loc) {
    return wires.points.getExclusive(loc);
  }

  //
  // access methods
  //
  public String getName() {
    return staticAttributes.getValue(CircuitAttributes.NAME_ATTR);
  }

  public void addLoadedMap(String boardName, HashMap<String, CircuitMapInfo> map) {
    loadedMaps.put(boardName, map);
  }

  public Set<String> getBoardMapNamestoSave() {
    val ret = new HashSet<String>();
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
      for (val key : loadedMaps.get(boardName).keySet()) {
        val cmap = loadedMaps.get(boardName).get(key);
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

  public Set<Location> getSplitLocations() {
    return wires.points.getSplitLocations();
  }

  public BitWidth getWidth(Location p) {
    return wires.getWidth(p);
  }

  public Location getWidthDeterminant(Location p) {
    return wires.getWidthDeterminant(p);
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
    val loc = comp.getLocation();
    val existing = wires.points.getNonWires(loc);
    for (val existingComp : existing) {
      if (existingComp.getFactory().equals(comp.getFactory())) {
        /* we make an exception for the pin in case we have an input placed on an output */
        if (comp.getFactory() instanceof Pin) {
          val dir1 = comp.getAttributeSet().getValue(Pin.ATTR_TYPE);
          val dir2 = existingComp.getAttributeSet().getValue(Pin.ATTR_TYPE);
          if (dir1 == dir2) return true;
        } else {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isConnected(Location loc, Component ignore) {
    for (val o : wires.points.getComponents(loc)) {
      if (o != ignore) return true;
    }
    return false;
  }

  void mutatorAdd(Component c) {
    locker.checkForWritePermission("add", this);

    isAnnotated = false;
    netList.clear();
    if (c instanceof Wire) {
      val w = (Wire) c;
      if (w.getEnd0().equals(w.getEnd1())) return;
      var added = wires.add(w);
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
        val labels = new HashSet<String>();
        for (val comp : comps) {
          if (comp.equals(c) || comp.getFactory() instanceof Tunnel) continue;
          if (comp.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
            val label = comp.getAttributeSet().getValue(StdAttr.LABEL);
            if (label != null && !label.isEmpty()) labels.add(label.toUpperCase());
          }
        }
        /* we also have to check for the entity name */
        if (getName() != null && !getName().isEmpty()) labels.add(getName());
        val label = c.getAttributeSet().getValue(StdAttr.LABEL);
        if (label != null && !label.isEmpty() && labels.contains(label.toUpperCase()))
          c.getAttributeSet().setValue(StdAttr.LABEL, "");
      }
      wires.add(c);
      val factory = c.getFactory();
      if (factory instanceof Clock) {
        clocks.add(c);
      } else if (factory instanceof Rom) {
        Rom.closeHexFrame(c);
      } else if (factory instanceof SubcircuitFactory) {
        val subcirc = (SubcircuitFactory) factory;
        subcirc.getSubcircuit().circuitsUsingThis.put(c, this);
      } else if (factory instanceof VhdlEntity) {
        val vhdl = (VhdlEntity) factory;
        vhdl.addCircuitUsing(c, this);
      }
      c.addComponentListener(myComponentListener);
    }
    RemoveWrongLabels(c.getFactory().getName());
    fireEvent(CircuitEvent.ACTION_ADD, c);
  }

  public void mutatorClear() {
    locker.checkForWritePermission("clear", this);

    val oldComps = comps;
    comps = new LinkedHashSet<>();
    wires = new CircuitWires();
    clocks.clear();
    netList.clear();
    isAnnotated = false;
    for (val comp : oldComps) {
      socSim.removeComponent(comp);
      val factory = comp.getFactory();
      factory.removeComponent(this, comp, project.getCircuitState(this));
    }
    fireEvent(CircuitEvent.ACTION_CLEAR, oldComps);
  }

  void mutatorRemove(Component c) {
    locker.checkForWritePermission("remove", this);

    isAnnotated = false;
    netList.clear();
    if (c instanceof Wire) {
      wires.remove(c);
    } else {
      wires.remove(c);
      comps.remove(c);
      socSim.removeComponent(c);
      val factory = c.getFactory();
      factory.removeComponent(this, c, project.getCircuitState(this));
      if (factory instanceof Clock) {
        clocks.remove(c);
      } else if (factory instanceof DynamicElementProvider) {
        DynamicElementProvider.removeDynamicElements(this, c);
      }
      c.removeComponentListener(myComponentListener);
    }
    fireEvent(CircuitEvent.ACTION_REMOVE, c);
  }

  private void RemoveWrongLabels(String label) {
    var haveAChange = false;
    for (val comp : comps) {
      val attrs = comp.getAttributeSet();
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        val compLabel = attrs.getValue(StdAttr.LABEL);
        if (label.equalsIgnoreCase(compLabel)) {
          attrs.setValue(StdAttr.LABEL, "");
          haveAChange = true;
        }
      }
    }
    // we do not have to check the wires as (1) Wire is a reserved keyword,
    // and (2) they cannot have a label
    if (haveAChange)
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
    staticAttributes.setValue(CircuitAttributes.NAME_ATTR, name);
  }

  @Override
  public String toString() {
    return staticAttributes.getValue(CircuitAttributes.NAME_ATTR);
  }

  public static class TimeoutSimulation extends TimerTask {

    /* Make it atomic */
    private volatile boolean timedOut;

    public TimeoutSimulation() {
      timedOut = false;
    }

    public boolean getTimeout() {
      return timedOut;
    }

    @Override
    public void run() {
      timedOut = true;
    }
  }

  public double getTickFrequency() {
    return staticAttributes.getValue(CircuitAttributes.SIMULATION_FREQUENCY);
  }

  public void setTickFrequency(double value) {
    val currentTickFrequency = staticAttributes.getValue(CircuitAttributes.SIMULATION_FREQUENCY);
    if (value == currentTickFrequency) return;
    staticAttributes.setValue(CircuitAttributes.SIMULATION_FREQUENCY, value);
    if ((project != null) && (currentTickFrequency > 0)) project.setForcedDirty();
  }

  public double getDownloadFrequency() {
    return staticAttributes.getValue(CircuitAttributes.DOWNLOAD_FREQUENCY);
  }

  public void setDownloadFrequency(double value) {
    if (value == staticAttributes.getValue(CircuitAttributes.DOWNLOAD_FREQUENCY)) return;
    staticAttributes.setValue(CircuitAttributes.DOWNLOAD_FREQUENCY, value);
    if (project != null) project.setForcedDirty();
  }
}
