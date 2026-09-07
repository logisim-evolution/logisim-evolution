/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.cburch.draw.model.Drawing;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.draw.shapes.Poly;
import com.cburch.logisim.data.Location;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelMoveHandleActionTest {

  @Test
  void movingPolyHandleCanBeUndoneAndRedone() {
    final var drawing = new Drawing();
    final var poly =
        new Poly(
            false,
            List.of(
                Location.create(0, 0, false),
                Location.create(10, 0, false),
                Location.create(10, 10, false)));
    drawing.addObjects(0, List.of(poly));
    final var originalHandle = poly.getHandles(null).get(0);
    final var gesture = new HandleGesture(originalHandle, 5, 7, 0);
    final var action = new ModelMoveHandleAction(drawing, gesture);

    action.doIt();

    final var movedHandle = action.getNewHandle();
    assertHandleAt(movedHandle, 5, 7);
    assertSame(movedHandle, gesture.getResultingHandle());
    assertSame(movedHandle, poly.getHandles(null).get(0));

    action.undo();

    assertHandleAt(poly.getHandles(null).get(0), 0, 0);

    action.doIt();

    assertHandleAt(action.getNewHandle(), 5, 7);
    assertSame(action.getNewHandle(), poly.getHandles(null).get(0));
  }

  private static void assertHandleAt(Handle handle, int x, int y) {
    assertNotNull(handle);
    assertEquals(x, handle.getX());
    assertEquals(y, handle.getY());
  }
}
