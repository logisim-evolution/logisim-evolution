/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.gui.generic.ZoomModel;
import java.awt.Point;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

final class CircuitViewMemory {
  private final IdentityHashMap<Circuit, ViewState> states = new IdentityHashMap<>();

  void remember(Circuit circuit, ZoomModel zoomModel, Point viewPosition) {
    if (circuit == null || zoomModel == null || viewPosition == null) return;
    states.put(circuit, new ViewState(zoomModel.getZoomFactor(), new Point(viewPosition)));
  }

  void forget(Circuit circuit) {
    if (circuit != null) states.remove(circuit);
  }

  boolean restore(Circuit circuit, ZoomModel zoomModel, Consumer<Point> viewPositionSetter) {
    if (circuit == null || zoomModel == null || viewPositionSetter == null) return false;
    final var state = states.get(circuit);
    if (state == null) return false;
    zoomModel.setZoomFactor(state.zoomFactor);
    viewPositionSetter.accept(new Point(state.viewPosition));
    return true;
  }

  private record ViewState(double zoomFactor, Point viewPosition) {}
}
