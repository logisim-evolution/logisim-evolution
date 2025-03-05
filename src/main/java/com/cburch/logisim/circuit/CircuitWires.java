/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.PullResistor;
import com.cburch.logisim.std.wiring.Tunnel;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.IteratorUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CircuitWires {

  static class BundleMap {
    final HashMap<Location, WireBundle> pointBundles = new HashMap<>();
    final HashSet<WireBundle> bundles = new HashSet<>();
    ArrayList<Location> allLocations = new ArrayList<>();
    HashMap<Location, ArrayList> componentsAtLocations = new HashMap<>();
    boolean isValid = true;
    // NOTE: It would make things more efficient if we also had
    // a set of just the first bundle in each tree.
    HashSet<WidthIncompatibilityData> incompatibilityData = null;

    void addWidthIncompatibilityData(WidthIncompatibilityData e) {
      if (incompatibilityData == null) {
        incompatibilityData = new HashSet<>();
      }
      incompatibilityData.add(e);
    }

    WireBundle createBundleAt(Location p) {
      var ret = pointBundles.get(p);
      if (ret == null) {
        ret = new WireBundle(p);
        pointBundles.put(p, ret);
        bundles.add(ret);
      }
      return ret;
    }

    WireBundle getBundleAt(Location p) {
      return pointBundles.get(p);
    }

    Set<Location> getBundlePoints() {
      return pointBundles.keySet();
    }

    Set<WireBundle> getBundles() {
      return bundles;
    }

    HashSet<WidthIncompatibilityData> getWidthIncompatibilityData() {
      return incompatibilityData;
    }

    void invalidate() {
      isValid = false;
    }

    boolean isValid() {
      return isValid;
    }

    void setBundleAt(Location p, WireBundle b) {
      pointBundles.put(p, b);
    }
  }

  static class SplitterData {
    final WireBundle[] endBundle; // PointData associated with each end

    SplitterData(int fanOut) {
      endBundle = new WireBundle[fanOut + 1];
    }
  }

  static class ValuedThread {
    int steps;
    ValuedBus[] bus;
    int[] position;
    boolean pullUp, pullDown;
    Value val = null;

    ValuedThread(WireThread t, HashMap<WireBundle, ValuedBus> allBuses) {
      steps = t.steps;
      position = t.position;
      bus = new ValuedBus[steps];
      for (int i = 0; i < steps; i++) {
        WireBundle b = t.bundle[i];
        bus[i] = allBuses.get(b);
        Value pullHere = b.getPullValue();
        pullUp |= (pullHere == Value.TRUE);
        pullDown |= (pullHere == Value.FALSE);
      }
      if (pullUp && pullDown)
        pullUp = pullDown = false;
    }

    Value recalculate() {
      Value ret = Value.UNKNOWN;
      for (int i = 0; i < steps; i++) {
        ValuedBus vb = bus[i];
        int pos = position[i];
        Value val = vb.valAtPointSum;
        if (val != Value.NIL) {
          ret = ret.combine(val.get(pos));
        }
      }
      if (ret != Value.UNKNOWN)
        return ret;
      else if (pullUp)
        return Value.TRUE;
      else if (pullDown)
        return Value.FALSE;
      else
        return Value.UNKNOWN;
    }
  }

  static class ValuedBus {
    int idx = -1;
    ValuedThread[] threads;
    Location[] componentPoints; // subset of wire bundle xpoints that have components at them
    Component[][] componentsAffected; // components at each of those points
    Value[] valAtPoint;
    Value valAtPointSum; // cached sum of valAtPoint, or null if dirty
    Value val; // cached final value for bus
    int width;
    ValuedBus[] dependentBuses; // buses affected if this one changes value.
    boolean dirty;

    ValuedBus(WireBundle wb, BundleMap bm) {
      idx = -1; // filled in by caller
      filterComponents(bm, wb.xpoints);
      valAtPoint = new Value[componentPoints.length];
      width = wb.threads == null ? -1 : wb.getWidth().getWidth();
      dirty = true;
    }

    void filterComponents(BundleMap bm, Location[] locs) {
      ArrayList<Location> found = new ArrayList<>();
      ArrayList<ArrayList<Component>> affected = new ArrayList<>();
      for (Location p : locs) {
        @SuppressWarnings("unchecked")
        ArrayList<Component> a = bm.componentsAtLocations.get(p);
        if (a == null)
          continue;
        found.add(p);
        affected.add(a);
      }
      int n = found.size();
      componentPoints = n == locs.length ? locs : found.toArray(new Location[n]);
      componentsAffected = new Component[n][];
      for (int i = 0; i < n; i++) {
        ArrayList<Component> a = affected.get(i);
        componentsAffected[i] = a.toArray(new Component[a.size()]);
      }
    }

    void makeThreads(WireThread[] wbthreads, HashMap<WireBundle, ValuedBus> allBuses,
                     HashMap<WireThread, ValuedThread> allThreads) {
      if (width <= 0)
        return;
      threads = new ValuedThread[width];
      for (int i = 0; i < width; i++) {
        WireThread t = wbthreads[i];
        threads[i] = allThreads.get(t);
        if (threads[i] == null) {
          threads[i] = new ValuedThread(t, allBuses);
          allThreads.put(t, threads[i]);
        }
      }
    }

    Value recalculate() {
      if (width == 1) {
        Value tv = threads[0].val;
        if (tv == null)
          tv = threads[0].val = threads[0].recalculate();
        return tv;
      }
      long error = 0, unknown = 0, value = 0;
      for (int i = 0; i < width; i++) {
        long mask = 1L << i;
        Value tv = threads[i].val;
        if (tv == null)
          tv = threads[i].val = threads[i].recalculate();
        if (tv == Value.TRUE)
          value |= mask;
        else if (tv == Value.FALSE)
          ;
        else if (tv == Value.UNKNOWN)
          unknown |= mask;
        else
          error |= mask;
      }
      return Value.create_unsafe(width, error, unknown, value);
    }
  }

  State newState(CircuitState circState) {
    return new State(circState, getBundleMap());
  }

  static class State {
    private BundleMap bundleMap; // original source of connectivity info
    ValuedBus[] buses;
    int numDirty;
    HashMap<Location, ValuedBus> busAt = new HashMap<>();

    State(CircuitState circState, BundleMap bundleMap) {
      this.bundleMap = bundleMap;
      HashMap<WireBundle, ValuedBus> allBuses = new HashMap<>();
      HashMap<ValuedBus, WireBundle> srcBuses = new HashMap<>();
      buses = new ValuedBus[bundleMap.bundles.size()];
      int i = 0;
      for (WireBundle wb : bundleMap.bundles) {
        ValuedBus vb = new ValuedBus(wb, bundleMap);
        vb.idx = i++;
        buses[vb.idx] = vb;
        for (Location loc : wb.xpoints) {
          ValuedBus old = busAt.put(loc, vb);
          if (old != null) {
            throw new IllegalStateException("oops, two wires occupy same location");
          }
        }
        allBuses.put(wb, vb);
        srcBuses.put(vb, wb);
      }
      HashMap<WireThread, ValuedThread> allThreads = new HashMap<>();
      for (ValuedBus vb : buses) {
        vb.makeThreads(srcBuses.get(vb).threads, allBuses, allThreads);
        if (circState != null) {
          for (int j = 0; j < vb.componentPoints.length; j++) {
            Value val = Propagator.getDrivenValueAt(circState, vb.componentPoints[j]);
            vb.valAtPoint[j] = val;
          }
        }
      }
      for (ValuedBus vb : buses) {
        if (vb.threads == null)
          continue;
        HashSet<ValuedBus> deps = new HashSet<>();
        for (ValuedThread t : vb.threads)
          for (ValuedBus dep : t.bus)
            if (dep != vb)
              deps.add(dep);
        int n = deps.size();
        if (n == 0)
          continue;
        vb.dependentBuses = deps.toArray(new ValuedBus[n]);
      }
      numDirty = buses.length;
    }

    void markClean(ValuedBus vb) {
      if (!vb.dirty) {
        throw new IllegalStateException("can't clean element that is not dirty");
      }
      if (vb.idx > numDirty - 1) {
        throw new IllegalStateException("bad position for dirty element");
      }
      if (vb.idx < numDirty - 1) {
        ValuedBus other = buses[numDirty - 1];
        other.idx = vb.idx;
        buses[other.idx] = other;
        vb.idx = numDirty - 1;
        buses[vb.idx] = vb;
      }
      vb.dirty = false;
      numDirty--;
    }

    void markDirty(ValuedBus vb) {
      if (vb.dirty) {
        throw new IllegalStateException("can't mark dirty element as dirty");
      }
      if (vb.idx < numDirty) {
        throw new IllegalStateException("bad position for clean element");
      }
      if (vb.idx > numDirty) {
        ValuedBus other = buses[numDirty];
        other.idx = vb.idx;
        buses[other.idx] = other;
        vb.idx = numDirty;
        buses[vb.idx] = vb;
      }
      if (vb.threads != null) {
        for (ValuedThread vt : vb.threads)
          vt.val = null;
      }
      vb.dirty = true;
      numDirty++;
    }
  }

  private class TunnelListener implements AttributeListener {
    @Override
    public void attributeListChanged(AttributeEvent e) {
      // do nothing.
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      final var attr = e.getAttribute();
      if (attr == StdAttr.LABEL || attr == PullResistor.ATTR_PULL_TYPE) {
        voidBundleMap();
      }
    }
  }

  private static Value pullValue(Value base, Value pullTo) {
    if (base.isFullyDefined()) {
      return base;
    } else if (base.getWidth() == 1) {
      if (base == Value.UNKNOWN) return pullTo;
      else return base;
    } else {
      final var ret = base.getAll();
      for (var i = 0; i < ret.length; i++) {
        if (ret[i] == Value.UNKNOWN) ret[i] = pullTo;
      }
      return Value.create(ret);
    }
  }

  static final Logger logger = LoggerFactory.getLogger(CircuitWires.class);

  // user-given data
  private final HashSet<Wire> wires = new HashSet<>();
  private final HashSet<Splitter> splitters = new HashSet<>();
  private final HashSet<Component> tunnels = new HashSet<>(); // of
  // Components
  // with
  // Tunnel
  // factory
  private final TunnelListener tunnelListener = new TunnelListener();
  private final HashSet<Component> pulls = new HashSet<>(); // of
  // Components
  // with
  // PullResistor
  // factory

  final CircuitPoints points = new CircuitPoints();
  private Bounds bounds = Bounds.EMPTY_BOUNDS;

  private volatile BundleMap masterBundleMap = null;

  CircuitWires() {}

  // NOTE: this could be made much more efficient in most cases to
  // avoid voiding the bundle map.
  /*synchronized*/ boolean add(Component comp) {
    var added = true;
    if (comp instanceof Wire wire) {
      added = addWire(wire);
    } else if (comp instanceof Splitter splitter) {
      splitters.add(splitter);
    } else {
      final var factory = comp.getFactory();
      if (factory instanceof Tunnel) {
        tunnels.add(comp);
        comp.getAttributeSet().addAttributeListener(tunnelListener);
      } else if (factory instanceof PullResistor) {
        pulls.add(comp);
        comp.getAttributeSet().addAttributeListener(tunnelListener);
      }
    }
    if (added) {
      points.add(comp);
      voidBundleMap();
    }
    return added;
  }

  /*synchronized*/ void add(Component comp, EndData end) {
    points.add(comp, end);
    voidBundleMap();
  }

  private boolean addWire(Wire w) {
    final var added = wires.add(w);
    if (!added) return false;

    if (bounds != Bounds.EMPTY_BOUNDS) { // update bounds
      bounds = bounds.add(w.e0).add(w.e1);
    }
    return true;
  }

  // To be called by getBundleMap only
  private void computeBundleMap(BundleMap ret) {
    // create bundles corresponding to wires and tunnels
    connectWires(ret);
    connectTunnels(ret);
    connectPullResistors(ret);

    // merge any WireBundle objects united by previous steps
    for (final var it = ret.getBundles().iterator(); it.hasNext(); ) {
      final var b = it.next();
      final var bpar = b.find();
      if (bpar != b) { // b isn't group's representative
        for (final var pt : b.tempPoints) {
          ret.setBundleAt(pt, bpar);
        }
        bpar.tempPoints.addAll(b.tempPoints);
        bpar.addPullValue(b.getPullValue());
        it.remove();
      }
    }

    // make a WireBundle object for each end of a splitter
    for (final var spl : splitters) {
      final var ends = new ArrayList<>(spl.getEnds());
      for (final var end : ends) {
        final var p = end.getLocation();
        final var pb = ret.createBundleAt(p);
        pb.setWidth(end.getWidth(), p);
      }
    }

    // set the width for each bundle whose size is known
    // based on components
    for (final var p : ret.getBundlePoints()) {
      final var pb = ret.getBundleAt(p);
      final var width = points.getWidth(p);
      if (width != BitWidth.UNKNOWN) {
        pb.setWidth(width, p);
      }
    }

    // determine the bundles at the end of each splitter
    for (final var spl : splitters) {
      final var ends = new ArrayList<>(spl.getEnds());
      int index = -1;
      for (final var end : ends) {
        index++;
        final var p = end.getLocation();
        final var pb = ret.getBundleAt(p);
        if (pb != null) {
          pb.setWidth(end.getWidth(), p);
          spl.wireData.endBundle[index] = pb;
        }
      }
    }

    // finish constructing the bundles, start constructing the threads
    for (WireBundle b : ret.getBundles()) {
      b.xpoints = b.tempPoints.toArray(new Location[b.tempPoints.size()]);
      b.tempPoints = null;
      BitWidth width = b.getWidth();
      if (width != BitWidth.UNKNOWN) {
        int n = width.getWidth();
        b.threads = new WireThread[n];
        for (int i = 0; i < n; i++)
          b.threads[i] = new WireThread();
      }
    }

    // unite threads going through splitters
    for (final var spl : splitters) {
      synchronized (spl) {
        final var splAttrs = (SplitterAttributes) spl.getAttributeSet();
        final var bitEnd = splAttrs.bitEnd;
        final var splData = spl.wireData;
        final var fromBundle = splData.endBundle[0];
        if (fromBundle == null || !fromBundle.isValid()) continue;

        for (var i = 0; i < bitEnd.length; i++) {
          var j = bitEnd[i];
          if (j > 0) {
            var thr = spl.bitThread[i];
            final var toBundle = splData.endBundle[j];
            final var toThreads = toBundle.threads;
            if (toThreads != null && toBundle.isValid()) {
              final var fromThreads = fromBundle.threads;
              if (i >= fromThreads.length) {
                throw new ArrayIndexOutOfBoundsException("from " + i + " of " + fromThreads.length);
              }
              if (thr >= toThreads.length) {
                throw new ArrayIndexOutOfBoundsException("to " + thr + " of " + toThreads.length);
              }
              fromThreads[i].unite(toThreads[thr]);
            }
          }
        }
      }
    }

    // merge any threads united by previous step
    for (final var wireBundle : ret.getBundles()) {
      if (wireBundle.threads != null) {
        for (int i = 0; i < wireBundle.threads.length; i++) {
          final var thr = wireBundle.threads[i].getRepresentative();
          wireBundle.threads[i] = thr;
          thr.addBundlePosition(i, wireBundle);
        }
      }
    }

    // finish constructing the threads
    for (WireBundle b : ret.getBundles()) {
      if (b.threads != null) {
        for (WireThread t : b.threads)
          t.finishConstructing();
      }
    }

    // All bundles are made, all threads are now sewn together.

    // Record all component locations so they can be marked as dirty when this
    // wire bundle map is used to initialize a new State.
    ret.allLocations.addAll(points.getAllLocations());

    // Record all interesting component (non-wire, non-splitter) locations so
    // they can be used to filter out uninteresting points when this wire bundle
    // map is used to initialize a new State. We also need to know which
    // interesting components are at those locations.
    for (Location p : ret.allLocations) {
      ArrayList<Component> a = null;
      for (Component comp : points.getComponents(p)) {
        if ((comp instanceof Wire) || (comp instanceof Splitter))
          continue;
        if (a == null)
          a = new ArrayList<Component>();
        a.add(comp);
      }
      if (a != null)
        ret.componentsAtLocations.put(p, a);
    }

    // Compute the exception set before leaving.
    final var exceptions = points.getWidthIncompatibilityData();
    if (CollectionUtil.isNotEmpty(exceptions)) {
      for (final var wid : exceptions) {
        ret.addWidthIncompatibilityData(wid);
      }
    }
    for (final var wireBundle : ret.getBundles()) {
      final var e = wireBundle.getWidthIncompatibilityData();
      if (e != null) ret.addWidthIncompatibilityData(e);
    }
  }

  private void connectPullResistors(BundleMap ret) {
    for (final var comp : pulls) {
      final var loc = comp.getEnd(0).getLocation();
      var b = ret.getBundleAt(loc);
      if (b == null) {
        b = ret.createBundleAt(loc);
        b.tempPoints.add(loc);
        ret.setBundleAt(loc, b);
      }
      final var instance = Instance.getInstanceFor(comp);
      b.addPullValue(PullResistor.getPullValue(instance));
    }
  }

  private void connectTunnels(BundleMap ret) {
    // determine the sets of tunnels
    final var tunnelSets = new HashMap<String, ArrayList<Location>>();
    for (final var comp : tunnels) {
      final var label = comp.getAttributeSet().getValue(StdAttr.LABEL).trim();
      if (!label.equals("")) {
        final var tunnelSet = tunnelSets.computeIfAbsent(label, k -> new ArrayList<>(3));
        tunnelSet.add(comp.getLocation());
      }
    }

    // now connect the bundles that are tunnelled together
    for (ArrayList<Location> tunnelSet : tunnelSets.values()) {
      WireBundle foundBundle = null;
      Location foundLocation = null;
      for (final var loc : tunnelSet) {
        final var bundle = ret.getBundleAt(loc);
        if (bundle != null) {
          foundBundle = bundle;
          foundLocation = loc;
          break;
        }
      }
      if (foundBundle == null) {
        foundLocation = tunnelSet.get(0);
        foundBundle = ret.createBundleAt(foundLocation);
      }
      for (final var loc : tunnelSet) {
        if (loc != foundLocation) {
          final var bundle = ret.getBundleAt(loc);
          if (bundle == null) {
            foundBundle.tempPoints.add(loc);
            ret.setBundleAt(loc, foundBundle);
          } else {
            bundle.unite(foundBundle);
          }
        }
      }
    }
  }

  private void connectWires(BundleMap ret) {
    // make a WireBundle object for each tree of connected wires
    for (final var wire : wires) {
      final var bundleA = ret.getBundleAt(wire.e0);
      if (bundleA == null) {
        final var bundleB = ret.createBundleAt(wire.e1);
        bundleB.tempPoints.add(wire.e0);
        ret.setBundleAt(wire.e0, bundleB);
      } else {
        final var bundleB = ret.getBundleAt(wire.e1);
        if (bundleB == null) { // t1 doesn't exist
          bundleA.tempPoints.add(wire.e1);
          ret.setBundleAt(wire.e1, bundleA);
        } else {
          bundleB.unite(bundleA); // unite bundles
        }
      }
    }
  }

  static Value getBusValue(CircuitState state, Location loc) {
    State s = state.getWireData();
    if (s == null)
      return Value.NIL; // return state.getValue(loc); // fallback, probably wrong, who cares
    ValuedBus vb = s.busAt.get(loc);
    if (vb == null)
      return Value.NIL; // return state.getValue(loc); // fallback, probably wrong, who cares
    Value v = vb.val;
    if (v == null)
      return Value.NIL; // return state.getValue(loc); // fallback, probably wrong, who cares
    return v;
  }

  void draw(ComponentDrawContext context, Collection<Component> hidden) {
    final var showState = context.getShowState();
    final var state = context.getCircuitState();
    final var g = (Graphics2D) context.getGraphics();
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, Wire.WIDTH);
    final var highlighted = context.getHighlightedWires();

    final var bmap = getBundleMap();
    final var isValid = bmap.isValid();
    if (CollectionUtil.isNullOrEmpty(hidden)) {
      for (final var wire : wires) {
        final var s = wire.e0;
        final var t = wire.e1;
        final var wb = bmap.getBundleAt(s);
        var width = 5;
        if (!wb.isValid()) {
          g.setColor(Value.widthErrorColor);
        } else if (showState) {
          g.setColor(!isValid ? Value.nilColor : getBusValue(state, s).getColor());
        } else {
          g.setColor(Color.BLACK);
        }
        if (highlighted.containsWire(wire)) {
          width = wb.isBus() ? Wire.HIGHLIGHTED_WIDTH_BUS : Wire.HIGHLIGHTED_WIDTH;
          GraphicsUtil.switchToWidth(g, width);
          g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());

          final var oldStroke = g.getStroke();
          g.setStroke(Wire.HIGHLIGHTED_STROKE);
          g.setColor(Value.strokeColor);
          g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
          g.setStroke(oldStroke);
        } else {
          width = wb.isBus() ? Wire.WIDTH_BUS : Wire.WIDTH;
          GraphicsUtil.switchToWidth(g, width);
          g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
        }
        /* The following part is used by the FPGA-commanders DRC to highlight a wire with DRC
         * problems (KTT1)
         */
        if (wire.isDrcHighlighted()) {
          width += 2;
          g.setColor(wire.getDrcHighlightColor());
          GraphicsUtil.switchToWidth(g, 2);
          if (wire.isVertical()) {
            g.drawLine(s.getX() - width, s.getY(), t.getX() - width, t.getY());
            g.drawLine(s.getX() + width, s.getY(), t.getX() + width, t.getY());
          } else {
            g.drawLine(s.getX(), s.getY() - width, t.getX(), t.getY() - width);
            g.drawLine(s.getX(), s.getY() + width, t.getX(), t.getY() + width);
          }
        }
      }

      for (Location loc : points.getAllLocations()) {
        if (points.getComponentCount(loc) > 2) {
          final var wb = bmap.getBundleAt(loc);
          if (wb != null) {
            var color = Color.BLACK;
            if (!wb.isValid()) {
              color = Value.widthErrorColor;
            } else if (showState) {
              color = !isValid ? Value.nilColor : state.getValue(loc).getColor();
            }
            g.setColor(color);

            int radius =
                highlighted.containsLocation(loc)
                    ? wb.isBus()
                        ? (int) (Wire.HIGHLIGHTED_WIDTH_BUS * Wire.DOT_MULTIPLY_FACTOR)
                        : (int) (Wire.HIGHLIGHTED_WIDTH * Wire.DOT_MULTIPLY_FACTOR)
                    : wb.isBus()
                        ? (int) (Wire.WIDTH_BUS * Wire.DOT_MULTIPLY_FACTOR)
                        : (int) (Wire.WIDTH * Wire.DOT_MULTIPLY_FACTOR);
            radius = (int) (radius * Wire.DOT_MULTIPLY_FACTOR);
            g.fillOval(loc.getX() - radius, loc.getY() - radius, radius * 2, radius * 2);
          }
        }
      }
    } else {
      for (final var wire : wires) {
        if (!hidden.contains(wire)) {
          final var s = wire.e0;
          final var t = wire.e1;
          final var wb = bmap.getBundleAt(s);
          if (!wb.isValid()) {
            g.setColor(Value.widthErrorColor);
          } else if (showState) {
            g.setColor(!isValid ? Value.nilColor : getBusValue(state, s).getColor());
          } else {
            g.setColor(Color.BLACK);
          }
          if (highlighted.containsWire(wire)) {
            GraphicsUtil.switchToWidth(g, Wire.WIDTH + 2);
            g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
            GraphicsUtil.switchToWidth(g, Wire.WIDTH);
          } else {
            GraphicsUtil.switchToWidth(g, wb.isBus() ? Wire.WIDTH_BUS : Wire.WIDTH);
            g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
          }
        }
      }

      // this is just an approximation, but it's good enough since
      // the problem is minor, and hidden only exists for a short
      // while at a time anway.
      for (Location loc : points.getAllLocations()) {
        if (points.getComponentCount(loc) > 2) {
          var icount = 0;
          for (final var comp : points.getComponents(loc)) {
            if (!hidden.contains(comp)) ++icount;
          }
          if (icount > 2) {
            final var wireBundle = bmap.getBundleAt(loc);
            if (wireBundle != null) {
              if (!wireBundle.isValid()) {
                g.setColor(Value.widthErrorColor);
              } else if (showState) {
                g.setColor(!isValid ? Value.nilColor : getBusValue(state, loc).getColor());
              } else {
                g.setColor(Color.BLACK);
              }
              var radius = highlighted.containsLocation(loc)
                      ? (wireBundle.isBus() ? Wire.HIGHLIGHTED_WIDTH_BUS : Wire.HIGHLIGHTED_WIDTH)
                      : (wireBundle.isBus() ? Wire.WIDTH_BUS : Wire.WIDTH);
              radius = (int) (radius * Wire.DOT_MULTIPLY_FACTOR);
              g.fillOval(loc.getX() - radius, loc.getY() - radius, radius * 2, radius * 2);
            }
          }
        }
      }
    }
  }

  // There are only two threads that need to use the bundle map, I think:
  // the AWT event thread, and the simulation worker thread.
  // AWT does modifications to the components and wires, then voids the
  // masterBundleMap, and eventually recomputes a new map (if needed) during
  // painting. AWT sometimes locks a splitter, then changes components and
  // wires.
  // Computing a new bundle map requires both locking splitters and touching
  // the components and wires, so to avoid deadlock, only the AWT should create
  // the new bundle map. The bundle map is (essentially, if not entirely)
  // read-only once it is fully constructed.
  // The simulation thread never creates a new bundle map. On the other hand,
  // the simulation thread creates the State objects for each simulated instance
  // of the circuit, and each State duplicates data from the bundle map.

  private class BundleMapGetter implements Runnable {
    BundleMap result;
    public void run() {
      result = getBundleMap();
    }
  }

  /*synchronized*/ private BundleMap getBundleMap() {
    final var map = masterBundleMap; // volatile read by AWT or simulation thread
    if (map != null) return map;
    if (SwingUtilities.isEventDispatchThread()) {
      // AWT event thread.
      final var ret = new BundleMap();
      try {
        computeBundleMap(ret);
        masterBundleMap = ret; // volatile write by AWT thread
      } catch (Exception t) {
        ret.invalidate();
        logger.error(t.getLocalizedMessage());
      }
      return ret;
    } else {
      // Simulation thread.
      try {
        BundleMapGetter awtThread = new BundleMapGetter();
        SwingUtilities.invokeAndWait(awtThread);
        return awtThread.result;
      } catch (Exception t) {
        logger.error(t.getLocalizedMessage());
        final var ret = new BundleMap();
        ret.invalidate();
        return ret;
      }
    }
  }

  Iterator<? extends Component> getComponents() {
    return IteratorUtil.createJoinedIterator(splitters.iterator(), wires.iterator());
  }

  BitWidth getWidth(Location q) {
    final var det = points.getWidth(q);
    if (det != BitWidth.UNKNOWN) return det;

    final var bmap = getBundleMap();
    if (!bmap.isValid()) return BitWidth.UNKNOWN;
    final var qb = bmap.getBundleAt(q);
    if (qb != null && qb.isValid()) return qb.getWidth();

    return BitWidth.UNKNOWN;
  }

  Set<WidthIncompatibilityData> getWidthIncompatibilityData() {
    return getBundleMap().getWidthIncompatibilityData();
  }

  Bounds getWireBounds() {
    var bds = bounds;
    if (bds == Bounds.EMPTY_BOUNDS) {
      bds = recomputeBounds();
    }
    return bds;
  }

  WireBundle getWireBundle(Location query) {
    final var bundleMap = getBundleMap();
    return bundleMap.getBundleAt(query);
  }

  Set<Wire> getWires() {
    return wires;
  }

  WireSet getWireSet(Wire start) {
    final var wireBundle = getWireBundle(start.e0);
    if (wireBundle == null) return WireSet.EMPTY;
    final var wires = new HashSet<Wire>();
    for (final var loc : wireBundle.xpoints) {
      wires.addAll(points.getWires(loc));
    }
    return new WireSet(wires);
  }

  // boolean isMapVoided() {
  //   return masterBundleMap == null; // volatile read by simulation thread
  // }

  void propagate(CircuitState circState, ArrayList<Location> dirtyPoints, ArrayList<Value> newVals) {
    final var map = getBundleMap();
    ArrayList<WireThread> dirtyThreads = new ArrayList<>();

    // get state, or create a new one if current state is outdated
    var state = circState.getWireData();
    if (state == null || state.bundleMap != map) {
      // if it is outdated, we need to compute for all threads
      state = new State(circState, map);
      circState.setWireData(state);
      // note: all buses are already marked as dirty.
      // But we need to mark all points as dirty as well
      dirtyPoints.addAll(map.allLocations);
      for (Location p : map.allLocations)
        newVals.add(Propagator.getDrivenValueAt(circState, p));
    }

    int npoints = dirtyPoints.size();
    for (int k = 0; k < npoints; k++) { // for each point of interest
      Location p = dirtyPoints.get(k);
      Value val = newVals.get(k);
      ValuedBus vb = state.busAt.get(p);
      if (vb == null) {
        // point is not wired: just set that point's value and be done
        // todo: we could keep track of the affected components here
        circState.setValueByWire(p, val);
      } else if (vb.threads == null) {
        // point is wired to a threadless (e.g. invalid-width) bundle:
        // propagate NIL across entire bundle
        if (vb.dirty)
          state.markClean(vb);
        int n = vb.componentPoints.length;
        for (int i = 0; i < n; i++) {
          Location buspt = vb.componentPoints[i];
          Component[] affected = vb.componentsAffected[i];
          circState.setValueByWire(buspt, Value.NIL, affected);
        }
      } else {
        // common case... it is wired to a normal bus: update the stored value
        // of this point on the bus, mark the bus as dirty, and mark as dirty
        // any related buses.
        for (int i = 0; i < vb.componentPoints.length; i++) {
          if (vb.componentPoints[i].equals(p)) {
            Value old = vb.valAtPoint[i];
            vb.valAtPointSum = null;
            if ((val == null || val == Value.NIL) && (old == null || old == Value.NIL))
              break; // ignore, both old and new are NIL
            if (val != null && old != null && val.equals(old))
              break; // ignore, both old and new are same non-NIL value
            vb.valAtPoint[i] = val;
            if (!vb.dirty) {
              state.markDirty(vb);
              if (vb.dependentBuses != null) {
                for (ValuedBus dep : vb.dependentBuses)
                  if (!dep.dirty)
                    state.markDirty(dep);
              }
            }
            break;
          }
        }
      }
    }

    if (state.numDirty <= 0) return;

    // recompute valAtPointSum for each dirty bus
    for (int i = 0; i < state.numDirty; i++) {
      ValuedBus vb = state.buses[i];
      vb.valAtPointSum = Value.combineLikeWidths(vb.valAtPoint);
    }

    // recompute thread values for all threads passing through dirty buses,
    // recompute aggregate bus values for all dirty buses,
    // and post those notifications to all bus points
    for (int i = 0; i < state.numDirty; i++) {
      ValuedBus vb = state.buses[i];
      Value val = vb.val = vb.recalculate();
      vb.dirty = false;
      int n = vb.componentPoints.length;
      for (int j = 0; j < n; j++) {
        Location p = vb.componentPoints[j];
        Component[] affected = vb.componentsAffected[j];
        circState.setValueByWire(p, val, affected);
      }
    }
    state.numDirty = 0;
  }

  private Bounds recomputeBounds() {
    final var it = wires.iterator();
    if (!it.hasNext()) {
      bounds = Bounds.EMPTY_BOUNDS;
      return Bounds.EMPTY_BOUNDS;
    }

    var w = it.next();
    var xmin = w.e0.getX();
    var ymin = w.e0.getY();
    var xmax = w.e1.getX();
    var ymax = w.e1.getY();
    while (it.hasNext()) {
      w = it.next();
      final var x0 = w.e0.getX();
      if (x0 < xmin) xmin = x0;
      final var x1 = w.e1.getX();
      if (x1 > xmax) xmax = x1;
      final var y0 = w.e0.getY();
      if (y0 < ymin) ymin = y0;
      final var y1 = w.e1.getY();
      if (y1 > ymax) ymax = y1;
    }
    bounds = Bounds.create(xmin, ymin, xmax - xmin + 1, ymax - ymin + 1);
    return bounds;
  }

  /*synchronized*/ void remove(Component comp) {
    if (comp instanceof Wire wire) {
      removeWire(wire);
    } else if (comp instanceof Splitter) {
      splitters.remove(comp);
    } else {
      final var factory = comp.getFactory();
      if (factory instanceof Tunnel) {
        tunnels.remove(comp);
        comp.getAttributeSet().removeAttributeListener(tunnelListener);
      } else if (factory instanceof PullResistor) {
        pulls.remove(comp);
        comp.getAttributeSet().removeAttributeListener(tunnelListener);
      }
    }
    points.remove(comp);
    voidBundleMap();
  }

  /*synchronized*/ void remove(Component comp, EndData end) {
    points.remove(comp, end);
    voidBundleMap();
  }

  private void removeWire(Wire w) {
    if (!wires.remove(w)) return;

    if (bounds != Bounds.EMPTY_BOUNDS) {
      // bounds is valid - invalidate if endpoint on border
      final var smaller = bounds.expand(-2);
      if (!smaller.contains(w.e0) || !smaller.contains(w.e1)) {
        bounds = Bounds.EMPTY_BOUNDS;
      }
    }
  }

  /*synchronized*/ void replace(Component comp, EndData oldEnd, EndData newEnd) {
    points.remove(comp, oldEnd);
    points.add(comp, newEnd);
    voidBundleMap();
  }

  private void voidBundleMap() {
    // This should really only be called by AWT thread, but main() also
    // calls it during startup. It should not be called by the simulation
    // thread.
    masterBundleMap = null;
  }
}
