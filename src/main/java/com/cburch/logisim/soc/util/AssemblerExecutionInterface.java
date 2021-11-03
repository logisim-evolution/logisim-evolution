/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.util;

import com.cburch.logisim.circuit.CircuitState;
import java.util.ArrayList;

public interface AssemblerExecutionInterface {
  boolean execute(Object processorState, CircuitState circuitState);
  String getAsmInstruction();
  int getBinInstruction();
  boolean setAsmInstruction(AssemblerAsmInstruction instruction);
  boolean setBinInstruction(int instr);
  boolean performedJump();
  boolean isValid();
  String getErrorMessage();
  ArrayList<String> getInstructions();
  int getInstructionSizeInBytes(String instruction);
}
