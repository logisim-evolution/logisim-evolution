/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

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
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import javax.swing.Icon;

public class Keypad extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT
   * change as it will
   * prevent project files from loading.
   *
   * <p>
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Keypad";

  private static final int PORT_BIT_WIDTH = 4;
  private static final int WIDTH = 60;
  private static final int HEIGHT = 80;
  private static final int BUTTON_WIDTH = 20;
  private static final int BUTTON_HEIGHT = 20;
  private static final Color BORDER_COLOR = Color.BLACK;
  private static final Color BUTTON_COLOR = Color.WHITE;
  private static final Color PRESSED_COLOR = new Color(170, 170, 170); // A light gray
  private static final BitWidth FOUR_BITS = BitWidth.create(PORT_BIT_WIDTH);

  public Keypad() {
    super(_ID, S.getter("keypadComponent"), new AbstractSimpleIoHdlGeneratorFactory(true), true);
    setAttributes(
        new Attribute[] {
            StdAttr.FACING,
            StdAttr.LABEL,
            StdAttr.LABEL_LOC,
            StdAttr.LABEL_FONT,
            StdAttr.LABEL_COLOR,
            StdAttr.LABEL_VISIBILITY
        },
        new Object[] {
            Direction.EAST,
            "",
            Direction.WEST,
            StdAttr.DEFAULT_LABEL_FONT,
            StdAttr.DEFAULT_LABEL_COLOR,
            true
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new KeypadIcon());
    final var ports = new Port[1];
    ports[0] = new Port(0, 0, Port.OUTPUT, PORT_BIT_WIDTH);
    ports[0].setToolTip(S.getter("Output"));

    setPorts(ports);
    setInstancePoker(KeypadPoker.class);
    setInstanceLogger(KeypadLogger.class);
  }

  // Maps the button index (0-9) to its 4-bit value
  private static final Value[] BUTTON_VALUES = {
      Value.createKnown(FOUR_BITS, 0), // 0
      Value.createKnown(FOUR_BITS, 1), // 1
      Value.createKnown(FOUR_BITS, 2), // 2
      Value.createKnown(FOUR_BITS, 3), // 3
      Value.createKnown(FOUR_BITS, 4), // 4
      Value.createKnown(FOUR_BITS, 5), // 5
      Value.createKnown(FOUR_BITS, 6), // 6
      Value.createKnown(FOUR_BITS, 7), // 7
      Value.createKnown(FOUR_BITS, 8), // 8
      Value.createKnown(FOUR_BITS, 9) // 9
  };

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(-WIDTH, -HEIGHT / 2, WIDTH, HEIGHT);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.computeLabelTextField(Instance.AVOID_CENTER);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      updatePorts(instance);
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_CENTER);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_CENTER);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    KeypadData data = (KeypadData) state.getData();
    Value val = (data == null) ? Value.createUnknown(FOUR_BITS) : data.getValue();
    state.setPort(0, val, 1);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Bounds bds = painter.getBounds();
    int x = bds.getX();
    int y = bds.getY();
    Graphics g = painter.getGraphics();

    // Draw the main body
    g.setColor(new Color(224, 224, 224));
    g.fillRect(x, y, bds.getWidth(), bds.getHeight());
    g.setColor(BORDER_COLOR);
    g.drawRect(x, y, bds.getWidth(), bds.getHeight());

    // Draw the buttons
    KeypadData data = (KeypadData) painter.getData();
    int pressedButton = (data == null) ? -1 : data.getPressedButton();

    Font originalFont = g.getFont();
    g.setFont(originalFont.deriveFont(Font.BOLD, 12f));

    // Buttons 1-9
    for (int i = 1; i < 10; i++) {
      int row = (i - 1) / 3;
      int col = (i - 1) % 3;
      drawButton(g, x + col * BUTTON_WIDTH, y + row * BUTTON_HEIGHT,
          String.valueOf(i), i == pressedButton);
    }
    // Button 0
    drawButton(g, x + BUTTON_WIDTH, y + 3 * BUTTON_HEIGHT, "0", 0 == pressedButton);

    g.setFont(originalFont); // Restore original font

    painter.drawLabel();
    painter.drawPorts();
  }

  private void drawButton(Graphics g, int x, int y, String text, boolean pressed) {
    // Draw button body
    g.setColor(pressed ? PRESSED_COLOR : BUTTON_COLOR);
    g.fillRect(x + 1, y + 1, BUTTON_WIDTH - 2, BUTTON_HEIGHT - 2);

    // Draw button border
    g.setColor(BORDER_COLOR);
    g.drawRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);

    // Draw button text (number)
    g.setColor(Color.BLACK);
    GraphicsUtil.drawCenteredText(g, text, x + BUTTON_WIDTH / 2, y + BUTTON_HEIGHT / 2);
  }

  private void updatePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    int dx = 0;
    int dy = 0;
    if (facing == Direction.WEST) {
      dx = -3 * BUTTON_WIDTH;
    } else if (facing == Direction.EAST) {
      dy = 0;
      dx = 0;
    } else if (facing == Direction.SOUTH) {
      dx = -3 * BUTTON_WIDTH / 2;
      dy = 2 * BUTTON_HEIGHT;
    } else { // North
      dx = -3 * BUTTON_WIDTH / 2;
      dy = -2 * BUTTON_HEIGHT;
    }
    final var ports = new Port[1];
    ports[0] = new Port(dx, dy, Port.OUTPUT, PORT_BIT_WIDTH);
    ports[0].setToolTip(S.getter("Output"));
    instance.setPorts(ports);
  }

  private static class KeypadData implements InstanceData, Cloneable {
    private int pressedButton; // -1 if none, 0-9 otherwise

    public KeypadData() {
      this.pressedButton = -1;
    }

    @Override
    public KeypadData clone() {
      try {
        return (KeypadData) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public void setPressedButton(int button) {
      this.pressedButton = button;
    }

    public int getPressedButton() {
      return pressedButton;
    }

    public Value getValue() {
      if (pressedButton >= 0 && pressedButton <= 9) {
        return BUTTON_VALUES[pressedButton];
      }
      return Value.createUnknown(FOUR_BITS);
    }
  }

  public static class KeypadPoker extends InstancePoker {
    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      updatePressedButton(state, e);
    }

    @Override
    public void mouseDragged(InstanceState state, MouseEvent e) {
      updatePressedButton(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      setValue(state, -1); // No button pressed
    }

    private void updatePressedButton(InstanceState state, MouseEvent e) {
      Bounds bds = state.getInstance().getBounds();
      int x = e.getX() - bds.getX();
      int y = e.getY() - bds.getY();
      int button = getButtonAt(x, y);
      setValue(state, button);
    }

    private int getButtonAt(int x, int y) {
      if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
        return -1;
      }

      int col = x / BUTTON_WIDTH;
      int row = y / BUTTON_HEIGHT;

      if (col >= 3 || row >= 4) {
        return -1;
      }

      if (row < 3) {
        return row * 3 + col + 1;
      } else { // row == 3
        if (col == 1) {
          return 0; // The '0' button
        }
      }
      return -1; // Click was in the grid but outside any button
    }

    private void setValue(InstanceState state, int button) {
      KeypadData data = (KeypadData) state.getData();
      if (data == null) {
        data = new KeypadData();
        state.setData(data);
      }
      if (data.getPressedButton() != button) {
        data.setPressedButton(button);
        state.getInstance().fireInvalidated();
      }
    }
  }

  public static class KeypadLogger extends InstanceLogger {
    @Override
    public String getLogName(InstanceState state, Object option) {
      return state.getAttributeValue(StdAttr.LABEL);
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return FOUR_BITS;
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      KeypadData data = (KeypadData) state.getData();
      return (data == null) ? Value.createUnknown(FOUR_BITS) : data.getValue();
    }

    @Override
    public boolean isInput(InstanceState state, Object option) {
      return false;
    }
  }

  private static class KeypadIcon implements Icon {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(Color.DARK_GRAY);
      // Draw a 3x3 grid to represent the keypad
      for (int row = 0; row < 3; row++) {
        for (int col = 0; col < 3; col++) {
          // g.fillRect(x + col * 5, y + row * 5, 4, 4);
          g.fillRect(x + col * AppPreferences.getScaled(5),
              y + row * AppPreferences.getScaled(5),
              AppPreferences.getScaled(4),
              AppPreferences.getScaled(4));
        }
      }
    }

    @Override
    public int getIconWidth() {
      return AppPreferences.getScaled(16);
    }

    @Override
    public int getIconHeight() {
      return AppPreferences.getScaled(16);
    }
  }
}