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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplacementMap {

  static final Logger logger = LoggerFactory.getLogger(ReplacementMap.class);

  private boolean frozen;
  private final HashMap<Component, HashSet<Component>> map;
  private final HashMap<Component, HashSet<Component>> inverse;

  public ReplacementMap() {
    this(new HashMap<>(), new HashMap<>());
  }

  public ReplacementMap(Component oldComp, Component newComp) {
    this(new HashMap<>(), new HashMap<>());
    final var oldSet = new HashSet<Component>(3);
    oldSet.add(oldComp);
    final var newSet = new HashSet<Component>(3);
    newSet.add(newComp);
    map.put(oldComp, newSet);
    inverse.put(newComp, oldSet);
  }

  private ReplacementMap(HashMap<Component, HashSet<Component>> map, HashMap<Component, HashSet<Component>> inverse) {
    this.map = map;
    this.inverse = inverse;
  }

  public void add(Component comp) {
    if (frozen) {
      throw new IllegalStateException("cannot change map after frozen");
    }
    inverse.put(comp, new HashSet<>(3));
  }

  void append(ReplacementMap next) {
    for (Map.Entry<Component, HashSet<Component>> e : next.map.entrySet()) {
      final var b = e.getKey();
      final var cs = e.getValue(); // what b is replaced by
      var as = this.inverse.remove(b); // what was replaced
      // to get b
      if (as == null) { // b pre-existed replacements so
        as = new HashSet<>(3); // we say it replaces itself.
        as.add(b);
      }

      for (Component a : as) {
        final var aDst = this.map.computeIfAbsent(a, k -> new HashSet<>(cs.size()));
        // should happen when b pre-existed only
        aDst.remove(b);
        aDst.addAll(cs);
      }

      for (Component c : cs) {
        var cSrc = this.inverse.get(c); // should always
        // be null
        if (cSrc == null) {
          cSrc = new HashSet<>(as.size());
          this.inverse.put(c, cSrc);
        }
        cSrc.addAll(as);
      }
    }

    for (Map.Entry<Component, HashSet<Component>> e : next.inverse.entrySet()) {
      final var c = e.getKey();
      if (!inverse.containsKey(c)) {
        final var bs = e.getValue();
        if (!bs.isEmpty()) {
          logger.error("Internal error: component replaced but not represented");
        }
        inverse.put(c, new HashSet<>(3));
      }
    }
  }

  void freeze() {
    frozen = true;
  }

  public Collection<? extends Component> getAdditions() {
    return inverse.keySet();
  }

  public Collection<Component> getReplacementsFor(Component a) {
    return map.get(a);
  }

  public Collection<Component> getReplacedBy(Component b) {
    return inverse.get(b);
  }

  ReplacementMap getInverseMap() {
    return new ReplacementMap(inverse, map);
  }

  public Collection<? extends Component> getRemovals() {
    return map.keySet();
  }

  public boolean isEmpty() {
    return map.isEmpty() && inverse.isEmpty();
  }

  public void print(PrintStream out) {
    var found = false;
    for (final var comp : getRemovals()) {
      if (!found) out.println("  removals:");
      found = true;
      out.println("    " + comp.toString());
      for (final var b : map.get(comp))
        out.println("     `--> " + b.toString());
    }
    if (!found) out.println("  removals: none");

    found = false;
    for (final var b : getAdditions()) {
      if (!found) out.println("  additions:");
      found = true;
      out.println("    " + b.toString());
      for (final var a : inverse.get(b))
        out.println("     ^-- " + a.toString());
    }
    if (!found) out.println("  additions: none");
  }

  public void put(Component a, Collection<? extends Component> bs) {
    if (frozen)
      throw new IllegalStateException("cannot change map after frozen");

    final var oldBs = map.computeIfAbsent(a, k -> new HashSet<>(bs.size()));
    oldBs.addAll(bs);

    for (final var b : bs) {
      final var oldAs = inverse.computeIfAbsent(b, k -> new HashSet<Component>(3));
      oldAs.add(a);
    }
  }

  public void remove(Component a) {
    if (frozen) throw new IllegalStateException("cannot change map after frozen");
    map.put(a, new HashSet<>(3));
  }

  public void replace(Component a, Component b) {
    put(a, Collections.singleton(b));
  }

  public void reset() {
    map.clear();
    inverse.clear();
  }

  @Override
  public String toString() {
    final var out = new ByteArrayOutputStream();
    try (final var p = new PrintStream(out, true, StandardCharsets.UTF_8)) {
      print(p);
    } catch (Exception ignored) {
    }
    return out.toString(StandardCharsets.UTF_8);
  }
}
