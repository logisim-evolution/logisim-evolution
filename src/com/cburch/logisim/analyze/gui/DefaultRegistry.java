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

package com.cburch.logisim.analyze.gui;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRootPane;


class DefaultRegistry {
	private class MyListener implements FocusListener {
		JButton defaultButton;

		MyListener(JButton defaultButton) {
			this.defaultButton = defaultButton;
		}

		public void focusGained(FocusEvent event) {
			rootPane.setDefaultButton(defaultButton);
		}

		public void focusLost(FocusEvent event) {
			JButton currentDefault = (JButton)rootPane.getDefaultButton();
			if (currentDefault == defaultButton)
				rootPane.setDefaultButton(null);
		}
	}

	private JRootPane rootPane;

	public DefaultRegistry(JRootPane rootPane) {
		this.rootPane = rootPane;
		rootPane.setDefaultButton(null);
	}

	public void registerDefaultButton(JComponent comp, JButton button) {
		comp.addFocusListener(new MyListener(button));
	}
}
