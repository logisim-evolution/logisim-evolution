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

public class RV32imIntegerRegisterImmediateInstructions implements RV32imExecutionUnitInterface {

  private static final int OP_IMM = 0x13;
  private static final int LUI = 0x37;
  private static final int AUIPC = 0x17;
  private static final int ADDI = 0;
  private static final int XORI = 4;
  private static final int SLTIU = 3;
  private static final int SLLI = 1;
  private static final int SRLAI = 5;
  
  private static final int INSTR_ADDI = 0;
  private static final int INSTR_SLTI = 2;
  private static final int INSTR_SLTIU = 3;
  private static final int INSTR_XORI = 4;
  private static final int INSTR_ORI = 6;
  private static final int INSTR_ANDI = 7;
  private static final int INSTR_SLLI = 1;
  private static final int INSTR_SRLI = 5;
  private static final int INSTR_SRAI = 8;
  private static final int INSTR_LUI = 9;
  private static final int INSTR_AUIPC= 10;
  private static final int INSTR_NOP = 11;
  private static final int INSTR_LI = 12;
  private static final int INSTR_SEQZ = 13;
  private static final int INSTR_NOT = 14;
  private static final int INSTR_MV = 15;
  
  private final static String[] AsmOpcodes = {"ADDI","SLLI","SLTI","SLTIU","XORI","SRLI","ORI",
                                              "ANDI","SRAI","LUI","AUIPC","NOP","LI","SEQZ","NOT","MV"};
  /* pseudo instructions:
   * NOP -> ADDI r0,r0,0
   * LI rd,imm -> ADDI rd,r0,imm
   * SEQZ rd,rs -> SLTIU rd,rs,1
   * NOT rd,rs -> XORI rd,rs,-1
   * MV  rd,rs -> ADDI rd,rs,0
   */
  private int instruction = 0;
  private int destination;
  private int source;
  private int immediate;
  private int operation;
  private boolean valid = false;
  
  public ArrayList<String> getInstructions() {
    ArrayList<String> opcodes = new ArrayList<String>();
    for (int i = 0 ; i < AsmOpcodes.length ; i++)
      opcodes.add(AsmOpcodes[i]);
    return opcodes;
  };

  public boolean execute(RV32im_state.ProcessorState state, CircuitState cState) {
    if (!valid)
      return false;
    int result = 0;
    int regVal = state.getRegisterValue(source);
    switch (operation) {
      case INSTR_LI   : 
      case INSTR_NOP  : 
      case INSTR_MV   :
      case INSTR_ADDI : result = regVal+immediate;
                        break;
      case INSTR_SLTI : result = (regVal < immediate) ? 1 : 0;
                        break;
      case INSTR_SEQZ :
      case INSTR_SLTIU: result = (ElfHeader.getLongValue((Integer)regVal)<ElfHeader.getLongValue((Integer)immediate)) ? 1 : 0;
                        break;
      case INSTR_NOT  :
      case INSTR_XORI : result = regVal ^ immediate;
                        break;
      case INSTR_ORI  : result = regVal | immediate;
                        break;
      case INSTR_ANDI : result = regVal & immediate;
                        break;
      case INSTR_SLLI : result = regVal << immediate;
                        break;
      case INSTR_SRLI : Long val1 = ElfHeader.getLongValue((Integer)regVal);
                        val1 >>= immediate;
                        result = ElfHeader.getIntValue(val1);
                        break;
      case INSTR_SRAI : result = regVal >> immediate;
                        break;
      case INSTR_LUI  : result = immediate;
                        break;
      case INSTR_AUIPC: long pc = Long.parseUnsignedLong(String.format("%08X", state.getProgramCounter()),16);
    	                result = ElfHeader.getIntValue(pc+immediate);
    	                break;
      default         : return false;
    }
    state.writeRegister(destination, result);
    return true;
  }

  public String getAsmInstruction() {
    if (!valid)
      return "Unknown";
    StringBuffer s = new StringBuffer();
    s.append(AsmOpcodes[operation].toLowerCase());
    while (s.length()<RV32imSupport.ASM_FIELD_SIZE)
      s.append(" ");
    switch (operation) {
      case INSTR_NOP  : break;
      case INSTR_LI   : s.append(RV32im_state.registerABINames[destination]+","+immediate);
                        break;
      case INSTR_LUI  :
      case INSTR_AUIPC: s.append(RV32im_state.registerABINames[destination]+","+((immediate>>12)&0xFFFFF));
                        break;
      case INSTR_MV   :
      case INSTR_NOT  :
      case INSTR_SEQZ : s.append(RV32im_state.registerABINames[destination]+","+
                                 RV32im_state.registerABINames[source]);
                        break;
      default         : s.append(RV32im_state.registerABINames[destination]+","+
                                 RV32im_state.registerABINames[source]+","+immediate);
    }
    return s.toString();
  }

  public int getBinInstruction() {
    return instruction;
  }

  public boolean setAsmInstruction(String instr) {
	valid = false;
    return false;
  }

  public boolean setBinInstruction(int instr) {
	instruction = instr;
	decodeBin();
    return valid;
  }

  public boolean performedJump() {return false;}
  
  public boolean isValid() {return valid;}
  
  private void decodeBin() {
    int opcode = RV32imSupport.getOpcode(instruction);
    switch (opcode) {
      case LUI    : 
      case AUIPC  : valid = true;
                    destination = RV32imSupport.getDestinationRegisterIndex(instruction);
                    immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.U_TYPE);
                    operation = (opcode == LUI) ? INSTR_LUI : INSTR_AUIPC;
                    return;
      case OP_IMM : valid = true;
                    source = RV32imSupport.getSourceRegister1Index(instruction);
                    destination = RV32imSupport.getDestinationRegisterIndex(instruction);
                    immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.I_TYPE);
                    break;
      default     : valid = false;
                    return;
    }
    /* we land up here only in case of a OP_IMM opcode */
    int op3 = RV32imSupport.getFunct3(instruction);
    if (op3 != SRLAI) {
      operation = op3;
      if (op3 == SLLI) {
    	if (RV32imSupport.getFunct7(instruction) != 0) {
    	  valid = false;
    	  return;
    	}
        immediate = (instruction >> 20)&0x1F;
      }
      if (op3 == ADDI) {
        if (destination == 0 && source == 0 && immediate == 0)
          operation = INSTR_NOP;
        else if (source == 0)
          operation = INSTR_LI;
        else if (immediate == 0)
          operation = INSTR_MV;
      }
      if (op3 == SLTIU) {
        if (immediate == 1)
          operation = INSTR_SEQZ;
      }
      if (op3 == XORI) {
        if (immediate == -1)
          operation = INSTR_NOT;
      }
      return;
    }
    /* we land up here only in case of a SRLI or SRAI instruction */
    int funct7 = RV32imSupport.getFunct7(instruction);
    if (funct7 == 0 || funct7 == 0x20) {
      int bit30 = (instruction >> 30)&1;
      operation = op3+bit30*3;
      immediate = (instruction >> 20)&0x1F;
      return;
    }
    valid = false;
  }

  public String getErrorMessage() { return null; }
}
