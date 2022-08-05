/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.plexers;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics;
import java.util.List;

public class PlexersLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Plexers";

  static boolean contains(Location loc, Bounds bds, Direction facing) {
    if (bds.contains(loc, 1)) {
      int x = loc.getX();
      int y = loc.getY();
      int x0 = bds.getX();
      int x1 = x0 + bds.getWidth();
      int y0 = bds.getY();
      int y1 = y0 + bds.getHeight();
      if (facing == Direction.NORTH || facing == Direction.SOUTH) {
        if (x < x0 + 5 || x > x1 - 5) {
          if (facing == Direction.SOUTH) {
            return y < y0 + 5;
          } else {
            return y > y1 - 5;
          }
        } else {
          return true;
        }
      } else {
        if (y < y0 + 5 || y > y1 - 5) {
          if (facing == Direction.EAST) {
            return x < x0 + 5;
          } else {
            return x > x1 - 5;
          }
        } else {
          return true;
        }
      }
    } else {
      return false;
    }
  }

  static void drawTrapezoid(Graphics g, Bounds bds, Direction facing, int facingLean) {
    int wid = bds.getWidth();
    int ht = bds.getHeight();
    int x0 = bds.getX();
    int x1 = x0 + wid;
    int y0 = bds.getY();
    int y1 = y0 + ht;
    int[] xp = {x0, x1, x1, x0};
    int[] yp = {y0, y0, y1, y1};
    if (facing == Direction.WEST) {
      yp[0] += facingLean;
      yp[3] -= facingLean;
    } else if (facing == Direction.NORTH) {
      xp[0] += facingLean;
      xp[1] -= facingLean;
    } else if (facing == Direction.SOUTH) {
      xp[2] -= facingLean;
      xp[3] += facingLean;
    } else {
      yp[1] += facingLean;
      yp[2] -= facingLean;
    }
    GraphicsUtil.switchToWidth(g, 2);
    g.drawPolygon(xp, yp, 4);
  }

  static final AttributeOption SIZE_NARROW = new AttributeOption(20, S.getter("gateSizeNarrowOpt"));
  static final AttributeOption SIZE_WIDE =
      new AttributeOption(40, S.getter("gateSizeWideOpt")); /* 30 for 2-to-1 */
  public static final Attribute<AttributeOption> ATTR_SIZE =
      Attributes.forOption(
          "size", S.getter("gateSizeAttr"), new AttributeOption[] {SIZE_NARROW, SIZE_WIDE});

  public static final Attribute<BitWidth> ATTR_SELECT =
      Attributes.forBitWidth("select", S.getter("plexerSelectBitsAttr"), 1, 5);
  public static final Object DEFAULT_SELECT = BitWidth.create(1);

  public static final Attribute<Boolean> ATTR_TRISTATE =
      Attributes.forBoolean("tristate", S.getter("plexerThreeStateAttr"));
  public static final Object DEFAULT_TRISTATE = Boolean.FALSE;
  public static final AttributeOption DISABLED_FLOATING =
      new AttributeOption("Z", S.getter("plexerDisabledFloating"));

  public static final AttributeOption DISABLED_ZERO =
      new AttributeOption("0", S.getter("plexerDisabledZero"));

  public static final Attribute<AttributeOption> ATTR_DISABLED =
      Attributes.forOption(
          "disabled",
          S.getter("plexerDisabledAttr"),
          new AttributeOption[] {DISABLED_FLOATING, DISABLED_ZERO});
  public static final Attribute<Boolean> ATTR_ENABLE =
      Attributes.forBoolean("enable", S.getter("plexerEnableAttr"));
  public static final Object DEFAULT_ENABLE = Boolean.FALSE;

  public static final int DELAY = 3;

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(Multiplexer.class, S.getter("multiplexerComponent"), "multiplexer.gif"),
    new FactoryDescription(
        Demultiplexer.class, S.getter("demultiplexerComponent"), "demultiplexer.gif"),
    new FactoryDescription(Decoder.class, S.getter("decoderComponent"), "decoder.gif"),
    new FactoryDescription(
        PriorityEncoder.class, S.getter("priorityEncoderComponent"), "priencod.gif"),
    new FactoryDescription(BitSelector.class, S.getter("bitSelectorComponent"), "bitSelector.gif"),
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("plexerLibrary");
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(PlexersLibrary.class, DESCRIPTIONS);
    }
    return tools;
  }
}
