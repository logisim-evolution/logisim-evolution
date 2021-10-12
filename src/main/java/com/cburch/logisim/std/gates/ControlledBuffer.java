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

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.WireRepair;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;

class ControlledBuffer extends InstanceFactory {

  // FIXME: No _ID const here due to constructor logic

  private static final AttributeOption RIGHT_HANDED =
      new AttributeOption("right", S.getter("controlledRightHanded"));
  private static final AttributeOption LEFT_HANDED =
      new AttributeOption("left", S.getter("controlledLeftHanded"));
  private static final Attribute<AttributeOption> ATTR_CONTROL =
      Attributes.forOption(
          "control",
          S.getter("controlledControlOption"),
          new AttributeOption[] {RIGHT_HANDED, LEFT_HANDED});

  public static final ComponentFactory FACTORY_BUFFER = new ControlledBuffer(false);
  public static final ComponentFactory FACTORY_INVERTER = new ControlledBuffer(true);

  private final boolean isInverter;

  private ControlledBuffer(boolean isInverter) {
    super(
        isInverter ? "Controlled Inverter" : "Controlled Buffer",
        isInverter
            ? S.getter("controlledInverterComponent")
            : S.getter("controlledBufferComponent"),
        new ControlledBufferHdlGenerator());
    this.isInverter = isInverter;
    if (isInverter) {
      setAttributes(
          new Attribute[] {
            StdAttr.FACING,
            StdAttr.WIDTH,
            NotGate.ATTR_SIZE,
            ATTR_CONTROL,
            StdAttr.LABEL,
            StdAttr.LABEL_FONT
          },
          new Object[] {
            Direction.EAST,
            BitWidth.ONE,
            NotGate.SIZE_WIDE,
            RIGHT_HANDED,
            "",
            StdAttr.DEFAULT_LABEL_FONT
          });
    } else {
      setAttributes(
          new Attribute[] {
            StdAttr.FACING, StdAttr.WIDTH, ATTR_CONTROL, StdAttr.LABEL, StdAttr.LABEL_FONT
          },
          new Object[] {
            Direction.EAST, BitWidth.ONE, RIGHT_HANDED, "", StdAttr.DEFAULT_LABEL_FONT
          });
    }
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
  }

  public boolean isInverter() {
    return isInverter;
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    configurePorts(instance);
    NotGate.configureLabel(instance, false, instance.getPortLocation(2));
  }

  private void configurePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    final var bds = getOffsetBounds(instance.getAttributeSet());
    int d = Math.max(bds.getWidth(), bds.getHeight()) - 20;
    final var loc0 = Location.create(0, 0);
    final var loc1 = loc0.translate(facing.reverse(), 20 + d);
    Location loc2;
    if (instance.getAttributeValue(ATTR_CONTROL) == LEFT_HANDED) {
      loc2 = loc0.translate(facing.reverse(), 10 + d, 10);
    } else {
      loc2 = loc0.translate(facing.reverse(), 10 + d, -10);
    }

    final var ports = new Port[3];
    ports[0] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ports[1] = new Port(loc1.getX(), loc1.getY(), Port.INPUT, StdAttr.WIDTH);
    ports[2] = new Port(loc2.getX(), loc2.getY(), Port.INPUT, 1);
    instance.setPorts(ports);
  }

  @Override
  public Object getInstanceFeature(final Instance instance, Object key) {
    if (key == WireRepair.class) {
      return (WireRepair) data -> {
        final var port2 = instance.getPortLocation(2);
        return data.getPoint().equals(port2);
      };
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    var w = 20;
    if (isInverter && !NotGate.SIZE_NARROW.equals(attrs.getValue(NotGate.ATTR_SIZE))) {
      w = 30;
    }
    final var facing = attrs.getValue(StdAttr.FACING);
    if (facing == Direction.NORTH) return Bounds.create(-10, 0, 20, w);
    if (facing == Direction.SOUTH) return Bounds.create(-10, -w, 20, w);
    if (facing == Direction.WEST) return Bounds.create(0, -10, w, 20);
    return Bounds.create(-w, -10, w, 20);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING || attr == NotGate.ATTR_SIZE) {
      instance.recomputeBounds();
      configurePorts(instance);
      NotGate.configureLabel(instance, false, instance.getPortLocation(2));
    } else if (attr == ATTR_CONTROL) {
      configurePorts(instance);
      NotGate.configureLabel(instance, false, instance.getPortLocation(2));
    }
  }

  //
  // graphics methods
  //
  @Override
  public void paintGhost(InstancePainter painter) {
    paintShape(painter);
  }

  @Override
  public void paintIcon(InstancePainter painter) {
    final var g = (Graphics2D) painter.getGraphics();
    if (painter.getGateShape() == AppPreferences.SHAPE_RECTANGULAR)
      AbstractGate.paintIconIEC(g, "EN1", isInverter, false);
    else
      AbstractGate.paintIconBufferAnsi(g, isInverter, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var face = painter.getAttributeValue(StdAttr.FACING);

    final var g = painter.getGraphics();

    // draw control wire
    GraphicsUtil.switchToWidth(g, 3);
    final var pt0 = painter.getInstance().getPortLocation(2);
    Location pt1;
    if (painter.getAttributeValue(ATTR_CONTROL) == LEFT_HANDED) {
      pt1 = pt0.translate(face, 0, 6);
    } else {
      pt1 = pt0.translate(face, 0, -6);
    }
    if (painter.getShowState()) {
      g.setColor(painter.getPortValue(2).getColor());
    }
    g.drawLine(pt0.getX(), pt0.getY(), pt1.getX(), pt1.getY());

    // draw triangle
    g.setColor(Color.BLACK);
    paintShape(painter);

    // draw input and output pins
    if (!painter.isPrintView()) {
      painter.drawPort(0);
      painter.drawPort(1);
    }
    painter.drawLabel();
  }

  private void paintShape(InstancePainter painter) {
    final var facing = painter.getAttributeValue(StdAttr.FACING);
    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    var rotate = 0.0;
    final var g = painter.getGraphics();
    g.translate(x, y);
    if (facing != Direction.EAST && g instanceof Graphics2D) {
      rotate = -facing.toRadians();
      ((Graphics2D) g).rotate(rotate);
    }

    if (isInverter) {
      PainterShaped.paintNot(painter);
    } else {
      GraphicsUtil.switchToWidth(g, 2);
      final var d = isInverter ? 10 : 0;
      final var xp = new int[] {-d, -19 - d, -19 - d, -d};
      final var yp = new int[] {0, -7, 7, 0};
      g.drawPolyline(xp, yp, 4);
    }

    if (rotate != 0.0) {
      ((Graphics2D) g).rotate(-rotate);
    }
    g.translate(-x, -y);
  }

  @Override
  public void propagate(InstanceState state) {
    final var control = state.getPortValue(2);
    final var width = state.getAttributeValue(StdAttr.WIDTH);
    if (control == Value.TRUE) {
      final var in = state.getPortValue(1);
      /* in cannot passed directly, as the simulator would pass an u at the input to an u at the output.
       * Doing double inversion is the correct way to resolve this problem.
       */
      state.setPort(0, isInverter ? in.not() : in.not().not(), GateAttributes.DELAY);
    } else if (control == Value.ERROR || control == Value.UNKNOWN) {
      state.setPort(0, Value.createError(width), GateAttributes.DELAY);
    } else {
      Value out;
      if (control == Value.UNKNOWN || control == Value.NIL) {
        final var opts = state.getProject().getOptions().getAttributeSet();
        if (opts.getValue(Options.ATTR_GATE_UNDEFINED).equals(Options.GATE_UNDEFINED_ERROR)) {
          out = Value.createError(width);
        } else {
          out = Value.createUnknown(width);
        }
      } else {
        out = Value.createUnknown(width);
      }
      state.setPort(0, out, GateAttributes.DELAY);
    }
  }

}
