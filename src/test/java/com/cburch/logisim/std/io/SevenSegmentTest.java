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

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;
import java.util.List;
import org.junit.jupiter.api.Test;

class SevenSegmentTest {

  @Test
  void separatePinsKeepTheOriginalPositions() {
    final var attrs = attributes(true, false, Direction.NORTH);
    final var ends = placed(attrs).getEnds();
    assertEquals(8, ends.size());
    assertEquals(8, attrs.getValue(StdAttr.MAPINFO).getNrOfOutPorts());
    assertAt(ends, SevenSegment.Segment_A, 20, 0);
    assertAt(ends, SevenSegment.Segment_B, 30, 0);
    assertAt(ends, SevenSegment.Segment_C, 20, 60);
    assertAt(ends, SevenSegment.Segment_D, 10, 60);
    assertAt(ends, SevenSegment.Segment_E, 0, 60);
    assertAt(ends, SevenSegment.Segment_F, 10, 0);
    assertAt(ends, SevenSegment.Segment_G, 0, 0);
    assertAt(ends, SevenSegment.DP, 30, 60);
    for (final var end : ends) {
      assertEquals(BitWidth.ONE, end.getWidth());
      assertEquals(EndData.INPUT_ONLY, end.getType());
    }
  }

  @Test
  void hidingTheDecimalPointRemovesOnlyThatPin() {
    final var attrs = attributes(false, false, Direction.WEST);
    final var ends = placed(attrs).getEnds();
    assertEquals(7, ends.size());
    assertEquals(7, attrs.getValue(StdAttr.MAPINFO).getNrOfOutPorts());
    assertAt(ends, SevenSegment.Segment_G, 0, 0);
  }

  @Test
  void busSideChoosesOnePinAndKeepsTheDigitUpright() {
    assertEquals(10, SevenSegment.busHorizontalGridPoint());
    assertBus(true, Direction.WEST, 0, 30, 8);
    assertBus(true, Direction.EAST, 30, 30, 8);
    assertBus(true, Direction.NORTH, 10, 0, 8);
    assertBus(false, Direction.SOUTH, 10, 60, 7);
  }

  @Test
  void busModeKeepsTheOutlineAndLandsOnTheWiringGrid() {
    final var factory = new SevenSegment();
    final var separate = attributes(true, false, Direction.WEST);
    final var bus = attributes(true, true, Direction.NORTH);
    final var outline = Bounds.create(-5, 0, 40, 60);
    assertEquals(outline, factory.getOffsetBounds(separate));
    assertEquals(outline, factory.getOffsetBounds(bus));

    final var comp = placed(bus);
    assertEquals(outline, offsetOf(comp));
    final var end = comp.getEnds().get(0).getLocation();
    assertOnGrid(end.getX(), end.getY());
    assertEquals(10, end.getX());
    assertEquals(0, end.getY());

    bus.setValue(SevenSegment.ATTR_INPUT, SevenSegment.INPUT_SEPARATE);
    assertEquals(outline, offsetOf(comp));
    assertEquals(8, comp.getEnds().size());
  }

  @Test
  void changingTheBusSideMovesOnlyTheBusPin() {
    final var attrs = attributes(true, true, Direction.WEST);
    final var comp = placed(attrs);
    final var outline = Bounds.create(-5, 0, 40, 60);
    assertEquals(Location.create(0, 30, false), comp.getEnds().get(0).getLocation());
    assertEquals(outline, offsetOf(comp));
    attrs.setValue(SevenSegment.ATTR_BUS_SIDE, Direction.NORTH);
    assertEquals(1, comp.getEnds().size());
    assertEquals(Location.create(10, 0, false), comp.getEnds().get(0).getLocation());
    assertEquals(BitWidth.create(8), comp.getEnds().get(0).getWidth());
    assertEquals(outline, offsetOf(comp));
  }

  @Test
  void segmentMaskUsesPortOrderAndIgnoresUnknownBits() {
    final var bits =
        new Value[] {
          Value.TRUE,
          Value.FALSE,
          Value.TRUE,
          Value.UNKNOWN,
          Value.ERROR,
          Value.TRUE,
          Value.FALSE,
          Value.TRUE
        };
    assertEquals(0b10100101, SevenSegment.segmentMask(bits));
    final var bus = Value.create(bits);
    final var fromBus = new Value[8];
    for (var i = 0; i < fromBus.length; i++) fromBus[i] = bus.get(i);
    assertEquals(SevenSegment.segmentMask(bits), SevenSegment.segmentMask(fromBus));
  }

  private static Bounds offsetOf(Component comp) {
    return comp.getBounds().translate(-comp.getLocation().getX(), -comp.getLocation().getY());
  }

  private static void assertOnGrid(int x, int y) {
    assertEquals(0, Math.floorMod(x, SevenSegment.GRID));
    assertEquals(0, Math.floorMod(y, SevenSegment.GRID));
  }

  private static void assertBus(boolean decimalPoint, Direction side, int x, int y, int width) {
    final var attrs = attributes(decimalPoint, true, side);
    final var ends = placed(attrs).getEnds();
    assertEquals(1, ends.size());
    assertEquals(width, attrs.getValue(StdAttr.MAPINFO).getNrOfOutPorts());
    assertEquals(Location.create(x, y, false), ends.get(0).getLocation());
    assertEquals(BitWidth.create(width), ends.get(0).getWidth());
    assertEquals(EndData.INPUT_ONLY, ends.get(0).getType());
  }

  private static void assertAt(List<EndData> ends, int index, int x, int y) {
    assertEquals(Location.create(x, y, false), ends.get(index).getLocation());
  }

  private static AttributeSet attributes(boolean decimalPoint, boolean bus, Direction side) {
    final var attrs = new SevenSegment().createAttributeSet();
    attrs.setValue(SevenSegment.ATTR_DP, decimalPoint);
    attrs.setValue(SevenSegment.ATTR_INPUT, bus ? SevenSegment.INPUT_BUS : SevenSegment.INPUT_SEPARATE);
    attrs.setValue(SevenSegment.ATTR_BUS_SIDE, side);
    return attrs;
  }

  private static Component placed(AttributeSet attrs) {
    return new SevenSegment().createComponent(Location.create(0, 0, false), attrs);
  }
}
