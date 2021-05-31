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

package com.cburch.draw.actions;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.util.ZOrder;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ModelRemoveAction extends ModelAction {
  private final Map<CanvasObject, Integer> removed;

  public ModelRemoveAction(CanvasModel model, CanvasObject removed) {
    this(model, Collections.singleton(removed));
  }

  public ModelRemoveAction(CanvasModel model, Collection<CanvasObject> removed) {
    super(model);
    this.removed = ZOrder.getZIndex(removed, model);
  }

  @Override
  void doSub(CanvasModel model) {
    model.removeObjects(removed.keySet());
  }

  @Override
  public String getName() {
    return S.fmt("actionRemove", getShapesName(removed.keySet()));
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.unmodifiableSet(removed.keySet());
  }

  @Override
  void undoSub(CanvasModel model) {
    model.addObjects(removed);
  }
}
