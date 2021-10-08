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
import com.cburch.logisim.soc.file.ElfHeader;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;
import java.util.ArrayList;
import java.util.Arrays;

public class RV32imIntegerRegisterImmediateInstructions implements AssemblerExecutionInterface {

  private static final int OP_IMM = 0x13;
  private static final int LUI = 0x37;
  private static final int AUIPC = 0x17;
  private static final int ADDI = 0;
  private static final int XORI = 4;
  private static final int SLTIU = 3;
  private static final int SLLI = 1;
  private static final int SRLAI = 5;

  private static final int INSTR_ADDI = 0;
  private static final int INSTR_SLTI = 2;
  private static final int INSTR_SLTIU = 3;
  private static final int INSTR_XORI = 4;
  private static final int INSTR_ORI = 6;
  private static final int INSTR_ANDI = 7;
  private static final int INSTR_SLLI = 1;
  private static final int INSTR_SRLI = 5;
  private static final int INSTR_SRAI = 8;
  private static final int INSTR_LUI = 9;
  private static final int INSTR_AUIPC = 10;
  private static final int INSTR_NOP = 11;
  private static final int INSTR_LI = 12;
  private static final int INSTR_SEQZ = 13;
  private static final int INSTR_NOT = 14;
  private static final int INSTR_MV = 15;

  private static final String[] AsmOpcodes = {
      "ADDI", "SLLI", "SLTI", "SLTIU", "XORI", "SRLI", "ORI",
      "ANDI", "SRAI", "LUI", "AUIPC", "NOP", "LI", "SEQZ", "NOT", "MV"};
  /* pseudo instructions:
   * NOP -> ADDI r0,r0,0
   * LI rd,imm -> ADDI rd,r0,imm
   * SEQZ rd,rs -> SLTIU rd,rs,1
   * NOT rd,rs -> XORI rd,rs,-1
   * MV  rd,rs -> ADDI rd,rs,0
   */
  private int instruction = 0;
  private int destination;
  private int source;
  private int immediate;
  private int operation;
  private boolean valid = false;

  @Override
  public ArrayList<String> getInstructions() {
    return new ArrayList<>(Arrays.asList(AsmOpcodes));
  }

  @Override
  public boolean execute(Object state, CircuitState cState) {
    if (!valid) return false;
    RV32imState.ProcessorState cpuState = (RV32imState.ProcessorState) state;
    int result = 0;
    int regVal = cpuState.getRegisterValue(source);
    switch (operation) {
      case INSTR_LI:
      case INSTR_NOP:
      case INSTR_MV:
      case INSTR_ADDI:
        result = regVal + immediate;
        break;
      case INSTR_SLTI:
        result = (regVal < immediate) ? 1 : 0;
        break;
      case INSTR_SEQZ:
      case INSTR_SLTIU:
        result = (ElfHeader.getLongValue(regVal) < ElfHeader.getLongValue(immediate)) ? 1 : 0;
        break;
      case INSTR_NOT:
      case INSTR_XORI:
        result = regVal ^ immediate;
        break;
      case INSTR_ORI:
        result = regVal | immediate;
        break;
      case INSTR_ANDI:
        result = regVal & immediate;
        break;
      case INSTR_SLLI:
        result = regVal << immediate;
        break;
      case INSTR_SRLI:
        long val1 = ElfHeader.getLongValue(regVal);
        val1 >>= immediate;
        result = ElfHeader.getIntValue(val1);
        break;
      case INSTR_SRAI:
        result = regVal >> immediate;
        break;
      case INSTR_LUI:
        result = immediate;
        break;
      case INSTR_AUIPC:
        long pc = Long.parseUnsignedLong(String.format("%08X", cpuState.getProgramCounter()), 16);
        result = ElfHeader.getIntValue(pc + immediate);
        break;
      default:
        return false;
    }
    cpuState.writeRegister(destination, result);
    return true;
  }

  @Override
  public String getAsmInstruction() {
    if (!valid)
      return "Unknown";
    StringBuilder s = new StringBuilder();
    s.append(AsmOpcodes[operation].toLowerCase());
    while (s.length() < RV32imSupport.ASM_FIELD_SIZE)
        s.append(" ");

    switch (operation) {
      case INSTR_NOP:
        break;
      case INSTR_LI:
        s.append(RV32imState.registerABINames[destination]).append(",").append(immediate);
        break;
      case INSTR_LUI:
      case INSTR_AUIPC:
        s.append(RV32imState.registerABINames[destination])
            .append(",")
            .append((immediate >> 12) & 0xFFFFF);
        break;
      case INSTR_MV:
      case INSTR_NOT:
      case INSTR_SEQZ:
        s.append(RV32imState.registerABINames[destination])
            .append(",")
            .append(RV32imState.registerABINames[source]);
        break;
      default:
        s.append(RV32imState.registerABINames[destination])
            .append(",")
            .append(RV32imState.registerABINames[source])
            .append(",")
            .append(immediate);
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
    decodeBin();
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

  private void decodeBin() {
    int opcode = RV32imSupport.getOpcode(instruction);
    switch (opcode) {
      case LUI:
      case AUIPC:
        valid = true;
        destination = RV32imSupport.getDestinationRegisterIndex(instruction);
        immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.U_TYPE);
        operation = (opcode == LUI) ? INSTR_LUI : INSTR_AUIPC;
        return;
      case OP_IMM:
        valid = true;
        source = RV32imSupport.getSourceRegister1Index(instruction);
        destination = RV32imSupport.getDestinationRegisterIndex(instruction);
        immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.I_TYPE);
        break;
      default:
        valid = false;
        return;
    }
    /* we land up here only in case of a OP_IMM opcode */
    int op3 = RV32imSupport.getFunct3(instruction);
    if (op3 != SRLAI) {
      operation = op3;
      if (op3 == SLLI) {
        if (RV32imSupport.getFunct7(instruction) != 0) {
          valid = false;
          return;
        }
        immediate = (instruction >> 20) & 0x1F;
      }
      if (op3 == ADDI) {
        if (destination == 0 && source == 0 && immediate == 0)
          operation = INSTR_NOP;
        else if (source == 0)
          operation = INSTR_LI;
        else if (immediate == 0)
          operation = INSTR_MV;
      }
      if (op3 == SLTIU) {
        if (immediate == 1)
          operation = INSTR_SEQZ;
      }
      if (op3 == XORI) {
        if (immediate == -1)
          operation = INSTR_NOT;
      }
      return;
    }
    /* we land up here only in case of a SRLI or SRAI instruction */
    int funct7 = RV32imSupport.getFunct7(instruction);
    if (funct7 == 0 || funct7 == 0x20) {
      int bit30 = (instruction >> 30) & 1;
      operation = op3 + bit30 * 3;
      immediate = (instruction >> 20) & 0x1F;
      return;
    }
    valid = false;
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
      if (AsmOpcodes[i].equals(instr.getOpcode().toUpperCase()))
          operation = i;
    if (operation < 0) {
      valid = false;
      return false;
    }
    boolean errors = false;
    AssemblerToken[] param1, param2, param3;
    switch (operation) {
      case INSTR_NOP:
        if (instr.getNrOfParameters() != 0) {
          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedNoArguments"));
          errors = true;
          break;
        }
        operation = INSTR_ADDI;
        destination = source = immediate = 0;
        break;
      case INSTR_LUI:
      case INSTR_AUIPC:
      case INSTR_LI: /* format opcode rd,#imm */
        if (instr.getNrOfParameters() != 2) {
          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
          errors = true;
          break;
        }
        param1 = instr.getParameter(0);
        param2 = instr.getParameter(1);
        if (param1.length != 1 || param1[0].getType() != AssemblerToken.REGISTER) {
          for (AssemblerToken assemblerToken : param1)
            instr.setError(assemblerToken, S.getter("AssemblerExpectedRegister"));
          errors = true;
          break;
        }
        if (param2.length != 1 || !param2[0].isNumber()) {
          for (AssemblerToken assemblerToken : param2)
            instr.setError(assemblerToken, S.getter("AssemblerExpectedImmediateValue"));
          errors = true;
          break;
        }
        if (operation == INSTR_LI) operation = INSTR_ADDI;
        source = 0;
        destination = RV32imState.getRegisterIndex(param1[0].getValue());
        if (destination < 0 || destination > 31) {
          errors = true;
          instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
        }
        immediate = param2[0].getNumberValue();
        if (operation == INSTR_LUI && param2[0].isLabel()) immediate = (immediate >> 12) & 0xFFFFF;
        if (operation == INSTR_AUIPC && param2[0].isLabel()) {
          long imm = immediate;
          imm -= instr.getProgramCounter();
          immediate = (int) imm;
          immediate = (immediate >> 12) & 0xFFFFF;
        }
        break;
      case INSTR_MV:
      case INSTR_NOT:
      case INSTR_SEQZ: /* format opcode rd,rs */
        if (instr.getNrOfParameters() != 2) {
          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
          errors = true;
          break;
        }
        param1 = instr.getParameter(0);
        param2 = instr.getParameter(1);
        if (param1.length != 1 || param1[0].getType() != AssemblerToken.REGISTER) {
          for (AssemblerToken assemblerToken : param1)
            instr.setError(assemblerToken, S.getter("AssemblerExpectedRegister"));
          errors = true;
          break;
        }
        if (param2.length != 1 || param2[0].getType() != AssemblerToken.REGISTER) {
          for (AssemblerToken assemblerToken : param2)
            instr.setError(assemblerToken, S.getter("AssemblerExpectedRegister"));
          errors = true;
          break;
        }
        immediate = -1;
        source = RV32imState.getRegisterIndex(param2[0].getValue());
        if (source < 0 || source > 31) {
          errors = true;
          instr.setError(param2[0], S.getter("AssemblerUnknownRegister"));
        }
        destination = RV32imState.getRegisterIndex(param1[0].getValue());
        if (destination < 0 || destination > 31) {
          errors = true;
          instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
        }
        switch (operation) {
          case INSTR_MV:
            immediate = 0;
            operation = INSTR_ADDI;
            break;
          case INSTR_NOT:
            operation = INSTR_XORI;
            break;
          default:
            operation = INSTR_SLTIU;
            immediate = 1;
            break;
        }
        break;
      default: /* format: opcode rd,rs,#imm */
        if (instr.getNrOfParameters() != 3) {
          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedThreeArguments"));
          errors = true;
          break;
        }
        param1 = instr.getParameter(0);
        param2 = instr.getParameter(1);
        param3 = instr.getParameter(2);
        if (param1.length != 1 || param1[0].getType() != AssemblerToken.REGISTER) {
          for (AssemblerToken assemblerToken : param1)
            instr.setError(assemblerToken, S.getter("AssemblerExpectedRegister"));
          errors = true;
          break;
        }
        if (param2.length != 1 || param2[0].getType() != AssemblerToken.REGISTER) {
          for (AssemblerToken assemblerToken : param2)
            instr.setError(assemblerToken, S.getter("AssemblerExpectedRegister"));
          errors = true;
          break;
        }
        if (param3.length != 1 || !param3[0].isNumber()) {
          for (AssemblerToken assemblerToken : param3)
            instr.setError(assemblerToken, S.getter("AssemblerExpectedImmediateValue"));
          errors = true;
          break;
        }
        immediate = param3[0].getNumberValue();
        source = RV32imState.getRegisterIndex(param2[0].getValue());
        if (source < 0 || source > 31) {
          errors = true;
          instr.setError(param2[0], S.getter("AssemblerUnknownRegister"));
        }
        destination = RV32imState.getRegisterIndex(param1[0].getValue());
        if (destination < 0 || destination > 31) {
          errors = true;
          instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
        }
        break;
    }

    if (!errors) {
      switch (operation) {
        case INSTR_ADDI:
        case INSTR_SLTI:
        case INSTR_SLTIU:
        case INSTR_ANDI:
        case INSTR_ORI:
        case INSTR_XORI:
          if (immediate > 2047 || immediate < -2048) {
            errors = true;
            instr.setError(
                instr.getParameter(instr.getNrOfParameters() - 1)[0],
                S.getter("AssemblerImmediateOutOfRange"));
            break;
          }
          instruction =
              RV32imSupport.getITypeInstruction(OP_IMM, destination, operation, source, immediate);
          break;
        case INSTR_SLLI:
        case INSTR_SRLI:
        case INSTR_SRAI:
          if (immediate > 31 || immediate < 0) {
            errors = true;
            instr.setError(
                instr.getParameter(instr.getNrOfParameters() - 1)[0],
                S.getter("AssemblerImmediateOutOfRange"));
            break;
          }
          if (operation == INSTR_SRAI) {
            immediate |= 1 << 10;
            operation = INSTR_SRLI;
          }
          instruction =
              RV32imSupport.getITypeInstruction(OP_IMM, destination, operation, source, immediate);
          break;
        case INSTR_LUI:
        case INSTR_AUIPC:
          if (immediate < 0 || immediate >= (1 << 20)) {
            errors = true;
            instr.setError(
                instr.getParameter(instr.getNrOfParameters() - 1)[0],
                S.getter("AssemblerImmediateOutOfRange"));
            break;
          }
          int opcode = operation == INSTR_LUI ? LUI : AUIPC;
          instruction = RV32imSupport.getUTypeInstruction(opcode, destination, immediate);
          break;
        default:
          errors = true;
          instr.setError(instr.getInstruction(), S.getter("RV32imAssemblerBUG"));
          break;
      }
    }
    valid = !errors;
    if (valid)
        instr.setInstructionByteCode(instruction, 4);
    return true;
  }

}
