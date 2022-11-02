/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.file.ElfProgramHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader;

public interface SocProcessorInterface {

  void setEntryPointandReset(
      CircuitState state, long entryPoint, ElfProgramHeader progInfo, ElfSectionHeader sectInfo);

  void insertTransaction(SocBusTransaction trans, boolean hidden, CircuitState cState);

  int getEntryPoint(CircuitState cState);
}
