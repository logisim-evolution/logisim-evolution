/**
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
import com.cburch.logisim.data.Attribute;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CircuitWires {

  static class BundleMap {
    HashMap<Location, WireBundle> pointBundles = new HashMap<Location, WireBundle>();
    HashSet<WireBundle> bundles = new HashSet<WireBundle>();
    boolean isValid = true;
    // NOTE: It would make things more efficient if we also had
    // a set of just the first bundle in each tree.
    HashSet<WidthIncompatibilityData> incompatibilityData = null;

    void addWidthIncompatibilityData(WidthIncompatibilityData e) {
      if (incompatibilityData == null) {
        incompatibilityData = new HashSet<WidthIncompatibilityData>();
      }
      incompatibilityData.add(e);
    }

    WireBundle createBundleAt(Location p) {
      WireBundle ret = pointBundles.get(p);
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
    WireBundle[] end_bundle; // PointData associated with each end

    SplitterData(int fan_out) {
      end_bundle = new WireBundle[fan_out + 1];
    }
  }

  static class State {
    BundleMap bundleMap;
    HashMap<WireThread, Value> thr_values = new HashMap<WireThread, Value>();

    State(BundleMap bundleMap) {
      this.bundleMap = bundleMap;
    }

    @Override
    public Object clone() {
      State ret = new State(this.bundleMap);
      ret.thr_values.putAll(this.thr_values);
      return ret;
    }
  }

  static class ThreadBundle {
    int loc;
    WireBundle b;

    ThreadBundle(int loc, WireBundle b) {
      this.loc = loc;
      this.b = b;
    }
  }

  private class TunnelListener implements AttributeListener {
    public void attributeListChanged(AttributeEvent e) {}

    public void attributeValueChanged(AttributeEvent e) {
      Attribute<?> attr = e.getAttribute();
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
      Value[] ret = base.getAll();
      for (int i = 0; i < ret.length; i++) {
        if (ret[i] == Value.UNKNOWN) ret[i] = pullTo;
      }
      return Value.create(ret);
    }
  }

  static final Logger logger = LoggerFactory.getLogger(CircuitWires.class);

  // user-given data
  private HashSet<Wire> wires = new HashSet<Wire>();
  private HashSet<Splitter> splitters = new HashSet<Splitter>();
  private HashSet<Component> tunnels = new HashSet<Component>(); // of
  // Components
  // with
  // Tunnel
  // factory
  private TunnelListener tunnelListener = new TunnelListener();
  private HashSet<Component> pulls = new HashSet<Component>(); // of
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
    boolean added = true;
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
    boolean added = wires.add(w);
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
    for (Iterator<WireBundle> it = ret.getBundles().iterator(); it.hasNext(); ) {
      WireBundle b = it.next();
      WireBundle bpar = b.find();
      if (bpar != b) { // b isn't group's representative
        for (Location pt : b.points) {
          ret.setBundleAt(pt, bpar);
          bpar.points.add(pt);
        }
        bpar.addPullValue(b.getPullValue());
        it.remove();
      }
    }

    // make a WireBundle object for each end of a splitter
    for (Splitter spl : splitters) {
      List<EndData> ends = new ArrayList<EndData>(spl.getEnds());
      for (EndData end : ends) {
        Location p = end.getLocation();
        WireBundle pb = ret.createBundleAt(p);
        pb.setWidth(end.getWidth(), p);
      }
    }

    // set the width for each bundle whose size is known
    // based on components
    for (Location p : ret.getBundlePoints()) {
      WireBundle pb = ret.getBundleAt(p);
      BitWidth width = points.getWidth(p);
      if (width != BitWidth.UNKNOWN) {
        pb.setWidth(width, p);
      }
    }

    // determine the bundles at the end of each splitter
    for (Splitter spl : splitters) {
      List<EndData> ends = new ArrayList<EndData>(spl.getEnds());
      int index = -1;
      for (EndData end : ends) {
        index++;
        Location p = end.getLocation();
        WireBundle pb = ret.getBundleAt(p);
        if (pb != null) {
          pb.setWidth(end.getWidth(), p);
          spl.wire_data.end_bundle[index] = pb;
        }
      }
    }

    // unite threads going through splitters
    for (Splitter spl : splitters) {
      synchronized (spl) {
        SplitterAttributes spl_attrs = (SplitterAttributes) spl.getAttributeSet();
        byte[] bit_end = spl_attrs.bit_end;
        SplitterData spl_data = spl.wire_data;
        WireBundle from_bundle = spl_data.end_bundle[0];
        if (from_bundle == null || !from_bundle.isValid()) continue;

        for (int i = 0; i < bit_end.length; i++) {
          int j = bit_end[i];
          if (j > 0) {
            int thr = spl.bit_thread[i];
            WireBundle to_bundle = spl_data.end_bundle[j];
            WireThread[] to_threads = to_bundle.threads;
            if (to_threads != null && to_bundle.isValid()) {
              WireThread[] from_threads = from_bundle.threads;
              if (i >= from_threads.length) {
                throw new ArrayIndexOutOfBoundsException(
                    "from " + i + " of " + from_threads.length);
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
    for (WireBundle b : ret.getBundles()) {
      if (b.isValid() && b.threads != null) {
        for (int i = 0; i < b.threads.length; i++) {
          WireThread thr = b.threads[i].find();
          b.threads[i] = thr;
          thr.getBundles().add(new ThreadBundle(i, b));
        }
      }
    }

    // All threads are sewn together! Compute the exception set before
    // leaving
    Collection<WidthIncompatibilityData> exceptions = points.getWidthIncompatibilityData();
    if (exceptions != null && exceptions.size() > 0) {
      for (WidthIncompatibilityData wid : exceptions) {
        ret.addWidthIncompatibilityData(wid);
      }
    }
    for (WireBundle b : ret.getBundles()) {
      WidthIncompatibilityData e = b.getWidthIncompatibilityData();
      if (e != null) ret.addWidthIncompatibilityData(e);
    }
  }

  private void connectPullResistors(BundleMap ret) {
    for (Component comp : pulls) {
      Location loc = comp.getEnd(0).getLocation();
      WireBundle b = ret.getBundleAt(loc);
      if (b == null) {
        b = ret.createBundleAt(loc);
        b.points.add(loc);
        ret.setBundleAt(loc, b);
      }
      Instance instance = Instance.getInstanceFor(comp);
      b.addPullValue(PullResistor.getPullValue(instance));
    }
  }

  private void connectTunnels(BundleMap ret) {
    // determine the sets of tunnels
    HashMap<String, ArrayList<Location>> tunnelSets = new HashMap<String, ArrayList<Location>>();
    for (Component comp : tunnels) {
      String label = comp.getAttributeSet().getValue(StdAttr.LABEL);
      label = label.trim();
      if (!label.equals("")) {
        ArrayList<Location> tunnelSet = tunnelSets.get(label);
        if (tunnelSet == null) {
          tunnelSet = new ArrayList<Location>(3);
          tunnelSets.put(label, tunnelSet);
        }
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
      for (Location loc : tunnelSet) {
        if (loc != foundLocation) {
          WireBundle b = ret.getBundleAt(loc);
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
    for (Wire w : wires) {
      WireBundle b0 = ret.getBundleAt(w.e0);
      if (b0 == null) {
        WireBundle b1 = ret.createBundleAt(w.e1);
        b1.points.add(w.e0);
        ret.setBundleAt(w.e0, b1);
      } else {
        WireBundle b1 = ret.getBundleAt(w.e1);
        if (b1 == null) { // t1 doesn't exist
          b0.points.add(w.e1);
          ret.setBundleAt(w.e1, b0);
        } else {
          b1.unite(b0); // unite b0 and b1
        }
      }
    }
  }

  void draw(ComponentDrawContext context, Collection<Component> hidden) {
    boolean showState = context.getShowState();
    CircuitState state = context.getCircuitState();
    Graphics2D g = (Graphics2D) context.getGraphics();
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, Wire.WIDTH);
    WireSet highlighted = context.getHighlightedWires();

    BundleMap bmap = getBundleMap();
    boolean isValid = bmap.isValid();
    if (hidden == null || hidden.size() == 0) {
      for (Wire w : wires) {
        Location s = w.e0;
        Location t = w.e1;
        WireBundle wb = bmap.getBundleAt(s);
        int width = 5;
        if (!wb.isValid()) {
          g.setColor(Value.WIDTH_ERROR_COLOR);
        } else if (showState) {
          if (!isValid) g.setColor(Value.NIL_COLOR);
          else g.setColor(state.getValue(s).getColor());
        } else {
          g.setColor(Color.BLACK);
        }
        if (highlighted.containsWire(w)) {
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
        if (w.IsDRCHighlighted()) {
          width += 2;
          g.setColor(w.GetDRCHighlightColor());
          GraphicsUtil.switchToWidth(g, 2);
          if (w.isVertical()) {
            g.drawLine(s.getX() - width, s.getY(), t.getX() - width, t.getY());
            g.drawLine(s.getX() + width, s.getY(), t.getX() + width, t.getY());
          } else {
            g.drawLine(s.getX(), s.getY() - width, t.getX(), t.getY() - width);
            g.drawLine(s.getX(), s.getY() + width, t.getX(), t.getY() + width);
          }
        }
      }

      for (Location loc : points.getSplitLocations()) {
        if (points.getComponentCount(loc) > 2) {
          WireBundle wb = bmap.getBundleAt(loc);
          if (wb != null) {
            if (!wb.isValid()) {
              g.setColor(Value.WIDTH_ERROR_COLOR);
            } else if (showState) {
              if (!isValid) g.setColor(Value.NIL_COLOR);
              else g.setColor(state.getValue(loc).getColor());
            } else {
              g.setColor(Color.BLACK);
            }
            int radius;
            if (highlighted.containsLocation(loc)) {
              radius =
                  wb.isBus()
                      ? (int) (Wire.HIGHLIGHTED_WIDTH_BUS * Wire.DOT_MULTIPLY_FACTOR)
                      : (int) (Wire.HIGHLIGHTED_WIDTH * Wire.DOT_MULTIPLY_FACTOR);
            } else {
              radius =
                  wb.isBus()
                      ? (int) (Wire.WIDTH_BUS * Wire.DOT_MULTIPLY_FACTOR)
                      : (int) (Wire.WIDTH * Wire.DOT_MULTIPLY_FACTOR);
            }
            radius = (int) (radius * Wire.DOT_MULTIPLY_FACTOR);
            g.fillOval(loc.getX() - radius, loc.getY() - radius, radius * 2, radius * 2);
          }
        }
      }
    } else {
      for (Wire w : wires) {
        if (!hidden.contains(w)) {
          Location s = w.e0;
          Location t = w.e1;
          WireBundle wb = bmap.getBundleAt(s);
          if (!wb.isValid()) {
            g.setColor(Value.WIDTH_ERROR_COLOR);
          } else if (showState) {
            if (!isValid) g.setColor(Value.NIL_COLOR);
            else g.setColor(state.getValue(s).getColor());
          } else {
            g.setColor(Color.BLACK);
          }
          if (highlighted.containsWire(w)) {
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
      for (Location loc : points.getSplitLocations()) {
        if (points.getComponentCount(loc) > 2) {
          int icount = 0;
          for (Component comp : points.getComponents(loc)) {
            if (!hidden.contains(comp)) ++icount;
          }
          if (icount > 2) {
            WireBundle wb = bmap.getBundleAt(loc);
            if (wb != null) {
              if (!wb.isValid()) {
                g.setColor(Value.WIDTH_ERROR_COLOR);
              } else if (showState) {
                if (!isValid) g.setColor(Value.NIL_COLOR);
                else g.setColor(state.getValue(loc).getColor());
              } else {
                g.setColor(Color.BLACK);
              }
              int radius;
              if (highlighted.containsLocation(loc)) {
                radius = wb.isBus() ? Wire.HIGHLIGHTED_WIDTH_BUS : Wire.HIGHLIGHTED_WIDTH;
              } else {
                radius = wb.isBus() ? Wire.WIDTH_BUS : Wire.WIDTH;
              }
              radius = (int) (radius * Wire.DOT_MULTIPLY_FACTOR);
              g.fillOval(loc.getX() - radius, loc.getY() - radius, radius * 2, radius * 2);
            }
          }
        }
      }
    }
  }

  // void ensureComputed() {
  //	getBundleMap();
  // }

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
      BundleMap ret = new BundleMap();
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
        final BundleMap ret[] = new BundleMap[1];
        SwingUtilities.invokeAndWait(
            new Runnable() {
              public void run() {
                ret[0] = getBundleMap();
              }
            });
        return ret[0];
      } catch (Exception e) {
        BundleMap ret = new BundleMap();
        ret.invalidate();
        return ret;
      }
    }
  }

  Iterator<? extends Component> getComponents() {
    return IteratorUtil.createJoinedIterator(splitters.iterator(), wires.iterator());
  }

  private Value getThreadValue(CircuitState state, WireThread t) {
    Value ret = Value.UNKNOWN;
    Value pull = Value.UNKNOWN;
    for (ThreadBundle tb : t.getBundles()) {
      for (Location p : tb.b.points) {
        Value val = state.getComponentOutputAt(p);
        if (val != null && val != Value.NIL) {
          ret = ret.combine(val.get(tb.loc));
        }
      }
      Value pullHere = tb.b.getPullValue();
      if (pullHere != Value.UNKNOWN) pull = pull.combine(pullHere);
    }
    if (pull != Value.UNKNOWN) {
      ret = pullValue(ret, pull);
    }
    return ret;
  }

  BitWidth getWidth(Location q) {
    BitWidth det = points.getWidth(q);
    if (det != BitWidth.UNKNOWN) return det;

    BundleMap bmap = getBundleMap();
    if (!bmap.isValid()) return BitWidth.UNKNOWN;
    WireBundle qb = bmap.getBundleAt(q);
    if (qb != null && qb.isValid()) return qb.getWidth();

    return BitWidth.UNKNOWN;
  }

  Location getWidthDeterminant(Location q) {
    BitWidth det = points.getWidth(q);
    if (det != BitWidth.UNKNOWN) return q;

    WireBundle qb = getBundleMap().getBundleAt(q);
    if (qb != null && qb.isValid()) return qb.getWidthDeterminant();

    return q;
  }

  Set<WidthIncompatibilityData> getWidthIncompatibilityData() {
    return getBundleMap().getWidthIncompatibilityData();
  }

  Bounds getWireBounds() {
    Bounds bds = bounds;
    if (bds == Bounds.EMPTY_BOUNDS) {
      bds = recomputeBounds();
    }
    return bds;
  }

  WireBundle getWireBundle(Location query) {
    BundleMap bmap = getBundleMap();
    return bmap.getBundleAt(query);
  }

  Set<Wire> getWires() {
    return wires;
  }

  WireSet getWireSet(Wire start) {
    WireBundle bundle = getWireBundle(start.e0);
    if (bundle == null) return WireSet.EMPTY;
    HashSet<Wire> wires = new HashSet<Wire>();
    for (Location loc : bundle.points) {
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
    BundleMap map = getBundleMap();
    CopyOnWriteArraySet<WireThread> dirtyThreads =
        new CopyOnWriteArraySet<WireThread>(); // affected
    // threads

    // get state, or create a new one if current state is outdated
    State s = circState.getWireData();
    if (s == null || s.bundleMap != map) {
      // if it is outdated, we need to compute for all threads
      s = new State(map);
      for (WireBundle b : map.getBundles()) {
        WireThread[] th = b.threads;
        if (b.isValid() && th != null) {
          for (WireThread t : th) {
            dirtyThreads.add(t);
          }
        }
      }
      circState.setWireData(s);
    }

    // determine affected threads, and set values for unwired points
    for (Location p : points) {
      WireBundle pb = map.getBundleAt(p);
      if (pb == null) { // point is not wired
        circState.setValueByWire(p, circState.getComponentOutputAt(p));
      } else {
        WireThread[] th = pb.threads;
        if (!pb.isValid() || th == null) {
          // immediately propagate NILs across invalid bundles
          CopyOnWriteArraySet<Location> pbPoints = pb.points;
          if (pbPoints == null) {
            circState.setValueByWire(p, Value.NIL);
          } else {
            for (Location loc2 : pbPoints) {
              circState.setValueByWire(loc2, Value.NIL);
            }
          }
        } else {
          for (WireThread t : th) {
            dirtyThreads.add(t);
          }
        }
      }
    }

    if (dirtyThreads.isEmpty()) return;

    // determine values of affected threads
    HashSet<ThreadBundle> bundles = new HashSet<ThreadBundle>();
    for (WireThread t : dirtyThreads) {
      Value v = getThreadValue(circState, t);
      s.thr_values.put(t, v);
      bundles.addAll(t.getBundles());
    }

    // now propagate values through circuit
    for (ThreadBundle tb : bundles) {
      WireBundle b = tb.b;

      Value bv = null;
      if (!b.isValid() || b.threads == null) {; // do nothing
      } else if (b.threads.length == 1) {
        bv = s.thr_values.get(b.threads[0]);
      } else {
        Value[] tvs = new Value[b.threads.length];
        boolean tvs_valid = true;
        for (int i = 0; i < tvs.length; i++) {
          Value tv = s.thr_values.get(b.threads[i]);
          if (tv == null) {
            tvs_valid = false;
            break;
          }
          tvs[i] = tv;
        }
        if (tvs_valid) bv = Value.create(tvs);
      }

      if (bv != null) {
        for (Location p : b.points) {
          circState.setValueByWire(p, bv);
        }
      }
    }
  }

  private Bounds recomputeBounds() {
    Iterator<Wire> it = wires.iterator();
    if (!it.hasNext()) {
      bounds = Bounds.EMPTY_BOUNDS;
      return Bounds.EMPTY_BOUNDS;
    }

    Wire w = it.next();
    int xmin = w.e0.getX();
    int ymin = w.e0.getY();
    int xmax = w.e1.getX();
    int ymax = w.e1.getY();
    while (it.hasNext()) {
      w = it.next();
      int x0 = w.e0.getX();
      if (x0 < xmin) xmin = x0;
      int x1 = w.e1.getX();
      if (x1 > xmax) xmax = x1;
      int y0 = w.e0.getY();
      if (y0 < ymin) ymin = y0;
      int y1 = w.e1.getY();
      if (y1 > ymax) ymax = y1;
    }
    Bounds bds = Bounds.create(xmin, ymin, xmax - xmin + 1, ymax - ymin + 1);
    bounds = bds;
    return bds;
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
    boolean removed = wires.remove(w);
    if (!removed) return;

    if (bounds != Bounds.EMPTY_BOUNDS) {
      // bounds is valid - invalidate if endpoint on border
      Bounds smaller = bounds.expand(-2);
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
