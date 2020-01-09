/**
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

package com.cburch.draw.actions;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.undo.Action;
import java.util.Collection;
import java.util.Collections;

public abstract class ModelAction extends Action {
  static String getShapesName(Collection<CanvasObject> coll) {
    if (coll.size() != 1) {
      return S.get("shapeMultiple");
    } else {
      CanvasObject shape = coll.iterator().next();
      return shape.getDisplayName();
    }
  }

  private CanvasModel model;

  public ModelAction(CanvasModel model) {
    this.model = model;
  }

  @Override
  public final void doIt() {
    doSub(model);
  }

  abstract void doSub(CanvasModel model);

  public CanvasModel getModel() {
    return model;
  }

  @Override
  public abstract String getName();

  public Collection<CanvasObject> getObjects() {
    return Collections.emptySet();
  }

  @Override
  public final void undo() {
    undoSub(model);
  }

  abstract void undoSub(CanvasModel model);
}
