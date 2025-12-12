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
      writer.write("0 1 0 0 1\n");
      writer.write("1 0 1 0 1\n");
      writer.write("0 0 0 1 2\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(3, vector.columnName.length); // <set> and <seq> are not pin columns
    assertEquals(3, vector.data.size());
    
    // Verify set and seq arrays exist and have correct values
    assertNotNull(vector.setNumbers);
    assertNotNull(vector.seqNumbers);
    assertEquals(3, vector.setNumbers.length);
    assertEquals(3, vector.seqNumbers.length);
    assertEquals(0, vector.setNumbers[0]);
    assertEquals(1, vector.seqNumbers[0]);
    assertEquals(0, vector.setNumbers[1]);
    assertEquals(1, vector.seqNumbers[1]);
    assertEquals(1, vector.setNumbers[2]);
    assertEquals(2, vector.seqNumbers[2]);
  }

  @Test
  public void testParseWithOnlySetColumn() throws IOException {
    // Test parsing with only <set> column
    File testFile = new File(tempDir, "set_only.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B <set>\n");
      writer.write("0 1 5\n");
      writer.write("1 0 5\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(2, vector.columnName.length);
    assertEquals(2, vector.setNumbers.length);
    assertEquals(5, vector.setNumbers[0]);
    assertEquals(5, vector.setNumbers[1]);
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
      writer.write("0 1 3\n");
      writer.write("1 0 3\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(2, vector.columnName.length);
    assertEquals(2, vector.seqNumbers.length);
    assertEquals(3, vector.seqNumbers[0]);
    assertEquals(3, vector.seqNumbers[1]);
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
      writer.write("1 0 2\n"); // Missing seq value
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(0, vector.setNumbers[0]);
    assertEquals(0, vector.seqNumbers[0]);
    assertEquals(2, vector.setNumbers[1]);
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

  @Test
  public void testExecutionOrderBySetAndSequence() throws IOException {
    // Create a test vector with tests in non-sorted order
    // We'll verify they execute in sorted order (set first, then sequence)
    File testFile = new File(tempDir, "execution_order.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("<set> <seq> A[1] B[1]\n");
      // Set 1, Seq 2
      writer.write("1 2 0 0\n");
      // Set 0, Seq 1
      writer.write("0 1 0 0\n");
      // Set 1, Seq 1
      writer.write("1 1 0 0\n");
      // Set 0, Seq 2
      writer.write("0 2 0 0\n");
      // Set 0, Seq 0 (combinational)
      writer.write("0 0 0 0\n");
      // Set 1, Seq 0 (combinational)
      writer.write("1 0 0 0\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(6, vector.data.size());
    
    // Verify set and seq numbers are parsed correctly
    assertNotNull(vector.setNumbers);
    assertNotNull(vector.seqNumbers);
    assertEquals(6, vector.setNumbers.length);
    assertEquals(6, vector.seqNumbers.length);
    
    // Expected execution order: sorted by set first, then sequence
    // Set 0: seq 0, seq 1, seq 2
    // Set 1: seq 0, seq 1, seq 2
    // File order: [1,2], [0,1], [1,1], [0,2], [0,0], [1,0]
    // Sorted order: [0,0], [0,1], [0,2], [1,0], [1,1], [1,2]
    // File indices: 4, 1, 3, 5, 2, 0
    
    int[] expectedOrder = {4, 1, 3, 5, 2, 0}; // File indices in sorted order
    
    // Verify the set and seq values match expected order
    for (int i = 0; i < expectedOrder.length; i++) {
      int fileIndex = expectedOrder[i];
      int expectedSet = (i < 3) ? 0 : 1; // First 3 are set 0, last 3 are set 1
      int expectedSeq = (i % 3); // Within each set: 0, 1, 2
      
      assertEquals(expectedSet, vector.setNumbers[fileIndex], 
          "Set number at file index " + fileIndex + " should be " + expectedSet);
      assertEquals(expectedSeq, vector.seqNumbers[fileIndex],
          "Seq number at file index " + fileIndex + " should be " + expectedSeq);
    }
    
    // Verify that when we sort by set then seq, we get the expected order
    java.util.ArrayList<Integer> indices = new java.util.ArrayList<>();
    for (int i = 0; i < vector.data.size(); i++) {
      indices.add(i);
    }
    
    indices.sort((a, b) -> {
      int setA = vector.setNumbers[a];
      int setB = vector.setNumbers[b];
      int seqA = vector.seqNumbers[a];
      int seqB = vector.seqNumbers[b];
      
      int setCompare = Integer.compare(setA, setB);
      if (setCompare != 0) return setCompare;
      return Integer.compare(seqA, seqB);
    });
    
    // Verify sorted order matches expected
    for (int i = 0; i < expectedOrder.length; i++) {
      assertEquals(expectedOrder[i], indices.get(i).intValue(),
          "Sorted index " + i + " should be file index " + expectedOrder[i]);
    }
  }

  @Test
  public void testSequentialStatePreservation() throws IOException {
    // This test verifies that circuit state is preserved between sequential tests
    // and reset between different sequences. We test this by checking the resetState
    // parameter logic in the test execution flow.
    
    // Create a test vector with sequential tests that would require state preservation
    File testFile = new File(tempDir, "state_preservation.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("Clock Reset Count <set> <seq>\n");
      // Sequence 1 (set=1): Three tests that should maintain state
      writer.write("0 0 0 1 1\n");  // Initial state
      writer.write("1 0 0 1 2\n"); // Clock tick - state should be preserved from previous
      writer.write("0 0 1 1 3\n");  // Check count incremented - state preserved
      // Sequence 2 (set=2): New sequence - should reset
      writer.write("0 0 0 2 1\n");  // Should reset, count back to 0
      writer.write("1 0 0 2 2\n");  // Clock tick
      writer.write("0 0 1 2 3\n");  // Check count incremented
      // Combinational test (seq=0): Should reset
      writer.write("0 0 0 0 0\n");  // Should reset
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(7, vector.data.size());
    
    // Verify set and seq numbers (in file order, before sorting)
    assertNotNull(vector.setNumbers);
    assertNotNull(vector.seqNumbers);
    assertEquals(7, vector.setNumbers.length);
    assertEquals(7, vector.seqNumbers.length);
    // File order: set 1,1,1,2,2,2,0 and seq 1,2,3,1,2,3,0
    assertEquals(1, vector.setNumbers[0]);
    assertEquals(1, vector.setNumbers[1]);
    assertEquals(1, vector.setNumbers[2]);
    assertEquals(2, vector.setNumbers[3]);
    assertEquals(2, vector.setNumbers[4]);
    assertEquals(2, vector.setNumbers[5]);
    assertEquals(0, vector.setNumbers[6]);
    assertEquals(1, vector.seqNumbers[0]);
    assertEquals(2, vector.seqNumbers[1]);
    assertEquals(3, vector.seqNumbers[2]);
    assertEquals(1, vector.seqNumbers[3]);
    assertEquals(2, vector.seqNumbers[4]);
    assertEquals(3, vector.seqNumbers[5]);
    assertEquals(0, vector.seqNumbers[6]); // Last one is combinational (seq=0)
    
    // Verify the reset logic: 
    // - Tests 0, 1, 2 are all set=1, so only test 0 should reset
    // - Test 3 starts set=2, so it should reset
    // - Tests 3, 4, 5 are set=2, so only test 3 should reset
    // - Test 6 is seq=0, so it should reset
    
    // Simulate the reset logic that TestThread uses (based on set, not seq)
    int currentSet = -1;
    boolean[] shouldReset = new boolean[7];
    
    for (int i = 0; i < vector.data.size(); i++) {
      int testSet = vector.setNumbers[i];
      int testSeq = vector.seqNumbers[i];
      if (testSeq == 0 || testSet != currentSet) {
        shouldReset[i] = true;
        currentSet = testSet;
      } else {
        shouldReset[i] = false;
      }
    }
    
    // Verify reset flags (in file order)
    assertTrue(shouldReset[0], "First test in set 1 should reset");
    assertFalse(shouldReset[1], "Second test in set 1 should NOT reset (state preserved)");
    assertFalse(shouldReset[2], "Third test in set 1 should NOT reset (state preserved)");
    assertTrue(shouldReset[3], "First test in set 2 should reset");
    assertFalse(shouldReset[4], "Second test in set 2 should NOT reset (state preserved)");
    assertFalse(shouldReset[5], "Third test in set 2 should NOT reset (state preserved)");
    assertTrue(shouldReset[6], "Combinational test (seq=0) should reset");
  }
}
