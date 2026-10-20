/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.util;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.file.ElfProgramHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public interface AssemblerInterface {
  void decode(int instruction);

  boolean assemble(AssemblerAsmInstruction instruction);

  AssemblerExecutionInterface getExeUnit();

  ArrayList<String> getOpcodes();

  int getInstructionSize(String opcode);

  boolean usesRoundedBrackets();

  String getProgram(
      CircuitState circuitState,
      SocProcessorInterface processorInterface,
      ElfProgramHeader elfHeader,
      ElfSectionHeader elfSections,
      HashMap<Integer, Integer> validDebugLines);

  String getHighlightStringIdentifier();

  void performUpSpecificOperationsOnTokens(LinkedList<AssemblerToken> tokens);

  HashSet<Integer> getAcceptedParameterTypes();
}
