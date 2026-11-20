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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;

public class RV32imNiosVVendorSpecific implements AssemblerExecutionInterface {

  private static final int MISC_MEM = 0x0F;
  private static int fenceIIdx = 7;
  private static final String[] AsmOpcodes = {"CBO.INVAL.IX", "CBO.CLEAN.IX", "CBO.FLUSH.IX",
      "CBO.CLEAN", "CBO.FLUSH", "CBO.INVAL", "CBO.ZERO", "FENCE.I" };
  private static final int[] AsmBinCodes = {0x80, 0x81, 0x82, 0x1, 0x2, 0x0, 0x4, 0x0};

  private int instruction = 0;
  private boolean valid = false;
  private int operation = -1;
  private int source = -1;

  @Override
  public boolean execute(Object processorState, CircuitState circuitState) {
    /* The Altera Cache vendor specific instructions are just ignored in this simulator,
       as it models the ideal world of always a cache hit and no communication delay */
    return valid;
  }

  @Override
  public String getAsmInstruction() {
    if (!valid) return null;
    final var s = new StringBuilder();
    s.append(AsmOpcodes[operation].toLowerCase(Locale.ROOT));
    s.append(" ");
    while (s.length() < RV32imSupport.ASM_FIELD_SIZE) {
      s.append(" ");
    }
    s.append(RV32imState.registerABINames[source]);
    return s.toString();
  }

  @Override
  public int getBinInstruction() {
    return instruction;
  }

  @Override
  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    operation = -1;
    for (var i = 0; i < AsmOpcodes.length; i++) {
      if (AsmOpcodes[i].equals(instr.getOpcode().toUpperCase(Locale.ROOT))) {
        operation = i;
        break;
      }
    }
    if (operation < 0) {
      valid = false;
      return false;
    }
    if (instr.getNrOfParameters() != 1) {
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedOneArgument"));
      valid = false;
    }
    final var param1 = instr.getParameter(0);
    if (param1.length != 1 || param1[0].getType() != AssemblerToken.REGISTER) {
      instr.setError(param1[0], S.getter("AssemblerExpectedRegister"));
      valid = false;
    }
    source = RV32imState.getRegisterIndex(param1[0].getValue());
    if (source < 0 || source > 31) {
      instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
      valid = false;
    }
    if (!valid) {
      return true;
    }
    instruction = RV32imSupport.getITypeInstruction(MISC_MEM, 0, (operation == fenceIIdx) ? 1 : 2, source, AsmBinCodes[operation]);
    return true;
  }

  @Override
  public boolean setBinInstruction(int instr) {
    instruction = instr;
    valid = decodeBin();
    return valid;
  }

  private boolean decodeBin() {
    if (RV32imSupport.getOpcode(instruction) == MISC_MEM) {
      final var func3 = RV32imSupport.getFunct3(instruction);
      if (func3 != 2 && func3 != 1) {
        return false;
      }
      operation = -1;
      final var immValue = RV32imSupport.getImmediateValue(instruction, RV32imSupport.I_TYPE);
      for (var i = 0; i < AsmOpcodes.length; i++) {
        if (immValue == AsmBinCodes[i]) {
          operation = i;
          break;
        }
      }
      if (operation == fenceIIdx && func3 != 1) {
        return false;
      }
      if (operation < fenceIIdx && func3 == 1) {
        return false;
      }
      if (operation < 0) {
        return false;
      }
      if (RV32imSupport.getDestinationRegisterIndex(instruction) != 0) {
        return false;
      }
      source = RV32imSupport.getSourceRegister1Index(instruction);
      return true;
    }
    return false;
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
    return null;
  }

  @Override
  public ArrayList<String> getInstructions() {
    ArrayList<String> opcodes = new ArrayList<>(Arrays.asList(AsmOpcodes));
    return opcodes;
  }

  @Override
  public int getInstructionSizeInBytes(String instruction) {
    if (getInstructions().contains(instruction.toUpperCase(Locale.ROOT))) return 4;
    return -1;
  }
}
