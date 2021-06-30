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

package com.cburch.logisim.file;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileStatistics {
  public static class Count {
    private Library library;
    private final ComponentFactory factory;
    private int simpleCount;
    private int uniqueCount;
    private int recursiveCount;

    private Count(ComponentFactory factory) {
      this.library = null;
      this.factory = factory;
      this.simpleCount = 0;
      this.uniqueCount = 0;
      this.recursiveCount = 0;
    }

    public ComponentFactory getFactory() {
      return factory;
    }

    public Library getLibrary() {
      return library;
    }

    public int getRecursiveCount() {
      return recursiveCount;
    }

    public int getSimpleCount() {
      return simpleCount;
    }

    public int getUniqueCount() {
      return uniqueCount;
    }
  }

  public static FileStatistics compute(LogisimFile file, Circuit circuit) {
    final var include = new HashSet<Circuit>(file.getCircuits());
    final var countMap = new HashMap<Circuit, Map<ComponentFactory, Count>>();
    doRecursiveCount(circuit, include, countMap);
    doUniqueCounts(countMap.get(circuit), countMap);
    final var countList = sortCounts(countMap.get(circuit), file);
    return new FileStatistics(countList, getTotal(countList, include), getTotal(countList, null));
  }

  private static Map<ComponentFactory, Count> doRecursiveCount(
      Circuit circuit, Set<Circuit> include, Map<Circuit, Map<ComponentFactory, Count>> countMap) {
    if (countMap.containsKey(circuit)) {
      return countMap.get(circuit);
    }

    final var counts = doSimpleCount(circuit);
    countMap.put(circuit, counts);
    for (final var count : counts.values()) {
      count.uniqueCount = count.simpleCount;
      count.recursiveCount = count.simpleCount;
    }
    for (final var sub : include) {
      final var subFactory = sub.getSubcircuitFactory();
      if (counts.containsKey(subFactory)) {
        final var multiplier = counts.get(subFactory).simpleCount;
        final var subCountRecursive = doRecursiveCount(sub, include, countMap);
        for (final var subCount : subCountRecursive.values()) {
          final var subfactory = subCount.factory;
          var superCount = counts.get(subfactory);
          if (superCount == null) {
            superCount = new Count(subfactory);
            counts.put(subfactory, superCount);
          }
          superCount.recursiveCount += multiplier * subCount.recursiveCount;
        }
      }
    }

    return counts;
  }

  private static Map<ComponentFactory, Count> doSimpleCount(Circuit circuit) {
    final var counts = new HashMap<ComponentFactory, Count>();
    for (final var comp : circuit.getNonWires()) {
      final var factory = comp.getFactory();
      var count = counts.get(factory);
      if (count == null) {
        count = new Count(factory);
        counts.put(factory, count);
      }
      count.simpleCount++;
    }
    return counts;
  }

  private static void doUniqueCounts(
      Map<ComponentFactory, Count> counts,
      Map<Circuit, Map<ComponentFactory, Count>> circuitCounts) {
    for (final var count : counts.values()) {
      final var factory = count.getFactory();
      var unique = 0;
      for (final var circ : circuitCounts.keySet()) {
        final var subcount = circuitCounts.get(circ).get(factory);
        if (subcount != null) {
          unique += subcount.simpleCount;
        }
      }
      count.uniqueCount = unique;
    }
  }

  private static Count getTotal(List<Count> counts, Set<Circuit> exclude) {
    final var ret = new Count(null);
    for (final var count : counts) {
      final var factory = count.getFactory();
      Circuit factoryCirc = null;
      if (factory instanceof SubcircuitFactory) {
        factoryCirc = ((SubcircuitFactory) factory).getSubcircuit();
      }
      if (exclude == null || !exclude.contains(factoryCirc)) {
        ret.simpleCount += count.simpleCount;
        ret.uniqueCount += count.uniqueCount;
        ret.recursiveCount += count.recursiveCount;
      }
    }
    return ret;
  }

  private static List<Count> sortCounts(Map<ComponentFactory, Count> counts, LogisimFile file) {
    final var ret = new ArrayList<Count>();
    for (final var tool : file.getTools()) {
      final var factory = tool.getFactory();
      final var count = counts.get(factory);
      if (count != null) {
        count.library = file;
        ret.add(count);
      }
    }
    for (final var lib : file.getLibraries()) {
      for (final var tool : lib.getTools()) {
        if (tool instanceof AddTool) {
          final var factory = ((AddTool) tool).getFactory();
          final var count = counts.get(factory);
          if (count != null) {
            count.library = lib;
            ret.add(count);
          }
        }
      }
    }
    return ret;
  }

  private final List<Count> counts;
  private final Count totalWithout;
  private final Count totalWith;

  private FileStatistics(List<Count> counts, Count totalWithout, Count totalWith) {
    this.counts = Collections.unmodifiableList(counts);
    this.totalWithout = totalWithout;
    this.totalWith = totalWith;
  }

  public List<Count> getCounts() {
    return counts;
  }

  public Count getTotalWithoutSubcircuits() {
    return totalWithout;
  }

  public Count getTotalWithSubcircuits() {
    return totalWith;
  }
}
