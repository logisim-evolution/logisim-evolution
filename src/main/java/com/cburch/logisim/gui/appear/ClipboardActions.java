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
import java.util.Map;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.util.ZOrder;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;

public class ClipboardActions extends Action {

	public static Action copy(AppearanceCanvas canvas) {
		return new ClipboardActions(false, canvas);
	}

	public static Action cut(AppearanceCanvas canvas) {
		return new ClipboardActions(true, canvas);
	}

	private boolean remove;
	private AppearanceCanvas canvas;
	private CanvasModel canvasModel;
	private ClipboardContents oldClipboard;
	private Map<CanvasObject, Integer> affected;
	private ClipboardContents newClipboard;

	private ClipboardActions(boolean remove, AppearanceCanvas canvas) {
		this.remove = remove;
		this.canvas = canvas;
		this.canvasModel = canvas.getModel();

		ArrayList<CanvasObject> contents = new ArrayList<CanvasObject>();
		Direction anchorFacing = null;
		Location anchorLocation = null;
		ArrayList<CanvasObject> aff = new ArrayList<CanvasObject>();
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
		newClipboard = new ClipboardContents(contents, anchorLocation,
				anchorFacing);
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
			return Strings.get("cutSelectionAction");
		} else {
			return Strings.get("copySelectionAction");
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
