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
import com.cburch.logisim.soc.util.AbstractExecutionUnitWithLabelSupport;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerToken;

public class Nios2ProgramControlInstructions extends AbstractExecutionUnitWithLabelSupport {

  private static final int INSTR_CALLR = 0;
  private static final int INSTR_RET = 1;
  private static final int INSTR_JMP = 2;
  private static final int INSTR_CALL = 3;
  private static final int INSTR_JMPI = 4;
  private static final int INSTR_BR = 5;
  private static final int INSTR_BGE = 6;
  private static final int INSTR_BGEU = 7;
  private static final int INSTR_BLT = 8;
  private static final int INSTR_BLTU = 9;
  private static final int INSTR_BEQ = 10;
  private static final int INSTR_BNE = 11;
  private static final int INSTR_BGT = 12;
  private static final int INSTR_BGTU = 13;
  private static final int INSTR_BLE = 14;
  private static final int INSTR_BLEU = 15;

  private static final int SIGN_EXTEND = 0x100;
  private static final int PSEUDO_INSTR = 0x200;
  
  private final static String[] AsmOpcodes = {"CALLR", "RET", "JMP", "CALL", "JMPI", "BR",
                                              "BGE", "BGEU", "BLT", "BLTU", "BEQ", "BNE", 
                                              "BGT", "BGTU", "BLE", "BLEU"};
  private final static Integer[] AsmOpcs = {0x3A, 0x3A, 0x3A, 0x00, 0x01, 0x06,
                                            0x0E, 0x2E, 0x16, 0x36, 0x26, 0x1E, 
                                            PSEUDO_INSTR, PSEUDO_INSTR, PSEUDO_INSTR, PSEUDO_INSTR};
  private final static Integer[] AsmOpxs = {0x1D, 0x05, 0x0d, -1, -1, SIGN_EXTEND,
                                            SIGN_EXTEND, SIGN_EXTEND, SIGN_EXTEND, SIGN_EXTEND, 
                                            SIGN_EXTEND, SIGN_EXTEND, SIGN_EXTEND, SIGN_EXTEND, 
                                            SIGN_EXTEND, SIGN_EXTEND};
  /* pseudo instructions:
   * bgt rA, rB, label   => blt rB, rA, label
   * bgtu rA, rB, label  => bltu rB, rA, label
   * ble rA, rB, label   => bge rB, rA, label
   * bleu rA, rB, label  => bgeu rB, rA, label
   */
  
  private ArrayList<String> Opcodes = new ArrayList<String>();
  private ArrayList<Integer> OpcCodes = new ArrayList<Integer>(); 
  private ArrayList<Integer> OpxCodes = new ArrayList<Integer>(); 

  private int instruction;
  private boolean valid;
  private boolean jumped;
  private int operation;
  private int immediate;
  private int sourceA;
  private int sourceB;
  
  public Nios2ProgramControlInstructions() {
    for (int i = 0 ; i < AsmOpcodes.length ; i++) {
      Opcodes.add(AsmOpcodes[i].toLowerCase());
      OpcCodes.add(AsmOpcs[i]);
      OpxCodes.add(AsmOpxs[i]);
    }
  }

  public boolean execute(Object processorState, CircuitState circuitState) {
    if (!valid) return false;
    Nios2State.ProcessorState cpuState = (Nios2State.ProcessorState) processorState;
    jumped = false;
    int valueA = cpuState.getRegisterValue(sourceA);
    int valueB = cpuState.getRegisterValue(sourceB);
    long valueAu = SocSupport.convUnsignedInt(valueA);
    long valueBu = SocSupport.convUnsignedInt(valueB);
    long pc = SocSupport.convUnsignedInt(cpuState.getProgramCounter());
    long nextpc = pc+4;
    int imm = ((immediate<<16)>>16);
    long target = nextpc+imm;
    switch (operation) {
      case INSTR_CALLR  : jumped = true;
                          cpuState.writeRegister(31, SocSupport.convUnsignedLong(nextpc));
                          cpuState.setProgramCounter(valueA);
                          break;
      case INSTR_RET    : jumped = true;
                          cpuState.setProgramCounter(cpuState.getRegisterValue(31));
                          break;
      case INSTR_JMP    : jumped = true;
                          cpuState.setProgramCounter(valueA);
                          break;
      case INSTR_CALL   : cpuState.writeRegister(31, SocSupport.convUnsignedLong(nextpc));
      case INSTR_JMPI   : jumped = true;
                          cpuState.setProgramCounter(immediate<<2);
                          break;
      case INSTR_BR     : jumped = true;
                          cpuState.setProgramCounter(SocSupport.convUnsignedLong(target));
                          break;
      case INSTR_BGE    : if (valueA >= valueB) {
                            jumped = true;
                            cpuState.setProgramCounter(SocSupport.convUnsignedLong(target));
                          }
                          break;
      case INSTR_BGEU   : if (valueAu >= valueBu) {
                            jumped = true;
                            cpuState.setProgramCounter(SocSupport.convUnsignedLong(target));
                          }
                          break;
      case INSTR_BLT    : if (valueA < valueB) {
                            jumped = true;
                            cpuState.setProgramCounter(SocSupport.convUnsignedLong(target));
                          }
                          break;
      case INSTR_BLTU   : if (valueAu < valueBu) {
                            jumped = true;
                            cpuState.setProgramCounter(SocSupport.convUnsignedLong(target));
                          }
                          break;
      case INSTR_BEQ    : if (valueA == valueB) {
                            jumped = true;
                            cpuState.setProgramCounter(SocSupport.convUnsignedLong(target));
                          }
                          break;
      case INSTR_BNE    : if (valueA != valueB) {
                            jumped = true;
                            cpuState.setProgramCounter(SocSupport.convUnsignedLong(target));
                          }
                          break;
      default           : return false;
    }
    return true;
  }

  public String getAsmInstruction() {
    if (!valid) return null;
    StringBuffer s = new StringBuffer();
    s.append(Opcodes.get(operation));
    while (s.length() < Nios2Support.ASM_FIELD_SIZE) s.append(" ");
    int imm = ((immediate << 16) >> 16)+4;
    switch (operation) {
      case INSTR_RET     : break;
      case INSTR_CALLR   : 
      case INSTR_JMP     : s.append(Nios2State.registerABINames[sourceA]);
                           break;
      case INSTR_JMPI    :
      case INSTR_CALL    : s.append(Integer.toString(immediate<<2));
                           break;
      case INSTR_BR      : s.append("pc"+(imm >= 0 ? "+":"")+imm);
                           break;
      default            : s.append(Nios2State.registerABINames[sourceA]+",");
                           s.append(Nios2State.registerABINames[sourceB]+",");
                           s.append("pc"+(imm >= 0 ? "+":"")+imm);
                           break;
    }
    return s.toString();
  }

  public int getBinInstruction() { return instruction; }

  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    valid = false;
    if (!Opcodes.contains(instr.getOpcode().toLowerCase())) return false;
    operation = Opcodes.indexOf(instr.getOpcode().toLowerCase());
    valid = true;
    long pc = instr.getProgramCounter();
    switch (operation) {
      case INSTR_JMP   : 
      case INSTR_CALLR : if (instr.getNrOfParameters() != 1) {
                           valid = false;
                           instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedOneArgument"));
                           return true;
                         }
                         valid &= Nios2Support.isCorrectRegister(instr, 0);
                         sourceA = sourceB = Nios2Support.getRegisterIndex(instr, 0);
                         immediate = 0;
                         break;
      case INSTR_RET   : if (instr.getNrOfParameters() != 0) {
                           valid = false;
                           instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedNoArguments"));
                           return true;
                         }
                         break;
      case INSTR_CALL  :
      case INSTR_JMPI  : if (instr.getNrOfParameters() != 1) {
                           valid = false;
                           instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedOneArgument"));
                           return true;
                         }
                         AssemblerToken[] imm = instr.getParameter(0);
                         if (imm.length != 1 || !imm[0].isNumber()) {
                           valid = false;
                           instr.setError(imm[0], S.getter("AssemblerExpextedImmediateOrLabel"));
                         } else {
                           sourceA = sourceB = 0;
                           immediate = imm[0].getNumberValue() >> 2;
                           if (immediate >= (1 << 26) || immediate < 0) {
                             valid = false;
                             instr.setError(imm[0], S.getter("AssemblerImmediateOutOfRange"));
                           }
                         }
                         break;
      case INSTR_BR    : if (instr.getNrOfParameters() != 1) {
                           valid = false;
                           instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedOneArgument"));
                           return true;
                         }
                         imm = instr.getParameter(0);
                         if (imm.length != 1 || !imm[0].isNumber()) {
                           valid = false;
                           instr.setError(imm[0], S.getter("AssemblerExpextedImmediateOrLabel"));
                         } else {
                           sourceA = sourceB = 0;
                           long target = SocSupport.convUnsignedInt(imm[0].getNumberValue());
                           long imml = pc-target-4L;
                           if (imml >= (1L << 15) || imml < -(1L << 15)) {
                             valid = false;
                             instr.setError(imm[0], S.getter("AssemblerImmediateOutOfRange"));
                           }
                           immediate = (int)imml;
                         }
                         break;
      default          : if (instr.getNrOfParameters() != 3) {
    	                   valid = false;
    	                   instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedThreeArguments"));
    	                   return true;
                         }
                         valid &= Nios2Support.isCorrectRegister(instr, 0);
                         valid &= Nios2Support.isCorrectRegister(instr, 1);
                         sourceA = Nios2Support.getRegisterIndex(instr, 0);
                         sourceB = Nios2Support.getRegisterIndex(instr, 1);
                         imm = instr.getParameter(2);
                         if (imm.length != 1 || !imm[0].isNumber()) {
                           valid = false;
                           instr.setError(imm[0], S.getter("AssemblerExpextedImmediateOrLabel"));
                         }
                         long target = SocSupport.convUnsignedInt(imm[0].getNumberValue());
                         long imml = pc-target-4L;
                         if (imml >= (1L << 15) || imml < -(1L << 15)) {
                           valid = false;
                           instr.setError(imm[0], S.getter("AssemblerImmediateOutOfRange"));
                         }
                         immediate = (int)imml;
                         break;
    }
    /* transform the pseudo instructions */
    boolean switchab = false;
    switch (operation) {
      case INSTR_BGT  : operation = INSTR_BLT;
                        switchab = true;
                        break;
      case INSTR_BGTU : operation = INSTR_BLTU;
                        switchab = true;
                        break;
      case INSTR_BLE  : operation = INSTR_BGE;
                        switchab = true;
                        break;
      case INSTR_BLEU : operation = INSTR_BGEU;
                        switchab = true;
                        break;
    }
    if (switchab) {
      int tmp = sourceA;
      sourceA = sourceB;
      sourceB = tmp;
    }
    if (valid) {
      switch (operation) {
        case INSTR_CALLR : instruction = Nios2Support.getRTypeInstructionCode(sourceA, 0, 0x1f, 0x1d);
                           break;
        case INSTR_RET   : instruction = Nios2Support.getRTypeInstructionCode(0x1f, 0, 0, 0x05);
                           break;
        case INSTR_JMP   : instruction = Nios2Support.getRTypeInstructionCode(sourceA, 0, 0, 0x0d);
                           break;
        case INSTR_CALL  :
        case INSTR_JMPI  : instruction = Nios2Support.getJTypeInstructionCode(immediate, OpcCodes.get(operation));
                           break;
        default          : instruction = Nios2Support.getITypeInstructionCode(sourceA, sourceB, immediate, 
                                         OpcCodes.get(operation));
                           break;
      }
      instr.setInstructionByteCode(instruction, 4);
    }
    return true;
  }

  public boolean setBinInstruction(int instr) {
    instruction = instr;
    valid = false;
    int opcode = Nios2Support.getOpcode(instr);
    if (opcode == 0x3A) {
      int opx = Nios2Support.getOPXCode(instr, Nios2Support.R_TYPE);
      if (!OpxCodes.contains(opx) || Nios2Support.getOPXImm(instr, Nios2Support.R_TYPE) != 0) return false;
      operation =  OpxCodes.indexOf(opx);
      int ra = Nios2Support.getRegAIndex(instr, Nios2Support.R_TYPE);
      int rb = Nios2Support.getRegBIndex(instr, Nios2Support.R_TYPE);
      int rc = Nios2Support.getRegCIndex(instr, Nios2Support.R_TYPE);
      switch (operation) {
        case INSTR_CALLR : if (rc != 0x1F || rb != 0) return false;
                           sourceA = ra;
                           break;
        case INSTR_RET   : if (ra != 0x1F || rb != 0 || rc != 0) return false;
                           break;
        case INSTR_JMP   : if (rb != 0 || rc != 0) return false;
                           sourceA = ra;
                           break;
        default          : return false;
      }
      valid = true;
    } else {
      if (!OpcCodes.contains(opcode)) return false;
      valid = true;
      operation = OpcCodes.indexOf(opcode);
      switch (operation) {
        case INSTR_JMPI : 
        case INSTR_CALL : immediate = Nios2Support.getImmediate(instr, Nios2Support.J_TYPE);
                          break;
        case INSTR_BR   : immediate = Nios2Support.getImmediate(instr, Nios2Support.I_TYPE);
                          if (Nios2Support.getRegAIndex(instr, Nios2Support.I_TYPE) != 0 ||
                              Nios2Support.getRegBIndex(instr, Nios2Support.I_TYPE) != 0) {
                            valid = false;
                          }
        default         : immediate = Nios2Support.getImmediate(instr, Nios2Support.I_TYPE);
                          sourceA = Nios2Support.getRegAIndex(instr, Nios2Support.I_TYPE);
                          sourceB = Nios2Support.getRegBIndex(instr, Nios2Support.I_TYPE);
                          break;
      }
    }
    return valid;
  }

  public boolean performedJump() { return valid && jumped; }
  public boolean isValid() { return valid; }
  public String getErrorMessage() { return null; }
  public ArrayList<String> getInstructions() { return Opcodes; }

  public int getInstructionSizeInBytes(String instruction) {
    if (Opcodes.contains(instruction.toLowerCase())) return 4;
    return -1;
  }

  public boolean isLabelSupported() { return operation >= INSTR_CALL; }

  public long getLabelAddress(long pc) {
    if (!isLabelSupported()) return -1;
    switch (operation) {
      case INSTR_JMPI   :
      case INSTR_CALL   : return SocSupport.convUnsignedInt(immediate << 2);
      default           : int imm = (immediate<<16)>>16;
                          return pc+4L+imm;
    }
  }

  public String getAsmInstruction(String label) {
    if (!valid) return null;
    StringBuffer s = new StringBuffer();
    s.append(Opcodes.get(operation));
    while (s.length() < Nios2Support.ASM_FIELD_SIZE) s.append(" ");
    switch (operation) {
      case INSTR_RET     : break;
      case INSTR_CALLR   : 
      case INSTR_JMP     : s.append(Nios2State.registerABINames[sourceA]);
                           break;
      case INSTR_BR      :
      case INSTR_JMPI    :
      case INSTR_CALL    : s.append(label);
                           break;
      default            : s.append(Nios2State.registerABINames[sourceA]+",");
                           s.append(Nios2State.registerABINames[sourceB]+",");
                           s.append(label);
                           break;
    }
    return s.toString();
  }


}
