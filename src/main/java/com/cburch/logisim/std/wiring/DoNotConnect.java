/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import java.awt.Color;
import java.awt.Graphics2D;

public class DoNotConnect extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "NoConnect";

  public DoNotConnect() {
    super(_ID, S.getter("noConnectionComponent"), new InlinedHdlGeneratorFactory());
    setIconName("noconnect.gif");
    setAttributes(new Attribute[] {StdAttr.WIDTH}, new Object[] {BitWidth.ONE});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setPorts(new Port[] {new Port(0, 0, Port.INOUT, StdAttr.WIDTH)});
  }

  private void drawInstance(InstancePainter painter, boolean isGhost) {
    Graphics2D g = (Graphics2D) painter.getGraphics().create();
    Location loc = painter.getLocation();
    g.setColor(isGhost ? Color.GRAY : Color.RED);
    g.drawLine(loc.getX() - 5, loc.getY() - 5, loc.getX() + 5, loc.getY() + 5);
    g.drawLine(loc.getX() - 5, loc.getY() + 5, loc.getX() + 5, loc.getY() - 5);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(-5, -5, 10, 10);
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    drawInstance(painter, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    drawInstance(painter, false);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    // do nothing
  }
}
