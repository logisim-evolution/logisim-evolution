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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;

class OpenRecent extends JMenu implements PropertyChangeListener {
	private class RecentItem extends JMenuItem implements ActionListener {
		private static final long serialVersionUID = 1L;
		private File file;

		RecentItem(File file) {
			super(getFileText(file));
			this.file = file;
			setEnabled(file != null);
			addActionListener(this);
		}

		public void actionPerformed(ActionEvent event) {
			Project proj = menubar.getProject();
			Component par = proj == null ? null : proj.getFrame().getCanvas();
			ProjectActions.doOpen(par, proj, file);
		}
	}

	private static String getFileText(File file) {
		if (file == null) {
			return Strings.get("fileOpenRecentNoChoices");
		} else {
			String ret;
			try {
				ret = file.getCanonicalPath();
			} catch (IOException e) {
				ret = file.toString();
			}
			if (ret.length() <= MAX_ITEM_LENGTH) {
				return ret;
			} else {
				ret = ret.substring(ret.length() - MAX_ITEM_LENGTH + 3);
				int splitLoc = ret.indexOf(File.separatorChar);
				if (splitLoc >= 0) {
					ret = ret.substring(splitLoc);
				}
				return "..." + ret;
			}
		}
	}

	private static final long serialVersionUID = 1L;

	private static final int MAX_ITEM_LENGTH = 50;
	private LogisimMenuBar menubar;

	private List<RecentItem> recentItems;

	OpenRecent(LogisimMenuBar menubar) {
		this.menubar = menubar;
		this.recentItems = new ArrayList<RecentItem>();
		AppPreferences.addPropertyChangeListener(
				AppPreferences.RECENT_PROJECTS, this);
		renewItems();
	}
	

	void localeChanged() {
		setText(Strings.get("fileOpenRecentItem"));
		for (RecentItem item : recentItems) {
			if (item.file == null) {
				item.setText(Strings.get("fileOpenRecentNoChoices"));
			}
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(AppPreferences.RECENT_PROJECTS)) {
			renewItems();
		}
	}

	private void renewItems() {
		for (int index = recentItems.size() - 1; index >= 0; index--) {
			RecentItem item = recentItems.get(index);
			remove(item);
		}
		recentItems.clear();

		List<File> files = AppPreferences.getRecentFiles();
		if (files.isEmpty()) {
			recentItems.add(new RecentItem(null));
		} else {
			for (File file : files) {
				recentItems.add(new RecentItem(file));
			}
		}

		for (RecentItem item : recentItems) {
			add(item);
		}
	}
}
