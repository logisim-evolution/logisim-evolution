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

public class Nios2OtherControlInstructions implements AssemblerExecutionInterface {

  private static final int INSTR_TRAP = 0;
  private static final int INSTR_ERET = 1;
  private static final int INSTR_BREAK = 2;
  private static final int INSTR_BRET = 3;
  private static final int INSTR_RDCTL = 4;
  private static final int INSTR_WRCTL = 5;
  private static final int INSTR_FLUSHD = 6;
  private static final int INSTR_FLUSHDA = 7;
  private static final int INSTR_FLUSHI = 8;
  private static final int INSTR_INITD = 9;
  private static final int INSTR_INITDA = 10;
  private static final int INSTR_INITI = 11;
  private static final int INSTR_FLUSHP = 12;
  private static final int INSTR_SYNC = 13;

  private static final int SIGN_EXTEND = 0x100;
  private static final String[] AsmOpcodes = {"TRAP", "ERET", "BREAK", "BRET",
    "RDCTL", "WRCTL", "FLUSHD", "FLUSHDA", "FLUSHI", "INITD", "INITDA", "INITI",
    "FLUSHP", "SYNC" };
  private static final Integer[] AsmOpcs = {0x3A, 0x3A, 0x3A, 0x3A,
    0x3A, 0x3A, 0x3B, 0x1B, 0x3A, 0x33, 0x13, 0x3A,
    0x3A, 0x3A };
  private static final Integer[] AsmOpxs = {0x2D, 0x01, 0x34, 0x09,
    0x26, 0x2E, SIGN_EXTEND, SIGN_EXTEND, 0x0C, SIGN_EXTEND, SIGN_EXTEND, 0x29,
    0x04, 0x36 };

  private final ArrayList<String> Opcodes = new ArrayList<>();
  private final ArrayList<Integer> OpcCodes = new ArrayList<>();
  private final ArrayList<Integer> OpxCodes = new ArrayList<>();

  private int instruction;
  private boolean valid;
  private boolean jumped;
  private int operation;
  private int sourceA;
  private int immediate;

  public Nios2OtherControlInstructions() {
    for (int i = 0; i < AsmOpcodes.length; i++) {
      Opcodes.add(AsmOpcodes[i].toLowerCase());
      OpcCodes.add(AsmOpcs[i]);
      OpxCodes.add(AsmOpxs[i]);
    }
  }

  public boolean execute(Object processorState, CircuitState circuitState) {
    jumped = false;
    if (!valid) return false;
    Nios2State.ProcessorState cpuState = (Nios2State.ProcessorState) processorState;
    long pc = SocSupport.convUnsignedInt(cpuState.getProgramCounter());
    long nextPc = pc + 4;
    switch (operation) {
      case INSTR_TRAP -> {
        cpuState.writeRegister(29, SocSupport.convUnsignedLong(nextPc));
        cpuState.interrupt();
        jumped = true;
      }
      case INSTR_ERET -> {
        cpuState.endofInterrupt();
        jumped = true;
      }
      case INSTR_BREAK -> {
        cpuState.breakReq();
        jumped = true;
      }
      case INSTR_BRET -> {
        cpuState.breakRet();
        jumped = true;
      }
      case INSTR_RDCTL -> cpuState.writeRegister(sourceA, cpuState.getControlRegister(immediate));
      case INSTR_WRCTL ->
          cpuState.setControlRegister(immediate, cpuState.getRegisterValue(sourceA));
      default -> {
      } /* nothing to do in simulation, these are HW dependent operations */
    }
    return true;
  }

  public String getAsmInstruction() {
    if (!valid) return null;
    StringBuilder s = new StringBuilder();
    s.append(Opcodes.get(operation));
    while (s.length() < Nios2Support.ASM_FIELD_SIZE) s.append(" ");
    switch (operation) {
      case INSTR_BREAK:
      case INSTR_TRAP:
        if (immediate != 0) s.append(immediate);
        break;
      case INSTR_RDCTL:
        s.append(Nios2State.registerABINames[sourceA]).append(",ctl").append(immediate);
        break;
      case INSTR_WRCTL:
        s.append("ctl").append(immediate).append(",").append(Nios2State.registerABINames[sourceA]);
        break;
      case INSTR_INITD:
      case INSTR_INITDA:
      case INSTR_FLUSHDA:
      case INSTR_FLUSHD:
        int imm = ((immediate << 16) >> 16);
        s.append(imm).append("(").append(Nios2State.registerABINames[sourceA]).append(")");
        break;
      case INSTR_INITI:
      case INSTR_FLUSHI:
        s.append(Nios2State.registerABINames[sourceA]);
        break;
    }
    return s.toString();
  }

  public int getBinInstruction() {
    return instruction;
  }

  @SuppressWarnings("fallthrough")
  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    valid = false;
    if (!Opcodes.contains(instr.getOpcode().toLowerCase())) return false;
    operation = Opcodes.indexOf(instr.getOpcode().toLowerCase());
    valid = true;
    int first = -1;
    switch (operation) {
      case INSTR_BREAK:
      case INSTR_TRAP:
        if (instr.getNrOfParameters() == 0) {
          immediate = 0;
          sourceA = 0;
        } else if (instr.getNrOfParameters() == 1) {
          AssemblerToken[] imm = instr.getParameter(0);
          if (imm.length != 1 || !imm[0].isNumber()) {
            valid = false;
            instr.setError(imm[0], S.getter("AssemblerExpectedImmediateValue"));
          }
          immediate = imm[0].getNumberValue();
          if (immediate > 0x1F || immediate < 0) {
            valid = false;
            instr.setError(imm[0], S.getter("AssemblerImmediateOutOfRange"));
          }
        } else {
          valid = false;
          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedZeroOrOneArgument"));
        }
        break;
      case INSTR_WRCTL:
        first = 1;
        // fall through
      case INSTR_RDCTL:
        if (instr.getNrOfParameters() != 2) {
          valid = false;
          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
          return true;
        }
        if (first < 0) first = 0;
        valid &= Nios2Support.isCorrectRegister(instr, first);
        sourceA = Nios2Support.getRegisterIndex(instr, first);
        first += 1;
        first &= 1;
        AssemblerToken[] ctl = instr.getParameter(first);
        if (ctl.length != 1 || ctl[0].getType() != Nios2Assembler.CONTROL_REGISTER) {
          valid = false;
          instr.setError(ctl[0], S.getter("Nios2ExpectedControlRegister"));
        }
        immediate = Nios2State.getRegisterIndex(ctl[0].getValue());
        if (immediate < 0 || immediate > 31) {
          valid = false;
          instr.setError(ctl[0], S.getter("AssemblerUnknownRegister"));
        }
        break;
      case INSTR_INITI:
      case INSTR_FLUSHI:
        if (instr.getNrOfParameters() != 1) {
          valid = false;
          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedOneArgument"));
          return true;
        }
        valid &= Nios2Support.isCorrectRegister(instr, 0);
        sourceA = Nios2Support.getRegisterIndex(instr, 0);
        immediate = 0;
        break;
      case INSTR_INITD:
      case INSTR_INITDA:
      case INSTR_FLUSHDA:
      case INSTR_FLUSHD:
        if (instr.getNrOfParameters() != 1) {
          valid = false;
          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedOneArgument"));
          return true;
        }
        AssemblerToken[] ireg = instr.getParameter(0);
        if (ireg.length != 2) {
          valid = false;
          instr.setError(ireg[0], S.getter("Nios2AssemblerExpectedImmediateIndexedRegister"));
          return true;
        }
        if (!ireg[0].isNumber()) {
          valid = false;
          instr.setError(ireg[0], S.getter("AssemblerExpectedImmediateValue"));
        }
        immediate = ireg[0].getNumberValue();
        if (immediate >= (1 << 15) || immediate < -(1 << 15)) {
          valid = false;
          instr.setError(ireg[0], S.getter("AssemblerImmediateOutOfRange"));
        }
        if (ireg[1].getType() != AssemblerToken.BRACKETED_REGISTER) {
          valid = false;
          instr.setError(ireg[1], S.getter("Nios2AssemblerExpectedBracketedRegister"));
          return true;
        }
        if (Nios2State.isCustomRegister(ireg[1].getValue())) {
          valid = false;
          instr.setError(ireg[1], S.getter("Nios2CannotUseCustomRegister"));
          return true;
        }
        if (Nios2State.isControlRegister(ireg[1].getValue())) {
          valid = false;
          instr.setError(ireg[1], S.getter("Nios2CannotUseControlRegister"));
          return true;
        }
        sourceA = Nios2State.getRegisterIndex(ireg[1].getValue());
        if (sourceA < 0 || sourceA > 31) {
          valid = false;
          instr.setError(ireg[1], S.getter("AssemblerUnknownRegister"));
        }
        break;

      default:
        if (instr.getNrOfParameters() != 0) {
          valid = false;
          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedNoArguments"));
        }
        break;
    }

    if (valid) {
      switch (operation) {
        case INSTR_TRAP ->
            instruction = Nios2Support.getRTypeInstructionCode(0, 0, 0x1d, 0x2d, immediate);
        case INSTR_ERET -> instruction = Nios2Support.getRTypeInstructionCode(0x1d, 0x1e, 0, 0x01);
        case INSTR_BREAK ->
            instruction = Nios2Support.getRTypeInstructionCode(0, 0, 0x1e, 0x34, immediate);
        case INSTR_BRET -> instruction = Nios2Support.getRTypeInstructionCode(0x1E, 0, 0x1E, 0x09);
        case INSTR_RDCTL ->
            instruction = Nios2Support.getRTypeInstructionCode(0, 0, sourceA, 0x26, immediate);
        case INSTR_WRCTL ->
            instruction = Nios2Support.getRTypeInstructionCode(sourceA, 0, 0, 0x2E, immediate);
        case INSTR_FLUSHP, INSTR_INITI, INSTR_FLUSHI -> instruction =
            Nios2Support.getRTypeInstructionCode(sourceA, 0, 0, OpxCodes.get(operation));
        case INSTR_SYNC -> instruction = Nios2Support.getRTypeInstructionCode(0, 0, 0, 0x36);
        case INSTR_INITDA, INSTR_INITD, INSTR_FLUSHDA, INSTR_FLUSHD -> instruction =
            Nios2Support.getITypeInstructionCode(sourceA, 0, immediate, OpcCodes.get(operation));
        default -> {
          valid = false;
          return false;
        }
      }
      instr.setInstructionByteCode(instruction, 4);
    }
    return true;
  }

  public boolean setBinInstruction(int instr) {
    valid = false;
    instruction = instr;
    int opcode = Nios2Support.getOpcode(instr);
    if (opcode == 0x3A) {
      int opx = Nios2Support.getOPXCode(instr, Nios2Support.R_TYPE);
      if (!OpxCodes.contains(opx)) return false;
      valid = true;
      operation = OpxCodes.indexOf(opx);
      int ra = Nios2Support.getRegAIndex(instr, Nios2Support.R_TYPE);
      int rb = Nios2Support.getRegBIndex(instr, Nios2Support.R_TYPE);
      int rc = Nios2Support.getRegCIndex(instr, Nios2Support.R_TYPE);
      int imm5 = Nios2Support.getOPXImm(instr, Nios2Support.R_TYPE);
      switch (operation) {
        case INSTR_TRAP:
          if (ra != 0 || rb != 0 || rc != 0x1D) valid = false;
          immediate = imm5;
          break;
        case INSTR_ERET:
          if (ra != 0x1d || rb != 0x1e || rc != 0 || imm5 != 0) valid = false;
          break;
        case INSTR_BREAK:
          if (ra != 0 || rb != 0 || rc != 0x1E) valid = false;
          immediate = imm5;
          break;
        case INSTR_BRET:
          if (ra != 0x1E || rb != 0 || rc != 0x1E || imm5 != 0) valid = false;
          break;
        case INSTR_RDCTL:
          if (ra != 0 || rb != 0) valid = false;
          sourceA = rc;
          immediate = imm5;
          break;
        case INSTR_WRCTL:
          if (rb != 0 || rc != 0) valid = false;
          sourceA = ra;
          immediate = imm5;
          break;
        case INSTR_FLUSHP:
        case INSTR_INITI:
        case INSTR_FLUSHI:
          if (rb != 0 || rc != 0 || imm5 != 0) valid = false;
          sourceA = ra;
          break;
        case INSTR_SYNC:
          if (ra != 0 || rb != 0 || rc != 0 || imm5 != 0) valid = false;
          break;
        default:
          valid = false;
          break;
      }
    } else {
      if (!OpcCodes.contains(opcode)) return false;
      operation = OpcCodes.indexOf(opcode);
      valid = true;
      switch (operation) {
        case INSTR_INITDA, INSTR_INITD, INSTR_FLUSHDA, INSTR_FLUSHD -> {
          if (Nios2Support.getRegBIndex(instr, Nios2Support.I_TYPE) != 0)
            valid = false;
          sourceA = Nios2Support.getRegAIndex(instr, Nios2Support.I_TYPE);
          immediate = Nios2Support.getImmediate(instr, Nios2Support.I_TYPE);
        }
        default -> valid = false;
      }
    }
    return valid;
  }

  public boolean performedJump() {
    return jumped && valid;
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
