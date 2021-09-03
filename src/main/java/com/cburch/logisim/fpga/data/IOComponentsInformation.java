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
import javax.swing.JPanel;

public class IOComponentsInformation {

  private Frame parent;
  private final ArrayList<FPGAIOInformationContainer> IOcomps;
  private int DefaultStandard = 0;
  private int DefaultDriveStrength = 0;
  private int DefaultPullSelection = 0;
  private int DefaultActivity = 1;
  private final boolean mapMode;
  private final int imageHeight;
  private final FPGAIOInformationContainer[][] lookup;
  private FPGAIOInformationContainer highlighted;
  private ArrayList<IOComponentsListener> listeners;

  public IOComponentsInformation(Frame parentFrame, boolean mapMode) {
    parent = parentFrame;
    imageHeight =
        mapMode
            ? BoardManipulator.IMAGE_HEIGHT + BoardManipulator.CONSTANT_BAR_HEIGHT
            : BoardManipulator.IMAGE_HEIGHT;
    IOcomps = new ArrayList<>();
    lookup = new FPGAIOInformationContainer[BoardManipulator.IMAGE_WIDTH][imageHeight];
    this.mapMode = mapMode;
    clear();
  }

  public void clear() {
    IOcomps.clear();
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
    for (var io : IOcomps)
      overlap |= io.GetRectangle().Overlap(rect);
    return overlap;
  }

  public boolean hasOverlap(BoardRectangle orig, BoardRectangle update) {
    var overlap = false;
    for (var io : IOcomps)
      if (!io.GetRectangle().equals(orig)) overlap |= io.GetRectangle().Overlap(update);
    return overlap;
  }

  public boolean hasComponents() {
    return !IOcomps.isEmpty();
  }

  public ArrayList<FPGAIOInformationContainer> getComponents() {
    return IOcomps;
  }

  public boolean hasHighlighted() {
    return highlighted != null;
  }

  public FPGAIOInformationContainer getHighligted() {
    return highlighted;
  }

  public boolean tryMap(JPanel parent) {
    if (!mapMode) return false;
    if (highlighted != null) return highlighted.tryMap(parent);
    return false;
  }

  public void setSelectable(MapListModel.MapInfo comp, float scale) {
    for (var io : IOcomps) {
      if (io.setSelectable(comp)) this.fireRedraw(io.GetRectangle(), scale);
    }
  }

  public void removeSelectable(float scale) {
    for (var io : IOcomps) {
      if (io.removeSelectable()) this.fireRedraw(io.GetRectangle(), scale);
    }
  }

  public void addComponent(FPGAIOInformationContainer comp, float scale) {
    if (!IOcomps.contains(comp)) {
      IOcomps.add(comp);
      var rect = comp.GetRectangle();
      for (var x = rect.getXpos(); x < rect.getXpos() + rect.getWidth(); x++)
        for (var y = rect.getYpos(); y < rect.getYpos() + rect.getHeight(); y++)
          if (x < BoardManipulator.IMAGE_WIDTH && y < imageHeight) lookup[x][y] = comp;
      if (mapMode) return;
      fireRedraw(comp.GetRectangle(), scale);
    }
  }

  public void removeComponent(FPGAIOInformationContainer comp, float scale) {
    if (IOcomps.contains(comp)) {
      if (highlighted == comp) highlighted = null;
      IOcomps.remove(comp);
      var rect = comp.GetRectangle();
      for (var x = rect.getXpos(); x < rect.getXpos() + rect.getWidth(); x++)
        for (var y = rect.getYpos(); y < rect.getYpos() + rect.getHeight(); y++)
          lookup[x][y] = null;
      fireRedraw(comp.GetRectangle(), scale);
    }
  }

  public void replaceComponent(FPGAIOInformationContainer oldI, FPGAIOInformationContainer newI,
      MouseEvent e, float scale) {
    if (!IOcomps.contains(oldI)) return;
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
        fireRedraw(highlighted.GetRectangle(), scale);
      return;
    }
    if (highlighted != null) {
      highlighted.unsetHighlighted();
      fireRedraw(highlighted.GetRectangle(), scale);
    }
    if (selected != null) {
      selected.setHighlighted();
      fireRedraw(selected.GetRectangle(), scale);
    }
    highlighted = selected;
  }

  public void mouseExited(float scale) {
    if (highlighted != null) {
      highlighted.unsetHighlighted();
      fireRedraw(highlighted.GetRectangle(), scale);
      highlighted = null;
    }
  }

  public void addListener(IOComponentsListener l) {
    if (listeners == null) {
      listeners = new ArrayList<>();
      listeners.add(l);
    } else if (!listeners.contains(l)) listeners.add(l);
  }

  public void removeListener(IOComponentsListener l) {
    if (listeners != null)
      listeners.remove(l);
  }

  public int GetDefaultActivity() {
    return DefaultActivity;
  }

  public int GetDefaultDriveStrength() {
    return DefaultDriveStrength;
  }

  public int GetDefaultPullSelection() {
    return DefaultPullSelection;
  }

  public int GetDefaultStandard() {
    return DefaultStandard;
  }

  public void SetDefaultActivity(int value) {
    DefaultActivity = value;
  }

  public void SetDefaultDriveStrength(int value) {
    DefaultDriveStrength = value;
  }

  public void SetDefaultPullSelection(int value) {
    DefaultPullSelection = value;
  }

  public void SetDefaultStandard(int value) {
    DefaultStandard = value;
  }

  public void paint(Graphics2D g, float scale) {
    for (var c : IOcomps)
      c.paint(g, scale);
  }

  private void fireRedraw(BoardRectangle rect, float scale) {
    if (listeners == null) return;
    var area = new Rectangle(
        AppPreferences.getScaled(rect.getXpos() - 2, scale),
        AppPreferences.getScaled(rect.getYpos() - 2, scale),
        AppPreferences.getScaled(rect.getWidth() + 4, scale),
        AppPreferences.getScaled(rect.getHeight() + 4, scale));
    for (var l : listeners) l.repaintRequest(area);
  }
}
