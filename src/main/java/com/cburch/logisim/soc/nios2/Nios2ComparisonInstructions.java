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

public class Nios2ComparisonInstructions implements AssemblerExecutionInterface {

  private static final int INSTR_CMPEQ = 0;
  private static final int INSTR_CMPNE = 1;
  private static final int INSTR_CMPGE = 2;
  private static final int INSTR_CMPGEU = 3;
  private static final int INSTR_CMPLT = 4;
  private static final int INSTR_CMPLTU = 5;
  private static final int INSTR_CMPGT = 6;
  private static final int INSTR_CMPGTU = 7;
  private static final int INSTR_CMPLE = 8;
  private static final int INSTR_CMPLEU = 9;
  private static final int INSTR_CMPEQI = 10;
  private static final int INSTR_CMPNEI = 11;
  private static final int INSTR_CMPGEI = 12;
  private static final int INSTR_CMPGEUI = 13;
  private static final int INSTR_CMPLTI = 14;
  private static final int INSTR_CMPLTUI = 15;
  private static final int INSTR_CMPGTI = 16;
  private static final int INSTR_CMPGTUI = 17;
  private static final int INSTR_CMPLEI = 18;
  private static final int INSTR_CMPLEUI = 19;

  private static final int SIGN_EXTEND = 0x100;
  private static final int PSEUDO_INSTR = 0x200;

  private static final String[] AsmOpcodes = {
      "CMPEQ", "CMPNE", "CMPGE", "CMPGEU", "CMPLT", "CMPLTU",
      "CMPGT", "CMPGTU", "CMPLE", "CMPLEU",
      "CMPEQI", "CMPNEI", "CMPGEI", "CMPGEUI", "CMPLTI", "CMPLTUI",
      "CMPGTI", "CMPGTUI", "CMPLEI", "CMPLEUI"};
  private static final Integer[] AsmOpcs = {
      0x3A, 0x3A, 0x3A, 0x3A, 0x3A, 0x3A,
      PSEUDO_INSTR, PSEUDO_INSTR, PSEUDO_INSTR, PSEUDO_INSTR,
      0x20, 0x18, 0x08, 0x28, 0x10, 0x30,
      PSEUDO_INSTR, PSEUDO_INSTR, PSEUDO_INSTR, PSEUDO_INSTR};
  private static final Integer[] AsmOpxs = {
      0x20, 0x18, 0x08, 0x28, 0x10, 0x30,
      SIGN_EXTEND, -1, SIGN_EXTEND, -1,
      SIGN_EXTEND, SIGN_EXTEND, SIGN_EXTEND, -1, SIGN_EXTEND, -1,
      SIGN_EXTEND, -1, SIGN_EXTEND, -1};

  /* pseudo instructions
   * cmpgt rC, rA, rB      => cmplt rC, rB, rA
   * cmpgti rB, rA, IMMED  => cmpgei rB, rA, (IMMED+1)
   * cmpgtu rC, rA, rB     => cmpltu rC, rB, rA
   * cmpgtui rB, rA, IMMED => cmpgeui rB, rA, (IMMED+1)
   * cmple rC, rA, rB      => cmpge rC, rB, rA
   * cmplei rB, rA, IMMED  => cmplti rB, rA, (IMMED+1)
   * cmpleu rC, rA, rB     => cmpgeu rC, rB, rA
   * cmpleui rB, rA, IMMED => cmpltui rB, rA, (IMMED+1)
   */

  private final ArrayList<String> Opcodes = new ArrayList<>();
  private final ArrayList<Integer> OpcCodes = new ArrayList<>();
  private final ArrayList<Integer> OpxCodes = new ArrayList<>();

  private int instruction;
  private boolean valid;
  private int operation;
  private int destination;
  private int immediate;
  private int sourceA;
  private int sourceB;

  public Nios2ComparisonInstructions() {
    for (int i = 0; i < AsmOpcodes.length; i++) {
      Opcodes.add(AsmOpcodes[i].toLowerCase());
      OpcCodes.add(AsmOpcs[i]);
      OpxCodes.add(AsmOpxs[i]);
    }
  }

  @SuppressWarnings("fallthrough")
  public boolean execute(Object processorState, CircuitState circuitState) {
    if (!valid) return false;
    Nios2State.ProcessorState cpuState = (Nios2State.ProcessorState) processorState;
    int valueA = cpuState.getRegisterValue(sourceA);
    int valueB = cpuState.getRegisterValue(sourceB);
    int imm =
        OpxCodes.get(operation) != SIGN_EXTEND ? immediate & 0xFFFF : ((immediate << 16) >> 16);
    int result = 0;
    switch (operation) {
      case INSTR_CMPEQI:
        valueB = imm;
        // fall through
      case INSTR_CMPEQ:
        result = (valueA == valueB) ? 1 : 0;
        break;
      case INSTR_CMPNEI:
        valueB = imm;
        // fall through
      case INSTR_CMPNE:
        result = (valueA != valueB) ? 1 : 0;
        break;
      case INSTR_CMPGEI:
        valueB = imm;
        // fall through
      case INSTR_CMPGE:
        result = (valueA >= valueB) ? 1 : 0;
        break;
      case INSTR_CMPGEUI:
        valueB = imm;
        // fall through
      case INSTR_CMPGEU:
        long opA = SocSupport.convUnsignedInt(valueA);
        long opB = SocSupport.convUnsignedInt(valueB);
        result = (opA >= opB) ? 1 : 0;
        break;
      case INSTR_CMPLTI:
        valueB = imm;
        // fall through
      case INSTR_CMPLT:
        result = (valueA < valueB) ? 1 : 0;
        break;
      case INSTR_CMPLTUI:
        valueB = imm;
        // fall through
      case INSTR_CMPLTU:
        opA = SocSupport.convUnsignedInt(valueA);
        opB = SocSupport.convUnsignedInt(valueB);
        result = (opA < opB) ? 1 : 0;
        break;
      default:
        return false;
    }
    cpuState.writeRegister(destination, result);
    return true;
  }

  public String getAsmInstruction() {
    if (!valid) return null;
    StringBuilder s = new StringBuilder();
    s.append(Opcodes.get(operation));
    while (s.length() < Nios2Support.ASM_FIELD_SIZE) s.append(" ");
    s.append(Nios2State.registerABINames[destination]).append(",");
    s.append(Nios2State.registerABINames[sourceA]).append(",");
    if (operation >= INSTR_CMPEQI) {
      int imm =
          OpxCodes.get(operation) != SIGN_EXTEND ? immediate & 0xFFFF : ((immediate << 16) >> 16);
      s.append(imm);
    } else {
      s.append(Nios2State.registerABINames[sourceB]);
    }
    return s.toString();
  }

  public int getBinInstruction() {
    return instruction;
  }

  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    valid = false;
    if (!Opcodes.contains(instr.getOpcode().toLowerCase())) return false;
    valid = true;
    operation = Opcodes.indexOf(instr.getOpcode().toLowerCase());
    if (instr.getNrOfParameters() != 3) {
      valid = false;
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedThreeArguments"));
      return true;
    }
    valid &= Nios2Support.isCorrectRegister(instr, 0);
    valid &= Nios2Support.isCorrectRegister(instr, 1);
    destination = Nios2Support.getRegisterIndex(instr, 0);
    sourceA = Nios2Support.getRegisterIndex(instr, 1);
    if (operation >= INSTR_CMPEQI) {
      AssemblerToken[] param3 = instr.getParameter(2);
      if (param3.length != 1 || !param3[0].isNumber()) {
        valid = false;
        instr.setError(param3[0], S.getter("AssemblerExpectedImmediateValue"));
      }
      immediate = param3[0].getNumberValue();
      sourceB = 0;
    } else {
      valid &= Nios2Support.isCorrectRegister(instr, 2);
      sourceB = Nios2Support.getRegisterIndex(instr, 2);
      immediate = 0;
    }
    if (!valid) return true;
    if (OpcCodes.get(operation) == PSEUDO_INSTR) {
      switch (operation) {
        case INSTR_CMPGT:
          operation = INSTR_CMPLT;
          int tmp = sourceA;
          sourceA = sourceB;
          sourceB = tmp;
          break;
        case INSTR_CMPGTI:
          operation = INSTR_CMPGEI;
          immediate++;
          break;
        case INSTR_CMPGTU:
          operation = INSTR_CMPLTU;
          tmp = sourceA;
          sourceA = sourceB;
          sourceB = tmp;
          break;
        case INSTR_CMPGTUI:
          operation = INSTR_CMPGEUI;
          immediate++;
          break;
        case INSTR_CMPLE:
          operation = INSTR_CMPGE;
          tmp = sourceA;
          sourceA = sourceB;
          sourceB = tmp;
          break;
        case INSTR_CMPLEI:
          operation = INSTR_CMPLTI;
          immediate++;
          break;
        case INSTR_CMPLEU:
          operation = INSTR_CMPGEU;
          tmp = sourceA;
          sourceA = sourceB;
          sourceB = tmp;
          break;
        case INSTR_CMPLEUI:
          operation = INSTR_CMPLTUI;
          immediate++;
          break;
        default:
          valid = false;
          return false;
      }
    }
    if (OpxCodes.get(operation) == SIGN_EXTEND) {
      if (immediate >= (1 << 15) || immediate < -(1 << 15)) {
        valid = false;
        instr.setError(instr.getParameter(2)[0], S.getter("AssemblerImmediateOutOfRange"));
      }
    } else {
      if (immediate >= (1 << 16) || immediate < 0) {
        valid = false;
        instr.setError(instr.getParameter(2)[0], S.getter("AssemblerImmediateOutOfRange"));
      }
    }
    if (valid) {
      if (operation >= INSTR_CMPEQI)
        instruction =
            Nios2Support.getITypeInstructionCode(
                sourceA, destination, immediate, OpcCodes.get(operation));
      else
        instruction =
            Nios2Support.getRTypeInstructionCode(
                sourceA, sourceB, destination, OpxCodes.get(operation));
      instr.setInstructionByteCode(instruction, 4);
    }
    return true;
  }

  public boolean setBinInstruction(int instr) {
    instruction = instr;
    valid = false;
    int opcode = Nios2Support.getOpcode(instr);
    if (opcode == 0x3A) {
      if (Nios2Support.getOPXImm(instr, Nios2Support.R_TYPE) != 0) return false;
      int opxCode = Nios2Support.getOPXCode(instr, Nios2Support.R_TYPE);
      if (!OpxCodes.contains(opxCode)) return false;
      valid = true;
      operation = OpxCodes.indexOf(opxCode);
      destination = Nios2Support.getRegCIndex(instr, Nios2Support.R_TYPE);
      sourceA = Nios2Support.getRegAIndex(instr, Nios2Support.R_TYPE);
      sourceB = Nios2Support.getRegBIndex(instr, Nios2Support.R_TYPE);
      immediate = 0;
    } else {
      if (!OpcCodes.contains(opcode)) return false;
      valid = true;
      operation = OpcCodes.indexOf(opcode);
      destination = Nios2Support.getRegBIndex(instr, Nios2Support.I_TYPE);
      sourceA = sourceB = Nios2Support.getRegAIndex(instr, Nios2Support.I_TYPE);
      immediate = Nios2Support.getImmediate(instr, Nios2Support.I_TYPE);
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
