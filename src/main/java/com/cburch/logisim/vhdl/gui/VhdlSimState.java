/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.gui;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class VhdlSimState extends JPanel implements VhdlSimulatorListener {

  private static final long serialVersionUID = 1L;
  final Ellipse2D.Double circle;
  Color color;
  private final int margin = 5;
  private final Project proj;

  public VhdlSimState(Project proj) {
    this.proj = proj;
    int radius = 15;
    circle = new Ellipse2D.Double(margin, margin, radius, radius);
    setOpaque(false);
    color = Color.GRAY;
    this.setBorder(new EmptyBorder(margin, margin, margin, margin));
  }

  @Override
  public Dimension getPreferredSize() {
    Rectangle bounds = circle.getBounds();
    return new Dimension(bounds.width + 2 * margin, bounds.height + 2 * margin);
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(color);
    g2.fill(circle);
  }

  @Override
  public void stateChanged() {
    switch (proj.getVhdlSimulator().getState()) {
      case DISABLED -> color = Color.GRAY;
      case ENABLED -> color = Color.RED;
      case STARTING -> color = Color.ORANGE;
      case RUNNING -> color = new Color(40, 180, 40);
    }
    this.repaint();
  }
}
