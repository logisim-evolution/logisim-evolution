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
import com.cburch.logisim.util.SmartScroller;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorListener;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorTop;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

public class VhdlSimulatorConsole extends JPanel {

  private class VhdlSimState extends JPanel implements VhdlSimulatorListener {

    private static final long serialVersionUID = 1L;
    final Ellipse2D.Double circle;
    Color color;
    private final int margin = 5;

    public VhdlSimState() {
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
      VhdlSimulatorTop vsim = project.getVhdlSimulator();
      switch (vsim.getState()) {
        case DISABLED:
          color = Color.GRAY;
          break;

        case ENABLED:
          color = Color.RED;
          break;

        case STARTING:
          color = Color.ORANGE;
          break;

        case RUNNING:
          color = new Color(40, 180, 40);
          break;
      }

      this.repaint();
    }
  }

  private static final long serialVersionUID = 1L;
  private final JLabel label = new JLabel();
  private final JScrollPane log = new JScrollPane();
  private final JTextArea logContent = new JTextArea();
  private final VhdlSimState vhdlSimState;

  private final Project project;

  public VhdlSimulatorConsole(Project proj) {
    project = proj;

    this.setLayout(new BorderLayout());

    /* Add title */
    label.setText("VHDL simulator log");
    this.add(label, BorderLayout.PAGE_START);

    /* Add console log */
    logContent.setEditable(false);
    log.setViewportView(logContent);
    new SmartScroller(log);
    this.add(log, BorderLayout.CENTER);

    /* Add Simulator state indicator */
    vhdlSimState = new VhdlSimState();
    vhdlSimState.stateChanged();

    project.getVhdlSimulator().addVhdlSimStateListener(vhdlSimState);

    this.add(vhdlSimState, BorderLayout.PAGE_END);
  }

  public void append(String s) {
    logContent.append(s);
  }

  public void clear() {
    logContent.setText("");
  }
}
