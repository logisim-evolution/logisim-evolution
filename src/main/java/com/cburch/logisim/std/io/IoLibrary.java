/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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

public class IoLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "I/O";

  public static final Attribute<Color> ATTR_COLOR =
      Attributes.forColor("color", S.getter("ioColorAttr"));
  public static final Attribute<Color> ATTR_ON_COLOR =
      Attributes.forColor("color", S.getter("ioOnColor"));
  public static final Attribute<Color> ATTR_OFF_COLOR =
      Attributes.forColor("offcolor", S.getter("ioOffColor"));
  static final Attribute<Color> ATTR_BACKGROUND =
      Attributes.forColor("bg", S.getter("ioBackgroundColor"));
  static final Attribute<Boolean> ATTR_ACTIVE =
      Attributes.forBoolean("active", S.getter("ioActiveAttr"));

  static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255, 0);

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(Button.class, S.getter("buttonComponent"), "button.gif"),
    new FactoryDescription(DipSwitch.class, S.getter("dipswitchComponent"), "dipswitch.gif"),
    new FactoryDescription(Joystick.class, S.getter("joystickComponent"), "joystick.gif"),
    new FactoryDescription(Keyboard.class, S.getter("keyboardComponent"), "keyboard.gif"),
    new FactoryDescription(Led.class, S.getter("ledComponent"), "led.gif"),
    new FactoryDescription(LedBar.class, S.getter("ioLedBarComponent"), "ledlightbar.gif"),
    new FactoryDescription(RgbLed.class, S.getter("RGBledComponent"), "rgbled.gif"),
    new FactoryDescription(SevenSegment.class, S.getter("sevenSegmentComponent"), "7seg.gif"),
    new FactoryDescription(HexDigit.class, S.getter("hexDigitComponent"), "hexdig.gif"),
    new FactoryDescription(DotMatrix.class, S.getter("dotMatrixComponent"), "dotmat.gif"),
    new FactoryDescription(Tty.class, S.getter("ttyComponent"), "tty.gif"),
    new FactoryDescription(PortIo.class, S.getter("pioComponent"), "pio.gif"),
    new FactoryDescription(ReptarLocalBus.class, S.getter("repLBComponent"), "localbus.gif"),
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
      tools.addAll(FactoryDescription.getTools(IoLibrary.class, DESCRIPTIONS));
      tools.add(new AddTool(Video.factory));
    }
    return tools;
  }
}
