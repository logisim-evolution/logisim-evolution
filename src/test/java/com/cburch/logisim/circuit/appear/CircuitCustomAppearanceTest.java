/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.data.Bounds;
import java.util.List;
import org.junit.jupiter.api.Test;

class CircuitCustomAppearanceTest {

  @Test
  void getObjectsInUsesCustomObjectsEvenWhenParentShowsDefaultAppearance() {
    final var parent = mock(CircuitAppearance.class);
    final var customShape = new Rectangle(10, 10, 20, 20);
    final var defaultShape = new Rectangle(100, 100, 20, 20);
    final var query = Bounds.create(0, 0, 40, 40);
    final var model = new CircuitCustomAppearance(parent);

    when(parent.getCustomObjectsFromBottom()).thenReturn(List.of(customShape));
    when(parent.getObjectsIn(query)).thenReturn(List.of(defaultShape));

    final var result = model.getObjectsIn(query);

    assertEquals(1, result.size());
    assertSame(customShape, result.iterator().next());
  }
}
