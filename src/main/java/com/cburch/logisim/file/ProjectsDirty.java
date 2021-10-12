/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public final class ProjectsDirty {
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
      for (final var listener : listeners) {
        listener.proj.removeLibraryListener(listener);
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

  private static final ProjectListListener projectListListener = new ProjectListListener();
  private static final List<DirtyListener> listeners = new ArrayList<>();

  private ProjectsDirty() {}

  public static void initialize() {
    Projects.addPropertyChangeListener(Projects.PROJECT_LIST_PROPERTY, projectListListener);
  }
}
