/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.sim;

import com.cburch.logisim.vhdl.base.VhdlParser;
import java.util.List;

/** Names and ports needed to connect one VHDL entity instance to the simulation bridge. */
record VhdlSimulatorComponent(
    String entityName, String simulationName, List<VhdlParser.PortDescription> ports) {

  VhdlSimulatorComponent {
    ports = List.copyOf(ports);
  }
}
