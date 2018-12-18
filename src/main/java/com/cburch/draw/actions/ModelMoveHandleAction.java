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

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;

public class ModelMoveHandleAction extends ModelAction {
	private HandleGesture gesture;
	private Handle newHandle;

	public ModelMoveHandleAction(CanvasModel model, HandleGesture gesture) {
		super(model);
		this.gesture = gesture;
	}

	@Override
	void doSub(CanvasModel model) {
		newHandle = model.moveHandle(gesture);
	}

	@Override
	public String getName() {
		return Strings.get("actionMoveHandle");
	}

	public Handle getNewHandle() {
		return newHandle;
	}

	@Override
	public Collection<CanvasObject> getObjects() {
		return Collections.singleton(gesture.getHandle().getObject());
	}

	@Override
	void undoSub(CanvasModel model) {
		Handle oldHandle = gesture.getHandle();
		int dx = oldHandle.getX() - newHandle.getX();
		int dy = oldHandle.getY() - newHandle.getY();
		HandleGesture reverse = new HandleGesture(newHandle, dx, dy, 0);
		model.moveHandle(reverse);
	}
}
