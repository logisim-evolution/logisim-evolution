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
 * <code>FailException</code> is a specialized <code>TestException</code> class
 * intended for representing mismatched test vector values (failed tests).
 * It supports storing other <code>FailedException</code>s. however,
 * this does not categorize as an arbitrary exception tree, as the children of a
 * <code>FailedException</code> never have other children.
 *
 * <p>
 * Code taken from
 * <a href=" http://www.cs.cornell.edu/courses/cs3410/2015sp/">Cornell's version of Logisim</a>
 */
public class FailException extends TestException {

  private static final long serialVersionUID = 1L;
  private final int column;
  private final Value expected;
  private final Value computed;
  private final ArrayList<FailException> more = new ArrayList<>();

  /**
   * Initializes a <code>FailException</code> with the provided
   * @param column The index of the test vector column where the comparison test failed
   * @param columnName The name of said column
   * @param expected The expected test value
   * @param computed The actual computed value
   */
  public FailException(int column, String columnName, Value expected, Value computed) {
    super(
        columnName
            + " = "
            + computed.toDisplayString(2)
            + " (expected "
            + expected.toDisplayString(2)
            + ")");
    this.column = column;
    this.expected = expected;
    this.computed = computed;
  }

  /**
   * Adds a <code>FailException</code> to the children of this <code>FailException</code>.
   * The children of <code>another</code> are also added to <code>this</code>, and removed
   * from <code>another</code>.
   * @param another The exception child to be added
   */
  public void add(FailException another) {
    more.add(another);
    more.addAll(another.getMore());
    another.clearMore();
  }

  /**
   * @return The index of the test vector column where the comparison test failed.
   */
  public int getColumn() {
    return column;
  }

  /**
   * @return The computed/actual value of this failed test.
   */
  public Value getComputed() {
    return computed;
  }

  /**
   * @return The expected value of this failed test.
   */
  public Value getExpected() {
    return expected;
  }

  /**
   * @return The list of children exceptions to this exception.
   */
  public List<FailException> getMore() {
    return more;
  }

  /**
   * Removes all children from this exception.
   */
  public void clearMore() {
    more.clear();
  }

  /**
   * @return a list with this exception, alongside its children.
   */
  public List<FailException> getAll() {
    final var ret = new ArrayList<FailException>();
    ret.add(this);
    ret.addAll(more);
    return ret;
  }
}
