/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

public class SocBusInfo {
  private String busId;
  private SocSimulationManager socManager;
  private Component myComp;

  public SocBusInfo(String id) {
    busId = id;
    socManager = null;
  }

  public void setBusId(String value) {
    busId = value;
  }

  public String getBusId() {
    return busId;
  }

  public void setSocSimulationManager(SocSimulationManager man, Component comp) {
    socManager = man;
    myComp = comp;
  }

  public SocSimulationManager getSocSimulationManager() {
    return socManager;
  }

  public Component getComponent() {
    return myComp;
  }

  public void paint(Graphics g, Bounds b) {
    final var ident = socManager == null ? null : socManager.getSocBusDisplayString(busId);
    final var color = (ident == null) ? Color.RED : Color.GREEN;
    g.setColor(color);
    g.fillRect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
    g.setColor(Color.BLACK);
    GraphicsUtil.drawCenteredText(
        g, ident == null ? S.get("SocBusNotConnected") : ident, b.getCenterX(), b.getCenterY());
  }
}
