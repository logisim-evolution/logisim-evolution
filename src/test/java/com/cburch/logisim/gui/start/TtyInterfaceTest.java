/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.start;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.data.TestVector;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TtyInterfaceTest {

  @TempDir File tempDir;

  @Test
  public void testVectorHeaderIncludesBitWidth() {
    assertEquals("a[1]", TtyInterface.formatTestVectorHeader("a", 1));
    assertEquals("x[4]", TtyInterface.formatTestVectorHeader("x", 4));
  }

  @Test
  public void testVectorHeaderCanBeParsedByTestVector() throws IOException {
    File testFile = new File(tempDir, "tty-table-output.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write(
          TtyInterface.formatTestVectorHeader("a", 2)
              + " "
              + TtyInterface.formatTestVectorHeader("x", 4)
              + "\n");
      writer.write("00 0001\n");
      writer.write("11 1000\n");
    }

    TestVector vector = new TestVector(testFile);

    assertEquals("a", vector.columnName[0]);
    assertEquals(2, vector.columnWidth[0].getWidth());
    assertEquals("x", vector.columnName[1]);
    assertEquals(4, vector.columnWidth[1].getWidth());
    assertEquals(2, vector.data.size());
  }
}
