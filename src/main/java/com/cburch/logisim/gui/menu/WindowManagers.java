/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import com.cburch.logisim.analyze.gui.AnalyzerManager;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.WindowMenuItemManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import javax.swing.JFrame;

public class WindowManagers {
  private static final MyListener myListener = new MyListener();
  private static final HashMap<Project, ProjectManager> projectMap =
      new LinkedHashMap<>();
  private static boolean initialized = false;

  private WindowManagers() {}

  private static void computeListeners() {
    final var nowOpen = Projects.getOpenProjects();

    final var closed = new HashSet<Project>(projectMap.keySet());
    nowOpen.forEach(closed::remove);
    for (final var proj : closed) {
      final var manager = projectMap.get(proj);
      manager.frameClosed(manager.getJFrame(false, null));
      projectMap.remove(proj);
    }

    final var opened = new LinkedHashSet<Project>(nowOpen);
    opened.removeAll(projectMap.keySet());
    for (final var proj : opened) {
      final var manager = new ProjectManager(proj);
      projectMap.put(proj, manager);
    }
  }

  public static void initialize() {
    if (!initialized) {
      initialized = true;
      AnalyzerManager.initialize();
      PreferencesFrame.initializeManager();
      Projects.addPropertyChangeListener(Projects.PROJECT_LIST_PROPERTY, myListener);
      computeListeners();
    }
  }

  private static class MyListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      computeListeners();
    }
  }

  private static class ProjectManager extends WindowMenuItemManager
      implements ProjectListener, LibraryListener {
    private final Project proj;

    ProjectManager(Project proj) {
      super(proj.getLogisimFile().getName(), false);
      this.proj = proj;
      proj.addProjectListener(this);
      proj.addLibraryListener(this);
      frameOpened(proj.getFrame());
    }

    @Override
    public JFrame getJFrame(boolean create, java.awt.Component parent) {
      return proj.getFrame();
    }

    @Override
    public void libraryChanged(LibraryEvent event) {
      if (event.getAction() == LibraryEvent.SET_NAME) {
        setText((String) event.getData());
      }
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      if (event.getAction() == ProjectEvent.ACTION_SET_FILE) {
        setText(proj.getLogisimFile().getName());
      }
    }
  }
}
