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
 *******************************************************************************/

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import java.util.ArrayList;
import java.awt.Window;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.util.StringGetter;

public class ShowStateOption {
		private final boolean all, none;
		private final ArrayList<String> which = new ArrayList<String>();

		private ShowStateOption(boolean b) {
				all = b;
				none = !b;
		}

		public static final ShowStateOption ALL = new ShowStateOption(true);
		public static final ShowStateOption NONE = new ShowStateOption(false);

		public static final Attribute<ShowStateOption> ATTR =
			new ShowStateAttribute("cshowstate", S.getter("circuitShowStateAttr"));

		private static class ShowStateAttribute extends Attribute<ShowStateOption> {
			private ShowStateAttribute(String name, StringGetter disp) {
				super(name, disp);
			}

			@Override
			public ShowStateOption parse(Window source, String value) {
				return parse(value);
			}

			@Override
			public ShowStateOption parse(String value) {
				if (value.equalsIgnoreCase("none"))
					return NONE;
				else if (value.equalsIgnoreCase("all"))
					return ALL;
				else
					return NONE; // fixme: some
			}

			@Override
			public String toDisplayString(ShowStateOption value) {
				if (value == NONE)
					return S.get("circuitShowStateNone");
				else if (value == ALL)
					return S.get("circuitShowStateAll");
				else 
					return "some of n";
			}

			@Override
			public String toStandardString(ShowStateOption value) {
				if (value == NONE)
					return "none";
				else if (value == ALL)
					return "all";
				else 
					return "some";
			}
		}
}

