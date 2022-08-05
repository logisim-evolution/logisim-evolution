/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import com.cburch.logisim.fpga.gui.BoardManipulator;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class IoComponentsInformation {

  private Frame parent;
  private final ArrayList<FpgaIoInformationContainer> ioComps;
  private int defaultStandard = 0;
  private int defaultDriveStrength = 0;
  private int defaultPullSelection = 0;
  private int defaultActivity = 1;
  private final boolean mapMode;
  private final int imageHeight;
  private final FpgaIoInformationContainer[][] lookup;
  private FpgaIoInformationContainer highlighted;
  private ArrayList<IoComponentsListener> listeners;

  public IoComponentsInformation(Frame parentFrame, boolean mapMode) {
    parent = parentFrame;
    imageHeight =
        mapMode
            ? BoardManipulator.IMAGE_HEIGHT + BoardManipulator.CONSTANT_BAR_HEIGHT
            : BoardManipulator.IMAGE_HEIGHT;
    ioComps = new ArrayList<>();
    lookup = new FpgaIoInformationContainer[BoardManipulator.IMAGE_WIDTH][imageHeight];
    this.mapMode = mapMode;
    clear();
  }

  public void clear() {
    ioComps.clear();
    for (var x = 0; x < BoardManipulator.IMAGE_WIDTH; x++)
      for (var y = 0; y < imageHeight; y++) lookup[x][y] = null;
    highlighted = null;
  }

  public Frame getParentFrame() {
    return parent;
  }

  public void setParentFrame(Frame parent) {
    this.parent = parent;
  }

  public boolean hasOverlap(BoardRectangle rect) {
    var overlap = false;
    for (var io : ioComps) overlap |= io.getRectangle().overlap(rect);
    return overlap;
  }

  public boolean hasOverlap(BoardRectangle orig, BoardRectangle update) {
    var overlap = false;
    for (var io : ioComps)
      if (!io.getRectangle().equals(orig)) overlap |= io.getRectangle().overlap(update);
    return overlap;
  }

  public boolean hasComponents() {
    return !ioComps.isEmpty();
  }

  public List<FpgaIoInformationContainer> getComponents() {
    return ioComps;
  }

  public boolean hasHighlighted() {
    return highlighted != null;
  }

  public FpgaIoInformationContainer getHighligted() {
    return highlighted;
  }

  public boolean tryMap(JPanel parent) {
    if (!mapMode) return false;
    if (highlighted != null) return highlighted.tryMap(parent);
    return false;
  }

  public void setSelectable(MapListModel.MapInfo comp, float scale) {
    for (var io : ioComps) {
      if (io.setSelectable(comp)) this.fireRedraw(io.getRectangle(), scale);
    }
  }

  public void removeSelectable(float scale) {
    for (final FpgaIoInformationContainer io : ioComps) {
      if (io.removeSelectable()) this.fireRedraw(io.getRectangle(), scale);
    }
  }

  public void addComponent(FpgaIoInformationContainer comp, float scale) {
    if (!ioComps.contains(comp)) {
      ioComps.add(comp);
      var rect = comp.getRectangle();
      for (var x = rect.getXpos(); x < rect.getXpos() + rect.getWidth(); x++)
        for (var y = rect.getYpos(); y < rect.getYpos() + rect.getHeight(); y++)
          if (x < BoardManipulator.IMAGE_WIDTH && y < imageHeight) lookup[x][y] = comp;
      if (mapMode) return;
      fireRedraw(comp.getRectangle(), scale);
    }
  }

  public void removeComponent(FpgaIoInformationContainer comp, float scale) {
    if (ioComps.contains(comp)) {
      if (highlighted == comp) highlighted = null;
      ioComps.remove(comp);
      var rect = comp.getRectangle();
      for (var x = rect.getXpos(); x < rect.getXpos() + rect.getWidth(); x++)
        for (var y = rect.getYpos(); y < rect.getYpos() + rect.getHeight(); y++)
          lookup[x][y] = null;
      fireRedraw(comp.getRectangle(), scale);
    }
  }

  public void replaceComponent(
      FpgaIoInformationContainer oldI, FpgaIoInformationContainer newI, MouseEvent e, float scale) {
    if (!ioComps.contains(oldI)) return;
    removeComponent(oldI, scale);
    addComponent(newI, scale);
    mouseMoved(e, scale);
  }

  public void mouseMoved(MouseEvent e, float scale) {
    var xpos = AppPreferences.getDownScaled(e.getX(), scale);
    var ypos = AppPreferences.getDownScaled(e.getY(), scale);
    xpos = Math.max(xpos, 0);
    xpos = Math.min(xpos, BoardManipulator.IMAGE_WIDTH - 1);
    ypos = Math.max(ypos, 0);
    ypos = Math.min(ypos, imageHeight - 1);
    var selected = lookup[xpos][ypos];
    if (selected == highlighted) {
      if (highlighted != null && highlighted.selectedPinChanged(xpos, ypos))
        fireRedraw(highlighted.getRectangle(), scale);
      return;
    }
    if (highlighted != null) {
      highlighted.unsetHighlighted();
      fireRedraw(highlighted.getRectangle(), scale);
    }
    if (selected != null) {
      selected.setHighlighted();
      fireRedraw(selected.getRectangle(), scale);
    }
    highlighted = selected;
  }

  public void mouseExited(float scale) {
    if (highlighted != null) {
      highlighted.unsetHighlighted();
      fireRedraw(highlighted.getRectangle(), scale);
      highlighted = null;
    }
  }

  public void addListener(IoComponentsListener l) {
    if (listeners == null) {
      listeners = new ArrayList<>();
      listeners.add(l);
    } else if (!listeners.contains(l)) listeners.add(l);
  }

  public void removeListener(IoComponentsListener l) {
    if (listeners != null) listeners.remove(l);
  }

  public int getDefaultActivity() {
    return defaultActivity;
  }

  public int getDefaultDriveStrength() {
    return defaultDriveStrength;
  }

  public int getDefaultPullSelection() {
    return defaultPullSelection;
  }

  public int getDefaultStandard() {
    return defaultStandard;
  }

  public void setDefaultActivity(int value) {
    defaultActivity = value;
  }

  public void setDefaultDriveStrength(int value) {
    defaultDriveStrength = value;
  }

  public void setDefaultPullSelection(int value) {
    defaultPullSelection = value;
  }

  public void setDefaultStandard(int value) {
    defaultStandard = value;
  }

  public void paint(Graphics2D g, float scale) {
    for (var c : ioComps) c.paint(g, scale);
  }

  private void fireRedraw(BoardRectangle rect, float scale) {
    if (listeners == null) return;
    var area =
        new Rectangle(
            AppPreferences.getScaled(rect.getXpos() - 2, scale),
            AppPreferences.getScaled(rect.getYpos() - 2, scale),
            AppPreferences.getScaled(rect.getWidth() + 4, scale),
            AppPreferences.getScaled(rect.getHeight() + 4, scale));
    for (var l : listeners) l.repaintRequest(area);
  }
}
