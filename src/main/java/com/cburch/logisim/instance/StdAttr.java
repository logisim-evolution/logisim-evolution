/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
  Attribute<Direction> FACING = Attributes.forDirection("facing", S.getter("stdFacingAttr"));

  Attribute<ComponentMapInformationContainer> MAPINFO = Attributes.forMap();

  Attribute<BitWidth> WIDTH = Attributes.forBitWidth("width", S.getter("stdDataWidthAttr"));

  AttributeOption TRIG_RISING = new AttributeOption("rising", S.getter("stdTriggerRising"));
  AttributeOption TRIG_FALLING = new AttributeOption("falling", S.getter("stdTriggerFalling"));
  AttributeOption TRIG_HIGH = new AttributeOption("high", S.getter("stdTriggerHigh"));
  AttributeOption TRIG_LOW = new AttributeOption("low", S.getter("stdTriggerLow"));
  Attribute<AttributeOption> TRIGGER =
      Attributes.forOption(
          "trigger",
          S.getter("stdTriggerAttr"),
          new AttributeOption[] {TRIG_RISING, TRIG_FALLING, TRIG_HIGH, TRIG_LOW});
  Attribute<AttributeOption> EDGE_TRIGGER =
      Attributes.forOption(
          "trigger", S.getter("stdTriggerAttr"), new AttributeOption[] {TRIG_RISING, TRIG_FALLING});

  Attribute<String> LABEL = Attributes.forString("label", S.getter("stdLabelAttr"));
  Attribute<Font> LABEL_FONT = Attributes.forFont("labelfont", S.getter("stdLabelFontAttr"));
  Font DEFAULT_LABEL_FONT = new Font("SansSerif", Font.BOLD, 16);
  Attribute<Color> LABEL_COLOR = Attributes.forColor("labelcolor", S.getter("ioLabelColorAttr"));
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
  AttributeOption APPEAR_CLASSIC = new AttributeOption("classic", S.getter("stdClassicAppearance"));
  AttributeOption APPEAR_FPGA =
      new AttributeOption("evolution", S.getter("stdEvolutionAppearance"));
  AttributeOption APPEAR_EVOLUTION =
      new AttributeOption("logisim_evolution", S.getter("stdLogisimEvolutionAppearance"));
  Attribute<AttributeOption> APPEARANCE =
      Attributes.forOption(
          "appearance",
          S.getter("stdAppearanceAttr"),
          new AttributeOption[] {APPEAR_CLASSIC, APPEAR_FPGA, APPEAR_EVOLUTION});

  Attribute<BitWidth> FP_WIDTH =
      Attributes.forOption(
          "fpwidth",
          S.getter("stdFPDataWidthAttr"),
          new BitWidth[] {BitWidth.create(16), BitWidth.create(32), BitWidth.create(64)});

  AttributeOption SELECT_BOTTOM_LEFT =
      new AttributeOption("bl", S.getter("stdSelectBottomLeftOption"));
  AttributeOption SELECT_TOP_RIGHT =
      new AttributeOption("tr", S.getter("stdSelectTopRightOption"));
  Attribute<AttributeOption> SELECT_LOC =
      Attributes.forOption(
          "selloc",
          S.getter("stdSelectLocAttr"),
          new AttributeOption[] {SELECT_BOTTOM_LEFT, SELECT_TOP_RIGHT});

  Attribute<String> DUMMY = Attributes.forHidden();
}
