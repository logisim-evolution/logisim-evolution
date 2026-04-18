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

import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Constant extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Constant";

  private static class ConstantAttributes extends AbstractAttributeSet {
    private Direction facing = Direction.EAST;
    private BitWidth width = BitWidth.ONE;
    private Value value = Value.TRUE;

    @Override
    protected void copyInto(AbstractAttributeSet destObj) {
      ConstantAttributes dest = (ConstantAttributes) destObj;
      dest.facing = this.facing;
      dest.width = this.width;
      dest.value = this.value;
    }

    @Override
    public List<Attribute<?>> getAttributes() {
      return ATTRIBUTES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attr) {
      if (attr == StdAttr.FACING) return (V) facing;
      if (attr == StdAttr.WIDTH) return (V) width;
      if (attr == ATTR_VALUE) return (V) value.toBigInteger(true);
      return null;
    }

    @Override
    public <V> void setValue(Attribute<V> attr, V value) {
      if (attr == StdAttr.FACING) {
        facing = (Direction) value;
      } else if (attr == StdAttr.WIDTH) {
        width = (BitWidth) value;
        this.value =
            this.value.extendWidth(width.getWidth(), this.value.get(this.value.getWidth() - 1));
      } else if (attr == ATTR_VALUE) {
        this.value = Value.createKnown(width, (BigInteger) value);
      } else {
        throw new IllegalArgumentException("unknown attribute " + attr);
      }
      fireAttributeValueChanged(attr, value, null);
    }

    @Override
    public <V> List<Attribute<?>> attributesMayAlsoBeChanged(Attribute<V> attr, V value) {
      if (attr != StdAttr.WIDTH || Objects.equals(getValue(attr), value)) {
        return null;
      }
      return List.of(ATTR_VALUE);
    }
  }

  private static class ConstantExpression implements ExpressionComputer {
    private final Instance instance;

    public ConstantExpression(Instance instance) {
      this.instance = instance;
    }

    @Override
    public void computeExpression(ExpressionComputer.Map expressionMap) {
      AttributeSet attrs = instance.getAttributeSet();
      int width = attrs.getValue(StdAttr.WIDTH).getWidth();
      Value v = Value.createKnown(BitWidth.create(width), attrs.getValue(ATTR_VALUE));
      for (int b = 0; b < width; b++) {
        expressionMap.put(
            instance.getLocation(), b, Expressions.constant((int) v.get(b).toLongValue()));
      }
    }
  }

  private static class ConstantHdlGeneratorFactory extends AbstractConstantHdlGeneratorFactory {
    @Override
    public BigInteger getConstant(AttributeSet attrs) {
      return attrs.getValue(Constant.ATTR_VALUE);
    }
  }

  public static final Attribute<BigInteger> ATTR_VALUE =
      Attributes.forHexBigInteger("value", S.getter("constantValueAttr"));

  public static final InstanceFactory FACTORY = new Constant();

  private static final Color BACKGROUND_COLOR = new Color(230, 230, 230);
  private static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);

  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(StdAttr.FACING, StdAttr.WIDTH, ATTR_VALUE);

  public Constant() {
    super(_ID, S.getter("constantComponent"), new ConstantHdlGeneratorFactory());
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(
        JoinedConfigurator.create(
            new ConstantConfigurator(), new BitWidthConfigurator(StdAttr.WIDTH)));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new ConstantAttributes();
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == ExpressionComputer.class) return new ConstantExpression(instance);
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    BitWidth width = attrs.getValue(StdAttr.WIDTH);
    int w = Probe.getOffsetBounds(facing, width, RadixOption.RADIX_16, false, true)
        .getWidth();
    if (facing == Direction.EAST) return Bounds.create(-w, -8, w, 16);
    else if (facing == Direction.WEST) return Bounds.create(0, -8, w, 16);
    else if (facing == Direction.SOUTH) return Bounds.create(-w / 2, -16, w, 16);
    else if (facing == Direction.NORTH) return Bounds.create(-w / 2, 0, w, 16);
    else throw new IllegalArgumentException("unrecognized arguments " + facing + " " + width);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.WIDTH) {
      instance.recomputeBounds();
      updatePorts(instance);
    } else if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
    } else if (attr == ATTR_VALUE) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    Bounds bds = getOffsetBounds(painter.getAttributeSet());
    BigInteger v = painter.getAttributeValue(ATTR_VALUE);
    var width = painter.getAttributeValue(StdAttr.WIDTH);

    Graphics g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    g.fillOval(-2, -2, 4, 4);
    g.setFont(DEFAULT_FONT);

    paintValue(
        painter,
        g,
        bds,
        Value.createKnown(width, v),
        Location.create(0, 0, false));
  }

  //
  // painting methods
  //
  @Override
  public void paintIcon(InstancePainter painter) {
    int w = painter.getAttributeValue(StdAttr.WIDTH).getWidth();
    int pinx = 16;
    int piny = 9;
    Direction dir = painter.getAttributeValue(StdAttr.FACING);
    if (dir == Direction.EAST) {
    } else if (dir == Direction.WEST) {
      pinx = 4;
    } else if (dir == Direction.NORTH) {
      pinx = 9;
      piny = 4;
    } else if (dir == Direction.SOUTH) {
      pinx = 9;
      piny = 16;
    }

    Graphics g = painter.getGraphics();
    if (w == 1) {
      var v = painter.getAttributeValue(ATTR_VALUE).longValue();
      Value val = v == 1L ? Value.TRUE : Value.FALSE;
      g.setColor(val.getColor());
      GraphicsUtil.drawCenteredText(g, "" + v, 10, 9);
    } else {
      g.setFont(g.getFont().deriveFont(9.0f));
      GraphicsUtil.drawCenteredText(g, "x" + w, 10, 9);
    }
    g.fillOval(pinx, piny, 3, 3);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Bounds bds = getOffsetBounds(painter.getAttributeSet());
    BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
    BigInteger longValue = painter.getAttributeValue(ATTR_VALUE);
    Value v = Value.createKnown(width, longValue);
    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();

    Graphics g = painter.getGraphics();
    if (painter.shouldDrawColor()) {
      g.setColor(BACKGROUND_COLOR);
      g.fillRect(x + bds.getX(), y + bds.getY() - 2, bds.getWidth(), bds.getHeight() + 4);
    }
    paintValue(painter, g, bds, v, loc);
    painter.drawPorts();
  }

  public static void paintValue(InstancePainter p, Graphics g, Bounds bds, Value v, Location loc) {
    int y = loc.getY() + bds.getY() + bds.getHeight() / 2 - 3;
    if (v.getWidth() == 1) {
      int x = loc.getX() + bds.getX() + bds.getWidth() / 2;
      if (p.shouldDrawColor()) g.setColor(v.getColor());
      g.setFont(DEFAULT_FONT);
      GraphicsUtil.drawCenteredText(g, v.toString(), x, y);
    } else {
      g.setColor(Color.BLACK);
      g.setFont(DEFAULT_FONT);
      String text = RadixOption.RADIX_16.toString(v);
      int cx = loc.getX() + bds.getX()
          + (v.getWidth() < 5 ? (bds.getWidth() / 2) : (bds.getWidth() - 6));
      for (int k = text.length() - 1; k >= 0; k--) {
        GraphicsUtil.drawCenteredText(g, text.substring(k, k + 1), cx, y);
        cx -= Pin.DIGIT_WIDTH;
      }
    }
  }

  @Override
  public void propagate(InstanceState state) {
    BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
    BigInteger value = state.getAttributeValue(ATTR_VALUE);
    state.setPort(0, Value.createKnown(width, value), 1);
  }

  private void updatePorts(Instance instance) {
    Port[] ps = {new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH)};
    instance.setPorts(ps);
  }

  // TODO: Allow editing of value via text tool/attribute table
}
