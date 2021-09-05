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
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
          if (end != null) {
            map.put(end.getLocation(), end);
          }
        }
      } else if (val instanceof EndData) {
        final var end = (EndData) val;
        map.put(end.getLocation(), end);
      }
      return map;
    }

    @Override
    public void LabelChanged(ComponentEvent e) {
      final var attre = (AttributeEvent) e.getData();
      if (attre.getSource() == null || attre.getValue() == null) {
        return;
      }
      final var newLabel = (String) attre.getValue();
      final var oldLabel = attre.getOldValue() != null ? (String) attre.getOldValue() : "";
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

  private static boolean IsExistingLabel(
      String name, AttributeSet me, Set<Component> comps, Boolean showDialog) {
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

  private static final int MAX_TIMEOUT_TEST_BENCH_SEC = 60000;
  private final MyComponentListener myComponentListener = new MyComponentListener();
  private final CircuitAppearance appearance;
  private final AttributeSet staticAttrs;
  private final SubcircuitFactory subcircuitFactory;
  private final EventSourceWeakSupport<CircuitListener> listeners = new EventSourceWeakSupport<>();
  private LinkedHashSet<Component> comps = new LinkedHashSet<>(); // doesn't
  // include
  // wires
  CircuitWires wires = new CircuitWires();
  private final ArrayList<Component> clocks = new ArrayList<>();
  private final CircuitLocker locker;

  static final Logger logger = LoggerFactory.getLogger(Circuit.class);

  private final WeakHashMap<Component, Circuit> circuitsUsingThis;
  private final Netlist myNetList;
  private final HashMap<String, MappableResourcesContainer> myMappableResources;
  private final HashMap<String, HashMap<String, CircuitMapInfo>> loadedMaps;
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

  public void SetProject(Project proj) {
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

  public void RecalcDefaultShape() {
    if (appearance.isDefaultAppearance()) {
      appearance.recomputeDefaultAppearance();
    }
  }

  private static String GetAnnotationName(Component comp) {
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

  public void Annotate(Project proj, boolean clearExistingLabels, boolean insideLibrary) {
    if (this.proj == null) this.proj = proj;
    this.Annotate(clearExistingLabels, insideLibrary);
  }

  public void Annotate(boolean clearExistingLabels, boolean insideLibrary) {
    /* If I am already completely annotated, return */
    if (isAnnotated) {
      Reporter.Report.AddInfo("Nothing to do !");
      return;
    }
    SortedSet<Component> comps = new TreeSet<>(Location.CompareVertical);
    final var lablers = new HashMap<String, AutoLabel>();
    final var labelNames = new LinkedHashSet<String>();
    final var subCircuits = new LinkedHashSet<String>();
    for (Component comp : getNonWires()) {
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
            Reporter.Report.AddSevereWarning(
                "Removed duplicated label " + this.getName() + "/" + label);
          } else {
            labelNames.add(label.toUpperCase());
          }
        }
      }
      /* now we only process those that require a label */
      if (comp.getFactory().RequiresNonZeroLabel()) {
        if (clearExistingLabels) {
          /* in case of label cleaning, we clear first the old label */
          Reporter.Report.AddInfo(
              "Cleared " + this.getName() + "/" + comp.getAttributeSet().getValue(StdAttr.LABEL));
          final var act = new SetAttributeAction(this, S.getter("changeComponentAttributesAction"));
          act.set(comp, StdAttr.LABEL, "");
          proj.doAction(act);
        }
        if (comp.getAttributeSet().getValue(StdAttr.LABEL).isEmpty()) {
          comps.add(comp);
          final var componentName = GetAnnotationName(comp);
          if (!lablers.containsKey(componentName)) {
            lablers.put(componentName, new AutoLabel(componentName + "_0", this));
          }
        }
      }
      /* if the current component is a sub-circuit, recurse into it */
      if (comp.getFactory() instanceof SubcircuitFactory) {
        final var sub = (SubcircuitFactory) comp.getFactory();
        subCircuits.add(sub.getName());
      }
    }
    /* Now Annotate */
    var sizeMightHaveChanged = false;
    for (final var comp : comps) {
      final var componentName = GetAnnotationName(comp);
      if (!lablers.containsKey(componentName) || !lablers.get(componentName).hasNext(this)) {
        /* This should never happen! */
        Reporter.Report.AddFatalError(
            "Annotate internal Error: Either there exists duplicate labels or the label syntax is incorrect!\nPlease try annotation on labeled components also\n");
        return;
      } else {
        final var newLabel = lablers.get(componentName).getNext(this, comp.getFactory());
        final var act = new SetAttributeAction(this, S.getter("changeComponentAttributesAction"));
        act.set(comp, StdAttr.LABEL, newLabel);
        proj.doAction(act);
        Reporter.Report.AddInfo("Labeled " + this.getName() + "/" + newLabel);
        if (comp.getFactory() instanceof Pin) {
          sizeMightHaveChanged = true;
        }
      }
    }
    if (!comps.isEmpty() & insideLibrary) {
      Reporter.Report.AddSevereWarning(
          "Annotated the circuit \""
              + this.getName()
              + "\" which is inside a library these changes will not be saved!");
    }
    if (sizeMightHaveChanged)
      Reporter.Report.AddSevereWarning(
          "Annotated one ore more pins in circuit \""
              + this.getName()
              + "\" this might have changed it's boxsize and might have impacted it's connections in circuits using this one!");
    isAnnotated = true;
    /* Now annotate all circuits below me */
    for (final var subs : subCircuits) {
      final var circ = LibraryTools.getCircuitFromLibs(proj.getLogisimFile(), subs.toUpperCase());
      final var inLibrary = !proj.getLogisimFile().getCircuits().contains(circ);
      circ.Annotate(proj, clearExistingLabels, inLibrary);
    }
  }

  //
  // Annotation module for all components that require a non-zero-length label
  public void ClearAnnotationLevel() {
    isAnnotated = false;
    myNetList.clear();
    for (final var comp : this.getNonWires()) {
      if (comp.getFactory() instanceof SubcircuitFactory) {
        final var sub = (SubcircuitFactory) comp.getFactory();
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
    final var state = project.getCircuitState();
    /* This is introduced in order to not block in case both the signal never happend*/
    final var pinsState = new InstanceState[pin.length];
    final var vPins = new Value[pin.length];
    state.reset();

    final var ts = new TimeoutSimulation();
    final var timer = new Timer();
    timer.schedule(ts, MAX_TIMEOUT_TEST_BENCH_SEC);

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

      if (ts.getTimeout()) {
        return false;
      }
    }
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  public void doTestVector(Project project, Instance[] pin, Value[] val) throws TestException {
    final var state = project.getCircuitState();
    state.reset();

    for (var i = 0; i < pin.length; ++i) {
      if (Pin.FACTORY.isInputPin(pin[i])) {
        final var pinState = state.getInstanceState(pin[i]);
        Pin.FACTORY.setValue(pinState, val[i]);
      }
    }

    final var prop = state.getPropagator();

    try {
      prop.propagate();
    } catch (Throwable thr) {
      thr.printStackTrace();
    }

    if (prop.isOscillating()) throw new TestException("oscillation detected");

    FailException err = null;

    for (var i = 0; i < pin.length; i++) {
      final var pinState = state.getInstanceState(pin[i]);
      if (Pin.FACTORY.isInputPin(pin[i])) continue;

      final var v = Pin.FACTORY.getValue(pinState);
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

    if (hidden == null || hidden.size() == 0) {
      for (final var c : comps) {
        final var gNew = g.create();
        context.setGraphics(gNew);
        gCopy.dispose();
        gCopy = gNew;

        c.draw(context);
      }
    } else {
      for (Component c : comps) {
        if (!hidden.contains(c)) {
          final var gNew = g.create();
          context.setGraphics(gNew);
          gCopy.dispose();
          gCopy = gNew;

          try {
            c.draw(context);
          } catch (RuntimeException e) {
            // this is a JAR developer error - display it and move
            // on
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
    if (wireBounds.getWidth() == 0 || wireBounds.getHeight() == 0) {
      return compBounds;
    } else {
      return compBounds.add(wireBounds);
    }
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
        int x0 = bds.getX();
        int x1 = x0 + bds.getWidth();
        int y0 = bds.getY();
        int y1 = y0 + bds.getHeight();
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

  public ArrayList<Component> getClocks() {
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

  public void addLoadedMap(String boardName, HashMap<String, CircuitMapInfo> map) {
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

  public Set<Location> getSplitLocations() {
    return wires.points.getSplitLocations();
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
    final var loc = comp.getLocation();
    final var existing = wires.points.getNonWires(loc);
    for (final var existingComp : existing) {
      if (existingComp.getFactory().equals(comp.getFactory())) {
        /* we make an exception for the pin in case we have an input placed on an output */
        if (comp.getFactory() instanceof Pin) {
          final var dir1 = comp.getAttributeSet().getValue(Pin.ATTR_TYPE);
          final var dir2 = existingComp.getAttributeSet().getValue(Pin.ATTR_TYPE);
          if (dir1 == dir2) return true;
        } else { 
          return true;
        }
      }
    }
    return false;
  }

  public boolean isConnected(Location loc, Component ignore) {
    for (Component o : wires.points.getComponents(loc)) {
      if (o != ignore) return true;
    }
    return false;
  }

  void mutatorAdd(Component c) {
    // logger.debug("mutatorAdd: {}", c);
    locker.checkForWritePermission("add", this);

    isAnnotated = false;
    myNetList.clear();
    if (c instanceof Wire) {
      final var w = (Wire) c;
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
        final var labels = new HashSet<String>();
        for (Component comp : comps) {
          if (comp.equals(c) || comp.getFactory() instanceof Tunnel) continue;
          if (comp.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
            final var label = comp.getAttributeSet().getValue(StdAttr.LABEL);
            if (label != null && !label.isEmpty()) labels.add(label.toUpperCase());
          }
        }
        /* we also have to check for the entity name */
        if (getName() != null && !getName().isEmpty()) labels.add(getName());
        final var label = c.getAttributeSet().getValue(StdAttr.LABEL);
        if (label != null && !label.isEmpty() && labels.contains(label.toUpperCase()))
          c.getAttributeSet().setValue(StdAttr.LABEL, "");
      }
      wires.add(c);
      final var factory = c.getFactory();
      if (factory instanceof Clock) {
        clocks.add(c);
      } else if (factory instanceof Rom) {
        Rom.closeHexFrame(c);
      } else if (factory instanceof SubcircuitFactory) {
        final var subcirc = (SubcircuitFactory) factory;
        subcirc.getSubcircuit().circuitsUsingThis.put(c, this);
      } else if (factory instanceof VhdlEntity) {
        final var vhdl = (VhdlEntity) factory;
        vhdl.addCircuitUsing(c, this);
      }
      c.addComponentListener(myComponentListener);
    }
    RemoveWrongLabels(c.getFactory().getName());
    fireEvent(CircuitEvent.ACTION_ADD, c);
  }

  public void mutatorClear() {
    locker.checkForWritePermission("clear", this);

    Set<Component> oldComps = comps;
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
    // logger.debug("mutatorRemove: {}", c);

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

  private void RemoveWrongLabels(String label) {
    var haveAChange = false;
    for (final var comp : comps) {
      final var attrs = comp.getAttributeSet();
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        final var compLabel = attrs.getValue(StdAttr.LABEL);
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

    public boolean getTimeout() {
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
    if (value == currentTickFrequency) return;
    staticAttrs.setValue(CircuitAttributes.SIMULATION_FREQUENCY, value);
    if ((proj != null) && (currentTickFrequency > 0)) proj.setForcedDirty();
  }

  public double getDownloadFrequency() {
    return staticAttrs.getValue(CircuitAttributes.DOWNLOAD_FREQUENCY);
  }

  public void setDownloadFrequency(double value) {
    if (value == staticAttrs.getValue(CircuitAttributes.DOWNLOAD_FREQUENCY)) return;
    staticAttrs.setValue(CircuitAttributes.DOWNLOAD_FREQUENCY, value);
    if (proj != null) proj.setForcedDirty();
  }
}
