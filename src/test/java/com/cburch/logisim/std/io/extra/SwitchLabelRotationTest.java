/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.StdAttr;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SwitchLabelRotationTest {
  private static final Switch SWITCH = new Switch();
  private static final Location LOCATION = Location.create(50, 50, false);

  @Test
  void rotateLabelsDefaultsToNo() {
    assertEquals(Boolean.FALSE, SWITCH.createAttributeSet().getValue(Switch.ATTR_ROTATE_LABELS));
  }

  @Test
  void artworkRotatesOnlyForSouthAndWest() {
    for (final var facing : Direction.cardinals) {
      assertFalse(Switch.rotatesArtwork(facing, false), facing.toString());
    }
    assertFalse(Switch.rotatesArtwork(Direction.EAST, true));
    assertFalse(Switch.rotatesArtwork(Direction.NORTH, true));
    assertTrue(Switch.rotatesArtwork(Direction.WEST, true));
    assertTrue(Switch.rotatesArtwork(Direction.SOUTH, true));
  }

  @Test
  void rotatingLabelsLeavesPortsInPlace() {
    for (final var facing : Direction.cardinals) {
      final var fixed = Instance.getInstanceFor(component(facing, false));
      final var turned = Instance.getInstanceFor(component(facing, true));
      assertEquals(fixed.getPortLocation(0), turned.getPortLocation(0), facing.toString());
      assertEquals(fixed.getPortLocation(1), turned.getPortLocation(1), facing.toString());
    }
  }

  @Test
  void rotationChangesOnlySouthAndWestArtwork() {
    for (final var facing : Direction.cardinals) {
      final var fixed = pixels(paint(facing, false));
      final var turned = pixels(paint(facing, true));
      if (Switch.rotatesArtwork(facing, true)) {
        assertFalse(Arrays.equals(fixed, turned), facing.toString());
      } else {
        assertArrayEquals(fixed, turned, facing.toString());
      }
    }
  }

  private static Component component(Direction facing, boolean rotateLabels) {
    final var attrs = attributes(facing, rotateLabels);
    return SWITCH.createComponent(LOCATION, attrs);
  }

  private static AttributeSet attributes(Direction facing, boolean rotateLabels) {
    final var attrs = SWITCH.createAttributeSet();
    attrs.setValue(StdAttr.FACING, facing);
    attrs.setValue(Switch.ATTR_ROTATE_LABELS, rotateLabels);
    return attrs;
  }

  private static BufferedImage paint(Direction facing, boolean rotateLabels) {
    final var component = (InstanceComponent) component(facing, rotateLabels);
    final var image = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);
    final var graphics = image.createGraphics();
    try {
      final var context = new ComponentDrawContext(null, null, null, graphics, graphics, true);
      SWITCH.paintInstance(new InstancePainter(context, component));
      return image;
    } finally {
      graphics.dispose();
    }
  }

  private static int[] pixels(BufferedImage image) {
    return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
  }
}
