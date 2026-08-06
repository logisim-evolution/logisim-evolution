/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImageShapeTest {

  @Test
  void testImageShapeAttributesAndResizing() {
    final var shape = new ImageShape(10, 20, 100, 50);

    assertTrue(shape.getAttributes().contains(ImageShape.IMAGE_ATTR));
    assertTrue(shape.getAttributes().contains(ImageShape.WIDTH_ATTR));
    assertTrue(shape.getAttributes().contains(ImageShape.HEIGHT_ATTR));

    assertEquals(10, shape.getX());
    assertEquals(20, shape.getY());
    assertEquals(100, shape.getWidth());
    assertEquals(50, shape.getHeight());

    assertEquals(100, shape.getValue(ImageShape.WIDTH_ATTR));
    assertEquals(50, shape.getValue(ImageShape.HEIGHT_ATTR));

    shape.setValue(ImageShape.WIDTH_ATTR, 200);
    shape.setValue(ImageShape.HEIGHT_ATTR, 150);

    assertEquals(10, shape.getX());
    assertEquals(20, shape.getY());
    assertEquals(200, shape.getWidth());
    assertEquals(150, shape.getHeight());

    assertEquals(200, shape.getValue(ImageShape.WIDTH_ATTR));
    assertEquals(150, shape.getValue(ImageShape.HEIGHT_ATTR));
  }
}
