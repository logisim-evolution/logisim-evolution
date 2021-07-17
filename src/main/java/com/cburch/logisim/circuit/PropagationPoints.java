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

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import java.util.HashMap;
import java.util.HashSet;

class PropagationPoints {
  private static class Entry<T> {
    private final CircuitState state;
    private final T item;

    private Entry(CircuitState state, T item) {
      this.state = state;
      this.item = item;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Entry)) return false;
      final var o = (Entry) other;
      return state.equals(o.state) && item.equals(o.item);
    }

    @Override
    public int hashCode() {
      return state.hashCode() * 31 + item.hashCode();
    }
  }

  private final HashSet<Entry<Location>> data =  new HashSet<>();
  private final HashSet<Entry<Component>> pendingInputs =  new HashSet<>();

  PropagationPoints() {
    // no-op implementation
  }

  void addPendingInput(CircuitState state, Component comp) {
    pendingInputs.add(new Entry<>(state, comp));
  }

  void add(CircuitState state, Location loc) {
    data.add(new Entry<>(state, loc));
  }

  private void addSubstates(HashMap<CircuitState, CircuitState> map, CircuitState source, CircuitState value) {
    map.put(source, value);
    for (final var s : source.getSubStates()) {
      addSubstates(map, s, value);
    }
  }

  void clear() {
    data.clear();
    pendingInputs.clear();
  }

  void draw(ComponentDrawContext context) {
    if (data.isEmpty()) return;

    final var circState = context.getCircuitState();
    final var stateMap = new HashMap<CircuitState, CircuitState>();
    for (final var state : circState.getSubStates()) addSubstates(stateMap, state, state);

    final var g = context.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    for (final var entry : data) {
      if (entry.state == circState) {
        final var p = entry.item;
        g.drawOval(p.getX() - 4, p.getY() - 4, 8, 8);
      } else if (stateMap.containsKey(entry.state)) {
        final var subState = stateMap.get(entry.state);
        final var subCircuit = subState.getSubcircuit();
        final var bound = subCircuit.getBounds();
        g.drawRect(bound.getX(), bound.getY(), bound.getWidth(), bound.getHeight());
      }
    }
    GraphicsUtil.switchToWidth(g, 1);
  }

  void drawPendingInputs(ComponentDrawContext context) {
    if (pendingInputs.isEmpty())
      return;

    final var state = context.getCircuitState();
    final var stateMap = new HashMap<CircuitState, CircuitState>();
    for (final var s : state.getSubStates()) addSubstates(stateMap, s, s);

    final var g = context.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    for (Entry<Component> e : pendingInputs) {
      Component comp;
      if (e.state == state)
        comp = e.item;
      else if (stateMap.containsKey(e.state))
        comp = stateMap.get(e.state).getSubcircuit();
      else
        continue;
      final var b = comp.getBounds();
      g.drawRect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
    }

    GraphicsUtil.switchToWidth(g, 1);
  }

  String getSingleStepMessage() {
    final var n = data.isEmpty() ? "no" : "" + data.size();
    final var m = pendingInputs.isEmpty() ? "no" : "" + pendingInputs.size();
    return S.get("singleStepMessage", n, m);
  }
}
