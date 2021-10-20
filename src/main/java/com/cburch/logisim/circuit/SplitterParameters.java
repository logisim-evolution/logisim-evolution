/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.util.GraphicsUtil;
import lombok.Getter;

class SplitterParameters {
  /**
   * location of split end 0 relative to origin
   */
  @Getter private final int end0X;
  @Getter private final int end0Y;
  /**
   * distance from split end i to split end (i + 1)
   */
  @Getter private final int endToEndDeltaX;
  @Getter private final int endToEndDeltaY;
  /**
   * distance from split end to spine
   */
  @Getter private final int endToSpineDeltaX;
  @Getter private final int endToSpineDeltaY;
  /**
   * distance from origin to far end of spine
   */
  @Getter private final int spine0X;
  @Getter private final int spine0Y;
  /**
   * distance from origin to near end of spine
   */
  @Getter private final int spine1X;
  @Getter private final int spine1Y;
  /**
   * angle to rotate text
   */
  @Getter private final int textAngle;
  /**
   * justification of text
   */
  @Getter private final int textHorzontalAlign;
  @Getter private final int textVerticalAlign;

  SplitterParameters(SplitterAttributes attrs) {
    final var appear = attrs.appear;
    final var fanout = attrs.fanout;
    final var facing = attrs.facing;

    var justify = -1;
    if (appear == SplitterAttributes.APPEAR_CENTER || appear == SplitterAttributes.APPEAR_LEGACY) {
      justify = 0;
    } else if (appear == SplitterAttributes.APPEAR_RIGHT) {
      justify = 1;
    }
    final var width = 20;

    final var gap = attrs.spacing * 10;
    final var offs = 6;
    if (facing == Direction.NORTH || facing == Direction.SOUTH) { // ^ or V
      final var m = facing == Direction.NORTH ? 1 : -1;
      end0X =
          justify == 0
              ? gap * ((fanout + 1) / 2 - 1)
              : m * justify < 0 ? -10 : (10 + gap * (fanout - 1));
      end0Y = -m * width;
      endToEndDeltaX = -gap;
      endToEndDeltaY = 0;
      endToSpineDeltaX = 0;
      endToSpineDeltaY = m * (width - offs);
      spine0X = m * justify * (10 + gap * (fanout - 1) - 1);
      spine0Y = -m * offs;
      spine1X = m * justify * offs;
      spine1Y = -m * offs;
      textAngle = 90;
      textHorzontalAlign = m > 0 ? GraphicsUtil.H_RIGHT : GraphicsUtil.H_LEFT;
      textVerticalAlign = GraphicsUtil.V_BASELINE;
    } else { // > or <
      final var m = facing == Direction.WEST ? -1 : 1;
      end0X = m * width;
      end0Y = justify == 0 ? -gap * (fanout / 2) : m * justify > 0 ? 10 : -(10 + gap * (fanout - 1));
      endToEndDeltaX = 0;
      endToEndDeltaY = gap;
      endToSpineDeltaX = -m * (width - offs);
      endToSpineDeltaY = 0;
      spine0X = m * offs;
      spine0Y = m * justify * (10 + gap * (fanout - 1) - 1);
      spine1X = m * offs;
      spine1Y = m * justify * offs;
      textAngle = 0;
      textHorzontalAlign = m > 0 ? GraphicsUtil.H_LEFT : GraphicsUtil.H_RIGHT;
      textVerticalAlign = m * justify < 0 ? GraphicsUtil.V_TOP : GraphicsUtil.V_BASELINE;
    }
  }

}
