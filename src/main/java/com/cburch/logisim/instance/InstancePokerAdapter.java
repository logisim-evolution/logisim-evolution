/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.tools.AbstractCaret;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.Pokable;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InstancePokerAdapter extends AbstractCaret implements Pokable {

  static final Logger logger = LoggerFactory.getLogger(InstancePokerAdapter.class);

  private InstanceComponent comp;
  private Canvas canvas;
  private InstancePoker poker;
  private InstanceStateImpl state;
  private ComponentDrawContext context;

  public InstancePokerAdapter(InstanceComponent comp, Class<? extends InstancePoker> pokerClass) {
    try {
      this.comp = comp;
      poker = pokerClass.getDeclaredConstructor().newInstance();
    } catch (Exception t) {
      handleError(t, pokerClass);
      poker = null;
    }
  }

  private void checkCurrent() {
    if (state != null && canvas != null) {
      CircuitState s0 = state.getCircuitState();
      CircuitState s1 = canvas.getCircuitState();
      if (s0 != s1) {
        state = new InstanceStateImpl(s1, comp);
      }
    }
  }

  @Override
  public void draw(Graphics g) {
    if (poker != null) {
      context.setGraphics(g);
      InstancePainter painter = new InstancePainter(context, comp);
      poker.paint(painter);
    }
  }

  @Override
  public Bounds getBounds(Graphics gfx) {
    if (poker != null) {
      context.setGraphics(gfx);
      InstancePainter painter = new InstancePainter(context, comp);
      return poker.getBounds(painter);
    } else {
      return Bounds.EMPTY_BOUNDS;
    }
  }

  @Override
  public Caret getPokeCaret(ComponentUserEvent event) {
    if (poker == null) {
      return null;
    } else {
      canvas = event.getCanvas();
      CircuitState circState = event.getCircuitState();
      InstanceStateImpl state = new InstanceStateImpl(circState, comp);
      MouseEvent e =
          new MouseEvent(
              event.getCanvas(),
              MouseEvent.MOUSE_PRESSED,
              System.currentTimeMillis(),
              0,
              event.getX(),
              event.getY(),
              1,
              false);
      boolean isAccepted = poker.init(state, e);
      if (isAccepted) {
        this.state = state;
        this.context =
            new ComponentDrawContext(
                event.getCanvas(), event.getCanvas().getCircuit(), circState, null, null);
        mousePressed(e);
        return this;
      } else {
        poker = null;
        return null;
      }
    }
  }

  private void handleError(Throwable t, Class<? extends InstancePoker> pokerClass) {
    String className = pokerClass.getName();
    logger.error("Error while instantiating poker {}: {}", className, t.getClass().getName());
    String msg = t.getMessage();
    if (msg != null) logger.error("  ({})", msg);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (poker != null) {
      poker.keyPressed(state, e);
      checkCurrent();
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (poker != null) {
      poker.keyReleased(state, e);
      checkCurrent();
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {
    if (poker != null) {
      poker.keyTyped(state, e);
      checkCurrent();
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (poker != null) {
      poker.mouseDragged(state, e);
      checkCurrent();
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (poker != null) {
      poker.mousePressed(state, e);
      checkCurrent();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (poker != null) {
      poker.mouseReleased(state, e);
      checkCurrent();
    }
  }

  @Override
  public void stopEditing() {
    if (poker != null) {
      poker.stopEditing(state);
      checkCurrent();
    }
  }
}
