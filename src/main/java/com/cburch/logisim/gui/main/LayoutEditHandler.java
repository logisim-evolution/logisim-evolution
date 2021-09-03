/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.tools.EditTool;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import lombok.val;

public class LayoutEditHandler extends EditHandler implements ProjectListener, LibraryListener, PropertyChangeListener {
  private final Frame frame;

  LayoutEditHandler(Frame frame) {
    this.frame = frame;

    Project proj = frame.getProject();
    Clipboard.addPropertyChangeListener(Clipboard.CONTENTS_PROPERTY, this);
    proj.addProjectListener(this);
    proj.addLibraryListener(this);
  }

  @Override
  public void addControlPoint() {
    // not yet supported in layout mode
  }

  @Override
  public void computeEnabled() {
    val proj = frame.getProject();
    val sel = proj == null ? null : proj.getSelection();
    val selEmpty = (sel == null || sel.isEmpty());
    val canChange = proj != null && proj.getLogisimFile().contains(proj.getCurrentCircuit());

    var selectAvailable = false;
    for (val lib : proj.getLogisimFile().getLibraries()) {
      if (lib instanceof BaseLibrary) {
        selectAvailable = true;
        break;
      }
    }

    setEnabled(LogisimMenuBar.CUT, !selEmpty && selectAvailable && canChange);
    setEnabled(LogisimMenuBar.COPY, !selEmpty && selectAvailable);
    setEnabled(LogisimMenuBar.PASTE, selectAvailable && canChange && !Clipboard.isEmpty());
    setEnabled(LogisimMenuBar.DELETE, !selEmpty && selectAvailable && canChange);
    setEnabled(LogisimMenuBar.DUPLICATE, !selEmpty && selectAvailable && canChange);
    setEnabled(LogisimMenuBar.SELECT_ALL, selectAvailable);
    setEnabled(LogisimMenuBar.RAISE, false);
    setEnabled(LogisimMenuBar.LOWER, false);
    setEnabled(LogisimMenuBar.RAISE_TOP, false);
    setEnabled(LogisimMenuBar.LOWER_BOTTOM, false);
    setEnabled(LogisimMenuBar.ADD_CONTROL, false);
    setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
  }

  @Override
  public void copy() {
    val proj = frame.getProject();
    val sel = frame.getCanvas().getSelection();
    proj.doAction(SelectionActions.copy(sel));
  }

  @Override
  public void cut() {
    val proj = frame.getProject();
    val sel = frame.getCanvas().getSelection();
    proj.doAction(SelectionActions.cut(sel));
  }

  @Override
  public void delete() {
    val proj = frame.getProject();
    val sel = frame.getCanvas().getSelection();
    proj.doAction(SelectionActions.clear(sel));
  }

  @Override
  public void duplicate() {
    val proj = frame.getProject();
    val sel = frame.getCanvas().getSelection();
    proj.doAction(SelectionActions.duplicate(sel));
  }

  @Override
  public void libraryChanged(LibraryEvent e) {
    val action = e.getAction();
    if (action == LibraryEvent.ADD_LIBRARY) {
      computeEnabled();
    } else if (action == LibraryEvent.REMOVE_LIBRARY) {
      computeEnabled();
    }
  }

  @Override
  public void lower() {
    // not yet supported in layout mode
  }

  @Override
  public void lowerBottom() {
    // not yet supported in layout mode
  }

  @Override
  public void paste() {
    val proj = frame.getProject();
    val sel = frame.getCanvas().getSelection();
    selectSelectTool(proj);
    val action = SelectionActions.pasteMaybe(proj, sel);
    if (action != null) {
      proj.doAction(action);
    }
  }

  @Override
  public void projectChanged(ProjectEvent e) {
    val action = e.getAction();
    switch (action) {
      case ProjectEvent.ACTION_SET_FILE:
      case ProjectEvent.ACTION_SET_CURRENT:
      case ProjectEvent.ACTION_SELECTION:
        computeEnabled();
        break;

      default:
        // do nothing
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals(Clipboard.CONTENTS_PROPERTY)) {
      computeEnabled();
    }
  }

  @Override
  public void raise() {
    // not yet supported in layout mode
  }

  @Override
  public void raiseTop() {
    // not yet supported in layout mode
  }

  @Override
  public void removeControlPoint() {
    // not yet supported in layout mode
  }

  @Override
  public void selectAll() {
    val proj = frame.getProject();
    val sel = frame.getCanvas().getSelection();
    selectSelectTool(proj);
    val circ = proj.getCurrentCircuit();
    sel.addAll(circ.getWires());
    sel.addAll(circ.getNonWires());
    proj.repaintCanvas();
  }

  private void selectSelectTool(Project proj) {
    for (val sub : proj.getLogisimFile().getLibraries()) {
      if (sub instanceof BaseLibrary) {
        val baseLibrary = (BaseLibrary) sub;
        val tool = baseLibrary.getTool(EditTool._ID);
        if (tool != null) proj.setTool(tool);
      }
    }
  }
}
