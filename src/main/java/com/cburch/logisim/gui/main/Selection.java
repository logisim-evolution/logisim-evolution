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

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.CustomHandles;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import lombok.Getter;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Selection extends SelectionBase {

  static final Logger logger = LoggerFactory.getLogger(Selection.class);
  private final MyListener myListener;
  private final boolean isVisible = true;
  @Getter private final SelectionAttributes attributeSet;

  public Selection(Project proj, Canvas canvas) {
    super(proj);

    myListener = new MyListener();
    attributeSet = new SelectionAttributes(canvas, this);
    proj.addProjectListener(myListener);
    proj.addCircuitListener(myListener);
  }

  public boolean contains(Component comp) {
    return unionSet.contains(comp);
  }

  //
  // graphics methods
  //
  public void draw(ComponentDrawContext context, Set<Component> hidden) {
    val gfx = context.getGraphics();

    for (val c : lifted) {
      if (!hidden.contains(c)) {
        val loc = c.getLocation();

        val gfxNew = gfx.create();
        context.setGraphics(gfxNew);
        c.getFactory().drawGhost(context, Color.GRAY, loc.getX(), loc.getY(), c.getAttributeSet());
        gfxNew.dispose();
      }
    }

    for (val comp : unionSet) {
      if (!suppressHandles.contains(comp) && !hidden.contains(comp)) {
        val gfxNew = gfx.create();
        context.setGraphics(gfxNew);
        val handler = (CustomHandles) comp.getFeature(CustomHandles.class);
        if (handler == null) {
          context.drawHandles(comp);
        } else {
          handler.drawHandles(context);
        }
        gfxNew.dispose();
      }
    }

    context.setGraphics(gfx);
  }

  public void drawGhostsShifted(ComponentDrawContext context, int dx, int dy) {
    if (shouldSnap()) {
      dx = Canvas.snapXToGrid(dx);
      dy = Canvas.snapYToGrid(dy);
    }
    val gfx = context.getGraphics();
    for (val comp : unionSet) {
      val attrs = comp.getAttributeSet();
      val loc = comp.getLocation();
      val x = loc.getX() + dx;
      val y = loc.getY() + dy;
      context.setGraphics(gfx.create());
      comp.getFactory().drawGhost(context, Color.gray, x, y, attrs);
      context.getGraphics().dispose();
    }
    context.setGraphics(gfx);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Selection)) return false;
    val otherSelection = (Selection) other;
    return this.selected.equals(otherSelection.selected) && this.lifted.equals(otherSelection.lifted);
  }

  public Collection<Component> getAnchoredComponents() {
    return selected;
  }

  public Set<Component> getComponents() {
    return unionSet;
  }

  public Collection<Component> getComponentsContaining(Location query) {
    val ret = new HashSet<Component>();
    for (val comp : unionSet) {
      if (comp.contains(query)) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getComponentsContaining(Location query, Graphics g) {
    val ret = new HashSet<Component>();
    for (val comp : unionSet) {
      if (comp.contains(query, g)) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getComponentsWithin(Bounds bds) {
    val ret = new HashSet<Component>();
    for (val comp : unionSet) {
      if (bds.contains(comp.getBounds())) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getComponentsWithin(Bounds bds, Graphics g) {
    val ret = new HashSet<Component>();
    for (val comp : unionSet) {
      if (bds.contains(comp.getBounds(g))) ret.add(comp);
    }
    return ret;
  }

  public Collection<Component> getFloatingComponents() {
    return lifted;
  }

  //
  // query methods
  //
  public boolean isEmpty() {
    return selected.isEmpty() && lifted.isEmpty();
  }

  @Override
  public void print() {
    logger.error("isVisible: {}", isVisible);
    super.print();
  }

  public interface Listener {
    void selectionChanged(Selection.Event event);
  }

  public static class Event {
    @Getter final Object source;

    Event(Object source) {
      this.source = source;
    }
  }

  private class MyListener implements ProjectListener, CircuitListener {
    private final WeakHashMap<Action, SelectionSave> savedSelections;

    MyListener() {
      savedSelections = new WeakHashMap<>();
    }

    @Override
    public void circuitChanged(CircuitEvent event) {
      if (event.getAction() == CircuitEvent.TRANSACTION_DONE) {
        val circuit = event.getCircuit();
        val repl = event.getResult().getReplacementMap(circuit);
        var change = false;

        val oldAnchored = new ArrayList<>(getComponents());
        for (val comp : oldAnchored) {
          val replacedBy = repl.getReplacementsFor(comp);
          if (replacedBy != null) {
            change = true;
            selected.remove(comp);
            lifted.remove(comp);
            for (val add : replacedBy) {
              if (circuit.contains(add)) {
                selected.add(add);
              } else {
                lifted.add(add);
              }
            }
          }
        }

        if (change) {
          fireSelectionChanged();
        }
      }
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      val type = event.getAction();
      if (type == ProjectEvent.ACTION_START) {
        val save = SelectionSave.create(Selection.this);
        savedSelections.put((Action) event.getData(), save);
      } else if (type == ProjectEvent.ACTION_COMPLETE) {
        val save = savedSelections.get(event.getData());
        if (save != null && save.isSame(Selection.this)) {
          savedSelections.remove(event.getData());
        }
      } else if (type == ProjectEvent.ACTION_MERGE) {
        val save = savedSelections.get(event.getOldData());
        savedSelections.put((Action) event.getData(), save);
      } else if (type == ProjectEvent.UNDO_COMPLETE) {
        val circ = event.getProject().getCurrentCircuit();
        val act = (Action) event.getData();
        val save = savedSelections.get(act);
        if (save != null) {
          lifted.clear();
          selected.clear();
          for (var i = 0; i < 2; i++) {
            val components = (i == 0) ? save.getFloatingComponents() : save.getAnchoredComponents();

            if (components != null) {
              for (val comp : components) {
                if (circ.contains(comp)) {
                  selected.add(comp);
                } else {
                  lifted.add(comp);
                }
              }
            }
          }
          fireSelectionChanged();
        }
      }
    }
  }
}
