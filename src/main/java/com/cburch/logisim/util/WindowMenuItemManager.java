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

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JRadioButtonMenuItem;

public abstract class WindowMenuItemManager {
	private class MyListener implements WindowListener {
		public void windowActivated(WindowEvent event) {
			addToManager();
			WindowMenuManager.setCurrentManager(WindowMenuItemManager.this);
		}

		public void windowClosed(WindowEvent event) {
			removeFromManager();
		}

		public void windowClosing(WindowEvent event) {
			JFrame frame = getJFrame(false);
			if (frame.getDefaultCloseOperation() == JFrame.HIDE_ON_CLOSE) {
				removeFromManager();
			}
		}

		public void windowDeactivated(WindowEvent event) {
			WindowMenuManager.unsetCurrentManager(WindowMenuItemManager.this);
		}

		public void windowDeiconified(WindowEvent event) {
		}

		public void windowIconified(WindowEvent event) {
			addToManager();
			WindowMenuManager.setCurrentManager(WindowMenuItemManager.this);
		}

		public void windowOpened(WindowEvent event) {
		}
	}

	private MyListener myListener = new MyListener();
	private String text;
	private boolean persistent;
	private boolean listenerAdded = false;
	private boolean inManager = false;
	private HashMap<WindowMenu, JRadioButtonMenuItem> menuItems = new HashMap<WindowMenu, JRadioButtonMenuItem>();

	public WindowMenuItemManager(String text, boolean persistent) {
		this.text = text;
		this.persistent = persistent;
		if (persistent) {
			WindowMenuManager.addManager(this);
		}
	}

	private void addToManager() {
		if (!persistent && !inManager) {
			WindowMenuManager.addManager(this);
			inManager = true;
		}
	}

	void createMenuItem(WindowMenu menu) {
		WindowMenuItem ret = new WindowMenuItem(this);
		menuItems.put(menu, ret);
		menu.addMenuItem(this, ret, persistent);
	}

	public void frameClosed(JFrame frame) {
		if (!persistent) {
			if (listenerAdded) {
				frame.removeWindowListener(myListener);
				listenerAdded = false;
			}
			removeFromManager();
		}
	}

	public void frameOpened(JFrame frame) {
		if (!listenerAdded) {
			frame.addWindowListener(myListener);
			listenerAdded = true;
		}
		addToManager();
		WindowMenuManager.setCurrentManager(this);
	}

	public abstract JFrame getJFrame(boolean create);

	JRadioButtonMenuItem getMenuItem(WindowMenu key) {
		return menuItems.get(key);
	}

	public String getText() {
		return text;
	}

	private void removeFromManager() {
		if (!persistent && inManager) {
			inManager = false;
			for (WindowMenu menu : WindowMenuManager.getMenus()) {
				JRadioButtonMenuItem menuItem = menuItems.get(menu);
				menu.removeMenuItem(this, menuItem);
			}
			WindowMenuManager.removeManager(this);
		}
	}

	void removeMenuItem(WindowMenu menu) {
		JRadioButtonMenuItem item = menuItems.remove(menu);
		if (item != null)
			menu.removeMenuItem(this, item);
	}

	void setSelected(boolean selected) {
		for (JRadioButtonMenuItem item : menuItems.values()) {
			item.setSelected(selected);
		}
	}

	public void setText(String value) {
		text = value;
		for (JRadioButtonMenuItem menuItem : menuItems.values()) {
			menuItem.setText(text);
		}
	}
}
