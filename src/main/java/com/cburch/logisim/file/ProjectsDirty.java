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

package com.cburch.logisim.file;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

class ProjectsDirty {
  private static class DirtyListener implements LibraryListener {
    final Project proj;

    DirtyListener(Project proj) {
      this.proj = proj;
    }

    @Override
    public void libraryChanged(LibraryEvent event) {
      if (event.getAction() == LibraryEvent.DIRTY_STATE) {
        final var lib = proj.getLogisimFile();
        final var file = lib.getLoader().getMainFile();
        LibraryManager.instance.setDirty(file, lib.isDirty());
      }
    }
  }

  private static class ProjectListListener implements PropertyChangeListener {
    @Override
    public synchronized void propertyChange(PropertyChangeEvent event) {
      for (final var l : listeners) {
        l.proj.removeLibraryListener(l);
      }
      listeners.clear();
      for (final var proj : Projects.getOpenProjects()) {
        final var l = new DirtyListener(proj);
        proj.addLibraryListener(l);
        listeners.add(l);

        final var lib = proj.getLogisimFile();
        LibraryManager.instance.setDirty(lib.getLoader().getMainFile(), lib.isDirty());
      }
    }
  }

  public static void initialize() {
    Projects.addPropertyChangeListener(Projects.PROJECT_LIST_PROPERTY, projectListListener);
  }

  private static final ProjectListListener projectListListener = new ProjectListListener();
  private static final ArrayList<DirtyListener> listeners = new ArrayList<>();

  private ProjectsDirty() {}
}
