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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.InstanceState;

/**
 * Represents a BLIF circuit.
 */
public class BlifCircuitComponent extends HdlCircuitComponent<BlifContentComponent> {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "BLIFCircuit";

  public static final Attribute<BlifContentComponent> CONTENT_ATTR = new HdlContentAttribute<>(BlifContentComponent::create);

  public BlifCircuitComponent() {
    super(_ID, S.getter("blifComponent"), null, false, CONTENT_ATTR);
    this.setIcon(new ArithmeticIcon("BLIF"));
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new BlifCircuitAttributes();
  }

  @Override
  public void propagate(InstanceState state) {
  }
}
