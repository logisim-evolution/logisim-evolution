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

import com.cburch.contracts.BaseDocumentListenerContract;
import com.cburch.contracts.BaseKeyListenerContract;
import com.cburch.contracts.BaseWindowFocusListenerContract;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Canvas;
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
import com.cburch.logisim.prefs.PrefMonitorBooleanConvert;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.font.TextLayout;
import java.math.BigInteger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Pin extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Pin";

  @SuppressWarnings("serial")
  private static class EditDecimal extends JDialog implements BaseKeyListenerContract, LocaleListener {

    private final JFormattedTextField text;
    private final int bitWidth;
    final PinState pinState;
    final InstanceState state;
    final RadixOption radix;
    final boolean tristate;
    private static final Color VALID_COLOR = new Color(0xff, 0xf0, 0x99);
    private static final Color INVALID_COLOR = new Color(0xff, 0x66, 0x66);
    final JButton ok;
    final JButton cancel;

    @Override
    public void localeChanged() {
      setTitle(S.get("PinEnterDecimal"));
      ok.setText(S.get("PinOkay"));
      cancel.setText(S.get("PinCancel"));
    }

    public EditDecimal(InstanceState state) {
      super();
      this.state = state;
      radix = state.getAttributeValue(RadixOption.ATTRIBUTE);
      pinState = getState(state);
      Value value = pinState.intendedValue;
      bitWidth = value.getWidth();
      PinAttributes attrs = (PinAttributes) state.getAttributeSet();
      tristate = attrs.behavior == TRISTATE;

      setTitle(S.get("PinEnterDecimal"));
      GridBagConstraints gbc = new GridBagConstraints();
      ok = new JButton(S.get("PinOkay"));
      cancel = new JButton(S.get("PinCancel"));
      ok.addActionListener(
          e -> accept());
      cancel.addActionListener(
          e -> EditDecimal.this.setVisible(false));
      addWindowFocusListener(
          new BaseWindowFocusListenerContract() {
            @Override
            public void windowLostFocus(WindowEvent e) {
              EditDecimal.this.setVisible(false);
            }
          });
      setLayout(new GridBagLayout());

      text = new JFormattedTextField();
      text.setFont(AppPreferences.getScaledFont(DEFAULT_FONT));
      text.setColumns(11);
      text.setText(value.toDecimalString(radix == RadixOption.RADIX_10_SIGNED));
      text.selectAll();

      text.getDocument()
          .addDocumentListener(
              new BaseDocumentListenerContract() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                  String s = text.getText();
                  if (isEditValid(s)) {
                    text.setBackground(VALID_COLOR);
                    ok.setEnabled(true);
                  } else {
                    text.setBackground(INVALID_COLOR);
                    ok.setEnabled(false);
                  }
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                  insertUpdate(e);
                }
              });

      gbc.gridx = 0;
      gbc.gridy = 1;
      add(cancel, gbc);
      gbc.gridx = 1;
      gbc.gridy = 1;
      add(ok, gbc);
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.anchor = GridBagConstraints.BASELINE;
      gbc.insets = new Insets(8, 4, 8, 4);
      text.addKeyListener(this);
      text.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      text.setBackground(VALID_COLOR);
      add(text, gbc);

      pack();
    }

    public void accept() {
      String s = text.getText();
      if (isEditValid(s)) {
        Value newVal;
        if (s.equals(Character.toString(Value.UNKNOWNCHAR).toLowerCase())
            || s.equals(Character.toString(Value.UNKNOWNCHAR).toUpperCase())
            || s.equals("???")) {
          newVal = Value.createUnknown(BitWidth.create(bitWidth));
        } else {
          try {
            BigInteger n = new BigInteger(s);
            BigInteger signedMax = new BigInteger("1").shiftLeft(bitWidth - 1);
            if (radix == RadixOption.RADIX_10_SIGNED || n.compareTo(signedMax) < 0) {
              newVal = Value.createKnown(BitWidth.create(bitWidth), n.longValue());
            } else {
              BigInteger max = new BigInteger("1").shiftLeft(bitWidth);
              BigInteger newValue = n.subtract(max);
              newVal = Value.createKnown(BitWidth.create(bitWidth), newValue.longValue());
            }
          } catch (NumberFormatException exception) {
            return;
          }
        }
        setVisible(false);
        pinState.intendedValue = newVal;
        state.fireInvalidated();
      }
    }

    boolean isEditValid(String s) {
      if (s == null) return false;
      s = s.trim();
      if (s.equals("")) return false;
      if (tristate
          && (s.equals(Character.toString(Value.UNKNOWNCHAR).toLowerCase())
              || s.equals(Character.toString(Value.UNKNOWNCHAR).toUpperCase())
              || s.equals("???"))) return true;
      try {
        BigInteger n = new BigInteger(s);
        if (radix == RadixOption.RADIX_10_SIGNED) {
          BigInteger min = new BigInteger("-1").shiftLeft(bitWidth - 1);
          BigInteger max = new BigInteger("1").shiftLeft(bitWidth - 1);
          return (n.compareTo(min) >= 0) && (n.compareTo(max) < 0);
        } else {
          BigInteger max = new BigInteger("1").shiftLeft(bitWidth);
          return (n.compareTo(BigInteger.ZERO) >= 0) && (n.compareTo(max) < 0);
        }
      } catch (NumberFormatException e) {
        return false;
      }
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        accept();
      } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        setVisible(false);
      }
    }
  }

  private static class EditFloat extends JDialog implements BaseKeyListenerContract, LocaleListener {
    private final JFormattedTextField text;
    private final int bitWidth;
    final PinState pinState;
    final InstanceState state;
    final boolean tristate;
    private static final Color VALID_COLOR = new Color(0xff, 0xf0, 0x99);
    private static final Color INVALID_COLOR = new Color(0xff, 0x66, 0x66);
    final JButton ok;
    final JButton cancel;

    @Override
    public void localeChanged() {
      setTitle(S.get("PinEnterFloat"));
      ok.setText(S.get("PinOkay"));
      cancel.setText(S.get("PinCancel"));
    }

    public EditFloat(InstanceState state) {
      super();
      this.state = state;
      pinState = getState(state);
      final var value = pinState.intendedValue;
      bitWidth = value.getWidth();
      final var attrs = (PinAttributes) state.getAttributeSet();
      tristate = attrs.behavior == TRISTATE;

      setTitle(S.get("PinEnterFloat"));
      final var gbc = new GridBagConstraints();
      ok = new JButton(S.get("PinOkay"));
      cancel = new JButton(S.get("PinCancel"));
      ok.addActionListener(
          e -> accept());
      cancel.addActionListener(
          e -> EditFloat.this.setVisible(false));
      addWindowFocusListener(
          new BaseWindowFocusListenerContract() {
            @Override
            public void windowLostFocus(WindowEvent e) {
              EditFloat.this.setVisible(false);
            }
          });
      setLayout(new GridBagLayout());

      text = new JFormattedTextField();
      text.setFont(AppPreferences.getScaledFont(DEFAULT_FONT));
      text.setColumns(11);
      text.setText(value.toStringFromFloatValue());
      text.selectAll();

      text.getDocument()
          .addDocumentListener(
              new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                  final var s = text.getText();
                  if (isEditValid(s)) {
                    text.setBackground(VALID_COLOR);
                    ok.setEnabled(true);
                  } else {
                    text.setBackground(INVALID_COLOR);
                    ok.setEnabled(false);
                  }
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                  insertUpdate(e);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                  // do nothing
                }
              });

      gbc.gridx = 0;
      gbc.gridy = 1;
      add(cancel, gbc);
      gbc.gridx = 1;
      gbc.gridy = 1;
      add(ok, gbc);
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.anchor = GridBagConstraints.BASELINE;
      gbc.insets = new Insets(8, 4, 8, 4);
      text.addKeyListener(this);
      text.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      text.setBackground(VALID_COLOR);
      add(text, gbc);

      pack();
    }

    public void accept() {
      final var s = text.getText();
      if (isEditValid(s)) {
        Value newVal;
        if (s.equals(Character.toString(Value.UNKNOWNCHAR).toLowerCase())
            || s.equals(Character.toString(Value.UNKNOWNCHAR).toUpperCase())
            || s.equals("???")) {
          newVal = Value.createUnknown(BitWidth.create(bitWidth));
        } else {
          double val;
          if (s.equalsIgnoreCase("inf") || s.equalsIgnoreCase("+inf")) val = Double.POSITIVE_INFINITY;
          else if (s.equalsIgnoreCase("-inf")) val = Double.NEGATIVE_INFINITY;
          else if (s.equalsIgnoreCase("nan")) val = Double.NaN;
          else val = Double.parseDouble(s);
          newVal = switch (bitWidth) {
            case 16 -> Value.createKnown(16, Float.floatToFloat16((float) val));
            case 32 -> Value.createKnown((float) val);
            case 64 -> Value.createKnown(val);
            default -> Value.ERROR;
          };
        }
        setVisible(false);
        pinState.intendedValue = newVal;
        state.fireInvalidated();
      }
    }

    boolean isEditValid(String s) {
      if (s == null) return false;
      s = s.trim();
      if (s.equals("")) return false;
      if (tristate
          && (s.equals(Character.toString(Value.UNKNOWNCHAR).toLowerCase())
              || s.equals(Character.toString(Value.UNKNOWNCHAR).toUpperCase())
              || s.equals("???"))) return true;
      if (s.equalsIgnoreCase("nan")
          || s.equalsIgnoreCase("inf")
          || s.equalsIgnoreCase("+inf")
          || s.equalsIgnoreCase("-inf")) return true;

      try {
        Double.parseDouble(s);
        return true;
      } catch (NumberFormatException e) {
        return false;
      }
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        accept();
      } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        setVisible(false);
      }
    }
  }

  public static class PinLogger extends InstanceLogger {

    @Override
    public String getLogName(InstanceState state, Object option) {
      PinAttributes attrs = (PinAttributes) state.getAttributeSet();
      String ret = attrs.label;
      if (ret == null || ret.equals("")) {
        String type = attrs.type == INPUT ? S.get("pinInputName") : S.get("pinOutputName");
        return type + state.getInstance().getLocation();
      } else {
        return ret;
      }
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return state.getAttributeValue(StdAttr.WIDTH);
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      PinState s = getState(state);
      return s.intendedValue;
    }

    @Override
    public boolean isInput(InstanceState state, Object option) {
      PinAttributes attrs = (PinAttributes) state.getAttributeSet();
      return attrs.type == INPUT;
    }
  }

  public static class PinPoker extends InstancePoker {

    int bitPressed = -1;
    int bitCaret = -1;

    private int getRow(InstanceState state, MouseEvent e) {
      int row = 0;
      Direction dir = state.getAttributeValue(StdAttr.FACING);
      Bounds bds = state.getInstance().getBounds();
      if (dir == Direction.EAST || dir == Direction.WEST)
        row = (bds.getY() + bds.getHeight() - e.getY()) / 20;
      else if (dir == Direction.NORTH) row = (bds.getX() + bds.getWidth() - e.getX()) / 20;
      else row = (e.getX() - bds.getX()) / 20;
      return row;
    }

    private int getColumn(InstanceState state, MouseEvent e, boolean isBinair) {
      int col = 0;
      int distance = isBinair ? 10 : DIGIT_WIDTH;
      Direction dir = state.getAttributeValue(StdAttr.FACING);
      Bounds bds = state.getInstance().getBounds();
      if (dir == Direction.EAST || dir == Direction.WEST) {
        int offset = dir == Direction.EAST ? 20 : 10;
        col = (bds.getX() + bds.getWidth() - e.getX() - offset) / distance;
      } else if (dir == Direction.NORTH) col = (e.getY() - bds.getY() - 20) / distance;
      else col = (bds.getY() + bds.getHeight() - e.getY() - 20) / distance;

      return col;
    }

    private int getBit(InstanceState state, MouseEvent e) {
      RadixOption radix = state.getAttributeValue(RadixOption.ATTRIBUTE);
      BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
      int r;
      if (radix == RadixOption.RADIX_16) {
        r = 4;
      } else if (radix == RadixOption.RADIX_8) {
        r = 3;
      } else if (radix == RadixOption.RADIX_2) {
        r = 1;
      } else {
        return -1;
      }
      if (width.getWidth() <= r) {
        return 0;
      } else {
        Bounds bds = state.getInstance().getBounds();
        int i, j;
        if (state.getAttributeValue(ProbeAttributes.PROBEAPPEARANCE)
            == ProbeAttributes.APPEAR_EVOLUTION_NEW) {
          i = getColumn(state, e, r == 1);
          j = getRow(state, e);
        } else {
          i = (bds.getX() + bds.getWidth() - e.getX() - (r == 1 ? 0 : 4)) / (r == 1 ? 10 : 8);
          j = (bds.getY() + bds.getHeight() - e.getY() - 2) / 14;
        }
        int bit = (r == 1) ? 8 * j + i : i * r;
        if (bit < 0 || bit >= width.getWidth()) {
          return -1;
        } else {
          return bit;
        }
      }
    }

    private boolean handleBitPress(
        InstanceState state, int bit, RadixOption radix, java.awt.Component src, char ch) {
      PinAttributes attrs = (PinAttributes) state.getAttributeSet();
      if (!attrs.isInput()) {
        return false;
      }
      if (src instanceof Canvas canvas && !state.isCircuitRoot()) {
        CircuitState circState = canvas.getCircuitState();
        java.awt.Component frame = SwingUtilities.getRoot(canvas);
        int choice =
            OptionPane.showConfirmDialog(
                frame,
                S.get("pinFrozenQuestion"),
                S.get("pinFrozenTitle"),
                OptionPane.OK_CANCEL_OPTION,
                OptionPane.WARNING_MESSAGE);
        if (choice == OptionPane.OK_OPTION) {
          circState = circState.cloneAsNewRootState();
          canvas.getProject().setCircuitState(circState);
          state = circState.getInstanceState(state.getInstance());
        } else {
          return false;
        }
      }
      BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
      PinState pinState = getState(state);
      int r = (radix == RadixOption.RADIX_16 ? 4 : (radix == RadixOption.RADIX_8 ? 3 : 1));
      if (bit + r > width.getWidth()) r = width.getWidth() - bit;
      Value[] val = pinState.intendedValue.getAll();
      boolean tristate = (attrs.behavior == TRISTATE);
      // if this was just converted from substate to root state, normalize val
      if (tristate) { // convert E bits to x
        for (int b = 0; b < val.length; b++)
          if (val[b] != Value.TRUE && val[b] != Value.FALSE && val[b] != Value.UNKNOWN)
            val[b] = Value.UNKNOWN;
      } else { // convert E and X bits to 1
        for (int b = 0; b < val.length; b++)
          if (val[b] != Value.TRUE && val[b] != Value.FALSE)
            val[b] = Value.TRUE;
      }
      if (ch == 0) { // toggle
        boolean ones = true, defined = true;
        for (int b = bit; b < bit + r; b++) {
          if (val[b] == Value.FALSE) ones = false;
          else if (val[b] != Value.TRUE) defined = false;
        }
        if (!defined || (ones && !tristate)) {
          for (int b = bit; b < bit + r; b++) val[b] = Value.FALSE;
        } else if (ones && tristate) {
          for (int b = bit; b < bit + r; b++) val[b] = Value.UNKNOWN;
        } else {
          int carry = 1;
          Value[] v = new Value[] {Value.FALSE, Value.TRUE};
          for (int b = bit; b < bit + r; b++) {
            int s = (val[b] == Value.TRUE ? 1 : 0) + carry;
            val[b] = v[(s % 2)];
            carry = s / 2;
          }
        }
      } else if (tristate
          && (ch == Character.toLowerCase(Value.UNKNOWNCHAR)
              || ch == Character.toUpperCase(Value.UNKNOWNCHAR))) {
        for (int b = bit; b < bit + r; b++) val[b] = Value.UNKNOWN;
      } else {
        int d;
        if ('0' <= ch && ch <= '9') d = ch - '0';
        else if ('a' <= ch && ch <= 'f') d = 0xa + (ch - 'a');
        else if ('A' <= ch && ch <= 'F') d = 0xA + (ch - 'A');
        else return false;
        if (d >= 1 << r) return false;
        for (int i = 0; i < r; i++)
          val[bit + i] = (((d & (1 << i)) != 0) ? Value.TRUE : Value.FALSE);
      }
      pinState.intendedValue = Value.create(val);
      state.fireInvalidated();
      return true;
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      bitPressed = getBit(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      if (!((PinAttributes) state.getAttributeSet()).isInput()) {
        bitPressed = -1;
        bitCaret = -1;
        return;
      }
      RadixOption radix = state.getAttributeValue(RadixOption.ATTRIBUTE);
      if (radix == RadixOption.RADIX_10_SIGNED || radix == RadixOption.RADIX_10_UNSIGNED) {
        EditDecimal dialog = new EditDecimal(state);
        dialog.setLocation(e.getXOnScreen() - 60, e.getYOnScreen() - 40);
        dialog.setVisible(true);
      } else if (radix == RadixOption.RADIX_FLOAT) {
        final var dialog = new EditFloat(state);
        dialog.setLocation(e.getXOnScreen() - 60, e.getYOnScreen() - 40);
        dialog.setVisible(true);
      } else {
        int bit = getBit(state, e);
        if (bit == bitPressed && bit >= 0) {
          bitCaret = bit;
          handleBitPress(state, bit, radix, e.getComponent(), (char) 0);
        }
        if (bitCaret < 0) {
          BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
          int r = (radix == RadixOption.RADIX_16 ? 4 : (radix == RadixOption.RADIX_8 ? 3 : 1));
          bitCaret = ((width.getWidth() - 1) / r) * r;
        }
      }
      bitPressed = -1;
    }

    @Override
    public void keyTyped(InstanceState state, KeyEvent e) {
      char ch = e.getKeyChar();
      RadixOption radix = state.getAttributeValue(RadixOption.ATTRIBUTE);
      if (radix == RadixOption.RADIX_10_SIGNED || radix == RadixOption.RADIX_10_UNSIGNED) return;
      int r = (radix == RadixOption.RADIX_16 ? 4 : (radix == RadixOption.RADIX_8 ? 3 : 1));
      BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
      if (bitCaret < 0) bitCaret = ((width.getWidth() - 1) / r) * r;
      if (handleBitPress(state, bitCaret, radix, e.getComponent(), ch)) {
        bitCaret -= r;
        if (bitCaret < 0) bitCaret = ((width.getWidth() - 1) / r) * r;
      }
    }

    @Override
    public void paint(InstancePainter painter) {
      if (bitCaret < 0) return;
      BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
      RadixOption radix = painter.getAttributeValue(RadixOption.ATTRIBUTE);
      if (radix == RadixOption.RADIX_10_SIGNED || radix == RadixOption.RADIX_10_UNSIGNED) return;
      int r = (radix == RadixOption.RADIX_16 ? 4 : (radix == RadixOption.RADIX_8 ? 3 : 1));
      if (width.getWidth() <= r) return;
      Bounds bds = painter.getBounds();
      Graphics g = painter.getGraphics();
      g.setColor(Color.RED);
      int y = bds.getY() + bds.getHeight();
      int x = bds.getX() + bds.getWidth();
      if (painter.getAttributeValue(ProbeAttributes.PROBEAPPEARANCE)
          == ProbeAttributes.APPEAR_EVOLUTION_NEW) {
        Direction dir = painter.getAttributeValue(StdAttr.FACING);
        int distance = radix == RadixOption.RADIX_2 ? 10 : DIGIT_WIDTH;
        int bwidth = 15;
        int bheight = distance - 1;
        if (dir == Direction.EAST || dir == Direction.WEST) {
          int offset = dir == Direction.EAST ? 20 : 10;
          x -= offset + distance * (radix == RadixOption.RADIX_2 ? bitCaret % 8 : bitCaret / r);
          y -= radix == RadixOption.RADIX_2 ? 20 * (bitCaret / 8) : 0;
          bwidth = distance - 1;
          bheight = 15;
          x -= bwidth;
          y -= 18;
        } else if (dir == Direction.NORTH) {
          y =
              bds.getY()
                  + 21
                  + distance * (radix == RadixOption.RADIX_2 ? bitCaret % 8 : bitCaret / r);
          x -= 18 + (radix == RadixOption.RADIX_2 ? 20 * (bitCaret / 8) : 0);
        } else {
          y -=
              19
                  + distance
                  + distance * (radix == RadixOption.RADIX_2 ? bitCaret % 8 : bitCaret / r);
          x = bds.getX() + 3 + (radix == RadixOption.RADIX_2 ? 20 * (bitCaret / 8) : 0);
        }
        g.drawRect(x, y, bwidth, bheight);
      } else {
        if (radix == RadixOption.RADIX_2) {
          x -= 2 + 10 * (bitCaret % 8);
          y -= 2 + 14 * (bitCaret / 8);
        } else {
          x -= 4 + DIGIT_WIDTH * (bitCaret / r);
          y -= 4;
        }
        GraphicsUtil.switchToWidth(g, 2);
        g.drawLine(x - 6, y, x, y);
      }
      g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    }
  }

  private static class PinState implements InstanceData, Cloneable {
    Value foundValue; // for color - received value from wire connected to this pin
    Value intendedValue; // for display - output: received value; input: UI or parent value

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  private static PinState getState(InstanceState state) {
    final var attrs = (PinAttributes) state.getAttributeSet();
    final var width = attrs.width;
    var ret = (PinState) state.getData();
    if (ret == null) {
      var initialValue = attrs.getValue(ATTR_INITIAL);
      final var newValue = attrs.behavior == TRISTATE
          ? Value.createUnknown(width)
          : Value.createKnown(width.getWidth(), initialValue);
      ret = new PinState();
      ret.foundValue = ret.intendedValue = newValue;
      state.setData(ret);
    }
    if (ret.intendedValue.getWidth() != width.getWidth()) {
      ret.intendedValue = ret.intendedValue.extendWidth(width.getWidth(), attrs.defaultBitValue());
    }
    if (ret.foundValue.getWidth() != width.getWidth()) {
      ret.foundValue = ret.foundValue.extendWidth(width.getWidth(), Value.UNKNOWN);
    }
    return ret;
  }

  public static final AttributeOption INPUT = new AttributeOption("input",
      S.getter("pinInputOption"));
  public static final AttributeOption OUTPUT = new AttributeOption("output",
      S.getter("pinOutputOption"));
  public static final Attribute<AttributeOption> ATTR_TYPE =
      Attributes.forOption("type", S.getter("pinTypeAttr"),
          new AttributeOption[] { INPUT, OUTPUT });

  public static final AttributeOption SIMPLE = new AttributeOption("simple",
      S.getter("pinSimpleOption"));
  public static final AttributeOption TRISTATE = new AttributeOption("tristate",
      S.getter("pinTristateOption"));
  public static final AttributeOption PULL_DOWN = new AttributeOption("pulldown",
      S.getter("pinPullDownOption"));
  public static final AttributeOption PULL_UP = new AttributeOption("pullup",
      S.getter("pinPullUpOption"));
  public static final Attribute<AttributeOption> ATTR_BEHAVIOR =
      Attributes.forOption("behavior", S.getter("pinBehaviorAttr"),
          new AttributeOption[] { SIMPLE, TRISTATE, PULL_DOWN, PULL_UP });
  public static final Attribute<Long> ATTR_INITIAL =
      Attributes.forHexLong("initial", S.getter("pinResetValue"));;

  public static final Pin FACTORY = new Pin();
  private static final Font ICON_WIDTH_FONT = new Font("SansSerif", Font.BOLD, 9);
  public static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);
  private static final Color ICON_WIDTH_COLOR = Value.widthErrorColor.darker();
  public static final int DIGIT_WIDTH = 8;

  public Pin() {
    super(_ID, S.getter("pinComponent"));
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(StdAttr.WIDTH),
            new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK)));
    setInstanceLogger(PinLogger.class);
    setInstancePoker(PinPoker.class);
  }

  private static Direction pinLabelLoc(Direction PinDir) {
    if (PinDir == Direction.EAST) return Direction.WEST;
    else if (PinDir == Direction.WEST) return Direction.EAST;
    else if (PinDir == Direction.NORTH) return Direction.SOUTH;
    else return Direction.NORTH;
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
    instance.addAttributeListener();
    ((PrefMonitorBooleanConvert) AppPreferences.NEW_INPUT_OUTPUT_SHAPES).addConvertListener(attrs);
    configurePorts(instance);
    instance.computeLabelTextField(
        Instance.AVOID_LEFT, pinLabelLoc(attrs.getValue(StdAttr.FACING)));
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    return attr.equals(ProbeAttributes.PROBEAPPEARANCE)
        ? ProbeAttributes.getDefaultProbeAppearance()
        : super.getDefaultAttributeValue(attr, ver);
  }

  private void configurePorts(Instance instance) {
    PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
    String endType = attrs.isOutput() ? Port.INPUT : Port.OUTPUT;
    Port port = new Port(0, 0, endType, StdAttr.WIDTH);
    if (attrs.isOutput()) {
      port.setToolTip(S.getter("pinOutputToolTip"));
    } else {
      port.setToolTip(S.getter("pinInputToolTip"));
    }
    instance.setPorts(new Port[] {port});
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new PinAttributes();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    BitWidth width = attrs.getValue(StdAttr.WIDTH);
    boolean NewLayout =
        attrs.getValue(ProbeAttributes.PROBEAPPEARANCE) == ProbeAttributes.APPEAR_EVOLUTION_NEW;
    return Probe.getOffsetBounds(
        facing, width, attrs.getValue(RadixOption.ATTRIBUTE), NewLayout, true);
  }

  public Value getValue(InstanceState state) {
    return getState(state).intendedValue;
  }

  public BitWidth getWidth(Instance instance) {
    PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
    return attrs.width;
  }

  @Override
  public boolean hasThreeStateDrivers(AttributeSet attrs) {
    // For purposes of HDL, Pin never generates floating values, regardless of attributes.
    return false;
  }

  @Override
  public boolean isHDLSupportedComponent(AttributeSet attrs) {
    return true;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_TYPE) {
      configurePorts(instance);
    } else if (attr == StdAttr.WIDTH
        || attr == StdAttr.FACING
        || attr == RadixOption.ATTRIBUTE
        || attr == ProbeAttributes.PROBEAPPEARANCE) {
      instance.recomputeBounds();
      PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
      instance.computeLabelTextField(Instance.AVOID_LEFT, pinLabelLoc(attrs.facing));
    } else if (attr == Pin.ATTR_BEHAVIOR) {
      instance.fireInvalidated();
    }
  }

  public boolean isInputPin(Instance instance) {
    PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
    return attrs.type == INPUT;
  }

  public boolean isClockPin(Instance instance) {
    PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
    return attrs.isClock();
  }

  private void drawNewStyleValue(
      InstancePainter painter, int width, int height, boolean isOutput, boolean isGhost) {
    /* Note: we are here in an translated environment the point (0,0) presents the pin location*/
    if (isGhost) return;
    Value value = getState(painter).intendedValue;
    Graphics g = painter.getGraphics();
    Graphics2D g2 = (Graphics2D) g;
    g.setFont(Pin.DEFAULT_FONT);
    RadixOption radix = painter.getAttributeValue(RadixOption.ATTRIBUTE);
    Direction dir = painter.getAttributeSet().getValue(StdAttr.FACING);
    int westTranslate = (isOutput) ? width : width + 10;
    final var baseColor = new Color(AppPreferences.COMPONENT_COLOR.get());
    if (dir == Direction.WEST) {
      g2.rotate(-Math.PI);
      g2.translate(westTranslate, 0);
    }
    if (!painter.getShowState()) {
      g.setColor(baseColor);
      GraphicsUtil.drawCenteredText(
          g,
          "x" + ((PinAttributes) painter.getAttributeSet()).width.getWidth(),
          -15 - (width - 15) / 2,
          0);
    } else {
      int labelYPos = height / 2 - 2;
      int LabelValueXOffset = (isOutput) ? -15 : -20;
      g.setColor(Color.BLUE);
      g2.scale(0.7, 0.7);
      g2.drawString(
          radix.getIndexChar(),
          (int) ((double) LabelValueXOffset / 0.7),
          (int) ((double) labelYPos / 0.7));
      g2.scale(1.0 / 0.7, 1.0 / 0.7);
      g.setColor(baseColor);
      if (radix == null || radix == RadixOption.RADIX_2) {
        int wid = value.getWidth();
        if (wid == 0) {
          GraphicsUtil.switchToWidth(g, 2);
          int x = -15 - (width - 15) / 2;
          g.drawLine(x - 4, 0, x + 4, 0);
          if (dir == Direction.WEST) {
            g2.translate(-westTranslate, 0);
            g2.rotate(Math.PI);
          }
          return;
        }
        int x0 = (isOutput) ? -20 : -25;
        int cx = x0;
        int cy = height / 2 - 12;
        int cur = 0;
        for (int k = 0; k < wid; k++) {
          if (radix == RadixOption.RADIX_2 && !isOutput) {
            g.setColor(value.get(k).getColor());
            g.fillOval(cx - 4, cy - 5, 9, 14);
            g.setColor(Color.WHITE);
          }
          GraphicsUtil.drawCenteredText(g, value.get(k).toDisplayString(), cx, cy);
          if (radix == RadixOption.RADIX_2 && !isOutput) g.setColor(baseColor);
          ++cur;
          if (cur == 8) {
            cur = 0;
            cx = x0;
            cy -= 20;
          } else {
            cx -= 10;
          }
        }
      } else {
        String text = radix.toString(value);
        int cx = (isOutput) ? -15 : -20;
        for (int k = text.length() - 1; k >= 0; k--) {
          GraphicsUtil.drawText(
              g, text.substring(k, k + 1), cx, -2, GraphicsUtil.H_RIGHT, GraphicsUtil.H_CENTER);
          cx -= Pin.DIGIT_WIDTH;
        }
      }
    }
    if (dir == Direction.WEST) {
      g2.translate(-westTranslate, 0);
      g2.rotate(Math.PI);
    }
  }

  private void drawInputShape(
      InstancePainter painter,
      int x,
      int y,
      int width,
      int height,
      Color LineColor,
      boolean isGhost) {
    PinAttributes attrs = (PinAttributes) painter.getAttributeSet();
    boolean NewShape = attrs.getValue(ProbeAttributes.PROBEAPPEARANCE) == ProbeAttributes.APPEAR_EVOLUTION_NEW;
    boolean isBus = attrs.getValue(StdAttr.WIDTH).getWidth() > 1;
    Direction dir = attrs.getValue(StdAttr.FACING);
    Graphics g = painter.getGraphics();
    if (!NewShape) {
      g.drawRect(x + 1, y + 1, width - 1, height - 1);
      if (!isGhost) {
        if (!painter.getShowState()) {
          g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
          GraphicsUtil.drawCenteredText(
              g, "x" + attrs.width.getWidth(), x + width / 2, y + height / 2);
        } else {
          Probe.paintValue(painter, getState(painter).intendedValue, !isBus);
        }
      }
    } else {
      Graphics2D g2 = (Graphics2D) g;
      int xpos = x + width;
      int ypos = y + height / 2;
      int rwidth = width;
      int rheight = height;
      double rotation = 0;
      if (dir == Direction.NORTH) {
        rotation = -Math.PI / 2;
        xpos = x + width / 2;
        ypos = y;
        rwidth = height;
        rheight = width;
      } else if (dir == Direction.SOUTH) {
        rotation = Math.PI / 2;
        xpos = x + width / 2;
        ypos = y + height;
        rwidth = height;
        rheight = width;
      } else if (dir == Direction.WEST) {
        rotation = Math.PI;
        xpos = x;
        ypos = y + height / 2;
      }
      g2.translate(xpos, ypos);
      g2.rotate(rotation);
      Color col = g.getColor();
      if (isBus) {
        g.setColor(Value.multiColor);
        GraphicsUtil.switchToWidth(g, Wire.WIDTH_BUS);
        g.drawLine(Wire.WIDTH_BUS / 2 - 5, 0, 0, 0);
        GraphicsUtil.switchToWidth(g, 2);
      } else {
        if (painter.getShowState())
          g.setColor(LineColor);
        GraphicsUtil.switchToWidth(g, Wire.WIDTH);
        g.drawLine(-5, 0, 0, 0);
        GraphicsUtil.switchToWidth(g, 2);
      }
      g.setColor(col);
      int[] xPoints = new int[] {-rwidth, -15, -5, -15, -rwidth};
      int yBottom = rheight / 2;
      int yTop = -yBottom;
      int[] yPoints = new int[] {yTop, yTop, 0, yBottom, yBottom};
      g.drawPolygon(xPoints, yPoints, 5);
      drawNewStyleValue(painter, rwidth, rheight, false, isGhost);
      g2.rotate(-rotation);
      g2.translate(-xpos, -ypos);
    }
  }

  private void drawOutputShape(
      InstancePainter painter,
      int x,
      int y,
      int width,
      int height,
      Color LineColor,
      boolean isGhost) {
    PinAttributes attrs = (PinAttributes) painter.getAttributeSet();
    boolean NewShape = attrs.getValue(ProbeAttributes.PROBEAPPEARANCE) == ProbeAttributes.APPEAR_EVOLUTION_NEW;
    boolean isBus = attrs.getValue(StdAttr.WIDTH).getWidth() > 1;
    Direction dir = attrs.getValue(StdAttr.FACING);
    Graphics g = painter.getGraphics();
    if (NewShape) {
      Graphics2D g2 = (Graphics2D) g;
      int xpos = x + width;
      int ypos = y + height / 2;
      int rwidth = width;
      int rheight = height;
      double rotation = 0;
      if (dir == Direction.NORTH) {
        rotation = -Math.PI / 2;
        xpos = x + width / 2;
        ypos = y;
        rwidth = height;
        rheight = width;
      } else if (dir == Direction.SOUTH) {
        rotation = Math.PI / 2;
        xpos = x + width / 2;
        ypos = y + height;
        rwidth = height;
        rheight = width;
      } else if (dir == Direction.WEST) {
        rotation = Math.PI;
        xpos = x;
        ypos = y + height / 2;
      }
      g2.translate(xpos, ypos);
      g2.rotate(rotation);
      final var col = g.getColor();
      if (isBus) {
        g.setColor(Value.multiColor);
        GraphicsUtil.switchToWidth(g, Wire.WIDTH_BUS);
        g.drawLine(-3, 0, -Wire.WIDTH_BUS / 2, 0);
        GraphicsUtil.switchToWidth(g, 2);
      } else {
        if (painter.getShowState())
          g.setColor(LineColor);
        GraphicsUtil.switchToWidth(g, Wire.WIDTH);
        g.drawLine(-3, 0, 0, 0);
        GraphicsUtil.switchToWidth(g, 2);
      }
      g.setColor(col);
      int[] xPoints = new int[] {-5, 10 - rwidth, -rwidth, 10 - rwidth, -5};
      int yTop = rheight / 2;
      int yBottom = -yTop;
      int[] yPoints = new int[] {yTop, yTop, 0, yBottom, yBottom};
      g.drawPolygon(xPoints, yPoints, 5);
      drawNewStyleValue(painter, rwidth, rheight, true, isGhost);
      g2.rotate(-rotation);
      g2.translate(-xpos, -ypos);
    } else {
      if (!isBus) {
        g.drawOval(x + 1, y + 1, width - 1, height - 1);
      } else {
        g.drawRoundRect(x + 1, y + 1, width - 1, height - 1, 6, 6);
      }
      if (!isGhost) {
        if (!painter.getShowState()) {
          g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
          GraphicsUtil.drawCenteredText(
              g, "x" + attrs.width.getWidth(), x + width / 2, y + height / 2);
        } else {
          Probe.paintValue(painter, getState(painter).intendedValue, !isBus);
        }
      }
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    PinAttributes attrs = (PinAttributes) painter.getAttributeSet();
    Location loc = painter.getLocation();
    Bounds bds = painter.getOffsetBounds();
    int x = loc.getX();
    int y = loc.getY();
    Graphics g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    if (attrs.isOutput()) {
      drawOutputShape(
          painter,
          x + bds.getX(),
          y + bds.getY(),
          bds.getWidth(),
          bds.getHeight(),
          Color.GRAY,
          true);
    } else {
      drawInputShape(
          painter,
          x + bds.getX(),
          y + bds.getY(),
          bds.getWidth(),
          bds.getHeight(),
          Color.GRAY,
          true);
    }
  }

  @Override
  public void paintIcon(InstancePainter painter) {
    PinAttributes attrs = (PinAttributes) painter.getAttributeSet();
    Direction dir = attrs.facing;
    boolean output = attrs.isOutput();
    Graphics2D g = (Graphics2D) painter.getGraphics();
    int iconSize = AppPreferences.getIconSize();
    GraphicsUtil.switchToWidth(g, AppPreferences.getScaled(1));
    BitWidth w = attrs.getValue(StdAttr.WIDTH);
    int pinSize = iconSize >> 2;
    final var baseColor = g.getColor();
    if (attrs.getValue(ProbeAttributes.PROBEAPPEARANCE) == ProbeAttributes.APPEAR_EVOLUTION_NEW) {
      int arrowHeight = (10 * iconSize) >> 4;
      int yoff = (3 * iconSize) >> 4;
      int xoff = output ? pinSize : 0;
      int[] yPoints =
          new int[] {yoff, yoff, yoff + (arrowHeight >> 1), yoff + arrowHeight, yoff + arrowHeight};
      int[] xPoints =
          new int[] {
            xoff,
            xoff + iconSize - (pinSize << 1),
            xoff + iconSize - pinSize,
            xoff + iconSize - (pinSize << 1),
            xoff
          };
      g.setColor(baseColor);
      g.drawPolygon(xPoints, yPoints, xPoints.length);
      g.setColor(Value.TRUE.getColor());
      GraphicsUtil.switchToWidth(g, AppPreferences.getScaled(2));
      if (output)
        g.drawLine(0, yoff + (arrowHeight >> 1), pinSize, yoff + (arrowHeight >> 1));
      else
        g.drawLine(iconSize - pinSize, yoff + (arrowHeight >> 1), iconSize, yoff + (arrowHeight >> 1));
    } else {
      int iconOffset = AppPreferences.getScaled(4);
      int boxWidth = iconSize - (iconOffset << 1);
      int pinWidth = AppPreferences.getScaled(3);
      int pinx = iconOffset + boxWidth;
      int piny = iconOffset + (boxWidth >> 1) - (pinWidth >> 1);
      if (dir == Direction.WEST) {
        pinx = iconOffset - pinWidth;
      } else if (dir == Direction.NORTH) {
        pinx = iconOffset + (boxWidth >> 1) - (pinWidth >> 1);
        piny = iconOffset - pinWidth;
      } else if (dir == Direction.SOUTH) {
        pinx = iconOffset + (boxWidth >> 1) - (pinWidth >> 1);
        piny = iconOffset + boxWidth;
      }
      g.setColor(baseColor);
      if (output) {
        g.drawOval(iconOffset, iconOffset, boxWidth, boxWidth);
      } else {
        g.drawRect(iconOffset, iconOffset, boxWidth, boxWidth);
      }
      g.setColor(Value.TRUE.getColor());
      g.fillOval(iconOffset + (boxWidth >> 2), iconOffset + (boxWidth >> 3), boxWidth >> 1, (3 * boxWidth) >> 2);
      g.fillOval(pinx, piny, pinWidth, pinWidth);
    }
    if (!w.equals(BitWidth.ONE)) {
      g.setColor(ICON_WIDTH_COLOR);
      g.setFont(ICON_WIDTH_FONT);
      TextLayout bw = new TextLayout(Integer.toString(w.getWidth()), ICON_WIDTH_FONT, g.getFontRenderContext());
      float xpos = (float) AppPreferences.getIconSize() / 2 - (float) bw.getBounds().getCenterX();
      float ypos = (float) AppPreferences.getIconSize() / 2 - (float) bw.getBounds().getCenterY();
      if (attrs.getValue(ProbeAttributes.PROBEAPPEARANCE) == ProbeAttributes.APPEAR_EVOLUTION_NEW)
        if (output)
          xpos = pinSize + (iconSize - pinSize) / 2 - (float) bw.getBounds().getCenterX();
        else
          xpos = (iconSize - pinSize) / 2 - (float) bw.getBounds().getCenterX();
      bw.draw(g, xpos, ypos);
      g.setColor(baseColor);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    PinAttributes attrs = (PinAttributes) painter.getAttributeSet();
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getInstance().getBounds(); // intentionally with no graphics object - we don't want label included
    boolean IsOutput = attrs.type == OUTPUT;
    PinState state = getState(painter);
    Value found = state.foundValue;
    int x = bds.getX();
    int y = bds.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    if (IsOutput) {
      drawOutputShape(
          painter, x + 1, y + 1, bds.getWidth() - 1, bds.getHeight() - 1, found.getColor(), false);
    } else {
      drawInputShape(
          painter, x + 1, y + 1, bds.getWidth() - 1, bds.getHeight() - 1, found.getColor(), false);
    }
    painter.drawLabel();
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    PinAttributes attrs = (PinAttributes) state.getAttributeSet();

    PinState q = getState(state);
    Value found = state.getPortValue(0);
    if (attrs.type == OUTPUT) {
      q.foundValue = found; // for color
      q.intendedValue = found; // for display
    } else {
      q.foundValue = found; // for color
      Value drive = pull(attrs, q.intendedValue);
      if (!drive.equals(found))
        state.setPort(0, drive, 1);
    }
  }

  @Override
  public boolean requiresNonZeroLabel() {
    return true;
  }

  // Version 4.0.0: Simplified new behavior:
  //   Output pin [behavior option hidden, has no effect]
  //      Depending on value of connected bus, UI displays 0, 1, E, or X, and
  //      subcircuit passes whatever is displayed up to parent. Color matches
  //      both the connected bus and the value passed up to parent. This is the
  //      simplest option, and it is the only option for output pins.
  //   Input pin in simple mode: [Behavior=simple]
  //      User can choose 0 or 1. Parent circuit can send 0, 1, E, or X. UI
  //      displays whatever user chose (or parent sent). Sends whatever is
  //      displayed to connected bus. Color matches whatever appears on
  //      connected bus (which may be different from what is displayed). Note:
  //      This mode never generates floating values, it only passes them along
  //      from a parent circuit.
  //   Input pin in tristate mode: [Behavior=tristate]
  //      Same circuit behavior as simple, but different UI behavior.
  //      User can choose 0, 1, or X. Parent circuit can send 0, 1, E, or X. UI
  //      displays whatever user chose (or parent sent). Sends whatever is
  //      displayed to connected bus. Color matches whatever appears on
  //      connected bus (which may be different from what is displayed). Note:
  //      This mode can generate X values from the UI, but for purposes of
  //      HDL-generation, this mode never generates floating values, it only
  //      passes them along from a parent circuit.
  //   Input pin with pull-up (or pull-down): [Behavior=pullup/pulldown]
  //      Same UI behavior as simple, but different circuit behavior.
  //      User can choose 0 or 1. Parent circuit can send 0, 1, or E, but if it
  //      tries to send X it gets converted to and displayed as 1 (or 0). Sends
  //      whatever is displayed to connected bus. Color matches whatever appears
  //      on connected bus. [or we could show blue in case of pull, or half
  //      blue]

  public void driveInputPin(InstanceState state, Value value) {
    PinAttributes attrs = (PinAttributes) state.getAttributeSet();
    PinState myState = getState(state);
    // don't pull here -- instead, pull when displaying and propagating
    myState.intendedValue = value; // pull(attrs, value);
  }

  private Value pull(PinAttributes attrs, Value value) {
    if (value == Value.NIL)
      return Value.createUnknown(attrs.width);
    else if (attrs.behavior == PULL_UP)
      return value.pullEachBitTowards(Value.TRUE);
    else if (attrs.behavior == PULL_DOWN)
      return value.pullEachBitTowards(Value.FALSE);
    else
      return value;
  }

}
