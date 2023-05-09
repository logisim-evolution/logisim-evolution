/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

class NotGate extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "NOT Gate";

  static void configureLabel(Instance instance, boolean isRectangular, Location control) {
    Object facing = instance.getAttributeValue(StdAttr.FACING);
    final var bds = instance.getBounds();
    int x;
    int y;
    int halign;
    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
      x = bds.getX() + bds.getWidth() / 2 + 2;
      y = bds.getY() - 2;
      halign = TextField.H_LEFT;
    } else { // west or east
      y = isRectangular ? bds.getY() - 2 : bds.getY();
      if (control != null && control.getY() == bds.getY()) {
        // the control line will get in the way
        x = control.getX() + 2;
        halign = TextField.H_LEFT;
      } else {
        x = bds.getX() + bds.getWidth() / 2;
        halign = TextField.H_CENTER;
      }
    }
    instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign, TextField.V_BASELINE);
  }

  public static final AttributeOption SIZE_NARROW =
      new AttributeOption(20, S.getter("gateSizeNarrowOpt"));
  public static final AttributeOption SIZE_WIDE =
      new AttributeOption(30, S.getter("gateSizeWideOpt"));

  public static final Attribute<AttributeOption> ATTR_SIZE =
      Attributes.forOption(
          "size", S.getter("gateSizeAttr"), new AttributeOption[] {SIZE_NARROW, SIZE_WIDE});
  private static final String RECT_LABEL = "1";

  // private static final Icon toolIconDin = Icons.getIcon("dinNotGate.gif");

  public static final InstanceFactory FACTORY = new NotGate();

  private NotGate() {
    super(_ID, S.getter("notGateComponent"), new AbstractBufferHdlGenerator(true));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          StdAttr.WIDTH,
          ATTR_SIZE,
          GateAttributes.ATTR_OUTPUT,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
        },
        new Object[] {
          Direction.EAST,
          BitWidth.ONE,
          SIZE_WIDE,
          GateAttributes.OUTPUT_01,
          "",
          StdAttr.DEFAULT_LABEL_FONT,
        });
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
    String gateShape = AppPreferences.GATE_SHAPE.get();
    configureLabel(instance, gateShape.equals(AppPreferences.SHAPE_RECTANGULAR), null);
  }

  private void configurePorts(Instance instance) {
    Object size = instance.getAttributeValue(ATTR_SIZE);
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    int dx = size == SIZE_NARROW ? -20 : -30;

    final var ports = new Port[2];
    ports[0] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    Location out = Location.create(0, 0, true).translate(facing, dx);
    ports[1] = new Port(out.getX(), out.getY(), Port.INPUT, StdAttr.WIDTH);
    instance.setPorts(ports);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var CompleteName = new StringBuilder();
    CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()).toUpperCase());
    final var width = attrs.getValue(StdAttr.WIDTH);
    if (width.getWidth() > 1) CompleteName.append("_BUS");
    return CompleteName.toString();
  }

  @Override
  protected Object getInstanceFeature(final Instance instance, Object key) {
    if (key == ExpressionComputer.class) {
      return (ExpressionComputer)
          expressionMap -> {
            int width = instance.getAttributeValue(StdAttr.WIDTH).getWidth();
            for (var b = 0; b < width; b++) {
              final var e = expressionMap.get(instance.getPortLocation(1), b);
              if (e != null) {
                expressionMap.put(instance.getPortLocation(0), b, Expressions.not(e));
              }
            }
          };
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Object value = attrs.getValue(ATTR_SIZE);
    if (value == SIZE_NARROW) {
      final var facing = attrs.getValue(StdAttr.FACING);
      if (facing == Direction.SOUTH) return Bounds.create(-9, -20, 18, 20);
      if (facing == Direction.NORTH) return Bounds.create(-9, 0, 18, 20);
      if (facing == Direction.WEST) return Bounds.create(0, -9, 20, 18);
      return Bounds.create(-20, -9, 20, 18);
    } else {
      Direction facing = attrs.getValue(StdAttr.FACING);
      if (facing == Direction.SOUTH) return Bounds.create(-9, -30, 18, 30);
      if (facing == Direction.NORTH) return Bounds.create(-9, 0, 18, 30);
      if (facing == Direction.WEST) return Bounds.create(0, -9, 30, 18);
      return Bounds.create(-30, -9, 30, 18);
    }
  }

  @Override
  public boolean hasThreeStateDrivers(AttributeSet attrs) {
    if (attrs.containsAttribute(GateAttributes.ATTR_OUTPUT))
      return !(attrs.getValue(GateAttributes.ATTR_OUTPUT) == GateAttributes.OUTPUT_01);
    else return false;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_SIZE || attr == StdAttr.FACING) {
      instance.recomputeBounds();
      configurePorts(instance);
      String gateShape = AppPreferences.GATE_SHAPE.get();
      configureLabel(instance, gateShape.equals(AppPreferences.SHAPE_RECTANGULAR), null);
    }
  }

  private void paintBase(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var facing = painter.getAttributeValue(StdAttr.FACING);
    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    g.translate(x, y);
    var rotate = 0.0;
    if (facing != null && facing != Direction.EAST && g instanceof Graphics2D) {
      rotate = -facing.toRadians();
      ((Graphics2D) g).rotate(rotate);
    }

    Object shape = painter.getGateShape();
    if (shape == AppPreferences.SHAPE_RECTANGULAR) {
      paintRectangularBase(g, painter);

      //    } else if (shape == AppPreferences.SHAPE_DIN40700) {
      //      int width = painter.getAttributeValue(ATTR_SIZE) == SIZE_NARROW ? 20 : 30;
      //      PainterDin.paintAnd(painter, width, 18, true);

    } else {
      PainterShaped.paintNot(painter);
    }

    if (rotate != 0.0) {
      ((Graphics2D) g).rotate(-rotate);
    }
    g.translate(-x, -y);
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    paintBase(painter);
  }

  //
  // painting methods
  //
  @Override
  public void paintIcon(InstancePainter painter) {
    final var g = (Graphics2D) painter.getGraphics();
    if (painter.getGateShape() == AppPreferences.SHAPE_RECTANGULAR)
      AbstractGate.paintIconIEC(g, RECT_LABEL, true, true);
    else AbstractGate.paintIconBufferAnsi(g, true, false);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    painter.getGraphics().setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    paintBase(painter);
    painter.drawPorts();
    painter.drawLabel();
  }

  private void paintRectangularBase(Graphics g, InstancePainter painter) {
    GraphicsUtil.switchToWidth(g, 2);
    if (painter.getAttributeValue(ATTR_SIZE) == SIZE_NARROW) {
      g.drawRect(-20, -9, 14, 18);
      GraphicsUtil.drawCenteredText(g, RECT_LABEL, -13, 0);
      g.drawOval(-6, -3, 6, 6);
    } else {
      g.drawRect(-30, -9, 20, 18);
      GraphicsUtil.drawCenteredText(g, RECT_LABEL, -20, 0);
      g.drawOval(-10, -5, 9, 9);
    }
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    final var in = state.getPortValue(1);
    var out = in.not();
    out = Buffer.repair(state, out);
    state.setPort(0, out, GateAttributes.DELAY);
  }
}
