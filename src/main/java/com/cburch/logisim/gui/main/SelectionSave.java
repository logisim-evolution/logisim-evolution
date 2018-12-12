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

package com.cburch.logisim.gui.main;

import java.util.Collection;
import java.util.HashSet;

import com.cburch.logisim.comp.Component;

class SelectionSave {
	public static SelectionSave create(Selection sel) {
		SelectionSave save = new SelectionSave();

		Collection<Component> lifted = sel.getFloatingComponents();
		if (!lifted.isEmpty()) {
			save.floating = lifted.toArray(new Component[lifted.size()]);
		}

		Collection<Component> selected = sel.getAnchoredComponents();
		if (!selected.isEmpty()) {
			save.anchored = selected.toArray(new Component[selected.size()]);
		}

		return save;
	}

	private static boolean isSame(Component[] save, Collection<Component> sel) {
		if (save == null) {
			return sel.isEmpty();
		} else {
			return toSet(save).equals(sel);
		}
	}

	private static boolean isSame(Component[] a, Component[] b) {
		if (a == null || a.length == 0) {
			return b == null || b.length == 0;
		} else if (b == null || b.length == 0) {
			return false;
		} else if (a.length != b.length) {
			return false;
		} else {
			return toSet(a).equals(toSet(b));
		}
	}

	private static HashSet<Component> toSet(Component[] comps) {
		HashSet<Component> ret = new HashSet<Component>(comps.length);
		for (Component c : comps)
			ret.add(c);
		return ret;
	}

	private Component[] floating;

	private Component[] anchored;

	private SelectionSave() {
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SelectionSave) {
			SelectionSave o = (SelectionSave) other;
			return isSame(this.floating, o.floating)
					&& isSame(this.anchored, o.anchored);
		} else {
			return false;
		}
	}

	public Component[] getAnchoredComponents() {
		return anchored;
	}

	public Component[] getFloatingComponents() {
		return floating;
	}

	@Override
	public int hashCode() {
		int ret = 0;
		if (floating != null) {
			for (Component c : floating)
				ret += c.hashCode();
		}
		if (anchored != null) {
			for (Component c : anchored)
				ret += c.hashCode();
		}
		return ret;
	}

	public boolean isSame(Selection sel) {
		return isSame(floating, sel.getFloatingComponents())
				&& isSame(anchored, sel.getAnchoredComponents());
	}
}
