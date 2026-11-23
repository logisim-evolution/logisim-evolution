/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import org.junit.jupiter.api.Test;

class AttrTableComponentModelTest {
  @Test
  void offersCommandOnlyForSplitterAndDisablesForeignCircuits() {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();
    final var attrs = SplitterFactory.instance.createAttributeSet();
    attrs.setValue(SplitterAttributes.ATTR_WIDTH, BitWidth.create(8));
    attrs.setValue(SplitterAttributes.ATTR_FANOUT, 3);
    final var splitter = new Splitter(Location.create(0, 0, true), attrs);
    final var add = new CircuitMutation(circuit);
    add.add(splitter);
    add.execute();
    final var model = new AttrTableComponentModel(project, circuit, splitter);
    final var row = model.getRow(model.getRowCount() - 1);
    assertInstanceOf(SplitterDistributionRow.class, row);
    assertTrue(row.isValueEditable());
    final var otherProject = new Project(LogisimFile.createNew(new Loader(null), null));
    final var foreignModel = new AttrTableComponentModel(otherProject, circuit, splitter);
    assertFalse(foreignModel.getRow(foreignModel.getRowCount() - 1).isValueEditable());
    final var pin = Pin.FACTORY.createComponent(Location.create(40, 0, true),
        Pin.FACTORY.createAttributeSet());
    final var pinModel = new AttrTableComponentModel(project, circuit, pin);
    for (var i = 0; i < pinModel.getRowCount(); i++) {
      assertFalse(pinModel.getRow(i) instanceof SplitterDistributionRow);
    }
  }
}
