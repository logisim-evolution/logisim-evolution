/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.chrono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.log.Signal;
import com.cburch.logisim.gui.log.SignalInfo;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.DefaultListSelectionModel;
import org.junit.jupiter.api.Test;

class RightPanelTest {

  @Test
  void vectorExportPaintsWaveformsDirectly() {
    final var signalInfo = mock(SignalInfo.class);
    when(signalInfo.getWidth()).thenReturn(1);
    when(signalInfo.format(any(Value.class))).thenAnswer(inv -> inv.getArgument(0).toString());
    final var signal = new Signal(0, signalInfo, Value.FALSE, 10, 0, 0);
    signal.extend(Value.TRUE, 10);

    final var model = mock(Model.class);
    when(model.getSignalCount()).thenReturn(1);
    when(model.getSignal(0)).thenReturn(signal);
    when(model.getStartTime()).thenReturn(0L);
    when(model.getEndTime()).thenReturn(20L);
    when(model.getTimeScale()).thenReturn(10L);

    final var chronoPanel = mock(ChronoPanel.class);
    when(chronoPanel.getModel()).thenReturn(model);
    when(chronoPanel.rowColors(any(SignalInfo.class), anyBoolean()))
        .thenReturn(
            new Color[] {
              Color.LIGHT_GRAY,
              Color.GRAY,
              Color.BLACK,
              Color.PINK,
              Color.BLACK,
              Color.ORANGE,
              Color.BLACK
            });

    final var rightPanel = new RightPanel(chronoPanel, new DefaultListSelectionModel());
    final var exportGraphics = mock(Graphics2D.class);
    final var waveformGraphics = mock(Graphics2D.class);
    final var metricsGraphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
    when(exportGraphics.create()).thenReturn(waveformGraphics);
    when(waveformGraphics.getFontMetrics()).thenReturn(metricsGraphics.getFontMetrics());

    rightPanel.paintExportImage(exportGraphics);

    verify(waveformGraphics, atLeastOnce()).drawLine(anyInt(), anyInt(), anyInt(), anyInt());
    metricsGraphics.dispose();
  }
}
