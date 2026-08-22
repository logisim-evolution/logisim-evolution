package com.cburch.logisim.gui.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class TwoWaySwitchIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    // White background inside rounded box
    g2.setColor(Color.WHITE);
    g2.fillRoundRect(scale(2), scale(2), scale(12), scale(11), scale(3), scale(3));

    // Black outer border
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke((float) scale(1.0)));
    g2.drawRoundRect(scale(2), scale(2), scale(12), scale(11), scale(3), scale(3));

    // Left grey port dot
    g2.setColor(Color.GRAY);
    g2.fillOval(scale(0), scale(7), scale(2), scale(2));

    // Right blue port dots
    g2.setColor(new Color(30, 64, 255));
    g2.fillOval(scale(14), scale(4), scale(2), scale(2));
    g2.fillOval(scale(14), scale(10), scale(2), scale(2));

    // 3 Black internal contact points
    g2.setColor(Color.BLACK);
    g2.fillOval(scale(4), scale(7), scale(2), scale(2));   // Left contact
    g2.fillOval(scale(10), scale(4), scale(2), scale(2));  // Top-right contact
    g2.fillOval(scale(10), scale(10), scale(2), scale(2)); // Bottom-right contact

    // Black switch lever
    g2.setStroke(new BasicStroke((float) scale(1.5), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.drawLine(scale(4), scale(8), scale(10), scale(5));
  }
}
