/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;

class LedBarGeometryTest {

  @Test
  void defaultLayoutMatchesHistoricalBar() {
    final var bar = new LedBar();
    final var attrs = bar.createAttributeSet();

    assertEquals(Direction.EAST, attrs.getValue(StdAttr.FACING));
    assertEquals(LedBar.DEFAULT_THICKNESS, attrs.getValue(LedBar.ATTR_THICKNESS));
    assertEquals(Bounds.create(-5, -30, 80, 30), bar.getOffsetBounds(attrs));

    final var origin = Location.create(100, 100, true);
    final var comp = bar.createComponent(origin, attrs);
    assertEquals(8, comp.getEnds().size());
    for (var i = 0; i < 8; i++) {
      assertEquals(Location.create(100 + 10 * i, 100, false), comp.getEnd(i).getLocation());
    }
  }

  @Test
  void thicknessChangesOnlyTheShortSide() {
    final var bar = new LedBar();
    final var attrs = bar.createAttributeSet();

    attrs.setValue(LedBar.ATTR_THICKNESS, 2);
    assertEquals(Bounds.create(-5, -20, 80, 20), bar.getOffsetBounds(attrs));

    attrs.setValue(LedBar.ATTR_THICKNESS, 4);
    assertEquals(Bounds.create(-5, -40, 80, 40), bar.getOffsetBounds(attrs));
  }

  @Test
  void verticalBarsOfThicknessTwoAndFourTileOnTheGrid() {
    final var bar = new LedBar();
    final var attrs = bar.createAttributeSet();
    attrs.setValue(StdAttr.FACING, Direction.NORTH);

    attrs.setValue(LedBar.ATTR_THICKNESS, 2);
    final var narrow = bar.getOffsetBounds(attrs);
    assertEquals(20, narrow.getWidth());
    assertEquals(0, narrow.getX() + narrow.getWidth());

    attrs.setValue(LedBar.ATTR_THICKNESS, 4);
    final var wide = bar.getOffsetBounds(attrs);
    assertEquals(40, wide.getWidth());
    assertEquals(0, wide.getX() + wide.getWidth());

    final var origin = Location.create(200, 80, true);
    final var comp = bar.createComponent(origin, attrs);
    assertEquals(Location.create(200, 80, false), comp.getEnd(0).getLocation());
    assertEquals(Location.create(200, 80 - 70, false), comp.getEnd(7).getLocation());
  }

  @Test
  void facingRotatesSeparatedPinsAroundTheAnchor() {
    final var bar = new LedBar();
    final var attrs = bar.createAttributeSet();
    final var origin = Location.create(50, 60, true);
    final var comp = bar.createComponent(origin, attrs);

    attrs.setValue(StdAttr.FACING, Direction.WEST);
    assertEquals(Location.create(50, 60, false), comp.getEnd(0).getLocation());
    assertEquals(Location.create(50 - 70, 60, false), comp.getEnd(7).getLocation());
    assertEquals(Bounds.create(-75, 0, 80, 30), bar.getOffsetBounds(attrs));
  }

  @Test
  void oneWirePortStaysOnTheAnchorForTheDefaultFacing() {
    final var bar = new LedBar();
    final var attrs = bar.createAttributeSet();
    attrs.setValue(LedBar.ATTR_INPUT_TYPE, LedBar.INPUT_ONE_WIRE);

    assertEquals(Bounds.create(0, -15, 80, 30), bar.getOffsetBounds(attrs));
    final var comp = bar.createComponent(Location.create(10, 20, true), attrs);
    assertEquals(1, comp.getEnds().size());
    assertEquals(Location.create(10, 20, false), comp.getEnd(0).getLocation());
  }
}
