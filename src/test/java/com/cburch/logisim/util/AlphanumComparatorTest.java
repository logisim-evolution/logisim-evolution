package com.cburch.logisim.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlphanumComparatorTest {

  final List<String> values =
      Arrays.asList(
          "dazzle2", "dazzle10", "dazzle1", "dazzle2.7", "dazzle2.10",
          "2", "10", "1",
          "EctoMorph6", "EctoMorph62", "EctoMorph7"
      );

  final List<String> expected =
      Arrays.asList(
          "1", "2", "10",
          "EctoMorph6", "EctoMorph7", "EctoMorph62",
          "dazzle1", "dazzle2", "dazzle2.7", "dazzle2.10", "dazzle10"
      );

  /**
   * Tests if AlphanumComparator works for sorting.
   */
  @Test
  public void testWithSort() {
    final var result = values.stream().sorted(new AlphanumComparator()).collect(Collectors.toList());
    assertEquals(expected, result);
  }
}
