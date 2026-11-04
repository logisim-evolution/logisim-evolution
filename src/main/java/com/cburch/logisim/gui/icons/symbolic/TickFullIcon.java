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
 * Modern vector Full-Tick icon faithfully matching classic Logisim semantics and colors:
 * - Circular clock face with white dial background and dark bezel.
 * - Bold red hand (pointing up-left) and dark hand (pointing up-right).
 * - Full-Tick features BOTH wide, clearly visible arrowheads:
 *   1) Purple arc with an arrowhead pointing straight UP at top-right.
 *   2) Magenta arc with an arrowhead pointing LEFT at bottom-left.
 */
public class TickFullIcon extends VectorBaseIcon {

  public static final Color MAGENTA_ARROW = new Color(0xFF, 0x00, 0xCC); // Classic Logisim Magenta
  public static final Color PURPLE_ARROW  = new Color(0x66, 0x00, 0x99); // Classic Logisim Dark Purple/Violet
  public static final Color RED_HAND      = new Color(0xEE, 0x11, 0x11); // Classic Red Clock Hand

  public TickFullIcon() {
    super();
  }

  public TickFullIcon(int baseSize) {
    super(baseSize);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size;
    boolean isEnabled = (c == null || c.isEnabled());
    Color magCol  = isEnabled ? MAGENTA_ARROW : toDisabled(MAGENTA_ARROW);
    Color purpCol = isEnabled ? PURPLE_ARROW  : toDisabled(PURPLE_ARROW);
    Color redCol  = isEnabled ? RED_HAND      : toDisabled(RED_HAND);

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

    float arcR = r; // hugs perimeter

    // 3. Arc 1: Top-left Purple Arc (from -135° sweeping -180° to top-right)
    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    g2.setColor(purpCol);
    Arc2D.Float topArc = new Arc2D.Float(cx - arcR, cy - arcR, arcR * 2f, arcR * 2f, -135, -180, Arc2D.OPEN);
    g2.draw(topArc);

    // Arrowhead 1 at top-right pointing upwards (bold and clearly visible)
    float topTipX = cx + arcR * 0.707f;
    float topTipY = cy - arcR * 0.707f;
    Path2D.Float topHead = new Path2D.Float();
    topHead.moveTo(topTipX + s * 0.06f, topTipY - s * 0.20f); // sharp point pointing up
    topHead.lineTo(topTipX - s * 0.18f, topTipY + s * 0.08f); // left barb
    topHead.lineTo(topTipX + s * 0.20f, topTipY + s * 0.08f); // right barb
    topHead.closePath();
    g2.fill(topHead);

    // 4. Arc 2: Bottom-right Magenta Arc
    g2.setColor(magCol);
    Arc2D.Float botArc = new Arc2D.Float(cx - arcR, cy - arcR, arcR * 2f, arcR * 2f, 45, -180, Arc2D.OPEN);
    g2.draw(botArc);

    // Arrowhead 2 at bottom-left pointing left (bold and clearly visible)
    float botTipX = cx - arcR * 0.707f;
    float botTipY = cy + arcR * 0.707f;
    Path2D.Float botHead = new Path2D.Float();
    botHead.moveTo(botTipX - s * 0.12f, botTipY);               // sharp point extending left
    botHead.lineTo(botTipX + s * 0.22f, botTipY - s * 0.08f);      // upper barb
    botHead.lineTo(botTipX + s * 0.08f, botTipY + s * 0.26f);      // lower barb extending downwards
    botHead.closePath();
    g2.fill(botHead);
  }
}
