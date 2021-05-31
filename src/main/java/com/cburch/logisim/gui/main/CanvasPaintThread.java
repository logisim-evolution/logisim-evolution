/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
