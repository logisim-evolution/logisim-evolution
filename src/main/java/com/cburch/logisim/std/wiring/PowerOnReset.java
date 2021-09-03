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
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

public class PowerOnReset extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "POR";

  public static final PowerOnReset FACTORY = new PowerOnReset();

  public PowerOnReset() {
    super(_ID, S.getter("PowerOnResetComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          new DurationAttribute("PorHighDuration", S.getter("porHighAttr"), 1, 10, false),
        },
        new Object[] {
          Direction.EAST, 2,
        });
    setFacingAttribute(StdAttr.FACING);
    setIconName("por.png");
  }

  private static class PORState implements InstanceData, Cloneable, ActionListener {

    private boolean value;
    private final Timer tim;
    private final InstanceState state;

    public PORState(InstanceState state) {
      value = true;
      DurationAttribute attr =
          (DurationAttribute) state.getAttributeSet().getAttribute("PorHighDuration");
      tim = new Timer(state.getAttributeValue(attr) * 1000, this);
      tim.start();
      this.state = state;
    }

    public boolean getValue() {
      return value;
    }

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == tim) {
        if (value) {
          value = false;
          state.getInstance().fireInvalidated();
          tim.stop();
        }
      }
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, BitWidth.ONE)});
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    return Bounds.create(0, -20, 200, 40).rotate(Direction.WEST, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    java.awt.Graphics g = painter.getGraphics();
    Bounds bds = painter.getInstance().getBounds();
    int x = bds.getX();
    int y = bds.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.ORANGE);
    g.fillRect(x, y, bds.getWidth(), bds.getHeight());
    g.setColor(Color.BLACK);
    g.drawRect(x, y, bds.getWidth(), bds.getHeight());
    Font old = g.getFont();
    g.setFont(old.deriveFont(18.0f).deriveFont(Font.BOLD));
    FontMetrics fm = g.getFontMetrics();
    String txt = "Power-On Reset";
    int wide = Math.max(bds.getWidth(), bds.getHeight());
    int offset = (wide - fm.stringWidth(txt)) / 2;
    Direction facing = painter.getAttributeValue(StdAttr.FACING);
    if (((facing == Direction.NORTH) || (facing == Direction.SOUTH)) && (g instanceof Graphics2D)) {
      Graphics2D g2 = (Graphics2D) g;
      int xpos = facing == Direction.NORTH ? x + 20 - fm.getDescent() : x + 20 + fm.getDescent();
      int ypos = facing == Direction.NORTH ? y + offset : y + bds.getHeight() - offset;
      g2.translate(xpos, ypos);
      g2.rotate(facing.toRadians());
      g.drawString(txt, 0, 0);
      g2.rotate(-facing.toRadians());
      g2.translate(-xpos, -ypos);
    } else {
      g.drawString(txt, x + offset, y + fm.getDescent() + 20);
    }
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    PORState ret = (PORState) state.getData();
    if (ret == null) {
      ret = new PORState(state);
      state.setData(ret);
    }
    state.setPort(0, Value.createKnown(BitWidth.ONE, ret.getValue() ? 1 : 0), 0);
    // TODO Auto-generated method stub

  }
}
