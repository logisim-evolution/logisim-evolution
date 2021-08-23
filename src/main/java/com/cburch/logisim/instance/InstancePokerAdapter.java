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

package com.cburch.logisim.instance;

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
import lombok.val;
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
      val s0 = state.getCircuitState();
      val s1 = canvas.getCircuitState();
      if (s0 != s1) {
        state = new InstanceStateImpl(s1, comp);
      }
    }
  }

  @Override
  public void draw(Graphics g) {
    if (poker != null) {
      context.setGraphics(g);
      poker.paint(new InstancePainter(context, comp));
    }
  }

  @Override
  public Bounds getBounds(Graphics gfx) {
    if (poker != null) {
      context.setGraphics(gfx);
      return poker.getBounds(new InstancePainter(context, comp));
    }
    return Bounds.EMPTY_BOUNDS;
  }

  @Override
  public Caret getPokeCaret(ComponentUserEvent event) {
    if (poker == null) return null;

    canvas = event.getCanvas();
    val circState = event.getCircuitState();
    val state = new InstanceStateImpl(circState, comp);
    val e =
        new MouseEvent(
            event.getCanvas(),
            MouseEvent.MOUSE_PRESSED,
            System.currentTimeMillis(),
            0,
            event.getX(),
            event.getY(),
            1,
            false);
    val isAccepted = poker.init(state, e);
    if (isAccepted) {
      this.state = state;
      this.context =
          new ComponentDrawContext(
              event.getCanvas(), event.getCanvas().getCircuit(), circState, null, null);
      mousePressed(e);
      return this;
    }

    poker = null;
    return null;
  }

  private void handleError(Throwable t, Class<? extends InstancePoker> pokerClass) {
    val className = pokerClass.getName();
    logger.error("Error while instantiating poker {}: {}", className, t.getClass().getName());
    val msg = t.getMessage();
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
