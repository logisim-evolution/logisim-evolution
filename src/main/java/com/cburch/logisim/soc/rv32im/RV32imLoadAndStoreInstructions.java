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
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.file.ElfHeader;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;
import java.util.ArrayList;
import java.util.Arrays;

public class RV32imLoadAndStoreInstructions implements AssemblerExecutionInterface {

  private static final int LOAD = 0x3;
  private static final int STORE = 0x23;
  private static final int LB = 0;
  private static final int LH = 1;
  private static final int LW = 2;
  private static final int LBU = 4;
  private static final int LHU = 5;

  private static final int INSTR_LB = 0;
  private static final int INSTR_LH = 1;
  private static final int INSTR_LW = 2;
  private static final int INSTR_LBU = 3;
  private static final int INSTR_LHU = 4;
  private static final int INSTR_SB = 5;
  private static final int INSTR_SH = 6;
  private static final int INSTR_SW = 7;

  private static final String[] AsmOpcodes = {"LB", "LH", "LW", "LBU", "LHU", "SB", "SH", "SW"};

  private int instruction = 0;
  private boolean valid = false;
  private int operation;
  private int destination;
  private int immediate;
  private int base;
  private String errorMessage;

  @Override
  public ArrayList<String> getInstructions() {
    return new ArrayList<>(Arrays.asList(AsmOpcodes));
  }

  @Override
  @SuppressWarnings("fallthrough")
  public boolean execute(Object state, CircuitState cState) {
    if (!valid)
      return false;
    RV32imState.ProcessorState cpuState = (RV32imState.ProcessorState) state;
    errorMessage = null;
    int toBeStored = cpuState.getRegisterValue(destination);
    long address = ElfHeader.getLongValue(cpuState.getRegisterValue(base)) + immediate;
    int transType = -1;
    switch (operation) {
      case INSTR_SB:
        toBeStored &= 0xFF;
        transType = SocBusTransaction.BYTE_ACCESS;
        // fall through
      case INSTR_SH:
        toBeStored &= 0xFFFF;
        if (transType < 0) transType = SocBusTransaction.HALF_WORD_ACCESS;
        // fall through
      case INSTR_SW:
        if (transType < 0) transType = SocBusTransaction.WORD_ACCESS;
        SocBusTransaction trans =
            new SocBusTransaction(
                SocBusTransaction.WRITE_TRANSACTION,
                ElfHeader.getIntValue(address),
                toBeStored,
                transType,
                cpuState.getMasterComponent());
        cpuState.insertTransaction(trans, false, cState);
        return !transactionHasError(trans);
      case INSTR_LB:
      case INSTR_LBU:
        transType = SocBusTransaction.BYTE_ACCESS;
        // fall through
      case INSTR_LH:
      case INSTR_LHU:
        if (transType < 0) transType = SocBusTransaction.HALF_WORD_ACCESS;
        // fall through
      case INSTR_LW:
        if (transType < 0) transType = SocBusTransaction.WORD_ACCESS;
        trans =
            new SocBusTransaction(
                SocBusTransaction.READ_TRANSACTION,
                ElfHeader.getIntValue(address),
                0,
                transType,
                cpuState.getMasterComponent());
        cpuState.insertTransaction(trans, false, cState);
        if (transactionHasError(trans)) return false;
        int toBeLoaded = trans.getReadData();
        switch (operation) {
          case INSTR_LBU -> toBeLoaded &= 0xFF;
          case INSTR_LB -> {
            toBeLoaded <<= 24;
            toBeLoaded >>= 24;
          }
          case INSTR_LHU -> toBeLoaded &= 0xFFFF;
          case INSTR_LH -> {
            toBeLoaded <<= 16;
            toBeLoaded >>= 16;
          }
        }
        cpuState.writeRegister(destination, toBeLoaded);
        return true;
    }
    return false;
  }

  private boolean transactionHasError(SocBusTransaction trans) {
    if (trans.hasError()) {
      StringBuilder s = new StringBuilder();
      if (trans.isReadTransaction())
        s.append(S.get("LoadStoreErrorInReadTransaction")).append("\n");
      else
        s.append(S.get("LoadStoreErrorInWriteTransaction")).append("\n");
      s.append(trans.getErrorMessage());
      errorMessage = s.toString();
    }
    return trans.hasError();
  }

  @Override
  public String getAsmInstruction() {
    if (!valid)
      return null;
    StringBuilder s = new StringBuilder();
    s.append(AsmOpcodes[operation].toLowerCase());
    while (s.length() < RV32imSupport.ASM_FIELD_SIZE)
      s.append(" ");
    s.append(RV32imState.registerABINames[destination]).append(",");
    s.append(immediate);
    s.append("(").append(RV32imState.registerABINames[base]).append(")");
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
    int opcode = RV32imSupport.getOpcode(instruction);
    if (opcode == LOAD) {
      destination = RV32imSupport.getDestinationRegisterIndex(instruction);
      immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.I_TYPE);
      base = RV32imSupport.getSourceRegister1Index(instruction);
      int funct3 = RV32imSupport.getFunct3(instruction);
      switch (funct3) {
        case LB:
        case LH:
        case LW:
          operation = funct3;
          return true;
        case LBU:
        case LHU:
          operation = funct3 - 1;
          return true;
        default:
          return false;
      }
    }
    if (opcode == STORE) {
      int funct3 = RV32imSupport.getFunct3(instruction);
      if (funct3 > 2)
        return false;
      operation = funct3 + 5;
      immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.S_TYPE);
      base = RV32imSupport.getSourceRegister1Index(instruction);
      destination = RV32imSupport.getSourceRegister2Index(instruction);
      return true;
    }
    return false;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
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
    if (instr.getNrOfParameters() != 2) {
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
      valid = false;
      return true;
    }
    AssemblerToken[] param1, param2;
    valid = true;
    param1 = instr.getParameter(0);
    if (param1.length != 1 || param1[0].getType() != AssemblerToken.REGISTER) {
      instr.setError(param1[0], S.getter("AssemblerExpectedRegister"));
      valid = false;
    }
    param2 = instr.getParameter(1);
    if (param2.length != 2) {
      instr.setError(param2[0], S.getter("RV32imAssemblerExpectedImmediateIndexedRegister"));
      valid = false;
      return true;
    }
    if (!param2[0].isNumber()) {
      instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
      valid = false;
    }
    if (param2[1].getType() != AssemblerToken.BRACKETED_REGISTER) {
      instr.setError(param2[1], S.getter("RV32imAssemblerExpectedBracketedRegister"));
      valid = false;
    }
    if (!valid) return true;
    destination = RV32imState.getRegisterIndex(param1[0].getValue());
    if (destination < 0 || destination > 31) {
      instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
      valid = false;
    }
    base = RV32imState.getRegisterIndex(param2[1].getValue());
    if (base < 0 || base > 31) {
      instr.setError(param2[1], S.getter("AssemblerUnknownRegister"));
      valid = false;
    }
    immediate = param2[0].getNumberValue();
    if (immediate >= (1 << 11) || immediate < -(1 << 11)) {
      instr.setError(param2[0], S.getter("AssemblerImmediateOutOfRange"));
      valid = false;
    }
    if (!valid) return true;
    switch (operation) {
      case INSTR_SB:
      case INSTR_SH:
      case INSTR_SW:
        int funct3 = operation - INSTR_SB;
        instruction =
            RV32imSupport.getSTypeInstruction(STORE, base, destination, funct3, immediate);
        break;
      case INSTR_LB:
      case INSTR_LBU:
      case INSTR_LH:
      case INSTR_LHU:
      case INSTR_LW:
        funct3 = (operation == INSTR_LBU || operation == INSTR_LHU) ? operation + 1 : operation;
        instruction = RV32imSupport.getITypeInstruction(LOAD, destination, funct3, base, immediate);
        break;
      default:
        valid = false;
        OptionPane.showMessageDialog(null, "Severe Bug in RV32imLoadAndStoreInstructions.java");
        break;
    }
    if (valid) {
      instr.setInstructionByteCode(instruction, 4);
      // DEBUG: System.out.println(String.format("0x%08X 0x%08X", instr.getProgramCounter(), instruction));
    }
    return true;
  }

}
