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

package com.cburch.logisim.instance;

import java.awt.Color;
import java.awt.Font;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;

public interface StdAttr {
	public static final Attribute<Direction> FACING = Attributes.forDirection(
			"facing", Strings.getter("stdFacingAttr"));

	public static final Attribute<BitWidth> WIDTH = Attributes.forBitWidth(
			"width", Strings.getter("stdDataWidthAttr"));

	public static final AttributeOption TRIG_RISING = new AttributeOption(
			"rising", Strings.getter("stdTriggerRising"));
	public static final AttributeOption TRIG_FALLING = new AttributeOption(
			"falling", Strings.getter("stdTriggerFalling"));
	public static final AttributeOption TRIG_HIGH = new AttributeOption("high",
			Strings.getter("stdTriggerHigh"));
	public static final AttributeOption TRIG_LOW = new AttributeOption("low",
			Strings.getter("stdTriggerLow"));
	public static final Attribute<AttributeOption> TRIGGER = Attributes
			.forOption("trigger", Strings.getter("stdTriggerAttr"),
					new AttributeOption[] { TRIG_RISING, TRIG_FALLING,
							TRIG_HIGH, TRIG_LOW });
	public static final Attribute<AttributeOption> EDGE_TRIGGER = Attributes
			.forOption("trigger", Strings.getter("stdTriggerAttr"),
					new AttributeOption[] { TRIG_RISING, TRIG_FALLING });

	public static final Attribute<String> LABEL = Attributes.forString("label",
			Strings.getter("stdLabelAttr"));
	public static final Attribute<Font> LABEL_FONT = Attributes.forFont(
			"labelfont", Strings.getter("stdLabelFontAttr"));
	public static final Font DEFAULT_LABEL_FONT = new Font("SansSerif",
			Font.BOLD, 16);
	public static final Attribute<Color> LABEL_COLOR = Attributes.forColor(
			"labelcolor", Strings.getter("ioLabelColorAttr"));
	public static final Color DEFAULT_LABEL_COLOR = Color.BLUE;
	public static final Attribute<Boolean> LABEL_VISIBILITY = Attributes.forBoolean(
			"labelvisible", Strings.getter("stdLabelVisibility")); 
	
	public static final Attribute<String> DUMMY = Attributes.forHidden();
}
