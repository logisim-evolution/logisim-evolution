/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.util;

public interface AbstractExecutionUnitWithLabelSupport extends AssemblerExecutionInterface {

  public abstract boolean isLabelSupported();
  public abstract long getLabelAddress(long pc);
  public abstract String getAsmInstruction(String label);
}
