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

package com.cburch.draw.toolbar;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class Toolbar extends JPanel {
	private class MyListener implements ToolbarModelListener {
		public void toolbarAppearanceChanged(ToolbarModelEvent event) {
			repaint();
		}

		public void toolbarContentsChanged(ToolbarModelEvent event) {
			computeContents();
		}
	}

	private static final long serialVersionUID = 1L;
	public static final Object VERTICAL = new Object();

	public static final Object HORIZONTAL = new Object();

	private ToolbarModel model;
	private JPanel subpanel;
	private Object orientation;
	private MyListener myListener;
	private ToolbarButton curPressed;

	public Toolbar(ToolbarModel model) {
		super(new BorderLayout());
		this.subpanel = new JPanel();
		this.model = model;
		this.orientation = HORIZONTAL;
		this.myListener = new MyListener();
		this.curPressed = null;

		this.add(new JPanel(), BorderLayout.CENTER);
		setOrientation(HORIZONTAL);

		computeContents();
		if (model != null)
			model.addToolbarModelListener(myListener);
	}

	private void computeContents() {
		subpanel.removeAll();
		ToolbarModel m = model;
		if (m != null) {
			for (ToolbarItem item : m.getItems()) {
				subpanel.add(new ToolbarButton(this, item));
			}
			subpanel.add(Box.createGlue());
		}
		revalidate();
	}

	Object getOrientation() {
		return orientation;
	}

	ToolbarButton getPressed() {
		return curPressed;
	}

	public ToolbarModel getToolbarModel() {
		return model;
	}

	public void setOrientation(Object value) {
		int axis;
		String position;
		if (value.equals(HORIZONTAL)) {
			axis = BoxLayout.X_AXIS;
			position = BorderLayout.LINE_START;
		} else if (value.equals(VERTICAL)) {
			axis = BoxLayout.Y_AXIS;
			position = BorderLayout.NORTH;
		} else {
			throw new IllegalArgumentException();
		}
		this.remove(subpanel);
		subpanel.setLayout(new BoxLayout(subpanel, axis));
		this.add(subpanel, position);
		this.orientation = value;
	}

	void setPressed(ToolbarButton value) {
		ToolbarButton oldValue = curPressed;
		if (oldValue != value) {
			curPressed = value;
			if (oldValue != null)
				oldValue.repaint();
			if (value != null)
				value.repaint();
		}
	}

	public void setToolbarModel(ToolbarModel value) {
		ToolbarModel oldValue = model;
		if (value != oldValue) {
			if (oldValue != null)
				oldValue.removeToolbarModelListener(myListener);
			if (value != null)
				value.addToolbarModelListener(myListener);
			model = value;
			computeContents();
		}
	}
}
