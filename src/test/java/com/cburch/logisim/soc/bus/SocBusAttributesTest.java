/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.bus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;

class SocBusAttributesTest {
  @Test
  void traceVisibilityCanBeDisabled() {
    final var attrs = new SocBusAttributes();

    assertTrue(attrs.getValue(SocBusAttributes.SOC_TRACE_VISIBLE));

    attrs.setValue(SocBusAttributes.SOC_TRACE_VISIBLE, false);

    assertFalse(attrs.getValue(SocBusAttributes.SOC_TRACE_VISIBLE));
  }

  @Test
  void labelVisibilityCanBeDisabled() {
    final var attrs = new SocBusAttributes();

    assertTrue(attrs.getValue(StdAttr.LABEL_VISIBILITY));

    attrs.setValue(StdAttr.LABEL_VISIBILITY, false);

    assertFalse(attrs.getValue(StdAttr.LABEL_VISIBILITY));
  }
}
