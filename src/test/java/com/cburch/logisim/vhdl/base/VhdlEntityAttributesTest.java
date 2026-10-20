/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;

class VhdlEntityAttributesTest {
  @Test
  void appearanceBelongsToVhdlContentAttributesNotInstanceAttributes() {
    final var content = VhdlContent.create("VhdlEntityTest", null);
    final var instanceAttrs = new VhdlEntityAttributes(content);

    assertFalse(instanceAttrs.getAttributes().contains(StdAttr.APPEARANCE));
    assertTrue(content.getStaticAttributes().getAttributes().contains(StdAttr.APPEARANCE));

    content.getStaticAttributes().setValue(StdAttr.APPEARANCE, StdAttr.APPEAR_CLASSIC);

    assertSame(StdAttr.APPEAR_CLASSIC, content.getAppearance());
    assertSame(StdAttr.APPEAR_CLASSIC, instanceAttrs.getValue(StdAttr.APPEARANCE));
  }

  @Test
  void legacyProgrammaticInstanceAppearanceWritesStillUpdateContent() {
    final var content = VhdlContent.create("VhdlEntityLegacyTest", null);
    final var instanceAttrs = new VhdlEntityAttributes(content);

    instanceAttrs.setValue(StdAttr.APPEARANCE, StdAttr.APPEAR_CLASSIC);

    assertSame(StdAttr.APPEAR_CLASSIC, content.getAppearance());
  }

  @Test
  void clonedVhdlContentAttributesPointToClone() {
    final var content = VhdlContent.create("VhdlEntityCloneTest", null);
    content.setAppearance(StdAttr.APPEAR_CLASSIC);

    final var cloned = content.clone();
    cloned.getStaticAttributes().setValue(StdAttr.APPEARANCE, StdAttr.APPEAR_FPGA);

    assertSame(StdAttr.APPEAR_CLASSIC, content.getAppearance());
    assertSame(StdAttr.APPEAR_FPGA, cloned.getAppearance());
  }
}
