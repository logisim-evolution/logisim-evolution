/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.ProjectActions;
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
    for (final var item : recentItems) {
      if (item.file == null) {
        item.setText(S.get("fileOpenRecentNoChoices"));
      }
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals(AppPreferences.RECENT_PROJECTS)) {
      renewItems();
    }
  }

  private void renewItems() {
    for (var index = recentItems.size() - 1; index >= 0; index--) {
      remove(recentItems.get(index));
    }
    recentItems.clear();

    final var files = AppPreferences.getRecentFiles();
    if (files.isEmpty()) {
      recentItems.add(new RecentItem(null));
    } else {
      for (final var file : files) {
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

    @Override
    public void actionPerformed(ActionEvent event) {
      final var proj = menubar.getSaveProject();
      final var baseProj = menubar.getBaseProject();
      final var parent =
          (baseProj != null) ? baseProj.getFrame().getCanvas() : menubar.getParentFrame();
      final var newProj = ProjectActions.doOpen(parent, baseProj, file);
      // If the current project hasn't been touched and has no file associated
      // with it (i.e. is entirely blank), and the new file was opened
      // successfully, then go ahead and close the old blank window.
      // TODO: and has no subwindows or dialogs open?
      if (newProj != null
          && proj != null
          && !proj.isFileDirty()
          && proj.getLogisimFile().getLoader().getMainFile() == null) {
        proj.getFrame().dispose();
      }
    }
  }
}
