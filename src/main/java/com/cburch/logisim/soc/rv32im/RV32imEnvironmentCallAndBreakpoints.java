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
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;

public class RV32imEnvironmentCallAndBreakpoints implements AssemblerExecutionInterface {

  private final static int SYSTEM = 0x73;
  
  private final static int INSTR_ECALL = 0;
  private final static int INSTR_EBREAK = 1;
  
  private final static String[] AsmOpcodes = {"ECALL","EBREAK"};

  private int instruction = 0;
  private int operation;
  private boolean valid;
  
  public ArrayList<String> getInstructions() {
    ArrayList<String> opcodes = new ArrayList<String>();
    for (int i = 0 ; i < AsmOpcodes.length ; i++)
      opcodes.add(AsmOpcodes[i]);
    return opcodes;
  };

  public boolean execute(Object state, CircuitState cState) {
    if (!valid)
      return false;
    OptionPane.showMessageDialog(null, S.get("Rv32imECABNotImplmented"));
    return true;
  }

  public String getAsmInstruction() {
    if (!valid)
      return null;
    return AsmOpcodes[operation].toLowerCase();
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
    if (RV32imSupport.getOpcode(instruction) == SYSTEM) {
      int funct12 = (instruction >> 20)&0xFFF;
      if (funct12 > 1)
        return false;
      operation = funct12;
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
	if (instr.getNrOfParameters() != 0) {
	  instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedNoArguments"));
	  valid = false;
	  return true;
	}
	instruction = RV32imSupport.getITypeInstruction(SYSTEM, 0, 0, 0, operation);
    valid = true;
    instr.setInstructionByteCode(instruction, 4);
    return true;
  }



}
