package com.cburch.logisim.gui.icons;

import java.awt.Graphics2D;

import com.cburch.logisim.circuit.SubcircuitFactory;

public class CircuitIcon  extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    SubcircuitFactory.paintEvolutionIcon(g2);
  }

}
