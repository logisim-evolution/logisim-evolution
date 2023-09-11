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
      if (attr == ATTR_VALUE) return (V) Long.valueOf(value.toLongValue());
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
        long val = (Long) value;
        this.value = Value.createKnown(width, val);
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
    public long getConstant(AttributeSet attrs) {
      return attrs.getValue(Constant.ATTR_VALUE);
    }
  }

  public static final Attribute<Long> ATTR_VALUE =
      Attributes.forHexLong("value", S.getter("constantValueAttr"));

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
    int chars = (width.getWidth() + 3) / 4;
    int w = 7 + 7 * chars;
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
    long v = painter.getAttributeValue(ATTR_VALUE);
    String vStr = Long.toHexString(v);
    Bounds bds = getOffsetBounds(painter.getAttributeSet());

    Graphics g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    g.fillOval(-2, -2, 4, 4);
    g.setFont(DEFAULT_FONT);
    GraphicsUtil.drawCenteredText(
        g, vStr, bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2 - 2);
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
      long v = painter.getAttributeValue(ATTR_VALUE);
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
    Bounds bds = painter.getOffsetBounds();
    BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
    long longValue = painter.getAttributeValue(ATTR_VALUE);
    Value v = Value.createKnown(width, longValue);
    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();

    Graphics g = painter.getGraphics();
    if (painter.shouldDrawColor()) {
      g.setColor(BACKGROUND_COLOR);
      g.fillRect(x + bds.getX(), y + bds.getY(), bds.getWidth(), bds.getHeight());
    }
    if (v.getWidth() == 1) {
      if (painter.shouldDrawColor()) g.setColor(v.getColor());
      g.setFont(DEFAULT_FONT);
      GraphicsUtil.drawCenteredText(
          g,
          v.toString(),
          x + bds.getX() + bds.getWidth() / 2,
          y + bds.getY() + bds.getHeight() / 2 - 2);
    } else {
      g.setColor(Color.BLACK);
      g.setFont(DEFAULT_FONT);
      GraphicsUtil.drawCenteredText(
          g,
          v.toHexString(),
          x + bds.getX() + bds.getWidth() / 2,
          y + bds.getY() + bds.getHeight() / 2 - 2);
    }
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
    long value = state.getAttributeValue(ATTR_VALUE);
    state.setPort(0, Value.createKnown(width, value), 1);
  }

  private void updatePorts(Instance instance) {
    Port[] ps = {new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH)};
    instance.setPorts(ps);
  }

  // TODO: Allow editing of value via text tool/attribute table
}
