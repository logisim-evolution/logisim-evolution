/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.chrono;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.log.Signal;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class RightPanel extends JPanel {

  private static final Font MSG_FONT = AppPreferences.getScaledFont(new Font("Serif", Font.ITALIC, 12));
  private static final Font TIME_FONT = new Font("Serif", Font.ITALIC, 9);
  private static final long serialVersionUID = 1L;
  private static final int WAVE_HEIGHT = ChronoPanel.SIGNAL_HEIGHT;
  private static final int EXTRA_SPACE = 40;
  private static final int CURSOR_GAP = 20;
  private static final int TIMELINE_SPACING = 80;
  private final ChronoPanel chronoPanel;
  final DefaultListSelectionModel selectionModel;
  private Model model;
  private final ArrayList<Waveform> rows = new ArrayList<>();
  private int curX = Integer.MAX_VALUE; // pixel coordinate of cursor, or MAX_VALUE to pin at right
  private long curT = Long.MAX_VALUE; // time of cursor, or MAX_VALUE to pin at right
  private int zoom = 20;
  private double tickWidth = 20.0; // display width of one time unit (timeScale simulated nanosecs)
  private final int slope; // display width of transitions, when duration of signal permits
  private long timeStartDraw = 0; // drawing started at this time, inclusive
  private long timeNextDraw = 0; // done drawing up to this time, exclusive
  private int width;
  private int height;
  private final MyListener myListener = new MyListener();
  private Timeline header;

  public RightPanel(ChronoPanel p, ListSelectionModel m) {
    chronoPanel = p;
    selectionModel = (DefaultListSelectionModel) m;
    model = p.getModel();
    slope = (tickWidth < 12) ? (int) (tickWidth / 3) : 4;
    configure();
  }

  private void configure() {
    final var n = model.getSignalCount();
    height = n * ChronoPanel.SIGNAL_HEIGHT;
    setBackground(Color.WHITE);
    final var timeScale = model.getTimeScale();
    final var numTicks = ((model.getEndTime() - model.getStartTime()) + timeScale - 1) / timeScale;
    width = (int) (tickWidth * numTicks + EXTRA_SPACE + 0.5);
    header = new Timeline();
    header.setPreferredSize(new Dimension(width, ChronoPanel.HEADER_HEIGHT));
    addMouseListener(myListener);
    addMouseMotionListener(myListener);
    final var tracker =
        new MouseAdapter() {
          void track(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) chronoPanel.setSignalCursorX(e.getX());
          }

          @Override
          public void mousePressed(MouseEvent e) {
            track(e);
          }

          @Override
          public void mouseDragged(MouseEvent e) {
            track(e);
          }
        };
    header.addMouseListener(tracker);
    header.addMouseMotionListener(tracker);
    updateSignals();
  }

  public void setModel(Model m) {
    model = m;
    setSignalCursorX(Integer.MAX_VALUE);
    updateSignals();
  }

  int indexOf(Signal s) {
    final var n = rows.size();
    for (var i = 0; i < n; i++) {
      final var waveForm = rows.get(i);
      if (waveForm.signal == s) return i;
    }
    return -1;
  }

  public void updateSignals() {
    final var n = model.getSignalCount();
    for (var i = 0; i < n; i++) {
      final var s = model.getSignal(i);
      final var idx = indexOf(s);
      if (idx < 0) {
        // new signal, add in correct position
        rows.add(i, new Waveform(s));
      } else if (idx != i) {
        // existing signal, move to correct position
        rows.add(i, rows.remove(idx));
      }
    }
    if (rows.size() > n) rows.subList(n, rows.size()).clear();
    updateWaveforms(true);
  }

  public void updateWaveforms(boolean force) {
    final var t0 = model.getStartTime();
    final var t1 = model.getEndTime();
    if (!force && t0 == timeStartDraw && t1 == timeNextDraw) {
      // already drawn all signal values
      return;
    }
    timeStartDraw = t0;
    timeNextDraw = t1;
    updateSize(true);
    flushWaveforms();
    header.repaint();
    repaint();
  }

  private void updateSize(boolean scrollRight) {
    final var timeScale = model.getTimeScale();
    final var numTicks = ((timeNextDraw - timeStartDraw) + timeScale - 1) / timeScale;
    final var m = model.getSignalCount();
    height = m * ChronoPanel.SIGNAL_HEIGHT;
    width = (int) (tickWidth * numTicks + EXTRA_SPACE + 0.5);
    final var d = getPreferredSize();
    if (d.width == width && d.height == height) return;
    final int oldWidth = d.width;
    final var v = chronoPanel.getRightViewport();
    final var sb = chronoPanel.getHorizontalScrollBar();
    final var oldR = v == null ? null : v.getViewRect();

    d.width = width;
    d.height = height;
    header.setPreferredSize(new Dimension(width, ChronoPanel.HEADER_HEIGHT));
    setPreferredSize(d); // necessary for scrollbar
    revalidate();

    if (!scrollRight || sb == null || v == null || sb.getValueIsAdjusting()) return;

    // if cursor is off screen, but right edge was on screen, scroll to max position
    // if cursor is on screen and right edge was on screen, scroll as far as possible while still
    // keeping cursor on screen
    // .....(.....|....... )
    // .....(........|.... )
    // ...(.|..........)..
    // (.|..........).....
    // (...|...     )
    // ^                   ^
    // never go below left=0 (0%) or above right=width-1 (100%)
    // try to not go above cursor-CURSOR_GAP

    final var r = v.getViewRect(); // has this updated yet?

    final var edgeVisible = (oldWidth <= oldR.x + oldR.width);
    final var cursorVisible = (oldR.x <= curX && curX <= oldR.x + oldR.width);
    if (cursorVisible && edgeVisible) {
      // cursor was on screen, keep it on screen
      r.x = Math.max(oldR.x, curX - CURSOR_GAP);
      r.width = Math.max(r.width, width - r.x);
      SwingUtilities.invokeLater(() -> scrollRectToVisible(r));
    } else if (edgeVisible) {
      // right edge was on screen, keep it on screen
      r.x = Math.max(0, width - r.width);
      r.width = width - r.x;
      SwingUtilities.invokeLater(() -> scrollRectToVisible(r));
    } else {
      // do nothing
    }
  }

  static long snapToPixel(long delta, long t) {
    // Each pixel covers delta=timeScale/tickWidth amount of time, and
    // rather than just truncating, we can try to find a good rounding.
    // e.g. t = 1234567, delta = 100
    // and the best point within range [1234567, 1234667) is 12345600
    var s = 1L;
    while (s < t && ((t + 10 * s - 1) / (10 * s)) * (10 * s) < t + delta) s *= 10;
    return ((t + s - 1) / s) * s;
  }

  public void setSignalCursorX(int posX) {
    final var f = model.getTimeScale() / tickWidth;
    curX = Math.max(0, posX);
    final var t0 = model.getStartTime();
    curT = Math.max(t0, snapToPixel((long) f, (long) (t0 + curX * f)));
    if (curT >= model.getEndTime()) {
      curX = Integer.MAX_VALUE; // pin to right side
      curT = Long.MAX_VALUE;
    }
    header.repaint();
    repaint();
  }

  public int getSignalCursorX() {
    final var timeScale = model.getTimeScale();
    return curX == Integer.MAX_VALUE
        ? (int) ((model.getEndTime() - model.getStartTime() - 1) * tickWidth / timeScale)
        : curX;
  }

  public long getCurrentTime() {
    return curT == Long.MAX_VALUE ? model.getEndTime() - 1 : curT;
  }

  public void changeSpotlight(Signal oldSignal, Signal newSignal) {
    if (oldSignal != null) {
      final var waveform = rows.get(oldSignal.idx);
      waveform.flush();
      repaint(waveform.getBounds());
    }
    if (newSignal != null) {
      final var waveform = rows.get(newSignal.idx);
      waveform.flush();
      repaint(waveform.getBounds());
    }
  }

  public void updateSelected(int firstIdx, int lastIdx) {
    for (var i = firstIdx; i <= lastIdx; i++) {
      final var waveform = rows.get(i);
      final var selected = selectionModel.isSelectedIndex(i);
      if (selected != waveform.selected) {
        waveform.selected = selected;
        waveform.flush();
        repaint(waveform.getBounds());
      }
    }
  }

  private void flushWaveforms() {
    for (final var w : rows) w.flush();
  }

  @Override
  public void paintComponent(Graphics graphics) {
    final var gfx = (Graphics2D) graphics;
    /* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
    gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    gfx.setColor(Color.WHITE);
    gfx.fillRect(0, 0, getWidth(), getHeight()); // entire viewport, not just (width, height)
    gfx.setColor(Color.BLACK);
    if (rows.isEmpty()) {
      final var f = gfx.getFont();
      gfx.setFont(MSG_FONT);
      final var lines = S.get("NoSignalsSelected");
      final var x = AppPreferences.getScaled(15);
      var y = AppPreferences.getScaled(15);
      for (final var s : lines.split("\\|")) {
        gfx.drawString(s.trim(), x, y);
        y += AppPreferences.getScaled(14);
      }
      gfx.setFont(f);
      return;
    }
    if (width > 32000) {
      gfx.setColor(Color.BLACK);
      gfx.setFont(MSG_FONT);
      gfx.drawString("Oops! Chronogram is too large to display.", 15, 15);
      gfx.drawString("Try zooming out, or reset the simulation.", 15, 29);
    } else {
      for (final var w : rows) w.paintWaveform(gfx);
      paintCursor(gfx);
    }
  }

  private void paintCursor(Graphics2D g) {
    final var x = getSignalCursorX();
    g.setColor(Color.RED);
    g.setStroke(new BasicStroke(1));
    g.drawLine(x, 0, x, getHeight());
  }

  private class MyListener extends MouseAdapter {
    boolean shiftDrag;
    boolean controlDrag;
    boolean subtracting;

    Signal getSignal(int y, boolean force) {
      var idx = y / WAVE_HEIGHT;
      final var n = model.getSignalCount();
      if (idx < 0 && force) idx = 0;
      else if (idx >= n && force) idx = n - 1;
      return (idx < 0 || idx >= n) ? null : model.getSignal(idx);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      chronoPanel.changeSpotlight(getSignal(e.getY(), false));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      chronoPanel.changeSpotlight(getSignal(e.getY(), false));
    }

    @Override
    public void mouseExited(MouseEvent e) {
      chronoPanel.changeSpotlight(null);
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (SwingUtilities.isLeftMouseButton(e)) {
        chronoPanel.setSignalCursorX(e.getX());
        final var signal = getSignal(e.getY(), false);
        if (signal == null) {
          shiftDrag = controlDrag = subtracting = false;
          return;
        }
        shiftDrag = e.isShiftDown();
        controlDrag = !shiftDrag && e.isControlDown();
        subtracting = controlDrag && selectionModel.isSelectedIndex(signal.idx);
        selectionModel.setValueIsAdjusting(true);
        if (shiftDrag) {
          if (selectionModel.getAnchorSelectionIndex() < 0)
            selectionModel.setAnchorSelectionIndex(0);
          selectionModel.setLeadSelectionIndex(signal.idx);
        } else if (controlDrag) {
          if (subtracting) selectionModel.removeSelectionInterval(signal.idx, signal.idx);
          else selectionModel.addSelectionInterval(signal.idx, signal.idx);
        } else {
          selectionModel.setSelectionInterval(signal.idx, signal.idx);
        }
      }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      chronoPanel.changeSpotlight(getSignal(e.getY(), false));
      if (SwingUtilities.isLeftMouseButton(e)) {
        chronoPanel.setSignalCursorX(e.getX());
        if (!selectionModel.getValueIsAdjusting()) return;
        final var signal = getSignal(e.getY(), false);
        if (signal == null) return;
        selectionModel.setLeadSelectionIndex(signal.idx);
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (SwingUtilities.isLeftMouseButton(e)) {
        if (!selectionModel.getValueIsAdjusting()) return;
        final var signal = getSignal(e.getY(), true);
        if (signal == null) return;
        var idx = selectionModel.getAnchorSelectionIndex();
        if (idx < 0) {
          idx = signal.idx;
          selectionModel.setAnchorSelectionIndex(signal.idx);
        }
        selectionModel.setLeadSelectionIndex(signal.idx);
        shiftDrag = controlDrag = subtracting = false;
        selectionModel.setValueIsAdjusting(false);
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (!SwingUtilities.isRightMouseButton(e)) return;
      final var signals = chronoPanel.getLeftPanel().getSelectedValuesList();
      if (signals.isEmpty()) {
        final var signal = getSignal(e.getY(), false);
        if (signal != null) signals.add(signal);
      }
      final var m = new PopupMenu(chronoPanel, signals);
      m.doPop(e);
    }
  }

  private class Waveform {

    private static final int HIGH = ChronoPanel.GAP;
    private static final int LOW = WAVE_HEIGHT - ChronoPanel.GAP;
    private static final int MID = WAVE_HEIGHT / 2;

    final Signal signal;
    private BufferedImage buf;
    boolean selected;

    public Waveform(Signal s) {
      this.signal = s;
    }

    Rectangle getBounds() {
      int y = WAVE_HEIGHT * signal.idx;
      return new Rectangle(0, y, width, WAVE_HEIGHT);
    }

    private void drawSignal(Graphics2D g, boolean bold, Color[] colors) {
      g.setStroke(new BasicStroke(bold ? 2 : 1));

      final var t0 = model.getStartTime();
      Signal.Iterator cur = signal.new Iterator(t0);

      final var fm = g.getFontMetrics();

      final var max = signal.getFormattedMaxValue();
      final var min = signal.getFormattedMinValue();
      final var labelWidth = Math.max(fm.stringWidth(max), fm.stringWidth(min));

      final var z = tickWidth / model.getTimeScale();
      var prevHi = false;
      var prevLo = false;
      Color prevFill = null;
      while (cur.value != null) {
        final var v = cur.getFormattedValue();
        final var x0 = (int) (z * (cur.time - t0));
        final var x1 = (int) (z * (cur.time + cur.duration - t0));

        var hi = true;
        var lo = true;
        Color lineColor;
        Color fillColor;

        if (v.contains("E")) {
          fillColor = colors[3];
          lineColor = colors[4];
        } else if (v.contains("x")) {
          fillColor = colors[5];
          lineColor = colors[6];
        } else if (v.equals(min)) {
          hi = false;
          fillColor = colors[1];
          lineColor = colors[2];
        } else if (v.equals(max)) {
          lo = false;
          fillColor = colors[1];
          lineColor = colors[2];
        } else {
          fillColor = colors[1];
          lineColor = colors[2];
        }
        // __________       _____ __________       ______
        //     \_____\_____/_____X_____/    \_____/
        //    |     |     |     |     |    |     |

        if (prevFill != null) {
          // draw left transition
          final var xt = x0 + Math.min(slope, (x1 - x0) / 2);
          // if (xt <= x0 + 3)
          //   xt = x0;
          if (xt == x0) {
            // not enough room for sloped transition
            if (hi) {
              g.setColor(fillColor);
              g.fillRect(x0, HIGH, (x1 - x0) + 1, LOW - HIGH + 1);
            }
            g.setColor(lineColor);
            g.drawLine(x0, HIGH, x0, LOW);
            if (hi) g.drawLine(x0, HIGH, x1, HIGH);
            if (lo) g.drawLine(x0, LOW, x1, LOW);
          } else {
            // draw sloped transition
            if (prevHi && prevLo && hi && lo) {
              //   ____ _____
              //   ____X_____
              //
              g.setColor(prevFill);
              g.fillPolygon(new int[] {x0, x0 + (xt - x0) / 2, x0}, new int[] {HIGH, MID, LOW + 1}, 3);
              g.setColor(fillColor);
              g.fillPolygon(
                  new int[] {x0 + (xt - x0) / 2, xt, x1, x1, xt},
                  new int[] {MID, HIGH, HIGH, LOW + 1, LOW + 1},
                  5);
              g.setColor(lineColor);
              g.drawLine(x0, HIGH, xt, LOW);
              g.drawLine(x0, LOW, xt, HIGH);
              g.drawLine(xt, HIGH, x1, HIGH);
              g.drawLine(xt, LOW, x1, LOW);
            } else if (!hi) {
              //   _____
              //   _ _ _\_____
              //
              g.setColor(prevFill);
              g.fillPolygon(new int[] {x0, xt, x0}, new int[] {HIGH, LOW + 1, LOW + 1}, 3);
              g.setColor(lineColor);
              g.drawLine(x0, HIGH, xt, LOW);
              g.drawLine(prevLo ? x0 : xt, LOW, x1, LOW);
            } else if (!lo) {
              //   _ _ _ _____
              //   _____/
              //
              if (prevHi) {
                g.setColor(prevFill);
                g.fillPolygon(new int[] {x0, xt, x0}, new int[] {HIGH, HIGH, LOW + 1}, 3);
              }
              g.setColor(fillColor);
              g.fillPolygon(new int[] {x0, xt, x1, x1, x0}, new int[] {LOW + 1, HIGH, HIGH, LOW + 1}, 4);
              g.setColor(lineColor);
              g.drawLine(x0, LOW, xt, HIGH);
              g.drawLine(prevHi ? x0 : xt, HIGH, x1, HIGH);
            } else if (!prevHi) {
              //         _____
              //   _____/_____
              //
              g.setColor(fillColor);
              g.fillPolygon(new int[] {x0, xt, x1, x1, x0}, new int[] {LOW + 1, HIGH, HIGH, LOW + 1}, 4);
              g.setColor(lineColor);
              g.drawLine(x0, LOW, x1, LOW);
              g.drawLine(x0, LOW, xt, HIGH);
              g.drawLine(xt, HIGH, x1, HIGH);
            } else if (!prevLo) {
              //   ___________
              //        \_____
              //
              g.setColor(prevFill);
              g.fillPolygon(new int[] {x0, xt, x0}, new int[] {HIGH, LOW + 1, LOW + 1}, 3);
              g.setColor(fillColor);
              g.fillPolygon(new int[] {x0, x1, x1, xt}, new int[] {HIGH, HIGH, LOW + 1, LOW + 1}, 4);
              g.setColor(lineColor);
              g.drawLine(x0, HIGH, x1, HIGH);
              g.drawLine(x0, HIGH, xt, LOW);
              g.drawLine(xt, LOW, x1, LOW);
            } else {
              System.out.println("huh? Unknown (and unhandled) case occured!");
            }
          }
        } else {
          // first point, no left transition
          if (hi) {
            g.setColor(fillColor);
            g.fillRect(x0, HIGH, (x1 - x0) + 1, LOW - HIGH + 1);
            g.setColor(lineColor);
            g.drawLine(x0, HIGH, x1, HIGH);
          }
          if (lo) {
            g.setColor(lineColor);
            g.drawLine(x0, LOW, x1, LOW);
          }
        }
        if (x1 - x0 > labelWidth) {
          g.setColor(Color.BLACK);
          g.drawString(v, x0 + 6, MID + 5);
        }

        prevHi = hi;
        prevLo = lo;
        prevFill = fillColor;
        if (!cur.advance()) break;
      }
    }

    private void createOffscreen() {
      buf = (BufferedImage) createImage(width, WAVE_HEIGHT);
      final var g = buf.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final var isBold = (model.getSpotlight() == signal);
      final var colors = chronoPanel.rowColors(signal.info, selected);
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, width, ChronoPanel.GAP - 1);
      g.fillRect(0, LOW, width, ChronoPanel.GAP - 1);
      g.setColor(colors[0]);
      g.fillRect(0, HIGH, width, LOW - HIGH);
      g.setColor(Color.BLACK);
      drawSignal(g, isBold, colors);
      g.dispose();
    }

    public void paintWaveform(Graphics2D g) {
      if (buf == null) {
        // todo: reallocating image each time seems silly
        createOffscreen();
      }
      final var y = WAVE_HEIGHT * signal.idx;
      g.drawImage(buf, null, 0, y);
    }

    public void flush() {
      buf = null;
    }
  }

  public void zoom(int sens, int posX) {
    if (zoom + sens < 1 || zoom + sens > 40) return;

    // fixme: Offscreen image can be max 32k pixels wide. We should not be
    // making such huge offscreen images anyway.
    final var timeScale = model.getTimeScale();
    final var t0 = model.getStartTime();
    final var t1 = model.getEndTime();
    final var numTicks = (t1 - t0 + timeScale - 1) / timeScale;
    final var newTickWidth = 20 * Math.pow(1.15, zoom + sens - 20);
    final var newWidth = (int) (newTickWidth * numTicks + EXTRA_SPACE + 0.5);
    if (newWidth > 32000) return;
    final var f = timeScale / tickWidth;
    final var mouseT = t0 + posX * f;
    final var sb = chronoPanel.getHorizontalScrollBar();
    final var vx = posX - sb.getValue();

    // adjust pixel scale
    zoom += sens;
    tickWidth = 20 * Math.pow(1.15, zoom - 20);

    // adjust cursor pixel coordinate to match cursor time
    final var q = tickWidth / timeScale;
    if (curX != Integer.MAX_VALUE) curX = (int) Math.max(0, (curT - t0) * q);

    updateSize(false); // don't scroll right

    // try to put time t back to being at coordinate vx in our view,
    // but do it after the revalidation happens
    SwingUtilities.invokeLater(
        () -> {
          int x = Math.max(0, (int) ((mouseT - t0) * q));
          int scrollPos = Math.min(sb.getMaximum(), Math.max(sb.getMinimum(), x - vx));
          sb.setValue(scrollPos);
        });

    // repaint
    flushWaveforms();
    header.repaint();
    repaint();
  }

  static final long[] unit = new long[] {10, 20, 25, 50};
  static final long[] subd = new long[] {4, 4, 5, 5};

  private class Timeline extends JPanel {
    final Color borderColor = UIManager.getDefaults().getColor("InternalFrame.borderDarkShadow");
    final int height = ChronoPanel.HEADER_HEIGHT;

    @Override
    public void paintComponent(Graphics gr) {
      final var g = (Graphics2D) gr;
      /* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(getBackground());
      g.fillRect(0, 0, getWidth() - 1, height - 1);
      g.setColor(borderColor);
      g.drawLine(0, height - 1, getWidth() - 1, height - 1);
      paintScale(g);
      paintCursorWithLabel(g);
    }

    void paintCursorWithLabel(Graphics2D g) {
      final var x = getSignalCursorX();
      final var t = getCurrentTime();
      g.setFont(TIME_FONT);

      final var s = Model.formatDuration(t);
      final var fm = g.getFontMetrics();
      final var r = fm.getStringBounds(s, g);
      g.setColor(Color.YELLOW);
      final var y = (height - ChronoPanel.GAP) / 2;
      g.fillRect(
          x + 2 + (int) r.getX() - 1,
          y + (int) r.getY() - 1,
          (int) r.getWidth() + 2,
          (int) r.getHeight() + 2);
      g.setColor(Color.RED);
      g.drawString(s, x + 2, y);
      g.setStroke(new BasicStroke(1));
      g.drawLine(x, 0, x, height);
    }

    void paintScale(Graphics2D g) {
      final var timeScale = model.getTimeScale();
      final var pixelPerTime = tickWidth / timeScale;

      // Pick the smallest unit among:
      //   10,  20,  25,  50
      //   100, 200, 250, 500
      //   etc., such that labels are at least TIMELINE_SPACING pixels apart.
      // todo: in clock and step mode, maybe use timeScale as unit?
      var b = 1L;
      var j = 0;
      while ((int) (unit[j] * b * pixelPerTime) < TIMELINE_SPACING) {
        if (++j >= unit.length) {
          b *= 10;
          j = 0;
        }
      }
      long divMajor = unit[j] * b;
      long numMinor = subd[j];
      long divMinor = divMajor / numMinor;

      final var f = g.getFont();
      g.setFont(TIME_FONT);
      final var time0 = model.getStartTime();
      final var timeL = (time0 / divMajor) * divMajor;
      final var h = ChronoPanel.HEADER_HEIGHT - ChronoPanel.GAP;
      g.setColor(Color.BLACK);
      g.drawLine(0, height - 2, width, height - 2);
      for (var i = 0; true; i++) {
        final var t = timeL + divMinor * i;
        if (t < time0) continue;
        final var x = (int) ((t - time0) * pixelPerTime);
        if (x >= width) break;
        if (i % numMinor == 0) {
          if (x + EXTRA_SPACE <= width) {
            g.drawString(Model.formatDuration(t), x, h / 2);
          }
          g.drawLine(x, h * 2 / 3, x, h);
        } else {
          g.drawLine(x, h - 2, x, h);
        }
      }
      g.setFont(f);
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    final var jsp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
    if (jsp != null && header != null) jsp.setColumnHeaderView(header);
  }

  @Override
  public void removeNotify() {
    super.addNotify();
    final var jsp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
    if (jsp != null) jsp.setColumnHeaderView(null);
  }

  public JPanel getTimelineHeader() {
    return header;
  }
}
