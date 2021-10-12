/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.CaretEvent;
import com.cburch.logisim.tools.CaretListener;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;

class TextFieldCaret implements Caret, TextFieldListener {

  public static final Color EDIT_BACKGROUND = new Color(0xff, 0xff, 0x99);
  public static final Color EDIT_BORDER = Color.DARK_GRAY;
  public static final Color SELECTION_BACKGROUND = new Color(0x99, 0xcc, 0xff);

  private final LinkedList<CaretListener> listeners = new LinkedList<>();
  private final TextField field;
  private final Graphics g;
  private String oldText;
  private String curText;
  private int pos;
  private int end;

  public TextFieldCaret(TextField field, Graphics g, int pos) {
    this.field = field;
    this.g = g;
    this.oldText = field.getText();
    this.curText = field.getText();
    this.pos = pos;
    this.end = pos;

    field.addTextFieldListener(this);
  }

  public TextFieldCaret(TextField field, Graphics g, int x, int y) {
    this(field, g, 0);
    pos = end = findCaret(x, y);
  }

  @Override
  public void addCaretListener(CaretListener l) {
    listeners.add(l);
  }

  @Override
  public void cancelEditing() {
    final var e = new CaretEvent(this, oldText, oldText);
    curText = oldText;
    pos = curText.length();
    end = pos;
    for (final var l : new ArrayList<>(listeners)) {
      l.editingCanceled(e);
    }
    field.removeTextFieldListener(this);
  }

  @Override
  public void commitText(String text) {
    curText = text;
    pos = curText.length();
    end = pos;
    field.setText(text);
  }

  @Override
  public void draw(Graphics g) {
    final var x = field.getX();
    final var y = field.getY();
    final var halign = field.getHAlign();
    final var valign = field.getVAlign();
    if (field.getFont() != null) g.setFont(field.getFont());

    // draw boundary
    final var box = getBounds(g);
    g.setColor(EDIT_BACKGROUND);
    g.fillRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    g.setColor(EDIT_BORDER);
    g.drawRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());

    // draw selection
    if (pos != end) {
      g.setColor(SELECTION_BACKGROUND);
      final var p = GraphicsUtil.getTextCursor(g, curText, x, y, Math.min(pos, end), halign, valign);
      final var e = GraphicsUtil.getTextCursor(g, curText, x, y, Math.max(pos, end), halign, valign);
      g.fillRect(p.x, p.y - 1, e.x - p.x + 1, e.height + 2);
    }

    // draw text
    g.setColor(Color.BLACK);
    GraphicsUtil.drawText(g, curText, x, y, halign, valign);

    // draw cursor
    if (pos == end) {
      final var p = GraphicsUtil.getTextCursor(g, curText, x, y, pos, halign, valign);
      g.drawLine(p.x, p.y, p.x, p.y + p.height);
    }
  }

  @Override
  public String getText() {
    return curText;
  }

  @Override
  public Bounds getBounds(Graphics g) {
    final var x = field.getX();
    final var y = field.getY();
    final var halign = field.getHAlign();
    final var valign = field.getVAlign();
    final var font = field.getFont();
    final var bds = Bounds.create(GraphicsUtil.getTextBounds(g, font, curText, x, y, halign, valign));
    return bds.add(field.getBounds(g)).expand(3);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    final var ign = InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK;
    if ((e.getModifiersEx() & ign) != 0) return;
    final var shift = ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0);
    final var ctrl = ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);
    arrowKeyMaybePressed(e, shift, ctrl);
    if (e.isConsumed()) return;
    if (ctrl)
      controlKeyPressed(e, shift);
    else
      normalKeyPressed(e, shift);
  }

  protected boolean wordBoundary(int pos) {
    return (pos == 0
        || pos >= curText.length()
        || (Character.isWhitespace(curText.charAt(pos - 1))
            != Character.isWhitespace(curText.charAt(pos))));
  }

  protected boolean allowedCharacter(char c) {
    return (c != KeyEvent.CHAR_UNDEFINED) && !Character.isISOControl(c);
  }

  protected void moveCaret(int dx, int dy, boolean shift, boolean ctrl) {
    if (!shift) normalizeSelection();

    if (dy < 0) {
      pos = 0;
    } else if (dy > 0) {
      pos = curText.length();
    } else if (pos + dx >= 0 && pos + dx <= curText.length()) {
      if (!shift && pos != end) {
        if (dx < 0) end = pos;
        else pos = end;
      } else {
        pos += dx;
      }
      while (ctrl && !wordBoundary(pos)) pos += dx;
    }

    if (!shift) end = pos;
  }

  @SuppressWarnings("fallthrough")
  protected void controlKeyPressed(KeyEvent e, boolean shift) {
    var cut = false;
    switch (e.getKeyCode()) {
      case KeyEvent.VK_A:
        pos = 0;
        end = curText.length();
        e.consume();
        break;
      case KeyEvent.VK_CUT:
      case KeyEvent.VK_X:
        cut = true;
        // fall through
      case KeyEvent.VK_COPY:
      case KeyEvent.VK_C:
        if (end != pos) {
          final var pp = (Math.min(pos, end));
          final var ee = (Math.max(pos, end));
          final var s = curText.substring(pp, ee);
          final var sel = new StringSelection(s);
          Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
          if (cut) {
            normalizeSelection();
            curText =
                curText.substring(0, pos) + (end < curText.length() ? curText.substring(end) : "");
            end = pos;
          }
        }
        e.consume();
        break;
      case KeyEvent.VK_INSERT:
      case KeyEvent.VK_PASTE:
      case KeyEvent.VK_V:
        try {
          String s =
              (String)
                  Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
          var lastWasSpace = false;
          for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (!allowedCharacter(c)) {
              if (lastWasSpace) continue;
              c = ' ';
            }
            lastWasSpace = (c == ' ');
            normalizeSelection();
            curText = (end < curText.length())
                    ? curText.substring(0, pos) + c + curText.substring(end)
                    : curText.substring(0, pos) + c;
            ++pos;
            end = pos;
          }
        } catch (Exception ignored) {
        }
        e.consume();
        break;
      default:// ignore
    }
  }

  protected void arrowKeyMaybePressed(KeyEvent e, boolean shift, boolean ctrl) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_KP_LEFT:
        moveCaret(-1, 0, shift, ctrl);
        e.consume();
        break;
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_KP_RIGHT:
        moveCaret(1, 0, shift, ctrl);
        e.consume();
        break;
      case KeyEvent.VK_UP:
      case KeyEvent.VK_KP_UP:
        moveCaret(0, -1, shift, ctrl);
        e.consume();
        break;
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_KP_DOWN:
        moveCaret(0, 1, shift, ctrl);
        e.consume();
        break;
      case KeyEvent.VK_HOME:
        pos = 0;
        if (!shift) {
          end = pos;
        }
        e.consume();
        break;
      case KeyEvent.VK_END:
        pos = curText.length();
        if (!shift) {
          end = pos;
        }
        e.consume();
        break;
      default:
        break;
    }
  }

  protected void normalKeyPressed(KeyEvent e, boolean shift) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_ESCAPE:
      case KeyEvent.VK_CANCEL:
        cancelEditing();
        break;
      case KeyEvent.VK_CLEAR:
        curText = "";
        end = pos = 0;
        break;
      case KeyEvent.VK_ENTER:
        stopEditing();
        break;
      case KeyEvent.VK_BACK_SPACE:
        normalizeSelection();
        if (pos != end) {
          curText = curText.substring(0, pos) + curText.substring(end);
          end = pos;
        } else if (pos > 0) {
          curText = curText.substring(0, pos - 1) + curText.substring(pos);
          --pos;
          end = pos;
        }
        break;
      case KeyEvent.VK_DELETE:
        normalizeSelection();
        if (pos != end) {
          curText =
              curText.substring(0, pos) + (end < curText.length() ? curText.substring(end) : "");
          end = pos;
        } else if (pos < curText.length()) {
          curText = curText.substring(0, pos) + curText.substring(pos + 1);
        }
        break;
      default:// ignore
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {
    final var ign = InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK;
    if ((e.getModifiersEx() & ign) != 0) return;

    final var c = e.getKeyChar();
    if (allowedCharacter(c)) {
      normalizeSelection();
      if (end < curText.length()) {
        curText = curText.substring(0, pos) + c + curText.substring(end);
      } else {
        curText = curText.substring(0, pos) + c;
      }
      ++pos;
      end = pos;
    } else if (c == '\n') {
      stopEditing();
    }
  }

  protected void normalizeSelection() {
    if (pos > end) {
      int t = end;
      end = pos;
      pos = t;
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    end = findCaret(e.getX(), e.getY());
  }

  @Override
  public void mousePressed(MouseEvent e) {
    pos = end = findCaret(e.getX(), e.getY());
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    end = findCaret(e.getX(), e.getY());
  }

  protected int findCaret(int x, int y) {
    x -= field.getX();
    y -= field.getY();
    final var halign = field.getHAlign();
    final var valign = field.getVAlign();
    return GraphicsUtil.getTextPosition(g, curText, x, y, halign, valign);
  }

  @Override
  public void removeCaretListener(CaretListener l) {
    listeners.remove(l);
  }

  @Override
  public void stopEditing() {
    final var e = new CaretEvent(this, oldText, curText);
    field.setText(curText);
    for (final var l : new ArrayList<>(listeners)) {
      l.editingStopped(e);
    }
    field.removeTextFieldListener(this);
  }

  @Override
  public void textChanged(TextFieldEvent e) {
    curText = field.getText();
    oldText = curText;
    pos = curText.length();
    end = pos;
  }
}
