/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Canvas;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import org.junit.jupiter.api.Test;

class NumericConfiguratorTest {

  @Test
  void rejectsValuesOutsideOptionAttributeChoices() {
    final var attributes = attributes(StdAttr.FP_WIDTH, 32);
    final var configurator = new BitWidthConfigurator(StdAttr.FP_WIDTH);

    assertNull(type(configurator, attributes, '5'));
  }

  @Test
  void acceptsSingleAndMultiDigitOptionAttributeChoices() {
    final var attributes = attributes(StdAttr.FP_WIDTH, 32);

    assertEquals(
        BitWidth.create(8),
        configuredValue(StdAttr.FP_WIDTH, attributes, '8'));
    assertEquals(BitWidth.create(16), configuredValue(StdAttr.FP_WIDTH, attributes, '1', '6'));
    assertEquals(BitWidth.create(32), configuredValue(StdAttr.FP_WIDTH, attributes, '3', '2'));
    assertEquals(BitWidth.create(64), configuredValue(StdAttr.FP_WIDTH, attributes, '6', '4'));
  }

  @Test
  void continuesToAcceptValuesOfNonOptionAttributes() {
    final Attribute<BitWidth> width = Attributes.forBitWidth("width");
    final var attributes = attributes(width, 1);

    assertEquals(BitWidth.create(5), configuredValue(width, attributes, '5'));
  }

  private static AttributeSet attributes(Attribute<BitWidth> attribute, int initialWidth) {
    return AttributeSets.fixedSet(
        new Attribute<?>[] {attribute}, new Object[] {BitWidth.create(initialWidth)});
  }

  private static BitWidth configuredValue(
      Attribute<BitWidth> attribute, AttributeSet attributes, char... digits) {
    final var configurator = new BitWidthConfigurator(attribute);
    KeyConfigurationResult result = null;
    for (final var digit : digits) {
      result = type(configurator, attributes, digit);
    }
    assertNotNull(result);
    return (BitWidth) result.getAttributeValues().get(attribute);
  }

  private static KeyConfigurationResult type(
      BitWidthConfigurator configurator, AttributeSet attributes, char digit) {
    final var keyEvent =
        new KeyEvent(
            new Canvas(),
            KeyEvent.KEY_TYPED,
            System.currentTimeMillis(),
            InputEvent.ALT_DOWN_MASK,
            KeyEvent.VK_UNDEFINED,
            digit);
    final var configurationEvent =
        new KeyConfigurationEvent(
            KeyConfigurationEvent.KEY_TYPED, attributes, keyEvent, null);
    return configurator.keyEventReceived(configurationEvent);
  }
}
