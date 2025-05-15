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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.io.IoLibrary;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public class Slider extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Slider";

  private static final int MAXIMUM_NUMBER_OF_BITS = 8;
  private static final int MAXIMUM_SLIDER_POSITION = (1 << MAXIMUM_NUMBER_OF_BITS) - 1;

  public static class Poker extends InstancePoker {
    private boolean dragging = false;

    @Override
    public void mouseDragged(InstanceState state, MouseEvent e) {
      if (dragging) {
        var data = (SliderValue) state.getData();
        if (data == null) {
          data = new SliderValue();
          data.setDirection(state.getAttributeValue(ATTR_DIR) == RIGHT_TO_LEFT);
          data.setCurrentBitWidth(state.getAttributeValue(WIDTH).getWidth());
          state.setData(data);
        }
        data.setSliderPosition(e.getX() - state.getInstance().getBounds().getX() - 10);
        state.fireInvalidated();
      }
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      final var data = (SliderValue) state.getData();
      final var sliderPosition =
          (data != null)
              ? data.getSliderPosition()
              : state.getAttributeValue(ATTR_DIR) == RIGHT_TO_LEFT ? MAXIMUM_SLIDER_POSITION : 0;
      final var bounds = state.getInstance().getBounds();
      final var slider =
          new Rectangle(
              bounds.getX() + sliderPosition + 5, bounds.getY() + bounds.getHeight() - 16, 12, 12);
      // check if clicking slider rectangle
      dragging = slider.contains(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      dragging = false;
    }
  }

  public static class SliderValue implements InstanceData, Cloneable {
    private int nrOfBits = MAXIMUM_NUMBER_OF_BITS;
    private int sliderPosition = 0;
    private boolean rightToLeft = false;

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public int getCurrentValue() {
      final var completeValue =
          rightToLeft ? (MAXIMUM_SLIDER_POSITION - sliderPosition) : sliderPosition;
      return completeValue >> (MAXIMUM_NUMBER_OF_BITS - nrOfBits);
    }

    public int getSliderPosition() {
      return sliderPosition;
    }

    public void setSliderPosition(int value) {
      sliderPosition = Math.max(0, Math.min(value, MAXIMUM_SLIDER_POSITION));
    }

    public void setCurrentBitWidth(int width) {
      if ((width < 0) || (width > MAXIMUM_NUMBER_OF_BITS) || (width == nrOfBits)) return;
      nrOfBits = width;
    }

    public void setDirection(boolean value) {
      if (value != rightToLeft) {
        rightToLeft = value;
        sliderPosition = MAXIMUM_SLIDER_POSITION - sliderPosition;
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
  private static final Attribute<BitWidth> WIDTH =
      Attributes.forBitWidth("width", S.getter("stdDataWidthAttr"), 1, MAXIMUM_NUMBER_OF_BITS);

  public Slider() {
    super(_ID, S.getter("Slider"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          WIDTH,
          RadixOption.ATTRIBUTE,
          IoLibrary.ATTR_COLOR,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          ATTR_DIR
        },
        new Object[] {
          Direction.EAST,
          BitWidth.create(MAXIMUM_NUMBER_OF_BITS),
          RadixOption.RADIX_2,
          Color.WHITE,
          "",
          StdAttr.DEFAULT_LABEL_FONT,
          true,
          LEFT_TO_RIGHT
        });
    setFacingAttribute(StdAttr.FACING);
    setIconName("slider.gif");
    setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, 1)});
    setInstancePoker(Poker.class);
  }

  private void computeTextField(Instance instance) {
    final var isWestOrientated = instance.getAttributeValue(StdAttr.FACING) == Direction.WEST;
    final var bounds = instance.getBounds();
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        bounds.getX() - 3,
        (isWestOrientated) ? bounds.getY() : bounds.getY() + bounds.getHeight() / 2 - 1,
        GraphicsUtil.H_RIGHT,
        (isWestOrientated) ? GraphicsUtil.V_BASELINE : GraphicsUtil.V_CENTER_OVERALL);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    computeTextField(instance);
    updateports(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var facing = attrs.getValue(StdAttr.FACING);
    final var width = MAXIMUM_SLIDER_POSITION + 20;
    final var height = 30;
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
    } else if (attr == WIDTH) {
      updateports(instance);
      instance.fireInvalidated();
    } else if (attr == ATTR_DIR) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var gfx = (Graphics2D) painter.getGraphics();
    final var bounds = painter.getBounds();
    final var data = (SliderValue) painter.getData();
    final var posX = bounds.getX();
    final var posY = bounds.getY();
    final var sliderPosition =
        (data != null)
            ? data.getSliderPosition()
            : (painter.getAttributeValue(ATTR_DIR) == RIGHT_TO_LEFT) ? MAXIMUM_SLIDER_POSITION : 0;
    painter.drawRoundBounds(painter.getAttributeValue(IoLibrary.ATTR_COLOR));
    GraphicsUtil.switchToWidth(gfx, 2);
    // slider line
    gfx.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    gfx.drawLine(
        posX + 10,
        posY + bounds.getHeight() - 10,
        posX + bounds.getWidth() - 10,
        posY + bounds.getHeight() - 10);
    gfx.setColor(Color.DARK_GRAY);
    // slider
    gfx.fillRoundRect(posX + sliderPosition + 5, posY + bounds.getHeight() - 15, 10, 10, 4, 4);
    gfx.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    gfx.drawRoundRect(posX + sliderPosition + 5, posY + bounds.getHeight() - 15, 10, 10, 4, 4);
    painter.drawPorts();
    painter.drawLabel();
    // paint current value
    gfx.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
    GraphicsUtil.drawCenteredValue(
        gfx,
        painter.getPortValue(0),
        painter.getAttributeValue(RadixOption.ATTRIBUTE),
        posX + bounds.getWidth() / 2,
        posY + 6);
  }

  @Override
  public void propagate(InstanceState state) {
    final var data = (SliderValue) state.getData();
    final var bitWidth = state.getAttributeValue(WIDTH);
    var sliderValue = 0;
    if (data != null) {
      data.setDirection(state.getAttributeValue(ATTR_DIR) == RIGHT_TO_LEFT);
      data.setCurrentBitWidth(bitWidth.getWidth());
      sliderValue = data.getCurrentValue();
    }
    state.setPort(0, Value.createKnown(bitWidth, sliderValue), 1);
  }

  private void updateports(Instance instance) {
    instance.setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, instance.getAttributeValue(WIDTH))});
  }
}
