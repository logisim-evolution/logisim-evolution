/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import java.util.Collection;
import java.util.List;

class DefaultAppearance {
  private DefaultAppearance() {
    // dummy, private
  }

  public static List<CanvasObject> build(Collection<Instance> pins, AttributeOption style, boolean Fixed, String CircuitName) {
    if (style == CircuitAttributes.APPEAR_CLASSIC) {
      return DefaultClassicAppearance.build(pins);
    } else if (style == CircuitAttributes.APPEAR_FPGA) {
      return DefaultHolyCrossAppearance.build(pins, CircuitName);
    } else {
      return DefaultEvolutionAppearance.build(pins, CircuitName, Fixed);
    }
  }

  static void sortPinList(List<Instance> pins, Direction facing) {
    if (facing == Direction.NORTH || facing == Direction.SOUTH) Location.sortHorizontal(pins);
    else Location.sortVertical(pins);
  }
}
