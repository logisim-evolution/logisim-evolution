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

package com.cburch.logisim.gui.chrono;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class RightPanel extends JPanel {

  private static final long serialVersionUID = 1L;
  private static final int WAVE_HEIGHT = ChronoPanel.SIGNAL_HEIGHT;
  private static final int EXTRA_SPACE = 40;
  private ChronoPanel chronoPanel;
  DefaultListSelectionModel selectionModel;
  private ChronoData data;
  private ArrayList<Waveform> rows = new ArrayList<>();
  private int curX = Integer.MAX_VALUE; // pixel coordinate of cursor, or MAX_INT to pin at right
  private int curT = Integer.MAX_VALUE; // tick number of cursor, or MAX_INT to pin at right
  private int tickWidth = 20; // display width of one time unit 
  private int numTicks, slope;
  private int width, height;
  private MyListener myListener = new MyListener();
  
  public RightPanel(ChronoPanel p, ListSelectionModel m) {
    chronoPanel = p;
    selectionModel = (DefaultListSelectionModel)m;
    data = p.getChronoData();
    slope = (tickWidth < 12) ? tickWidth / 3 : 4;
    configure();
  }

  public RightPanel(RightPanel oldPanel, ListSelectionModel m) {
    try { throw new Exception(); }
    catch (Exception e) { e.printStackTrace(); }
    chronoPanel = oldPanel.chronoPanel;
    selectionModel = (DefaultListSelectionModel)m;
    tickWidth = oldPanel.tickWidth;
    slope = (tickWidth < 12) ? tickWidth / 3 : 4;
    curX = oldPanel.curX;
    curT = oldPanel.curT;
    configure();
  }

  private void configure() {
    int n = data.getSignalCount();
    height = ChronoPanel.HEADER_HEIGHT + n * ChronoPanel.SIGNAL_HEIGHT;
    setBackground(Color.WHITE);
    numTicks = data.getValueCount();
    width = tickWidth * numTicks + EXTRA_SPACE;
    addMouseListener(myListener);
    addMouseMotionListener(myListener);
    addMouseWheelListener(myListener);
    updateSignals();
  }

  
  int indexOf(ChronoData.Signal s) {
    int n = rows.size();
    for (int i = 0; i < n; i++) {
      Waveform w = rows.get(i);
      if (w.signal == s)
        return i;
    }
    return -1;
  }

  public void updateSignals() {
    int n = data.getSignalCount();
    for (int i = 0; i < n; i++) {
      ChronoData.Signal s = data.getSignal(i);
      int idx = indexOf(s);
      if (idx < 0) {
        // new signal, add in correct position
        rows.add(i, new Waveform(s));
      } else if (idx != i) {
        // existing signal, move to correct position
        rows.add(i, rows.remove(idx));
      }
    }
    if (rows.size() > n)
      rows.subList(n, rows.size()).clear();
    numTicks = -1; // forces updateWaveforms() to refresh waveforms
    updateWaveforms();
  }

  public void updateWaveforms() {
    int n = data.getValueCount();
    if (n == numTicks)
      return; // size has not changed

    numTicks = n;
    width = tickWidth * numTicks + EXTRA_SPACE; // todo: even clock spacing

    int m = data.getSignalCount();
    height = ChronoPanel.HEADER_HEIGHT + m * ChronoPanel.SIGNAL_HEIGHT;
    setPreferredSize(new Dimension(width, height)); // necessary for scrollbar
    flushWaveforms();
    repaint();
  }

  public void setSignalCursor(int posX) {
    if (posX >= width - EXTRA_SPACE - 2) {
      curX = Integer.MAX_VALUE; // pin to right side
      curT = Integer.MAX_VALUE; // pin to right side
    } else {
      curX = Math.max(0, posX);
      curT = Math.max(0, Math.min(numTicks-1, (curX - slope/2) / tickWidth));
    }
    repaint();
  }

  public int getSignalCursor() {
    return curX == Integer.MAX_VALUE ? tickWidth * numTicks : curX;
  }

  public int getCurrentTick() {
    return curT == Integer.MAX_VALUE ? numTicks-1 : curT;
  }

  public void changeSpotlight(ChronoData.Signal oldSignal, ChronoData.Signal newSignal) {
    if (oldSignal != null) {
      Waveform w = rows.get(oldSignal.idx);
      w.flush();
      repaint(w.getBounds());
    }
    if (newSignal != null) {
      Waveform w = rows.get(newSignal.idx);
      w.flush();
      repaint(w.getBounds());
    }
  }

  public void updateSelected(int firstIdx, int lastIdx) {
    for (int i = firstIdx; i <= lastIdx; i++) {
      Waveform w = rows.get(i);
      boolean selected = selectionModel.isSelectedIndex(i);
      if (selected != w.selected) {
        w.selected = selected;
        w.flush();
        repaint(w.getBounds());
      }
    }
  }

  public void flushWaveforms() {
    for (Waveform w : rows)
      w.flush();
  }

  @Override
  public void paintComponent(Graphics gr) {
    Graphics2D g = (Graphics2D)gr;
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, getWidth(), getHeight()); // entire viewport, not just (width, height)
    g.setColor(Color.BLACK);
    for (Waveform w : rows)
      w.paintWaveform(g);
    paintTimeline(g);
    paintCursor(g);
  }

  public void paintTimeline(Graphics2D g) {
    g.drawLine(0, 5, width, 5);
    for (int i = 0; i <= numTicks; i++)
      g.drawLine(i*tickWidth, 2, i*tickWidth, 8);
  }


 private void paintCursor(Graphics2D g) {
    int pos = getSignalCursor();
  g.setStroke(new BasicStroke(1));
  g.setPaint(Color.RED);
  g.drawLine(pos, getHeight(), pos, 0);
 }

 private class MyListener extends MouseAdapter {
    boolean shiftDrag, controlDrag, subtracting;

    ChronoData.Signal getSignal(int y, boolean force) {
      int idx = (y - ChronoPanel.HEADER_HEIGHT) / WAVE_HEIGHT;
      int n = data.getSignalCount();
      if (idx < 0 && force)
        idx = 0;
      else if (idx >= n && force)
        idx = n - 1;
      return (idx < 0 || idx >= n) ? null : data.getSignal(idx);
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
        chronoPanel.setSignalCursor(e.getX());
        ChronoData.Signal signal = getSignal(e.getY(), false);
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
          if (subtracting)
            selectionModel.removeSelectionInterval(signal.idx, signal.idx);
          else
            selectionModel.addSelectionInterval(signal.idx, signal.idx);
        } else {
          selectionModel.setSelectionInterval(signal.idx, signal.idx);
        }
      }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
      chronoPanel.changeSpotlight(getSignal(e.getY(), false));
      if (SwingUtilities.isLeftMouseButton(e)) {
        chronoPanel.setSignalCursor(e.getX());
        if (!selectionModel.getValueIsAdjusting())
          return;
        ChronoData.Signal signal = getSignal(e.getY(), false);
        if (signal == null)
          return;
        selectionModel.setLeadSelectionIndex(signal.idx);
      }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
      if (SwingUtilities.isLeftMouseButton(e)) {
        if (!selectionModel.getValueIsAdjusting())
          return;
        ChronoData.Signal signal = getSignal(e.getY(), true);
        if (signal == null)
          return;
        int idx = selectionModel.getAnchorSelectionIndex();
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
      if (SwingUtilities.isRightMouseButton(e)) {
        List<ChronoData.Signal> signals = chronoPanel.getLeftPanel().getSelectedValuesList();
        if (signals.size() == 0) {
          ChronoData.Signal signal = getSignal(e.getY(), false);
          if (signal == null)
            return;
          signals.add(signal);
          PopupMenu m = new PopupMenu(chronoPanel, signals);
          m.doPop(e);
        }
      }
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
      zoom(e.getWheelRotation() > 0 ? -1 : +1, e.getPoint().x);
  }
 }

  private class Waveform {

    private static final int HIGH = ChronoPanel.GAP;
    private static final int LOW = WAVE_HEIGHT - ChronoPanel.GAP;
    private static final int MID = WAVE_HEIGHT / 2;

    final ChronoData.Signal signal;
    private BufferedImage buf;
    boolean selected;

    public Waveform(ChronoData.Signal s) {
      this.signal = s;
    }

    Rectangle getBounds() {
      int y = ChronoPanel.HEADER_HEIGHT + WAVE_HEIGHT * signal.idx;
      return new Rectangle(0, y, width, WAVE_HEIGHT);
    }

    private void drawSignal(Graphics2D g, boolean bold, Color bg, Color fg) {
      g.setStroke(new BasicStroke(bold ? 2 : 1));

      int t = 0;

      String max = signal.getFormattedMaxValue();
      String min = signal.getFormattedMinValue();
      String prec = signal.getFormattedValue(t);

      int x = 0;
      while (t < numTicks) {
        String suiv = signal.getFormattedValue(t++);

        if (suiv.equals("-")) {
          x += tickWidth;
          continue;
        }

        if (suiv.contains("E")) {
          g.setColor(Color.red);
          g.drawLine(x, HIGH, x + tickWidth, MID);
          g.drawLine(x, MID, x + tickWidth, HIGH);
          g.drawLine(x, MID, x + tickWidth, LOW);
          g.drawLine(x, LOW, x + tickWidth, MID);
          g.setColor(Color.BLACK);
        } else if (suiv.contains("x")) {
          g.setColor(Color.blue);
          g.drawLine(x, HIGH, x + tickWidth, MID);
          g.drawLine(x, MID, x + tickWidth, HIGH);
          g.drawLine(x, MID, x + tickWidth, LOW);
          g.drawLine(x, LOW, x + tickWidth, MID);
          g.setColor(Color.BLACK);
        } else if (suiv.equals(min)) {
          if (!prec.equals(min)) {
            if (slope > 0) {
              g.setColor(fg);
              g.fillPolygon(
                  new int[] { x, x + slope, x },
                  new int[] { HIGH, LOW+1, LOW+1 },
                  3);
              g.setColor(Color.BLACK);
            }
            g.drawLine(x, HIGH, x + slope, LOW);
            g.drawLine(x + slope, LOW, x + tickWidth, LOW);
          } else {
            g.drawLine(x, LOW, x + tickWidth, LOW);
          }
        } else if (suiv.equals(max)) {
          if (!prec.equals(max)) {
            g.setColor(fg);
            g.fillPolygon(
                new int[] { x, x + slope, x + tickWidth + 1, x + tickWidth + 1},
                new int[] { LOW+1, HIGH, HIGH, LOW+1 },
                4);
            g.setColor(Color.BLACK);
            g.drawLine(x, LOW, x + slope, HIGH);
            g.drawLine(x + slope, HIGH, x + tickWidth, HIGH);
          } else {
            g.setColor(fg);
            g.fillRect(x, HIGH, tickWidth + 1, LOW - HIGH + 1);
            g.setColor(Color.BLACK);
            g.drawLine(x, HIGH, x + tickWidth, HIGH);
          }
        } else {
          if (suiv.equals(prec)) {
            g.drawLine(x, LOW, x + tickWidth, LOW);
            g.drawLine(x, HIGH, x + tickWidth, HIGH);
            if (t == 1) // first segment also gets a label
              g.drawString(suiv, x + 2, MID);
          } else {
            g.drawLine(x, LOW, x + slope, HIGH);
            g.drawLine(x, HIGH, x + slope, LOW);
            g.drawLine(x + slope, HIGH, x + tickWidth, HIGH);
            g.drawLine(x + slope, LOW, x + tickWidth, LOW);
            g.drawString(suiv, x + tickWidth, MID);
          }
        }

        prec = suiv;
        x += tickWidth;
      }
    }

    private void createOffscreen() {
      buf = (BufferedImage)createImage(width, WAVE_HEIGHT);
      Graphics2D g = buf.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_DEFAULT);
      boolean bold = data.getSpotlight() ==  signal;
      Color[] colors = chronoPanel.rowColors(signal.info, selected);
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, width, ChronoPanel.GAP-1);
      g.fillRect(0, LOW, width, ChronoPanel.GAP-1);
      g.setColor(colors[0]);
      g.fillRect(0, HIGH, width, LOW - HIGH);
      g.setColor(Color.BLACK);
      drawSignal(g, bold, colors[0], colors[1]);
      g.dispose();
    }

    public void paintWaveform(Graphics2D g) {
      if (buf == null) // todo: reallocating image each time seems silly
        createOffscreen();
      int y = ChronoPanel.HEADER_HEIGHT + WAVE_HEIGHT * signal.idx;
      g.drawImage(buf, null, 0, y);
    }

    public void flush() {
      buf = null;
    }

  }

  public void zoom(int sens, int posX) {
  }

  public void adjustmentValueChanged(int value) {
  } 
}
