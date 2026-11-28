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

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * Vector Add Circuit icon: bold green cross with gradient styling.
 */
public class AddCircuitIcon extends VectorBaseIcon {

  public AddCircuitIcon() {
    this(20);
  }

  public AddCircuitIcon(int baseSize) {
    super(baseSize);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size / 20.0f;
    boolean isEnabled = (c == null || c.isEnabled());

    // Coordinates for the classic 12-point plus cross
    float x1 = 3.0f * s;
    float x2 = 7.5f * s;
    float x3 = 12.5f * s;
    float x4 = 17.0f * s;

    float y1 = 3.0f * s;
    float y2 = 7.5f * s;
    float y3 = 12.5f * s;
    float y4 = 17.0f * s;

    Path2D.Float cross = new Path2D.Float();
    cross.moveTo(x2, y1);
    cross.lineTo(x3, y1);
    cross.lineTo(x3, y2);
    cross.lineTo(x4, y2);
    cross.lineTo(x4, y3);
    cross.lineTo(x3, y3);
    cross.lineTo(x3, y4);
    cross.lineTo(x2, y4);
    cross.lineTo(x2, y3);
    cross.lineTo(x1, y3);
    cross.lineTo(x1, y2);
    cross.lineTo(x2, y2);
    cross.closePath();

    // Vibrant gradient colors (Light: #28B463 to #196F3D, Dark: #76F0A4 to #42DC80)
    boolean isDark = com.cburch.logisim.prefs.AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
    Color topColor = isDark ? new Color(0x76, 0xF0, 0xA4) : new Color(0x28, 0xB4, 0x63);
    Color botColor = isDark ? new Color(0x42, 0xDC, 0x80) : new Color(0x19, 0x6F, 0x3D);
    Color outlineColor = isDark ? new Color(0x30, 0xC8, 0x6D) : new Color(0x14, 0x5A, 0x32);

    if (!isEnabled) {
      topColor = toDisabled(topColor);
      botColor = toDisabled(botColor);
      outlineColor = toDisabled(outlineColor);
    }

    // 1. Fill cross with gradient
    g2.setPaint(new GradientPaint(0, y1, topColor, 0, y4, botColor));
    g2.fill(cross);

    // 2. Draw crisp border outline
    g2.setPaint(outlineColor);
    g2.setStroke(createStroke(1.1f * s));
    g2.draw(cross);
  }
}
