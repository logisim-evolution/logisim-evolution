/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.vhdl.base.VhdlContent;
import com.cburch.logisim.vhdl.base.VhdlEntity;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ToolboxModel implements ProjectListener, LibraryListener, PropertyChangeListener {

  public interface Listener {
    void toolArmed(Tool tool);
    void circuitChanged(Circuit circuit);
    void structureChanged();
    void filterChanged(String query);
    void viewModeChanged(String newMode);
  }

  private final Project proj;
  private final Frame frame;
  private final List<Listener> listeners = new ArrayList<>();
  private String searchQuery = "";
  private String viewMode;
  private String activeCategory = "Gates";

  public ToolboxModel(Project proj, Frame frame) {
    this.proj = proj;
    this.frame = frame;
    this.viewMode = AppPreferences.EXPLORER_VIEW.get();

    proj.addProjectListener(this);
    if (proj.getLogisimFile() != null) {
      proj.getLogisimFile().addLibraryListener(this);
    }
    AppPreferences.EXPLORER_VIEW.addPropertyChangeListener(this);
  }

  public Project getProject() {
    return proj;
  }

  public Frame getFrame() {
    return frame;
  }

  public void addListener(Listener l) {
    if (!listeners.contains(l)) {
      listeners.add(l);
    }
  }

  public void removeListener(Listener l) {
    listeners.remove(l);
  }

  public String getViewMode() {
    return viewMode;
  }

  public void setViewMode(String mode) {
    if (mode != null && !mode.equals(viewMode)) {
      viewMode = mode;
      AppPreferences.EXPLORER_VIEW.set(mode);
      for (final var l : listeners) {
        l.viewModeChanged(mode);
      }
    }
  }

  public String getSearchQuery() {
    return searchQuery;
  }

  public void setSearchQuery(String query) {
    final var q = (query == null) ? "" : query.trim();
    if (!q.equals(searchQuery)) {
      searchQuery = q;
      for (final var l : listeners) {
        l.filterChanged(searchQuery);
      }
    }
  }

  public String getActiveCategory() {
    return activeCategory;
  }

  public void setActiveCategory(String cat) {
    if (cat != null && !cat.equals(activeCategory)) {
      activeCategory = cat;
    }
  }

  public Tool getArmedTool() {
    return proj != null ? proj.getTool() : null;
  }

  public void armTool(Tool t) {
    if (proj != null) {
      proj.setTool(t);
    }
  }

  public Circuit getCurrentCircuit() {
    return proj != null ? proj.getCurrentCircuit() : null;
  }

  public void setCurrentCircuit(Circuit c) {
    if (proj != null && c != null) {
      proj.setCurrentCircuit(c);
      if (frame != null) {
        frame.setEditorView(Frame.EDIT_LAYOUT);
      }
    }
  }

  public void setCurrentHdl(VhdlContent content) {
    if (proj != null && content != null) {
      proj.setCurrentHdlModel(content);
    }
  }

  /**
   * Returns project circuits and HDL entities (tools of LogisimFile).
   */
  public List<AddTool> getCircuits() {
    final var file = proj != null ? proj.getLogisimFile() : null;
    if (file == null) return Collections.emptyList();

    final var result = new ArrayList<AddTool>();
    for (final var tool : file.getTools()) {
      if (tool instanceof AddTool addTool) {
        final var factory = addTool.getFactory(false);
        if (factory instanceof SubcircuitFactory) {
          result.add(addTool);
        }
      }
    }
    return result;
  }

  /**
   * Returns list of top-level libraries imported into the project.
   */
  public List<Library> getLibraries() {
    final var file = proj != null ? proj.getLogisimFile() : null;
    if (file == null) return Collections.emptyList();
    return file.getLibraries();
  }

  public boolean isCircuitActive(AddTool tool) {
    if (tool == null || proj == null) return false;
    final var factory = tool.getFactory(false);
    if (factory instanceof SubcircuitFactory sub) {
      return sub.getSubcircuit() != null && sub.getSubcircuit() == proj.getCurrentCircuit();
    }
    if (factory instanceof VhdlEntity vhdl) {
      return frame != null && vhdl.getContent() != null && vhdl.getContent() == frame.getHdlEditorView();
    }
    return false;
  }

  @Override
  public void projectChanged(ProjectEvent event) {
    final var act = event.getAction();
    if (act == ProjectEvent.ACTION_SET_CURRENT) {
      final var circ = proj.getCurrentCircuit();
      for (final var l : listeners) {
        l.circuitChanged(circ);
      }
    } else if (act == ProjectEvent.ACTION_SET_TOOL) {
      final var tool = proj.getTool();
      for (final var l : listeners) {
        l.toolArmed(tool);
      }
    } else if (act == ProjectEvent.ACTION_SET_FILE) {
      final var oldFile = (LogisimFile) event.getOldData();
      final var newFile = (LogisimFile) event.getData();
      if (oldFile != null) oldFile.removeLibraryListener(this);
      if (newFile != null) newFile.addLibraryListener(this);
      for (final var l : listeners) {
        l.structureChanged();
      }
    }
  }

  @Override
  public void libraryChanged(LibraryEvent event) {
    for (final var l : listeners) {
      l.structureChanged();
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (AppPreferences.EXPLORER_VIEW.isSource(evt)) {
      final var newMode = AppPreferences.EXPLORER_VIEW.get();
      if (!newMode.equals(viewMode)) {
        viewMode = newMode;
        for (final var l : listeners) {
          l.viewModeChanged(newMode);
        }
      }
    }
  }
}
