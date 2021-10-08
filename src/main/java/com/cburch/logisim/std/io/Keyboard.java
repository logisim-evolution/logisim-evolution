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
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.KeyboardIcon;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class Keyboard extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Keyboard";

  public static class Poker extends InstancePoker {
    public void draw(InstancePainter painter) {
      final var data = getKeyboardState(painter);
      final var bds = painter.getInstance().getBounds();
      final var g = painter.getGraphics();
      final var fm = g.getFontMetrics(DEFAULT_FONT);

      String str;
      int cursor;
      int dispStart;
      synchronized (data) {
        str = data.toString();
        cursor = data.getCursorPosition();
        if (!data.isDisplayValid()) data.updateDisplay(fm);
        dispStart = data.getDisplayStart();
      }

      final var asc = fm.getAscent();
      var x = bds.getX() + 8;
      if (dispStart > 0) {
        x += fm.stringWidth(str.charAt(0) + "m");
        x += fm.stringWidth(str.substring(dispStart, cursor));
      } else if (cursor >= str.length()) {
        x += fm.stringWidth(str);
      } else {
        x += fm.stringWidth(str.substring(0, cursor));
      }
      final var y = bds.getY() + (bds.getHeight() + asc) / 2;
      g.drawLine(x, y - asc, x, y);
    }

    @Override
    public void keyPressed(InstanceState state, KeyEvent e) {
      final var data = getKeyboardState(state);
      var changed = false;
      var used = true;

      synchronized (data) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_DELETE:
            changed = data.delete();
            break;
          case KeyEvent.VK_LEFT:
            data.moveCursorBy(-1);
            break;
          case KeyEvent.VK_RIGHT:
            data.moveCursorBy(1);
            break;
          case KeyEvent.VK_HOME:
            data.setCursor(0);
            break;
          case KeyEvent.VK_END:
            data.setCursor(Integer.MAX_VALUE);
            break;
          default:
            used = false;
        }
      }
      if (used) e.consume();
      if (changed) state.getInstance().fireInvalidated();
    }

    @Override
    public void keyTyped(InstanceState state, KeyEvent e) {
      final var data = getKeyboardState(state);
      final var ch = e.getKeyChar();
      var changed = false;
      if (ch != KeyEvent.CHAR_UNDEFINED) {
        if (!Character.isISOControl(ch) || ch == '\b' || ch == '\n' || ch == FORM_FEED) {
          synchronized (data) {
            changed = data.insert(ch);
          }
          e.consume();
        }
      }
      if (changed) state.getInstance().fireInvalidated();
    }
  }

  public static void addToBuffer(InstanceState state, char[] newChars) {
    final var keyboardData = getKeyboardState(state);
    for (char newChar : newChars) {
      keyboardData.insert(newChar);
    }
  }

  private static int getBufferLength(Object bufferAttr) {
    return (bufferAttr instanceof Integer)
           ? (Integer) bufferAttr
           : 32;
  }

  private static KeyboardData getKeyboardState(InstanceState state) {
    final var bufLen = getBufferLength(state.getAttributeValue(ATTR_BUFFER));
    var ret = (KeyboardData) state.getData();
    if (ret == null) {
      ret = new KeyboardData(bufLen);
      state.setData(ret);
    } else {
      ret.updateBufferLength(bufLen);
    }
    return ret;
  }

  private static final int CLR = 0;

  private static final int CK = 1;
  private static final int RE = 2;

  private static final int AVL = 3;
  private static final int OUT = 4;

  private static final int DELAY0 = 9;
  private static final int DELAY1 = 11;

  static final int WIDTH = 145;

  static final int HEIGHT = 25;

  private static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);

  @SuppressWarnings("checkstyle:IllegalTokenText")
  private static final char FORM_FEED = 12; // control-L (LINE FEED)

  private static final Attribute<Integer> ATTR_BUFFER =
      Attributes.forIntegerRange("buflen", S.getter("keybBufferLengthAttr"), 1, 256);

  public Keyboard() {
    super(_ID, S.getter("keyboardComponent"));
    setAttributes(
        new Attribute[] {ATTR_BUFFER, StdAttr.EDGE_TRIGGER},
        new Object[] {32, StdAttr.TRIG_RISING});
    setOffsetBounds(Bounds.create(0, -15, WIDTH, HEIGHT));
    setIcon(new KeyboardIcon());
    setInstancePoker(Poker.class);

    final var ps = new Port[5];
    ps[CLR] = new Port(20, 10, Port.INPUT, 1);
    ps[CK] = new Port(0, 0, Port.INPUT, 1);
    ps[RE] = new Port(10, 10, Port.INPUT, 1);
    ps[AVL] = new Port(130, 10, Port.OUTPUT, 1);
    ps[OUT] = new Port(140, 10, Port.OUTPUT, 7);
    ps[CLR].setToolTip(S.getter("keybClearTip"));
    ps[CK].setToolTip(S.getter("keybClockTip"));
    ps[RE].setToolTip(S.getter("keybEnableTip"));
    ps[AVL].setToolTip(S.getter("keybAvailTip"));
    ps[OUT].setToolTip(S.getter("keybOutputTip"));
    setPorts(ps);
  }

  private void drawBuffer(
      Graphics g,
      FontMetrics fm,
      String str,
      int dispStart,
      int dispEnd,
      ArrayList<Integer> specials,
      Bounds bds) {
    int x = bds.getX();
    int y = bds.getY();

    g.setFont(DEFAULT_FONT);
    if (fm == null) fm = g.getFontMetrics();
    final var asc = fm.getAscent();
    final var x0 = x + 8;
    final var ys = y + (HEIGHT + asc) / 2;
    final var dotsWidth = fm.stringWidth("m");
    int xs;
    if (dispStart > 0) {
      g.drawString(str.substring(0, 1), x0, ys);
      xs = x0 + fm.stringWidth(str.charAt(0) + "m");
      drawDots(g, xs - dotsWidth, ys, dotsWidth, asc);
      final var sub = str.substring(dispStart, dispEnd);
      g.drawString(sub, xs, ys);
      if (dispEnd < str.length()) {
        drawDots(g, xs + fm.stringWidth(sub), ys, dotsWidth, asc);
      }
    } else if (dispEnd < str.length()) {
      final var sub = str.substring(dispStart, dispEnd);
      xs = x0;
      g.drawString(sub, xs, ys);
      drawDots(g, xs + fm.stringWidth(sub), ys, dotsWidth, asc);
    } else {
      xs = x0;
      g.drawString(str, xs, ys);
    }

    if (specials.size() > 0) {
      drawSpecials(specials, x0, xs, ys, asc, g, fm, str, dispStart, dispEnd);
    }
  }

  private void drawDots(Graphics g, int x, int y, int width, int ascent) {
    var r = width / 10;
    if (r < 1) r = 1;
    final var d = 2 * r;
    if (2 * r + 1 * d <= width) g.fillOval(x + r, y - d, d, d);
    if (3 * r + 2 * d <= width) g.fillOval(x + 2 * r + d, y - d, d, d);
    if (5 * r + 3 * d <= width) g.fillOval(x + 3 * r + 2 * d, y - d, d, d);
  }

  private void drawSpecials(ArrayList<Integer> specials, int x0, int xs, int ys, int asc, Graphics g, FontMetrics fm, String str, int dispStart, int dispEnd) {
    final var px = new int[3];
    final var py = new int[3];
    for (final var special : specials) {
      final var code = special;
      final var pos = code & 0xFF;
      int w0;
      int w1;
      if (pos == 0) {
        w0 = x0;
        w1 = x0 + fm.stringWidth(str.substring(0, 1));
      } else if (pos >= dispStart && pos < dispEnd) {
        w0 = xs + fm.stringWidth(str.substring(dispStart, pos));
        w1 = xs + fm.stringWidth(str.substring(dispStart, pos + 1));
      } else {
        continue; // this character is not in current view
      }
      w0++;
      w1--;

      final var key = code >> 16;
      if (key == '\b') {
        final var y1 = ys - asc / 2;
        g.drawLine(w0, y1, w1, y1);
        px[0] = w0 + 3;
        py[0] = y1 - 3;
        px[1] = w0;
        py[1] = y1;
        px[2] = w0 + 3;
        py[2] = y1 + 3;
        g.drawPolyline(px, py, 3);
      } else if (key == '\n') {
        final var y1 = ys - 3;
        px[0] = w1;
        py[0] = ys - asc;
        px[1] = w1;
        py[1] = y1;
        px[2] = w0;
        py[2] = y1;
        g.drawPolyline(px, py, 3);
        px[0] = w0 + 3;
        py[0] = y1 - 3;
        px[1] = w0;
        py[1] = y1;
        px[2] = w0 + 3;
        py[2] = y1 + 3;
        g.drawPolyline(px, py, 3);
      } else if (key == FORM_FEED) {
        g.drawRect(w0, ys - asc, w1 - w0, asc);
      }
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var showState = painter.getShowState();
    final var g = painter.getGraphics();
    painter.drawClock(CK, Direction.EAST);
    painter.drawBounds();
    painter.drawPort(CLR);
    painter.drawPort(RE);
    painter.drawPort(AVL);
    painter.drawPort(OUT);

    if (showState) {
      String str;
      int dispStart;
      int dispEnd;
      final var specials = new ArrayList<Integer>();
      FontMetrics fm = null;
      final var state = getKeyboardState(painter);
      synchronized (state) {
        str = state.toString();
        for (var i = state.getNextSpecial(0); i >= 0; i = state.getNextSpecial(i + 1)) {
          final var c = state.getChar(i);
          specials.add(c << 16 | i);
        }
        if (!state.isDisplayValid()) {
          fm = g.getFontMetrics(DEFAULT_FONT);
          state.updateDisplay(fm);
        }
        dispStart = state.getDisplayStart();
        dispEnd = state.getDisplayEnd();
      }

      if (str.length() > 0) {
        final var bds = painter.getBounds();
        drawBuffer(g, fm, str, dispStart, dispEnd, specials, bds);
      }
    } else {
      final var bds = painter.getBounds();
      final var len = getBufferLength(painter.getAttributeValue(ATTR_BUFFER));
      final var str = S.get("keybDesc", "" + len);
      final var fm = g.getFontMetrics();
      final var x = bds.getX() + (WIDTH - fm.stringWidth(str)) / 2;
      final var y = bds.getY() + (HEIGHT + fm.getAscent()) / 2;
      g.drawString(str, x, y);
    }
  }

  @Override
  public void propagate(InstanceState circState) {
    Object trigger = circState.getAttributeValue(StdAttr.EDGE_TRIGGER);
    final var state = getKeyboardState(circState);
    final var clear = circState.getPortValue(CLR);
    final var clock = circState.getPortValue(CK);
    final var enable = circState.getPortValue(RE);
    char c;

    synchronized (state) {
      final var lastClock = state.setLastClock(clock);
      if (clear == Value.TRUE) {
        state.clear();
      } else if (enable != Value.FALSE) {
        final var go = (trigger == StdAttr.TRIG_FALLING)
                ? lastClock == Value.TRUE && clock == Value.FALSE
                : lastClock == Value.FALSE && clock == Value.TRUE;
        if (go) state.dequeue();
      }

      c = state.getChar(0);
    }
    final var out = Value.createKnown(BitWidth.create(7), c & 0x7F);
    circState.setPort(OUT, out, DELAY0);
    circState.setPort(AVL, c != '\0' ? Value.TRUE : Value.FALSE, DELAY1);
  }
}
