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
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * Library category icon: isometric 3-cube assembly with theme green outline
 * and subtle semi-transparent top-face duotone shading.
 */
public class CategoryModuleIcon extends VectorBaseIcon {

  public CategoryModuleIcon() {
    this(20);
  }

  public CategoryModuleIcon(int baseSize) {
    super(baseSize);
  }

  @Override
  public Color getSemanticColor() {
    final var isDark = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
    return isDark ? new Color(0x4C, 0xAF, 0x50) : new Color(0x2E, 0x7D, 0x32);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size / 20.0f;
    float strokeW = 1.35f * s;

    final var isDark = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
    final var isEnabled = (c == null || c.isEnabled());

    Color outlineColor;
    if (!isEnabled) {
      outlineColor = toDisabled(isDark ? new Color(0x4C, 0xAF, 0x50) : new Color(0x2E, 0x7D, 0x32));
    } else {
      outlineColor = isDark ? new Color(0x4C, 0xAF, 0x50) : new Color(0x2E, 0x7D, 0x32);
    }

    // Isometric parameters
    float cx = 10.0f * s;
    float dx = 3.9f * s;
    float dy = 2.25f * s;
    float h = 4.5f * s;
    float topY = 1.6f * s;

    // Top Cube vertices:
    float tTopX = cx;
    float tTopY = topY;
    float tLeftX = cx - dx;
    float tLeftY = topY + dy;
    float tRightX = cx + dx;
    float tRightY = topY + dy;
    float tMidX = cx;
    float tMidY = topY + 2 * dy;

    // Center meeting vertex C:
    float cX = cx;
    float cY = tMidY + h; // (10, 10.6)

    // Bottom-Left Cube vertices:
    float blOuterX = cx - 2 * dx;
    float blOuterY = cY;
    float blFrontX = cx - dx;
    float blFrontY = cY + dy;

    // Bottom-Right Cube vertices:
    float brOuterX = cx + 2 * dx;
    float brOuterY = cY;
    float brFrontX = cx + dx;
    float brFrontY = cY + dy;

    // -------------------------------------------------------------
    // 1. SEMI-TRANSPARENT DUOTONE FILL: 3 Top Rhombus Faces (~28% alpha)
    // -------------------------------------------------------------
    Color tintFill = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), 72);
    g2.setColor(tintFill);

    // Top face of Top Cube
    Path2D.Float topCubeTopFace = new Path2D.Float();
    topCubeTopFace.moveTo(tTopX, tTopY);
    topCubeTopFace.lineTo(tLeftX, tLeftY);
    topCubeTopFace.lineTo(tMidX, tMidY);
    topCubeTopFace.lineTo(tRightX, tRightY);
    topCubeTopFace.closePath();
    g2.fill(topCubeTopFace);

    // Top face of Bottom-Left Cube
    Path2D.Float blCubeTopFace = new Path2D.Float();
    blCubeTopFace.moveTo(tLeftX, tLeftY + h);
    blCubeTopFace.lineTo(blOuterX, blOuterY);
    blCubeTopFace.lineTo(blFrontX, blFrontY);
    blCubeTopFace.lineTo(cX, cY);
    blCubeTopFace.closePath();
    g2.fill(blCubeTopFace);

    // Top face of Bottom-Right Cube
    Path2D.Float brCubeTopFace = new Path2D.Float();
    brCubeTopFace.moveTo(tRightX, tRightY + h);
    brCubeTopFace.lineTo(cX, cY);
    brCubeTopFace.lineTo(brFrontX, brFrontY);
    brCubeTopFace.lineTo(brOuterX, brOuterY);
    brCubeTopFace.closePath();
    g2.fill(brCubeTopFace);

    // -------------------------------------------------------------
    // 2. WIREFRAME OUTLINE: Crisp geometry strokes
    // -------------------------------------------------------------
    g2.setColor(outlineColor);
    g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Top cube top face:
    drawLine(g2, tTopX, tTopY, tLeftX, tLeftY);
    drawLine(g2, tLeftX, tLeftY, tMidX, tMidY);
    drawLine(g2, tMidX, tMidY, tRightX, tRightY);
    drawLine(g2, tRightX, tRightY, tTopX, tTopY);

    // Top cube verticals:
    drawLine(g2, tLeftX, tLeftY, tLeftX, tLeftY + h);
    drawLine(g2, tRightX, tRightY, tRightX, tRightY + h);
    drawLine(g2, tMidX, tMidY, cX, cY);

    // Top cube bottom edges (connecting to C):
    drawLine(g2, tLeftX, tLeftY + h, cX, cY);
    drawLine(g2, tRightX, tRightY + h, cX, cY);

    // BL top face remaining edges:
    drawLine(g2, tLeftX, tLeftY + h, blOuterX, blOuterY);
    drawLine(g2, blOuterX, blOuterY, blFrontX, blFrontY);
    drawLine(g2, blFrontX, blFrontY, cX, cY);

    // BL verticals:
    drawLine(g2, blOuterX, blOuterY, blOuterX, blOuterY + h);
    drawLine(g2, blFrontX, blFrontY, blFrontX, blFrontY + h);
    drawLine(g2, cX, cY, cX, cY + h);

    // BL bottom edges:
    drawLine(g2, blOuterX, blOuterY + h, blFrontX, blFrontY + h);
    drawLine(g2, blFrontX, blFrontY + h, cX, cY + h);

    // BR top face remaining edges:
    drawLine(g2, tRightX, tRightY + h, brOuterX, brOuterY);
    drawLine(g2, brOuterX, brOuterY, brFrontX, brFrontY);
    drawLine(g2, brFrontX, brFrontY, cX, cY);

    // BR verticals:
    drawLine(g2, brOuterX, brOuterY, brOuterX, brOuterY + h);
    drawLine(g2, brFrontX, brFrontY, brFrontX, brFrontY + h);

    // BR bottom edges:
    drawLine(g2, cX, cY + h, brFrontX, brFrontY + h);
    drawLine(g2, brFrontX, brFrontY + h, brOuterX, brOuterY + h);
  }

  private static void drawLine(Graphics2D g2, float x1, float y1, float x2, float y2) {
    g2.draw(new Line2D.Float(x1, y1, x2, y2));
  }
}
