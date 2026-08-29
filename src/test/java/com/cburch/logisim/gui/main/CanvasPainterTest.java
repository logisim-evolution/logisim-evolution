/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.proj.Project;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.ImageProducer;
import org.junit.jupiter.api.Test;

class CanvasPainterTest {
  @Test
  void paintContentsReturnsWhenProjectHasNoCurrentCircuit() {
    final var canvas = mock(Canvas.class);
    when(canvas.createImage(any(ImageProducer.class)))
        .thenReturn(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB));
    when(canvas.getSize()).thenReturn(new Dimension(100, 100));
    when(canvas.getZoomFactor()).thenReturn(1.0);

    final var project = mock(Project.class);
    when(project.getCurrentCircuit()).thenReturn(null);

    final var painter = new CanvasPainter(canvas);
    final var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
    final var graphics = image.createGraphics();
    graphics.setClip(0, 0, 100, 100);

    assertDoesNotThrow(() -> painter.paintContents(graphics, project));
    verify(project, never()).getCircuitState();

    graphics.dispose();
  }
}
