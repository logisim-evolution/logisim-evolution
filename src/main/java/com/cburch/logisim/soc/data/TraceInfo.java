/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.gui.CpuDrawSupport;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class TraceInfo {
  private final int pc;
  private final int instruction;
  private final String asm;
  private boolean error;

  public TraceInfo(int pc, int instruction, String asm, boolean error) {
    this.pc = pc;
    this.instruction = instruction;
    this.asm = asm;
    this.error = error;
  }

  public void setError() {
    error = true;
  }

  public void paint(Graphics2D g, int yOffset, boolean scale) {
    int blockWidth = CpuDrawSupport.getBlockWidth(g, scale);
    if (scale) blockWidth = AppPreferences.getDownScaled(blockWidth);
    int xOff = 5;
    paintBox(g, xOff, yOffset, pc, scale, blockWidth);
    xOff += blockWidth + 5;
    paintBox(g, xOff, yOffset, instruction, scale, blockWidth);
    xOff += blockWidth + 5;
    g.setColor(error ? Color.RED : Color.BLACK);
    Font f = g.getFont();
    Font myFont =
        scale
            ? AppPreferences.getScaledFont(
                new Font("Monospaced", Font.PLAIN, 12).deriveFont(Font.BOLD))
            : new Font("Monospaced", Font.PLAIN, 12).deriveFont(Font.BOLD);
    g.setFont(myFont);
    Bounds bds = CpuDrawSupport.getBounds(xOff, yOffset + 15, 0, 0, scale);
    g.drawString(asm, bds.getX(), bds.getY());
    g.setFont(f);
  }

  private void paintBox(Graphics2D g, int x, int y, int value, boolean scale, int blockWidth) {
    g.setColor(Color.WHITE);
    Bounds bds;
    bds = CpuDrawSupport.getBounds(x, y + 1, blockWidth, CpuDrawSupport.TRACE_HEIGHT - 2, scale);
    g.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g.setColor(Color.BLACK);
    g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g.setColor(error ? Color.RED : Color.DARK_GRAY);
    bds =
        CpuDrawSupport.getBounds(
            x + blockWidth / 2, y + CpuDrawSupport.TRACE_HEIGHT / 2, 0, 0, scale);
    GraphicsUtil.drawCenteredText(g, String.format("0x%08X", value), bds.getX(), bds.getY());
    g.setColor(Color.BLACK);
  }
}
