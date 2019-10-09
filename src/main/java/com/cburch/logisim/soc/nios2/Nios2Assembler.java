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

import java.util.ArrayList;

import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerInterface;

public class Nios2Assembler implements AssemblerInterface {

  private ArrayList<AssemblerExecutionInterface> exeUnits;
  
  public Nios2Assembler() {
    exeUnits = new ArrayList<AssemblerExecutionInterface>();
  }

  @Override
  public void decode(int instruction) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean assemble(AssemblerAsmInstruction instruction) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public AssemblerExecutionInterface getExeUnit() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ArrayList<String> getOpcodes() {
    // TODO Auto-generated method stub
    return new ArrayList<String>();
  }

  public int getInstructionSize(String opcode) { 
    for (AssemblerExecutionInterface exe : exeUnits) {
      int size = exe.getInstructionSizeInBytes(opcode);
      if (size > 0) return size;
    }
    return 1; /* to make sure that instructions are not overwritten */
  }
  
  public boolean usesRoundedBrackets() { return true; }
  public String getHighlightStringIdentifier() { return "asm/nios2"; }

}
