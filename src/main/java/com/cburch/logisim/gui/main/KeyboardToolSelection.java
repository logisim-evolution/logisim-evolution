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

package com.cburch.logisim.gui.main;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.draw.toolbar.ToolbarModel;

public class KeyboardToolSelection extends AbstractAction {
	public static void register(Toolbar toolbar) {
		ActionMap amap = toolbar.getActionMap();
		InputMap imap = toolbar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		int mask = toolbar.getToolkit().getMenuShortcutKeyMask();
		for (int i = 0; i < 10; i++) {
			KeyStroke keyStroke = KeyStroke
					.getKeyStroke((char) ('0' + i), mask);
			int j = (i == 0 ? 10 : i - 1);
			KeyboardToolSelection action = new KeyboardToolSelection(toolbar, j);
			String key = "ToolSelect" + i;
			amap.put(key, action);
			imap.put(keyStroke, key);
		}
	}

	private static final long serialVersionUID = 1L;

	private Toolbar toolbar;
	private int index;

	public KeyboardToolSelection(Toolbar toolbar, int index) {
		this.toolbar = toolbar;
		this.index = index;
	}

	public void actionPerformed(ActionEvent event) {
		ToolbarModel model = toolbar.getToolbarModel();
		int i = -1;
		for (ToolbarItem item : model.getItems()) {
			if (item.isSelectable()) {
				i++;
				if (i == index) {
					model.itemSelected(item);
				}
			}
		}
	}
}
