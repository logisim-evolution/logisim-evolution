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
import com.cburch.logisim.std.wiring.Pin;
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

// CircuitWires stores and calculates the values being propagated along all
// wires and buses in a circuit, essentially anything related to the netlist
// connectivity of the circuit.
public class CircuitWires {

  // Connectivity holds info about how the Circuit's buses, wires, tunnels, and
  // splitters are connected to each other and to components. This gets
  // re-computed from scratch each time the circuit changes. It does *not* hold
  // any Values, which are dynamically computed by the simulator. It holds only
  // the static connectivity defined by the circuit. Within this data structure
  // are:
  // - WireBundle: a bus/wire as drawn by the user. Think: like an unbroken,
  //   physical ribbon cable that acts as a bundle of one or more threads. It
  //   has a width 1 <= n <= 32 (or incompatibilityData if the width is not
  //   consistent across the length of the bus), and touches a set of Location
  //   points (all the corners, intersections, and component port locations
  //   along the bus). It also has a pullValue, e.g. if there is a pull-down
  //   resistor connected to the bus. A WireBundle can traverse tunnels, but not
  //   splitters.
  // - WireThread: a 1-bit element of a WireBundle. Think: an
  //   electrically-contiguous trace within a circuit. Each wire WireThread
  //   traverses one or more WireBundles, and has a specific position within
  //   each WireBundle that it traverses. WireThreads traverse through
  //   splitters.
  private static class Connectivity {

    // All wire bundles. Initially, a bundle is created and added to this for
    // every bus wire segment, splitter endpoint, pull resistor endpoint, etc.
    // Eventually, as bundles get unified together across intersecting points,
    // tunnels, etc., this set gets trimmed down to just a single representative
    // WireBundle for each bus.
    HashSet<WireBundle> bundles = new HashSet<>();

    // Given a location, returns wire bundle at that location (if any)
    HashMap<Location, WireBundle> pointBundles = new HashMap<>();

    // All locations touched by a wire bundle
    ArrayList<Location> allLocations = new ArrayList<>();

    // All components except wires, splitters, and pull resistors
    ArrayList<Component> allComponents = new ArrayList<>();

    // Given a location, returns a list of Components that have a port at that location.
    HashMap<Location, ArrayList<Component>> componentsAtLocations = new HashMap<>();

    // The isValid flag remains true unless something goes wrong during initialization.
    volatile boolean isValid = true;

    // Info about width incompatibilities, used by GUI to display error.
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

    void setBundleAt(Location p, WireBundle b) {
      pointBundles.put(p, b);
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
  }

  static class SplitterData {
    final WireBundle[] endBundle; // PointData associated with each end

    SplitterData(int fanOut) {
      endBundle = new WireBundle[fanOut + 1];
    }
  }

  // ValuedThread is similar to WireThread, but also holds the
  // dynamically-computed 1-bit simulation Value carried on the thread as well.
  static class ValuedThread {
    int steps; // length of the thread (# of buses it traverses)
    ValuedBus[] bus; // buses traversed by this thread
    int[] position; // position of this thread within each of those buses
    boolean pullUp, pullDown, pullError; // whether this thread is being pulled up, or down, or error, or neither
    Value threadVal; // cached, resolved value carried by this thread (or error for conflicts, etc.)
    // threadVal is set to null when thread is dirty and should be recalculated

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
        pullError |= (pullHere == Value.ERROR);
      }
      if (pullUp && pullDown) {
        pullUp = pullDown = false;
        pullError = true;
      }
    }

    Value threadValue() {
      if (threadVal != null)
        return threadVal;
      threadVal = Value.UNKNOWN;
      for (int i = 0; i < steps; i++) {
        ValuedBus vb = bus[i];
        int pos = position[i];
        Value v = vb.localDrivenValue;
        if (v != Value.NIL)
          threadVal = threadVal.combine(v.get(pos));
      }
      if (threadVal == Value.UNKNOWN) {
        if (pullUp) {
          threadVal = Value.TRUE;
        } else if (pullDown) {
          threadVal = Value.FALSE;
        } else if (pullError) {
          threadVal = Value.ERROR;
        }
      }
      return threadVal;
    }
  }

  // BusConnection represents a point at which a Component connects to a
  // ValuedBus.
  // FIXME: it might be best to hold a reference to some kind of
  // CircuitComponentInfo data structure instead here, where we can store a flag
  // about whether this component has been marked dirty yet or not.
  public static class BusConnection {
    public final Component component;
    public final Location location;
    public final boolean isSink, isBidirectional;
    public Value drivenValue; // value this component is driving onto the bus (null for sinks)
    // todo: maybe also keep point number, or EndData, etc.?

    BusConnection(Component comp, Location loc) {
      component = comp;
      location = loc;
      EndData e = comp.getEnd(loc);
      // if (e == null)
      //   System.out.printf("missing end for %s at %s\n", comp, loc);
      // Special case: Pin is treated as a sink, because it needs notifications
      // of any changes to inputs in order to set the UI color propertly.
      isSink = (e.getType() == EndData.INPUT_ONLY)
          || (comp.getFactory() instanceof Pin);
      isBidirectional = (e.getType() == EndData.INPUT_OUTPUT);
      drivenValue = null;
    }

    @Override
    public String toString() {
      return String.format("component %s at %s is %sdirectional %s val %s",
          component, location, isBidirectional ? "bi" : "uni",
          isSink ? "sink" : "source", drivenValue);
    }
  }

  // ValuedBus is similar to WireBundle, but also holds the dynamically-computed
  // n-bit simulation Value (formed from joining all n 1-bit values from the
  // threads passing through this bus.
  // Degenerate case: If this bus isn't connected to any other buses (e.g. via
  // splitters), then all bits can be calculated together in one pass, rather
  // than calculating each thread separately then combining the results. This
  // case is detected by checking if there are dependent buses.
  static class ValuedBus {
    int idx; // State.buses[idx] will hold this ValuedBus
    int width; // negative for invalid width
    ValuedThread[] threads; // threads passing through this bus (or null if dependentBuses is empty, or if invalid width)

    BusConnection[] connections; // sink and source components connected to this bus
    Location[] locations; // set of all locations for those connections

    Value localDrivenValue; // sum of connections[i].drivenValue
    Value busVal; // cached, resolved value carried by this bus (or error for conflicts, etc.)
    boolean dirty; // whether localDrivenValue and busVal and valid
    ValuedBus[] dependentBuses; // other buses affected if this one's localDrivenValue changes
    Value pullVal; // only used if dependentBuses is empty

    // Location[] componentPoints; // subset of wire bundle xpoints that have components at them
    // Component[][] componentsAffected; // components at each of those points
    // Value[] valAtPoint;
    // Value valAtPointSum; // cached sum of valAtPoint, or null if dirty

    ValuedBus(int i, WireBundle wb, Connectivity cmap) {
      idx = i;
      filterComponents(cmap, wb.xpoints); // initializes locations[] and connections[]
      width = wb.threads == null ? -1 : wb.getWidth().getWidth();
      pullVal = wb.getPullValue();
      dirty = true;
    }

    void filterComponents(Connectivity cmap, Location[] xpoints) {
      ArrayList<Location> locs = new ArrayList<>();
      ArrayList<BusConnection> conns = new ArrayList<>();
      for (Location p : xpoints) {
        ArrayList<Component> a = cmap.componentsAtLocations.get(p);
        if (a == null)
          continue;
        locs.add(p);
        for (Component c : a)
          conns.add(new BusConnection(c, p));
      }
      int n = locs.size();
      locations = n == xpoints.length ? xpoints : locs.toArray(new Location[n]);
      connections = conns.toArray(new BusConnection[conns.size()]);
    }

    void makeThreads(WireThread[] wbthreads, HashMap<WireBundle, ValuedBus> allBuses,
                     HashMap<WireThread, ValuedThread> allThreads) {
      if (width <= 0)
        return;
      boolean degenerate = true;
      for (WireThread t : wbthreads) {
        if (t.steps > 1) {
          degenerate = false;
          break;
        }
      }
      if (degenerate)
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
      if (width <= 0) {
        busVal = Value.NIL;
        dirty = false;
        return busVal;
      } else if (dependentBuses.length == 0) {
        // degenerate case: threads are irrelevant
        busVal = localDrivenValue;
        if (pullVal != null)
          busVal = busVal.pullEachBitTowards(pullVal);
        dirty = false;
        return busVal;
      } else if (width == 1) {
        busVal = threads[0].threadValue();
        dirty = false;
        return busVal;
      }
      long error = 0, unknown = 0, value = 0;
      for (int i = 0; i < width; i++) {
        long mask = 1L << i;
        Value tv = threads[i].threadValue();
        if (tv == Value.TRUE)
          value |= mask;
        else if (tv == Value.FALSE)
          ;
        else if (tv == Value.UNKNOWN)
          unknown |= mask;
        else
          error |= mask;
      }
      busVal = Value.create_unsafe(width, error, unknown, value);
      dirty = false;
      return busVal;
    }
  }

  State newState(CircuitState circState) { // for cloning CircuitState
    return new State(getConnectivity(), circState.getWireData());
  }

  static class State {
    private Connectivity connectivity; // original source of connectivity info
    HashMap<Location, ValuedBus> busAt = new HashMap<>();
    ValuedBus[] buses;
    int numDirty;

    State(Connectivity cm, State prev) {
      connectivity = cm;
      HashMap<WireBundle, ValuedBus> allBuses = new HashMap<>();
      HashMap<ValuedBus, WireBundle> srcBuses = new HashMap<>();
      // initialize buses[] and busAt<>
      buses = new ValuedBus[connectivity.bundles.size()];
      int idx = 0;
      for (WireBundle wb : connectivity.bundles) {
        ValuedBus vb = new ValuedBus(idx++, wb, connectivity);
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
      // create threads for all buses that need them
      HashMap<WireThread, ValuedThread> allThreads = new HashMap<>();
      for (ValuedBus vb : buses)
        vb.makeThreads(srcBuses.get(vb).threads, allBuses, allThreads);
      // initialize BusConnection driven values from previous State, if any
      if (prev != null) {
        for (ValuedBus vb : buses)
          for (BusConnection bc : vb.connections)
            bc.drivenValue = prev.getDrivenValue(bc.component, bc.location);
      }
      // compute bus dependencies
      for (ValuedBus vb : buses) {
        if (vb.width <= 0)
          continue;
        if (vb.threads == null) {
          // degenerate
          vb.dependentBuses = EMPTY_DEPENDENCIES;
        } else {
          HashSet<ValuedBus> deps = new HashSet<>();
          for (ValuedThread t : vb.threads)
            for (ValuedBus dep : t.bus)
              if (dep != vb)
                deps.add(dep);
          int n = deps.size();
          vb.dependentBuses = deps.toArray(new ValuedBus[n]);
        }
      }
      // mark all dirty: recomputes values and triggers component propagation
      numDirty = buses.length;
    }

    static final ValuedBus[] EMPTY_DEPENDENCIES = new ValuedBus[0];

    Value getDrivenValue(Component c, Location loc) {
      ValuedBus vb = busAt.get(loc);
      if (vb == null)
        return null;
      for (BusConnection bc : vb.connections) {
        if (bc.component.equals(c) && bc.location.equals(loc))
          return bc.drivenValue;
      }
      return null;
    }

    void markClean(ValuedBus vb) {
      if (!vb.dirty) {
        throw new IllegalStateException("can't clean element that is not dirty");
      }
      if (vb.idx > numDirty - 1) {
        throw new IllegalStateException("bad position for dirty element");
      }
      if (vb.idx < numDirty - 1) { // swap toward end of dirty section of array
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
        return;
      }
      if (vb.idx < numDirty) {
        throw new IllegalStateException("bad position for clean element");
      }
      vb.localDrivenValue = null; // need to recompute based on connections[i].drivenValue
      vb.busVal = null; // need to recompute based on threads[i].threadValue
      if (vb.idx > numDirty) { // swap toward dirty section of array
        ValuedBus other = buses[numDirty];
        other.idx = vb.idx;
        buses[other.idx] = other;
        vb.idx = numDirty;
        buses[vb.idx] = vb;
      }
      if (vb.threads != null) { // invalidate threads
        for (ValuedThread vt : vb.threads)
          vt.threadVal = null;
      }
      vb.dirty = true;
      numDirty++;
    }
  }

  // Elements of the circuit, organized by type.
  private HashSet<Wire> wires = new HashSet<>(); // Components of type Wire
  private HashSet<Splitter> splitters = new HashSet<>(); // Components of type Splitter
  private HashSet<Component> tunnels = new HashSet<>(); // Components having Tunnel factory
  private HashSet<Component> pulls = new HashSet<>(); // Components having PullResistor factory
  private HashSet<Component> components = new HashSet<>(); // other Components

  private TunnelListener tunnelListener = new TunnelListener();

  private class TunnelListener implements AttributeListener {
    @Override
    public void attributeListChanged(AttributeEvent e) {
      // do nothing.
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      final var attr = e.getAttribute();
      if (attr == StdAttr.LABEL || attr == PullResistor.ATTR_PULL_TYPE)
        voidConnectivity();
    }
  }

  static final Logger logger = LoggerFactory.getLogger(CircuitWires.class);

  final CircuitPoints points = new CircuitPoints();
  private Bounds bounds = Bounds.EMPTY_BOUNDS;

  private volatile Connectivity masterConnectivity = null;

  CircuitWires() {}

  // NOTE: this could be made much more efficient in most cases to
  // avoid voiding the connectivity map.
  /*synchronized*/ boolean add(Component comp) {
    // System.out.println("wires adding " + comp);
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
      } else {
        components.add(comp);
      }
    }
    if (added) {
      points.add(comp);
      voidConnectivity();
    }
    return added;
  }

  /*synchronized*/ void add(Component comp, EndData end) {
    points.add(comp, end);
    voidConnectivity();
  }

  private boolean addWire(Wire w) {
    final var added = wires.add(w);
    if (!added) return false;

    if (bounds != Bounds.EMPTY_BOUNDS) { // update bounds
      bounds = bounds.add(w.e0).add(w.e1);
    }
    return true;
  }

  // To be called by getConnectivity() only
  private void computeConnectivity(Connectivity ret) {
    // System.out.println("computing new connectivity map");
    // create bundles corresponding to wires and tunnels
    connectComponents(ret);
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

    // Record all interesting components so they can be marked as dirty when
    // this wire connectivity map is used to initialize a new State.
    ret.allComponents.addAll(components);

    // Record all component locations so they can be marked as dirty when this
    // wire connectivity map is used to initialize a new State.
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

  private void connectPullResistors(Connectivity ret) {
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

  private void connectTunnels(Connectivity ret) {
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

  private void connectComponents(Connectivity ret) {
    // make a WireBundle object for each output or bidirectional port
    // of a component
    for (Component comp : components) {
      for (EndData e : comp.getEnds()) {
        if (e.getType() == EndData.INPUT_ONLY)
          continue;
        Location loc = e.getLocation();
        WireBundle b = ret.getBundleAt(loc);
        if (b == null) {
          b = ret.createBundleAt(loc);
          b.tempPoints.add(loc);
          ret.setBundleAt(loc, b);
        }
      }
    }
  }

  private void connectWires(Connectivity ret) {
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
    Value v = vb.busVal;
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

    Connectivity cmap = getConnectivity();
    boolean isValid = cmap.isValid();
    if (CollectionUtil.isNullOrEmpty(hidden)) {
      for (final var wire : wires) {
        final var s = wire.e0;
        final var t = wire.e1;
        final var wb = cmap.getBundleAt(s);
        var width = 5;
        if (!wb.isValid()) {
          g.setColor(Value.widthErrorColor);
        } else if (!showState) {
          g.setColor(Color.BLACK);
        } else if (!isValid) {
          g.setColor(Value.nilColor);
        } else {
          g.setColor(getBusValue(state, s).getColor());
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
          final var wb = cmap.getBundleAt(loc);
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
          final var wb = cmap.getBundleAt(s);
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
            final var wireBundle = cmap.getBundleAt(loc);
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

  // There are only two threads that need to use the connectivity map, I think:
  // the AWT event thread, and the simulation worker thread.
  // AWT does modifications to the components and wires, then voids the
  // masterConnectivity, and eventually recomputes a new map (if needed) during
  // painting. AWT sometimes locks a splitter, then changes components and
  // wires.
  // Computing a new connectivity map requires both locking splitters and touching
  // the components and wires, so to avoid deadlock, only the AWT should create
  // the new connectivity map. The connectivity map is (essentially, if not entirely)
  // read-only once it is fully constructed.
  // The simulation thread never creates a new connectivity map. On the other hand,
  // the simulation thread creates the State objects for each simulated instance
  // of the circuit, and each State duplicates data from the connectivity map.

  private class ConnectivityGetter implements Runnable {
    Connectivity result;
    public void run() {
      result = getConnectivity();
    }
  }

  /*synchronized*/ private Connectivity getConnectivity() {
    final var map = masterConnectivity; // volatile read by AWT or simulation thread
    if (map != null) return map;
    if (SwingUtilities.isEventDispatchThread()) {
      // AWT event thread.
      final var ret = new Connectivity();
      try {
        computeConnectivity(ret);
        masterConnectivity = ret; // volatile write by AWT thread
      } catch (Exception t) {
        ret.invalidate();
        logger.error(t.getLocalizedMessage());
      }
      return ret;
    } else {
      // Simulation thread.
      try {
        ConnectivityGetter awtThread = new ConnectivityGetter();
        SwingUtilities.invokeAndWait(awtThread);
        return awtThread.result;
      } catch (Exception t) {
        logger.error(t.getLocalizedMessage());
        final var ret = new Connectivity();
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

    Connectivity cmap = getConnectivity();
    if (!cmap.isValid()) return BitWidth.UNKNOWN;
    final var qb = cmap.getBundleAt(q);
    if (qb != null && qb.isValid()) return qb.getWidth();

    return BitWidth.UNKNOWN;
  }

  Set<WidthIncompatibilityData> getWidthIncompatibilityData() {
    return getConnectivity().getWidthIncompatibilityData();
  }

  Bounds getWireBounds() {
    var bds = bounds;
    if (bds == Bounds.EMPTY_BOUNDS) {
      bds = recomputeBounds();
    }
    return bds;
  }

  WireBundle getWireBundle(Location query) {
    Connectivity cmap = getConnectivity();
    return cmap.getBundleAt(query);
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

  void propagate(CircuitState circState, ArrayList<Propagator.SimulatorEvent> dirtyPoints) {
    Connectivity map = getConnectivity();
    ArrayList<WireThread> dirtyThreads = new ArrayList<>();

    // get state, or create a new one if current state is outdated
    var s = circState.getWireData();
    if (s == null || s.connectivity != map) {
      // if it is outdated, we need to compute for all threads
      s = new State(map, s);
      circState.setWireData(s);
      // Note: all buses are already marked as dirty.
      // But some component ports that were previously connected to buses
      // might no longer be connected to those same buses (or might not
      // be connected to any bus), and vice versa. So we should mark all
      // components as dirty.
      circState.clearValuesByWire();
      circState.markComponentsDirty(map.allComponents);
      // circState.markDirtyPoints(map.allLocations);
    }

    // make note of updates from simulator
    int npoints = dirtyPoints.size();
    for (int k = 0; k < npoints; k++) { // for each point of interest
      Propagator.SimulatorEvent ev = dirtyPoints.get(k);
      Location p = ev.loc;
      Component cause = ev.cause;
      Value val = ev.val;

      ValuedBus vb = s.busAt.get(p);
      if (vb == null) {
        // System.out.printf("simulator event, but no bus: comp=%s loc=%s val=%s ", cause, p, val);
        // point is not wired: just set that point's value and be done
        // todo: we could keep track of the affected components here
        // System.out.printf("  loc %s not wired, accept val %s\n", p, val);
        // circState.setValueByWire(val, p);
      } else if (vb.width <= 0) {
        // point is wired to a bus with invalid width: ignore new value
        // propagate NIL across entire bundle
        // for (Location buspt : vb.componentPoints)
        //   circState.setValueByWire(buspt, Value.NIL);
        // int n = vb.componentPoints.length;
        // for (int i = 0; i < n; i++) {
        //   Location buspt = vb.componentPoints[i];
        //   Component[] affected = vb.componentsAffected[i];
        //   circState.setValueByWire(buspt, Value.NIL, affected);
        // }
      } else {
        // common case... it is wired to a normal bus: update the stored value
        // of this point on the bus, mark the bus as dirty, and (if not
        // degenerate) mark as dirty any related buses.
        // System.out.printf("  loc %s is wired, processing val %s\n", p, val);
        // fixme: sort the connections list, sources first, then bidir, then sinks
        for (BusConnection bc : vb.connections) {
          if (bc.location.equals(p) && bc.component.equals(cause)) {
            Value old = bc.drivenValue;
            if (Value.equal(old, val))
              continue;
            bc.drivenValue = val;
            s.markDirty(vb);
            for (ValuedBus dep : vb.dependentBuses)
              s.markDirty(dep);
            break;
          }
        }
      }
    }

    if (s.numDirty <= 0)
      return;

    // recompute localDrivenValue for each dirty bus
    for (int i = 0; i < s.numDirty; i++) {
      ValuedBus vb = s.buses[i];
      if (vb.width <= 0) {
        // this bundle has inconsistent widths, or no width, hence no localDrivenValue
        vb.localDrivenValue = Value.NIL;
      } else {
        vb.localDrivenValue = Value.combineLikeWidths(vb.width, vb.connections);
      }
    }

    // recompute threadVal for all threads passing through dirty buses (if not degenerate),
    // recompute aggregate busVal for all dirty buses,
    // and post those results to the circuit state
    for (int i = 0; i < s.numDirty; i++) {
      ValuedBus vb = s.buses[i];
      Value old = vb.busVal;
      Value val = vb.recalculate();
      if (Value.equal(old, val))
        continue;
      circState.setValueByWire(val, vb.locations, vb.connections);
      // int n = vb.componentPoints.length;
      // for (int j = 0; j < n; j++) {
      //   Location p = vb.componentPoints[j];
      //   Component[] affected = vb.componentsAffected[j];
      //   // System.out.printf("  loc %s ready, set val %s affects %d components\n", p, val, (affected == null ? 0 : affected.length));
      //   circState.setValueByWire(p, val, affected);
      // }
    }
    s.numDirty = 0;
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
      } else {
        components.remove(comp);
      }
    }
    points.remove(comp);
    voidConnectivity();
  }

  /*synchronized*/ void remove(Component comp, EndData end) {
    points.remove(comp, end);
    voidConnectivity();
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
    // System.out.printf("replaced %s %s with %s\n", comp, oldEnd, newEnd);
    voidConnectivity();
  }

  private void voidConnectivity() {
    // System.out.println("voiding connectivity info");
    // This should really only be called by AWT thread, but main() also
    // calls it during startup. It should not be called by the simulation
    // thread.
    masterConnectivity = null; // volatile write by AWT thread (and sometimes main/startup)
  }
}
