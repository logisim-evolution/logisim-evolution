/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons.symbolic;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

/**
 * Scalable vector icon for circuits and VHDL entities, matching Logisim-evolution's
 * original styling with an optional red-outlined tinted magnifying glass overlay.
 */
public class CircuitGlyphIcon extends VectorBaseIcon {

  private boolean isCurrentCircuit;
  private final boolean isVhdl;
  private final String entityName;

  public CircuitGlyphIcon(boolean isCurrentCircuit) {
    this(isCurrentCircuit, false, "main", AppPreferences.getScaled(20));
  }

  public CircuitGlyphIcon(boolean isCurrentCircuit, int size) {
    this(isCurrentCircuit, false, "main", size);
  }

  public CircuitGlyphIcon(boolean isCurrentCircuit, boolean isVhdl, int size) {
    this(isCurrentCircuit, isVhdl, isVhdl ? "VHDL" : "main", size);
  }

  public CircuitGlyphIcon(boolean isCurrentCircuit, boolean isVhdl, String entityName, int size) {
    super(size);
    this.isCurrentCircuit = isCurrentCircuit;
    this.isVhdl = isVhdl;
    this.entityName = entityName != null ? entityName : (isVhdl ? "VHDL" : "main");
  }

  public void setCurrentCircuit(boolean current) {
    this.isCurrentCircuit = current;
  }

  public boolean isCurrentCircuit() {
    return isCurrentCircuit;
  }

  public boolean isVhdl() {
    return isVhdl;
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    float s = size;

    if (isVhdl) {
      paintVhdlBody(g2, s, color);
    } else {
      paintCircuitBody(g2, s, color);
    }

    if (isCurrentCircuit) {
      paintRedMagnifyingGlass(g2, s);
    }
  }

  private void paintCircuitBody(Graphics2D g2, float s, Color color) {
    g2.setColor(color);
    float strokeW = Math.max(1.5f, s * 0.08f);
    g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

    int chipX = Math.round(s * 0.16f);
    int chipY = Math.round(s * 0.04f);
    int chipW = Math.round(s * 0.68f);
    int chipH = Math.round(s * 0.92f);

    // 1. Draw chip package rectangular outline
    g2.drawRect(chipX, chipY, chipW, chipH);

    // 2. Draw solid bottom bar
    int barH = Math.round(s * 0.24f);
    int barY = chipY + chipH - barH;
    g2.fillRect(chipX, barY, chipW, barH);

    // 3. Draw horizontal pin stubs on left and right (matching paintEvolutionIcon)
    float pinLen = s * 0.12f;
    float[] pinFractions = {0.18f, 0.40f, 0.62f};
    for (int i = 0; i < pinFractions.length; i++) {
      int py = Math.round(chipY + chipH * pinFractions[i]);
      // Left pin
      g2.drawLine(Math.round(chipX - pinLen), py, chipX, py);
      // Right pin (top 2 pins)
      if (i < 2) {
        g2.drawLine(chipX + chipW, py, Math.round(chipX + chipW + pinLen), py);
      }
    }

    // 4. Inverted label text inside the solid bottom bar
    final var isDark = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
    Color textCol = isDark ? new Color(0x22, 0x22, 0x22) : Color.WHITE;
    g2.setColor(textCol);
    Font f = g2.getFont().deriveFont(Font.BOLD, Math.max(7.0f, s * 0.20f));
    try {
      TextLayout tl = new TextLayout(entityName, f, g2.getFontRenderContext());
      float tx = chipX + (chipW - (float) tl.getBounds().getWidth()) / 2.0f;
      float ty = barY + (barH + (float) tl.getAscent() - (float) tl.getDescent()) / 2.0f;
      tl.draw(g2, tx, ty);
    } catch (Exception ignored) {
      // Fallback
    }
  }

  private void paintVhdlBody(Graphics2D g2, float s, Color color) {
    // Match ArithmeticIcon("VHDL") exactly: simple border rect + "VH" top / "DL" bottom,
    // text centered via getBounds().getCenterX/Y, no name tag, no decorations.
    g2.setColor(color);
    int border = Math.round(s * 0.0625f); // scale(1) equivalent
    g2.setStroke(new BasicStroke(Math.max(1.0f, border * 2.0f),
        BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

    int iw = Math.round(s);
    int ih = Math.round(s);
    // rect: drawRect(scale(1), scale(1), w - scale(2), h - scale(2))
    g2.drawRect(border, border, iw - border * 2, ih - border * 2);

    // font size = iconWidth / 2  (nrOfChars=2)
    Font f = g2.getFont().deriveFont(Font.BOLD, (float) iw / 2.0f);
    try {
      int yOff = ih >> 2; // length > nrOfChars → yOff = height/4

      // Top line: "VH"
      TextLayout topTl = new TextLayout("VH", f, g2.getFontRenderContext());
      topTl.draw(g2,
          (float) (iw / 2.0 - topTl.getBounds().getCenterX()),
          (float) (yOff - topTl.getBounds().getCenterY()));

      // Bottom line: "DL"
      TextLayout botTl = new TextLayout("DL", f, g2.getFontRenderContext());
      botTl.draw(g2,
          (float) (iw / 2.0 - botTl.getBounds().getCenterX()),
          (float) (3 * yOff - botTl.getBounds().getCenterY()));
    } catch (Exception ignored) {
      // Fallback: nothing drawn
    }
  }

  private void paintRedMagnifyingGlass(Graphics2D g2, float s) {
    final var isDark = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
    // Theme-adaptive outline: bold coral red (#FF4D4D) in dark theme, deep ruby crimson (#D32F2F) in light theme
    Color redOutline = isDark ? new Color(0xFF, 0x4D, 0x4D) : new Color(0xD3, 0x2F, 0x2F);
    // Tinted transparent glass interior
    Color tintedGlass = isDark ? new Color(255, 120, 120, 70) : new Color(255, 90, 90, 85);

    // Separate centering formulas for small (list ~20px) vs large (tile ~40px) sizes.
    // At small sizes pixel rounding matters more, so we use slightly different calibrated offsets.
    // VHDL body center is exactly 0.50/0.50 (full square).
    // Circuit body open window (above solid bottom bar) has visual center at ~0.38 vertically.
    final boolean isSmall = s < 30f;
    float lensX;
    float lensY;
    float lensR;
    if (isVhdl) {
      lensX = s * 0.50f;
      lensY = isSmall ? s * 0.50f : s * 0.50f; // VHDL: always exact center
      lensR = isSmall ? s * 0.24f : s * 0.26f;
    } else {
      // Circuit chip: open window is from chipY(4%) to barY(72%), center ~ 38%
      lensX = s * 0.50f;
      lensY = isSmall ? s * 0.38f : s * 0.40f;
      lensR = isSmall ? s * 0.18f : s * 0.22f;
    }
    float rimStroke = Math.max(1.6f, s * 0.11f);
    float handleStroke = Math.max(2.0f, s * 0.14f);

    // Handle extending down-right at 45 degrees
    // Crucial: start strictly outside the outer rim stroke so it never protrudes into the lens
    float handleAngle = (float) Math.toRadians(45.0);
    float startDist = lensR + (rimStroke * 0.5f);
    float endDist = startDist + (s * 0.28f);
    float hStartX = lensX + (float) Math.cos(handleAngle) * startDist;
    float hStartY = lensY + (float) Math.sin(handleAngle) * startDist;
    float hEndX = lensX + (float) Math.cos(handleAngle) * endDist;
    float hEndY = lensY + (float) Math.sin(handleAngle) * endDist;

    // 1. Tinted transparent interior
    g2.setColor(tintedGlass);
    int d = Math.round(lensR * 2.0f);
    g2.fillOval(Math.round(lensX - lensR), Math.round(lensY - lensR), d, d);

    // 2. Crisp red circular rim
    g2.setColor(redOutline);
    g2.setStroke(new BasicStroke(rimStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.drawOval(Math.round(lensX - lensR), Math.round(lensY - lensR), d, d);

    // 3. Handle attached flush to outer rim
    g2.setStroke(new BasicStroke(handleStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.drawLine(Math.round(hStartX), Math.round(hStartY), Math.round(hEndX), Math.round(hEndY));
  }
}
