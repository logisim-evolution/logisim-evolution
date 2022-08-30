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
import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class WireRepair extends CircuitTransaction {

  private static class MergeSets {
    private final HashMap<Wire, ArrayList<Wire>> map = new HashMap<>();

    Collection<ArrayList<Wire>> getMergeSets() {
      IdentityHashMap<ArrayList<Wire>, Boolean> lists;
      lists = new IdentityHashMap<>();
      for (final var list : map.values()) {
        lists.put(list, Boolean.TRUE);
      }
      return lists.keySet();
    }

    void merge(Wire a, Wire b) {
      var set0 = map.get(a);
      var set1 = map.get(b);
      if (set0 == null && set1 == null) {
        set0 = new ArrayList<>(2);
        set0.add(a);
        set0.add(b);
        map.put(a, set0);
        map.put(b, set0);
      } else if (set0 == null && set1 != null) {
        set1.add(a);
        map.put(a, set1);
      } else if (set0 != null && set1 == null) {
        set0.add(b);
        map.put(b, set0);
      } else if (set0 != set1) { // neither is null, and they are
        // different
        if (set0.size() > set1.size()) { // ensure set1 is the larger
          final var temp = set0;
          set0 = set1;
          set1 = temp;
        }
        set1.addAll(set0);
        for (Wire w : set0) {
          map.put(w, set1);
        }
      }
    }
  }

  private final Circuit circuit;

  public WireRepair(Circuit circuit) {
    this.circuit = circuit;
  }

  private void doMerges(CircuitMutator mutator) {
    final var sets = new MergeSets();
    for (final var loc : circuit.wires.points.getSplitLocations()) {
      Collection<?> at = circuit.getComponents(loc);
      if (at.size() == 2) {
        Iterator<?> atit = at.iterator();
        Object at0 = atit.next();
        Object at1 = atit.next();
        if (at0 instanceof Wire w0 && at1 instanceof Wire w1) {
          if (w0.isParallel(w1)) {
            sets.merge(w0, w1);
          }
        }
      }
    }

    final var repl = new ReplacementMap();
    for (final var mergeSet : sets.getMergeSets()) {
      if (mergeSet.size() > 1) {
        final var locs = new ArrayList<Location>(2 * mergeSet.size());
        for (final var w : mergeSet) {
          locs.add(w.getEnd0());
          locs.add(w.getEnd1());
        }
        Collections.sort(locs);
        final var e0 = locs.get(0);
        final var e1 = locs.get(locs.size() - 1);
        final var wnew = Wire.create(e0, e1);
        Collection<Wire> wset = Collections.singleton(wnew);

        for (final var w : mergeSet) {
          if (!w.equals(wnew)) {
            repl.put(w, wset);
          }
        }
      }
    }
    mutator.replace(circuit, repl);
  }

  private void doMergeSet(ArrayList<Wire> mergeSet, ReplacementMap replacements, Set<Location> splitLocs) {
    final var ends = new TreeSet<Location>();
    for (final var w : mergeSet) {
      ends.add(w.getEnd0());
      ends.add(w.getEnd1());
    }
    final var whole = Wire.create(ends.first(), ends.last());

    final var mids = new TreeSet<Location>();
    mids.add(whole.getEnd0());
    mids.add(whole.getEnd1());
    for (final var loc : whole) {
      if (splitLocs.contains(loc)) {
        for (final var comp : circuit.getComponents(loc)) {
          if (!mergeSet.contains(comp)) {
            mids.add(loc);
            break;
          }
        }
      }
    }

    ArrayList<Wire> mergeResult = new ArrayList<>();
    if (mids.size() == 2) {
      mergeResult.add(whole);
    } else {
      Location e0 = null;
      for (final var e1 : mids) {
        if (e0 != null)
          mergeResult.add(Wire.create(e0, e1));
        e0 = e1;
      }
    }

    for (Wire w : mergeSet) {
      final var wRepl = new ArrayList<Component>(2);
      for (final var w2 : mergeResult) {
        if (w2.overlaps(w, false)) {
          wRepl.add(w2);
        }
      }
      replacements.put(w, wRepl);
    }
  }

  /*
   * for debugging: private void printWires(String prefix, PrintStream out) {
   * boolean first = true; for (Wire w : circuit.getWires()) { if (first) {
   * out.println(prefix + ": " + w); first = false; } else {
   * out.println("      " + w); } } out.println(prefix + ": none"); }
   */

  private void doOverlaps(CircuitMutator mutator) {
    final var wirePoints = new HashMap<Location, ArrayList<Wire>>();
    for (final var w : circuit.getWires()) {
      for (final var loc : w) {
        final var locWires = wirePoints.computeIfAbsent(loc, k -> new ArrayList<>(3));
        locWires.add(w);
      }
    }

    final var mergeSets = new MergeSets();
    for (final var locWires : wirePoints.values()) {
      if (locWires.size() > 1) {
        for (int i = 0, n = locWires.size(); i < n; i++) {
          final var w0 = locWires.get(i);
          for (var j = i + 1; j < n; j++) {
            final var w1 = locWires.get(j);
            if (w0.overlaps(w1, false))
              mergeSets.merge(w0, w1);
          }
        }
      }
    }

    final var replacements = new ReplacementMap();
    final var splitLocs = circuit.wires.points.getSplitLocations();
    for (final var mergeSet : mergeSets.getMergeSets()) {
      if (mergeSet.size() > 1) {
        doMergeSet(mergeSet, replacements, splitLocs);
      }
    }
    mutator.replace(circuit, replacements);
  }

  private void doSplits(CircuitMutator mutator) {
    final var splitLocs = circuit.wires.points.getSplitLocations();
    final var repl = new ReplacementMap();
    for (final var w : circuit.getWires()) {
      final var w0 = w.getEnd0();
      final var w1 = w.getEnd1();
      ArrayList<Location> splits = null;
      for (final var loc : splitLocs) {
        if (w.contains(loc) && !loc.equals(w0) && !loc.equals(w1)) {
          if (splits == null) splits = new ArrayList<>();
          splits.add(loc);
        }
      }
      if (splits != null) {
        splits.add(w1);
        Collections.sort(splits);
        var e0 = w0;
        final var subs = new ArrayList<Wire>(splits.size());
        for (final var e1 : splits) {
          subs.add(Wire.create(e0, e1));
          e0 = e1;
        }
        repl.put(w, subs);
      }
    }
    mutator.replace(circuit, repl);
  }

  @Override
  protected Map<Circuit, Integer> getAccessedCircuits() {
    return Collections.singletonMap(circuit, READ_WRITE);
  }

  @Override
  protected void run(CircuitMutator mutator) {
    doMerges(mutator);
    doOverlaps(mutator);
    doSplits(mutator);
  }
}
