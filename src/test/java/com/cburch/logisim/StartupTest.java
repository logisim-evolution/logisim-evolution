/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import java.util.stream.Stream;
import com.cburch.logisim.gui.start.Startup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests command-line execution. */
public class StartupTest extends TestBase {

  /** Checks if version works. */
  @ParameterizedTest
  @MethodSource
  public void testVersion(String options) {
    try {
        String[] args = options.split(" ");
        var startup = new Startup(args);
        assertEquals(Startup.UI.NONE,startup.ui);
        assertEquals(0,startup.exitCode);
    }
    catch (Exception e) {
        fail(e.getMessage());
    }
  }
  static Stream<String> testVersion() {
    return Stream.of(
      "--version",
      "-v",
      "--help",
      "-h"
      );
  }

  /** Check various bad options. */
  @ParameterizedTest
  @MethodSource
  public void testCLIErrors(String options) {
    try {
      String[] args = options.split(" ");
      var startup = new Startup(args);
      assertEquals(Startup.UI.NONE,startup.ui);
      assertEquals(1,startup.exitCode);
    }
    catch (Exception e) {
        fail(e.getMessage());
    }
  }
  private static Stream<String> testCLIErrors() {
    return Stream.of(
      "--tty",                // missing required parameter
      "-t table a b",         // too many files for --tty
      "-t table",             // not enough files for --tty
      "--tty other-thing",    // sub-option does not exist
      "--load",               // missing required parameter
      "--load file",          // missing --tty
      "-l a -l b",            // can't load twice
      "--save",               // missing required parameter
      "--save file",          // missing --tty
      "--save a --save b",    // can't save twice
      "-s",                   // missing required parameters
      "-s a",                 // missing required parameter
      "-s a b -s a c",        // can't substitute the same thing twice
      "--gates",              // missing required parameter
      "--gates wrong",        // sub-option does not exist
      "-m",                   // missing required parameter
      "-m 123",               // bad format, missing 'x'
      "-m 0x0",               // bad size, must be positive
      "-m x",                 // bad format, missing sizes
      "-m 10x10+",            // bad format, missing location
      "--locale",             // missing require parameter
      "--locale wrong",       // sub-option does not exist
      "--user-template",      // missing required parameter
      "-u a -u b",            // can't template twice
      "--toplevel-circuit",   // missing required parameter
      "--test-vector",        // missing required parameters
      "--test-vector a",      // missing required parameter
      "--test-fpga",          // missing required parameters
      "--test-fpga a",        // missing required parameter
      "--test-fpga a b c d",  // too many files
      "--test-circuit",       // missing required parameter
      "--test-circuit a b",   // too many files
      "--new-file-format",    // missing required parameters
      "-n a",                 // missing required parameter
      "-n a b c",             // too many files
      "--does-not-exist"
    );
    // TODO combinations for --tty that don't make sense
    // TODO TTY options should be mutally exclusive
  }
}
