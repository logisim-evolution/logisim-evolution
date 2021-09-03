/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.Value;

class GateFunctions {
  static Value computeAnd(Value[] inputs, int numInputs) {
    var ret = inputs[0];
    for (int i = 1; i < numInputs; i++) {
      ret = ret.and(inputs[i]);
    }
    return ret;
  }

  static Value computeExactlyOne(Value[] inputs, int numInputs) {
    final var width = inputs[0].getWidth();
    final var ret = new Value[width];
    for (var i = 0; i < width; i++) {
      var count = 0;
      for (var j = 0; j < numInputs; j++) {
        final var v = inputs[j].get(i);
        if (v == Value.TRUE) {
          count++;
        } else if (v == Value.FALSE) {
          // do nothing
        } else {
          count = -1;
          break;
        }
      }
      if (count < 0) {
        ret[i] = Value.ERROR;
      } else if (count == 1) {
        ret[i] = Value.TRUE;
      } else {
        ret[i] = Value.FALSE;
      }
    }
    return Value.create(ret);
  }

  static Value computeOddParity(Value[] inputs, int numInputs) {
    var ret = inputs[0];
    for (int i = 1; i < numInputs; i++) {
      ret = ret.xor(inputs[i]);
    }
    return ret;
  }

  static Value computeOr(Value[] inputs, int numInputs) {
    var ret = inputs[0];
    for (int i = 1; i < numInputs; i++) {
      ret = ret.or(inputs[i]);
    }
    return ret;
  }

  private GateFunctions() {}
}
