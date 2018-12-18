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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

public class WindowMenu extends JMenu {
	private class MyListener implements LocaleListener, ActionListener {
		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if (src == minimize) {
				doMinimize();
			} else if (src == zoom) {
				doZoom();
			} else if (src == close) {
				doClose();
			} else if (src instanceof WindowMenuItem) {
				WindowMenuItem choice = (WindowMenuItem) src;
				if (choice.isSelected()) {
					WindowMenuItem item = findOwnerItem();
					if (item != null) {
						item.setSelected(true);
					}
					choice.actionPerformed(e);
				}
			}
		}

		private WindowMenuItem findOwnerItem() {
			for (WindowMenuItem i : persistentItems) {
				if (i.getJFrame() == owner)
					return i;
			}
			for (WindowMenuItem i : transientItems) {
				if (i.getJFrame() == owner)
					return i;
			}
			return null;
		}

		public void localeChanged() {
			WindowMenu.this.setText(Strings.get("windowMenu"));
			minimize.setText(Strings.get("windowMinimizeItem"));
			close.setText(Strings.get("windowCloseItem"));
			zoom.setText(MacCompatibility.isQuitAutomaticallyPresent() ? Strings
					.get("windowZoomItemMac") : Strings.get("windowZoomItem"));
		}
	}

	private static final long serialVersionUID = 1L;

	private JFrame owner;
	private MyListener myListener = new MyListener();
	private JMenuItem minimize = new JMenuItem();
	private JMenuItem zoom = new JMenuItem();
	private JMenuItem close = new JMenuItem();
	private JRadioButtonMenuItem nullItem = new JRadioButtonMenuItem();
	private ArrayList<WindowMenuItem> persistentItems = new ArrayList<WindowMenuItem>();
	private ArrayList<WindowMenuItem> transientItems = new ArrayList<WindowMenuItem>();

	public WindowMenu(JFrame owner) {
		this.owner = owner;
		WindowMenuManager.addMenu(this);

		int menuMask = getToolkit().getMenuShortcutKeyMask();
		minimize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, menuMask));
		close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, menuMask));

		if (owner == null) {
			minimize.setEnabled(false);
			zoom.setEnabled(false);
			close.setEnabled(false);
		} else {
			minimize.addActionListener(myListener);
			zoom.addActionListener(myListener);
			close.addActionListener(myListener);
		}

		computeEnabled();
		computeContents();

		LocaleManager.addLocaleListener(myListener);
		myListener.localeChanged();
	}

    void addMenuItem(Object source, WindowMenuItem item, boolean persistent) {
		if (persistent)
			persistentItems.add(item);
		else
			transientItems.add(item);
		item.addActionListener(myListener);
		computeContents();
	}

	private void computeContents() {
		ButtonGroup bgroup = new ButtonGroup();
		bgroup.add(nullItem);

		removeAll();
		add(minimize);
		add(zoom);
		add(close);

		if (!persistentItems.isEmpty()) {
			addSeparator();
			for (JRadioButtonMenuItem item : persistentItems) {
				bgroup.add(item);
				add(item);
			}
		}

		if (!transientItems.isEmpty()) {
			addSeparator();
			for (JRadioButtonMenuItem item : transientItems) {
				bgroup.add(item);
				add(item);
			}
		}

		WindowMenuItemManager currentManager = WindowMenuManager
				.getCurrentManager();
		if (currentManager != null) {
			JRadioButtonMenuItem item = currentManager.getMenuItem(this);
			if (item != null) {
				item.setSelected(true);
			}
		}
	}

	void computeEnabled() {
		WindowMenuItemManager currentManager = WindowMenuManager
				.getCurrentManager();
		minimize.setEnabled(currentManager != null);
		zoom.setEnabled(currentManager != null);
		close.setEnabled(currentManager != null);
	}

	void doClose() {
		if (owner instanceof WindowClosable) {
			((WindowClosable) owner).requestClose();
		} else if (owner != null) {
			int action = owner.getDefaultCloseOperation();
			if (action == JFrame.EXIT_ON_CLOSE) {
				System.exit(0);
			} else if (action == JFrame.HIDE_ON_CLOSE) {
				owner.setVisible(false);
			} else if (action == JFrame.DISPOSE_ON_CLOSE) {
				owner.dispose();
			}
		}
	}

	void doMinimize() {
		if (owner != null) {
			owner.setExtendedState(Frame.ICONIFIED);
		}
	}

	void doZoom() {
		if (owner == null)
			return;

		owner.pack();
		Dimension screenSize = owner.getToolkit().getScreenSize();
		Dimension windowSize = owner.getPreferredSize();
		Point windowLoc = owner.getLocation();

		boolean locChanged = false;
		boolean sizeChanged = false;
		if (windowLoc.x + windowSize.width > screenSize.width) {
			windowLoc.x = Math.max(0, screenSize.width - windowSize.width);
			locChanged = true;
			if (windowLoc.x + windowSize.width > screenSize.width) {
				windowSize.width = screenSize.width - windowLoc.x;
				sizeChanged = true;
			}
		}
		if (windowLoc.y + windowSize.height > screenSize.height) {
			windowLoc.y = Math.max(0, screenSize.height - windowSize.height);
			locChanged = true;
			if (windowLoc.y + windowSize.height > screenSize.height) {
				windowSize.height = screenSize.height - windowLoc.y;
				sizeChanged = true;
			}
		}

		if (locChanged)
			owner.setLocation(windowLoc);
		if (sizeChanged)
			owner.setSize(windowSize);
	}

	void removeMenuItem(Object source, JRadioButtonMenuItem item) {
		if (transientItems.remove(item)) {
			item.removeActionListener(myListener);
		}
		computeContents();
	}

	void setNullItemSelected(boolean value) {
		nullItem.setSelected(value);
	}
}
