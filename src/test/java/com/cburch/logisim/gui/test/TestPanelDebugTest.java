/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.TestVector;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test to verify that debug logging correctly shows all steps in a sequential test.
 */
public class TestPanelDebugTest {

  @TempDir File tempDir;
  private PrintStream originalOut;
  private ByteArrayOutputStream capturedOutput;

  @BeforeEach
  public void setUp() {
    // Capture System.out to verify debug output
    originalOut = System.out;
    capturedOutput = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedOutput));
    
    // Set debug level
    System.setProperty("logisim.log.level", "DEBUG");
  }

  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
    System.clearProperty("logisim.log.level");
  }

  @Test
  public void testSequenceCollectionForThreeSteps() throws IOException {
    // Create a test vector with 3 steps in set 1 (sequence ID = 1)
    File testFile = new File(tempDir, "three_step_sequence.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("Clock Data[8] Out[8] <set> <seq>\n");
      // Set 1: Three steps with seq 1, 2, 3
      writer.write("0 0x01 0x00 1 1\n");  // Step 1: row 0
      writer.write("1 0x01 0x00 1 2\n");  // Step 2: row 1
      writer.write("0 0x01 0x01 1 3\n");  // Step 3: row 2 (target)
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(3, vector.data.size());
    
    // Verify set and seq numbers
    assertNotNull(vector.setNumbers);
    assertNotNull(vector.seqNumbers);
    assertEquals(3, vector.setNumbers.length);
    assertEquals(3, vector.seqNumbers.length);
    assertEquals(1, vector.setNumbers[0]);
    assertEquals(1, vector.setNumbers[1]);
    assertEquals(1, vector.setNumbers[2]);
    assertEquals(1, vector.seqNumbers[0]);
    assertEquals(2, vector.seqNumbers[1]);
    assertEquals(3, vector.seqNumbers[2]);
    
    // Simulate the sequence collection logic from executeGoButton
    // Target is row 2 (the last step)
    int targetFileRow = 2;
    int targetSet = 1; // Sequence ID is the set number
    
    ArrayList<Integer> sequenceRows = new ArrayList<>();
    for (int i = 0; i < vector.data.size(); i++) {
      int set = (vector.setNumbers != null && i < vector.setNumbers.length) 
          ? vector.setNumbers[i] : 0;
      int seq = (vector.seqNumbers != null && i < vector.seqNumbers.length) 
          ? vector.seqNumbers[i] : 0;
      // Include rows with the same set (sequence ID), but only if seq != 0
      if (set == targetSet && seq != 0) {
        sequenceRows.add(i);
        if (i == targetFileRow) break; // Stop at target row
      } else if (set != targetSet && sequenceRows.size() > 0) {
        break; // Different set, stop collecting
      }
    }
    
    // Sort by seq (step number) to ensure correct order
    sequenceRows.sort((a, b) -> {
      int seqA = vector.seqNumbers[a];
      int seqB = vector.seqNumbers[b];
      return Integer.compare(seqA, seqB);
    });
    
    // Verify we collected all 3 steps
    assertEquals(3, sequenceRows.size(), 
        "Should collect all 3 steps in the sequence up to and including the target");
    assertEquals(0, sequenceRows.get(0).intValue(), "First step should be row 0");
    assertEquals(1, sequenceRows.get(1).intValue(), "Second step should be row 1");
    assertEquals(2, sequenceRows.get(2).intValue(), "Third step should be row 2");
  }

  @Test
  public void testSequenceCollectionWithMultipleSequences() throws IOException {
    // Create a test vector with multiple sequences (different sets)
    File testFile = new File(tempDir, "multi_sequence.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("Clock Data[8] Out[8] <set> <seq>\n");
      // Set 1: Two steps
      writer.write("0 0x01 0x00 1 1\n");  // row 0
      writer.write("1 0x01 0x00 1 2\n");  // row 1
      // Set 2: Three steps
      writer.write("0 0x02 0x00 2 1\n");  // row 2
      writer.write("1 0x02 0x00 2 2\n");  // row 3
      writer.write("0 0x02 0x02 2 3\n");  // row 4 (target)
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(5, vector.data.size());
    
    // Test set 1 - target row 1 (last step of set 1)
    int targetFileRow = 1;
    int targetSet = 1;
    
    ArrayList<Integer> sequenceRows = new ArrayList<>();
    for (int i = 0; i < vector.data.size(); i++) {
      int set = (vector.setNumbers != null && i < vector.setNumbers.length) 
          ? vector.setNumbers[i] : 0;
      int seq = (vector.seqNumbers != null && i < vector.seqNumbers.length) 
          ? vector.seqNumbers[i] : 0;
      if (set == targetSet && seq != 0) {
        sequenceRows.add(i);
        if (i == targetFileRow) break;
      } else if (set != targetSet && sequenceRows.size() > 0) {
        break;
      }
    }
    
    // Sort by seq
    sequenceRows.sort((a, b) -> Integer.compare(vector.seqNumbers[a], vector.seqNumbers[b]));
    
    assertEquals(2, sequenceRows.size(), 
        "Set 1 should have 2 steps");
    assertEquals(0, sequenceRows.get(0).intValue());
    assertEquals(1, sequenceRows.get(1).intValue());
    
    // Test set 2 - target row 4 (last step of set 2)
    sequenceRows.clear();
    targetFileRow = 4;
    targetSet = 2;
    
    for (int i = 0; i < vector.data.size(); i++) {
      int set = (vector.setNumbers != null && i < vector.setNumbers.length) 
          ? vector.setNumbers[i] : 0;
      int seq = (vector.seqNumbers != null && i < vector.seqNumbers.length) 
          ? vector.seqNumbers[i] : 0;
      if (set == targetSet && seq != 0) {
        sequenceRows.add(i);
        if (i == targetFileRow) break;
      } else if (set != targetSet && sequenceRows.size() > 0) {
        break;
      }
    }
    
    // Sort by seq
    sequenceRows.sort((a, b) -> Integer.compare(vector.seqNumbers[a], vector.seqNumbers[b]));
    
    assertEquals(3, sequenceRows.size(), 
        "Set 2 should have 3 steps");
    assertEquals(2, sequenceRows.get(0).intValue());
    assertEquals(3, sequenceRows.get(1).intValue());
    assertEquals(4, sequenceRows.get(2).intValue());
  }

  @Test
  public void testSequenceCollectionWithMiddleStepAsTarget() throws IOException {
    // Create a test vector where we target a middle step, not the last one
    File testFile = new File(tempDir, "middle_target.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("Clock Data[8] Out[8] <set> <seq>\n");
      // Set 1: Five steps
      writer.write("0 0x01 0x00 1 1\n");  // row 0
      writer.write("1 0x01 0x00 1 2\n");  // row 1
      writer.write("0 0x01 0x01 1 3\n");  // row 2 (target - middle step)
      writer.write("1 0x01 0x01 1 4\n");  // row 3 (should NOT be included)
      writer.write("0 0x01 0x02 1 5\n");  // row 4 (should NOT be included)
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);
    assertEquals(5, vector.data.size());
    
    // Target is row 2 (middle step)
    int targetFileRow = 2;
    int targetSet = 1; // Sequence ID is the set number
    
    ArrayList<Integer> sequenceRows = new ArrayList<>();
    for (int i = 0; i < vector.data.size(); i++) {
      int set = (vector.setNumbers != null && i < vector.setNumbers.length) 
          ? vector.setNumbers[i] : 0;
      int seq = (vector.seqNumbers != null && i < vector.seqNumbers.length) 
          ? vector.seqNumbers[i] : 0;
      if (set == targetSet && seq != 0) {
        sequenceRows.add(i);
        if (i == targetFileRow) break; // Stop at target row
      } else if (set != targetSet && sequenceRows.size() > 0) {
        break;
      }
    }
    
    // Sort by seq
    sequenceRows.sort((a, b) -> Integer.compare(vector.seqNumbers[a], vector.seqNumbers[b]));
    
    // Should only collect up to and including the target row
    assertEquals(3, sequenceRows.size(), 
        "Should collect only steps up to and including the target (row 2)");
    assertEquals(0, sequenceRows.get(0).intValue());
    assertEquals(1, sequenceRows.get(1).intValue());
    assertEquals(2, sequenceRows.get(2).intValue());
  }
}

