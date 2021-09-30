/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.gui;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.data.SocUpSimulationState;
import com.cburch.logisim.soc.data.SocUpStateInterface;
import com.cburch.logisim.soc.data.TraceInfo;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

public class CpuDrawSupport {

  public static final int NR_OF_TRACES = 21;
  public static final int TRACE_HEIGHT = 20;

  public static final Bounds busConBounds = Bounds.create(50, 600, 280, 20);
  public static final Bounds simStateBounds = Bounds.create(340, 600, 270, 20);
  public static final Bounds upStateBounds = Bounds.create(50, 10, 590, 590);

  public static Bounds getBounds(int x, int y, int width, int height, boolean scale) {
    if (scale)
      return Bounds.create(
          AppPreferences.getScaled(x),
          AppPreferences.getScaled(y),
          AppPreferences.getScaled(width),
          AppPreferences.getScaled(height));
    return Bounds.create(x, y, width, height);
  }

  public static int getBlockWidth(Graphics2D g2, boolean scale) {
    FontMetrics f = g2.getFontMetrics();
    int StrWidth = f.stringWidth("0x00000000") + (scale ? AppPreferences.getScaled(2) : 2);
    int blkPrefWidth = scale ? AppPreferences.getScaled(80) : 80;
    return Math.max(StrWidth, blkPrefWidth);
  }

  public static void drawRegisters(
      Graphics2D g, int x, int y, boolean scale, SocUpStateInterface cpu) {
    Graphics2D g2 = (Graphics2D) g.create();
    Bounds bds;
    if (scale) g2.setFont(AppPreferences.getScaledFont(g.getFont()));
    g2.translate(x, y);
    int blockWidth = getBlockWidth(g2, scale);
    int blockX = ((scale ? AppPreferences.getScaled(160) : 160) - blockWidth) / 2;
    if (scale) {
      blockWidth = AppPreferences.getDownScaled(blockWidth);
      blockX = AppPreferences.getDownScaled(blockX);
    }
    g2.setColor(Color.YELLOW);
    bds = getBounds(0, 0, 160, 495, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLUE);
    bds = getBounds(0, 0, 160, 15, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.YELLOW);
    bds = getBounds(80, 6, 0, 0, scale);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imRegisterFile"), bds.getX(), bds.getY());
    g2.setColor(Color.BLACK);
    bds = getBounds(0, 0, 160, 495, scale);
    g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    for (int i = 0; i < 32; i++) {
      bds = getBounds(20, 21 + i * 15, 0, 0, scale);
      GraphicsUtil.drawCenteredText(g2, cpu.getRegisterNormalName(i), bds.getX(), bds.getY());
      g2.setColor(i == cpu.getLastRegisterWritten() ? Color.BLUE : Color.WHITE);
      bds = getBounds(blockX, 16 + i * 15, blockWidth, 13, scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.BLACK);
      g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(i == cpu.getLastRegisterWritten() ? Color.WHITE : Color.BLUE);
      bds = getBounds(blockX + blockWidth / 2, 21 + i * 15, 0, 0, scale);
      GraphicsUtil.drawCenteredText(g2, cpu.getRegisterValueHex(i), bds.getX(), bds.getY());
      g2.setColor(Color.darkGray);
      bds = getBounds(140, 21 + i * 15, 0, 0, scale);
      GraphicsUtil.drawCenteredText(g2, cpu.getRegisterAbiName(i), bds.getX(), bds.getY());
      g2.setColor(Color.BLACK);
    }
    g2.dispose();
  }

  public static void drawHexReg(
      Graphics2D g, int x, int y, boolean scale, int value, String Name, boolean valid) {
    Graphics2D g2 = (Graphics2D) g.create();
    Bounds bds;
    if (scale) g2.setFont(AppPreferences.getScaledFont(g.getFont()));
    bds = getBounds(x, y, 0, 0, scale);
    g2.translate(bds.getX(), bds.getY());
    int blockWidth = getBlockWidth(g2, scale);
    if (scale) blockWidth = AppPreferences.getDownScaled(blockWidth);
    g2.setColor(Color.YELLOW);
    bds = getBounds(0, 0, blockWidth, 30, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLUE);
    bds = getBounds(0, 0, blockWidth, 15, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.YELLOW);
    bds = getBounds(blockWidth / 2, 6, 0, 0, scale);
    GraphicsUtil.drawCenteredText(g2, Name, bds.getX(), bds.getY());
    g2.setColor(Color.BLACK);
    bds = getBounds(0, 0, blockWidth, 30, scale);
    g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.WHITE);
    bds = getBounds(1, 16, blockWidth - 2, 13, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLACK);
    g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.MAGENTA);
    bds = getBounds(blockWidth / 2, 21, 0, 0, scale);
    GraphicsUtil.drawCenteredText(
        g2, valid ? String.format("0x%08X", value) : "??????????", bds.getX(), bds.getY());
    g2.dispose();
  }

  private static void drawIrq(Graphics2D g, int nr, boolean scale, boolean irq, boolean mask) {
    Bounds block = getBounds(568 - nr * 17, 20, 15, 15, scale);
    g.setColor(Color.WHITE);
    g.fillRect(block.getX(), block.getY(), block.getWidth(), block.getHeight());
    g.setColor(Color.BLACK);
    g.drawRect(block.getX(), block.getY(), block.getWidth(), block.getHeight());
    block = getBounds(568 - nr * 17, 37, 15, 15, scale);
    g.setColor(Color.WHITE);
    g.fillRect(block.getX(), block.getY(), block.getWidth(), block.getHeight());
    g.setColor(Color.BLACK);
    g.drawRect(block.getX(), block.getY(), block.getWidth(), block.getHeight());
    block = getBounds(576 - nr * 17, 35, 576 - nr * 17, 37, scale);
    g.drawLine(block.getX(), block.getY(), block.getWidth(), block.getHeight());
    block = getBounds(576 - nr * 17, 52, 576 - nr * 17, 54, scale);
    g.drawLine(block.getX(), block.getY(), block.getWidth(), block.getHeight());
    block = getBounds(576 - nr * 17, 11, 0, 0, scale);
    GraphicsUtil.drawCenteredText(g, Integer.toString(nr), block.getX(), block.getY());
    block = getBounds(576 - nr * 17, 27, 576 - nr * 17, 44, scale);
    GraphicsUtil.drawCenteredText(g, irq ? "1" : "0", block.getX(), block.getY());
    GraphicsUtil.drawCenteredText(g, mask ? "1" : "0", block.getWidth(), block.getHeight());
  }

  public static void drawIRQs(
      Graphics2D g, int x, int y, boolean scale, int nrOfIrqs, int irqs, int irqMask) {
    Graphics2D g2 = (Graphics2D) g.create();
    Bounds bds;
    if (scale) g2.setFont(AppPreferences.getScaledFont(g.getFont()));
    bds = getBounds(x, y, 0, 0, scale);
    g2.translate(bds.getX(), bds.getY());
    bds = getBounds(0, 0, 585, 90, scale);
    g2.setColor(Color.YELLOW);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLACK);
    g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    for (int i = 0; i < nrOfIrqs; i++) {
      drawIrq(g2, i, scale, ((irqs >> i) & 1) != 0, ((irqMask >> i) & 1) != 0);
    }
    bds = getBounds(2, 31, 2, 48, scale);
    g2.drawString("IRQs:", bds.getX(), bds.getY());
    g2.drawString("Mask:", bds.getWidth(), bds.getHeight());
    bds = getBounds(41, 54, 542, 15, scale);
    g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    GraphicsUtil.drawCenteredText(g2, "\u2265" + "1", bds.getCenterX(), bds.getCenterY());
    bds = getBounds(312, 69, 312, 71, scale);
    g2.drawLine(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    bds = getBounds(304, 71, 15, 15, scale);
    g2.setColor(Color.WHITE);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLACK);
    g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    GraphicsUtil.drawCenteredText(
        g2, (irqs & irqMask) != 0 ? "1" : "0", bds.getCenterX(), bds.getCenterY());
    g2.dispose();
  }

  public static void drawTrace(Graphics2D g, int x, int y, boolean scale, SocUpStateInterface cpu) {
    Graphics2D g2 = (Graphics2D) g.create();
    Bounds bds;
    if (scale) g2.setFont(AppPreferences.getScaledFont(g.getFont()));
    int blockWidth = getBlockWidth(g2, scale);
    if (scale) blockWidth = AppPreferences.getDownScaled(blockWidth);
    bds = getBounds(x, y, 0, 0, scale);
    g2.translate(bds.getX(), bds.getY());
    g2.setColor(Color.YELLOW);
    bds = getBounds(0, 0, 415, 455, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLUE);
    bds = getBounds(0, 0, 415, 15, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.YELLOW);
    bds = getBounds(207, 6, 0, 0, scale);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imExecutionTrace"), bds.getX(), bds.getY());
    g2.setColor(Color.BLACK);
    bds = getBounds(0, 0, 415, 455, scale);
    g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.WHITE);
    bds = getBounds(5, 15, blockWidth, 15, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    bds = getBounds(10 + blockWidth, 15, blockWidth, 15, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    bds = getBounds(15 + 2 * blockWidth, 15, 395 - 2 * blockWidth, 15, scale);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLACK);
    bds = getBounds(5 + blockWidth / 2, 21, 0, 0, scale);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imProgramCounter"), bds.getX(), bds.getY());
    bds = getBounds(10 + blockWidth + blockWidth / 2, 21, 0, 0, scale);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imBinInstruction"), bds.getX(), bds.getY());
    bds = getBounds(215 + blockWidth, 21, 0, 0, scale);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imAsmInstruction"), bds.getX(), bds.getY());
    if (cpu.getTraces().isEmpty()) {
      bds = getBounds(207, 250, 0, 0, scale);
      GraphicsUtil.drawCenteredText(g2, S.get("Rv32imEmptyTrace"), bds.getX(), bds.getY());
    } else {
      int yOff = 30;
      for (TraceInfo t : cpu.getTraces()) {
        t.paint(g2, yOff, scale);
        yOff += TRACE_HEIGHT;
      }
    }
    g2.dispose();
  }

  public static class SimStatePoker extends InstancePoker {
    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      Location loc = state.getInstance().getLocation();
      Bounds bds = state.getInstance().getBounds();
      Bounds simButton =
          Bounds.create(
              bds.getWidth() - 300,
              bds.getHeight() - 40,
              simStateBounds.getWidth(),
              simStateBounds.getHeight());
      Bounds bloc = SocUpSimulationState.getButtonLocation(loc.getX(), loc.getY(), simButton);
      if (bloc.contains(e.getX(), e.getY())) {
        ((SocUpStateInterface) state.getData()).simButtonPressed();
      }
    }
  }
}
