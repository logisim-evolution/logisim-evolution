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

package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class VerticalSplitPane extends JPanel {
	private class MyDragbar extends HorizontalSplitPane.Dragbar {
		private static final long serialVersionUID = 1L;

		MyDragbar() {
			setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
		}

		@Override
		int getDragValue(MouseEvent e) {
			return getX() + e.getX() - VerticalSplitPane.this.getInsets().left;
		}

		@Override
		void setDragValue(int value) {
			Insets in = VerticalSplitPane.this.getInsets();
			setFraction((double) value
					/ (VerticalSplitPane.this.getWidth() - in.left - in.right));
			revalidate();
		}
	}

	private class MyLayout implements LayoutManager {
		public void addLayoutComponent(String name, Component comp) {
		}

		public void layoutContainer(Container parent) {
			Insets in = parent.getInsets();
			int maxWidth = parent.getWidth() - (in.left + in.right);
			int maxHeight = parent.getHeight() - (in.top + in.bottom);
			int split;
			if (fraction <= 0.0) {
				split = 0;
			} else if (fraction >= 1.0) {
				split = maxWidth;
			} else {
				split = (int) Math.round(maxWidth * fraction);
				split = Math
						.min(split, maxWidth - comp1.getMinimumSize().width);
				split = Math.max(split, comp0.getMinimumSize().width);
			}

			comp0.setBounds(in.left, in.top, split, maxHeight);
			comp1.setBounds(in.left + split, in.top, maxWidth - split,
					maxHeight);
			dragbar.setBounds(in.left + split
					- HorizontalSplitPane.DRAG_TOLERANCE, in.top,
					2 * HorizontalSplitPane.DRAG_TOLERANCE, maxHeight);
		}

		public Dimension minimumLayoutSize(Container parent) {
			if (fraction <= 0.0)
				return comp1.getMinimumSize();
			if (fraction >= 1.0)
				return comp0.getMinimumSize();
			Insets in = parent.getInsets();
			Dimension d0 = comp0.getMinimumSize();
			Dimension d1 = comp1.getMinimumSize();
			return new Dimension(in.left + d0.width + d1.width + in.right,
					in.top + Math.max(d0.height, d1.height) + in.bottom);
		}

		public Dimension preferredLayoutSize(Container parent) {
			if (fraction <= 0.0)
				return comp1.getPreferredSize();
			if (fraction >= 1.0)
				return comp0.getPreferredSize();
			Insets in = parent.getInsets();
			Dimension d0 = comp0.getPreferredSize();
			Dimension d1 = comp1.getPreferredSize();
			return new Dimension(in.left + d0.width + d1.width + in.right,
					in.top + Math.max(d0.height, d1.height) + in.bottom);
		}

		public void removeLayoutComponent(Component comp) {
		}
	}

	private static final long serialVersionUID = 1L;

	private JComponent comp0;
	private JComponent comp1;
	private MyDragbar dragbar;
	private double fraction;

	public VerticalSplitPane(JComponent comp0, JComponent comp1) {
		this(comp0, comp1, 0.5);
	}

	public VerticalSplitPane(JComponent comp0, JComponent comp1, double fraction) {
		this.comp0 = comp0;
		this.comp1 = comp1;
		this.dragbar = new MyDragbar(); // above the other components
		this.fraction = fraction;

		setLayout(new MyLayout());
		add(dragbar); // above the other components
		add(comp0);
		add(comp1);
	}

	public double getFraction() {
		return fraction;
	}

	public void setFraction(double value) {
		if (value < 0.0)
			value = 0.0;
		if (value > 1.0)
			value = 1.0;
		if (fraction != value) {
			fraction = value;
			revalidate();
		}
	}
}
