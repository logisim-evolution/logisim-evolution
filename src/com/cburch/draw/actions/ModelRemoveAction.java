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

package com.cburch.draw.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.util.ZOrder;

public class ModelRemoveAction extends ModelAction {
	private Map<CanvasObject, Integer> removed;

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
		return Strings.get("actionRemove", getShapesName(removed.keySet()));
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
