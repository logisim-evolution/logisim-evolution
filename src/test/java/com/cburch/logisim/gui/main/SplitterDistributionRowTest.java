/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.proj.Project;
import java.awt.event.FocusEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SplitterDistributionRowTest {
  @Test
  @SuppressWarnings("unchecked")
  void buttonCreatesOneActionAndFocusCompletionCreatesNone() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final var file = LogisimFile.createNew(new Loader(null), null);
      final var project = new Project(file);
      final var circuit = file.getMainCircuit();
      circuit.setProject(project);
      project.setCurrentCircuit(circuit);
      final var attrs = SplitterFactory.instance.createAttributeSet();
      attrs.setValue(SplitterAttributes.ATTR_WIDTH, BitWidth.create(8));
      attrs.setValue(SplitterAttributes.ATTR_FANOUT, 3);
      final var splitter = new Splitter(Location.create(0, 0, true), attrs);
      final var add = new CircuitMutation(circuit);
      add.add(splitter);
      add.execute();
      final var model = new AttrTableComponentModel(project, circuit, splitter);
      final var panel = new AttrTable(null);
      panel.setAttrTableModel(model);
      final var properties = (JPanel) panel.getComponent(0);
      final var scroll = (JScrollPane) properties.getComponent(1);
      final var table = (JTable) scroll.getViewport().getView();
      final var index = model.getRowCount() - 1;
      assertTrue(table.editCellAt(index, 1));
      final var editor = table.getEditorComponent();
      final var focusLost = new FocusEvent(editor, FocusEvent.FOCUS_LOST, false, new JPanel());
      for (final var listener : editor.getFocusListeners()) listener.focusLost(focusLost);
      assertNull(project.getLastAction());
      assertTrue(table.editCellAt(index, 1));
      ((JButton) table.getEditorComponent()).doClick();
      final var action = project.getLastAction();
      assertFalse(splitter.distributionChanges(1));
      assertFalse(model.getRow(index).isValueEditable());
      assertEquals(2, attrs.getValue((Attribute<Integer>) attrs.getAttribute("bit3")));
      project.undoAction();
      assertTrue(model.getRow(index).isValueEditable());
      assertTrue(splitter.distributionChanges(1));
      project.redoAction();
      assertSame(action, project.getLastAction());
      assertFalse(model.getRow(index).isValueEditable());
      final var remove = new CircuitMutation(circuit);
      remove.remove(splitter);
      remove.execute();
      assertFalse(model.getRow(index).isValueEditable());
      ((JButton) model.getRow(index).getEditor(null)).doClick();
      assertSame(action, project.getLastAction());
    });
  }
}
