/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.soc.nios2;

import static com.cburch.logisim.soc.Strings.S;

import java.util.ArrayList;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;

public class Nios2ArithmeticAndLogicalInstructions implements AssemblerExecutionInterface {

  private static final int INSTR_AND = 0;
  private static final int INSTR_OR = 1;
  private static final int INSTR_XOR = 2;
  private static final int INSTR_NOR = 3;
  private static final int INSTR_ADD = 4;
  private static final int INSTR_SUB = 5;
  private static final int INSTR_MUL = 6;
  private static final int INSTR_DIV = 7;
  private static final int INSTR_DIVU = 8;
  private static final int INSTR_MULXSS = 9;
  private static final int INSTR_MULXUU = 10;
  private static final int INSTR_MULXSU = 11;
  private static final int INSTR_ANDI = 12;
  private static final int INSTR_ORI = 13;
  private static final int INSTR_XORI = 14;
  private static final int INSTR_ANDHI = 15;
  private static final int INSTR_ORHI = 16;
  private static final int INSTR_XORHI = 17;
  private static final int INSTR_ADDI = 18;
  private static final int INSTR_SUBI = 19;
  private static final int INSTR_MULI = 20;
  private static final int INSTR_NOP = 21;
  private static final int INSTR_MOV = 22;
  private static final int INSTR_MOVHI = 23;
  private static final int INSTR_MOVI = 24;
  private static final int INSTR_MOVUI = 25;
  private static final int INSTR_MOVIA = 26;

  private static final int SIGN_EXTEND = 0x100;
  private static final int PSEUDO_INSTR = 0x200;
  private static final int DOUBLE_SIZE = 0x400;
  
  private final static String[] AsmOpcodes = {"AND", "OR", "XOR", "NOR", "ADD", "SUB", "MUL", "DIV", "DIVU",
          "MULXSS", "MULXUU", "MULXSU",
          "ANDI", "ORI", "XORI", "ANDHI", "ORHI", "XORHI", "ADDI", "SUBI", "MULI",
          "NOP", "MOV", "MOVHI", "MOVI", "MOVUI", "MOVIA"};
  private final static Integer[] AsmOpcs = {0x3A , 0x3A, 0x3A, 0x3A, 0x3A, 0x3A, 0x3A, 0x3A, 0x3A,
          0x3A, 0x3A, 0x3A, 0x0C, 0x14, 0x1C, 0x2C, 0x34, 0x3C, 0x04, 0x04, 0x24,
          PSEUDO_INSTR,PSEUDO_INSTR,PSEUDO_INSTR,PSEUDO_INSTR,PSEUDO_INSTR,PSEUDO_INSTR};
  private final static Integer[] AsmOpxs = {0x0e, 0x16, 0x1E, 0x06, 0x31, 0x39, 0x27, 0x18, 0x24,
          0x1F, 0x07, 0x17, -1, -1, -1, -1, -1, -1, SIGN_EXTEND, SIGN_EXTEND, SIGN_EXTEND,
          -1,-1,-1,SIGN_EXTEND,-1,DOUBLE_SIZE};

  /* Pseudo instructions:
   * subi rb,ra,imm => addi rb,ra,-imm
   * nop            => add r0,r0,r0
   * mov rc,ra      => add rc,ra,r0
   * movhi rb,imm   => orhi rb,r0,imm
   * movi rb,imm    => addi rb,r0,imm
   * movui rb,imm   => ori rb,r0,imm
   * movia rb,label => orhi rb,0,%hi(label); addi rb,r0,%lo(label)    
   */
  
  private ArrayList<String> Opcodes = new ArrayList<String>();
  private ArrayList<Integer> OpcCodes = new ArrayList<Integer>(); 
  private ArrayList<Integer> OpxCodes = new ArrayList<Integer>(); 

  private int instruction;
  private boolean valid;
  private int operation;
  private int destination;
  private int immediate;
  private int sourceA;
  private int sourceB;
  
  public Nios2ArithmeticAndLogicalInstructions() {
    for (int i = 0 ; i < AsmOpcodes.length ; i++) {
      Opcodes.add(AsmOpcodes[i].toLowerCase());
      OpcCodes.add(AsmOpcs[i]);
      OpxCodes.add(AsmOpxs[i]);
    }
  }

  public boolean execute(Object processorState, CircuitState circuitState) {
    if (!valid) return false;
    Nios2State.ProcessorState state = (Nios2State.ProcessorState) processorState;
    int valueB = state.getRegisterValue(sourceB);
    int valueA = state.getRegisterValue(sourceA);
    int result = 0;
    int imm = immediate << 16;
    if (operation == INSTR_SUBI) {
      imm = -imm;
      operation = INSTR_ADDI;
    }
    switch (operation) {
      case INSTR_MOVIA    : result = immediate;
                            break;
      case INSTR_ANDHI    : result = valueA & imm;
                            break;
      case INSTR_ANDI     : valueB = immediate;
      case INSTR_AND      : result = valueA & valueB;
                            break;
      case INSTR_MOVHI    :
      case INSTR_ORHI     : result = valueA | imm;
                            break;
      case INSTR_MOVUI    :
      case INSTR_ORI      : valueB = immediate;
      case INSTR_OR       : result = valueA | valueB;
                            break;
      case INSTR_NOR      : result = valueA | valueB;
                            result ^= -1;
                            break;
      case INSTR_XORHI    : result = valueA ^ imm;
                            break;
      case INSTR_XORI     : valueB = immediate;
      case INSTR_XOR      : result = valueA ^ valueB;
                            break;
      case INSTR_MOVI     :
      case INSTR_ADDI     : valueB = imm >> 16;
      case INSTR_MOV      :
      case INSTR_NOP      : 
      case INSTR_ADD      : result = valueA + valueB;
                            break;
      case INSTR_SUB      : result = valueA - valueB;
                            break;
      case INSTR_MULI     : valueB = imm >> 16;
      case INSTR_MULXSS   :
      case INSTR_MUL      : long oppA = valueA;
                            long oppB = valueB;
                            long res = oppA*oppB;
                            result = SocSupport.convUnsignedLong(operation == INSTR_MUL ? res : res >>32);
                            break;
      case INSTR_DIV      : result = valueA / valueB;
                            break;
      case INSTR_DIVU     : long opA = SocSupport.convUnsignedInt(valueA);
                            long opB = SocSupport.convUnsignedInt(valueB);
                            long div = opA/opB;
                            result = SocSupport.convUnsignedLong(div);
                            break;
      case INSTR_MULXUU   : oppA = SocSupport.convUnsignedInt(valueA);
                            oppB = SocSupport.convUnsignedInt(valueB);
                            res = oppA*oppB;
                            result = SocSupport.convUnsignedLong(res >> 32);
                            break;
      case INSTR_MULXSU   : oppA = valueA;
                            oppB = SocSupport.convUnsignedInt(valueB);
                            res = oppA*oppB;
                            result = SocSupport.convUnsignedLong(res >> 32);
                            break;
      default             : return false;
    }
    state.writeRegister(destination, result);
    return true;
  }
  
  public String getAsmInstruction() {
    if (!valid) return null;
    StringBuffer s = new StringBuffer();
    s.append(Opcodes.get(operation));
    while (s.length() < Nios2Support.ASM_FIELD_SIZE) s.append(" ");
    if (OpcCodes.get(operation) == PSEUDO_INSTR) {
      if (operation != INSTR_NOP) {
        if (operation == INSTR_MOV) {
          s.append(Nios2State.registerABINames[destination]+","+Nios2State.registerABINames[sourceA]);
        } else {
          int imm = immediate;
          if (OpxCodes.get(operation) == SIGN_EXTEND) {
            imm <<=16;
            imm >>= 16;
          }
          s.append(Nios2State.registerABINames[destination]+","+imm);
        }
      }
    } else if (OpcCodes.get(operation) == 0x3A) {
      s.append(Nios2State.registerABINames[destination]+","+Nios2State.registerABINames[sourceA]);
      s.append(","+Nios2State.registerABINames[sourceB]);
    } else {
      int imm = immediate;
      if (OpxCodes.get(operation) == SIGN_EXTEND) {
        imm = immediate << 16;
        imm >>= 16;
      }
      s.append(Nios2State.registerABINames[destination]+","+Nios2State.registerABINames[sourceA]+","+imm);
    }
    return s.toString();
  }

  public int getBinInstruction() { return instruction; }

  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    if (!Opcodes.contains(instr.getOpcode().toLowerCase())) return false;
    valid = true;
    operation = Opcodes.indexOf(instr.getOpcode().toLowerCase());
    AssemblerToken[] param2,param3;
    if (operation == INSTR_NOP) {
      if (instr.getNrOfParameters() != 0) {
        valid = false;
        instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedNoArguments"));
        return true;
      }
      sourceA = sourceB = destination = immediate = 0;
      instruction = Nios2Support.getRTypeInstructionCode(0, 0, 0, OpxCodes.get(INSTR_ADD));
      instr.setInstructionByteCode(instruction, 4);
      return true;
    } else if (operation == INSTR_MOVIA) {
      if (instr.getNrOfParameters() != 2) {
        valid = false;
        instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
        return true;
      }
      valid &= Nios2Support.isCorrectRegister(instr, 0);
      destination = Nios2Support.getRegisterIndex(instr, 0);
      param2 = instr.getParameter(1);
      if (param2.length != 1 || !param2[0].isNumber()) {
        valid = false;
        instr.setError(param2[0], S.getter("AssemblerExpextedImmediateOrLabel"));
      }
      immediate = param2[0].getNumberValue();
      sourceA = sourceB = 0;
      instruction = -1;
      if (valid) {
        int[] instrs = new int[2];
        int imm = (immediate >> 16)&0xFFFF;
        imm = imm + ((immediate>>15)&1);
        imm &= 0xFFFF;
        instrs[0] = Nios2Support.getITypeInstructionCode(0, destination, imm, OpcCodes.get(INSTR_ORHI));
        imm = immediate&0xFFFF;
        instrs[1] = Nios2Support.getITypeInstructionCode(destination, destination, imm, OpcCodes.get(INSTR_ADDI));
        instr.setInstructionByteCode(instrs, 4);
      }
      return true;
    } else if (operation == INSTR_MOV) {
      if (instr.getNrOfParameters() != 2) {
        valid = false;
        instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
        return true;
      }
      valid &= Nios2Support.isCorrectRegister(instr, 0);
      valid &= Nios2Support.isCorrectRegister(instr, 1);
      destination = Nios2Support.getRegisterIndex(instr,0);
      sourceA = sourceB = Nios2Support.getRegisterIndex(instr,1);
      if (valid) {
        instruction = Nios2Support.getRTypeInstructionCode(sourceA, 0, destination, OpxCodes.get(INSTR_ADD));
        instr.setInstructionByteCode(instruction, 4);
      }
      return true;
    } else if (OpcCodes.get(operation) == PSEUDO_INSTR) {
      if (instr.getNrOfParameters() != 2) {
        valid = false;
        instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
        return true;
      }
      valid &= Nios2Support.isCorrectRegister(instr, 0);
      destination = Nios2Support.getRegisterIndex(instr, 0);
      param2 = instr.getParameter(1);
      if (param2.length != 1 || !param2[0].isNumber()) {
        valid = false;
        instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
      }
      immediate = param2[0].getNumberValue();
      sourceA = sourceB = 0;
      switch (operation) {
        case INSTR_MOVUI :
        case INSTR_MOVHI : if (immediate >= (1 << 16) || immediate < 0) {
                         valid = false;
                         instr.setError(param2[0], S.getter("AssemblerImmediateOutOfRange"));
                           }
                           operation = operation == INSTR_MOVHI ? INSTR_ORHI : INSTR_ORI;
                           break;
        case INSTR_MOVI  : if (immediate >= (1 << 15) || immediate < -(1 << 15)) {
                             valid = false;
                             instr.setError(param2[0], S.getter("AssemblerImmediateOutOfRange"));
                           }
                           operation = INSTR_ADDI;
                           break;
        default          : valid = false;
                           return false;
      }
      if (valid) {
        instruction = Nios2Support.getITypeInstructionCode(sourceA, destination, immediate, OpcCodes.get(operation));
        instr.setInstructionByteCode(instruction, 4);
      }
      return true;
    }
    if (instr.getNrOfParameters() != 3) {
      valid = false;
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedThreeArguments"));
      return true;
    }
    valid &= Nios2Support.isCorrectRegister(instr, 0);
    valid &= Nios2Support.isCorrectRegister(instr, 1);
    destination = Nios2Support.getRegisterIndex(instr, 0);
    sourceA = Nios2Support.getRegisterIndex(instr, 1);
    if (operation >= INSTR_ANDI) {
      sourceB = 0;
      param3 = instr.getParameter(2);
      if (param3.length != 1 || !param3[0].isNumber()) {
        valid = false;
        instr.setError(param3[0], S.getter("AssemblerExpectedImmediateValue"));
      }
      immediate = param3[0].getNumberValue();
      if (OpxCodes.get(operation) == SIGN_EXTEND) {
        if (immediate >= (1 << 15) || immediate < -(1<<15)) {
          valid = false;
          instr.setError(param3[0], S.getter("AssemblerImmediateOutOfRange"));
        }
      } else {
        if (immediate >= (1 << 16) || immediate < 0) {
          valid = false;
          instr.setError(param3[0], S.getter("AssemblerImmediateOutOfRange"));
        }
      }
    } else {
      immediate = 0;
      valid &= Nios2Support.isCorrectRegister(instr, 2);
      sourceB = Nios2Support.getRegisterIndex(instr,2);
    }
    if (valid) {
      if (operation >= INSTR_ANDI)
        instruction = Nios2Support.getITypeInstructionCode(sourceA, destination, immediate, OpcCodes.get(operation));
      else
        instruction = Nios2Support.getRTypeInstructionCode(sourceA, sourceB, destination, OpxCodes.get(operation));
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
      int rcode = Nios2Support.getOPXCode(instr, Nios2Support.R_TYPE);
      if (OpxCodes.contains(rcode)) {
        operation = OpxCodes.indexOf(rcode);
        destination = Nios2Support.getRegCIndex(instr, Nios2Support.R_TYPE);
        sourceA = Nios2Support.getRegAIndex(instr, Nios2Support.R_TYPE);
        sourceB = Nios2Support.getRegBIndex(instr, Nios2Support.R_TYPE);
        immediate = -1;
        valid = true;
      }
    } else if (OpcCodes.contains(opcode)) {
      operation = OpcCodes.indexOf(opcode);
      destination = Nios2Support.getRegBIndex(instr, Nios2Support.I_TYPE);
      sourceA = sourceB = Nios2Support.getRegAIndex(instr, Nios2Support.I_TYPE);
      immediate = Nios2Support.getImmediate(instr, Nios2Support.I_TYPE);
      valid = true;
    }
    if (valid) convertToPseudo();
    return valid;
  }

  private void convertToPseudo() {
    switch (operation) {
      case INSTR_ADD :  if (sourceA == 0 && sourceB == 0 && destination == 0) {
                          operation = INSTR_NOP;
                          break;
                        }
                        if (sourceB == 0) {
                          operation = INSTR_MOV;
                        }
                        break;
      case INSTR_ORHI : if (sourceA == 0) operation = INSTR_MOVHI;
                        break;
      case INSTR_ADDI : if (sourceA == 0) operation = INSTR_MOVI;
                        break;
      case INSTR_ORI  : if (sourceA == 0) operation = INSTR_MOVUI;
                        break;
    }
  }

  public boolean performedJump() { return false; }
  public boolean isValid() { return valid; }
  public String getErrorMessage() { return null; }
  public ArrayList<String> getInstructions() { return Opcodes; }

  public int getInstructionSizeInBytes(String instruction) {
    if (Opcodes.contains(instruction.toLowerCase())) {
      int idx = Opcodes.indexOf(instruction.toLowerCase());
      return OpxCodes.get(idx) == DOUBLE_SIZE ? 8 : 4;
    }
    return -1;
  }

}
