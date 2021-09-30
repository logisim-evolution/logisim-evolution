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

public class Rv32imMemoryOrderingInstructions implements AssemblerExecutionInterface {

  private static final int FENCE = 0xF;

  private static final int I_FLAG = 8;
  private static final int O_FLAG = 4;
  private static final int R_FLAG = 2;
  private static final int W_FLAG = 1;

  private static final int INSTR_FENCE = 0;
  private static final int INSTR_FENCE_TSO = 1;

  private static final String[] AsmOpcodes = {"FENCE", "FENCE.TSO"};

  private int instruction = 0;
  private boolean valid = false;
  private int succ;
  private int pred;
  private int fm;
  private int operation;

  @Override
  public ArrayList<String> getInstructions() {
    ArrayList<String> opcodes = new ArrayList<>(Arrays.asList(AsmOpcodes));
    return opcodes;
  }

  @Override
  public boolean execute(Object state, CircuitState cState) {
    if (!valid)
      return false;
    OptionPane.showMessageDialog(null, S.get("Rv32imMOINotImplmented"));
    return true;
  }

  @Override
  public String getAsmInstruction() {
    if (!valid) return null;
    final var s = new StringBuilder();
    s.append(AsmOpcodes[operation].toLowerCase());
    if (operation != INSTR_FENCE_TSO) {
      while (s.length() < RV32imSupport.ASM_FIELD_SIZE)
        s.append(" ");
      addMasks(s, succ);
      s.append(",");
      addMasks(s, pred);
    }
    return s.toString();
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
    if (RV32imSupport.getOpcode(instruction) == FENCE) {
      if (RV32imSupport.getFunct3(instruction) != 0)
        return false;
      succ = (instruction >> 20) & 0xF;
      pred = (instruction >> 24) & 0xF;
      fm = (instruction >> 28) & 0xF;
      int rwmask = R_FLAG | W_FLAG;
      if (fm == 8 && succ == rwmask && pred == rwmask)
        operation = INSTR_FENCE_TSO;
      else
        operation = INSTR_FENCE;
      return true;
    }
    return false;
  }

  private void addMasks(StringBuilder buffer, int value) {
    if ((value & I_FLAG) != 0) buffer.append("i");
    if ((value & O_FLAG) != 0) buffer.append("o");
    if ((value & R_FLAG) != 0) buffer.append("r");
    if ((value & W_FLAG) != 0) buffer.append("w");
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
    instr.setError(instr.getInstruction(), S.getter("RV32imAssemblerNotSupportedYet"));
    valid = false;
    return true;
  }
}
