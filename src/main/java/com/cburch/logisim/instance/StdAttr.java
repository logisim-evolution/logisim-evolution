/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
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
 */

package com.cburch.logisim.instance;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import java.awt.Color;
import java.awt.Font;

public interface StdAttr {
  Attribute<Direction> FACING =
      Attributes.forDirection("facing", S.getter("stdFacingAttr"));
  
  Attribute<ComponentMapInformationContainer> MAPINFO =
      Attributes.forMap();

  Attribute<BitWidth> WIDTH =
      Attributes.forBitWidth("width", S.getter("stdDataWidthAttr"));

  AttributeOption TRIG_RISING =
      new AttributeOption("rising", S.getter("stdTriggerRising"));
  AttributeOption TRIG_FALLING =
      new AttributeOption("falling", S.getter("stdTriggerFalling"));
  AttributeOption TRIG_HIGH =
      new AttributeOption("high", S.getter("stdTriggerHigh"));
  AttributeOption TRIG_LOW =
      new AttributeOption("low", S.getter("stdTriggerLow"));
  Attribute<AttributeOption> TRIGGER =
      Attributes.forOption(
          "trigger",
          S.getter("stdTriggerAttr"),
          new AttributeOption[] {TRIG_RISING, TRIG_FALLING, TRIG_HIGH, TRIG_LOW});
  Attribute<AttributeOption> EDGE_TRIGGER =
      Attributes.forOption(
          "trigger", S.getter("stdTriggerAttr"), new AttributeOption[] {TRIG_RISING, TRIG_FALLING});

  Attribute<String> LABEL =
      Attributes.forString("label", S.getter("stdLabelAttr"));
  Attribute<Font> LABEL_FONT =
      Attributes.forFont("labelfont", S.getter("stdLabelFontAttr"));
  Font DEFAULT_LABEL_FONT = new Font("SansSerif", Font.BOLD, 16);
  Attribute<Color> LABEL_COLOR =
      Attributes.forColor("labelcolor", S.getter("ioLabelColorAttr"));
  Color DEFAULT_LABEL_COLOR = Color.BLUE;
  AttributeOption LABEL_CENTER =
      new AttributeOption("center", "center", S.getter("stdLabelCenter"));
  Attribute<Object> LABEL_LOC =
      Attributes.forOption(
          "labelloc",
          S.getter("stdLabelLocAttr"),
          new Object[] {
            LABEL_CENTER, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
          });
  Attribute<Boolean> LABEL_VISIBILITY =
      Attributes.forBoolean("labelvisible", S.getter("stdLabelVisibility"));
  AttributeOption APPEAR_CLASSIC =
      new AttributeOption("classic", S.getter("stdClassicAppearance"));
  AttributeOption APPEAR_FPGA =
      new AttributeOption("evolution", S.getter("stdEvolutionAppearance"));
  AttributeOption APPEAR_EVOLUTION =
      new AttributeOption("logisim_evolution", S.getter("stdLogisimEvolutionAppearance"));
  Attribute<AttributeOption> APPEARANCE =
      Attributes.forOption(
          "appearance",
          S.getter("stdAppearanceAttr"),
          new AttributeOption[] {APPEAR_CLASSIC, APPEAR_FPGA, APPEAR_EVOLUTION});

  Attribute<String> DUMMY = Attributes.forHidden();
}
