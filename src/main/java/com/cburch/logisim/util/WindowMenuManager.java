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

import java.util.ArrayList;
import java.util.List;

class WindowMenuManager {
	public static void addManager(WindowMenuItemManager manager) {
		for (WindowMenu menu : menus) {
			manager.createMenuItem(menu);
		}
		managers.add(manager);
	}

	public static void addMenu(WindowMenu menu) {
		for (WindowMenuItemManager manager : managers) {
			manager.createMenuItem(menu);
		}
		menus.add(menu);
	}

	private static void enableAll() {
		for (WindowMenu menu : menus) {
			menu.computeEnabled();
		}
	}

	static WindowMenuItemManager getCurrentManager() {
		return currentManager;
	}

	static List<WindowMenu> getMenus() {
		return menus;
	}

	// TODO frames should call removeMenu when they're destroyed

	public static void removeManager(WindowMenuItemManager manager) {
		for (WindowMenu menu : menus) {
			manager.removeMenuItem(menu);
		}
		managers.remove(manager);
	}

	static void setCurrentManager(WindowMenuItemManager value) {
		if (value == currentManager)
			return;

		boolean doEnable = (currentManager == null) != (value == null);
		if (currentManager == null)
			setNullItems(false);
		else
			currentManager.setSelected(false);
		currentManager = value;
		if (currentManager == null)
			setNullItems(true);
		else
			currentManager.setSelected(true);
		if (doEnable)
			enableAll();
	}

	private static void setNullItems(boolean value) {
		for (WindowMenu menu : menus) {
			menu.setNullItemSelected(value);
		}
	}

	static void unsetCurrentManager(WindowMenuItemManager value) {
		if (value != currentManager)
			return;
		setCurrentManager(null);
	}

	private static ArrayList<WindowMenu> menus = new ArrayList<WindowMenu>();

	private static ArrayList<WindowMenuItemManager> managers = new ArrayList<WindowMenuItemManager>();

	private static WindowMenuItemManager currentManager = null;

	private WindowMenuManager() {
	}
}
