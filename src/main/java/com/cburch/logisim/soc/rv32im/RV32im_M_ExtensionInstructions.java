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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

public class RV32im_M_ExtensionInstructions implements AssemblerExecutionInterface {

  private static final int OP = 0x33;

  private static final int INSTR_MUL = 0;
  private static final int INSTR_MULH = 1;
  private static final int INSTR_MULHSU = 2;
  private static final int INSTR_MULHU = 3;
  private static final int INSTR_DIV = 4;
  private static final int INSTR_DIVU = 5;
  private static final int INSTR_REM = 6;
  private static final int INSTR_REMU = 7;

  private static final String[] AsmOpcodes = {
      "MUL", "MULH", "MULHSU", "MULHU", "DIV", "DIVU", "REM", "REMU"};

  private int instruction;
  private boolean valid;
  private int operation;
  private int destination;
  private int source1;
  private int source2;

  public ArrayList<String> getInstructions() {
    ArrayList<String> opcodes = new ArrayList<>(Arrays.asList(AsmOpcodes));
    return opcodes;
  }

  public boolean execute(Object state, CircuitState cState) {
    if (!valid) return false;
    RV32im_state.ProcessorState cpuState = (RV32im_state.ProcessorState) state;
    int val1 = cpuState.getRegisterValue(source1);
    int val2 = cpuState.getRegisterValue(source2);
    BigInteger opp1, opp2, res;
    BigInteger mask = BigInteger.valueOf(1).shiftLeft(32).subtract(BigInteger.valueOf(1));
    int result = 0;
    switch (operation) {
      case INSTR_MULH:
      case INSTR_MUL:
        opp1 = BigInteger.valueOf(val1);
        opp2 = BigInteger.valueOf(val2);
        res = opp1.multiply(opp2);
        result =
            (operation == INSTR_MUL)
                ? res.and(mask).intValue()
                : res.shiftRight(32).and(mask).intValue();
        break;
      case INSTR_MULHSU:
        opp1 = BigInteger.valueOf(val1);
        opp2 = BigInteger.valueOf(ElfHeader.getLongValue(val2));
        res = opp1.multiply(opp2);
        result = res.shiftRight(32).and(mask).intValue();
        break;
      case INSTR_MULHU:
        opp1 = BigInteger.valueOf(ElfHeader.getLongValue(val1));
        opp2 = BigInteger.valueOf(ElfHeader.getLongValue(val2));
        res = opp1.multiply(opp2);
        result = res.shiftRight(32).and(mask).intValue();
        break;
      case INSTR_DIV:
      case INSTR_REM:
        opp1 = BigInteger.valueOf(val1);
        opp2 = BigInteger.valueOf(val2);
        res = (operation == INSTR_REM) ? opp1.remainder(opp2) : opp1.divide(opp2);
        result = res.and(mask).intValue();
        break;
      case INSTR_DIVU:
      case INSTR_REMU:
        opp1 = BigInteger.valueOf(ElfHeader.getLongValue(val1));
        opp2 = BigInteger.valueOf(ElfHeader.getLongValue(val2));
        res = (operation == INSTR_REMU) ? opp1.remainder(opp2) : opp1.divide(opp2);
        result = res.and(mask).intValue();
        break;
    }
    cpuState.writeRegister(destination, result);
    return true;
  }

  public String getAsmInstruction() {
    if (!valid) return null;
    StringBuilder s = new StringBuilder();
    s.append(AsmOpcodes[operation].toLowerCase());
    while (s.length() < RV32imSupport.ASM_FIELD_SIZE)
      s.append(" ");
    s.append(RV32im_state.registerABINames[destination]).append(",")
        .append(RV32im_state.registerABINames[source1]).append(",")
        .append(RV32im_state.registerABINames[source2]);
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
    if (RV32imSupport.getOpcode(instruction) == OP) {
      if (RV32imSupport.getFunct7(instruction) != 1)
        return false;
      operation = RV32imSupport.getFunct3(instruction);
      destination = RV32imSupport.getDestinationRegisterIndex(instruction);
      source1 = RV32imSupport.getSourceRegister1Index(instruction);
      source2 = RV32imSupport.getSourceRegister2Index(instruction);
      return true;
    }
    return false;
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
    valid = true;
    for (int i = 0; i < AsmOpcodes.length; i++)
      if (AsmOpcodes[i].equals(instr.getOpcode().toUpperCase())) operation = i;
    if (operation < 0) {
      valid = false;
      return false;
    }
    if (instr.getNrOfParameters() != 3) {
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedThreeArguments"));
      valid = false;
      return true;
    }
    AssemblerToken[] param1 = instr.getParameter(0);
    AssemblerToken[] param2 = instr.getParameter(1);
    AssemblerToken[] param3 = instr.getParameter(2);
    boolean errors = false;
    if (param1.length != 1 || param1[0].getType() != AssemblerToken.REGISTER) {
      instr.setError(param1[0], S.getter("AssemblerExpectedRegister"));
      errors = true;
    }
    if (param2.length != 1 || param2[0].getType() != AssemblerToken.REGISTER) {
      instr.setError(param2[0], S.getter("AssemblerExpectedRegister"));
      errors = true;
    }
    if (param3.length != 1 || param3[0].getType() != AssemblerToken.REGISTER) {
      instr.setError(param3[0], S.getter("AssemblerExpectedRegister"));
      errors = true;
    }
    destination = RV32im_state.getRegisterIndex(param1[0].getValue());
    if (destination < 0 || destination > 31) {
      instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
      errors = true;
    }
    source1 = RV32im_state.getRegisterIndex(param2[0].getValue());
    if (source1 < 0 || source1 > 31) {
      instr.setError(param2[0], S.getter("AssemblerUnknownRegister"));
      errors = true;
    }
    source2 = RV32im_state.getRegisterIndex(param3[0].getValue());
    if (source2 < 0 || source2 > 31) {
      instr.setError(param3[0], S.getter("AssemblerUnknownRegister"));
      errors = true;
    }
    valid = !errors;
    if (valid) {
      instruction =
          RV32imSupport.getRTypeInstruction(OP, destination, operation, source1, source2, 1);
      instr.setInstructionByteCode(instruction, 4);
    }
    return true;
  }

}
