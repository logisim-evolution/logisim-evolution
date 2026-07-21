/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.sim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class VhdlSimulatorScriptTest {

  @Test
  void runScriptRebuildsDedicatedPhysicalWorkLibrary() throws IOException {
    final var resource =
        VhdlSimulatorScriptTest.class.getResourceAsStream("/resources/logisim/sim/run.tcl");
    assertNotNull(resource);

    final var script = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(script.contains("set logisim_worklib logisim_work"));
    assertTrue(script.contains("file delete -force $logisim_worklib"));
    assertTrue(script.contains("vlib $logisim_worklib"));
    assertTrue(script.contains("vmap work $logisim_worklib"));
    assertFalse(script.contains("vlib work\nvmap work work"));
  }
}
