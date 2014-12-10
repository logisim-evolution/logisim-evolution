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

package com.cburch.logisim.file;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;

public class Options {
	public static final AttributeOption GATE_UNDEFINED_IGNORE = new AttributeOption(
			"ignore", Strings.getter("gateUndefinedIgnore"));
	public static final AttributeOption GATE_UNDEFINED_ERROR = new AttributeOption(
			"error", Strings.getter("gateUndefinedError"));

	public static final AttributeOption TICK_MAIN_PERIOD = new AttributeOption(
			"period", Strings.getter("tick_main_period"));
	public static final AttributeOption TICK_MAIN_HALF_PERIOD = new AttributeOption(
			"half_period", Strings.getter("tick_main_half_period"));

	public static final Attribute<Integer> sim_limit_attr = Attributes
			.forInteger("simlimit", Strings.getter("simLimitOption"));
	public static final Attribute<Integer> sim_rand_attr = Attributes
			.forInteger("simrand", Strings.getter("simRandomOption"));
	public static final Attribute<AttributeOption> ATTR_GATE_UNDEFINED = Attributes
			.forOption("gateUndefined", Strings.getter("gateUndefinedOption"),
					new AttributeOption[] { GATE_UNDEFINED_IGNORE,
							GATE_UNDEFINED_ERROR });
	public static final Attribute<AttributeOption> ATTR_TICK_MAIN = Attributes
			.forOption("tickmain", Strings.getter("mainTickOption"),
					new AttributeOption[] { TICK_MAIN_HALF_PERIOD,
							TICK_MAIN_PERIOD });

	public static final Integer sim_rand_dflt = Integer.valueOf(32);

	private static final Attribute<?>[] ATTRIBUTES = { ATTR_GATE_UNDEFINED,
			sim_limit_attr, sim_rand_attr, ATTR_TICK_MAIN, };
	private static final Object[] DEFAULTS = { GATE_UNDEFINED_IGNORE,
			Integer.valueOf(1000), Integer.valueOf(0), TICK_MAIN_HALF_PERIOD, };

	private AttributeSet attrs;
	private MouseMappings mmappings;
	private ToolbarData toolbar;

	public Options() {
		attrs = AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
		mmappings = new MouseMappings();
		toolbar = new ToolbarData();
	}

	public void copyFrom(Options other, LogisimFile dest) {
		AttributeSets.copy(other.attrs, this.attrs);
		this.toolbar.copyFrom(other.toolbar, dest);
		this.mmappings.copyFrom(other.mmappings, dest);
	}

	public AttributeSet getAttributeSet() {
		return attrs;
	}

	public MouseMappings getMouseMappings() {
		return mmappings;
	}

	public ToolbarData getToolbarData() {
		return toolbar;
	}
}
