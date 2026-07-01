/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;

class CounterAppearanceTest {

  @Test
  void compactDataRowsPreserveOneBitRowsForSmallCounters() {
    assertEquals(1, Counter.getBitsPerDataRow(8));
    assertEquals(8, Counter.getDataRowCount(8));
    assertEquals("0", Counter.getDataRowLabel(0, 8));
    assertEquals("7", Counter.getDataRowLabel(7, 8));
  }

  @Test
  void compactDataRowsGroupWideCountersIntoAtMostEightRows() {
    assertEquals(8, Counter.getBitsPerDataRow(64));
    assertEquals(8, Counter.getDataRowCount(64));
    assertEquals("0-7", Counter.getDataRowLabel(0, 64));
    assertEquals("56-63", Counter.getDataRowLabel(7, 64));
  }

  @Test
  void compactDataRowsHideDuplicateOutputRangeLabelsForGroupedRows() {
    assertEquals("7", Counter.getOutputDataRowLabel(7, 8));
    assertEquals("", Counter.getOutputDataRowLabel(7, 64));
  }

  @Test
  void compactDataRowsHideRepeatedControlTextForGroupedRows() {
    assertEquals("1,6D", Counter.getDataRowControlText(8));
    assertEquals("", Counter.getDataRowControlText(64));
  }

  @Test
  void compactEvolutionBoundsUseGroupedRowCount() {
    assertEquals(270, Counter.getEvolutionHeight(64));
  }

  @Test
  void compactEvolutionBoundsKeepWideCounterPortsStable() {
    final var instance = createEvolutionCounterInstance(64);

    assertEquals(300, instance.getBounds().getWidth());
    assertEquals(270, instance.getBounds().getHeight());
    assertPortLocation(instance, Counter.IN, 0, 110);
    assertPortLocation(instance, Counter.OUT, 300, 110);
    assertPortLocation(instance, Counter.CK, 0, 80);
    assertPortLocation(instance, Counter.CLR, 0, 20);
    assertPortLocation(instance, Counter.LD, 0, 30);
    assertPortLocation(instance, Counter.UD, 0, 50);
    assertPortLocation(instance, Counter.EN, 0, 70);
    assertPortLocation(instance, Counter.CARRY, 300, 50);
  }

  @Test
  void compactDataRowValueShowsGroupedBitsFromLowToHigh() {
    final var value = Value.createKnown(BitWidth.create(16), 0x5A3C);

    assertEquals("1100", Counter.getDataRowValue(value, 0, 16));
    assertEquals("0011", Counter.getDataRowValue(value, 1, 16));
    assertEquals("1010", Counter.getDataRowValue(value, 2, 16));
    assertEquals("0101", Counter.getDataRowValue(value, 3, 16));
  }

  @Test
  void compactDataRowValuePreservesMixedBitStates() {
    final var value =
        Value.create(
            new Value[] {
              Value.TRUE,
              Value.FALSE,
              Value.UNKNOWN,
              Value.ERROR,
              Value.FALSE,
              Value.TRUE,
              Value.FALSE,
              Value.TRUE,
              Value.FALSE,
              Value.FALSE,
              Value.FALSE,
              Value.FALSE,
              Value.FALSE,
              Value.FALSE,
              Value.FALSE,
              Value.TRUE
            });

    assertEquals("!?01", Counter.getDataRowValue(value, 0, 16));
    assertEquals("1010", Counter.getDataRowValue(value, 1, 16));
  }

  private static Instance createEvolutionCounterInstance(int width) {
    final var counter = new Counter();
    final var attrs = counter.createAttributeSet();
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
    attrs.setValue(StdAttr.APPEARANCE, StdAttr.APPEAR_EVOLUTION);
    return Instance.getInstanceFor(counter.createComponent(Location.create(0, 0, false), attrs));
  }

  private static void assertPortLocation(Instance instance, int portIndex, int x, int y) {
    final var location = instance.getPortLocation(portIndex);
    assertEquals(x, location.getX());
    assertEquals(y, location.getY());
  }
}
