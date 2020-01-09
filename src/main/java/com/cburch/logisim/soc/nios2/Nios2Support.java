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

import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerToken;

public class Nios2Support {

  public static final int ASM_FIELD_SIZE = 10;

  public static final int I_TYPE = 0;
  public static final int R_TYPE = 1;
  public static final int J_TYPE = 2;
  
  public static int getOpcode(int instruction) { return instruction&0x3F; }
  
  public static int getImmediate(int instruction, int type) {
    switch (type) {
      case I_TYPE : return (instruction >> 6)&0xFFFF;
      case J_TYPE : return (instruction >> 6)&0x3FFFFFF;
      default     : return 0;
    }
  }
  
  public static int getRegAIndex(int instruction, int type) {
    switch (type) {
      case I_TYPE :
      case R_TYPE : return (instruction >> 27)&0x1F;
      default     : return 0;
    }
  }

  public static int getRegBIndex(int instruction, int type) {
    switch (type) {
      case I_TYPE :
      case R_TYPE : return (instruction >> 22)&0x1F;
      default     : return 0;
    }
  }

  public static int getRegCIndex(int instruction, int type) {
    switch (type) {
      case R_TYPE : return (instruction >> 17)&0x1F;
      default     : return 0;
    }
  }

  public static int getOPX(int instruction, int type) {
    switch (type) {
      case R_TYPE : return (instruction >> 6)&0x7FF;
      default     : return 0;
    }
  }
	  
  public static int getOPXCode(int instruction, int type) {
    switch (type) {
      case R_TYPE : return (instruction >> 11)&0x3F;
      default     : return 0;
    }
  }
  
  public static int getOPXImm(int instruction, int type) {
	    switch (type) {
	      case R_TYPE : return (instruction >> 6)&0x1F;
	      default     : return 0;
	    }
	  }
	  
  public static int getITypeInstructionCode(int regA , int regB, int imm , int opc) {
    int instruction = opc&0x3F;
    instruction |= (imm&0xFFFF)<<6;
    instruction |= (regB&0x1F)<<22;
    instruction |= (regA&0x1F)<<27;
    return instruction;
  }
  
  public static int getCustomInstructionCode(int regA, int regB, int regC, int opx, int opc) {
    int instruction = opc&0x3F;
    instruction |= (opx&0x7FF)<<6;
    instruction |= (regC&0x1F)<<17;
    instruction |= (regB&0x1F)<<22;
    instruction |= (regA&0x1F)<<27;
    return instruction;
  }

  public static int getRTypeInstructionCode(int regA, int regB, int regC, int opxcode) {
    int instruction = 0x3A;
    instruction |= (opxcode&0x3F)<<11;
    instruction |= (regC&0x1F)<<17;
    instruction |= (regB&0x1F)<<22;
    instruction |= (regA&0x1F)<<27;
    return instruction;
  }

  public static int getRTypeInstructionCode(int regA, int regB, int regC, int opxcode, int opximm) {
    int instruction = 0x3A;
    instruction |= (opxcode&0x3F)<<11;
    instruction |= (opximm&0x1F)<<6;
    instruction |= (regC&0x1F)<<17;
    instruction |= (regB&0x1F)<<22;
    instruction |= (regA&0x1F)<<27;
    return instruction;
  }

  public static int getJTypeInstructionCode(int imm , int opc) {
    int instruction = opc&0x3F;
    instruction |= (imm&0x3FFFFFF)<<6;
    return instruction;
  }
  
  public static boolean isCorrectRegister(AssemblerAsmInstruction instr, int index) {
    if (index < 0 || index >= instr.getNrOfParameters()) return false;
    AssemblerToken[] tok = instr.getParameter(index);
    if (tok.length != 1) {
      instr.setError(tok[0], S.getter("AssemblerExpectedRegister"));
      return false;
    }
    if (tok[0].getType() == Nios2Assembler.CUSTOM_REGISTER) {
      instr.setError(tok[0], S.getter("Nios2CannotUseCustomRegister"));
      return false;
    }
    if (tok[0].getType() == Nios2Assembler.CONTROL_REGISTER) {
        instr.setError(tok[0], S.getter("Nios2CannotUseControlRegister"));
        return false;
      }
    if (tok[0].getType() != AssemblerToken.REGISTER) {
      instr.setError(tok[0], S.getter("AssemblerExpectedRegister"));
      return false;
    }
    int regid = Nios2State.getRegisterIndex(tok[0].getValue());
    if (regid < 0 || regid > 31) {
      instr.setError(tok[0], S.getter("AssemblerUnknownRegister"));
      return false;
    }
    return true;
  }
  
  public static int getRegisterIndex(AssemblerAsmInstruction instr, int index) {
    if (!isCorrectRegister(instr, index)) return 0;
    return Nios2State.getRegisterIndex(instr.getParameter(index)[0].getValue());
  }
  
}
