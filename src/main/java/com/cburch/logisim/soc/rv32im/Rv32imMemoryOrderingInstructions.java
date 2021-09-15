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

  public ArrayList<String> getInstructions() {
    ArrayList<String> opcodes = new ArrayList<>(Arrays.asList(AsmOpcodes));
    return opcodes;
  }

  public boolean execute(Object state, CircuitState cState) {
    if (!valid)
      return false;
    OptionPane.showMessageDialog(null, S.get("Rv32imMOINotImplmented"));
    return true;
  }

  public String getAsmInstruction() {
    if (!valid)
      return null;
    StringBuffer s = new StringBuffer();
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

  public int getBinInstruction() {
    return instruction;
  }

  public boolean setBinInstruction(int instr) {
    instruction = instr;
    valid = decodeBin();
    return valid;
  }

  public boolean performedJump() {
    return false;
  }

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

  private void addMasks(StringBuffer s, int value) {
    if ((value & I_FLAG) != 0) s.append("i");
    if ((value & O_FLAG) != 0) s.append("o");
    if ((value & R_FLAG) != 0) s.append("r");
    if ((value & W_FLAG) != 0) s.append("w");
  }

  public String getErrorMessage() {
    return null;
  }

  public int getInstructionSizeInBytes(String instruction) {
    if (getInstructions().contains(instruction.toUpperCase())) return 4;
    return -1;
  }

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
