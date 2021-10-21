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
import lombok.Getter;
import lombok.Setter;

public class IoComponentsInformation {

  @Getter @Setter private Frame parentFrame;
  @Getter private final List<FpgaIoInformationContainer> components;
  @Getter @Setter private int defaultStandard = 0;
  @Getter @Setter private int defaultDriveStrength = 0;
  @Getter @Setter private int defaultPullSelection = 0;
  @Getter @Setter private int defaultActivity = 1;
  private final boolean mapMode;
  private final int imageHeight;
  private final FpgaIoInformationContainer[][] lookup;
  @Getter private FpgaIoInformationContainer highlighted;
  private ArrayList<IoComponentsListener> listeners;

  public IoComponentsInformation(Frame parentFrame, boolean mapMode) {
    this.parentFrame = parentFrame;
    imageHeight =
        mapMode
            ? BoardManipulator.IMAGE_HEIGHT + BoardManipulator.CONSTANT_BAR_HEIGHT
            : BoardManipulator.IMAGE_HEIGHT;
    components = new ArrayList<>();
    lookup = new FpgaIoInformationContainer[BoardManipulator.IMAGE_WIDTH][imageHeight];
    this.mapMode = mapMode;
    clear();
  }

  public void clear() {
    components.clear();
    for (var x = 0; x < BoardManipulator.IMAGE_WIDTH; x++)
      for (var y = 0; y < imageHeight; y++) lookup[x][y] = null;
    highlighted = null;
  }

  public boolean hasOverlap(BoardRectangle rect) {
    var overlap = false;
    for (var io : components)
      overlap |= io.getRectangle().overlap(rect);
    return overlap;
  }

  public boolean hasOverlap(BoardRectangle orig, BoardRectangle update) {
    var overlap = false;
    for (var io : components)
      if (!io.getRectangle().equals(orig)) overlap |= io.getRectangle().overlap(update);
    return overlap;
  }

  public boolean hasComponents() {
    return !components.isEmpty();
  }

  public boolean hasHighlighted() {
    return highlighted != null;
  }

  public boolean tryMap(JPanel parent) {
    if (!mapMode) return false;
    if (highlighted != null) return highlighted.tryMap(parent);
    return false;
  }

  public void setSelectable(MapListModel.MapInfo comp, float scale) {
    for (var io : components) {
      if (io.setSelectable(comp)) this.fireRedraw(io.getRectangle(), scale);
    }
  }

  public void removeSelectable(float scale) {
    for (var compId = 0; compId < components.size(); compId++) {
      final var io = components.get(compId);
      if (io.removeSelectable()) this.fireRedraw(io.getRectangle(), scale);
    }
  }

  public void addComponent(FpgaIoInformationContainer comp, float scale) {
    if (!components.contains(comp)) {
      components.add(comp);
      var rect = comp.getRectangle();
      for (var x = rect.getPositionX(); x < rect.getPositionX() + rect.getWidth(); x++)
        for (var y = rect.getPositionY(); y < rect.getPositionY() + rect.getHeight(); y++)
          if (x < BoardManipulator.IMAGE_WIDTH && y < imageHeight) lookup[x][y] = comp;
      if (mapMode) return;
      fireRedraw(comp.getRectangle(), scale);
    }
  }

  public void removeComponent(FpgaIoInformationContainer comp, float scale) {
    if (components.contains(comp)) {
      if (highlighted == comp) highlighted = null;
      components.remove(comp);
      var rect = comp.getRectangle();
      for (var x = rect.getPositionX(); x < rect.getPositionX() + rect.getWidth(); x++)
        for (var y = rect.getPositionY(); y < rect.getPositionY() + rect.getHeight(); y++)
          lookup[x][y] = null;
      fireRedraw(comp.getRectangle(), scale);
    }
  }

  public void replaceComponent(FpgaIoInformationContainer oldI, FpgaIoInformationContainer newI,
                               MouseEvent e, float scale) {
    if (!components.contains(oldI)) return;
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
    if (listeners != null)
      listeners.remove(l);
  }

  public void paint(Graphics2D g, float scale) {
    for (var comp : components)
      comp.paint(g, scale);
  }

  private void fireRedraw(BoardRectangle rect, float scale) {
    if (listeners == null) return;
    var area = new Rectangle(
        AppPreferences.getScaled(rect.getPositionX() - 2, scale),
        AppPreferences.getScaled(rect.getPositionY() - 2, scale),
        AppPreferences.getScaled(rect.getWidth() + 4, scale),
        AppPreferences.getScaled(rect.getHeight() + 4, scale));
    for (var l : listeners) l.repaintRequest(area);
  }
}
