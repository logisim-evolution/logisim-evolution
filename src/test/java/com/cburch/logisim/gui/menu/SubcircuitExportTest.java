/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubcircuitExportTest {

  @Test
  void testCollectDependenciesWithSubcircuit() {
    final var loader = new Loader(null);
    final var file = LogisimFile.createNew(loader, null);
    final var proj = new Project(file);

    final var mainCirc = new Circuit("MainCirc", file, proj);
    final var subCirc = new Circuit("SubCirc", file, proj);
    file.addCircuit(mainCirc);
    file.addCircuit(subCirc);

    // Place subCirc inside mainCirc
    final var attrs = subCirc.getSubcircuitFactory().createAttributeSet();
    final var subComp = subCirc.getSubcircuitFactory().createComponent(Location.create(100, 100, false), attrs);
    final var mutation = new com.cburch.logisim.circuit.CircuitMutation(mainCirc);
    mutation.add(subComp);
    mutation.execute();

    final var dependentCircuits = new LinkedHashSet<Circuit>();
    final var dependentVhdl = new LinkedHashSet<VhdlContent>();
    ProjectCircuitActions.collectDependencies(file, mainCirc, dependentCircuits, dependentVhdl);

    assertEquals(1, dependentCircuits.size());
    assertTrue(dependentCircuits.contains(subCirc));
  }
}
