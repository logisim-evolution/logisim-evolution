/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.util;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AssemblerAsmInstruction {
  private final AssemblerToken instruction;
  private final ArrayList<AssemblerToken[]> parameters;
  private final int size;
  private final HashMap<AssemblerToken, StringGetter> errors;
  private Byte[] bytes;
  private long programCounter;

  public AssemblerAsmInstruction(AssemblerToken instruction, int size) {
    this.instruction = instruction;
    parameters = new ArrayList<>();
    errors = new HashMap<>();
    this.size = size;
    bytes = null;
    programCounter = -1;
  }

  public String getOpcode() {
    return instruction.getValue();
  }

  public AssemblerToken getInstruction() {
    return instruction;
  }

  public int getNrOfParameters() {
    return parameters.size();
  }

  public void addParameter(AssemblerToken[] param) {
    parameters.add(param);
  }

  public int getSizeInBytes() {
    return size;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public void setError(AssemblerToken token, StringGetter error) {
    errors.put(token, error);
  }

  public HashMap<AssemblerToken, StringGetter> getErrors() {
    return errors;
  }

  public Byte[] getBytes() {
    return bytes;
  }

  public void setProgramCounter(long value) {
    programCounter = value;
  }

  public long getProgramCounter() {
    return programCounter;
  }

  public void setInstructionByteCode(int instruction, int nrOfBytes) {
    if (bytes == null) bytes = new Byte[size];
    for (int i = 0; i < nrOfBytes && i < size; i++) {
      bytes[i] = (byte) ((instruction >> (i * 8)) & 0xFF);
    }
  }

  public void setInstructionByteCode(int[] instruction, int nrOfBytes) {
    if (bytes == null) bytes = new Byte[size];
    for (int j = 0; j < instruction.length; j++)
      for (int i = 0; i < nrOfBytes && i < size; i++) {
        bytes[j * nrOfBytes + i] = (byte) ((instruction[j] >> (i * 8)) & 0xFF);
      }
  }

  public AssemblerToken[] getParameter(int index) {
    if (index < 0 || index >= parameters.size()) return null;
    return parameters.get(index);
  }

  public boolean replaceLabels(
      HashMap<String, Long> labels, HashMap<AssemblerToken, StringGetter> errors) {
    for (AssemblerToken[] parameter : parameters) {
      for (AssemblerToken assemblerToken : parameter) {
        if (assemblerToken.getType() == AssemblerToken.PARAMETER_LABEL) {
          String Name = assemblerToken.getValue();
          if (!labels.containsKey(Name)) {
            errors.put(assemblerToken, S.getter("AssemblerCouldNotFindAddressForLabel"));
            return false;
          }
          assemblerToken.setType(AssemblerToken.HEX_NUMBER);
          assemblerToken.setValue(String.format("0x%08X", labels.get(Name)));
        }
      }
    }
    return true;
  }

  public boolean replaceDefines(
      HashMap<String, Integer> defines, HashMap<AssemblerToken, StringGetter> errors) {
    for (AssemblerToken[] parameter : parameters) {
      for (AssemblerToken assemblerToken : parameter) {
        if (assemblerToken.getType() == AssemblerToken.MAYBE_LABEL) {
          String Name = assemblerToken.getValue();
          if (!defines.containsKey(Name)) {
            errors.put(assemblerToken, S.getter("AssemblerCouldNotFindValueForDefine"));
            return false;
          }
          assemblerToken.setType(AssemblerToken.HEX_NUMBER);
          assemblerToken.setValue(String.format("0x%08X", defines.get(Name)));
        }
      }
    }
    return true;
  }

  public void replacePcAndDoCalc(long pc, HashMap<AssemblerToken, StringGetter> errors) {
    for (int idx = 0; idx < parameters.size(); idx++) {
      AssemblerToken[] parameter = parameters.get(idx);
      boolean found = false;
      for (AssemblerToken assemblerToken : parameter) {
        if (assemblerToken.getType() == AssemblerToken.PROGRAM_COUNTER) {
          found = true;
          assemblerToken.setType(AssemblerToken.HEX_NUMBER);
          assemblerToken.setValue(String.format("0x%08X", pc));
        }
      }
      if (found && parameter.length > 1) {
        int i = 0;
        HashSet<Integer> toBeRemoved = new HashSet<>();
        while (i < parameter.length) {
          if (AssemblerToken.MATH_OPERATORS.contains(parameter[i].getType())) {
            long beforeValue = -1;
            if (i == 0 || !parameter[i - 1].isNumber()) {
              beforeValue = 0L;
            } else if (i + 1 >= parameter.length || !parameter[i + 1].isNumber()) {
              errors.put(parameter[i], S.getter("AssemblerExpectedImmediateValueAfterMath"));
            } else {
              if (beforeValue < 0) {
                toBeRemoved.add(i - 1);
                beforeValue = parameter[i - 1].getLongValue();
              }
              long afterValue = parameter[i + 1].getLongValue();
              toBeRemoved.add(i);
              long result = 0;
              switch (parameter[i].getType()) {
                case AssemblerToken.MATH_ADD:
                  result = beforeValue + afterValue;
                  break;
                case AssemblerToken.MATH_SUBTRACT:
                  result = beforeValue - afterValue;
                  break;
                case AssemblerToken.MATH_SHIFT_LEFT:
                  result = beforeValue << afterValue;
                  break;
                case AssemblerToken.MATH_SHIFT_RIGHT:
                  result = beforeValue >> afterValue;
                  break;
                case AssemblerToken.MATH_MUL:
                  result = beforeValue * afterValue;
                  break;
                case AssemblerToken.MATH_DIV:
                  if (afterValue == 0) errors.put(parameter[i + 1], S.getter("AssemblerDivZero"));
                  else result = beforeValue / afterValue;
                  break;
                case AssemblerToken.MATH_REM:
                  if (afterValue == 0) errors.put(parameter[i + 1], S.getter("AssemblerDivZero"));
                  else result = beforeValue % afterValue;
                  break;
              }
              parameter[i + 1].setType(AssemblerToken.HEX_NUMBER);
              parameter[i + 1].setValue(String.format("0x%X", result));
            }
          }
          i++;
        }
        int newNrOfParameters = parameter.length - toBeRemoved.size();
        AssemblerToken[] newParameter = new AssemblerToken[newNrOfParameters];
        int j = 0;
        for (i = 0; i < parameter.length; i++) {
          if (!toBeRemoved.contains(i)) {
            newParameter[j] = parameter[i];
            j++;
          }
        }
        parameters.set(idx, newParameter);
      }
    }
  }
}
