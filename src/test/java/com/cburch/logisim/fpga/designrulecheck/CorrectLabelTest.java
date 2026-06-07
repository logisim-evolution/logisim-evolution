/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CorrectLabelTest {
  @Test
  void hdlLabelKeysRepresentNormalizedCaseInsensitiveIdentity() {
    assertEquals(CorrectLabel.hdlLabelKey("A"), CorrectLabel.hdlLabelKey("a"));
    assertEquals(CorrectLabel.hdlLabelKey("A B"), CorrectLabel.hdlLabelKey("a-b"));
    assertEquals("L_1A", CorrectLabel.getCorrectLabel("1A"));
    assertEquals("L_1A", CorrectLabel.hdlLabelKey("1a"));
  }

  @Test
  void hdlNameKeysOnlyFoldCaseForAlreadyGeneratedHdlNames() {
    assertEquals(CorrectLabel.hdlNameKey("ComponentA"), CorrectLabel.hdlNameKey("componenta"));
    assertEquals("A-B", CorrectLabel.hdlNameKey("a-b"));
  }
}
