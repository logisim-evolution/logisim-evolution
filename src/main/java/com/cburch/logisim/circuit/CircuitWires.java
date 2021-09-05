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
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.IteratorUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CircuitWires {

  static class BundleMap {
    final HashMap<Location, WireBundle> pointBundles = new HashMap<>();
    final HashSet<WireBundle> bundles = new HashSet<>();
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
        ret = new WireBundle();
        pointBundles.put(p, ret);
        ret.points.add(p);
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
    final WireBundle[] end_bundle; // PointData associated with each end

    SplitterData(int fan_out) {
      end_bundle = new WireBundle[fan_out + 1];
    }
  }

  static class State {
    final BundleMap bundleMap;
    final HashMap<WireThread, Value> thr_values = new HashMap<>();

    State(BundleMap bundleMap) {
      this.bundleMap = bundleMap;
    }

    @Override
    public Object clone() {
      final var ret = new State(this.bundleMap);
      ret.thr_values.putAll(this.thr_values);
      return ret;
    }
  }

  static class ThreadBundle {
    final int loc;
    final WireBundle b;

    ThreadBundle(int loc, WireBundle b) {
      this.loc = loc;
      this.b = b;
    }
  }

  private class TunnelListener implements AttributeListener {
    @Override
    public void attributeListChanged(AttributeEvent e) {}

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
  // derived data
  private Bounds bounds = Bounds.EMPTY_BOUNDS;

  private BundleMap masterBundleMap = null;

  CircuitWires() {}

  //
  // action methods
  //
  // NOTE: this could be made much more efficient in most cases to
  // avoid voiding the bundle map.
  /*synchronized*/ boolean add(Component comp) {
    var added = true;
    if (comp instanceof Wire) {
      added = addWire((Wire) comp);
    } else if (comp instanceof Splitter) {
      splitters.add((Splitter) comp);
    } else {
      Object factory = comp.getFactory();
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
        for (final var pt : b.points) {
          ret.setBundleAt(pt, bpar);
          bpar.points.add(pt);
        }
        bpar.addPullValue(b.getPullValue());
        it.remove();
      }
    }

    // make a WireBundle object for each end of a splitter
    for (final var spl : splitters) {
      final var ends = new ArrayList<EndData>(spl.getEnds());
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
      final var ends = new ArrayList<EndData>(spl.getEnds());
      int index = -1;
      for (final var end : ends) {
        index++;
        final var p = end.getLocation();
        final var pb = ret.getBundleAt(p);
        if (pb != null) {
          pb.setWidth(end.getWidth(), p);
          spl.wire_data.end_bundle[index] = pb;
        }
      }
    }

    // unite threads going through splitters
    for (final var spl : splitters) {
      synchronized (spl) {
        final var spl_attrs = (SplitterAttributes) spl.getAttributeSet();
        final var bit_end = spl_attrs.bit_end;
        final var spl_data = spl.wire_data;
        final var from_bundle = spl_data.end_bundle[0];
        if (from_bundle == null || !from_bundle.isValid()) continue;

        for (var i = 0; i < bit_end.length; i++) {
          var j = bit_end[i];
          if (j > 0) {
            var thr = spl.bit_thread[i];
            final var to_bundle = spl_data.end_bundle[j];
            final var to_threads = to_bundle.threads;
            if (to_threads != null && to_bundle.isValid()) {
              final var from_threads = from_bundle.threads;
              if (i >= from_threads.length) {
                throw new ArrayIndexOutOfBoundsException("from " + i + " of " + from_threads.length);
              }
              if (thr >= to_threads.length) {
                throw new ArrayIndexOutOfBoundsException("to " + thr + " of " + to_threads.length);
              }
              from_threads[i].unite(to_threads[thr]);
            }
          }
        }
      }
    }

    // merge any threads united by previous step
    for (final var wireBundle : ret.getBundles()) {
      if (wireBundle.isValid() && wireBundle.threads != null) {
        for (int i = 0; i < wireBundle.threads.length; i++) {
          final var thr = wireBundle.threads[i].find();
          wireBundle.threads[i] = thr;
          thr.getBundles().add(new ThreadBundle(i, wireBundle));
        }
      }
    }

    // All threads are sewn together! Compute the exception set before
    // leaving
    final var exceptions = points.getWidthIncompatibilityData();
    if (exceptions != null && exceptions.size() > 0) {
      for (WidthIncompatibilityData wid : exceptions) {
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
        b.points.add(loc);
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
        final var tunnelSet = tunnelSets.computeIfAbsent(label, k -> new ArrayList<Location>(3));
        tunnelSet.add(comp.getLocation());
      }
    }

    // now connect the bundles that are tunnelled together
    for (ArrayList<Location> tunnelSet : tunnelSets.values()) {
      WireBundle foundBundle = null;
      Location foundLocation = null;
      for (Location loc : tunnelSet) {
        WireBundle b = ret.getBundleAt(loc);
        if (b != null) {
          foundBundle = b;
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
          final var b = ret.getBundleAt(loc);
          if (b == null) {
            foundBundle.points.add(loc);
            ret.setBundleAt(loc, foundBundle);
          } else {
            b.unite(foundBundle);
          }
        }
      }
    }
  }

  private void connectWires(BundleMap ret) {
    // make a WireBundle object for each tree of connected wires
    for (final var wire : wires) {
      final var b0 = ret.getBundleAt(wire.e0);
      if (b0 == null) {
        final var b1 = ret.createBundleAt(wire.e1);
        b1.points.add(wire.e0);
        ret.setBundleAt(wire.e0, b1);
      } else {
        final var b1 = ret.getBundleAt(wire.e1);
        if (b1 == null) { // t1 doesn't exist
          b0.points.add(wire.e1);
          ret.setBundleAt(wire.e1, b0);
        } else {
          b1.unite(b0); // unite b0 and b1
        }
      }
    }
  }

  void draw(ComponentDrawContext context, Collection<Component> hidden) {
    boolean showState = context.getShowState();
    final var state = context.getCircuitState();
    final var g = (Graphics2D) context.getGraphics();
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, Wire.WIDTH);
    final var highlighted = context.getHighlightedWires();

    final var bmap = getBundleMap();
    final var isValid = bmap.isValid();
    if (hidden == null || hidden.size() == 0) {
      for (final var wire : wires) {
        final var s = wire.e0;
        final var t = wire.e1;
        final var wb = bmap.getBundleAt(s);
        var width = 5;
        if (!wb.isValid()) {
          g.setColor(Value.WIDTH_ERROR_COLOR);
        } else if (showState) {
          if (!isValid) g.setColor(Value.NIL_COLOR);
          else g.setColor(state.getValue(s).getColor());
        } else {
          g.setColor(Color.BLACK);
        }
        if (highlighted.containsWire(wire)) {
          if (wb.isBus()) width = Wire.HIGHLIGHTED_WIDTH_BUS;
          else width = Wire.HIGHLIGHTED_WIDTH;
          GraphicsUtil.switchToWidth(g, width);
          g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());

          Stroke oldStroke = g.getStroke();
          g.setStroke(Wire.HIGHLIGHTED_STROKE);
          g.setColor(Value.STROKE_COLOR);
          g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
          g.setStroke(oldStroke);
        } else {
          if (wb.isBus()) width = Wire.WIDTH_BUS;
          else width = Wire.WIDTH;
          GraphicsUtil.switchToWidth(g, width);
          g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
        }
        /* The following part is used by the FPGA-commanders DRC to highlight a wire with DRC
         * problems (KTT1)
         */
        if (wire.IsDRCHighlighted()) {
          width += 2;
          g.setColor(wire.GetDRCHighlightColor());
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

      for (final var loc : points.getSplitLocations()) {
        if (points.getComponentCount(loc) > 2) {
          final var wb = bmap.getBundleAt(loc);
          if (wb != null) {
            var color = Color.BLACK;
            if (!wb.isValid()) {
              color = Value.WIDTH_ERROR_COLOR;
            } else if (showState) {
              color = !isValid ? Value.NIL_COLOR : state.getValue(loc).getColor();
            }
            g.setColor(color);

            int radius;
            if (highlighted.containsLocation(loc)) {
              radius = wb.isBus()
                      ? (int) (Wire.HIGHLIGHTED_WIDTH_BUS * Wire.DOT_MULTIPLY_FACTOR)
                      : (int) (Wire.HIGHLIGHTED_WIDTH * Wire.DOT_MULTIPLY_FACTOR);
            } else {
              radius = wb.isBus()
                      ? (int) (Wire.WIDTH_BUS * Wire.DOT_MULTIPLY_FACTOR)
                      : (int) (Wire.WIDTH * Wire.DOT_MULTIPLY_FACTOR);
            }
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
            g.setColor(Value.WIDTH_ERROR_COLOR);
          } else if (showState) {
            if (!isValid) g.setColor(Value.NIL_COLOR);
            else g.setColor(state.getValue(s).getColor());
          } else {
            g.setColor(Color.BLACK);
          }
          if (highlighted.containsWire(wire)) {
            GraphicsUtil.switchToWidth(g, Wire.WIDTH + 2);
            g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
            GraphicsUtil.switchToWidth(g, Wire.WIDTH);
          } else {
            if (wb.isBus()) GraphicsUtil.switchToWidth(g, Wire.WIDTH_BUS);
            else GraphicsUtil.switchToWidth(g, Wire.WIDTH);
            g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
          }
        }
      }

      // this is just an approximation, but it's good enough since
      // the problem is minor, and hidden only exists for a short
      // while at a time anway.
      for (final var loc : points.getSplitLocations()) {
        if (points.getComponentCount(loc) > 2) {
          int icount = 0;
          for (final var comp : points.getComponents(loc)) {
            if (!hidden.contains(comp)) ++icount;
          }
          if (icount > 2) {
            final var wb = bmap.getBundleAt(loc);
            if (wb != null) {
              if (!wb.isValid()) {
                g.setColor(Value.WIDTH_ERROR_COLOR);
              } else if (showState) {
                if (!isValid) g.setColor(Value.NIL_COLOR);
                else g.setColor(state.getValue(loc).getColor());
              } else {
                g.setColor(Color.BLACK);
              }
              var radius = (highlighted.containsLocation(loc))
                      ? (wb.isBus() ? Wire.HIGHLIGHTED_WIDTH_BUS : Wire.HIGHLIGHTED_WIDTH)
                      : (wb.isBus() ? Wire.WIDTH_BUS : Wire.WIDTH);
              radius = (int) (radius * Wire.DOT_MULTIPLY_FACTOR);
              g.fillOval(loc.getX() - radius, loc.getY() - radius, radius * 2, radius * 2);
            }
          }
        }
      }
    }
  }

  //  void ensureComputed() {
  //    getBundleMap();
  //  }

  // There are only two threads that need to use the bundle map, I think:
  // the AWT event thread, and the simulation worker thread.
  // AWT does modifications to the components and wires, then voids the
  // masterBundleMap, and eventually recomputes a new map (if needed) during
  // painting. AWT sometimes locks a splitter, then changes components and
  // wires.
  // Computing a new bundle map requires both locking splitters and touching
  // the components and wires, so to avoid deadlock, only the AWT should
  // create the new bundle map.

  /*synchronized*/ private BundleMap getBundleMap() {
    if (SwingUtilities.isEventDispatchThread()) {
      // AWT event thread.
      if (masterBundleMap != null) return masterBundleMap;
      final var ret = new BundleMap();
      try {
        computeBundleMap(ret);
        masterBundleMap = ret;
      } catch (Exception t) {
        ret.invalidate();
        logger.error("{}", t.getLocalizedMessage());
      }
      return ret;
    } else {
      // Simulation thread.
      try {
        final var ret = new BundleMap[1];
        SwingUtilities.invokeAndWait(() -> ret[0] = getBundleMap());
        return ret[0];
      } catch (Exception e) {
        final var ret = new BundleMap();
        ret.invalidate();
        return ret;
      }
    }
  }

  Iterator<? extends Component> getComponents() {
    return IteratorUtil.createJoinedIterator(splitters.iterator(), wires.iterator());
  }

  private Value getThreadValue(CircuitState state, WireThread t) {
    var ret = Value.UNKNOWN;
    var pull = Value.UNKNOWN;
    for (final var tb : t.getBundles()) {
      for (final var p : tb.b.points) {
        final var val = state.getComponentOutputAt(p);
        if (val != null && val != Value.NIL) {
          ret = ret.combine(val.get(tb.loc));
        }
      }
      final var pullHere = tb.b.getPullValue();
      if (pullHere != Value.UNKNOWN) pull = pull.combine(pullHere);
    }
    if (pull != Value.UNKNOWN) {
      ret = pullValue(ret, pull);
    }
    return ret;
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

  Location getWidthDeterminant(Location q) {
    final var det = points.getWidth(q);
    if (det != BitWidth.UNKNOWN) return q;

    final var qb = getBundleMap().getBundleAt(q);
    if (qb != null && qb.isValid()) return qb.getWidthDeterminant();

    return q;
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
    final var bmap = getBundleMap();
    return bmap.getBundleAt(query);
  }

  Set<Wire> getWires() {
    return wires;
  }

  WireSet getWireSet(Wire start) {
    final var bundle = getWireBundle(start.e0);
    if (bundle == null) return WireSet.EMPTY;
    final var wires = new HashSet<Wire>();
    for (final var loc : bundle.points) {
      wires.addAll(points.getWires(loc));
    }
    return new WireSet(wires);
  }

  //
  // query methods
  //
  boolean isMapVoided() {
    return masterBundleMap == null;
  }

  //
  // utility methods
  //
  void propagate(CircuitState circState, Set<Location> points) {
    final var map = getBundleMap();
    final var dirtyThreads = new CopyOnWriteArraySet<WireThread>(); // affected threads

    // get state, or create a new one if current state is outdated
    var s = circState.getWireData();
    if (s == null || s.bundleMap != map) {
      // if it is outdated, we need to compute for all threads
      s = new State(map);
      for (final var b : map.getBundles()) {
        final var th = b.threads;
        if (b.isValid() && th != null) {
          dirtyThreads.addAll(Arrays.asList(th));
        }
      }
      circState.setWireData(s);
    }

    // determine affected threads, and set values for unwired points
    for (final var p : points) {
      final var pb = map.getBundleAt(p);
      if (pb == null) { // point is not wired
        circState.setValueByWire(p, circState.getComponentOutputAt(p));
      } else {
        final var th = pb.threads;
        if (!pb.isValid() || th == null) {
          // immediately propagate NILs across invalid bundles
          final var pbPoints = pb.points;
          if (pbPoints == null) {
            circState.setValueByWire(p, Value.NIL);
          } else {
            for (final var loc2 : pbPoints) {
              circState.setValueByWire(loc2, Value.NIL);
            }
          }
        } else {
          dirtyThreads.addAll(Arrays.asList(th));
        }
      }
    }

    if (dirtyThreads.isEmpty()) return;

    // determine values of affected threads
    final var bundles = new HashSet<ThreadBundle>();
    for (final var t : dirtyThreads) {
      final var v = getThreadValue(circState, t);
      s.thr_values.put(t, v);
      bundles.addAll(t.getBundles());
    }

    // now propagate values through circuit
    for (final var tb : bundles) {
      final var b = tb.b;

      Value bv = null;
      if (!b.isValid() || b.threads == null) {
        // do nothing
      } else if (b.threads.length == 1) {
        bv = s.thr_values.get(b.threads[0]);
      } else {
        final var tvs = new Value[b.threads.length];
        var tvs_valid = true;
        for (int i = 0; i < tvs.length; i++) {
          final var tv = s.thr_values.get(b.threads[i]);
          if (tv == null) {
            tvs_valid = false;
            break;
          }
          tvs[i] = tv;
        }
        if (tvs_valid) bv = Value.create(tvs);
      }

      if (bv != null) {
        for (final var p : b.points) {
          circState.setValueByWire(p, bv);
        }
      }
    }
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
    if (comp instanceof Wire) {
      removeWire((Wire) comp);
    } else if (comp instanceof Splitter) {
      splitters.remove(comp);
    } else {
      Object factory = comp.getFactory();
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

  //
  // helper methods
  //
  private void voidBundleMap() {
    // This should really only be called by AWT thread, but main() also
    // calls it during startup. It should not be called by the simulation
    // thread.
    masterBundleMap = null;
  }
}
