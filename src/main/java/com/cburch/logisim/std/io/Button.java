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

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.ButtonIcon;
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
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class Button extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Button";

  public static final AttributeOption BUTTON_PRESS_ACTIVE =
      new AttributeOption("active", S.getter("buttonPressActive"));
  public static final AttributeOption BUTTON_PRESS_PASSIVE =
      new AttributeOption("passive", S.getter("buttonPressPassive"));
  public static final Attribute<AttributeOption> ATTR_PRESS =
      Attributes.forOption(
          "press",
          S.getter("buttonPressAttr"),
          new AttributeOption[] {BUTTON_PRESS_ACTIVE, BUTTON_PRESS_PASSIVE});
  public static final AttributeOption KEY_NONE =
      new AttributeOption("none", S.getter("buttonKeyNone"));
  private static final String KEY_DIGITS = "0123456789";
  private static final String KEY_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  /** Cyrillic letters in QWERTY order, so the Latin letter printed on the key still matches. */
  private static final String CYRILLIC_LAYOUT = "йцукенгшщзфывапролдячсмить";
  private static final String LATIN_FOR_CYRILLIC_LAYOUT = "qwertyuiopasdfghjklzxcvbnm";
  private static final int UNICODE_EXTENDED_KEY_BASE = 0x01000000;
  private static final AttributeOption[] KEY_OPTIONS = createKeyOptions();
  public static final Attribute<AttributeOption> ATTR_KEY =
      Attributes.forOption("key", S.getter("buttonKeyAttr"), KEY_OPTIONS);
  private static final float TRIGGER_GLYPH_SIZE = 10f;

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
      return outputLevel(state);
    }

    @Override
    public boolean isInput(InstanceState state, Object option) {
      return true;
    }
  }

  public static class Poker extends InstancePoker {
    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      setMousePressed(state, true);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      setMousePressed(state, false);
    }
  }

  private static final int DEPTH = 3;

  public Button() {
    super(_ID, S.getter("buttonComponent"), new AbstractSimpleIoHdlGeneratorFactory(true), true);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          IoLibrary.ATTR_COLOR,
          ATTR_PRESS,
          ATTR_KEY,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO
        },
        new Object[] {
          Direction.EAST,
          Color.WHITE,
          BUTTON_PRESS_ACTIVE,
          KEY_NONE,
          "",
          Direction.WEST,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          new ComponentMapInformationContainer(1, 0, 0)
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new ButtonIcon());
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, 1)});
    setInstancePoker(Poker.class);
    setInstanceLogger(Logger.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.computeLabelTextField(Instance.AVOID_CENTER | Instance.AVOID_LEFT);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    return Bounds.create(-20, -10, 20, 20).rotate(Direction.EAST, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_CENTER | Instance.AVOID_LEFT);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_CENTER | Instance.AVOID_LEFT);
    } else if (attr == ATTR_PRESS) {
      final var instanceImplementation = instance.getComponent().getInstanceStateImpl();
      if (instanceImplementation == null) return;
      final var circuitState = instanceImplementation.getCircuitState();
      if (circuitState == null) return;
      final var state = circuitState.getInstanceState(instance.getComponent());
      if (state == null) return;
      state.getInstance().fireInvalidated();
    }
  }

  /** Mouse and keyboard holds are independent: the button stays down while either remains. */
  public static boolean setMousePressed(InstanceState state, boolean pressed) {
    return setSource(state, true, pressed);
  }

  public static boolean setKeyPressed(InstanceState state, boolean pressed) {
    return setSource(state, false, pressed);
  }

  private static boolean setSource(InstanceState state, boolean mouseSource, boolean pressed) {
    final var existing = state.getData();
    final Hold hold;
    if (existing instanceof Hold current) {
      hold = current;
    } else if (!pressed) {
      return false;
    } else {
      hold = new Hold();
      state.setData(hold);
    }
    if (mouseSource) {
      if (hold.mouse == pressed) return false;
      hold.mouse = pressed;
    } else {
      if (hold.key == pressed) return false;
      hold.key = pressed;
    }
    state.getInstance().fireInvalidated();
    return true;
  }

  static Value outputLevel(InstanceState state) {
    final var held = state.getData() instanceof Hold hold && (hold.mouse || hold.key);
    return level(state, held);
  }

  static Value level(InstanceState state, boolean pressed) {
    final var activeOnPress = state.getAttributeValue(ATTR_PRESS) != BUTTON_PRESS_PASSIVE;
    return pressed == activeOnPress ? Value.TRUE : Value.FALSE;
  }

  public static AttributeOption keyOption(String token) {
    if (token == null) return null;
    for (final var option : KEY_OPTIONS) {
      if (token.equals(option.toString())) return option;
    }
    return null;
  }

  static String tokenFor(KeyEvent event) {
    if (event == null) return null;
    return tokenFor(event.getKeyCode(), event.getKeyChar(), event.getExtendedKeyCode());
  }

  static String tokenFor(int keyCode, char keyChar, int extendedKeyCode) {
    final var fromCode = tokenForKeyCode(keyCode);
    if (fromCode != null) return fromCode;
    final var fromExtended = tokenForKeyCode(extendedKeyCode);
    if (fromExtended != null) return fromExtended;
    final var fromChar = tokenForCharacter(keyChar);
    if (fromChar != null) return fromChar;
    return tokenForCharacter(characterFromExtendedKeyCode(extendedKeyCode));
  }

  static String tokenForKeyCode(int keyCode) {
    if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
      return String.valueOf((char) keyCode);
    }
    if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
      return String.valueOf((char) keyCode);
    }
    if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9) {
      return String.valueOf((char) ('0' + (keyCode - KeyEvent.VK_NUMPAD0)));
    }
    if (keyCode >= '\u0400' && keyCode <= '\u04FF') {
      return tokenForCharacter((char) keyCode);
    }
    return null;
  }

  static String tokenForCharacter(char keyChar) {
    if (keyChar == KeyEvent.CHAR_UNDEFINED) return null;
    if (keyChar >= 'a' && keyChar <= 'z') {
      return String.valueOf((char) (keyChar - 'a' + 'A'));
    }
    if ((keyChar >= 'A' && keyChar <= 'Z') || (keyChar >= '0' && keyChar <= '9')) {
      return String.valueOf(keyChar);
    }
    final var index = CYRILLIC_LAYOUT.indexOf(Character.toLowerCase(keyChar));
    if (index < 0) return null;
    return String.valueOf(Character.toUpperCase(LATIN_FOR_CYRILLIC_LAYOUT.charAt(index)));
  }

  private static char characterFromExtendedKeyCode(int extendedKeyCode) {
    if (extendedKeyCode < UNICODE_EXTENDED_KEY_BASE) return KeyEvent.CHAR_UNDEFINED;
    final var unicode = extendedKeyCode - UNICODE_EXTENDED_KEY_BASE;
    if (unicode > Character.MAX_VALUE) return KeyEvent.CHAR_UNDEFINED;
    return (char) unicode;
  }

  public static boolean isKeyUsedByOther(Circuit circuit, Component self, AttributeOption key) {
    if (circuit == null || key == null || KEY_NONE.toString().equals(key.toString())) return false;
    final var token = key.toString();
    for (final var comp : circuit.getNonWires()) {
      if (comp == self || !(comp.getFactory() instanceof Button)) continue;
      final var other = comp.getAttributeSet().getValue(ATTR_KEY);
      if (other != null && token.equals(other.toString())) return true;
    }
    return false;
  }

  public static void clearDuplicateKey(Circuit circuit, Component added) {
    if (circuit == null || added == null || !(added.getFactory() instanceof Button)) return;
    final var key = added.getAttributeSet().getValue(ATTR_KEY);
    if (key != null && isKeyUsedByOther(circuit, added, key)) {
      added.getAttributeSet().setValue(ATTR_KEY, KEY_NONE);
    }
  }

  private static AttributeOption[] createKeyOptions() {
    final var options = new AttributeOption[1 + KEY_DIGITS.length() + KEY_LETTERS.length()];
    var index = 0;
    options[index++] = KEY_NONE;
    for (var i = 0; i < KEY_DIGITS.length(); i++) {
      options[index++] = fixedOption(KEY_DIGITS.substring(i, i + 1));
    }
    for (var i = 0; i < KEY_LETTERS.length(); i++) {
      options[index++] = fixedOption(KEY_LETTERS.substring(i, i + 1));
    }
    return options;
  }

  private static AttributeOption fixedOption(String token) {
    return new AttributeOption(token, new FixedLabel(token));
  }

  private static void drawTriggerGlyph(
      Graphics g, AttributeOption trigger, Color ink, int x, int y, int width, int height) {
    if (trigger == null || KEY_NONE.toString().equals(trigger.toString())) return;
    final var previousFont = g.getFont();
    g.setColor(ink);
    g.setFont(previousFont.deriveFont(Font.BOLD, TRIGGER_GLYPH_SIZE));
    GraphicsUtil.drawCenteredText(
        g, trigger.toString(), x + (width - DEPTH) / 2, y + (height - DEPTH) / 2);
    g.setFont(previousFont);
  }

  private static final class FixedLabel implements StringGetter {
    private final String value;

    private FixedLabel(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var defaultButtonState =
        painter.getAttributeValue(ATTR_PRESS) == BUTTON_PRESS_ACTIVE ? Value.FALSE : Value.TRUE;
    final var bds = painter.getBounds();
    var x = bds.getX();
    var y = bds.getY();
    final var w = bds.getWidth();
    final var h = bds.getHeight();
    final var baseColor = new Color(AppPreferences.COMPONENT_COLOR.get());

    final var val = painter.getShowState() ? outputLevel(painter) : defaultButtonState;

    var color = painter.getAttributeValue(IoLibrary.ATTR_COLOR);
    if (!painter.shouldDrawColor()) {
      int hue = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
      color = new Color(hue, hue, hue);
    }

    final var g = painter.getGraphics();
    int depress;
    if (val != defaultButtonState) {
      x += DEPTH;
      y += DEPTH;
      Object labelLoc = painter.getAttributeValue(StdAttr.LABEL_LOC);
      if (labelLoc == StdAttr.LABEL_CENTER
          || labelLoc == Direction.NORTH
          || labelLoc == Direction.WEST) {
        depress = DEPTH;
      } else {
        depress = 0;
      }

      Object facing = painter.getAttributeValue(StdAttr.FACING);
      if (facing == Direction.NORTH || facing == Direction.WEST) {
        final var p = painter.getLocation();
        int px = p.getX();
        int py = p.getY();
        GraphicsUtil.switchToWidth(g, Wire.WIDTH);
        g.setColor(Value.trueColor);
        if (facing == Direction.NORTH) g.drawLine(px, py, px, py + 10);
        else g.drawLine(px, py, px + 10, py);
        GraphicsUtil.switchToWidth(g, 1);
      }

      g.setColor(color);
      g.fillRect(x, y, w - DEPTH, h - DEPTH);
      g.setColor(baseColor);
      g.drawRect(x, y, w - DEPTH, h - DEPTH);
    } else {
      depress = 0;
      int[] xp = new int[] {x, x + w - DEPTH, x + w, x + w, x + DEPTH, x};
      int[] yp = new int[] {y, y, y + DEPTH, y + h, y + h, y + h - DEPTH};
      g.setColor(color.darker());
      g.fillPolygon(xp, yp, xp.length);
      g.setColor(color);
      g.fillRect(x, y, w - DEPTH, h - DEPTH);
      g.setColor(baseColor);
      g.drawPolygon(xp, yp, xp.length);

      // The following two draws are supposed to be polygons, not polylines.
      // This is not a mistake, but rather a graphical trick which leverages
      // the implicit miter limit to produce lines that look like they have
      // "butt" style line caps, even though they are drawn with a system
      // that produces only "square" style line caps.
      final int farX = x + w - DEPTH;
      final int farY = y + h - DEPTH;
      xp = new int[] {farX, farX, x, farX};
      yp = new int[] {y, farY, farY, farY};
      g.drawPolygon(xp, yp, xp.length);
      xp = new int[] {farX, x + w};
      yp = new int[] {farY, y + h};
      g.drawPolygon(xp, yp, xp.length);
    }

    drawTriggerGlyph(g, painter.getAttributeValue(ATTR_KEY), baseColor, x, y, w, h);
    g.translate(depress, depress);
    painter.drawLabel();
    g.translate(-depress, -depress);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    state.setPort(0, outputLevel(state), 1);
  }

  static final class Hold implements InstanceData, Cloneable {
    private boolean mouse;
    private boolean key;

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException exception) {
        return null;
      }
    }
  }
}
