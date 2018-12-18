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

package com.cburch.logisim.gui.opts;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.MouseMappings;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.StringUtil;

class OptionsActions {
	private static class RemoveMapping extends Action {
		MouseMappings mm;
		Integer mods;
		Tool oldtool;

		RemoveMapping(MouseMappings mm, Integer mods) {
			this.mm = mm;
			this.mods = mods;
		}

		@Override
		public void doIt(Project proj) {
			oldtool = mm.getToolFor(mods);
			mm.setToolFor(mods, null);
		}

		@Override
		public String getName() {
			return Strings.get("removeMouseMappingAction");
		}

		@Override
		public void undo(Project proj) {
			mm.setToolFor(mods, oldtool);
		}
	}

	private static class SetAction extends Action {
		private AttributeSet attrs;
		private Attribute<Object> attr;
		private Object newval;
		private Object oldval;

		SetAction(AttributeSet attrs, Attribute<?> attr, Object value) {
			@SuppressWarnings("unchecked")
			Attribute<Object> a = (Attribute<Object>) attr;
			this.attrs = attrs;
			this.attr = a;
			this.newval = value;
		}

		@Override
		public void doIt(Project proj) {
			oldval = attrs.getValue(attr);
			attrs.setValue(attr, newval);
		}

		@Override
		public String getName() {
			return StringUtil.format(Strings.get("setOptionAction"),
					attr.getDisplayName());
		}

		@Override
		public void undo(Project proj) {
			attrs.setValue(attr, oldval);
		}
	}

	private static class SetMapping extends Action {
		MouseMappings mm;
		Integer mods;
		Tool oldtool;
		Tool tool;

		SetMapping(MouseMappings mm, Integer mods, Tool tool) {
			this.mm = mm;
			this.mods = mods;
			this.tool = tool;
		}

		@Override
		public void doIt(Project proj) {
			oldtool = mm.getToolFor(mods);
			mm.setToolFor(mods, tool);
		}

		@Override
		public String getName() {
			return Strings.get("addMouseMappingAction");
		}

		@Override
		public void undo(Project proj) {
			mm.setToolFor(mods, oldtool);
		}
	}

	public static Action removeMapping(MouseMappings mm, Integer mods) {
		return new RemoveMapping(mm, mods);
	}

	public static Action setAttribute(AttributeSet attrs, Attribute<?> attr,
			Object value) {
		Object oldValue = attrs.getValue(attr);
		if (!oldValue.equals(value)) {
			return new SetAction(attrs, attr, value);
		} else {
			return null;
		}
	}

	public static Action setMapping(MouseMappings mm, Integer mods, Tool tool) {
		return new SetMapping(mm, mods, tool);
	}

	private OptionsActions() {
	}
}
