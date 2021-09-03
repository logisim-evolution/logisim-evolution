/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.io.IoLibrary;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public class Slider extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Slider";

  public static class Poker extends InstancePoker {
    private SliderValue data;
    private boolean dragging = false;
    private BitWidth b;

    @Override
    public void mouseDragged(InstanceState state, MouseEvent e) {
      if (dragging) {
        byte sliderPosition = (byte) (e.getX() - state.getInstance().getBounds().getX() - 10);
        if (sliderPosition < 0) sliderPosition = 0;
        else if (sliderPosition > SliderWidth) sliderPosition = SliderWidth;
        if (data.right_to_left) sliderPosition = (byte) (SliderWidth - sliderPosition);
        long value =
            Math.round(sliderPosition * (Math.pow(2, b.getWidth()) - 1) / SliderWidth);
        data.setCurrentValue(value);
        data.setCurrentX(sliderPosition);
        state.getAttributeSet().setValue(ATTR_VALUE, value);
        state.fireInvalidated();
      }
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      this.data = getValueState(state);
      this.b = state.getAttributeValue(StdAttr.WIDTH);
      Bounds bds = state.getInstance().getBounds();
      Rectangle slider =
          new Rectangle(
              bds.getX() + data.getCurrentX() + 5, bds.getY() + bds.getHeight() - 16, 12, 12);
      // check if clicking slider rectangle
      if (slider.contains(e.getX(), e.getY())) this.dragging = true;
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      this.dragging = false;
    }
  }

  public static class SliderValue implements InstanceData, Cloneable {
    private long currentvalue = 0;
    private byte bitwidth = 8, currentx = 0;
    private boolean right_to_left = false;

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public long getCurrentValue() {
      return this.currentvalue;
    }

    public byte getCurrentX() {
      return this.currentx;
    }

    public void setCurrentBitWidth(int b) {
      if (b != this.bitwidth) {
        this.bitwidth = (byte) b;
        setCurrentX();
      }
    }

    public void setCurrentValue(long x) {
      if (x != this.currentvalue) {
        this.currentvalue = x;
        setCurrentX();
      }
    }

    private void setCurrentX() {
      byte currentx =
          (byte)
              ((this.currentvalue & 0xffffffffL) * SliderWidth / (Math.pow(2, this.bitwidth) - 1));
      if (!this.right_to_left) this.currentx = currentx;
      else this.currentx = (byte) (SliderWidth - currentx);
    }

    public void setCurrentX(byte x) {
      if (!this.right_to_left) this.currentx = x;
      else this.currentx = (byte) (SliderWidth - x);
    }

    public void updateDir(boolean b) {
      if (b != right_to_left) {
        right_to_left = b;
        setCurrentX();
      }
    }
  }

  private static final AttributeOption RIGHT_TO_LEFT =
      new AttributeOption("right_to_left", S.getter("right_to_leftOption"));
  private static final AttributeOption LEFT_TO_RIGHT =
      new AttributeOption("left_to_right", S.getter("left_to_rightOption"));
  private static final Attribute<AttributeOption> ATTR_DIR =
      Attributes.forOption(
          "Direction",
          new LocaleManager("resources/logisim", "circuit").getter("wireDirectionAttr"),
          new AttributeOption[] {RIGHT_TO_LEFT, LEFT_TO_RIGHT});

  private static final Attribute<Long> ATTR_VALUE =
      Attributes.forHexLong("value", S.getter("constantValueAttr"));

  private static final byte SliderWidth = 100;

  private static SliderValue getValueState(InstanceState state) {
    SliderValue ret = (SliderValue) state.getData();
    if (ret == null) {
      ret = new SliderValue();
      state.setData(ret);
    } else {
      byte width = (byte) state.getAttributeValue(StdAttr.WIDTH).getWidth();
      long value = state.getAttributeValue(ATTR_VALUE);
      long maxvalue = (long) (Math.pow(2, width) - 1);
      // if old value is bigger than the max value for the selected bitwidth, set
      // value to its max value
      if (value > maxvalue) {
        value = maxvalue;
        state.getAttributeSet().setValue(ATTR_VALUE, value);
      }
      ret.updateDir(state.getAttributeValue(ATTR_DIR) == RIGHT_TO_LEFT);
      ret.setCurrentValue(value);
      ret.setCurrentBitWidth(width);
    }
    return ret;
  }

  public Slider() {
    super(_ID, S.getter("Slider"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          StdAttr.WIDTH,
          RadixOption.ATTRIBUTE,
          IoLibrary.ATTR_COLOR,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          ATTR_DIR,
          ATTR_VALUE
        },
        new Object[] {
          Direction.EAST,
          BitWidth.create(8),
          RadixOption.RADIX_2,
          Color.WHITE,
          "",
          StdAttr.DEFAULT_LABEL_FONT,
          true,
          LEFT_TO_RIGHT,
            0L
        });
    setFacingAttribute(StdAttr.FACING);
    setIconName("slider.gif");
    setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, 1)});
    setInstancePoker(Poker.class);
  }

  private void computeTextField(Instance instance) {
    Object d = instance.getAttributeValue(StdAttr.FACING);
    Bounds bds = instance.getBounds();
    int x = bds.getX() - 3;
    int y = bds.getY() + bds.getHeight() / 2 - 1;
    int halign = GraphicsUtil.H_RIGHT;
    int valign = GraphicsUtil.V_CENTER_OVERALL;
    if (d == Direction.WEST) {
      y = bds.getY();
      valign = GraphicsUtil.V_BASELINE;
    }
    instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign, valign);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    computeTextField(instance);
    updateports(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    byte width = SliderWidth + 20, height = 30;
    if (facing == Direction.EAST) return Bounds.create(-width, -height / 2, width, height);
    else if (facing == Direction.WEST) return Bounds.create(0, -height / 2, width, height);
    else if (facing == Direction.NORTH) return Bounds.create(-width / 2, 0, width, height);
    else return Bounds.create(-width / 2, -height, width, height); // Direction SUD
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updateports(instance);
      computeTextField(instance);
    } else if (attr == StdAttr.WIDTH) {
      updateports(instance);
      instance.fireInvalidated();
    } else if (attr == ATTR_VALUE) instance.fireInvalidated();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds();
    SliderValue data = getValueState(painter);
    int x = bds.getX(), y = bds.getY();
    painter.drawRoundBounds(painter.getAttributeValue(IoLibrary.ATTR_COLOR));
    GraphicsUtil.switchToWidth(g, 2);
    // slider line
    g.drawLine(x + 10, y + bds.getHeight() - 10, x + bds.getWidth() - 10, y + bds.getHeight() - 10);
    g.setColor(Color.DARK_GRAY);
    // slider
    g.fillRoundRect(x + data.getCurrentX() + 5, y + bds.getHeight() - 15, 10, 10, 4, 4);
    g.setColor(Color.BLACK);
    g.drawRoundRect(x + data.getCurrentX() + 5, y + bds.getHeight() - 15, 10, 10, 4, 4);
    painter.drawPorts();
    painter.drawLabel();
    // paint current value
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
    Value v = painter.getPortValue(0);
    FontMetrics fm = g.getFontMetrics();
    RadixOption radix = painter.getAttributeValue(RadixOption.ATTRIBUTE);
    String vStr = radix.toString(v);
    // if the string is too long, reduce its dimension
    if (fm.stringWidth(vStr) > bds.getWidth() - 10)
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 5));
    GraphicsUtil.drawCenteredText(g, vStr, x + bds.getWidth() / 2, y + 6);
  }

  @Override
  public void propagate(InstanceState state) {
    BitWidth b = state.getAttributeValue(StdAttr.WIDTH);
    long value = state.getAttributeValue(ATTR_VALUE);
    state.setPort(0, Value.createKnown(b, value), 1);
  }

  private void updateports(Instance instance) {
    BitWidth b = instance.getAttributeValue(StdAttr.WIDTH);
    instance.setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, b)});
  }
}
