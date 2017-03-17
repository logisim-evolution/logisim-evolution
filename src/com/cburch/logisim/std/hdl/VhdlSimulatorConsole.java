/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/
package com.cburch.logisim.std.hdl;

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

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.SmartScroller;

public class VhdlSimulatorConsole extends JPanel {

	private class VhdlSimState extends JPanel implements VhdlSimulatorListener {

		private static final long serialVersionUID = 1L;
		Ellipse2D.Double circle;
		Color color;
		private int margin = 5;

		public VhdlSimState() {
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

			switch (project.getVhdlSimulator().getState()) {
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
	private JLabel label = new JLabel();
	private JScrollPane log = new JScrollPane();
	private JTextArea logContent = new JTextArea();
	private VhdlSimState vhdlSimState;

	private Project project;

	public VhdlSimulatorConsole(Project proj) {
		project = proj;

		show();
	}

	public void append(String s) {
		logContent.append(s);
	}

	public void clear() {
		logContent.setText("");
	}

	public void show() {
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
}
