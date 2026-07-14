/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.tools.TextEditable;
import org.junit.jupiter.api.Test;

public class InstanceTextFieldTest {

  @Test
  public void changedTextCommitCreatesAction() {
    assertNotNull(newTextEditable().getCommitAction(null, "old", "new"));
  }

  @Test
  public void unchangedTextCommitCreatesNoAction() {
    assertNull(newTextEditable().getCommitAction(null, "same", "same"));
  }

  @Test
  public void committedTextChangeCanBeUndoneByProjectUndoManager() {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();
    final var attrs = Text.FACTORY.createAttributeSet();
    attrs.setValue(Text.ATTR_TEXT, "old");
    final var comp = Text.FACTORY.createComponent(Location.create(0, 0, false), attrs);
    final var addComponent = new CircuitMutation(circuit);
    addComponent.add(comp);
    addComponent.execute();
    final var editable = (TextEditable) comp.getFeature(TextEditable.class);

    final var action = editable.getCommitAction(circuit, "old", "new");

    assertNotNull(action);
    project.doAction(action);
    assertEquals("new", comp.getAttributeSet().getValue(Text.ATTR_TEXT));
    project.undoAction();
    assertEquals("old", comp.getAttributeSet().getValue(Text.ATTR_TEXT));
  }

  private static TextEditable newTextEditable() {
    final var attrs = Text.FACTORY.createAttributeSet();
    final var comp = Text.FACTORY.createComponent(Location.create(0, 0, false), attrs);
    final var editable = (TextEditable) comp.getFeature(TextEditable.class);

    assertNotNull(editable);
    return editable;
  }
}
