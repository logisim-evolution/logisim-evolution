/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.chrono;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.generic.TikZWriter;
import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.log.Signal;
import com.cburch.logisim.gui.log.SignalInfo;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.DefaultListSelectionModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RightPanelTest {

  @TempDir Path tempDir;

  @Test
  void vectorExportPaintsWaveformPaths() throws Exception {
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
    final var writer = new TikZWriter();
    rightPanel.paintExportImage(writer);

    final var exportFile = tempDir.resolve("timing.svg").toFile();
    writer.writeSvg(100, 30, exportFile);

    final var svg = Files.readString(exportFile.toPath());
    assertTrue(svg.contains("stroke=\"#000000\""));
    assertTrue(svg.contains("V28") || svg.contains("V2"));
  }
}
