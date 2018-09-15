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

package com.cburch.logisim.gui.generic;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class CardPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private ArrayList<ChangeListener> listeners;
	private String current;

	public CardPanel() {
		super(new CardLayout());
		listeners = new ArrayList<ChangeListener>();
		current = "";
	}

	public void addChangeListener(ChangeListener listener) {
		listeners.add(listener);
	}

	public void addView(String name, Component comp) {
		add(comp, name);
	}

	public String getView() {
		return current;
	}

	public void setView(String choice) {
		if (choice == null)
			choice = "";
		String oldChoice = current;
		if (!oldChoice.equals(choice)) {
			current = choice;
			((CardLayout) getLayout()).show(this, choice);
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener listener : listeners) {
				listener.stateChanged(e);
			}
		}
	}

}
