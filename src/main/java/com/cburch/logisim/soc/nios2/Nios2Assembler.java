/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.nios2;

import com.cburch.logisim.soc.util.AbstractAssembler;
import com.cburch.logisim.soc.util.AssemblerToken;
import java.util.LinkedList;

public class Nios2Assembler extends AbstractAssembler {

  public static final int CUSTOM_REGISTER = 256;
  public static final int CONTROL_REGISTER = 257;

  public Nios2Assembler() {
    super();
    super.addAcceptedParameterType(CUSTOM_REGISTER);
    super.addAcceptedParameterType(CONTROL_REGISTER);
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

  @Override
  public boolean usesRoundedBrackets() {
    return true;
  }

  @Override
  public String getHighlightStringIdentifier() {
    return "asm/nios2";
  }

  @Override
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
