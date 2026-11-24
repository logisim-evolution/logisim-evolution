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
import java.awt.Graphics2D;

/**
 * Vector recreation of classic Logisim circuit icon: dual-in-line IC package with pins,
 * featuring a modern magnifying glass badge with green outline when viewed on canvas.
 */
public class CircuitGlyphIcon extends VectorBaseIcon {

  private boolean isCurrentCircuit;

  public CircuitGlyphIcon(boolean isCurrentCircuit) {
    this(isCurrentCircuit, 16);
  }

  public CircuitGlyphIcon(boolean isCurrentCircuit, int size) {
    super(size);
    this.isCurrentCircuit = isCurrentCircuit;
  }

  public void setCurrentCircuit(boolean current) {
    this.isCurrentCircuit = current;
  }

  public boolean isCurrentCircuit() {
    return isCurrentCircuit;
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    float s = size;

    // DIP IC chip dimensions (faithful to classic subcirc.gif aspect ratio)
    int chipX = Math.round(s * 0.22f);
    int chipY = Math.round(s * 0.08f);
    int chipW = Math.round(s * 0.56f);
    int chipH = Math.round(s * 0.84f);

    // 1. Draw the IC chip body
    g2.setStroke(new BasicStroke(Math.max(1.2f, s * 0.085f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.setColor(color);
    g2.drawRoundRect(chipX, chipY, chipW, chipH, Math.round(s * 0.12f), Math.round(s * 0.12f));

    // 2. Draw 4 pairs of horizontal DIP pins on left and right edges
    float pinLen = s * 0.16f;
    float[] pinFractions = {0.24f, 0.42f, 0.60f, 0.78f};
    g2.setStroke(new BasicStroke(Math.max(1.0f, s * 0.075f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (float f : pinFractions) {
      int py = Math.round(s * f);
      // Left pin
      g2.drawLine(Math.round(chipX - pinLen), py, chipX, py);
      // Right pin
      g2.drawLine(chipX + chipW, py, Math.round(chipX + chipW + pinLen), py);
    }

    // 3. Top notch indicator on the chip
    int notchW = Math.max(3, Math.round(s * 0.22f));
    int notchX = chipX + (chipW - notchW) / 2;
    g2.drawLine(notchX, chipY, notchX + notchW, chipY);

    // 4. Modern Magnifying Glass overlay when viewed / active on canvas
    if (isCurrentCircuit) {
      final var isDark = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
      Color magOutline = isDark ? Color.WHITE : Color.BLACK;

      // Lens center & radius
      float lensX = s * 0.48f;
      float lensY = s * 0.46f;
      float lensR = s * 0.26f;

      // Handle properties (diagonal 45 degrees toward bottom-right)
      float handleAngle = (float) Math.toRadians(45.0);
      float startHandleDist = lensR * 0.88f;
      float endHandleDist = s * 0.48f;
      float hStartX = lensX + (float) Math.cos(handleAngle) * startHandleDist;
      float hStartY = lensY + (float) Math.sin(handleAngle) * startHandleDist;
      float hEndX = lensX + (float) Math.cos(handleAngle) * endHandleDist;
      float hEndY = lensY + (float) Math.sin(handleAngle) * endHandleDist;

      // Draw thick handle with rounded cap
      g2.setColor(magOutline);
      g2.setStroke(new BasicStroke(Math.max(2.2f, s * 0.16f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g2.drawLine(Math.round(hStartX), Math.round(hStartY), Math.round(hEndX), Math.round(hEndY));

      // Draw semi-transparent lens glass background
      Color glassBg = isDark ? new Color(30, 30, 30, 200) : new Color(255, 255, 255, 220);
      g2.setColor(glassBg);
      int d = Math.round(lensR * 2);
      g2.fillOval(Math.round(lensX - lensR), Math.round(lensY - lensR), d, d);

      // Draw modern circular lens ring with black/white outline
      g2.setColor(magOutline);
      g2.setStroke(new BasicStroke(Math.max(1.8f, s * 0.13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g2.drawOval(Math.round(lensX - lensR), Math.round(lensY - lensR), d, d);

      // Subtle light glass reflection arc inside the lens
      Color glareColor = isDark ? new Color(255, 255, 255, 140) : new Color(0, 0, 0, 90);
      g2.setColor(glareColor);
      g2.setStroke(new BasicStroke(Math.max(1.0f, s * 0.065f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      int glareR = Math.round(lensR * 0.65f);
      g2.drawArc(Math.round(lensX - glareR), Math.round(lensY - glareR), glareR * 2, glareR * 2, 105, 75);
    }
  }
}
