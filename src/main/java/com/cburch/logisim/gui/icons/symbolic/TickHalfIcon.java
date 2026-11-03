package com.cburch.logisim.gui.icons.symbolic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * Modern vector Half-Tick icon faithfully matching classic Logisim semantics and colors:
 * - Circular clock face with white dial background and dark bezel.
 * - Bold red hand (pointing up-left) and dark hand (pointing up-right).
 * - Vibrant Magenta arc along the right & bottom perimeter terminating in a solid,
 *   prominent arrowhead at the bottom-left pointing left.
 */
public class TickHalfIcon extends SymbolicIcon {

  public static final Color MAGENTA_ARROW = new Color(0xFF, 0x00, 0xCC); // Classic vibrant Logisim Magenta
  public static final Color RED_HAND      = new Color(0xEE, 0x11, 0x11); // Classic Red Clock Hand

  public TickHalfIcon() {
    super();
  }

  public TickHalfIcon(int baseSize) {
    super(baseSize);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size;
    boolean isEnabled = (c == null || c.isEnabled());
    Color arrowCol = isEnabled ? MAGENTA_ARROW : toDisabled(MAGENTA_ARROW);
    Color redCol   = isEnabled ? RED_HAND : toDisabled(RED_HAND);

    float cx = s * 0.48f;
    float cy = s * 0.46f;
    float r  = s * 0.36f;

    // 1. Clock Background & Border
    g2.setColor(c != null && !isEnabled ? new Color(240, 240, 240, 160) : Color.WHITE);
    g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2f, r * 2f));

    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.setColor(isEnabled ? new Color(0x44, 0x44, 0x44) : toDisabled(new Color(0x44, 0x44, 0x44)));
    g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2f, r * 2f));

    // 2. Clock Hands (Thick, bold V-shape)
    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    // Up-left Red Hand
    g2.setColor(redCol);
    g2.draw(new Line2D.Float(cx, cy, cx - r * 0.55f, cy - r * 0.55f));

    // Up-right Black/Dark Hand
    g2.setColor(isEnabled ? new Color(0x18, 0x18, 0x18) : toDisabled(new Color(0x18, 0x18, 0x18)));
    g2.draw(new Line2D.Float(cx, cy, cx + r * 0.55f, cy - r * 0.55f));

    // Center pivot
    g2.setColor(new Color(0x22, 0x22, 0x22));
    g2.fill(new Ellipse2D.Float(cx - 1.2f, cy - 1.2f, 2.4f, 2.4f));

    // 3. Half-Tick Magenta Arc (starts at top-right 45°, sweeps clockwise 180° to bottom-left)
    float arcR = r; // hugs the perimeter
    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    g2.setColor(arrowCol);
    Arc2D.Float arc = new Arc2D.Float(cx - arcR, cy - arcR, arcR * 2f, arcR * 2f, 45, -180, Arc2D.OPEN);
    g2.draw(arc);

    // 4. Prominent, wide arrowhead at bottom-left pointing left
    float tipX = cx - arcR * 0.707f;
    float tipY = cy + arcR * 0.707f;

    Path2D.Float arrowHead = new Path2D.Float();
    arrowHead.moveTo(tipX - s * 0.12f, tipY);                // sharp tip extending well to the left
    arrowHead.lineTo(tipX + s * 0.22f, tipY - s * 0.08f);       // upper barb (inside arc)
    arrowHead.lineTo(tipX + s * 0.08f, tipY + s * 0.26f);       // wide lower barb extending downwards
    arrowHead.closePath();
    g2.fill(arrowHead);
  }
}
