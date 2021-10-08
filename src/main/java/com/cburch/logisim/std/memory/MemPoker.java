/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class MemPoker extends InstancePoker {
  private static class AddrPoker extends MemPoker {
    @Override
    public Bounds getBounds(InstancePainter painter) {
      final var data = (MemState) painter.getData();
      return data.getBounds(-1, painter.getBounds());
    }

    @Override
    public void keyTyped(InstanceState state, KeyEvent e) {
      final var c = e.getKeyChar();
      final var val = Character.digit(e.getKeyChar(), 16);
      final var data = (MemState) state.getData();
      if (val >= 0) {
        long newScroll = (data.getScroll() * 16 + val) & (data.getLastAddress());
        data.setScroll(newScroll);
      } else if (c == ' ') {
        data.setScroll(data.getScroll() + (data.getNrOfLines() - 1) * data.getNrOfLineItems());
      } else if (c == '\r' || c == '\n') {
        data.setScroll(data.getScroll() + data.getNrOfLineItems());
      } else if (c == '\u0008' || c == '\u007f') {
        data.setScroll(data.getScroll() - data.getNrOfLineItems());
      } else if (c == 'R' || c == 'r') {
        data.getContents().clear();
      }
    }

    @Override
    public void keyPressed(InstanceState state, KeyEvent e) {
      final var data = (MemState) state.getData();
      if (e.getKeyCode() == KeyEvent.VK_UP) {
        data.setScroll(data.getScroll() - data.getNrOfLineItems());
      } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
        data.setScroll(data.getScroll() + data.getNrOfLineItems());
      } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
        data.setScroll(data.getScroll() - data.getNrOfLineItems());
      } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
        data.setScroll(data.getScroll() + data.getNrOfLineItems());
      } else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
        data.setScroll(data.getScroll() - (data.getNrOfLines() - 1) * data.getNrOfLineItems());
      } else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
        data.setScroll(data.getScroll() + (data.getNrOfLines() - 1) * data.getNrOfLineItems());
      }
    }

    @Override
    public void paint(InstancePainter painter) {
      final var bds = getBounds(painter);
      final var g = painter.getGraphics();
      g.setColor(Color.RED);
      g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g.setColor(Color.BLACK);
    }
  }

  private static class DataPoker extends MemPoker {
    long initValue;
    long curValue;

    private DataPoker(InstanceState state, MemState data, long addr) {
      data.setCursor(addr);
      initValue = data.getContents().get(data.getCursor());
      curValue = initValue;

      Object attrs = state.getInstance().getAttributeSet();
      if (attrs instanceof RomAttributes) {
        final var proj = state.getProject();
        if (proj != null) {
          ((RomAttributes) attrs).setProject(proj);
        }
      }
    }

    @Override
    public Bounds getBounds(InstancePainter painter) {
      final var data = (MemState) painter.getData();
      final var inBounds = painter.getInstance().getBounds();
      return data.getDataBounds(data.getCursor(), inBounds);
    }

    @Override
    public void keyTyped(InstanceState state, KeyEvent e) {
      final var c = e.getKeyChar();
      final var val = Character.digit(e.getKeyChar(), 16);
      final var data = (MemState) state.getData();
      if (val >= 0) {
        curValue = curValue * 16 + val;
        data.getContents().set(data.getCursor(), curValue);
        state.fireInvalidated();
      } else if (c == ' ' || c == '\t') {
        moveTo(data, data.getCursor() + 1);
      } else if (c == '\r' || c == '\n') {
        moveTo(data, data.getCursor() + data.getNrOfLineItems());
      } else if (c == '\u0008' || c == '\u007f') {
        moveTo(data, data.getCursor() - 1);
      } else if (c == 'R' || c == 'r') {
        data.getContents().clear();
      }
    }

    @Override
    public void keyPressed(InstanceState state, KeyEvent e) {
      final var data = (MemState) state.getData();
      if (e.getKeyCode() == KeyEvent.VK_UP) {
        moveTo(data, data.getCursor() - data.getNrOfLineItems());
      } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
        moveTo(data, data.getCursor() + data.getNrOfLineItems());
      } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
        moveTo(data, data.getCursor() - 1);
      } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
        moveTo(data, data.getCursor() + 1);
      } else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
        moveTo(data, data.getCursor() - (data.getNrOfLines() - 1) * data.getNrOfLineItems());
      } else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
        moveTo(data, data.getCursor() + (data.getNrOfLines() - 1) * data.getNrOfLineItems());
      }
    }

    private void moveTo(MemState data, long addr) {
      if (data.isValidAddr(addr)) {
        data.setCursor(addr);
        data.scrollToShow(addr);
        initValue = data.getContents().get(addr);
        curValue = initValue;
      }
    }

    @Override
    public void paint(InstancePainter painter) {
      final var bds = getBounds(painter);
      if (bds == null || bds == Bounds.EMPTY_BOUNDS) return;
      final var g = painter.getGraphics();
      g.setColor(Color.RED);
      g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g.setColor(Color.BLACK);
    }

    @Override
    public void stopEditing(InstanceState state) {
      final var data = (MemState) state.getData();
      data.setCursor(-1);
    }
  }

  private MemPoker sub;

  @Override
  public Bounds getBounds(InstancePainter state) {
    return sub.getBounds(state);
  }

  @Override
  public boolean init(InstanceState state, MouseEvent event) {
    final var bds = state.getInstance().getBounds();
    final var data = (MemState) state.getData();
    long addr = data.getAddressAt(event.getX() - bds.getX(), event.getY() - bds.getY());

    // See if outside box
    if (addr < 0) {
      sub = new AddrPoker();
    } else {
      sub = new DataPoker(state, data, addr);
    }
    return true;
  }

  @Override
  public void keyPressed(InstanceState state, KeyEvent e) {
    sub.keyPressed(state, e);
  }

  @Override
  public void keyTyped(InstanceState state, KeyEvent e) {
    sub.keyTyped(state, e);
  }

  @Override
  public void paint(InstancePainter painter) {
    sub.paint(painter);
  }
}
