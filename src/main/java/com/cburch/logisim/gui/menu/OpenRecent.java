/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
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

class OpenRecent extends JMenu implements PropertyChangeListener {
  private static final long serialVersionUID = 1L;
  private static final int MAX_ITEM_LENGTH = 50;
  private final LogisimMenuBar menubar;
  private final List<RecentItem> recentItems;

  OpenRecent(LogisimMenuBar menubar) {
    this.menubar = menubar;
    this.recentItems = new ArrayList<>();
    AppPreferences.addPropertyChangeListener(AppPreferences.RECENT_PROJECTS, this);
    renewItems();
  }

  private static String getFileText(File file) {
    if (file == null) {
      return S.get("fileOpenRecentNoChoices");
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

  void localeChanged() {
    setText(S.get("fileOpenRecentItem"));
    for (RecentItem item : recentItems) {
      if (item.file == null) {
        item.setText(S.get("fileOpenRecentNoChoices"));
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

  private class RecentItem extends JMenuItem implements ActionListener {
    private static final long serialVersionUID = 1L;
    private final File file;

    RecentItem(File file) {
      super(getFileText(file));
      this.file = file;
      setEnabled(file != null);
      addActionListener(this);
    }

    public void actionPerformed(ActionEvent event) {
      Project proj = menubar.getSaveProject();
      Project baseProj = menubar.getBaseProject();
      Component parent  = baseProj != null ? baseProj.getFrame().getCanvas() : menubar.getParentFrame();
      Project newProj = ProjectActions.doOpen(parent, baseProj, file);
      // If the current project hasn't been touched and has no file associated
      // with it (i.e. is entirely blank), and the new file was opened
      // successfully, then go ahead and close the old blank window.
      // todo: and has no subwindows or dialogs open?
      if (newProj != null && proj != null
          && !proj.isFileDirty()
          && proj.getLogisimFile().getLoader().getMainFile() == null) {
        proj.getFrame().dispose();
      }
    }
  }
}
