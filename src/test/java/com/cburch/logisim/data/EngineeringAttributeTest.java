/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.util.StringGetter;
import java.util.Random;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

/** Parsing and display rules shared by resistance and capacitance attributes. */
class EngineeringAttributeTest {
  private static final StringGetter LABEL =
      new StringGetter() {
        @Override
        public String toString() {
          return "value";
        }
      };

  @Test
  void kiloohmAcceptsThe74123Forms() {
    final var resistor = kiloohm();
    assertEquals(10, resistor.parse("10"));
    assertEquals(10, resistor.parse("10k"));
    assertEquals(10, resistor.parse("10 kΩ"));
    assertEquals(10, resistor.parse("1.0e4"));
    assertEquals(10, resistor.parse("10.e3"));
    assertEquals(10, resistor.parse("10000 ohm"));
    assertEquals(10, resistor.parse("10kohm"));
    assertEquals(10, resistor.parse("10 kOhms"));
    assertEquals(10, resistor.parse("10000 OHMS"));
    assertEquals(1000, resistor.parse("1M"));
    assertEquals(1000, resistor.parse("1meg"));
    assertEquals(1000, resistor.parse("1MegOhm"));
    assertEquals("10 kΩ", resistor.toDisplayString(10));
    assertEquals("1 MΩ", resistor.toDisplayString(1000));
    assertEquals("10", resistor.toStandardString(10));
    assertEquals("", resistor.toStandardString(null));
    assertEquals("", resistor.toDisplayString(null));
  }

  @Test
  void picofaradAcceptsThe74123Forms() {
    final var capacitor = picofarad();
    assertEquals(100_000, capacitor.parse("100000"));
    assertEquals(100_000, capacitor.parse("100n"));
    assertEquals(100_000, capacitor.parse("100 nF"));
    assertEquals(1_000_000, capacitor.parse("1u"));
    assertEquals(1_000_000, capacitor.parse("1 u"));
    assertEquals(1_000_000, capacitor.parse("1µF"));
    assertEquals(1_000_000, capacitor.parse("1μF"));
    assertEquals(1_000_000, capacitor.parse("1.0e-6"));
    assertEquals(6_666_667, capacitor.parse("6.666667u"));
    assertEquals(10_000, capacitor.parse("10000000f"));
    assertEquals(10_000, capacitor.parse("10000000fF"));
    assertEquals(10_000_000, capacitor.parse("10000000p"));
    assertEquals(1_000_000_000, capacitor.parse("1m"));
    assertEquals(1_000_000_000, capacitor.parse("1mF"));
    assertEquals("100 nF", capacitor.toDisplayString(100_000));
    assertEquals("10.001 nF", capacitor.toDisplayString(10_001));
    assertEquals("999.999 nF", capacitor.toDisplayString(999_999));
    assertEquals("1 µF", capacitor.toDisplayString(1_000_000));
    assertEquals("1.5 µF", capacitor.toDisplayString(1_500_000));
    assertEquals("6.666667 µF", capacitor.toDisplayString(6_666_667));
    assertEquals("1000 µF", capacitor.toDisplayString(1_000_000_000));
  }

  @Test
  void ohmAndMicrofaradAcceptTheNe555Forms() {
    final var resistor = ohm();
    final var capacitor = microfarad();
    assertEquals(71_660, resistor.parse("71660"));
    assertEquals(71_660, resistor.parse("71.66k"));
    assertEquals(71_660, resistor.parse("71.66 kΩ"));
    assertEquals(1000, resistor.parse("1k"));
    assertEquals(10_000_000, resistor.parse("10M"));
    assertEquals("71.66 kΩ", resistor.toDisplayString(71_660));
    assertEquals("1 kΩ", resistor.toDisplayString(1000));
    assertEquals("10 MΩ", resistor.toDisplayString(10_000_000));
    assertEquals("999.999 kΩ", resistor.toDisplayString(999_999));
    assertEquals("1 MΩ", resistor.toDisplayString(1_000_000));
    assertEquals("1.000001 MΩ", resistor.toDisplayString(1_000_001));
    assertEquals(10, capacitor.parse("10"));
    assertEquals(10, capacitor.parse("10u"));
    assertEquals(10, capacitor.parse("10µF"));
    assertEquals(10, capacitor.parse("10.0u"));
    assertEquals(1000, capacitor.parse("1mF"));
    assertEquals("10 µF", capacitor.toDisplayString(10));
    assertEquals("1000 µF", capacitor.toDisplayString(1000));
    assertThrows(NumberFormatException.class, () -> capacitor.parse("10.5µF"));
    assertThrows(NumberFormatException.class, () -> capacitor.parse("10.5u"));
    assertThrows(NumberFormatException.class, () -> resistor.parse("71.6666k"));
  }

  @Test
  void otherStorageUnitsRoundTrip() {
    final var megohm =
        Attributes.forEngineering("meg", LABEL, EngineeringAttribute.Unit.MEGOHM, 1, 1000, null);
    final var nanofarad =
        Attributes.forEngineering(
            "nano", LABEL, EngineeringAttribute.Unit.NANOFARAD, 1, 1000, null);
    assertEquals(1, megohm.parse("1"));
    assertEquals(1, megohm.parse("1M"));
    assertEquals(1, megohm.parse("1000k"));
    assertEquals("1 MΩ", megohm.toDisplayString(1));
    assertEquals("10 MΩ", megohm.toDisplayString(10));
    assertEquals("1000 MΩ", megohm.toDisplayString(1000));
    assertEquals(100, nanofarad.parse("100"));
    assertEquals(100, nanofarad.parse("100n"));
    assertEquals(100, nanofarad.parse("0.1u"));
    assertEquals(1000, nanofarad.parse("1u"));
    assertEquals("100 nF", nanofarad.toDisplayString(100));
    assertEquals("1 µF", nanofarad.toDisplayString(1000));
    assertRoundTrip(megohm, 1);
    assertRoundTrip(megohm, 10);
    assertRoundTrip(megohm, 1000);
    assertRoundTrip(nanofarad, 1);
    assertRoundTrip(nanofarad, 100);
    assertRoundTrip(nanofarad, 1000);
  }

  @Test
  void rejectsSyntaxThatDoesNotLandOnTheStoredUnit() {
    final var resistor = kiloohm();
    final var capacitor = picofarad();
    assertRejected(resistor, "10.4k");
    assertRejected(resistor, "2.5k");
    assertRejected(resistor, "1");
    assertRejected(resistor, "10K");
    assertRejected(resistor, "10farad");
    assertRejected(resistor, "100nF");
    assertRejected(resistor, "0x1p3");
    assertRejected(resistor, "10d");
    assertRejected(resistor, "10D");
    assertRejected(resistor, "11,38");
    assertRejected(resistor, "abc");
    assertRejected(resistor, "");
    assertRejected(resistor, null);
    assertRejected(capacitor, "1n");
    assertRejected(capacitor, "100nf");
    assertRejected(capacitor, "10000000ff");
    assertRejected(capacitor, "1U");
    assertRejected(capacitor, "100N");
    assertRejected(capacitor, "10000000P");
    assertRejected(capacitor, "10000000F");
    assertRejected(capacitor, "10ohm");
    assertRejected(capacitor, "10kΩ");
    assertOutOfRange(resistor, "1");
    assertOutOfRange(resistor, "1k");
    assertOutOfRange(resistor, "2M");
    assertSyntax(resistor, "abc");
    assertSyntax(capacitor, "abc");
  }

  @Test
  void displayAndFileTextRoundTripAcrossTheRanges() {
    final var resistor = kiloohm();
    for (var stored = 2; stored <= 1000; stored++) {
      assertRoundTrip(resistor, stored);
    }
    final var capacitor = microfarad();
    for (var stored = 10; stored <= 1000; stored++) {
      assertRoundTrip(capacitor, stored);
    }
    final var ohms = ohm();
    for (var stored = 1000; stored <= 2500; stored++) {
      assertRoundTrip(ohms, stored);
    }
    for (var stored = 999_000; stored <= 1_001_000; stored++) {
      assertRoundTrip(ohms, stored);
    }
    assertRoundTrip(ohms, 71_660);
    assertRoundTrip(ohms, 9_999_999);
    assertRoundTrip(ohms, 10_000_000);
    final var picofarads = picofarad();
    for (var stored = 10_000; stored <= 10_500; stored++) {
      assertRoundTrip(picofarads, stored);
    }
    for (var stored = 999_990; stored <= 1_000_010; stored++) {
      assertRoundTrip(picofarads, stored);
    }
    assertRoundTrip(picofarads, 1_500_000);
    assertRoundTrip(picofarads, 6_666_667);
    assertRoundTrip(picofarads, 1_000_000_000);
    sample(ohms, 1000, 10_000_000);
    sample(picofarads, 10_000, 1_000_000_000);
  }

  @Test
  void presetsUseAComboAndAPlainFieldDoesNot() {
    assertInstanceOf(ComboBox.class, kiloohm().getCellEditor(10));
    assertInstanceOf(ComboBox.class, kiloohm().getCellEditor(11));
    final var plain =
        Attributes.forEngineering("plain", LABEL, EngineeringAttribute.Unit.OHM, 1, 5, null);
    assertInstanceOf(JTextField.class, plain.getCellEditor(1));
  }

  @Test
  void constructorRejectsAnInvertedRangeAndAPresetOutsideIt() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Attributes.forEngineering(
                "bad", LABEL, EngineeringAttribute.Unit.OHM, 5, 1, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Attributes.forEngineering(
                "bad", LABEL, EngineeringAttribute.Unit.OHM, 2, 10, new int[] {1}));
  }

  private static EngineeringAttribute kiloohm() {
    return Attributes.forEngineering(
        "r", LABEL, EngineeringAttribute.Unit.KILOOHM, 2, 1000, new int[] {2, 10, 100, 1000});
  }

  private static EngineeringAttribute picofarad() {
    return Attributes.forEngineering(
        "c",
        LABEL,
        EngineeringAttribute.Unit.PICOFARAD,
        10_000,
        1_000_000_000,
        new int[] {10_000, 100_000});
  }

  private static EngineeringAttribute ohm() {
    return Attributes.forEngineering(
        "r1", LABEL, EngineeringAttribute.Unit.OHM, 1000, 10_000_000, null);
  }

  private static EngineeringAttribute microfarad() {
    return Attributes.forEngineering(
        "c1", LABEL, EngineeringAttribute.Unit.MICROFARAD, 10, 1000, null);
  }

  private static void assertRoundTrip(Attribute<Integer> attribute, int stored) {
    assertEquals(stored, attribute.parse(attribute.toDisplayString(stored)), stored);
    assertEquals(stored, attribute.parse(attribute.toStandardString(stored)), stored);
  }

  private static void sample(Attribute<Integer> attribute, int minimum, int maximum) {
    final var random = new Random(3084);
    final var span = (long) maximum - minimum + 1;
    for (var i = 0; i < 2000; i++) {
      final var stored = minimum + (int) random.nextLong(span);
      assertRoundTrip(attribute, stored);
    }
    assertRoundTrip(attribute, minimum);
    assertRoundTrip(attribute, maximum);
  }

  private static void assertRejected(Attribute<Integer> attribute, String text) {
    assertThrows(NumberFormatException.class, () -> attribute.parse(text));
  }

  private static void assertOutOfRange(Attribute<Integer> attribute, String text) {
    final var error = assertThrows(NumberFormatException.class, () -> attribute.parse(text));
    assertTrue(error.getMessage().contains("2 kΩ"), error.getMessage());
    assertTrue(error.getMessage().contains("1 MΩ"), error.getMessage());
  }

  private static void assertSyntax(Attribute<Integer> attribute, String text) {
    final var error = assertThrows(NumberFormatException.class, () -> attribute.parse(text));
    assertTrue(error.getMessage().contains(text), error.getMessage());
    assertFalse(error.getMessage().contains("2 kΩ"), error.getMessage());
  }
}
