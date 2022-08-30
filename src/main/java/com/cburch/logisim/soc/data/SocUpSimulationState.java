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

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;

public class SocUpSimulationState {
  public static final int SIMULATION_RUNNING = 0;
  public static final int SIMULATION_HALTED_BY_ERROR = 1;
  public static final int SIMULATION_HALTED_BY_BREAKPOINT = 2;
  public static final int SIMULATION_HALTED_BY_STOP = 3;

  private int simulationState = SIMULATION_RUNNING;
  private final ArrayList<SocUpSimulationStateListener> listeners = new ArrayList<>();
  private boolean canContinueAfterBreak = false;

  public String getStateString() {
    return switch (simulationState) {
      case SIMULATION_RUNNING -> S.get("SocUpSimRunning");
      case SIMULATION_HALTED_BY_ERROR -> S.get("SocUpSimHaltError");
      case SIMULATION_HALTED_BY_BREAKPOINT -> S.get("SocUpSimHaltBreak");
      case SIMULATION_HALTED_BY_STOP -> S.get("SocUpSimHalt");
      default -> S.get("SocUpUnknown");
    };
  }

  public void registerListener(SocUpSimulationStateListener l) {
    listeners.add(l);
  }

  public void reset() {
    canContinueAfterBreak = false;
    simulationState = SIMULATION_HALTED_BY_STOP;
    fireChange();
  }

  public boolean canExecute() {
    return simulationState == SIMULATION_RUNNING;
  }

  public void errorInExecution() {
    simulationState = SIMULATION_HALTED_BY_ERROR;
    fireChange();
  }

  public boolean breakPointReached() {
    if (canContinueAfterBreak) {
      canContinueAfterBreak = false;
      return false;
    }
    simulationState = SIMULATION_HALTED_BY_BREAKPOINT;
    fireChange();
    return true;
  }

  public void buttonPressed() {
    if (simulationState == SIMULATION_RUNNING)
      simulationState = SIMULATION_HALTED_BY_STOP;
    else {
      if (simulationState == SIMULATION_HALTED_BY_BREAKPOINT) canContinueAfterBreak = true;
      simulationState = SIMULATION_RUNNING;
    }
    fireChange();
  }

  public static Bounds getButtonLocation(int xoff, int yoff, Bounds b) {
    int width = b.getWidth() / 3;
    int xpos = xoff + b.getX() + 2 * width;
    return Bounds.create(xpos, yoff + b.getY(), width, b.getHeight());
  }

  public static Bounds getStateLocation(int xoff, int yoff, Bounds b) {
    int width = b.getWidth() / 3;
    int xpos = xoff + b.getX() + width;
    return Bounds.create(xpos, yoff + b.getY(), width, b.getHeight());
  }

  public static Bounds getLabelLocation(int xoff, int yoff, Bounds b) {
    int width = b.getWidth() / 3;
    int xpos = xoff + b.getX();
    return Bounds.create(xpos, yoff + b.getY(), width, b.getHeight());
  }

  private void paintState(Graphics g, int x, int y, Bounds b) {
    Bounds state = getStateLocation(x, y, b);
    g.setColor(Color.BLACK);
    g.drawRect(state.getX(), state.getY(), state.getWidth(), state.getHeight());
    switch (simulationState) {
      case SIMULATION_RUNNING -> g.setColor(Color.GREEN);
      case SIMULATION_HALTED_BY_ERROR -> g.setColor(Color.RED);
      case SIMULATION_HALTED_BY_BREAKPOINT -> g.setColor(Color.MAGENTA);
    }
    GraphicsUtil.drawCenteredText(g, getStateString(), state.getCenterX(), state.getCenterY());
  }

  public void paint(Graphics g, int x, int y, Bounds b) {
    Bounds button = getButtonLocation(x, y, b);
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(button.getX(), button.getY(), button.getWidth(), button.getHeight());
    g.setColor(Color.BLUE);
    Font f = g.getFont();
    g.setFont(StdAttr.DEFAULT_LABEL_FONT);
    String bname = simulationState == SIMULATION_RUNNING ? S.get("SocUpSimstateStop") : S.get("SocUpSimstateStart");
    GraphicsUtil.drawCenteredText(g, bname, button.getCenterX(), button.getCenterY());
    Bounds labloc = getLabelLocation(x, y, b);
    g.setColor(Color.black);
    GraphicsUtil.drawCenteredText(g, S.get("SocUpSimStateLabel"), labloc.getCenterX(), labloc.getCenterY());
    g.setFont(f);
    paintState(g, x, y, b);
  }

  private void fireChange() {
    for (SocUpSimulationStateListener l : listeners)
      l.simulationStateChanged();
  }
}
