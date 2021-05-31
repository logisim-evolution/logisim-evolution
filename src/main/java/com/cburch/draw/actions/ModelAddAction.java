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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ModelAddAction extends ModelAction {
  private final List<CanvasObject> added;
  private final int addIndex;

  public ModelAddAction(CanvasModel model, CanvasObject added) {
    this(model, Collections.singleton(added));
  }

  public ModelAddAction(CanvasModel model, Collection<CanvasObject> added) {
    super(model);
    this.added = new ArrayList<>(added);
    this.addIndex = model.getObjectsFromBottom().size();
  }

  public ModelAddAction(CanvasModel model, Collection<CanvasObject> added, int index) {
    super(model);
    this.added = new ArrayList<>(added);
    this.addIndex = index;
  }

  @Override
  void doSub(CanvasModel model) {
    model.addObjects(addIndex, added);
  }

  public int getDestinationIndex() {
    return addIndex;
  }

  @Override
  public String getName() {
    return S.fmt("actionAdd", getShapesName(added));
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.unmodifiableList(added);
  }

  @Override
  void undoSub(CanvasModel model) {
    model.removeObjects(added);
  }
}
