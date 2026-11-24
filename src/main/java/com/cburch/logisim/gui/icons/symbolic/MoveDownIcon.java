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
 * Modern vector Move Down Arrow icon (bold chevron head pointing down, shaft ending in a horizontal base bar).
 */
public class MoveDownIcon extends VectorBaseIcon {

  public MoveDownIcon() {
    this(20);
  }

  public MoveDownIcon(int baseSize) {
    super(baseSize);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size;
    g2.setStroke(createStroke(1.95f));

    boolean isEnabled = (c == null || c.isEnabled());
    Color moveColor = getRoleColor("Action.move", new Color(0x1C, 0x71, 0xD8), new Color(0x58, 0xA6, 0xFF), isEnabled);
    g2.setColor(moveColor);

    float midX = s * 0.50f;
    float topY = s * 0.20f;
    float botY = s * 0.82f;
    float spreadX = s * 0.28f;
    float chevY = s * 0.56f;

    // Base bar at top
    float barHalf = s * 0.22f;
    g2.draw(new Line2D.Float(midX - barHalf, topY, midX + barHalf, topY));

    // Arrow shaft from base down to head
    g2.draw(new Line2D.Float(midX, topY, midX, botY));

    // Chevron head pointing down
    Path2D.Float chevron = new Path2D.Float();
    chevron.moveTo(midX - spreadX, chevY);
    chevron.lineTo(midX, botY);
    chevron.lineTo(midX + spreadX, chevY);
    g2.draw(chevron);
  }
}
