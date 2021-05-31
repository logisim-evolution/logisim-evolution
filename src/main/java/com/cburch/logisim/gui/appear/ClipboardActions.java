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

package com.cburch.logisim.gui.appear;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.util.ZOrder;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import java.util.Map;

public class ClipboardActions extends Action {

  private final boolean remove;
  private final AppearanceCanvas canvas;
  private final CanvasModel canvasModel;
  private final Map<CanvasObject, Integer> affected;
  private final ClipboardContents newClipboard;
  private ClipboardContents oldClipboard;
  private ClipboardActions(boolean remove, AppearanceCanvas canvas) {
    this.remove = remove;
    this.canvas = canvas;
    this.canvasModel = canvas.getModel();

    ArrayList<CanvasObject> contents = new ArrayList<>();
    Direction anchorFacing = null;
    Location anchorLocation = null;
    ArrayList<CanvasObject> aff = new ArrayList<>();
    for (CanvasObject o : canvas.getSelection().getSelected()) {
      if (o.canRemove()) {
        aff.add(o);
        contents.add(o.clone());
      } else if (o instanceof AppearanceAnchor) {
        AppearanceAnchor anch = (AppearanceAnchor) o;
        anchorFacing = anch.getFacing();
        anchorLocation = anch.getLocation();
      }
    }
    contents.trimToSize();
    affected = ZOrder.getZIndex(aff, canvasModel);
    newClipboard = new ClipboardContents(contents, anchorLocation, anchorFacing);
  }

  public static Action copy(AppearanceCanvas canvas) {
    return new ClipboardActions(false, canvas);
  }

  public static Action cut(AppearanceCanvas canvas) {
    return new ClipboardActions(true, canvas);
  }

  @Override
  public void doIt(Project proj) {
    oldClipboard = Clipboard.get();
    Clipboard.set(newClipboard);
    if (remove) {
      canvasModel.removeObjects(affected.keySet());
    }
  }

  @Override
  public String getName() {
    if (remove) {
      return S.get("cutSelectionAction");
    } else {
      return S.get("copySelectionAction");
    }
  }

  @Override
  public void undo(Project proj) {
    if (remove) {
      canvasModel.addObjects(affected);
      canvas.getSelection().clearSelected();
      canvas.getSelection().setSelected(affected.keySet(), true);
    }
    Clipboard.set(oldClipboard);
  }
}
