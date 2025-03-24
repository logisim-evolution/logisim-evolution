/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


// This class forms a bridge between the simulation threads, which recompute
// circuit values and update circuit state, and the AWT thread, which redraws
// the screen.
//  (1) The sim thread calls requestRepaint() whenever the circuit has state
//      been updated and needs to be redrawn. This may happen as frequently as
//      every few milliseconds or faster, or as slowly as once per few seconds,
//      depending on the user's chosen tick frequency.
//  (2) CanvasPaintCoordinator keeps track of those requests, and periodically
//      invokes canvas.repaint(), which enqueues work on the AWT thread. The
//      repaint() calls are metered so they occur at most once per approx 50
//      milliseconds (so about 20 redraws per second), and so there is never more than
//      one repaint() outstanding at a time. We use a
//  (3) The ATW thread services the repaint() requests performs the drawing. It
//      also invokes repaintCompleted() as a callback to notify
//      CanvasPaintCoordinator that the repaining is finished, so that another
//      repaint() can be issued, if and when needed.

class CanvasPaintCoordinator {

  // We use a variety of times around 50ms to avoid common factors
  // with the auto-tick frequencies.
  private static final int[] REPAINT_TIMESPANS =
      new int[] { 47, 53, 49, 51, 50, 47, 53, 50, 48, 52 };
  private int repaintTimespanIdx = 0;

  private Canvas canvas;

  private volatile long tDirtied; // timestamp at which canvas was last dirtied
  private volatile long tCleaned; // timestamp at which last canvas cleaning started
  private volatile long sDirtied; // sequence number updated when canvas was last dirtied
  private volatile long sCleaned; // sequence number at which last canvas cleaning started
  private volatile boolean cleaning; // repaint is curently scheduled or in progress

  private Timer timer;
  private Object lock;

  public CanvasPaintCoordinator(Canvas canvas) {
    this.canvas = canvas;
    lock = new Object();
    tCleaned = tDirtied = System.currentTimeMillis();
    sDirtied = sCleaned = 0;
    cleaning = false;
    timer = new Timer(1, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        synchronized (lock) {
          sCleaned = sDirtied;
          tCleaned = tDirtied;
        }
        canvas.repaint();
      }
    });
    timer.setRepeats(false);
  }

  public void requestRepaint() {
    long now = System.currentTimeMillis();
    boolean repaintNow = false;
    long repaintSoon = 0;
    synchronized (lock) {
      sDirtied++;
      tDirtied = now;
      long ago = now - tCleaned;
      int repaint_timespan = REPAINT_TIMESPANS[repaintTimespanIdx];
      repaintTimespanIdx = (repaintTimespanIdx + 1) % REPAINT_TIMESPANS.length;
      if (!cleaning && ago >= repaint_timespan) {
        // it's been a while, so repaint immediately
        cleaning = true;
        sCleaned = sDirtied;
        tCleaned = tDirtied;
        repaintNow = true;
      } else if (!cleaning) {
        // we repainted too recently, so repaint in a little while
        cleaning = true;
        repaintSoon = repaint_timespan - ago;
      }
    }
    if (repaintNow) {
      canvas.repaint();
    } else if (repaintSoon > 0) {
      timer.setInitialDelay((int) repaintSoon);
      timer.start();
    }
  }

  public void repaintCompleted() {
    long now = System.currentTimeMillis();
    boolean repaintNow = false;
    long repaintSoon = 0;
    synchronized (lock) {
      cleaning = false;
      long ago = now - tCleaned;
      int repaintTimespan = REPAINT_TIMESPANS[repaintTimespanIdx];
      repaintTimespanIdx = (repaintTimespanIdx + 1) % REPAINT_TIMESPANS.length;
      if (sCleaned < sDirtied && ago >= repaintTimespan) {
        // it's been a while, so repaint immediately
        cleaning = true;
        sCleaned = sDirtied;
        tCleaned = tDirtied;
        repaintNow = true;
      } else if (sCleaned < sDirtied) {
        // we repainted too recently, so repaint in a little while
        cleaning = true;
        repaintSoon = repaintTimespan - ago;
      }
    }
    if (repaintNow) {
      canvas.repaint();
    } else if (repaintSoon > 0) {
      timer.setInitialDelay((int) repaintSoon);
      timer.start();
    }
  }

}
