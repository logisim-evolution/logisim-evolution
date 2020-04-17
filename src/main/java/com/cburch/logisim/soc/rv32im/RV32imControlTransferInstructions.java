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

package com.cburch.logisim.soc.rv32im;

import static com.cburch.logisim.soc.Strings.S;

import java.util.ArrayList;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.soc.file.ElfHeader;
import com.cburch.logisim.soc.util.AbstractExecutionUnitWithLabelSupport;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerToken;

public class RV32imControlTransferInstructions extends AbstractExecutionUnitWithLabelSupport {
    
  private static final int JAL = 0x6F;
  private static final int JALR = 0x67;
  private static final int BRANCH = 0x63;
  
  private static final int INSTR_BEQ = 0;
  private static final int INSTR_BNE = 1;
  private static final int INSTR_JAL = 2;
  private static final int INSTR_JALR = 3;
  private static final int INSTR_BLT = 4;
  private static final int INSTR_BGE = 5;
  private static final int INSTR_BLTU = 6;
  private static final int INSTR_BGEU = 7;
  private static final int INSTR_J = 8;
  private static final int INSTR_JR = 9;
  private static final int INSTR_RET = 10;
  private static final int INSTR_BNEZ = 11;
  private static final int INSTR_BEQZ = 12;
  
  /* pseudo instructions:
   * J pc+imm => JAL x0,pc+imm
   * JR rs => JALR x0,rs+0  
   * RET  => JALR x0,x1+0
   * BNEZ rs,pc+imm => BNE r0,rs,pc+imm 
   * BEQZ rs,pc+imm => BEQ r0,rs,pc+imm 
   */
  
  /* TODO: add all other pseudo branch instructions like BLZ,BGEZ, etc */
  private final static String[] AsmOpcodes = {"BEQ","BNE","JAL","JALR","BLT","BGE","BLTU","BGEU","J","JR",
          "RET","BNEZ","BEQZ"};

  private boolean valid = false;
  private boolean jumped = false;
  private int instruction = 0;
  private int destination;
  private int operation;
  private int immediate;
  private int source1;
  private int source2;
  public boolean isPcRelative;
  
  public ArrayList<String> getInstructions() {
    ArrayList<String> opcodes = new ArrayList<String>();
    for (int i = 0 ; i < AsmOpcodes.length ; i++)
      opcodes.add(AsmOpcodes[i]);
    return opcodes;
  };

  public boolean execute(Object state, CircuitState cState) {
    if (!valid)
      return false;
    RV32im_state.ProcessorState cpuState = (RV32im_state.ProcessorState) state;
    jumped = false;
    int target = cpuState.getProgramCounter()+immediate;
    int nextPc = cpuState.getProgramCounter()+4;
    int reg1 = cpuState.getRegisterValue(source1);
    int reg2 = cpuState.getRegisterValue(source2);
    switch (operation) {
      case INSTR_JAL  :
      case INSTR_J    : cpuState.setProgramCounter(target);
                        jumped = true;
                        cpuState.writeRegister(destination, nextPc);
                        return true;
      case INSTR_RET  :
      case INSTR_JR   :
      case INSTR_JALR : target = cpuState.getRegisterValue(source1)+immediate;
                        target = (target >>1)<<1;
                        cpuState.setProgramCounter(target);
                        jumped = true;
                        cpuState.writeRegister(destination, nextPc);
                        return true;
      case INSTR_BEQZ :
      case INSTR_BEQ  : if (reg1 == reg2) {
                          jumped = true;
                          cpuState.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BNEZ :
      case INSTR_BNE  : if (reg1 != reg2) {
                          jumped = true;
                          cpuState.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BLT  : if (reg1 < reg2) {
                          jumped = true;
                          cpuState.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BGE  : if (reg1 >= reg2) {
                          jumped = true;
                          cpuState.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BLTU : if (ElfHeader.getLongValue((Integer)reg1) < ElfHeader.getLongValue((Integer)reg2)) {
                          jumped = true;
                          cpuState.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BGEU : if (ElfHeader.getLongValue((Integer)reg1) >= ElfHeader.getLongValue((Integer)reg2)) {
                          jumped = true;
                          cpuState.setProgramCounter(target);
                        }
                        return true;
    }
    return false;
  }

  public String getAsmInstruction() {
    if (!valid)
      return null;
    StringBuffer s = new StringBuffer();
    s.append(AsmOpcodes[operation].toLowerCase());
    while (s.length()<RV32imSupport.ASM_FIELD_SIZE)
      s.append(" ");
    switch (operation) {
      case INSTR_RET  : break;
      case INSTR_JAL  : s.append(RV32im_state.registerABINames[destination]+",");
      case INSTR_J    : s.append("pc");
                        if (immediate != 0) s.append(((immediate>=0) ? "+" : "")+immediate);
                        break;
      case INSTR_JALR : s.append(RV32im_state.registerABINames[destination]+",");
      case INSTR_JR   : s.append(RV32im_state.registerABINames[source1]);
                        if (immediate != 0) s.append(","+immediate);
                        break;
      case INSTR_BEQZ :
      case INSTR_BNEZ : s.append(RV32im_state.registerABINames[source1]+",pc");
                        if (immediate != 0) s.append(((immediate>=0) ? "+" : "")+immediate);
                        break;
      default         : s.append(RV32im_state.registerABINames[source2]+","+RV32im_state.registerABINames[source1]+",pc");
                        if (immediate != 0) s.append(((immediate>=0) ? "+" : "")+immediate);
    }
    return s.toString();
  }
  
  public String getAsmInstruction( String label ) {
    if (!valid)
      return null;
    StringBuffer s = new StringBuffer();
    s.append(AsmOpcodes[operation].toLowerCase());
    while (s.length()<RV32imSupport.ASM_FIELD_SIZE)
      s.append(" ");
    switch (operation) {
      case INSTR_RET  : break;
      case INSTR_JAL  : s.append(RV32im_state.registerABINames[destination]+",");
      case INSTR_J    : s.append(label);
                        break;
      case INSTR_JALR : s.append(RV32im_state.registerABINames[destination]+",");
      case INSTR_JR   : s.append(RV32im_state.registerABINames[source1]);
                        if (immediate != 0) s.append(","+immediate);
                        break;
      case INSTR_BEQZ :
      case INSTR_BNEZ : s.append(RV32im_state.registerABINames[source1]+","+label);
                        break;
      default         : s.append(RV32im_state.registerABINames[source2]+","+RV32im_state.registerABINames[source1]+",");
                        s.append(label);
    }
    return s.toString();
  }
      
  

  public int getBinInstruction() { return instruction; }

  public boolean setBinInstruction(int instr) {
    instruction = instr;
    jumped = false;
    valid = decodeBin();
    return valid;
  }
  
  public boolean performedJump() {return valid&jumped;}

  public boolean isValid() { return valid; }
  
  private boolean decodeBin() {
    int opcode = RV32imSupport.getOpcode(instruction);
    isPcRelative = true;
    switch (opcode) {
      case JAL    : destination = RV32imSupport.getDestinationRegisterIndex(instruction);
                    operation = (destination == 0) ? INSTR_J : INSTR_JAL;
                    immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.J_TYPE);
                    return true;
      case JALR   : if (RV32imSupport.getFunct3(instruction)!=0)
                      return false;
                    isPcRelative = false;
                    destination = RV32imSupport.getDestinationRegisterIndex(instruction);
                    operation = (destination == 0) ? INSTR_JR : INSTR_JALR;
                    source1 = RV32imSupport.getSourceRegister1Index(instruction);
                    immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.I_TYPE);
                    if ((operation == INSTR_JR) && (source1 == 1) && (immediate == 0))
                      operation = INSTR_RET;
                    return true;
      case BRANCH : operation = RV32imSupport.getFunct3(instruction);
                    if ((operation == 2)||(operation == 3))
                      return false;
                    immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.B_TYPE);
                    source1 = RV32imSupport.getSourceRegister1Index(instruction);
                    source2 = RV32imSupport.getSourceRegister2Index(instruction);
                    if (operation == INSTR_BNE && source2 == 0)
                      operation = INSTR_BNEZ;
                    if (operation == INSTR_BEQ && source2 == 0)
                      operation = INSTR_BEQZ;
                    return true;
    }
    return false;
  }

  public String getErrorMessage() { return null; }

  public int getInstructionSizeInBytes(String instruction) {
    if (getInstructions().contains(instruction.toUpperCase())) return 4;
    return -1;
  }

  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    int operation = -1;
    for (int i = 0 ; i < AsmOpcodes.length ; i++) 
      if (AsmOpcodes[i].equals(instr.getOpcode().toUpperCase())) operation = i;
    if (operation < 0) {
      valid = false;
      return false;
    }
    boolean errors = false;
    AssemblerToken[] param1,param2,param3;
    switch (operation) {
      case INSTR_RET  : if (instr.getNrOfParameters() != 0) {
                          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedNoArguments"));
                          errors = true;
                          break;
                        }
                        destination = 0;
                        operation = INSTR_JALR;
                        immediate = 0;
                        source1 = source2 = 1;
                        break;
      case INSTR_BEQZ :
      case INSTR_BNEZ :
      case INSTR_JR   :
      case INSTR_JAL  : if (instr.getNrOfParameters() == 0 || instr.getNrOfParameters()>2) {
                          instr.setError(instr.getInstruction(), S.getter("Rv32imAssemblerExpectedOneOrTwoArguments"));
                          errors = true;
                          break;
                        }
                        param1 = instr.getParameter(0);
                        if (param1.length != 1 || param1[0].getType() != AssemblerToken.REGISTER) {
                          instr.setError(param1[0], S.getter("AssemblerExpectedRegister"));
                          errors = true;
                          break;
                        }
                        destination = RV32im_state.getRegisterIndex(param1[0].getValue());
                        if (destination < 0 || destination > 31) {
                          instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
                          errors = true;
                          break;
                        }
                        source1 = source2 = 0;
                        immediate = 0;
                        if (instr.getNrOfParameters() == 2) {
                          param2 = instr.getParameter(1);
                          if (param2.length != 1 || !param2[0].isNumber()) {
                            instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
                            errors = true;
                            break;
                          }
                          immediate = param2[0].getNumberValue();
                        }
                        if (operation == INSTR_JR) {
                          source1 = source2 = destination;
                          destination = 0;
                          operation = INSTR_JALR;
                        }
                        if (operation == INSTR_BEQZ || operation == INSTR_BNEZ) {
                          source1 = destination;
                          source2 = destination = 0;
                          operation = (operation == INSTR_BEQZ) ? INSTR_BEQ : INSTR_BNE;
                        }
                        break;
      case INSTR_J    : if (instr.getNrOfParameters() != 1) {
                          instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedOneArgument"));
                          errors = true;
                          break;
                        }
                        param1 = instr.getParameter(0);
                        if (param1.length != 1 || !param1[0].isNumber()) {
                          instr.setError(param1[0], S.getter("AssemblerExpectedImmediateValue"));
                        }
                        immediate = param1[0].getNumberValue();
                        destination = source1 = source2 = 0;
                        operation = INSTR_JAL;
                        break;
      default         : if (instr.getNrOfParameters() < 2 || instr.getNrOfParameters()>3) {
                          instr.setError(instr.getInstruction(), S.getter("Rv32imAssemblerExpectedTwoOrThreeArguments"));
                          errors = true;
                          break;
                        }
                        param1 = instr.getParameter(0);
                        if (param1.length != 1 || param1[0].getType() != AssemblerToken.REGISTER) {
                          instr.setError(param1[0], S.getter("AssemblerExpectedRegister"));
                          errors = true;
                          break;
                        }
                        destination = RV32im_state.getRegisterIndex(param1[0].getValue());
                        if (destination < 0 || destination > 31) {
                          instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
                          errors = true;
                          break;
                        }
                        param2 = instr.getParameter(1);
                        if (param2.length != 1 || param2[0].getType() != AssemblerToken.REGISTER) {
                          instr.setError(param2[0], S.getter("AssemblerExpectedRegister"));
                          errors = true;
                          break;
                        }
                        source1 = source2 = RV32im_state.getRegisterIndex(param2[0].getValue());
                        if (source1 < 0 || source1 > 31) {
                          instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
                          errors = true;
                          break;
                        }
                        immediate = 0;
                        if (instr.getNrOfParameters() == 3) {
                          param3 = instr.getParameter(2);
                          if (param3.length != 1 || !param3[0].isNumber()) {
                            instr.setError(param3[0], S.getter("AssemblerExpectedImmediateValue"));
                            errors = true;
                            break;
                          }
                          immediate = param3[0].getNumberValue();
                        }
                        if (operation != INSTR_JALR) {
                          source2 = destination;
                          destination = 0;
                        }
                        break;
    }
    if (!errors) {
      switch (operation) {
         case INSTR_JAL : long imm = immediate;
                          imm -= instr.getProgramCounter();
                          immediate = (int) imm;
                          if (immediate >= (1<<19) || immediate < -(1<<19)) {
                            instr.setError(instr.getParameter(instr.getNrOfParameters()-1)[0], 
                                S.getter("AssemblerImmediateOutOfRange"));
                            errors = true;
                            break;
                          }
                          instruction = RV32imSupport.getJTypeInstruction(JAL, destination, immediate);
                          break;
         case INSTR_JALR: if (immediate >= (1<<10) || immediate < -(1<<10)) {
        	                instr.setError(instr.getParameter(instr.getNrOfParameters()-1)[0], 
                                S.getter("AssemblerImmediateOutOfRange"));
        	                errors = true;
        	                break;
                          }
                          instruction = RV32imSupport.getITypeInstruction(JALR, destination, 0, source1, immediate);
                          break;
         case INSTR_BEQ :
         case INSTR_BNE :
         case INSTR_BLT :
         case INSTR_BGE :
         case INSTR_BLTU:
         case INSTR_BGEU: imm = immediate;
                          imm -= instr.getProgramCounter();
                          immediate = (int) imm;
                          if (immediate >= (1<<11) || immediate < -(1<<11)) {
                            instr.setError(instr.getParameter(instr.getNrOfParameters()-1)[0], 
                                S.getter("AssemblerImmediateOutOfRange"));
                            errors = true;
                            break;
                          }
                          instruction = RV32imSupport.getBTypeInstruction(BRANCH, operation, source1, source2, immediate);
                          break;
         default        : errors = true;
                          OptionPane.showMessageDialog(null, "Severe bug in RV32imControlTransferInstructions.java");
                          break;
      }
    }
    valid = !errors;
    if (valid) {
      instr.setInstructionByteCode(instruction, 4);
      // DEBUG: System.out.println(String.format("0x%08X 0x%08X", instr.getProgramCounter() , instruction));
    }
    return true;
  }

  public boolean isLabelSupported() { return isPcRelative; }
  public long getLabelAddress(long pc) { return pc+immediate; }

}
