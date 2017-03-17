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

package com.cburch.logisim.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;

public class MenuTool extends Tool {
	private class MenuComponent extends JPopupMenu implements ActionListener {
		private static final long serialVersionUID = 1L;
		Project proj;
		Circuit circ;
		Component comp;
		JMenuItem del = new JMenuItem(Strings.get("compDeleteItem"));
		JMenuItem attrs = new JMenuItem(Strings.get("compShowAttrItem"));
		JMenuItem rotate = new JMenuItem(Strings.get("compRotate"));

		MenuComponent(Project proj, Circuit circ, Component comp) {
			this.proj = proj;
			this.circ = circ;
			this.comp = comp;
			boolean canChange = proj.getLogisimFile().contains(circ);

			if (comp.getAttributeSet().containsAttribute(StdAttr.FACING)) {
				add(rotate);
				rotate.addActionListener(this);
			}

			add(del);
			del.addActionListener(this);
			del.setEnabled(canChange);
			add(attrs);
			attrs.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if (src == del) {
				Circuit circ = proj.getCurrentCircuit();
				CircuitMutation xn = new CircuitMutation(circ);
				xn.remove(comp);
				proj.doAction(xn.toAction(Strings.getter(
						"removeComponentAction", comp.getFactory()
								.getDisplayGetter())));
			} else if (src == attrs) {
				proj.getFrame().viewComponentAttributes(circ, comp);
			} else if (src == rotate) {
				Circuit circ = proj.getCurrentCircuit();
				CircuitMutation xn = new CircuitMutation(circ);
				Direction d = comp.getAttributeSet().getValue(StdAttr.FACING);
				xn.set(comp, StdAttr.FACING, d.getRight());
				proj.doAction(xn.toAction(Strings.getter(
						"rotateComponentAction", comp.getFactory()
								.getDisplayGetter())));
			}
		}
	}

	private class MenuSelection extends JPopupMenu implements ActionListener {
		private static final long serialVersionUID = 1L;
		Project proj;
		JMenuItem del = new JMenuItem(Strings.get("selDeleteItem"));
		JMenuItem cut = new JMenuItem(Strings.get("selCutItem"));
		JMenuItem copy = new JMenuItem(Strings.get("selCopyItem"));

		MenuSelection(Project proj) {
			this.proj = proj;
			boolean canChange = proj.getLogisimFile().contains(
					proj.getCurrentCircuit());
			add(del);
			del.addActionListener(this);
			del.setEnabled(canChange);
			add(cut);
			cut.addActionListener(this);
			cut.setEnabled(canChange);
			add(copy);
			copy.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			Selection sel = proj.getSelection();
			if (src == del) {
				proj.doAction(SelectionActions.clear(sel));
			} else if (src == cut) {
				proj.doAction(SelectionActions.cut(sel));
			} else if (src == copy) {
				proj.doAction(SelectionActions.copy(sel));
			}
		}

		/*
		 * public void show(JComponent parent, int x, int y) { super.show(this,
		 * x, y); }
		 */
	}

	public MenuTool() {
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MenuTool;
	}

	@Override
	public String getDescription() {
		return Strings.get("menuToolDesc");
	}

	@Override
	public String getDisplayName() {
		return Strings.get("menuTool");
	}

	@Override
	public String getName() {
		return "Menu Tool";
	}

	@Override
	public int hashCode() {
		return MenuTool.class.hashCode();
	}

	@Override
	public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		Location pt = Location.create(x, y);

		JPopupMenu menu;
		Project proj = canvas.getProject();
		Selection sel = proj.getSelection();
		Collection<Component> in_sel = sel.getComponentsContaining(pt, g);
		if (!in_sel.isEmpty()) {
			Component comp = in_sel.iterator().next();
			if (sel.getComponents().size() > 1) {
				menu = new MenuSelection(proj);
			} else {
				menu = new MenuComponent(proj, canvas.getCircuit(), comp);
				MenuExtender extender = (MenuExtender) comp
						.getFeature(MenuExtender.class);
				if (extender != null)
					extender.configureMenu(menu, proj);
			}
		} else {
			Collection<Component> cl = canvas.getCircuit().getAllContaining(pt,
					g);
			if (!cl.isEmpty()) {
				Component comp = cl.iterator().next();
				menu = new MenuComponent(proj, canvas.getCircuit(), comp);
				MenuExtender extender = (MenuExtender) comp
						.getFeature(MenuExtender.class);
				if (extender != null)
					extender.configureMenu(menu, proj);
			} else {
				menu = null;
			}
		}

		if (menu != null) {
			canvas.showPopupMenu(menu, x, y);
		}
	}

	@Override
	public void paintIcon(ComponentDrawContext c, int x, int y) {
		Graphics g = c.getGraphics();
		g.fillRect(x + 2, y + 1, 9, 2);
		g.drawRect(x + 2, y + 3, 15, 12);
		g.setColor(Color.lightGray);
		g.drawLine(x + 4, y + 2, x + 8, y + 2);
		for (int y_offs = y + 6; y_offs < y + 15; y_offs += 3) {
			g.drawLine(x + 4, y_offs, x + 14, y_offs);
		}
	}
}
