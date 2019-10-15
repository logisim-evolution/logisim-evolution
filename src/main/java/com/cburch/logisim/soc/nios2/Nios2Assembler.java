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

import java.util.LinkedList;

import com.cburch.logisim.soc.util.AbstractAssembler;
import com.cburch.logisim.soc.util.AssemblerToken;

public class Nios2Assembler extends AbstractAssembler {

  public static final int CUSTOM_REGISTER = 256;
  public static final int CONTROL_REGISTER = 257;

  public Nios2Assembler() {
	super();
    super.AddAcceptedParameterType(CUSTOM_REGISTER);
    super.AddAcceptedParameterType(CONTROL_REGISTER);
	/* Add the custom instructions */
    super.addAssemblerExecutionUnit(new Nios2CustomInstructions());
    /* Add all other instructions */
    super.addAssemblerExecutionUnit(new Nios2DataTransferInstructions());
    super.addAssemblerExecutionUnit(new Nios2ArithmeticAndLogicalInstructions());
    super.addAssemblerExecutionUnit(new Nios2ComparisonInstructions());
    super.addAssemblerExecutionUnit(new Nios2ShiftAndRotateInstructions());
    super.addAssemblerExecutionUnit(new Nios2ProgramControlInstructions());
    super.addAssemblerExecutionUnit(new Nios2OtherControlInstructions());
  }

  public boolean usesRoundedBrackets() { return true; }
  public String getHighlightStringIdentifier() { return "asm/nios2"; }

  public void performUpSpecificOperationsOnTokens(LinkedList<AssemblerToken> tokens) {
    for (AssemblerToken token : tokens) {
      if (token.getType() == AssemblerToken.REGISTER) {
    	if (token.getValue().toLowerCase().startsWith("ctl"))
    	  token.setType(CONTROL_REGISTER);
    	else if (token.getValue().toLowerCase().startsWith("c"))
          token.setType(CUSTOM_REGISTER);
      }
    }
  }

}
