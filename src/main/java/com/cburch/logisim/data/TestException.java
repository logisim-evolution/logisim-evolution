/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

/**
 * A checked exception for representing vector tests exceptional circumstances.
 * <p>
 * Code taken from
 * <a href=" http://www.cs.cornell.edu/courses/cs3410/2015sp/">Cornell's version of Logisim</a>
 *
 * @see FailException
 */
public class TestException extends Exception {

  private static final long serialVersionUID = 1L;

  public TestException(String s) {
    super(s);
  }
}
