/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.DualPortRam;
import com.cburch.logisim.std.memory.Rom;
import com.cburch.logisim.util.CollectionUtil;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SelectionBase {

  static final Logger logger = LoggerFactory.getLogger(SelectionBase.class);
  static final Set<Component> NO_COMPONENTS = Collections.emptySet();
  final HashSet<Component> selected = new HashSet<>(); // of selected
  // Components in circuit
  final HashSet<Component> lifted = new HashSet<>(); // of selected
  final HashSet<Component> suppressHandles = new HashSet<>(); // of
  // Components
  final Set<Component> unionSet = CollectionUtil.createUnmodifiableSetUnion(selected, lifted);
  private final ArrayList<Selection.Listener> listeners = new ArrayList<>();
  final Project proj;
  // Components removed
  private Bounds bounds = Bounds.EMPTY_BOUNDS;
  private boolean shouldSnap = false;

  public SelectionBase(Project proj) {
    this.proj = proj;
  }

  private static Bounds computeBounds(Collection<Component> components) {
    if (components.isEmpty()) {
      return Bounds.EMPTY_BOUNDS;
    } else {
      final var it = components.iterator();
      var ret = it.next().getBounds();
      while (it.hasNext()) {
        final var comp = it.next();
        final var bds = comp.getBounds();
        ret = ret.add(bds);
      }
      return ret;
    }
  }

  private static boolean shouldSnapComponent(Component comp) {
    final var shouldSnapValue =
        (Boolean)
            comp.getFactory().getFeature(ComponentFactory.SHOULD_SNAP, comp.getAttributeSet());
    return shouldSnapValue == null || shouldSnapValue;
  }

  //
  // action methods
  //
  public void add(Component comp) {
    if (selected.add(comp)) {
      fireSelectionChanged();
    }
  }

  public void addAll(Collection<? extends Component> comps) {
    if (selected.addAll(comps)) {
      fireSelectionChanged();
    }
  }

  //
  // listener methods
  //
  public void addListener(Selection.Listener l) {
    listeners.add(l);
  }

  void clear(CircuitMutation xn) {
    clear(xn, true);
  }

  // removes all from selection - NOT from circuit
  void clear(CircuitMutation xn, boolean dropLifted) {
    if (selected.isEmpty() && lifted.isEmpty()) return;

    if (dropLifted && !lifted.isEmpty()) {
      xn.addAll(lifted);
    }

    selected.clear();
    lifted.clear();
    shouldSnap = false;
    bounds = Bounds.EMPTY_BOUNDS;

    fireSelectionChanged();
  }

  //
  // private methods
  //
  private void computeShouldSnap() {
    shouldSnap = false;
    for (final var  comp : unionSet) {
      if (shouldSnapComponent(comp)) {
        shouldSnap = true;
        return;
      }
    }
  }

  private HashMap<Component, Component> copyComponents(Collection<Component> components, boolean translate) {
    // determine translation offset where we can legally place the clipboard
    int dx;
    int dy;
    final var bds = computeBounds(components);
    for (int index = 0; ; index++) {
      // compute offset to try: We try points along successively larger
      // squares radiating outward from 0,0
      if (index == 0) {
        dx = 0;
        dy = 0;
      } else {
        int side = 1;
        while (side * side <= index) side += 2;
        int offs = index - (side - 2) * (side - 2);
        dx = side / 2;
        dy = side / 2;
        if (offs < side - 1) { // top edge of square
          dx -= offs;
        } else if (offs < 2 * (side - 1)) { // left edge
          offs -= side - 1;
          dx = -dx;
          dy -= offs;
        } else if (offs < 3 * (side - 1)) { // right edge
          offs -= 2 * (side - 1);
          dx = -dx + offs;
          dy = -dy;
        } else {
          offs -= 3 * (side - 1);
          dy = -dy + offs;
        }
        dx *= 10;
        dy *= 10;
      }

      if (bds.getX() + dx >= 0
          && bds.getY() + dy >= 0
          && !hasConflictTranslated(components, dx, dy, true)) {
        return copyComponents(components, dx, dy, translate);
      }
    }
  }

  private HashMap<Component, Component> copyComponents(Collection<Component> components, int dx, int dy, boolean translate) {
    final var ret = new HashMap<Component, Component>();
    for (final var comp : components) {
      final var oldLoc = comp.getLocation();
      final var attrs =
          translate || (comp.getFactory() instanceof Rom) || ((comp.getFactory() instanceof Ram || comp.getFactory() instanceof DualPortRam))
              ? comp.getAttributeSet()
              : (AttributeSet) comp.getAttributeSet().clone();
      var newX = oldLoc.getX() + dx;
      var newY = oldLoc.getY() + dy;
      final var snap = comp.getFactory().getFeature(ComponentFactory.SHOULD_SNAP, attrs);
      if (snap == null || (Boolean) snap) {
        newX = Canvas.snapXToGrid(newX);
        newY = Canvas.snapYToGrid(newY);
      }
      final var newLoc = Location.create(newX, newY, false);
      final var copy = comp.getFactory().createComponent(newLoc, attrs);
      ret.put(comp, copy);
    }
    return ret;
  }

  void deleteAllHelper(CircuitMutation xn) {
    for (final var comp : selected) {
      xn.remove(comp);
    }
    selected.clear();
    lifted.clear();
    fireSelectionChanged();
  }

  void dropAll(CircuitMutation xn) {
    if (!lifted.isEmpty()) {
      xn.addAll(lifted);
      selected.addAll(lifted);
      lifted.clear();
    }
  }

  void duplicateHelper(CircuitMutation xn) {
    final var oldSelected = new HashSet<Component>(selected);
    oldSelected.addAll(lifted);
    pasteHelper(xn, oldSelected);
  }

  public void fireSelectionChanged() {
    bounds = null;
    computeShouldSnap();
    final var e = new Selection.Event(this);
    for (final var l : listeners) {
      l.selectionChanged(e);
    }
  }

  //
  // query methods
  //
  public Bounds getBounds() {
    if (bounds == null) {
      bounds = computeBounds(unionSet);
    }
    return bounds;
  }

  public Bounds getBounds(Graphics g) {
    final var it = unionSet.iterator();
    if (it.hasNext()) {
      bounds = it.next().getBounds(g);
      while (it.hasNext()) {
        final var comp = it.next();
        final var bds = comp.getBounds(g);
        bounds = bounds.add(bds);
      }
    } else {
      bounds = Bounds.EMPTY_BOUNDS;
    }
    return bounds;
  }

  private boolean hasConflictTranslated(Collection<Component> components,
                                        int dx, int dy, boolean selfConflicts) {
    final var circuit = proj.getCurrentCircuit();
    if (circuit == null) return false;
    for (final var comp : components) {
      if (!(comp instanceof Wire)) {
        for (final var endData : comp.getEnds()) {
          if (endData != null && endData.isExclusive()) {
            final var endLoc = endData.getLocation().translate(dx, dy);
            final var conflict = circuit.getExclusive(endLoc);
            if (conflict != null) {
              if (selfConflicts || !components.contains(conflict)) return true;
            }
          }
        }
        final var newLoc = comp.getLocation().translate(dx, dy);
        final var newBounds = comp.getBounds().translate(dx, dy);
        for (final var comp2 : circuit.getAllContaining(newLoc)) {
          final var bds = comp2.getBounds();
          if (bds.equals(newBounds)) {
            if (selfConflicts || !components.contains(comp2)) return true;
          }
        }
      }
    }
    return false;
  }

  public boolean hasConflictWhenMoved(int dx, int dy) {
    return hasConflictTranslated(unionSet, dx, dy, false);
  }

  void pasteHelper(CircuitMutation xn, Collection<Component> comps) {
    clear(xn);

    final var newLifted = copyComponents(comps, false);
    lifted.addAll(newLifted.values());
    fireSelectionChanged();
  }

  // debugging methods
  public void print() {
    logger.debug(" shouldSnap: {}", shouldSnap());

    var hasPrinted = false;
    for (final var comp : selected) {
      if (hasPrinted) logger.debug("       : {}  [{}]", comp, comp.hashCode());
      else logger.debug(" select: {}  [{}]", comp, comp.hashCode());
      hasPrinted = true;
    }

    hasPrinted = false;
    for (final var comp : lifted) {
      if (hasPrinted) logger.debug("       : {}  [{}]", comp, comp.hashCode());
      else logger.debug(" lifted: {}  [{}]", comp, comp.hashCode());
      hasPrinted = true;
    }
  }

  // removes from selection - NOT from circuit
  void remove(CircuitMutation xn, Component comp) {
    var removed = selected.remove(comp);
    if (lifted.contains(comp)) {
      if (xn == null) {
        throw new IllegalStateException("cannot remove");
      } else {
        lifted.remove(comp);
        removed = true;
        xn.add(comp);
      }
    }

    if (removed) {
      if (shouldSnapComponent(comp)) computeShouldSnap();
      fireSelectionChanged();
    }
  }

  public void removeListener(Selection.Listener l) {
    listeners.remove(l);
  }

  public void setSuppressHandles(Collection<Component> toSuppress) {
    suppressHandles.clear();
    if (toSuppress != null) suppressHandles.addAll(toSuppress);
  }

  public boolean shouldSnap() {
    return shouldSnap;
  }

  void translateHelper(CircuitMutation xn, int dx, int dy) {
    final var translatedComps = copyComponents(selected, dx, dy, true);
    for (final var entry : translatedComps.entrySet()) {
      xn.replace(entry.getKey(), entry.getValue());
      selected.add(entry.getValue());
    }

    final var liftedAfter = copyComponents(lifted, dx, dy, true);
    lifted.clear();
    for (final var entry : liftedAfter.entrySet()) {
      xn.add(entry.getValue());
      selected.add(entry.getValue());
    }
    fireSelectionChanged();
  }
}
