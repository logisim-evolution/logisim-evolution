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

import com.cburch.logisim.Main;
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
      if (Main.ANALYZE) AnalyzerManager.initialize();
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
