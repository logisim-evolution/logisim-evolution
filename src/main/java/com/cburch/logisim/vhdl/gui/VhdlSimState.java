package com.cburch.logisim.vhdl.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorListener;

public class VhdlSimState  extends JPanel implements VhdlSimulatorListener {


	private static final long serialVersionUID = 1L;
	Ellipse2D.Double circle;
	Color color;
	private int margin = 5;
	private Project proj;

	public VhdlSimState(Project proj) {
		this.proj = proj;
		int radius = 15;
		circle = new Ellipse2D.Double(margin, margin, radius, radius);
		setOpaque(false);
		color = Color.GRAY;
		this.setBorder(new EmptyBorder(margin, margin, margin, margin));
	}

	public Dimension getPreferredSize() {
		Rectangle bounds = circle.getBounds();
		return new Dimension(bounds.width + 2 * margin, bounds.height + 2
				* margin);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(color);
		g2.fill(circle);
	}

	@Override
	public void stateChanged() {
		switch (proj.getVhdlSimulator().getState()) {
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
