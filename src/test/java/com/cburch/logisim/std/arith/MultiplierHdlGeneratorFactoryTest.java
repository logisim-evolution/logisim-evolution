/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MultiplierHdlGeneratorFactoryTest {

  @Test
  void unsignedNumericTypeSetsUnsignedMultiplierGeneric() {
    final var parameters = parametersFor(Comparator.UNSIGNED_OPTION);

    assertEquals("1", parameters.get("unsignedMultiplier"));
  }

  @Test
  void twosComplementNumericTypeClearsUnsignedMultiplierGeneric() {
    final var parameters = parametersFor(Comparator.SIGNED_OPTION);

    assertEquals("0", parameters.get("unsignedMultiplier"));
  }

  private static Map<String, String> parametersFor(AttributeOption numericType) {
    final var attrs = new Multiplier().createAttributeSet();
    attrs.setValue(Comparator.MODE_ATTR, numericType);
    return new ExposedMultiplierHdlGeneratorFactory().parameterMap(attrs);
  }

  private static class ExposedMultiplierHdlGeneratorFactory extends MultiplierHdlGeneratorFactory {
    Map<String, String> parameterMap(AttributeSet attrs) {
      return myParametersList.getMaps(attrs);
    }
  }
}
