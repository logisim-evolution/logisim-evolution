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
import java.awt.geom.RoundRectangle2D;

/**
 * Vector Toggle Appearance icon: package outline in body color with diagonal edit pencil.
 */
public class AppearanceIcon extends VectorBaseIcon {

  public AppearanceIcon() {
    this(20);
  }

  public AppearanceIcon(int baseSize) {
    super(baseSize);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size / 20.0f;
    boolean isEnabled = (c == null || c.isEnabled());
    boolean isDark = com.cburch.logisim.prefs.AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());

    g2.setStroke(createStroke(1.75f * s));
    g2.setColor(color);

    // Box bounds
    float x = 4.0f * s;
    float y = 3.5f * s;
    float w = 12.0f * s;
    float h = 13.0f * s;
    float arc = 2.5f * s;

    // Chip package outline in body color
    g2.draw(new RoundRectangle2D.Float(x, y, w, h, arc, arc));

    // Pin stubs on left and right in body color
    float pinLen = 2.4f * s;
    g2.drawLine((int) (x - pinLen), (int) (y + h * 0.30f), (int) x, (int) (y + h * 0.30f));
    g2.drawLine((int) (x - pinLen), (int) (y + h * 0.70f), (int) x, (int) (y + h * 0.70f));
    g2.drawLine((int) (x + w), (int) (y + h * 0.30f), (int) (x + w + pinLen), (int) (y + h * 0.30f));
    g2.drawLine((int) (x + w), (int) (y + h * 0.70f), (int) (x + w + pinLen), (int) (y + h * 0.70f));

    // Diagonal edit pencil colors
    Color pencilTop = isDark ? new Color(0xFF, 0xC0, 0x48) : new Color(0xF3, 0x9C, 0x12);
    Color pencilBot = isDark ? new Color(0xFF, 0x9F, 0x1A) : new Color(0xC2, 0x5A, 0x00);
    Color tipLead = isDark ? new Color(0x30, 0x33, 0x38) : new Color(0x40, 0x43, 0x48);
    Color eraser = isDark ? new Color(0xFF, 0x6B, 0x8B) : new Color(0xE0, 0x56, 0x70);

    if (!isEnabled) {
      pencilTop = toDisabled(pencilTop);
      pencilBot = toDisabled(pencilBot);
      tipLead = toDisabled(tipLead);
      eraser = toDisabled(eraser);
    }

    // Pencil geometry across diagonal (from tip at bottom-left to eraser at top-right)
    // 1. Shaft body (polygon with thickness)
    Path2D.Float shaft = new Path2D.Float();
    shaft.moveTo(7.2f * s, 11.2f * s);
    shaft.lineTo(12.2f * s, 6.2f * s);
    shaft.lineTo(13.8f * s, 7.8f * s);
    shaft.lineTo(8.8f * s, 12.8f * s);
    shaft.closePath();

    g2.setPaint(new GradientPaint(7.0f * s, 13.0f * s, pencilBot, 14.0f * s, 6.0f * s, pencilTop));
    g2.fill(shaft);

    // 2. Triangular sharpened wood tip
    Path2D.Float woodTip = new Path2D.Float();
    woodTip.moveTo(6.0f * s, 14.0f * s);
    woodTip.lineTo(7.2f * s, 11.2f * s);
    woodTip.lineTo(8.8f * s, 12.8f * s);
    woodTip.closePath();
    g2.fill(woodTip);

    // 3. Graphite lead point at tip
    Path2D.Float lead = new Path2D.Float();
    lead.moveTo(6.0f * s, 14.0f * s);
    lead.lineTo(6.5f * s, 12.8f * s);
    lead.lineTo(7.2f * s, 13.5f * s);
    lead.closePath();
    g2.setColor(tipLead);
    g2.fill(lead);

    // 4. Eraser cap at upper-right
    Path2D.Float eraserCap = new Path2D.Float();
    eraserCap.moveTo(12.2f * s, 6.2f * s);
    eraserCap.lineTo(13.3f * s, 5.1f * s);
    eraserCap.lineTo(14.9f * s, 6.7f * s);
    eraserCap.lineTo(13.8f * s, 7.8f * s);
    eraserCap.closePath();
    g2.setColor(eraser);
    g2.fill(eraserCap);

    // Crisp outline on pencil
    Path2D.Float pencilOutline = new Path2D.Float();
    pencilOutline.moveTo(6.0f * s, 14.0f * s);
    pencilOutline.lineTo(7.2f * s, 11.2f * s);
    pencilOutline.lineTo(13.3f * s, 5.1f * s);
    pencilOutline.lineTo(14.9f * s, 6.7f * s);
    pencilOutline.lineTo(8.8f * s, 12.8f * s);
    pencilOutline.closePath();

    g2.setColor(isDark ? new Color(0x3B, 0x2A, 0x14) : new Color(0x7D, 0x38, 0x00));
    if (!isEnabled) {
      g2.setColor(toDisabled(g2.getColor()));
    }
    g2.setStroke(createStroke(0.9f * s));
    g2.draw(pencilOutline);
  }
}
