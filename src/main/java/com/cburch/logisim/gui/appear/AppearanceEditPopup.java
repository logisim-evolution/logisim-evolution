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

import java.util.HashMap;
import java.util.Map;

import com.cburch.logisim.gui.main.EditHandler;
import com.cburch.logisim.gui.menu.EditPopup;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;

public class AppearanceEditPopup extends EditPopup implements
		EditHandler.Listener {
	private static final long serialVersionUID = 1L;
	private AppearanceCanvas canvas;
	private EditHandler handler;
	private Map<LogisimMenuItem, Boolean> enabled;

	public AppearanceEditPopup(AppearanceCanvas canvas) {
		super(true);
		this.canvas = canvas;
		handler = new AppearanceEditHandler(canvas);
		handler.setListener(this);
		enabled = new HashMap<LogisimMenuItem, Boolean>();
		handler.computeEnabled();
		initialize();
	}

	public void enableChanged(EditHandler handler, LogisimMenuItem action,
			boolean value) {
		enabled.put(action, Boolean.valueOf(value));
	}

	@Override
	protected void fire(LogisimMenuItem item) {
		if (item == LogisimMenuBar.CUT) {
			handler.cut();
		} else if (item == LogisimMenuBar.COPY) {
			handler.copy();
		} else if (item == LogisimMenuBar.DELETE) {
			handler.delete();
		} else if (item == LogisimMenuBar.DUPLICATE) {
			handler.duplicate();
		} else if (item == LogisimMenuBar.RAISE) {
			handler.raise();
		} else if (item == LogisimMenuBar.LOWER) {
			handler.lower();
		} else if (item == LogisimMenuBar.RAISE_TOP) {
			handler.raiseTop();
		} else if (item == LogisimMenuBar.LOWER_BOTTOM) {
			handler.lowerBottom();
		} else if (item == LogisimMenuBar.ADD_CONTROL) {
			handler.addControlPoint();
		} else if (item == LogisimMenuBar.REMOVE_CONTROL) {
			handler.removeControlPoint();
		}
	}

	@Override
	protected boolean isEnabled(LogisimMenuItem item) {
		Boolean value = enabled.get(item);
		return value != null && value.booleanValue();
	}

	@Override
	protected boolean shouldShow(LogisimMenuItem item) {
		if (item == LogisimMenuBar.ADD_CONTROL
				|| item == LogisimMenuBar.REMOVE_CONTROL) {
			return canvas.getSelection().getSelectedHandle() != null;
		} else {
			return true;
		}
	}
}
