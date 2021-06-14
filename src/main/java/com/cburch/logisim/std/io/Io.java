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

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Io extends Library {

  /**
   * Unique identifier of the library, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "I/O";

  public static final Attribute<Color> ATTR_COLOR =
      Attributes.forColor("color", S.getter("ioColorAttr"));
  static final Attribute<Color> ATTR_ON_COLOR =
          Attributes.forColor("color", S.getter("ioOnColor"));
  static final Attribute<Color> ATTR_OFF_COLOR =
      Attributes.forColor("offcolor", S.getter("ioOffColor"));
  static final Attribute<Color> ATTR_BACKGROUND =
      Attributes.forColor("bg", S.getter("ioBackgroundColor"));
  static final Attribute<Boolean> ATTR_ACTIVE =
      Attributes.forBoolean("active", S.getter("ioActiveAttr"));

  static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255, 0);

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription("Button", S.getter("buttonComponent"), "button.gif", Button.class),
    new FactoryDescription("DipSwitch", S.getter("dipswitchComponent"), "dipswitch.gif", DipSwitch.class),
    new FactoryDescription("Joystick", S.getter("joystickComponent"), "joystick.gif", Joystick.class),
    new FactoryDescription("Keyboard", S.getter("keyboardComponent"), "keyboard.gif", Keyboard.class),
    new FactoryDescription("LED", S.getter("ledComponent"), "led.gif", Led.class),
    new FactoryDescription("LedBar", S.getter("ioLedBarComponent"), "ledlightbar.gif", LedBar.class),
    new FactoryDescription("RGBLED", S.getter("RGBledComponent"), "rgbled.gif", RGBLed.class),
    new FactoryDescription("7-Segment Display", S.getter("sevenSegmentComponent"), "7seg.gif", SevenSegment.class),
    new FactoryDescription("Hex Digit Display", S.getter("hexDigitComponent"), "hexdig.gif", HexDigit.class),
    new FactoryDescription("DotMatrix", S.getter("dotMatrixComponent"), "dotmat.gif", DotMatrix.class),
    new FactoryDescription("TTY", S.getter("ttyComponent"), "tty.gif", Tty.class),
    new FactoryDescription("PortIO", S.getter("pioComponent"), "pio.gif", PortIO.class),
    new FactoryDescription("ReptarLB", S.getter("repLBComponent"), "localbus.gif", ReptarLocalBus.class),
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("ioLibrary");
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = new ArrayList<>();
      tools.addAll(FactoryDescription.getTools(Io.class, DESCRIPTIONS));
      tools.add(new AddTool(Video.factory));
    }
    return tools;
  }
}
