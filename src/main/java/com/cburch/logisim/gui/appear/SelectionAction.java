/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.gui.appear;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.util.ZOrder;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;

class SelectionAction extends Action {
	private StringGetter displayName;
	private AppearanceCanvas canvas;
	private CanvasModel canvasModel;
	private Map<CanvasObject, Integer> toRemove;
	private Collection<CanvasObject> toAdd;
	private Collection<CanvasObject> oldSelection;
	private Collection<CanvasObject> newSelection;
	private Location anchorNewLocation;
	private Direction anchorNewFacing;
	private Location anchorOldLocation;
	private Direction anchorOldFacing;

	public SelectionAction(AppearanceCanvas canvas, StringGetter displayName,
			Collection<CanvasObject> toRemove, Collection<CanvasObject> toAdd,
			Collection<CanvasObject> newSelection, Location anchorLocation,
			Direction anchorFacing) {
		this.canvas = canvas;
		this.canvasModel = canvas.getModel();
		this.displayName = displayName;
		this.toRemove = toRemove == null ? null : ZOrder.getZIndex(toRemove,
				canvasModel);
		this.toAdd = toAdd;
		this.oldSelection = new ArrayList<CanvasObject>(canvas.getSelection()
				.getSelected());
		this.newSelection = newSelection;
		this.anchorNewLocation = anchorLocation;
		this.anchorNewFacing = anchorFacing;
	}

	@Override
	public void doIt(Project proj) {
		Selection sel = canvas.getSelection();
		sel.clearSelected();
		if (toRemove != null)
			canvasModel.removeObjects(toRemove.keySet());
		int dest = AppearanceCanvas.getMaxIndex(canvasModel) + 1;
		if (toAdd != null)
			canvasModel.addObjects(dest, toAdd);

		AppearanceAnchor anchor = findAnchor(canvasModel);
		if (anchor != null && anchorNewLocation != null) {
			anchorOldLocation = anchor.getLocation();
			anchor.translate(
					anchorNewLocation.getX() - anchorOldLocation.getX(),
					anchorNewLocation.getY() - anchorOldLocation.getY());
		}
		if (anchor != null && anchorNewFacing != null) {
			anchorOldFacing = anchor.getFacing();
			anchor.setValue(AppearanceAnchor.FACING, anchorNewFacing);
		}
		sel.setSelected(newSelection, true);
		canvas.repaint();
	}

	private AppearanceAnchor findAnchor(CanvasModel canvasModel) {
		for (Object o : canvasModel.getObjectsFromTop()) {
			if (o instanceof AppearanceAnchor) {
				return (AppearanceAnchor) o;
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return displayName.toString();
	}

	@Override
	public void undo(Project proj) {
		AppearanceAnchor anchor = findAnchor(canvasModel);
		if (anchor != null && anchorOldLocation != null) {
			anchor.translate(
					anchorOldLocation.getX() - anchorNewLocation.getX(),
					anchorOldLocation.getY() - anchorNewLocation.getY());
		}
		if (anchor != null && anchorOldFacing != null) {
			anchor.setValue(AppearanceAnchor.FACING, anchorOldFacing);
		}
		Selection sel = canvas.getSelection();
		sel.clearSelected();
		if (toAdd != null)
			canvasModel.removeObjects(toAdd);
		if (toRemove != null)
			canvasModel.addObjects(toRemove);
		sel.setSelected(oldSelection, true);
		canvas.repaint();
	}
}
