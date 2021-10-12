/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.rv32im;

import com.cburch.logisim.soc.util.AbstractAssembler;
import com.cburch.logisim.soc.util.AssemblerToken;
import java.util.LinkedList;

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

  public boolean usesRoundedBrackets() {
    return true;
  }

  public String getHighlightStringIdentifier() {
    return "asm/riscv";
  }

  public void performUpSpecificOperationsOnTokens(LinkedList<AssemblerToken> tokens) {}
}
