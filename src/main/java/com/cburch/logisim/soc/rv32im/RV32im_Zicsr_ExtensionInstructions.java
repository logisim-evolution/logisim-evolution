package com.cburch.logisim.soc.rv32im;

import static com.cburch.logisim.soc.Strings.S;

import java.util.ArrayList;
import java.util.Arrays;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;

public class RV32im_Zicsr_ExtensionInstructions implements AssemblerExecutionInterface {

  private static final int OP = 0x73;

  private static final int INSTR_CSRRW = 0;
  private static final int INSTR_CSRRS = 1;
  private static final int INSTR_CSRRC = 2;
  private static final int INSTR_CSRRWI = 3;
  private static final int INSTR_CSRRSI = 4;
  private static final int INSTR_CSRRCI = 5;

  private static final String[] AsmOpcodes = {
      "CSRRW", "CSRRS", "CSRRC", "CSRRWI", "CSRRSI", "CSRRCI",
      "CSRW", "CSRS", "CSRC", "CSRWI", "CSRSI", "CSRCI"};

  private int instruction;
  private boolean valid;
  private int operation;
  private int destination;
  private int source;
  private int sprIndex;

  @Override
  public boolean execute(Object processorState, CircuitState circuitState) {
    if (!valid) return false;
    final var cpuState = (RV32imState.ProcessorState) processorState;
    final var val = cpuState.getRegisterValue(source);
    switch (operation) {
      case INSTR_CSRRW -> {
        if (!RV32imState.isSprImplemented(sprIndex)) {
          return false;
        }
        if (destination != 0) {
          cpuState.writeRegister(destination, cpuState.getCsrValue(sprIndex));
        }
        cpuState.writeCsr(sprIndex, val);
        return true;
      }
      case INSTR_CSRRS -> {
        if (!RV32imState.isSprImplemented(sprIndex)) {
          return false;
        }
        if (destination != 0) {
          cpuState.writeRegister(destination, cpuState.getCsrValue(sprIndex));
        }
        final var csrContents = cpuState.getCsrValue(sprIndex);
        cpuState.writeCsr(sprIndex, val | csrContents);
        return true;
      }
      case INSTR_CSRRC -> {
        if (!RV32imState.isSprImplemented(sprIndex)) {
          return false;
        }
        if (destination != 0) {
          cpuState.writeRegister(destination, cpuState.getCsrValue(sprIndex));
        }
        final var csrContents = cpuState.getCsrValue(sprIndex);
        final var mask = val ^ 0xffffffff;
        cpuState.writeCsr(sprIndex, mask & csrContents);
        return true;
      }
      case INSTR_CSRRWI -> {
        if (!RV32imState.isSprImplemented(sprIndex)) {
          return false;
        }
        if (destination != 0) {
          cpuState.writeRegister(destination, cpuState.getCsrValue(sprIndex));
        }
        cpuState.writeCsr(sprIndex, source);
        return true;
      }
      case INSTR_CSRRSI -> {
        if (!RV32imState.isSprImplemented(sprIndex)) {
          return false;
        }
        if (destination != 0) {
          cpuState.writeRegister(destination, cpuState.getCsrValue(sprIndex));
        }
        final var csrContents = cpuState.getCsrValue(sprIndex);
        cpuState.writeCsr(sprIndex, source | csrContents);
        return true;
      }
      case INSTR_CSRRCI -> {
        if (!RV32imState.isSprImplemented(sprIndex)) {
          return false;
        }
        if (destination != 0) {
          cpuState.writeRegister(destination, cpuState.getCsrValue(sprIndex));
        }
        final var csrContents = cpuState.getCsrValue(sprIndex);
        final var mask = source ^ 0xffffffff;
        cpuState.writeCsr(sprIndex, mask & csrContents);
        return true;
      }
    }
    return true;
  }

  @Override
  public String getAsmInstruction() {
    if (!valid) return null;
    final var s = new StringBuilder();
    final var realOp = (destination == 0)
          ? operation + 6
          : operation;
    s.append(AsmOpcodes[realOp].toLowerCase());
    while (s.length() < RV32imSupport.ASM_FIELD_SIZE)
      s.append(" ");
    if (destination != 0) {
      s.append(RV32imState.registerABINames[destination]).append(",");
    }
    s.append(RV32imState.getSprName(sprIndex)).append(",")
     .append(operation < INSTR_CSRRWI 
           ? RV32imState.registerABINames[source]
           : String.format("0x%02X", source));
    return s.toString();
  }

  @Override
  public int getBinInstruction() {
    return instruction;
  }

  @Override
  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    int operation = -1;
    valid = true;
    for (int i = 0; i < AsmOpcodes.length; i++)
      if (AsmOpcodes[i].equals(instr.getOpcode().toUpperCase())) operation = i;
    if (operation < 0) {
      valid = false;
      return false;
    }
    if (instr.getNrOfParameters() == 2) {
      if (operation < 6) {
        instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedThreeArguments"));
        valid = false;
        return false;
      }
      operation -= 6;
      destination = 0;
      final var param1 = instr.getParameter(0);
      final var param2 = instr.getParameter(1);
      if(param1.length != 1) {
        instr.setError(param1[0], S.getter("AssemblerExpectedImmediateValue"));
        valid = false;
        return false;
      } else {
        if (param1[0].getType() == AssemblerToken.REGISTER) {
          final var sprIndex = RV32imState.getSprArrayIndex(param1[0].getValue());
          if (sprIndex < 0) {
            instr.setError(param1[0], S.getter("AssemblerExpectedImmediateValue"));
            valid = false;
            return false;
          }
          instruction = OP;
          instruction |= (RV32imState.getSprValue(sprIndex) << 20);
        } else if (param1[0].getType() == AssemblerToken.HEX_NUMBER || param1[0].getType() == AssemblerToken.DEC_NUMBER) {
          instruction = OP;
          instruction |= (param1[0].getNumberValue() << 20);
        } else {
          instr.setError(param1[0], S.getter("AssemblerExpectedImmediateValue"));
          valid = false;
          return false;
        }
      }
      if(param2.length != 1) {
        instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
        valid = false;
        return false;
      } else {
        if (param2[0].getType() == AssemblerToken.REGISTER) {
          final var regIndex = RV32imState.getRegisterIndex(param2[0].getValue());
          if (regIndex < 0) {
            instr.setError(param2[0], S.getter("AssemblerExpectedRegister"));
            valid = false;
            return false;
          }
          instruction |= (regIndex << 15);
        } else if (param2[0].getType() == AssemblerToken.HEX_NUMBER || param2[0].getType() == AssemblerToken.DEC_NUMBER) {
          final var value = param2[0].getNumberValue();
          if (value < 0 || value > 31) {
            instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
            valid = false;
            return false;
          }
          instruction |= (value << 15);
        } else {
          instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
          valid = false;
          return false;
        }
      }
      instruction |= (operation < 3)
            ? (operation+1) << 12
            : (operation+2) << 12;
      instr.setInstructionByteCode(instruction, 4);
      return true;
    }
    if (instr.getNrOfParameters() == 3) {
      if (operation > 5) {
        instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
        valid = false;
        return false;
      }
      final var param1 = instr.getParameter(0);
      final var param2 = instr.getParameter(1);
      final var param3 = instr.getParameter(2);
      if(param1.length != 1) {
        instr.setError(param1[0], S.getter("AssemblerExpectedRegister"));
        valid = false;
        return false;
      }
      if (param1[0].getType() == AssemblerToken.REGISTER) {
        final var reg = RV32imState.getRegisterIndex(param1[0].getValue());
        if (reg < 0) {
          instr.setError(param1[0], S.getter("AssemblerExpectedRegister"));
          valid = false;
          return false;
        }
        instruction = OP;
        instruction |= (reg << 7);
      } else {
        instr.setError(param1[0], S.getter("AssemblerExpectedRegister"));
        valid = false;
        return false;
      }
      if(param2.length != 1) {
        instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
        valid = false;
        return false;
      } else {
        if (param2[0].getType() == AssemblerToken.REGISTER) {
          final var sprIndex = RV32imState.getSprArrayIndex(param2[0].getValue());
          if (sprIndex < 0) {
            instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
            valid = false;
            return false;
          }
          instruction |= (RV32imState.getSprValue(sprIndex) << 20);
        } else if (param2[0].getType() == AssemblerToken.HEX_NUMBER || param2[0].getType() == AssemblerToken.DEC_NUMBER) {
          instruction |= (param2[0].getNumberValue() << 20);
        } else {
          instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
          valid = false;
          return false;
        }
      }
      if(param3.length != 1) {
        instr.setError(param3[0], S.getter("AssemblerExpectedImmediateValue"));
        valid = false;
        return false;
      } else {
        if (param3[0].getType() == AssemblerToken.REGISTER) {
          final var regIndex = RV32imState.getRegisterIndex(param3[0].getValue());
          if (regIndex < 0) {
            instr.setError(param3[0], S.getter("AssemblerExpectedRegister"));
            valid = false;
            return false;
          }
          instruction |= (regIndex << 15);
        } else if (param3[0].getType() == AssemblerToken.HEX_NUMBER || param3[0].getType() == AssemblerToken.DEC_NUMBER) {
          final var value = param3[0].getNumberValue();
          if (value < 0 || value > 31) {
            instr.setError(param3[0], S.getter("AssemblerExpectedImmediateValue"));
            valid = false;
            return false;
          }
          instruction |= (value << 15);
        } else {
          instr.setError(param3[0], S.getter("AssemblerExpectedImmediateValue"));
          valid = false;
          return false;
        }
      }
      instruction |= (operation < 3)
            ? (operation+1) << 12
            : (operation+2) << 12;
      instr.setInstructionByteCode(instruction, 4);
      return true;
    }
    return false;
  }

  @Override
  public boolean setBinInstruction(int instr) {
    instruction = instr;
    valid = decodeBin();
    return valid;
  }

  private boolean decodeBin() {
    if (RV32imSupport.getOpcode(instruction) == OP) {
      switch (RV32imSupport.getFunct3(instruction)) {
        case 1  : operation = 0;
                  break;
        case 2  : operation = 1;
                  break;
        case 3  : operation = 2;
                  break;
        case 5  : operation = 3;
                  break;
        case 6  : operation = 4;
                  break;
        case 7  : operation = 5;
                  break;
        default : return false;
      }
      sprIndex = (instruction >> 20)&0xFFF;
      destination = RV32imSupport.getDestinationRegisterIndex(instruction);
      source  = RV32imSupport.getSourceRegister1Index(instruction);
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
    return new ArrayList<>(Arrays.asList(AsmOpcodes));
  }

  @Override
  public int getInstructionSizeInBytes(String instruction) {
    if (getInstructions().contains(instruction.toUpperCase())) return 4;
    return -1;
  }

}
