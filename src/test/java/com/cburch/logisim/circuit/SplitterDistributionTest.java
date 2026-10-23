/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import org.junit.jupiter.api.Test;

class SplitterDistributionTest {
  @Test
  void redistributesBothSetupOrdersAndSupportsUndoRedo() {
    for (final var widthFirst : new boolean[] {true, false}) {
      final var file = LogisimFile.createNew(new Loader(null), null);
      final var project = new Project(file);
      final var circuit = file.getMainCircuit();
      circuit.setProject(project);
      project.setCurrentCircuit(circuit);
      final var attrs = new SplitterAttributes();
      if (widthFirst) attrs.setValue(SplitterAttributes.ATTR_WIDTH, BitWidth.create(16));
      attrs.setValue(SplitterAttributes.ATTR_FANOUT, 16);
      if (!widthFirst) attrs.setValue(SplitterAttributes.ATTR_WIDTH, BitWidth.create(16));
      final var splitter = new Splitter(Location.create(0, 0, true), attrs);
      final var add = new CircuitMutation(circuit);
      add.add(splitter);
      // Include a disconnected bit and a custom assignment in the undo snapshot.
      setBit(attrs, 0, 0);
      setBit(attrs, 1, 16);
      add.execute();
      final var original = attrs.bitEnd.clone();
      splitter.redistribute(project, circuit, 1);
      final var ascending = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
      assertArrayEquals(ascending, attrs.bitEnd);
      final var action = project.getLastAction();
      splitter.redistribute(project, circuit, 1);
      assertSame(action, project.getLastAction());
      project.undoAction();
      assertArrayEquals(original, attrs.bitEnd);
      assertNull(project.getLastAction());
      project.redoAction();
      assertArrayEquals(ascending, attrs.bitEnd);
    }
  }

  @Test
  void sharedActionRetainsDistributionEdgeCasesAndDescendingMenu() {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();
    circuit.setProject(project);
    project.setCurrentCircuit(circuit);
    final var attrs = new SplitterAttributes();
    attrs.setValue(SplitterAttributes.ATTR_WIDTH, BitWidth.create(8));
    attrs.setValue(SplitterAttributes.ATTR_FANOUT, 3);
    final var splitter = new Splitter(Location.create(0, 0, true), attrs);
    final var add = new CircuitMutation(circuit);
    add.add(splitter);
    add.execute();
    splitter.redistribute(project, circuit, 1);
    assertArrayEquals(new byte[] {1, 1, 1, 2, 2, 2, 3, 3}, attrs.bitEnd);
    new SplitterDistributeItem(project, splitter, -1).doClick();
    assertArrayEquals(new byte[] {3, 3, 2, 2, 2, 1, 1, 1}, attrs.bitEnd);
    final var moreEnds = new CircuitMutation(circuit);
    moreEnds.set(splitter, SplitterAttributes.ATTR_FANOUT, 10);
    moreEnds.execute();
    splitter.redistribute(project, circuit, 1);
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, attrs.bitEnd);
    final var oneEnd = new CircuitMutation(circuit);
    oneEnd.set(splitter, SplitterAttributes.ATTR_FANOUT, 1);
    oneEnd.execute();
    splitter.redistribute(project, circuit, 1);
    assertArrayEquals(new byte[] {1, 1, 1, 1, 1, 1, 1, 1}, attrs.bitEnd);
  }

  @SuppressWarnings("unchecked")
  private static void setBit(SplitterAttributes attrs, int bit, int end) {
    attrs.setValue((Attribute<Integer>) attrs.getBitOutAttribute(bit), end);
  }
}
