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

public class RV32imDecoder {

  private ArrayList<RV32imExecutionUnitInterface> exeUnits;
  
  public RV32imDecoder() {
    exeUnits = new ArrayList<RV32imExecutionUnitInterface>();
    /* Here we add the RV32I base integer instruction set */
    exeUnits.add(new RV32imIntegerRegisterImmediateInstructions());
    exeUnits.add(new RV32imIntegerRegisterRegisterOperations());
    exeUnits.add(new RV32imControlTransferInstructions());
    exeUnits.add(new RV32imLoadAndStoreInstructions());
    exeUnits.add(new Rv32imMemoryOrderingInstructions());
    exeUnits.add(new RV32imEnvironmentCallAndBreakpoints());
    /* Here we add the "M" standard extension for integer multiplication and Division */
    exeUnits.add(new RV32im_M_ExtensionInstructions());
  }
  
  public void decode(int instruction) {
    for (RV32imExecutionUnitInterface exe : exeUnits)
      exe.setBinInstruction(instruction);
  }
  
  public RV32imExecutionUnitInterface getExeUnit() {
    for (RV32imExecutionUnitInterface exe : exeUnits)
      if (exe.isValid())
        return exe;
    return null;
  }

}
