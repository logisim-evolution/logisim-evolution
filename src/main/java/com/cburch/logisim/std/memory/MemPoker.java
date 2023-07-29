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
      final var val = Character.digit(e.getKeyChar(), 16);
      final var data = (MemState) state.getData();
      if (val >= 0) {
        long newScroll = (data.getScroll() * 16 + val) & (data.getLastAddress());
        data.setScroll(newScroll);
      } else {
        switch (e.getKeyChar()) {
          case ' ' -> {
            if (e.isControlDown()) { // Ctrl + space
              data.setScroll(data.getScroll() + (data.getNrOfLines() - 1) * data.getNrOfLineItems());
            } else {                   // Space
              data.setScroll(data.getScroll() + (data.getNrOfLines() - 1) * data.getNrOfLineItems());
            }
          }
          case '\n', '\r' -> {
            if (e.isControlDown()) {  // Ctrl + Cariage return
              data.setScroll(data.getScroll() - data.getNrOfLineItems());
            } else {                    // Cariage return
              data.setScroll(data.getScroll() + data.getNrOfLineItems());
            }
          }
          case '\u0008' -> {            // Backspace Delete
            data.setScroll(data.getScroll() - (data.getNrOfLines() - 1) * data.getNrOfLineItems());
          }
          case '\u007F' -> {
            if (e.isControlDown()) {  // Ctrl + Backspace or Ctrl + Delete
              data.setScroll(data.getScroll() - (data.getNrOfLines() - 1) * data.getNrOfLineItems());
            }
          }
          case 'R', 'r' -> {
            data.getContents().clear();
          }
          default -> {
          }
        }
      }
    }

    @Override
    public void keyPressed(InstanceState state, KeyEvent e) {
      final var data = (MemState) state.getData();
      switch (e.getKeyCode()) {
        case KeyEvent.VK_UP ->
          data.setScroll(data.getScroll() - data.getNrOfLineItems());
        case KeyEvent.VK_DOWN ->
          data.setScroll(data.getScroll() + data.getNrOfLineItems());
        case KeyEvent.VK_LEFT ->
          data.setScroll(data.getScroll() - data.getNrOfLineItems());
        case KeyEvent.VK_RIGHT ->
          data.setScroll(data.getScroll() + data.getNrOfLineItems());
        case KeyEvent.VK_PAGE_UP ->
          data.setScroll(data.getScroll() - (data.getNrOfLines() - 1) * data.getNrOfLineItems());
        case KeyEvent.VK_PAGE_DOWN ->
          data.setScroll(data.getScroll() + (data.getNrOfLines() - 1) * data.getNrOfLineItems());
        default -> {
        }
      }
      e.consume();
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
      final var val = Character.digit(e.getKeyChar(), 16);
      final var data = (MemState) state.getData();
      if (val >= 0) {
        curValue = curValue * 16 + val;
        data.getContents().set(data.getCursor(), curValue);
        state.fireInvalidated();
      } else {
        switch (e.getKeyChar()) {
          case ' ' -> {
            if (e.isControlDown()) { // Ctrl + space
              moveTo(data, data.getCursor() + (data.getNrOfLines() - 1) * data.getNrOfLineItems());
            } else {  // Space
              moveTo(data, data.getCursor() + 1);
            }
          }
          case '\n', '\r' -> {
            if (e.isControlDown()) {  // Ctrl + Carriage return
              moveTo(data, data.getCursor() - data.getNrOfLineItems());
            } else {  // Carriage return
              moveTo(data, data.getCursor() + data.getNrOfLineItems());
            }
          }
          case '\u0008' -> {  //  Backspace
            moveTo(data, data.getCursor() - 1);

          }
          case 'R', 'r' -> {
            data.getContents().clear();
          }
          case '\u007F' -> {
            if (e.isControlDown()) {  // Ctrl + Backspace or Ctrl + Delete
              moveTo(data, data.getCursor() - (data.getNrOfLines() - 1) * data.getNrOfLineItems());
            } else {  //  Delete
              data.getContents().set(data.getCursor(), 0);
            }
          }
          default -> {
          }
        }
      }
    }
 
    @Override
    public void keyPressed(InstanceState state, KeyEvent e) {
      final var data = (MemState) state.getData();
      switch (e.getKeyCode()) {
        case KeyEvent.VK_UP ->
          moveTo(data, data.getCursor() - data.getNrOfLineItems());
        case KeyEvent.VK_DOWN ->
          moveTo(data, data.getCursor() + data.getNrOfLineItems());
        case KeyEvent.VK_LEFT ->
          moveTo(data, data.getCursor() - 1);
        case KeyEvent.VK_RIGHT ->
          moveTo(data, data.getCursor() + 1);
        case KeyEvent.VK_PAGE_UP ->
          moveTo(data, data.getCursor() - (data.getNrOfLines() - 1) * data.getNrOfLineItems());
        case KeyEvent.VK_PAGE_DOWN ->
          moveTo(data, data.getCursor() + (data.getNrOfLines() - 1) * data.getNrOfLineItems());
        default -> {
        }
      }
      e.consume();
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
