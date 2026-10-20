/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.data.Location;

public interface ExpressionComputer {
  /**
   * Propagates expression computation through a circuit. The parameter is a map from <code>Point
   * </code>s to <code>Expression</code>s. The method will use this to determine the expressions
   * coming into the component, and it should place any output expressions into the component.
   *
   * <p>If, in fact, no valid expression exists for the component, it throws <code>
   * UnsupportedOperationException</code>.
   */
  void computeExpression(Map expressionMap);

  interface Map {
    Expression get(Location point, int bit);

    Expression put(Location point, int bit, Expression expression);
  }
}
