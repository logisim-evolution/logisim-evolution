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

import java.util.LinkedList;

import com.cburch.logisim.soc.util.AbstractAssembler;
import com.cburch.logisim.soc.util.AssemblerToken;

public class RV32imAssembler extends AbstractAssembler {

  
  public RV32imAssembler() {
	super();
    /* Here we add the RV32I base integer instruction set */
    super.addAssemblerExecutionUnit(new RV32imIntegerRegisterImmediateInstructions());
    super.addAssemblerExecutionUnit(new RV32imIntegerRegisterRegisterOperations());
    super.addAssemblerExecutionUnit(new RV32imControlTransferInstructions());
    super.addAssemblerExecutionUnit(new RV32imLoadAndStoreInstructions());
    super.addAssemblerExecutionUnit(new Rv32imMemoryOrderingInstructions());
    super.addAssemblerExecutionUnit(new RV32imEnvironmentCallAndBreakpoints());
    /* Here we add the "M" standard extension for integer multiplication and Division */
    super.addAssemblerExecutionUnit(new RV32im_M_ExtensionInstructions());
  }
  
  public boolean usesRoundedBrackets() { return true; }
  public String getHighlightStringIdentifier() { return "asm/riscv"; }
  public void performUpSpecificOperationsOnTokens(LinkedList<AssemblerToken> tokens) { }
}
