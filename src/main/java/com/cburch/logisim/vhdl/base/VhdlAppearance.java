/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.appear.CircuitAppearance;
import com.cburch.logisim.circuit.appear.DefaultClassicAppearance;
import com.cburch.logisim.circuit.appear.DefaultEvolutionAppearance;
import com.cburch.logisim.circuit.appear.DefaultHolyCrossAppearance;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import java.util.List;

public class VhdlAppearance extends CircuitAppearance {
  VhdlAppearance(List<CanvasObject> shapes) {
    super(null);
    setObjectsForce(shapes, true);
  }

  static VhdlAppearance create(List<Instance> pins, String name, AttributeOption style) {
    if (style == StdAttr.APPEAR_CLASSIC) {
      return new VhdlAppearance(DefaultClassicAppearance.build(pins));
    } else if (style == StdAttr.APPEAR_FPGA) {
      return new VhdlAppearance(DefaultHolyCrossAppearance.build(pins, name));
    } else {
      return new VhdlAppearance(DefaultEvolutionAppearance.build(pins, name, true));
    }
  }
}
