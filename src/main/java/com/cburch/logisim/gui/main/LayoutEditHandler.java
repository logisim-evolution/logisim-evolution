/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.std.base.Image;
import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.TextEditActions;
import com.cburch.logisim.tools.TextTool;
import com.cburch.logisim.util.ImageUtil;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

public class LayoutEditHandler extends EditHandler
    implements ProjectListener, LibraryListener, PropertyChangeListener {
  private final Frame frame;

  LayoutEditHandler(Frame frame) {
    this.frame = frame;

    final var proj = frame.getProject();
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
    final var proj = frame.getProject();
    final var sel = proj == null ? null : proj.getSelection();
    final var selEmpty = (sel == null || sel.isEmpty());
    final var canChange = proj != null && proj.getLogisimFile().contains(proj.getCurrentCircuit());
    final var textEditActions = getTextEditActions();

    var selectAvailable = false;
    for (final var lib : proj.getLogisimFile().getLibraries()) {
      if (lib instanceof BaseLibrary) {
        selectAvailable = true;
        break;
      }
    }

    if (textEditActions != null) {
      setEnabled(LogisimMenuBar.CUT, canChange && textEditActions.canCut());
      setEnabled(LogisimMenuBar.COPY, textEditActions.canCopy());
      setEnabled(LogisimMenuBar.PASTE, canChange);
      setEnabled(LogisimMenuBar.DELETE, false);
      setEnabled(LogisimMenuBar.DUPLICATE, false);
      setEnabled(LogisimMenuBar.SELECT_ALL, textEditActions.canSelectAll());
      setEnabled(LogisimMenuBar.RAISE, false);
      setEnabled(LogisimMenuBar.LOWER, false);
      setEnabled(LogisimMenuBar.RAISE_TOP, false);
      setEnabled(LogisimMenuBar.LOWER_BOTTOM, false);
      setEnabled(LogisimMenuBar.ADD_CONTROL, false);
      setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
      return;
    }

    setEnabled(LogisimMenuBar.CUT, !selEmpty && selectAvailable && canChange);
    setEnabled(LogisimMenuBar.COPY, !selEmpty && selectAvailable);
    setEnabled(LogisimMenuBar.PASTE, selectAvailable && canChange);
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
    final var textEditActions = getTextEditActions();
    if (textEditActions != null) {
      textEditActions.copy();
      refreshAfterTextEditAction();
      return;
    }

    final var proj = frame.getProject();
    final var sel = frame.getCanvas().getSelection();
    proj.doAction(SelectionActions.copy(sel));
  }

  @Override
  public void cut() {
    final var textEditActions = getTextEditActions();
    if (textEditActions != null) {
      textEditActions.cut();
      refreshAfterTextEditAction();
      return;
    }

    final var proj = frame.getProject();
    final var sel = frame.getCanvas().getSelection();
    proj.doAction(SelectionActions.cut(sel));
  }

  @Override
  public void delete() {
    final var proj = frame.getProject();
    final var sel = frame.getCanvas().getSelection();
    proj.doAction(SelectionActions.clear(sel));
  }

  @Override
  public void duplicate() {
    final var proj = frame.getProject();
    final var sel = frame.getCanvas().getSelection();
    proj.doAction(SelectionActions.duplicate(sel));
  }

  @Override
  public void libraryChanged(LibraryEvent e) {
    final var action = e.getAction();
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
    final var textEditActions = getTextEditActions();
    if (textEditActions != null) {
      textEditActions.paste();
      refreshAfterTextEditAction();
      return;
    }

    final var proj = frame.getProject();
    final var sel = frame.getCanvas().getSelection();
    selectSelectTool(proj);

    if (pasteSystemClipboardImage(proj)) {
      return;
    }

    if (!Clipboard.isEmpty()) {
      final var action = SelectionActions.pasteMaybe(proj, sel);
      if (action != null) {
        proj.doAction(action);
        return;
      }
    }
  }

  private boolean pasteSystemClipboardImage(Project proj) {
    try {
      final var img = ImageUtil.getSystemClipboardImage();
      if (img != null) {
        final var base64 = ImageUtil.bufferedImageToBase64(img);
        if (base64 != null && !base64.isBlank()) {
          final var circuit = proj.getCurrentCircuit();
          final var attrs = Image.FACTORY.createAttributeSet();
          attrs.setValue(Image.ATTR_IMAGE, base64);
          attrs.setValue(Image.ATTR_WIDTH, img.getWidth());
          attrs.setValue(Image.ATTR_HEIGHT, img.getHeight());

          final var comp = Image.FACTORY.createComponent(Location.create(100, 100, false), attrs);
          final var xn = new CircuitMutation(circuit);
          xn.add(comp);
          proj.doAction(xn.toAction(S.getter("addComponentAction", Image.FACTORY.getDisplayGetter())));
          return true;
        }
      }
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(LayoutEditHandler.class)
          .error("Failed to paste image onto layout canvas from system clipboard", e);
    }
    return false;
  }

  @Override
  public void projectChanged(ProjectEvent e) {
    final var action = e.getAction();
    if (action == ProjectEvent.ACTION_SET_FILE) {
      computeEnabled();
    } else if (action == ProjectEvent.ACTION_SET_CURRENT) {
      computeEnabled();
    } else if (action == ProjectEvent.ACTION_SELECTION) {
      computeEnabled();
    } else if (action == ProjectEvent.ACTION_SET_TOOL) {
      computeEnabled();
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
    final var textEditActions = getTextEditActions();
    if (textEditActions != null) {
      textEditActions.selectAll();
      refreshAfterTextEditAction();
      return;
    }

    final var proj = frame.getProject();
    final var sel = frame.getCanvas().getSelection();
    selectSelectTool(proj);
    final var circ = proj.getCurrentCircuit();
    sel.addAll(circ.getWires());
    sel.addAll(circ.getNonWires());
    proj.repaintCanvas();
  }

  private void selectSelectTool(Project proj) {
    for (final var sub : proj.getLogisimFile().getLibraries()) {
      if (sub instanceof BaseLibrary baseLibrary) {
        final var tool = baseLibrary.getTool(EditTool._ID);
        if (tool != null) proj.setTool(tool);
      }
    }
  }

  private TextEditActions getTextEditActions() {
    final var proj = frame.getProject();
    if (proj != null && proj.getTool() instanceof TextTool textTool) {
      return textTool.getTextEditActions();
    }
    return null;
  }

  private void refreshAfterTextEditAction() {
    frame.computeEditMenuEnabled();
    frame.getProject().repaintCanvas();
  }
}
