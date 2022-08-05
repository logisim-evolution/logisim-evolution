/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.rv32im;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import java.util.ArrayList;
import java.util.Arrays;

public class RV32imEnvironmentCallAndBreakpoints implements AssemblerExecutionInterface {

  private static final int SYSTEM = 0x73;

  private static final int INSTR_ECALL = 0;
  private static final int INSTR_EBREAK = 1;

  private static final String[] AsmOpcodes = {"ECALL", "EBREAK"};

  private int instruction = 0;
  private int operation;
  private boolean valid;

  @Override
  public ArrayList<String> getInstructions() {
    return new ArrayList<>(Arrays.asList(AsmOpcodes));
  }

  @Override
  public boolean execute(Object state, CircuitState cState) {
    if (!valid) return false;
    OptionPane.showMessageDialog(null, S.get("Rv32imECABNotImplmented"));
    return true;
  }

  @Override
  public String getAsmInstruction() {
    if (!valid) return null;
    return AsmOpcodes[operation].toLowerCase();
  }

  @Override
  public int getBinInstruction() {
    return instruction;
  }

  @Override
  public boolean setBinInstruction(int instr) {
    instruction = instr;
    valid = decodeBin();
    return valid;
  }

  @Override
  public boolean performedJump() {
    return false;
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  private boolean decodeBin() {
    if (RV32imSupport.getOpcode(instruction) == SYSTEM) {
      int funct12 = (instruction >> 20) & 0xFFF;
      if (funct12 > 1) return false;
      operation = funct12;
      return true;
    }
    return false;
  }

  @Override
  public String getErrorMessage() {
    return null;
  }

  @Override
  public int getInstructionSizeInBytes(String instruction) {
    if (getInstructions().contains(instruction.toUpperCase())) return 4;
    return -1;
  }

  @Override
  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    int operation = -1;
    for (int i = 0; i < AsmOpcodes.length; i++)
      if (AsmOpcodes[i].equals(instr.getOpcode().toUpperCase())) operation = i;
    if (operation < 0) {
      valid = false;
      return false;
    }
    if (instr.getNrOfParameters() != 0) {
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedNoArguments"));
      valid = false;
      return true;
    }
    instruction = RV32imSupport.getITypeInstruction(SYSTEM, 0, 0, 0, operation);
    valid = true;
    instr.setInstructionByteCode(instruction, 4);
    return true;
  }
}
