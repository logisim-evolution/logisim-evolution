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

class SplitterParameters {
  private final int dxEnd0; // location of split end 0 relative to origin
  private final int dyEnd0;
  private final int ddxEnd; // distance from split end i to split end (i + 1)
  private final int ddyEnd;
  private final int dxEndSpine; // distance from split end to spine
  private final int dyEndSpine;
  private final int dxSpine0; // distance from origin to far end of spine
  private final int dySpine0;
  private final int dxSpine1; // distance from origin to near end of spine
  private final int dySpine1;
  private final int textAngle; // angle to rotate text
  private final int halign; // justification of text
  private final int valign;

  SplitterParameters(SplitterAttributes attrs) {
    final var appear = attrs.appear;
    final var fanout = attrs.fanout;
    final var facing = attrs.facing;

    int justify;
    if (appear == SplitterAttributes.APPEAR_CENTER || appear == SplitterAttributes.APPEAR_LEGACY) {
      justify = 0;
    } else if (appear == SplitterAttributes.APPEAR_RIGHT) {
      justify = 1;
    } else {
      justify = -1;
    }
    final var width = 20;

    final var gap = attrs.spacing * 10;
    final var offs = 6;
    if (facing == Direction.NORTH || facing == Direction.SOUTH) { // ^ or V
      final var m = facing == Direction.NORTH ? 1 : -1;
      dxEnd0 =
          justify == 0
              ? gap * ((fanout + 1) / 2 - 1)
              : m * justify < 0 ? -10 : (10 + gap * (fanout - 1));
      dyEnd0 = -m * width;
      ddxEnd = -gap;
      ddyEnd = 0;
      dxEndSpine = 0;
      dyEndSpine = m * (width - offs);
      dxSpine0 = m * justify * (10 + gap * (fanout - 1) - 1);
      dySpine0 = -m * offs;
      dxSpine1 = m * justify * offs;
      dySpine1 = -m * offs;
      textAngle = 90;
      halign = m > 0 ? GraphicsUtil.H_RIGHT : GraphicsUtil.H_LEFT;
      valign = GraphicsUtil.V_BASELINE;
    } else { // > or <
      final var m = facing == Direction.WEST ? -1 : 1;
      dxEnd0 = m * width;
      dyEnd0 =
          justify == 0 ? -gap * (fanout / 2) : m * justify > 0 ? 10 : -(10 + gap * (fanout - 1));
      ddxEnd = 0;
      ddyEnd = gap;
      dxEndSpine = -m * (width - offs);
      dyEndSpine = 0;
      dxSpine0 = m * offs;
      dySpine0 = m * justify * (10 + gap * (fanout - 1) - 1);
      dxSpine1 = m * offs;
      dySpine1 = m * justify * offs;
      textAngle = 0;
      halign = m > 0 ? GraphicsUtil.H_LEFT : GraphicsUtil.H_RIGHT;
      valign = m * justify < 0 ? GraphicsUtil.V_TOP : GraphicsUtil.V_BASELINE;
    }
  }

  public int getEnd0X() {
    return dxEnd0;
  }

  public int getEnd0Y() {
    return dyEnd0;
  }

  public int getEndToEndDeltaX() {
    return ddxEnd;
  }

  public int getEndToEndDeltaY() {
    return ddyEnd;
  }

  public int getEndToSpineDeltaX() {
    return dxEndSpine;
  }

  public int getEndToSpineDeltaY() {
    return dyEndSpine;
  }

  public int getSpine0X() {
    return dxSpine0;
  }

  public int getSpine0Y() {
    return dySpine0;
  }

  public int getSpine1X() {
    return dxSpine1;
  }

  public int getSpine1Y() {
    return dySpine1;
  }

  public int getTextAngle() {
    return textAngle;
  }

  public int getTextHorzAlign() {
    return halign;
  }

  public int getTextVertAlign() {
    return valign;
  }
}
