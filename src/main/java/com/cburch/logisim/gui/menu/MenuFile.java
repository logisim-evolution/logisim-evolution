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

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

class MenuFile extends Menu implements ActionListener {
  private static final long serialVersionUID = 1L;
  private final LogisimMenuBar menubar;
  private final JMenuItem newi = new JMenuItem();
  private final JMenuItem merge = new JMenuItem();
  private final JMenuItem open = new JMenuItem();
  private final OpenRecent openRecent;
  private final JMenuItem close = new JMenuItem();
  private final JMenuItem save = new JMenuItem();
  private final JMenuItem saveAs = new JMenuItem();
  private final JMenuItem exportProj = new JMenuItem();
  private final MenuItemImpl print = new MenuItemImpl(this, LogisimMenuBar.PRINT);
  private final MenuItemImpl exportImage = new MenuItemImpl(this, LogisimMenuBar.EXPORT_IMAGE);
  private final JMenuItem prefs = new JMenuItem();
  private final JMenuItem quit = new JMenuItem();

  public MenuFile(LogisimMenuBar menubar) {
    this.menubar = menubar;
    openRecent = new OpenRecent(menubar);

    final var menuMask = getToolkit().getMenuShortcutKeyMaskEx();

    newi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, menuMask));
    merge.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, menuMask));
    open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, menuMask));
    close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, menuMask | InputEvent.SHIFT_DOWN_MASK));
    save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask));
    saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask | InputEvent.SHIFT_DOWN_MASK));
    exportProj.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, menuMask | InputEvent.SHIFT_DOWN_MASK));
    print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, menuMask));
    quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, menuMask));

    add(newi);
    add(merge);
    add(open);
    add(openRecent);
    addSeparator();
    add(close);
    add(save);
    add(saveAs);
    add(exportProj);
    addSeparator();
    add(exportImage);
    add(print);
    if (!MacCompatibility.isPreferencesAutomaticallyPresent()) {
      addSeparator();
      add(prefs);
    }
    if (!MacCompatibility.isQuitAutomaticallyPresent()) {
      addSeparator();
      add(quit);
    }

    final var proj = menubar.getSaveProject();
    newi.addActionListener(this);
    open.addActionListener(this);
    if (proj == null) {
      merge.setEnabled(false);
      close.setEnabled(false);
      save.setEnabled(false);
      saveAs.setEnabled(false);
      exportProj.setEnabled(false);
    } else {
      merge.addActionListener(this);
      close.addActionListener(this);
      save.addActionListener(this);
      saveAs.addActionListener(this);
      exportProj.addActionListener(this);
    }
    menubar.registerItem(LogisimMenuBar.EXPORT_IMAGE, exportImage);
    menubar.registerItem(LogisimMenuBar.PRINT, print);
    prefs.addActionListener(this);
    quit.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final var src = e.getSource();
    final var proj = menubar.getSaveProject();
    final var baseProj = menubar.getBaseProject();
    if (src == newi) {
      ProjectActions.doNew(baseProj);
    } else if (src == merge) {
      ProjectActions.doMerge(baseProj == null ? null : baseProj.getFrame().getCanvas(), baseProj);
    } else if (src == open) {
      final var newProj = ProjectActions.doOpen(baseProj == null ? null : baseProj.getFrame().getCanvas(), baseProj);
      if (newProj != null
          && proj != null
          && !proj.isFileDirty()
          && proj.getLogisimFile().getLoader().getMainFile() == null) {
        proj.getFrame().dispose();
      }
    } else if (src == close && proj != null) {
      var result = 0;
      final var frame = proj.getFrame();
      if (proj.isFileDirty()) {
        /* Must use hardcoded strings here, because the string management is rotten */
        final var message = "What should happen to your unsaved changes to " + proj.getLogisimFile().getName();
        String[] options = {"Save", "Discard", "Cancel"};
        result =
            OptionPane.showOptionDialog(
                OptionPane.getFrameForComponent(this),
                message,
                "Confirm Close",
                0,
                OptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (result == 0) {
          ProjectActions.doSave(proj);
        }
      }

      // If "cancel" pressed do nothing, otherwise dispose the window,
      // opening one if this was the last opened window.
      if (result != 2) {
        // Get the list of open projects
        final var projectList = Projects.getOpenProjects();
        if (projectList.size() == 1) {
          // Since we have a single window open, before closing the
          // current project open a new empty one
          ProjectActions.doNew(proj);
        }

        // Close the current project
        frame.dispose();
      }
    } else if (src == prefs) {
      PreferencesFrame.showPreferences();
    } else if (src == quit) {
      ProjectActions.doQuit();
    } else if (proj != null) {
      if (src == save) {
        ProjectActions.doSave(proj);
      } else if (src == saveAs) {
        ProjectActions.doSaveAs(proj);
      } else if (src == exportProj) {
        ProjectActions.doExportProject(proj);
      }
    }
  }

  @Override
  void computeEnabled() {
    setEnabled(true);
    menubar.fireEnableChanged();
  }

  public void localeChanged() {
    this.setText(S.get("fileMenu"));
    newi.setText(S.get("fileNewItem"));
    merge.setText(S.get("fileMergeItem"));
    open.setText(S.get("fileOpenItem"));
    openRecent.localeChanged();
    close.setText(S.get("fileCloseItem"));
    save.setText(S.get("fileSaveItem"));
    saveAs.setText(S.get("fileSaveAsItem"));
    exportProj.setText(S.get("fileExportProject"));
    exportImage.setText(S.get("fileExportImageItem"));
    print.setText(S.get("filePrintItem"));
    prefs.setText(S.get("filePreferencesItem"));
    quit.setText(S.get("fileQuitItem"));
  }
}
