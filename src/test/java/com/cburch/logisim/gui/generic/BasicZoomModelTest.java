/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class BasicZoomModelTest {
  @Test
  void mouseZoomWithoutCanvasUpdatesZoomFactor() {
    final var model =
        new BasicZoomModel(
            AppPreferences.APPEARANCE_SHOW_GRID,
            AppPreferences.APPEARANCE_ZOOM,
            List.of(100.0, 200.0),
            null);
    model.setZoomFactor(1.0);

    model.setZoomFactor(
        2.0, new MouseEvent(new JPanel(), MouseEvent.MOUSE_MOVED, 0, 0, 10, 20, 0, false));

    assertEquals(2.0, model.getZoomFactor());
  }
}
