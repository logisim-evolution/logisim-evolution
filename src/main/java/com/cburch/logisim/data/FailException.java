/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
public class FailException extends TestException {

  private static final long serialVersionUID = 1L;
  private final int column;
  private final Value expected;
  private final Value computed;
  private final ArrayList<FailException> more = new ArrayList<>();

  public FailException(int column, String columnName, Value expected, Value computed, String msg) {
    super(
        columnName
            + " = "
            + computed.toDisplayString(2)
            + " (expected "
            + expected.toDisplayString(2)
            + ") "
            + msg);
    this.column = column;
    this.expected = expected;
    this.computed = computed;
  }

  public FailException(int column, String columnName, Value expected, Value computed) {
    this(column, columnName, expected, computed, "");
  }


  public FailException add(FailException another) {
    more.add(another);
    more.addAll(another.getMore());
    another.clearMore();
    return this;
  }

  public int getColumn() {
    return column;
  }

  public Value getComputed() {
    return computed;
  }

  public Value getExpected() {
    return expected;
  }

  public List<FailException> getMore() {
    return more;
  }

  public void clearMore() {
    more.clear();
  }

  public List<FailException> getAll() {
    final var ret = new ArrayList<FailException>();
    ret.add(this);
    ret.addAll(more);
    return ret;
  }
}
