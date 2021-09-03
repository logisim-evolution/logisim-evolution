/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.util.UniquelyNamedThread;
import java.awt.Rectangle;

class CanvasPaintThread extends UniquelyNamedThread {
  private static final int REPAINT_TIMESPAN = 50; // 50 ms between repaints

  private final Canvas canvas;
  private final Object lock;
  private boolean repaintRequested;
  private long nextRepaint;
  private boolean alive;
  private Rectangle repaintRectangle;

  public CanvasPaintThread(Canvas canvas) {
    super("CanvasPaintThread");
    this.canvas = canvas;
    lock = new Object();
    repaintRequested = false;
    alive = true;
    nextRepaint = System.currentTimeMillis();
  }

  public void requentRepaint(Rectangle rect) {
    synchronized (lock) {
      if (repaintRequested) {
        if (repaintRectangle != null) {
          repaintRectangle.add(rect);
        }
      } else {
        repaintRequested = true;
        repaintRectangle = rect;
        lock.notifyAll();
      }
    }
  }

  public void requestRepaint() {
    synchronized (lock) {
      if (!repaintRequested) {
        repaintRequested = true;
        repaintRectangle = null;
        lock.notifyAll();
      }
    }
  }

  public void requestStop() {
    synchronized (lock) {
      alive = false;
      lock.notifyAll();
    }
  }

  @Override
  public void run() {
    while (alive) {
      long now = System.currentTimeMillis();
      synchronized (lock) {
        long wait = nextRepaint - now;
        while (alive && !(repaintRequested && wait <= 0)) {
          try {
            if (wait > 0) {
              lock.wait(wait);
            } else {
              lock.wait();
            }
          } catch (InterruptedException ignored) {
          }
          now = System.currentTimeMillis();
          wait = nextRepaint - now;
        }
        if (!alive) break;
        repaintRequested = false;
        nextRepaint = now + REPAINT_TIMESPAN;
      }
      canvas.repaint();
    }
  }
}
