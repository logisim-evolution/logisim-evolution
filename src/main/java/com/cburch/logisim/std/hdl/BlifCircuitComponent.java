/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;

/**
 * Represents a BLIF circuit.
 */
public class BlifCircuitComponent extends GenericInterfaceComponent {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "BLIFCircuit";

  public BlifCircuitComponent() {
    super(_ID, S.getter("blifComponent"), null, false);
    this.setIcon(new ArithmeticIcon("BLIF"));
  }

  @Override
  public void propagate(InstanceState state) {
  }

  @Override
  protected String getGIAttributesName(AttributeSet attrs) {
    return "generic";
  }

  @Override
  protected Port[] getGIAttributesInputs(AttributeSet attrs) {
    return new Port[0];
  }

  @Override
  protected Port[] getGIAttributesOutputs(AttributeSet attrs) {
    return new Port[0];
  }

}
