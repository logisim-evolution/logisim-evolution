/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocUpSimulationState;
import com.cburch.logisim.soc.data.SocUpStateInterface;
import com.cburch.logisim.soc.data.TraceInfo;
import com.cburch.logisim.soc.util.AssemblerInterface;
import java.awt.Graphics2D;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class CpuDrawSupportConcurrentTraceReproTest {
  @Test
  void drawTraceHandlesTraceListChangesDuringPaint() {
    final var traces = new LinkedList<TraceInfo>();
    traces.add(
        new TraceInfo(0, 0, "mutates trace list during paint", false) {
          @Override
          public void paint(Graphics2D g, int yOffset, boolean scale) {
            traces.addFirst(new TraceInfo(1, 1, "new instruction", false));
          }
        });
    traces.add(new TraceInfo(2, 2, "next instruction", false));

    final var image = new BufferedImage(700, 700, BufferedImage.TYPE_INT_ARGB);
    final var graphics = image.createGraphics();

    assertDoesNotThrow(
        () -> CpuDrawSupport.drawTrace(graphics, 0, 0, false, new TestCpuState(traces)));
  }

  private record TestCpuState(LinkedList<TraceInfo> traces) implements SocUpStateInterface {
    @Override
    public int getLastRegisterWritten() {
      return -1;
    }

    @Override
    public String getRegisterValueHex(int index) {
      return "0x00000000";
    }

    @Override
    public String getRegisterAbiName(int index) {
      return "";
    }

    @Override
    public String getRegisterNormalName(int index) {
      return "r" + index;
    }

    @Override
    public int getProgramCounter() {
      return 0;
    }

    @Override
    public LinkedList<TraceInfo> getTraces() {
      return traces;
    }

    @Override
    public void simButtonPressed() {}

    @Override
    public SocUpSimulationState getSimState() {
      return null;
    }

    @Override
    public boolean programLoaded() {
      return true;
    }

    @Override
    public WindowListener getWindowListener() {
      return null;
    }

    @Override
    public JPanel getAsmWindow() {
      return null;
    }

    @Override
    public JPanel getStatePanel() {
      return null;
    }

    @Override
    public AssemblerInterface getAssembler() {
      return null;
    }

    @Override
    public SocProcessorInterface getProcessorInterface() {
      return null;
    }

    @Override
    public String getProcessorType() {
      return "nios2";
    }

    @Override
    public int getElfType() {
      return 0;
    }

    @Override
    public void repaint() {}
  }
}
