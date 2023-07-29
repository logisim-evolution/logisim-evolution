/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.hex;

import com.cburch.contracts.BaseKeyListenerContract;
import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.contracts.BaseMouseMotionListenerContract;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Caret {
  private static final Color SELECT_COLOR = new Color(192, 192, 255);
  private static final Stroke CURSOR_STROKE = new BasicStroke(2.0F);
  private final HexEditor hex;
  private final List<ChangeListener> listeners;
  private long mark;
  private long cursor;
  private Object highlight;

  Caret(HexEditor hex) {
    this.hex = hex;
    this.listeners = new ArrayList<>();
    this.cursor = -1;

    final var l = new Listener();
    hex.addMouseListener(l);
    hex.addMouseMotionListener(l);
    hex.addKeyListener(l);
    hex.addFocusListener(l);

    final var imap = hex.getInputMap();
    final var amap = hex.getActionMap();
    final var nullAction =
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          @Override
          public void actionPerformed(ActionEvent e) {
            // dummy
          }
        };
    final var nullKey = "null";
    amap.put(nullKey, nullAction);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), nullKey);
  }

  public void addChangeListener(ChangeListener l) {
    listeners.add(l);
  }

  private void expose(long loc, boolean scrollTo) {
    if (loc >= 0) {
      final var measures = hex.getMeasures();
      final var x = measures.toX(loc);
      final var y = measures.toY(loc);
      final var w = measures.getCellWidth();
      final var h = measures.getCellHeight();
      hex.repaint(x - 1, y - 1, w + 2, h + 2);
      if (scrollTo) {
        hex.scrollRectToVisible(new Rectangle(x, y, w, h));
      }
    }
  }

  public long getDot() {
    return cursor;
  }

  public long getMark() {
    return mark;
  }

  void paintForeground(Graphics g, long start, long end) {
    if (cursor >= start && cursor < end && hex.isFocusOwner()) {
      final var measures = hex.getMeasures();
      final var x = measures.toX(cursor);
      final var y = measures.toY(cursor);
      final var g2 = (Graphics2D) g;
      final var oldStroke = g2.getStroke();
      g2.setColor(hex.getForeground());
      g2.setStroke(CURSOR_STROKE);
      g2.drawRect(x, y, measures.getCellWidth() - 1, measures.getCellHeight() - 1);
      g2.setStroke(oldStroke);
    }
  }

  public void removeChangeListener(ChangeListener l) {
    listeners.remove(l);
  }

  public void setDot(long value, boolean keepMark) {
    final var model = hex.getModel();
    if (model == null || value < model.getFirstOffset() || value > model.getLastOffset()) {
      value = -1;
    }
    if (cursor != value) {
      final var oldValue = cursor;
      if (highlight != null) {
        hex.getHighlighter().remove(highlight);
        highlight = null;
      }
      if (!keepMark) {
        mark = value;
      } else if (mark != value) {
        highlight = hex.getHighlighter().add(mark, value, SELECT_COLOR);
      }
      cursor = value;
      expose(oldValue, false);
      expose(value, true);
      if (!listeners.isEmpty()) {
        final var event = new ChangeEvent(this);
        for (final var l : listeners) {
          l.stateChanged(event);
        }
      }
    }
  }

  private class Listener
      implements BaseMouseListenerContract,
          BaseMouseMotionListenerContract,
          BaseKeyListenerContract,
          FocusListener {

    private void movecursor(int m, boolean shift) {
      final var cols = hex.getMeasures().getColumnCount();
      int rows;
      switch (m) {
        case KeyEvent.VK_UP -> {
          if (cursor >= cols) {
            setDot(cursor - cols, shift);
          }
        }
        case KeyEvent.VK_LEFT -> {
          if (cursor >= 1) {
            setDot(cursor - 1, shift);
          }
        }
        case KeyEvent.VK_DOWN -> {
          if (cursor >= hex.getModel().getFirstOffset()
                  && cursor <= hex.getModel().getLastOffset() - cols) {
            setDot(cursor + cols, shift);
          }
        }
        case KeyEvent.VK_RIGHT -> {
          if (cursor >= hex.getModel().getFirstOffset()
                  && cursor <= hex.getModel().getLastOffset() - 1) {
            setDot(cursor + 1, shift);
          }
        }
        case KeyEvent.VK_HOME -> {
          if (cursor >= 0) {
            final var dist = (int) (cursor % cols);
            if (dist == 0) {
              setDot(0, shift);
            } else {
              setDot(cursor - dist, shift);
            }
          }
        }
        case KeyEvent.VK_END -> {
          if (cursor >= 0) {
            final var model = hex.getModel();
            var dest = (cursor / cols * cols) + cols - 1;
            if (model != null) {
              final var end = model.getLastOffset();
              if (dest > end || dest == cursor) {
                dest = end;
              }
            }
            setDot(dest, shift);
          }
        }
        case KeyEvent.VK_PAGE_DOWN -> {
          rows = hex.getVisibleRect().height / hex.getMeasures().getCellHeight();
          if (rows > 2) {
            rows--;
          }
          if (cursor >= 0) {
            final var max = hex.getModel().getLastOffset();
            if (cursor + rows * cols <= max) {
              setDot(cursor + rows * cols, shift);
            } else {
              var n = cursor;
              while (n + cols < max) {
                n += cols;
              }
              setDot(n, shift);
            }
          }
        }
        case KeyEvent.VK_PAGE_UP -> {
          rows = hex.getVisibleRect().height / hex.getMeasures().getCellHeight();
          if (rows > 2) {
            rows--;
          }
          if (cursor >= rows * cols) {
            setDot(cursor - rows * cols, shift);
          } else if (cursor >= cols) {
            setDot(cursor % cols, shift);
          }
        }

        default -> {
        }
      }
      // do nothing
    }

    
    @Override
    public void focusGained(FocusEvent e) {
      expose(cursor, false);
    }

    @Override
    public void focusLost(FocusEvent e) {
      expose(cursor, false);
    }

    @Override
    public void keyPressed(KeyEvent e) {
      final var shift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
      movecursor(e.getKeyCode(), shift);
    }

    @Override
    public void keyTyped(KeyEvent e) {
      final var shift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
      final var ctrlx = e.isControlDown();
      switch (e.getKeyChar()) {
        case ' ' -> {
          if (ctrlx) {
            movecursor(KeyEvent.VK_PAGE_DOWN, shift);
          } else {
            movecursor(KeyEvent.VK_RIGHT, shift);
          }
        }
        case '\n' -> {
          if (ctrlx) {
            movecursor(KeyEvent.VK_UP, shift);
          } else {
            movecursor(KeyEvent.VK_DOWN, shift);
          }
        }

        case '\u0008' ->
          movecursor(KeyEvent.VK_LEFT, shift);
        case '\u007f' -> {
          if (ctrlx) {
            movecursor(KeyEvent.VK_PAGE_UP, shift);
          } else {
            hex.delete();
          }
        }

        default -> {
          final var digit = Character.digit(e.getKeyChar(), 16);
          if (digit >= 0) {
            final var model = hex.getModel();
            if (model != null
                    && cursor >= model.getFirstOffset()
                    && cursor <= model.getLastOffset()) {
              final var curValue = model.get(cursor);
              final var newValue = 16 * curValue + digit;
              model.set(cursor, newValue);
            }
          }
        }
      }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      final var measures = hex.getMeasures();
      final var loc = measures.toAddress(e.getX(), e.getY());
      setDot(loc, true);

      // TODO should repeat dragged events when mouse leaves the
      // component
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
      // do nothing
    }

    @Override
    public void mousePressed(MouseEvent e) {
      final var measures = hex.getMeasures();
      final var loc = measures.toAddress(e.getX(), e.getY());
      setDot(loc, (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0);
      if (!hex.isFocusOwner()) hex.requestFocus();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      mouseDragged(e);
    }
  }
}
