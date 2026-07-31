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

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.MatrixKeypadIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class MatrixKeypad extends InstanceFactory implements DynamicElementProvider {

  public static final String _ID = "MatrixKeypad";

  public static final int ROWS = 4;
  public static final int COLS = 4;

  public static final int ROW_BASE = 0;
  public static final int COL_BASE = 4;

  public static final Attribute<String> ATTR_KEY_A = Attributes.forString("keyALabel", S.getter("matrixKeypadKeyAAttr"));
  public static final Attribute<String> ATTR_KEY_B = Attributes.forString("keyBLabel", S.getter("matrixKeypadKeyBAttr"));
  public static final Attribute<String> ATTR_KEY_C = Attributes.forString("keyCLabel", S.getter("matrixKeypadKeyCAttr"));
  public static final Attribute<String> ATTR_KEY_D = Attributes.forString("keyDLabel", S.getter("matrixKeypadKeyDAttr"));
  public static final Attribute<String> ATTR_KEY_STAR = Attributes.forString("keyStarLabel", S.getter("matrixKeypadKeyStarAttr"));
  public static final Attribute<String> ATTR_KEY_HASH = Attributes.forString("keyHashLabel", S.getter("matrixKeypadKeyHashAttr"));

  @SuppressWarnings("unchecked")
  public static final Attribute<String>[] KEY_ATTRIBUTES_RAW = (Attribute<String>[]) new Attribute<?>[] {
    null, null, null, ATTR_KEY_A,
    null, null, null, ATTR_KEY_B,
    null, null, null, ATTR_KEY_C,
    ATTR_KEY_STAR, null, ATTR_KEY_HASH, ATTR_KEY_D
  };

  private static final String[] KEY_LABELS_RAW = {
    "1", "2", "3", "A",
    "4", "5", "6", "B",
    "7", "8", "9", "C",
    "*", "0", "#", "D"
  };

  private static final String[] KEY_LABELS_PMOD_KYPD = {
    "1", "2", "3", "A",
    "4", "5", "6", "B",
    "7", "8", "9", "C",
    "0", "F", "E", "D"
  };

  private static final boolean[] KEY_IS_RED_RAW = {
    false, false, false, true,
    false, false, false, true,
    false, false, false, true,
    true, false, true, true
  };

  private static final Color COLOR_KEY_PMOD = new Color(30, 80, 140);
  private static final Color COLOR_KEY_PMOD_PRESSED = new Color(15, 45, 85);

  public static final AttributeOption MODE_RAW =
      new AttributeOption("raw", S.getter("matrixKeypadModeRaw"));
  public static final AttributeOption MODE_PMOD_KYPD =
      new AttributeOption("pmod_kypd", S.getter("matrixKeypadModePmodKYPD"));
  public static final Attribute<AttributeOption> ATTR_MODE =
      Attributes.forOption(
          "mode",
          S.getter("matrixKeypadModeAttr"),
          new AttributeOption[] {MODE_RAW, MODE_PMOD_KYPD});

  public static final AttributeOption SIZE_NORMAL =
      new AttributeOption("normal", S.getter("matrixKeypadSizeNormal"));
  public static final AttributeOption SIZE_LARGE =
      new AttributeOption("large", S.getter("matrixKeypadSizeLarge"));
  public static final Attribute<AttributeOption> ATTR_SIZE =
      Attributes.forOption(
          "size",
          S.getter("matrixKeypadSizeAttr"),
          new AttributeOption[] {SIZE_NORMAL, SIZE_LARGE});

  private static final Color COLOR_BODY = new Color(50, 50, 50);
  private static final Color COLOR_KEY_BLUE = new Color(0, 170, 220);
  private static final Color COLOR_KEY_RED = new Color(220, 40, 40);
  private static final Color COLOR_KEY_BLUE_PRESSED = new Color(0, 120, 170);
  private static final Color COLOR_KEY_RED_PRESSED = new Color(170, 30, 30);

  public static class Logger extends InstanceLogger {
    @Override
    public String getLogName(InstanceState state, Object option) {
      return state.getAttributeValue(StdAttr.LABEL);
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return BitWidth.ONE;
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      final var data = (InstanceDataSingleton) state.getData();
      final var pressedMatrix = data != null ? (boolean[][]) data.getValue() : null;
      if (pressedMatrix == null) return Value.FALSE;
      for (int row = 0; row < ROWS; row++) {
        for (int col = 0; col < COLS; col++) {
          if (pressedMatrix[row][col]) return Value.TRUE;
        }
      }
      return Value.FALSE;
    }
  }

  public static class Poker extends InstancePoker {
    private int lastPressedRow = -1;
    private int lastPressedCol = -1;

    @Override
    public void mousePressed(InstanceState state, MouseEvent event) {
      var data = (InstanceDataSingleton) state.getData();
      if (data == null) {
        data = new InstanceDataSingleton(new boolean[ROWS][COLS]);
        state.setData(data);
      }
      final var pressedMatrix = (boolean[][]) data.getValue();

      final var bounds = state.getInstance().getBounds();
      final var eventX = event.getX() - bounds.getX();
      final var eventY = event.getY() - bounds.getY();
      final var scale = state.getAttributeSet().getValue(ATTR_SIZE) == SIZE_LARGE ? 2.0f : 1.0f;

      final var keySize = (int) (12 * scale);
      final var gap = (int) (2 * scale);
      final var step = keySize + gap;
      final var startX = (int) (3 * scale);
      final var startY = (int) (3 * scale);

      lastPressedRow = -1;
      lastPressedCol = -1;

      for (int row = 0; row < ROWS; row++) {
        for (int col = 0; col < COLS; col++) {
          final var keyX = startX + col * step;
          final var keyY = startY + row * step;
          if (eventX >= keyX && eventX < keyX + keySize && eventY >= keyY && eventY < keyY + keySize) {
            pressedMatrix[row][col] = true;
            lastPressedRow = row;
            lastPressedCol = col;
            state.getInstance().fireInvalidated();
            return;
          }
        }
      }
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent event) {
      if (lastPressedRow < 0 || lastPressedCol < 0) {
        return;
      }
      var data = (InstanceDataSingleton) state.getData();
      if (data == null) {
        return;
      }
      final var pressedMatrix = (boolean[][]) data.getValue();
      pressedMatrix[lastPressedRow][lastPressedCol] = false;
      lastPressedRow = -1;
      lastPressedCol = -1;
      state.getInstance().fireInvalidated();
    }
  }

  private static class MatrixKeypadAttributes extends AbstractAttributeSet {
    private String label = "";
    private Direction labelLoc = Direction.NORTH;
    private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
    private Color labelColor = StdAttr.DEFAULT_LABEL_COLOR;
    private boolean labelVisibility = true;
    private AttributeOption mode = MODE_RAW;
    private AttributeOption size = SIZE_NORMAL;
    private String keyA = "A";
    private String keyB = "B";
    private String keyC = "C";
    private String keyD = "D";
    private String keyStar = "*";
    private String keyHash = "#";

    private static List<String> getInputLabels() {
      final var result = new ArrayList<String>();
      result.add("COL1");
      result.add("COL2");
      result.add("COL3");
      result.add("COL4");
      return result;
    }

    private static List<String> getOutputLabels() {
      final var result = new ArrayList<String>();
      result.add("ROW1");
      result.add("ROW2");
      result.add("ROW3");
      result.add("ROW4");
      return result;
    }

    private static final List<Attribute<?>> RAW_ATTRIBUTES =
        List.of(
            StdAttr.LABEL,
            StdAttr.LABEL_LOC,
            StdAttr.LABEL_FONT,
            StdAttr.LABEL_COLOR,
            StdAttr.LABEL_VISIBILITY,
            StdAttr.MAPINFO,
            ATTR_MODE,
            ATTR_SIZE,
            ATTR_KEY_A,
            ATTR_KEY_B,
            ATTR_KEY_C,
            ATTR_KEY_D,
            ATTR_KEY_STAR,
            ATTR_KEY_HASH);

    private static final List<Attribute<?>> PMOD_ATTRIBUTES =
        List.of(
            StdAttr.LABEL,
            StdAttr.LABEL_LOC,
            StdAttr.LABEL_FONT,
            StdAttr.LABEL_COLOR,
            StdAttr.LABEL_VISIBILITY,
            StdAttr.MAPINFO,
            ATTR_MODE,
            ATTR_SIZE);

    @Override
    protected void copyInto(AbstractAttributeSet destObj) {
      if (destObj instanceof MatrixKeypadAttributes dest) {
        dest.label = label;
        dest.labelLoc = labelLoc;
        dest.labelFont = labelFont;
        dest.labelColor = labelColor;
        dest.labelVisibility = labelVisibility;
        dest.mode = mode;
        dest.size = size;
        dest.keyA = keyA;
        dest.keyB = keyB;
        dest.keyC = keyC;
        dest.keyD = keyD;
        dest.keyStar = keyStar;
        dest.keyHash = keyHash;
      }
    }

    @Override
    public List<Attribute<?>> getAttributes() {
      return (mode == MODE_PMOD_KYPD) ? PMOD_ATTRIBUTES : RAW_ATTRIBUTES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attr) {
      if (attr == StdAttr.LABEL) return (V) label;
      if (attr == StdAttr.LABEL_LOC) return (V) labelLoc;
      if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
      if (attr == StdAttr.LABEL_COLOR) return (V) labelColor;
      if (attr == StdAttr.LABEL_VISIBILITY) return (V) Boolean.valueOf(labelVisibility);
      if (attr == StdAttr.MAPINFO) {
        return (V) new ComponentMapInformationContainer(4, 4, 0, getInputLabels(), getOutputLabels(), null);
      }
      if (attr == ATTR_MODE) return (V) mode;
      if (attr == ATTR_SIZE) return (V) size;
      if (attr == ATTR_KEY_A) return (V) keyA;
      if (attr == ATTR_KEY_B) return (V) keyB;
      if (attr == ATTR_KEY_C) return (V) keyC;
      if (attr == ATTR_KEY_D) return (V) keyD;
      if (attr == ATTR_KEY_STAR) return (V) keyStar;
      if (attr == ATTR_KEY_HASH) return (V) keyHash;
      return null;
    }

    @Override
    public <V> void setValue(Attribute<V> attr, V value) {
      final var oldMode = mode;
      if (attr == StdAttr.LABEL) label = (String) value;
      else if (attr == StdAttr.LABEL_LOC) labelLoc = (Direction) value;
      else if (attr == StdAttr.LABEL_FONT) labelFont = (Font) value;
      else if (attr == StdAttr.LABEL_COLOR) labelColor = (Color) value;
      else if (attr == StdAttr.LABEL_VISIBILITY) labelVisibility = (Boolean) value;
      else if (attr == ATTR_MODE) mode = (AttributeOption) value;
      else if (attr == ATTR_SIZE) size = (AttributeOption) value;
      else if (attr == ATTR_KEY_A) keyA = (String) value;
      else if (attr == ATTR_KEY_B) keyB = (String) value;
      else if (attr == ATTR_KEY_C) keyC = (String) value;
      else if (attr == ATTR_KEY_D) keyD = (String) value;
      else if (attr == ATTR_KEY_STAR) keyStar = (String) value;
      else if (attr == ATTR_KEY_HASH) keyHash = (String) value;

      fireAttributeValueChanged(attr, value, null);
      if (attr == ATTR_MODE && oldMode != mode) {
        fireAttributeListChanged();
      }
    }
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new MatrixKeypadAttributes();
  }

  public MatrixKeypad() {
    super(_ID, S.getter("matrixKeypadComponent"), new MatrixKeypadHdlGeneratorFactory(), true);
    setAttributes(
        new Attribute[] {
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO,
          ATTR_MODE,
          ATTR_SIZE,
          ATTR_KEY_A,
          ATTR_KEY_B,
          ATTR_KEY_C,
          ATTR_KEY_D,
          ATTR_KEY_STAR,
          ATTR_KEY_HASH
        },
        new Object[] {
          "",
          Direction.NORTH,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          new ComponentMapInformationContainer(4, 4, 0, MatrixKeypadAttributes.getInputLabels(), MatrixKeypadAttributes.getOutputLabels(), null),
          MODE_RAW,
          SIZE_NORMAL,
          "A",
          "B",
          "C",
          "D",
          "*",
          "#"
        });
    setIcon(new MatrixKeypadIcon());
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setInstancePoker(Poker.class);
    setInstanceLogger(Logger.class);

    final var ports = new Port[8];
    for (int row = 0; row < ROWS; row++) {
      ports[ROW_BASE + row] = new Port(-20 + row * 10, 30, Port.INPUT, BitWidth.ONE);
      ports[ROW_BASE + row].setToolTip(S.getter("matrixKeypadRowTip", "" + (row + 1)));
    }
    for (int col = 0; col < COLS; col++) {
      ports[COL_BASE + col] = new Port(30, -20 + col * 10, Port.OUTPUT, BitWidth.ONE);
      ports[COL_BASE + col].setToolTip(S.getter("matrixKeypadColTip", "" + (col + 1)));
    }
    setPorts(ports);
  }

  private boolean[][] getPressedState(InstanceState state) {
    var data = (InstanceDataSingleton) state.getData();
    if (data == null) {
      final var pressedMatrix = new boolean[ROWS][COLS];
      data = new InstanceDataSingleton(pressedMatrix);
      state.setData(data);
      return pressedMatrix;
    }
    return (boolean[][]) data.getValue();
  }

  private boolean[][] getPressedState(InstancePainter painter) {
    final var data = (InstanceDataSingleton) painter.getData();
    return data != null ? (boolean[][]) data.getValue() : null;
  }

  private void updatePorts(Instance instance) {
    final var isLarge = instance.getAttributeValue(ATTR_SIZE) == SIZE_LARGE;
    final var topLeft = isLarge ? -60 : -30;
    final var edgePos = isLarge ? 60 : 30;
    final var ports = new Port[8];
    for (int row = 0; row < ROWS; row++) {
      ports[ROW_BASE + row] = new Port(topLeft + (row + 1) * 10, edgePos, Port.INPUT, BitWidth.ONE);
      ports[ROW_BASE + row].setToolTip(S.getter("matrixKeypadRowTip", "" + (row + 1)));
    }
    for (int col = 0; col < COLS; col++) {
      ports[COL_BASE + col] = new Port(edgePos, topLeft + (col + 1) * 10, Port.OUTPUT, BitWidth.ONE);
      ports[COL_BASE + col].setToolTip(S.getter("matrixKeypadColTip", "" + (col + 1)));
    }
    instance.setPorts(ports);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.computeLabelTextField(Instance.AVOID_CENTER | Instance.AVOID_LEFT);
    updatePorts(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var size = attrs.getValue(ATTR_SIZE);
    if (size == SIZE_LARGE) {
      return Bounds.create(-60, -60, 120, 120);
    }
    return Bounds.create(-30, -30, 60, 60);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_CENTER | Instance.AVOID_LEFT);
    } else if (attr == ATTR_SIZE) {
      updatePorts(instance);
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_CENTER | Instance.AVOID_LEFT);
      instance.fireInvalidated();
    } else {
      for (final var keyAttr : KEY_ATTRIBUTES_RAW) {
        if (keyAttr != null && attr == keyAttr) {
          instance.fireInvalidated();
          return;
        }
      }
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var bounds = painter.getBounds();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 8, 8);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var bounds = painter.getBounds();
    final var pressedMatrix = getPressedState(painter);
    final var showState = painter.getShowState();
    final var scale = painter.getAttributeSet().getValue(ATTR_SIZE) == SIZE_LARGE ? 2.0f : 1.0f;

    final var keySize = (int) (12 * scale);
    final var gap = (int) (2 * scale);
    final var step = keySize + gap;
    final var startX = bounds.getX() + (int) (3 * scale);
    final var startY = bounds.getY() + (int) (3 * scale);

    g.setFont(new Font("SansSerif", Font.BOLD, (int) (9 * scale)));
    final var fontMetrics = g.getFontMetrics();

    g.setColor(COLOR_BODY);
    g.fillRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 8, 8);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 8, 8);

    final var isPmodKypd = painter.getAttributeSet().getValue(ATTR_MODE) == MODE_PMOD_KYPD;
    final var keyLabels = isPmodKypd ? KEY_LABELS_PMOD_KYPD : KEY_LABELS_RAW;
    final var keyAttrs = isPmodKypd ? null : KEY_ATTRIBUTES_RAW;

    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        final var keyIndex = row * COLS + col;
        final var keyX = startX + col * step;
        final var keyY = startY + row * step;
        final var isPressed = showState && pressedMatrix != null && pressedMatrix[row][col];
        final Color baseColor;
        final Color pressedColor;
        if (isPmodKypd) {
          baseColor = COLOR_KEY_PMOD;
          pressedColor = COLOR_KEY_PMOD_PRESSED;
        } else {
          final var isRedKey = KEY_IS_RED_RAW[keyIndex];
          baseColor = isRedKey ? COLOR_KEY_RED : COLOR_KEY_BLUE;
          pressedColor = isRedKey ? COLOR_KEY_RED_PRESSED : COLOR_KEY_BLUE_PRESSED;
        }
        final var keyColor = isPressed ? pressedColor : baseColor;

        g.setColor(keyColor);
        g.fillRoundRect(keyX, keyY, keySize, keySize, (int) (2 * scale), (int) (2 * scale));

        final var highlightColor = new Color(
            Math.min(255, keyColor.getRed() + 40),
            Math.min(255, keyColor.getGreen() + 40),
            Math.min(255, keyColor.getBlue() + 40));
        final var shadowColor = new Color(
            Math.max(0, keyColor.getRed() - 40),
            Math.max(0, keyColor.getGreen() - 40),
            Math.max(0, keyColor.getBlue() - 40));

        if (isPressed) {
          g.setColor(shadowColor);
          g.drawLine(keyX, keyY, keyX + keySize - 1, keyY);
          g.drawLine(keyX, keyY, keyX, keyY + keySize - 1);
          g.setColor(highlightColor);
          g.drawLine(keyX + 1, keyY + keySize - 1, keyX + keySize - 1, keyY + keySize - 1);
          g.drawLine(keyX + keySize - 1, keyY + keySize - 1, keyX + keySize - 1, keyY);
        } else {
          g.setColor(highlightColor);
          g.drawLine(keyX, keyY, keyX + keySize - 1, keyY);
          g.drawLine(keyX, keyY, keyX, keyY + keySize - 1);
          g.setColor(shadowColor);
          g.drawLine(keyX + 1, keyY + keySize - 1, keyX + keySize - 1, keyY + keySize - 1);
          g.drawLine(keyX + keySize - 1, keyY + keySize - 1, keyX + keySize - 1, keyY);
        }

        final var keyAttr = (keyAttrs != null) ? keyAttrs[keyIndex] : null;
        final var attrVal = (keyAttr != null) ? painter.getAttributeValue(keyAttr) : null;
        var label = (attrVal != null && !attrVal.isEmpty()) ? attrVal : keyLabels[keyIndex];
        if (label.length() > 1) {
          label = label.substring(0, 1);
        }
        final var labelWidth = fontMetrics.stringWidth(label);
        final var labelAscent = fontMetrics.getAscent();
        g.setColor(Color.WHITE);
        final var labelYPos = "*".equals(label)
            ? keyY + (keySize + labelAscent) / 2 + (int) (2 * scale)
            : keyY + (keySize + labelAscent) / 2 - (int) (1 * scale);
        g.drawString(label, keyX + (keySize - labelWidth) / 2, labelYPos);
      }
    }

    painter.drawLabel();
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    final var pressedMatrix = getPressedState(state);
    final var isPmodKypd = state.getAttributeValue(ATTR_MODE) == MODE_PMOD_KYPD;

    for (int col = 0; col < COLS; col++) {
      Value columnValue = isPmodKypd ? Value.TRUE : Value.UNKNOWN;
      for (int row = 0; row < ROWS; row++) {
        if (pressedMatrix[row][col]) {
          final var rowValue = state.getPortValue(ROW_BASE + row);
          if (!isPmodKypd) {
            // RAW Mode
            if (columnValue == Value.UNKNOWN) {
              columnValue = rowValue;
            } else if (rowValue != Value.UNKNOWN && columnValue != rowValue) {
              columnValue = Value.ERROR;
            }
          } else {
            // PMOD_KYPD Mode
            if (rowValue == Value.FALSE) {
              if (columnValue == Value.TRUE) {
                columnValue = Value.FALSE;
              } else if (columnValue != Value.FALSE) {
                columnValue = Value.ERROR;
              }
            } else if (rowValue == Value.ERROR) {
              columnValue = Value.ERROR;
            } else if (rowValue == Value.TRUE && columnValue == Value.FALSE) {
              columnValue = Value.ERROR;
            }
          }
        }
      }
      state.setPort(COL_BASE + col, columnValue, 1);
    }
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new MatrixKeypadShape(x, y, path);
  }
}
