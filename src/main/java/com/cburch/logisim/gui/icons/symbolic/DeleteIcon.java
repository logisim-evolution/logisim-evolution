/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * Vector Delete icon: trash-can outline with lid, tapered bin, and vertical ribs.
 */
public class DeleteIcon extends VectorBaseIcon {

  public DeleteIcon() {
    this(20);
  }

  public DeleteIcon(int baseSize) {
    super(baseSize);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size;
    g2.setStroke(createStroke(1.75f));

    boolean isEnabled = (c == null || c.isEnabled());
    Color destroyColor = getRoleColor("Action.destroy", new Color(0xC0, 0x1C, 0x28), new Color(0xFF, 0x6B, 0x6B), isEnabled);
    g2.setColor(destroyColor);

    // Lid handle
    float hX1 = s * 0.40f;
    float hX2 = s * 0.60f;
    float hY = s * 0.16f;
    float lidY = s * 0.26f;
    Path2D.Float handle = new Path2D.Float();
    handle.moveTo(hX1, lidY);
    handle.lineTo(hX1, hY);
    handle.lineTo(hX2, hY);
    handle.lineTo(hX2, lidY);
    g2.draw(handle);

    // Lid horizontal bar
    float lidLeft = s * 0.20f;
    float lidRight = s * 0.80f;
    g2.draw(new Line2D.Float(lidLeft, lidY, lidRight, lidY));

    // Bin body (tapered)
    float binTopY = lidY + s * 0.06f;
    float binBotY = s * 0.84f;
    float binTopLeft = s * 0.26f;
    float binTopRight = s * 0.74f;
    float binBotLeft = s * 0.32f;
    float binBotRight = s * 0.68f;

    Path2D.Float bin = new Path2D.Float();
    bin.moveTo(binTopLeft, binTopY);
    bin.lineTo(binBotLeft, binBotY);
    bin.quadTo(s * 0.50f, binBotY + s * 0.04f, binBotRight, binBotY);
    bin.lineTo(binTopRight, binTopY);
    g2.draw(bin);

    // Two vertical ribs inside bin
    g2.draw(new Line2D.Float(s * 0.42f, binTopY + s * 0.08f, s * 0.43f, binBotY - s * 0.06f));
    g2.draw(new Line2D.Float(s * 0.58f, binTopY + s * 0.08f, s * 0.57f, binBotY - s * 0.06f));
  }
}
