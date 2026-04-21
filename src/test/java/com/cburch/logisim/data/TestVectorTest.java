/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestVectorTest {

  @TempDir File tempDir;

  @Test
  public void testParseBasicTestVector() throws IOException {
    // Test backward compatibility - basic test vector without <set> or <seq>
    File testFile = new File(tempDir, "basic.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B C\n");
      writer.write("0 1 0\n");
      writer.write("1 0 1\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(3, vector.columnName.length);
    assertEquals(2, vector.data.size());
    assertEquals("A", vector.columnName[0]);
    assertEquals("B", vector.columnName[1]);
    assertEquals("C", vector.columnName[2]);
  }

  @Test
  public void testParseWithSetAndSeqColumns() throws IOException {
    // Test parsing header with <set> and <seq> columns
    File testFile = new File(tempDir, "sequential.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B C <set> <seq>\n");
      writer.write("0 1 0 0 0\n");
      writer.write("1 0 1 1 1\n");
      writer.write("0 0 0 1 2\n");
      writer.write("1 0 1 2 1\n");
      writer.write("0 0 0 2 3\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(3, vector.columnName.length); // <set> and <seq> are not pin columns
    assertEquals(5, vector.data.size());

    // Verify set and seq arrays exist and have correct values
    assertNotNull(vector.setNumbers);
    assertNotNull(vector.seqNumbers);
    assertEquals(5, vector.setNumbers.length);
    assertEquals(5, vector.seqNumbers.length);
    assertEquals(0, vector.setNumbers[0]);
    assertEquals(0, vector.seqNumbers[0]);
    assertEquals(1, vector.setNumbers[1]);
    assertEquals(1, vector.seqNumbers[1]);
    assertEquals(1, vector.setNumbers[2]);
    assertEquals(2, vector.seqNumbers[2]);
    assertEquals(2, vector.setNumbers[3]);
    assertEquals(1, vector.seqNumbers[3]);
    assertEquals(2, vector.setNumbers[4]);
    assertEquals(3, vector.seqNumbers[4]);
  }

  @Test
  public void testParseWithOnlySetColumn() throws IOException {
    // Test parsing with only <set> column
    File testFile = new File(tempDir, "set_only.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B <set>\n");
      writer.write("0 1 0\n");
      writer.write("1 0 0\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(2, vector.columnName.length);
    assertEquals(2, vector.setNumbers.length);
    assertEquals(0, vector.setNumbers[0]);
    assertEquals(0, vector.setNumbers[1]);
    // seqNumbers should default to 0
    assertNotNull(vector.seqNumbers);
    assertEquals(2, vector.seqNumbers.length);
    assertEquals(0, vector.seqNumbers[0]);
    assertEquals(0, vector.seqNumbers[1]);
  }

  @Test
  public void testParseWithOnlySeqColumn() throws IOException {
    // Test parsing with only <seq> column
    File testFile = new File(tempDir, "seq_only.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B <seq>\n");
      writer.write("0 1 0\n");
      writer.write("1 0 0\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(2, vector.columnName.length);
    assertEquals(2, vector.seqNumbers.length);
    assertEquals(0, vector.seqNumbers[0]);
    assertEquals(0, vector.seqNumbers[1]);
    // setNumbers should default to 0
    assertNotNull(vector.setNumbers);
    assertEquals(2, vector.setNumbers.length);
    assertEquals(0, vector.setNumbers[0]);
    assertEquals(0, vector.setNumbers[1]);
  }

  @Test
  public void testParseSetAndSeqDefaultsToZero() throws IOException {
    // Test that missing set/seq values default to 0
    File testFile = new File(tempDir, "defaults.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B <set> <seq>\n");
      writer.write("0 1\n"); // Missing set and seq values
      writer.write("1 0 0\n"); // Missing seq value
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(0, vector.setNumbers[0]);
    assertEquals(0, vector.seqNumbers[0]);
    assertEquals(0, vector.setNumbers[1]);
    assertEquals(0, vector.seqNumbers[1]);
  }

  @Test
  public void testParseSpecialValueDC() throws IOException {
    // Test parsing <DC> (Don't Care) special value
    File testFile = new File(tempDir, "dc.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B C\n");
      writer.write("<DC> 1 0\n");
      writer.write("<dc> 0 1\n"); // Case insensitive
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(2, vector.data.size());
    // Verify <DC> is marked as don't care
    assertTrue(vector.isDontCare(0, 0));
    assertTrue(vector.isDontCare(1, 0));
  }

  @Test
  public void testParseSpecialValueFloat() throws IOException {
    // Test parsing <float> (floating/high-impedance) special value
    File testFile = new File(tempDir, "float.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B C\n");
      writer.write("<float> 1 0\n");
      writer.write("<FLOAT> 0 1\n"); // Case insensitive
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(2, vector.data.size());
    // Verify <float> is marked as floating
    assertTrue(vector.isFloating(0, 0));
    assertTrue(vector.isFloating(1, 0));
    // Verify the actual value is UNKNOWN
    assertEquals(Value.UNKNOWN, vector.data.get(0)[0]);
    assertEquals(Value.UNKNOWN, vector.data.get(1)[0]);
  }

  @Test
  public void testParseMixedSpecialValues() throws IOException {
    // Test parsing with both <DC> and <float> in same line
    File testFile = new File(tempDir, "mixed.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B C D\n");
      writer.write("<DC> <float> 1 0\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(1, vector.data.size());
    assertTrue(vector.isDontCare(0, 0));
    assertTrue(vector.isFloating(0, 1));
    assertEquals(Value.TRUE, vector.data.get(0)[2]);
    assertEquals(Value.FALSE, vector.data.get(0)[3]);
  }

  @Test
  public void testParseSpecialValuesWithSetAndSeq() throws IOException {
    // Test special values with set and seq columns
    File testFile = new File(tempDir, "special_seq.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B <set> <seq>\n");
      writer.write("<DC> <float> 1 2\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(1, vector.data.size());
    assertTrue(vector.isDontCare(0, 0));
    assertTrue(vector.isFloating(0, 1));
    assertEquals(1, vector.setNumbers[0]);
    assertEquals(2, vector.seqNumbers[0]);
  }

  @Test
  public void testParseErrorOnInvalidSetValue() throws IOException {
    // Test that non-numeric set values cause error
    File testFile = new File(tempDir, "invalid_set.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B <set> <seq>\n");
      writer.write("0 1 abc 1\n");
    }

    assertThrows(IOException.class, () -> new TestVector(testFile));
  }

  @Test
  public void testParseErrorOnInvalidSeqValue() throws IOException {
    // Test that non-numeric seq values cause error
    File testFile = new File(tempDir, "invalid_seq.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B <set> <seq>\n");
      writer.write("0 1 0 xyz\n");
    }

    assertThrows(IOException.class, () -> new TestVector(testFile));
  }

  @Test
  public void testParseSetAndSeqOrderMatters() throws IOException {
    // Test that <set> and <seq> can be in any order in header
    File testFile = new File(tempDir, "order.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B <seq> <set>\n"); // seq before set
      writer.write("0 1 5 10\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(5, vector.seqNumbers[0]);
    assertEquals(10, vector.setNumbers[0]);
  }

  @Test
  public void testParseHexWithUnderscores() throws IOException {
    // Test that hex values can have underscores for readability
    File testFile = new File(tempDir, "hex_underscore.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A[32]\n");
      writer.write("0x0000_1111\n");
      writer.write("0xFFFF_0000\n");
      writer.write("0x_1234_5678\n"); // Underscore after 0x prefix
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(3, vector.data.size());
    assertEquals(0x00001111L, vector.data.get(0)[0].toLongValue());
    assertEquals(0xFFFF0000L, vector.data.get(1)[0].toLongValue());
    assertEquals(0x12345678L, vector.data.get(2)[0].toLongValue());
  }

  @Test
  public void testParseOctalWithUnderscores() throws IOException {
    // Test that octal values can have underscores
    File testFile = new File(tempDir, "octal_underscore.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A[32]\n"); // Need 32 bits for 10-digit octal number
      writer.write("0o1234_5670\n");
      writer.write("0o_7777\n"); // Underscore after 0o prefix
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(2, vector.data.size());
    // Verify the values are parsed correctly (0o12345670 = 2739128, 0o7777 = 4095)
    assertEquals(2739128L, vector.data.get(0)[0].toLongValue());
    assertEquals(4095L, vector.data.get(1)[0].toLongValue());
  }

  @Test
  public void testParseBinaryWithUnderscores() throws IOException {
    // Test that binary values can have underscores
    File testFile = new File(tempDir, "binary_underscore.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A[8]\n");
      writer.write("1111_0000\n");
      writer.write("1010_1010\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(2, vector.data.size());
    assertEquals(0xF0, vector.data.get(0)[0].toLongValue());
    assertEquals(0xAA, vector.data.get(1)[0].toLongValue());
  }

  @Test
  public void testParseDecimalWithUnderscores() throws IOException {
    // Test that decimal values can have underscores
    File testFile = new File(tempDir, "decimal_underscore.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A[16]\n");
      writer.write("1234\n"); // Without underscore
      writer.write("1_234\n"); // With underscore
      writer.write("-3_000\n"); // Negative with underscore (fits in 16-bit signed: -32768 to 32767)
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(3, vector.data.size());
    assertEquals(1234, vector.data.get(0)[0].toLongValue());
    assertEquals(1234, vector.data.get(1)[0].toLongValue());
    // For 16-bit signed, -3000 is stored as 62536 (unsigned representation)
    // toLongValue() returns the raw stored value
    assertEquals(62536L, vector.data.get(2)[0].toLongValue());
  }

  @Test
  public void testParseSpecialValuesWithUnderscores() throws IOException {
    // Test that underscores don't interfere with special values
    File testFile = new File(tempDir, "special_underscore.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A[16] B C\n"); // Specify width for hex value
      writer.write("0x_1234 <DC> <float>\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(1, vector.data.size());
    assertEquals(0x1234L, vector.data.get(0)[0].toLongValue());
    assertTrue(vector.isDontCare(0, 1));
    assertTrue(vector.isFloating(0, 2));
  }

  @Test
  public void testTooManyBitsErrorMessage() throws IOException {
    // Test that error message includes expected bit width and suggestion
    File testFile = new File(tempDir, "error_test.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A[1]\n");
      writer.write("0x11111\n"); // 16 bits but column expects 1 bit
    }

    IOException exception = assertThrows(IOException.class, () -> {
      new TestVector(testFile);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("Too many bits"), "Error should mention 'Too many bits'");
    assertTrue(message.contains("expected 1 bit"), "Error should mention expected bit width");
    assertTrue(message.contains("0x11111"), "Error should include the problematic value");
    assertTrue(message.contains("did you mean"), "Error should suggest correction");
    assertTrue(message.contains("A[20]"), "Error should include column name in suggestion");
    assertTrue(message.contains("Remember that 0x means hex and each hex digit is 4 bits"), "Error should include column name in suggestion");
  }

  @Test
  public void testTooManyBitsErrorMessageWithColumnName() throws IOException {
    // Test that error message includes column name in suggestion
    File testFile = new File(tempDir, "error_test2.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("dataWrite[2]\n");
      writer.write("0b11111111\n"); // 16 bits but column expects 1 bit
    }

    IOException exception = assertThrows(IOException.class, () -> {
      new TestVector(testFile);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("Too many bits"), "Error should mention 'Too many bits'");
    assertTrue(message.contains("expected 2 bits"), "Error should mention expected bit width");
    assertTrue(message.contains("did you mean"), "Error should suggest correction");
    assertTrue(message.contains("dataWrite[8]"), "Error should include column name in suggestion");
    assertTrue(message.contains("Remember that 0b means binary and each binary digit is 1 bit"), "Error should include column name in suggestion");
  }
}
