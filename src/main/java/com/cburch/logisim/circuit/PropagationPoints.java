/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
      return (other instanceof Entry o)
             ? state.equals(o.state) && item.equals(o.item)
             : false;
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
    for (final var s : source.getSubstates()) {
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
    for (final var state : circState.getSubstates()) addSubstates(stateMap, state, state);

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
    for (final var s : state.getSubstates()) addSubstates(stateMap, s, s);

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
    final var signalsChanged = data.isEmpty() ? "no" : String.valueOf(data.size());
    final var inputSignals = pendingInputs.isEmpty() ? "no" : String.valueOf(pendingInputs.size());
    return S.get("singleStepMessage", signalsChanged, inputSignals);
  }
}
