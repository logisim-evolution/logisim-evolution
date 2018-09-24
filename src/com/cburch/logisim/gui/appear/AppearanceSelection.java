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

import java.util.Collection;

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.appear.AppearanceElement;

public class AppearanceSelection extends Selection {
	@Override
	public void setMovingDelta(int dx, int dy) {
		if (shouldSnap(getSelected())) {
			dx = (dx + 5) / 10 * 10;
			dy = (dy + 5) / 10 * 10;
		}
		super.setMovingDelta(dx, dy);
	}

	@Override
	public void setMovingShapes(Collection<? extends CanvasObject> shapes,
			int dx, int dy) {
		if (shouldSnap(shapes)) {
			dx = (dx + 5) / 10 * 10;
			dy = (dy + 5) / 10 * 10;
		}
		super.setMovingShapes(shapes, dx, dy);
	}

	private boolean shouldSnap(Collection<? extends CanvasObject> shapes) {
		for (CanvasObject o : shapes) {
			if (o instanceof AppearanceElement) {
				return true;
			}
		}
		return false;
	}
}
