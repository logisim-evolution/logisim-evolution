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
import com.cburch.logisim.soc.file.ElfHeader;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;

public class RV32imIntegerRegisterRegisterOperations implements AssemblerExecutionInterface {

  private static final int OP = 0x33;
  private static final int ADD_SUB = 0;
  private static final int SRL_SRA = 5;

  private static final int INSTR_ADD = 0;
  private static final int INSTR_SLL = 1;
  private static final int INSTR_SLT = 2;
  private static final int INSTR_SLTU = 3;
  private static final int INSTR_XOR = 4;
  private static final int INSTR_SRL = 5;
  private static final int INSTR_OR = 6;
  private static final int INSTR_AND = 7;
  private static final int INSTR_SUB = 8;
  private static final int INSTR_SRA = 9;
  private static final int INSTR_SNEZ = 10;

  /* Pseudo instructions:
   * SNEZ rd,rs -> SLTU rd,x0,rs
   */
  private final static String[] AsmOpcodes = {"ADD","SLL","SLT","SLTU","XOR","SRL","OR","AND","SUB","SRA","SNEZ"};

  private int instruction = 0;
  private int destination;
  private int source1;
  private int source2;
  private int operation;
  private boolean valid = false;
      
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
    int opp1 = cpuState.getRegisterValue(source1);
    int opp2 = cpuState.getRegisterValue(source2);
    int result = 0;
    switch (operation) {
      case INSTR_ADD  : result = opp1 + opp2;
                        break;
      case INSTR_SUB  : result = opp1 - opp2;
                        break;
      case INSTR_SLL  : result = opp1 << (opp2&0x1F);
                        break;
      case INSTR_SLT  : result = (opp1 < opp2) ? 1 : 0;
                        break;
      case INSTR_SNEZ :
      case INSTR_SLTU : result = (ElfHeader.getLongValue((Integer)opp1)<ElfHeader.getLongValue((Integer)opp2)) ? 1 : 0;
                        break;
      case INSTR_XOR  : result = opp1 ^ opp2;
                        break;
      case INSTR_SRL  : Long val1 = ElfHeader.getLongValue((Integer)opp1);
                        val1 >>= (opp2&0x1F);
                        result = ElfHeader.getIntValue(val1);
                        break;
      case INSTR_OR   : result = opp1 | opp2;
                        break;
      case INSTR_AND  : result = opp1 & opp2;
                        break;
      case INSTR_SRA  : result = opp1 >> (opp2&0x1F);
                        break;
      default         : return false;
    }
    cpuState.writeRegister(destination, result);
    return true;
  }

  public String getAsmInstruction() {
    if (!valid)
      return "Unknown";
    StringBuffer s = new StringBuffer();
    s.append(AsmOpcodes[operation].toLowerCase());
    while (s.length()<RV32imSupport.ASM_FIELD_SIZE)
      s.append(" ");
    s.append(RV32im_state.registerABINames[destination]+","+
             ((operation == INSTR_SNEZ) ? "" : RV32im_state.registerABINames[source1]+",")+
             RV32im_state.registerABINames[source2]);
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

  public boolean performedJump() { return false; }

  public boolean isValid() { return valid; }
  
  private boolean decodeBin() {
    if (RV32imSupport.getOpcode(instruction)!= OP)
      return false;
    int funct7 = RV32imSupport.getFunct7(instruction);
    int funct3 = RV32imSupport.getFunct3(instruction);
    destination = RV32imSupport.getDestinationRegisterIndex(instruction);
    source1 = RV32imSupport.getSourceRegister1Index(instruction);
    source2 = RV32imSupport.getSourceRegister2Index(instruction);
    switch (funct3) {
       case ADD_SUB : if (funct7 == 0) {
                        operation = INSTR_ADD;
                        break;
                      }
                      if (funct7 == 0x20) {
                        operation = INSTR_SUB;
                        break;
                      }
                      return false;
       case SRL_SRA : if (funct7 == 0) {
                        operation = INSTR_SRL;
                        break;
                      }
                      if (funct7 == 0x20) {
                        operation = INSTR_SRA;
                        break;
                      }
                      return false;
       default      : if (funct7 != 0)
                        return false;
                      operation = funct3;
                      break;
    }
    if (operation == INSTR_SLTU && source1 == 0)
      operation = INSTR_SNEZ;
    return true;
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
    if (instr.getNrOfParameters() != (operation == INSTR_SNEZ ? 2 : 3)) {
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedThreeArguments"));
      valid = false;
      return true;
    }
    param1 = instr.getParameter(0);
    param2 = instr.getParameter(1);
    if (operation != INSTR_SNEZ)
      param3 = instr.getParameter(2);
    else
      param3 = param2;
    if (param1.length != 1 || param1[0].getType() != AssemblerToken.REGISTER) {
      errors = true;
      instr.setError(param1[0], S.getter("AssemblerExpectedRegister"));
    }
    if (param2.length != 1 || param2[0].getType() != AssemblerToken.REGISTER) {
      errors = true;
      instr.setError(param2[0], S.getter("AssemblerExpectedRegister"));
    }
    if (param3 != param2 && (param3.length != 1 || param3[0].getType() != AssemblerToken.REGISTER)) {
      errors = true;
      instr.setError(param3[0], S.getter("AssemblerExpectedRegister"));
    }
    destination = RV32im_state.getRegisterIndex(param1[0].getValue());
    source1 = RV32im_state.getRegisterIndex(param2[0].getValue());
    source2 = RV32im_state.getRegisterIndex(param3[0].getValue());
    if (destination < 0 || destination > 31) {
      errors = true;
      instr.setError(param1[0], S.getter("AssemblerUnknownRegister"));
    }
    if (source1 < 0 || source1 > 31) {
      errors = true;
      instr.setError(param2[0], S.getter("AssemblerUnknownRegister"));
    }
    if (param3 != param2 && (source2 < 0 || source2 > 31)) {
      errors = true;
      instr.setError(param3[0], S.getter("AssemblerUnknownRegister"));
    }
    if (operation == INSTR_SNEZ) {
      source1 = 0;
      operation = INSTR_SLTU;
    }
    valid = !errors;
    if (valid) {
      int funct7 = (operation == INSTR_SUB || operation == INSTR_SRA) ? 0x20 : 0;
      int funct3 = operation == INSTR_SUB ? ADD_SUB : operation == INSTR_SRA ? SRL_SRA : operation;
      instruction = RV32imSupport.getRTypeInstruction(OP, destination, funct3, source1, source2, funct7);
      instr.setInstructionByteCode(instruction, 4);
      // DEBUG : System.out.println(String.format("0x%08X 0x%08X", instr.getProgramCounter(), instruction));
    }
    return true;
  }

}
