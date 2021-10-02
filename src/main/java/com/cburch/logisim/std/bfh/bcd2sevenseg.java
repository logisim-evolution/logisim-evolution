/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.bfh;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;

public class bcd2sevenseg extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "BCD_to_7_Segment_decoder";

  static final int PER_DELAY = 1;
  public static final int SEGMENT_A = 0;
  public static final int SEGMENT_B = 1;
  public static final int SEGMENT_C = 2;
  public static final int SEGMENT_D = 3;
  public static final int SEGMENT_E = 4;
  public static final int SEGMENT_F = 5;
  public static final int SEGMENT_G = 6;
  public static final int BCD_IN = 7;

  public bcd2sevenseg() {
    super(_ID, S.getter("BCD2SevenSegment"), new bcd2sevensegHDLGeneratorFactory());
    setAttributes(new Attribute[] {StdAttr.DUMMY}, new Object[] {""});
    setOffsetBounds(Bounds.create(-10, -20, 50, 100));
    final var ps = new Port[8];
    ps[SEGMENT_A] = new Port(20, 0, Port.OUTPUT, 1);
    ps[SEGMENT_B] = new Port(30, 0, Port.OUTPUT, 1);
    ps[SEGMENT_C] = new Port(20, 60, Port.OUTPUT, 1);
    ps[SEGMENT_D] = new Port(10, 60, Port.OUTPUT, 1);
    ps[SEGMENT_E] = new Port(0, 60, Port.OUTPUT, 1);
    ps[SEGMENT_F] = new Port(10, 0, Port.OUTPUT, 1);
    ps[SEGMENT_G] = new Port(0, 0, Port.OUTPUT, 1);
    ps[BCD_IN] = new Port(10, 80, Port.INPUT, 4);
    ps[SEGMENT_A].setToolTip(S.getter("Segment_A"));
    ps[SEGMENT_B].setToolTip(S.getter("Segment_B"));
    ps[SEGMENT_C].setToolTip(S.getter("Segment_C"));
    ps[SEGMENT_D].setToolTip(S.getter("Segment_D"));
    ps[SEGMENT_E].setToolTip(S.getter("Segment_E"));
    ps[SEGMENT_F].setToolTip(S.getter("Segment_F"));
    ps[SEGMENT_G].setToolTip(S.getter("Segment_G"));
    ps[BCD_IN].setToolTip(S.getter("BCDValue"));
    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var gfx = painter.getGraphics();
    final var myBounds = painter.getBounds();
    if (!painter.isPrintView()) gfx.setColor(Color.BLUE);
    painter.drawRectangle(myBounds, "");
    painter.drawPort(BCD_IN, "BCD", Direction.SOUTH);
    for (var i = 0; i < 7; i++) {
      painter.drawPort(i);
    }
    gfx.setColor(Color.BLACK);
    painter.drawRectangle(
        myBounds.getX() + 5,
        myBounds.getY() + 20,
        myBounds.getWidth() - 10,
        myBounds.getHeight() - 40,
        "");
  }

  @Override
  public void propagate(InstanceState state) {
    if (state.getPortValue(BCD_IN).isFullyDefined()
        && !state.getPortValue(BCD_IN).isErrorValue()
        && !state.getPortValue(BCD_IN).isUnknown()) {
      int value = (int) state.getPortValue(BCD_IN).toLongValue();
      switch (value) {
        case 0:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          break;
        case 1:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          break;
        case 2:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 3:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 4:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 5:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 6:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 7:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          break;
        case 8:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        case 9:
          state.setPort(SEGMENT_A, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createKnown(BitWidth.create(1), 0), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createKnown(BitWidth.create(1), 1), PER_DELAY);
          break;
        default:
          state.setPort(SEGMENT_A, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(SEGMENT_B, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(SEGMENT_C, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(SEGMENT_D, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(SEGMENT_E, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(SEGMENT_F, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          state.setPort(SEGMENT_G, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
          break;
      }
    } else {
      for (int i = 0; i < 7; i++)
        state.setPort(i, Value.createUnknown(BitWidth.create(1)), PER_DELAY);
    }
  }
}
