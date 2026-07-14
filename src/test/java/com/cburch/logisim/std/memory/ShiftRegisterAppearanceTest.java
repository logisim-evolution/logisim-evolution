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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;

class ShiftRegisterAppearanceTest {

  @Test
  void stageValueBoundsStayInsideDataBlockForEverySupportedWidth() {
    for (var width = 1; width <= 64; width++) {
      final var bounds = ShiftRegister.getStageValueBounds(width);

      assertTrue(bounds.getX() >= 0, "left edge for width " + width);
      assertTrue(
          bounds.getX() + bounds.getWidth() <= ShiftRegister.symbolWidth,
          "right edge for width " + width);
      assertEquals(2, bounds.getY());
      assertEquals(16, bounds.getHeight());
    }
  }

  @Test
  void stageValueBoundsPreserveNarrowLayoutAndFitNineDigits() {
    assertBounds(ShiftRegister.getStageValueBounds(1), 61, 2, 10, 16);
    assertBounds(ShiftRegister.getStageValueBounds(32), 33, 2, 66, 16);
    assertBounds(ShiftRegister.getStageValueBounds(36), 26, 2, 74, 16);
  }

  @Test
  void stageValueTextPreservesValuesThroughNineHexDigits() {
    assertEquals(
        "89abcdef",
        ShiftRegister.getStageValueText(
            32, Value.createKnown(BitWidth.create(32), 0x89ABCDEFL)));
    assertEquals(
        "123456789",
        ShiftRegister.getStageValueText(
            36, Value.createKnown(BitWidth.create(36), 0x123456789L)));
  }

  @Test
  void stageValueTextSummarizesLowOrderDigitsAboveNineHexDigits() {
    assertEquals(
        "..fffffff",
        ShiftRegister.getStageValueText(
            37, Value.createKnown(BitWidth.create(37), 0x1FFFFFFFFFL)));
    assertEquals(
        "..456789a",
        ShiftRegister.getStageValueText(
            40, Value.createKnown(BitWidth.create(40), 0x123456789AL)));
    assertEquals(
        "..9abcdef",
        ShiftRegister.getStageValueText(
            64, Value.createKnown(BitWidth.create(64), 0x0123456789ABCDEFL)));
  }

  @Test
  void stageValueTextPreservesUnknownAndErrorMarkers() {
    assertEquals(
        "?", ShiftRegister.getStageValueText(64, Value.createUnknown(BitWidth.create(64))));
    assertEquals(
        "!", ShiftRegister.getStageValueText(64, Value.createError(BitWidth.create(64))));
  }

  @Test
  void wideEvolutionAppearanceKeepsBoundsAndPortsStable() {
    final var instance = createEvolutionShiftRegisterInstance(64);

    assertEquals(120, instance.getBounds().getWidth());
    assertEquals(240, instance.getBounds().getHeight());
    assertPortLocation(instance, ShiftRegister.IN, 0, 80);
    assertPortLocation(instance, ShiftRegister.OUT, 120, 230);
    assertPortLocation(instance, ShiftRegister.CK, 0, 50);
    assertPortLocation(instance, ShiftRegister.CLR, 0, 20);
    assertPortLocation(instance, ShiftRegister.SH, 0, 40);
    assertPortLocation(instance, ShiftRegister.LD, 0, 30);
    assertPortLocation(instance, 6, 0, 90);
    assertPortLocation(instance, 7, 120, 90);
  }

  private static Instance createEvolutionShiftRegisterInstance(int width) {
    final var shiftRegister = new ShiftRegister();
    final var attrs = shiftRegister.createAttributeSet();
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
    attrs.setValue(ShiftRegister.ATTR_LENGTH, 8);
    attrs.setValue(ShiftRegister.ATTR_LOAD, true);
    attrs.setValue(StdAttr.APPEARANCE, StdAttr.APPEAR_EVOLUTION);
    return Instance.getInstanceFor(
        shiftRegister.createComponent(Location.create(0, 0, false), attrs));
  }

  private static void assertBounds(Bounds bounds, int x, int y, int width, int height) {
    assertEquals(x, bounds.getX());
    assertEquals(y, bounds.getY());
    assertEquals(width, bounds.getWidth());
    assertEquals(height, bounds.getHeight());
  }

  private static void assertPortLocation(Instance instance, int portIndex, int x, int y) {
    final var location = instance.getPortLocation(portIndex);
    assertEquals(x, location.getX());
    assertEquals(y, location.getY());
  }
}
