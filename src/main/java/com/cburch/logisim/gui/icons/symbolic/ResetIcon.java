package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern vector Reset icon: Rewind to Start (|◀).
 * Visually mirrors StepIcon (▶|) and matches PlayIcon (▶).
 */
public class ResetIcon extends SymbolicIcon {

  public ResetIcon() {
    super();
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    float s = size;

    // Left vertical bar (start barrier at t=0)
    float barX = s * 0.18f;
    float barW = s * 0.14f;
    float barH = s * 0.52f;
    float barY = s * 0.24f;
    g2.fill(new RoundRectangle2D.Float(barX, barY, barW, barH, barW, barW));

    // Rewind triangle pointing left
    Path2D.Float triangle = new Path2D.Float();
    float tipX = s * 0.40f;
    float right = s * 0.78f;
    float top = s * 0.24f;
    float bottom = s * 0.76f;
    float midY = s * 0.50f;

    triangle.moveTo(right, top);
    triangle.lineTo(tipX, midY);
    triangle.lineTo(right, bottom);
    triangle.closePath();
    g2.fill(triangle);
  }
}
