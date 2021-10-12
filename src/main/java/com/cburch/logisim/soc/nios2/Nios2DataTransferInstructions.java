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
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;
import java.util.ArrayList;

public class Nios2DataTransferInstructions implements AssemblerExecutionInterface {

  private static final int INSTR_LDW = 0;
  private static final int INSTR_LDH = 1;
  private static final int INSTR_LDHU = 2;
  private static final int INSTR_LDB = 3;
  private static final int INSTR_LDBU = 4;
  private static final int INSTR_LDWIO = 5;
  private static final int INSTR_LDHIO = 6;
  private static final int INSTR_LDHUIO = 7;
  private static final int INSTR_LDBIO = 8;
  private static final int INSTR_LDBUIO = 9;
  private static final int INSTR_STW = 10;
  private static final int INSTR_STH = 11;
  private static final int INSTR_STB = 12;
  private static final int INSTR_STWIO = 13;
  private static final int INSTR_STHIO = 14;
  private static final int INSTR_STBIO = 15;

  private static final String[] AsmOpcodes = {"LDW", "LDH", "LDHU", "LDB", "LDBU",
                                              "LDWIO", "LDHIO", "LDHUIO", "LDBIO", "LDBUIO",
                                              "STW", "STH", "STB",
                                              "STWIO", "STHIO", "STBIO" };
  private static final Integer[] AsmOpcs = {0x17, 0x0F, 0x0B, 0x07, 0x03,
                                            0x37, 0x2F, 0x2B, 0x27, 0x23,
                                            0x15, 0x0d, 0x05,
                                            0x35, 0x2d, 0x25 };

  private final ArrayList<String> Opcodes;
  private final ArrayList<Integer> OpcCodes;

  private int instruction;
  private boolean valid;
  private int operation;
  private int destination;
  private int immediate;
  private int base;
  private String errorMessage;

  public Nios2DataTransferInstructions() {
    Opcodes = new ArrayList<>();
    OpcCodes = new ArrayList<>();
    for (int i = 0; i < 16; i++) {
      Opcodes.add(AsmOpcodes[i].toLowerCase());
      OpcCodes.add(AsmOpcs[i]);
    }
  }

  @Override
  @SuppressWarnings("fallthrough")
  public boolean execute(Object processorState, CircuitState circuitState) {
    if (!valid) return false;
    Nios2State.ProcessorState cpuState = (Nios2State.ProcessorState) processorState;
    long address = SocSupport.convUnsignedInt(cpuState.getRegisterValue(base)) + immediate;
    errorMessage = null;
    int toBeStored = cpuState.getRegisterValue(destination);
    int transType = -1;
    switch (operation) {
      case INSTR_STBIO:
      case INSTR_STB:
        toBeStored &= 0xFF;
        transType = SocBusTransaction.BYTE_ACCESS;
        // fall through
      case INSTR_STHIO:
      case INSTR_STH:
        toBeStored &= 0xFFFF;
        if (transType < 0) transType = SocBusTransaction.HALF_WORD_ACCESS;
        // fall through
      case INSTR_STWIO:
      case INSTR_STW:
        if (transType < 0) transType = SocBusTransaction.WORD_ACCESS;
        SocBusTransaction trans =
            new SocBusTransaction(
                SocBusTransaction.WRITE_TRANSACTION,
                SocSupport.convUnsignedLong(address),
                toBeStored,
                transType,
                cpuState.getMasterComponent());
        cpuState.insertTransaction(trans, false, circuitState);
        return !transactionHasError(trans);
      case INSTR_LDB:
      case INSTR_LDBIO:
      case INSTR_LDBU:
      case INSTR_LDBUIO:
        transType = SocBusTransaction.BYTE_ACCESS;
        // fall through
      case INSTR_LDH:
      case INSTR_LDHIO:
      case INSTR_LDHU:
      case INSTR_LDHUIO:
        if (transType < 0) transType = SocBusTransaction.HALF_WORD_ACCESS;
        // fall through
      case INSTR_LDW:
      case INSTR_LDWIO:
        if (transType < 0) transType = SocBusTransaction.WORD_ACCESS;
        trans =
            new SocBusTransaction(
                SocBusTransaction.READ_TRANSACTION,
                SocSupport.convUnsignedLong(address),
                0,
                transType,
                cpuState.getMasterComponent());
        cpuState.insertTransaction(trans, false, circuitState);
        if (transactionHasError(trans)) return false;
        int toBeLoaded = trans.getReadData();
        switch (operation) {
          case INSTR_LDBU:
          case INSTR_LDBUIO:
            toBeLoaded &= 0xFF;
            break;
          case INSTR_LDB:
          case INSTR_LDBIO:
            toBeLoaded <<= 24;
            toBeLoaded >>= 24;
            break;
          case INSTR_LDHU:
          case INSTR_LDHUIO:
            toBeLoaded &= 0xFFFF;
            break;
          case INSTR_LDH:
          case INSTR_LDHIO:
            toBeLoaded <<= 16;
            toBeLoaded >>= 16;
            break;
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
    if (!valid) return null;
    StringBuilder s = new StringBuilder();
    s.append(Opcodes.get(operation));
    while (s.length() < Nios2Support.ASM_FIELD_SIZE) s.append(" ");
    s.append(Nios2State.registerABINames[destination]).append(",").append(immediate).append("(");
    s.append(Nios2State.registerABINames[base]).append(")");
    return s.toString();
  }

  @Override
  public int getBinInstruction() {
    return instruction;
  }

  @Override
  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    valid = false;
    if (!Opcodes.contains(instr.getOpcode().toLowerCase())) return false;
    operation = Opcodes.indexOf(instr.getOpcode().toLowerCase());
    valid = true;
    if (instr.getNrOfParameters() != 2) {
      valid = false;
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
      return true;
    }
    AssemblerToken[] param2 = instr.getParameter(1);
    valid &= Nios2Support.isCorrectRegister(instr, 0);
    destination = Nios2Support.getRegisterIndex(instr, 0);
    if (param2.length != 2) {
      valid = false;
      instr.setError(param2[0], S.getter("Nios2AssemblerExpectedImmediateIndexedRegister"));
    }
    if (!valid) return true;
    if (!param2[0].isNumber()) {
      valid = false;
      instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
    }
    if (param2[1].getType() != AssemblerToken.BRACKETED_REGISTER) {
      valid = false;
      instr.setError(param2[1], S.getter("Nios2AssemblerExpectedBracketedRegister"));
    }
    if (!valid) return true;
    if (Nios2State.isCustomRegister(param2[1].getValue())) {
      valid = false;
      instr.setError(param2[1], S.getter("Nios2CannotUseCustomRegister"));
    }
    if (Nios2State.isControlRegister(param2[1].getValue())) {
      valid = false;
      instr.setError(param2[1], S.getter("Nios2CannotUseControlRegister"));
    }
    base = Nios2State.getRegisterIndex(param2[1].getValue());
    if (base < 0 || base > 31) {
      valid = false;
      instr.setError(param2[1], S.getter("AssemblerUnknownRegister"));
    }
    immediate = param2[0].getNumberValue();
    if (immediate >= (1 << 15) || immediate < -(1 << 15)) {
      valid = false;
      instr.setError(param2[0], S.getter("AssemblerImmediateOutOfRange"));
    }
    if (!valid) return true;
    instruction =
        Nios2Support.getITypeInstructionCode(base, destination, immediate, OpcCodes.get(operation));
    instr.setInstructionByteCode(instruction, 4);
    return true;
  }

  @Override
  public boolean setBinInstruction(int instr) {
    valid = false;
    int opc = Nios2Support.getOpcode(instr);
    if (!OpcCodes.contains(opc)) return false;
    valid = true;
    instruction = instr;
    operation = OpcCodes.indexOf(opc);
    immediate = Nios2Support.getImmediate(instr, Nios2Support.I_TYPE);
    if (((immediate >> 15) & 1) != 0) immediate |= 0xFFFF0000;
    base = Nios2Support.getRegAIndex(instr, Nios2Support.I_TYPE);
    destination = Nios2Support.getRegBIndex(instr, Nios2Support.I_TYPE);
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

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public ArrayList<String> getInstructions() {
    return Opcodes;
  }

  @Override
  public int getInstructionSizeInBytes(String instruction) {
    if (Opcodes.contains(instruction.toLowerCase())) return 4;
    return -1;
  }

}
