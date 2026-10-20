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
import static org.junit.jupiter.api.Assertions.assertSame;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import org.junit.jupiter.api.Test;

class VideoColorModelTest {

  @Test
  void rgb444ExpandsEachChannelToEightBits() {
    final var model = Video.getColorModel(Video.COLOR_444_RGB);

    assertEquals(12, model.getPixelSize());
    assertEquals(0xFF000000, model.getRGB(0x000));
    assertEquals(0xFFFF0000, model.getRGB(0xF00));
    assertEquals(0xFF00FF00, model.getRGB(0x0F0));
    assertEquals(0xFF0000FF, model.getRGB(0x00F));
    assertEquals(0xFFAABBCC, model.getRGB(0xABC));
  }

  @Test
  void rgb444OptionRoundTripsThroughAttributeSerialization() {
    assertSame(Video.COLOR_444_RGB, Video.COLOR_OPTION.parse(Video.COLOR_444_RGB));
    assertEquals(
        Video.COLOR_444_RGB, Video.COLOR_OPTION.toStandardString(Video.COLOR_444_RGB));
  }

  @Test
  void selectingRgb444ChangesDataPortToTwelveBits() {
    final var attrs = Video.factory.createAttributeSet();
    final var component =
        Video.factory.createComponent(Location.create(100, 100, false), attrs);

    assertEquals(BitWidth.create(24), component.getEnd(Video.P_DATA).getWidth());

    attrs.setValue(Video.COLOR_OPTION, Video.COLOR_444_RGB);

    assertEquals(BitWidth.create(12), component.getEnd(Video.P_DATA).getWidth());
  }
}
