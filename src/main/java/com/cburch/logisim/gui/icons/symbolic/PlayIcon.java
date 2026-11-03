package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * Modern vector Play icon with smooth vertices.
 */
public class PlayIcon extends SymbolicIcon {

  public PlayIcon() {
    super();
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    float s = size;
    Path2D.Float triangle = new Path2D.Float();
    float left = s * 0.30f;
    float right = s * 0.78f;
    float top = s * 0.22f;
    float bottom = s * 0.78f;
    float midY = s * 0.50f;

    triangle.moveTo(left, top);
    triangle.lineTo(right, midY);
    triangle.lineTo(left, bottom);
    triangle.closePath();

    g2.fill(triangle);
  }
}
