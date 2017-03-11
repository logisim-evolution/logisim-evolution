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

package com.cburch.logisim.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;


class MenuItemHelper implements ActionListener {
	private JMenuItem source;
	private LogisimMenuItem menuItem;
	private Menu menu;
	private boolean enabled;
	private boolean inActionListener;
	private ArrayList<ActionListener> listeners;

	public MenuItemHelper(JMenuItem source, Menu menu, LogisimMenuItem menuItem) {
		this.source = source;
		this.menu = menu;
		this.menuItem = menuItem;
		this.enabled = true;
		this.inActionListener = false;
		this.listeners = new ArrayList<ActionListener>();
	}

	public void actionPerformed(ActionEvent event) {
		if (!listeners.isEmpty()) {
			ActionEvent e = new ActionEvent(menuItem, event.getID(),
					event.getActionCommand(), event.getWhen(),
					event.getModifiers());
			for (ActionListener l : listeners) {
				l.actionPerformed(e);
			}
		}
	}

	public void addActionListener(ActionListener l) {
		listeners.add(l);
		computeEnabled();
	}

	private void computeEnabled() {
		inActionListener = true;
		try {
			source.setEnabled(enabled);
			menu.computeEnabled();
		} finally {
			inActionListener = false;
		}
	}

	public boolean hasListeners() {
		return !listeners.isEmpty();
	}

	public void removeActionListener(ActionListener l) {
		listeners.remove(l);
		computeEnabled();
	}

	public void setEnabled(boolean value) {
		if (!inActionListener) {
			enabled = value;
		}
	}
}
