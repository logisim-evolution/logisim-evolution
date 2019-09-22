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

import java.util.ArrayList;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.file.ElfHeader;

public class RV32imControlTransferInstructions implements RV32imExecutionUnitInterface {
    
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
   * JR rs => JALR x0,rx+imm  
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

  public boolean execute(RV32im_state.ProcessorState state, CircuitState cState) {
    if (!valid)
      return false;
    jumped = false;
    int target = state.getProgramCounter()+immediate;
    int nextPc = state.getProgramCounter()+4;
    int reg1 = state.getRegisterValue(source1);
    int reg2 = state.getRegisterValue(source2);
    switch (operation) {
      case INSTR_JAL  :
      case INSTR_J    : state.setProgramCounter(target);
                        jumped = true;
                        state.writeRegister(destination, nextPc);
                        return true;
      case INSTR_RET  :
      case INSTR_JR   :
      case INSTR_JALR : target = state.getRegisterValue(source1)+immediate;
                        target = (target >>1)<<1;
                        state.setProgramCounter(target);
                        jumped = true;
                        state.writeRegister(destination, nextPc);
                        return true;
      case INSTR_BEQZ :
      case INSTR_BEQ  : if (reg1 == reg2) {
                          jumped = true;
                          state.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BNEZ :
      case INSTR_BNE  : if (reg1 != reg2) {
                          jumped = true;
                          state.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BLT  : if (reg1 < reg2) {
                          jumped = true;
                          state.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BGE  : if (reg1 >= reg2) {
                          jumped = true;
                          state.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BLTU : if (ElfHeader.getLongValue((Integer)reg1) < ElfHeader.getLongValue((Integer)reg2)) {
                          jumped = true;
                          state.setProgramCounter(target);
                        }
                        return true;
      case INSTR_BGEU : if (ElfHeader.getLongValue((Integer)reg1) >= ElfHeader.getLongValue((Integer)reg2)) {
                          jumped = true;
                          state.setProgramCounter(target);
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
  public boolean isPcRelative() { return isPcRelative; }

  public boolean setAsmInstruction(String instr) {
    valid = false;
    return valid;
  }

  public boolean setBinInstruction(int instr) {
    instruction = instr;
    jumped = false;
    valid = decodeBin();
    return valid;
  }
  
  public int getOffset() { return immediate; }

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
                    isPcRelative = true;
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
  
}
