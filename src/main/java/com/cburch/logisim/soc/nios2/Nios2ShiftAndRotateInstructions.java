/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.nios2;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;
import java.util.ArrayList;

public class Nios2ShiftAndRotateInstructions implements AssemblerExecutionInterface {

  private static final int INSTR_ROL = 0;
  private static final int INSTR_ROR = 1;
  private static final int INSTR_SLL = 2;
  private static final int INSTR_SRA = 3;
  private static final int INSTR_SRL = 4;
  private static final int INSTR_ROLI = 5;
  private static final int INSTR_SLLI = 6;
  private static final int INSTR_SRAI = 7;
  private static final int INSTR_SRLI = 8;

  private static final String[] AsmOpcodes = {
      "ROL", "ROR", "SLL", "SRA", "SRL",
      "ROLI", "SLLI", "SRAI", "SRLI" };
  private static final Integer[] AsmOpxs = {
      0x03, 0x0B, 0x13, 0x3B, 0x1B,
      0x02, 0x12, 0x3A, 0x1A };

  private final ArrayList<String> Opcodes = new ArrayList<>();
  private final ArrayList<Integer> OpxCodes = new ArrayList<>();

  private int instruction;
  private boolean valid;
  private int operation;
  private int immediate;
  private int sourceA;
  private int sourceB;
  private int sourceC;

  public Nios2ShiftAndRotateInstructions() {
    for (int i = 0; i < AsmOpcodes.length; i++) {
      Opcodes.add(AsmOpcodes[i].toLowerCase());
      OpxCodes.add(AsmOpxs[i]);
    }
  }

  @SuppressWarnings("fallthrough")
  public boolean execute(Object processorState, CircuitState circuitState) {
    if (!valid) return false;
    Nios2State.ProcessorState cpuState = (Nios2State.ProcessorState) processorState;
    int imm = cpuState.getRegisterValue(sourceB) & 0x1F;
    int valueA = cpuState.getRegisterValue(sourceA);
    int result = 0;
    switch (operation) {
      case INSTR_ROLI:
        imm = immediate & 0x1F;
        // fall through
      case INSTR_ROL:
        long opp = SocSupport.convUnsignedInt(valueA) << imm;
        opp |= (opp >> 32);
        result = SocSupport.convUnsignedLong(opp);
        break;
      case INSTR_ROR:
        opp = SocSupport.convUnsignedInt(valueA) << (32 - imm);
        opp |= (opp >> 32);
        result = SocSupport.convUnsignedLong(opp);
        break;
      case INSTR_SLLI:
        imm = immediate & 0x1F;
        // fall through
      case INSTR_SLL:
        result = valueA << imm;
        break;
      case INSTR_SRAI:
        imm = immediate & 0x1F;
        // fall through
      case INSTR_SRA:
        result = valueA >> imm;
        break;
      case INSTR_SRLI:
        imm = immediate & 0x1F;
        // fall through
      case INSTR_SRL:
        long opA = SocSupport.convUnsignedInt(valueA);
        opA >>= imm;
        result = SocSupport.convUnsignedLong(opA);
        break;
      default:
        return false;
    }
    cpuState.writeRegister(sourceC, result);
    return true;
  }

  public String getAsmInstruction() {
    if (!valid) return null;
    StringBuilder s = new StringBuilder();
    s.append(Opcodes.get(operation));
    while (s.length() < Nios2Support.ASM_FIELD_SIZE) s.append(" ");
    s.append(Nios2State.registerABINames[sourceC]).append(",");
    s.append(Nios2State.registerABINames[sourceA]).append(",");
    if (operation < INSTR_ROLI) {
      s.append(Nios2State.registerABINames[sourceB]);
    } else {
      s.append(immediate);
    }
    return s.toString();
  }

  public int getBinInstruction() {
    return instruction;
  }

  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    valid = false;
    if (!Opcodes.contains(instr.getOpcode().toLowerCase())) return false;
    if (instr.getNrOfParameters() != 3) {
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedThreeArguments"));
      return false;
    }
    valid = true;
    operation = Opcodes.indexOf(instr.getOpcode().toLowerCase());
    valid &= Nios2Support.isCorrectRegister(instr, 0);
    sourceC = Nios2Support.getRegisterIndex(instr, 0);
    valid &= Nios2Support.isCorrectRegister(instr, 1);
    sourceA = Nios2Support.getRegisterIndex(instr, 1);
    if (operation >= INSTR_ROLI) {
      sourceB = 0;
      AssemblerToken[] param3 = instr.getParameter(2);
      if (param3.length != 1 || !param3[0].isNumber()) {
        valid = false;
        instr.setError(param3[0], S.getter("AssemblerExpectedImmediateValue"));
      }
      immediate = param3[0].getNumberValue();
      if (immediate > 31 || immediate < 0) {
        valid = false;
        instr.setError(param3[0], S.getter("AssemblerImmediateOutOfRange"));
      }
    } else {
      immediate = 0;
      valid &= Nios2Support.isCorrectRegister(instr, 2);
      sourceB = Nios2Support.getRegisterIndex(instr, 2);
    }
    if (valid) {
      instruction =
          Nios2Support.getRTypeInstructionCode(
              sourceA, sourceB, sourceC, OpxCodes.get(operation), immediate);
      instr.setInstructionByteCode(instruction, 4);
    }
    return true;
  }

  public boolean setBinInstruction(int instr) {
    instruction = instr;
    valid = false;
    if (Nios2Support.getOpcode(instr) != 0x3A) return false;
    valid = true;
    sourceA = Nios2Support.getRegAIndex(instr, Nios2Support.R_TYPE);
    sourceB = Nios2Support.getRegBIndex(instr, Nios2Support.R_TYPE);
    sourceC = Nios2Support.getRegCIndex(instr, Nios2Support.R_TYPE);
    immediate = Nios2Support.getOPXImm(instr, Nios2Support.R_TYPE);
    int opx = Nios2Support.getOPXCode(instr, Nios2Support.R_TYPE);
    if (!OpxCodes.contains(opx)) {
      valid = false;
    }
    operation = OpxCodes.indexOf(opx);
    if ((operation < INSTR_ROLI && immediate != 0) || (operation >= INSTR_ROLI && sourceB != 0)) {
      valid = false;
    }
    return valid;
  }

  public boolean performedJump() {
    return false;
  }

  public boolean isValid() {
    return valid;
  }

  public String getErrorMessage() {
    return null;
  }

  public ArrayList<String> getInstructions() {
    return Opcodes;
  }

  public int getInstructionSizeInBytes(String instruction) {
    if (Opcodes.contains(instruction.toLowerCase())) return 4;
    return -1;
  }
}
