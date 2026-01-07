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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.TestVectorEvaluator;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for D-Latch test vector execution.
 *
 * This test verifies that sequential test vectors work correctly for
 * stateful circuits like a D-Latch, where the output depends on previous
 * state as well as current inputs.
 */
public class DLatchTestVectorTest {

  @TempDir
  File tempDir;

  @Test
  public void testDLatchSequentialExecution() throws IOException {
    // Create test vector file matching the D-Latch test case
    // Note: This test vector has <seq> but no <set> column
    // All tests default to set=0, and since seq != 0, they should be sequential
    File testFile = new File(tempDir, "dlatch_test.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("# Simple test for D-Latch\n");
      writer.write("<seq> data write Q NQ\n");
      writer.write("1     0    0     0 1\n");
      writer.write("2     1    0     0 1\n");
      writer.write("3     1    1     1 0\n");
      writer.write("4     0    0     1 0\n");
      writer.write("5     0    1     0 1\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);

    // Verify parsing
    // Note: <seq> is metadata column, not included in columnName
    // Only actual pin columns are in columnName
    assertEquals(4, vector.columnName.length);
    assertEquals("data", vector.columnName[0]);
    assertEquals("write", vector.columnName[1]);
    assertEquals("Q", vector.columnName[2]);
    assertEquals("NQ", vector.columnName[3]);

    // Verify we have 5 test rows
    assertEquals(5, vector.data.size());

    // Verify sequence numbers
    assertNotNull(vector.seqNumbers);
    assertEquals(5, vector.seqNumbers.length);
    assertEquals(1, vector.seqNumbers[0]);
    assertEquals(2, vector.seqNumbers[1]);
    assertEquals(3, vector.seqNumbers[2]);
    assertEquals(4, vector.seqNumbers[3]);
    assertEquals(5, vector.seqNumbers[4]);

    // Verify set numbers (should default to 0 when not specified)
    // Since there's no <set> column, setNumbers should be empty array
    // But the code should handle this by defaulting to set=0
    if (vector.setNumbers != null && vector.setNumbers.length > 0) {
      // If setNumbers exists, all should be 0 (default)
      for (int i = 0; i < vector.setNumbers.length; i++) {
        assertEquals(0, vector.setNumbers[i], "Row " + i + " should default to set=0");
      }
    }

    // Verify test data values
    // Note: data array only contains pin values, not metadata columns
    // Column indices: 0=data, 1=write, 2=Q, 3=NQ
    // Row 0: seq=1, data=0, write=0, Q=0, NQ=1
    assertEquals(0L, vector.data.get(0)[0].toLongValue()); // data
    assertEquals(0L, vector.data.get(0)[1].toLongValue()); // write
    assertEquals(0L, vector.data.get(0)[2].toLongValue()); // Q
    assertEquals(1L, vector.data.get(0)[3].toLongValue()); // NQ

    // Row 1: seq=2, data=1, write=0, Q=0, NQ=1 (write=0, so Q should stay 0)
    assertEquals(1L, vector.data.get(1)[0].toLongValue()); // data
    assertEquals(0L, vector.data.get(1)[1].toLongValue()); // write
    assertEquals(0L, vector.data.get(1)[2].toLongValue()); // Q
    assertEquals(1L, vector.data.get(1)[3].toLongValue()); // NQ

    // Row 2: seq=3, data=1, write=1, Q=1, NQ=0 (write=1, so Q should become 1)
    assertEquals(1L, vector.data.get(2)[0].toLongValue()); // data
    assertEquals(1L, vector.data.get(2)[1].toLongValue()); // write
    assertEquals(1L, vector.data.get(2)[2].toLongValue()); // Q
    assertEquals(0L, vector.data.get(2)[3].toLongValue()); // NQ

    // Row 3: seq=4, data=0, write=0, Q=1, NQ=0 (write=0, so Q should stay 1)
    assertEquals(0L, vector.data.get(3)[0].toLongValue()); // data
    assertEquals(0L, vector.data.get(3)[1].toLongValue()); // write
    assertEquals(1L, vector.data.get(3)[2].toLongValue()); // Q
    assertEquals(0L, vector.data.get(3)[3].toLongValue()); // NQ

    // Row 4: seq=5, data=0, write=1, Q=0, NQ=1 (write=1, so Q should become 0)
    assertEquals(0L, vector.data.get(4)[0].toLongValue()); // data
    assertEquals(1L, vector.data.get(4)[1].toLongValue()); // write
    assertEquals(0L, vector.data.get(4)[2].toLongValue()); // Q
    assertEquals(1L, vector.data.get(4)[3].toLongValue()); // NQ
  }

  @Test
  public void testDLatchWithSetColumn() throws IOException {
    // Create test vector file with explicit <set> column
    // This should make the sequential behavior more explicit
    File testFile = new File(tempDir, "dlatch_with_set.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("# D-Latch test with explicit set column\n");
      writer.write("<set> <seq> data write Q NQ\n");
      writer.write("1     1     0    0     0 1\n");
      writer.write("1     2     1    0     0 1\n");
      writer.write("1     3     1    1     1 0\n");
      writer.write("1     4     0    0     1 0\n");
      writer.write("1     5     0    1     0 1\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);

    // Verify set numbers
    assertNotNull(vector.setNumbers);
    assertEquals(5, vector.setNumbers.length);
    for (int i = 0; i < 5; i++) {
      assertEquals(1, vector.setNumbers[i], "Row " + i + " should have set=1");
    }

    // Verify sequence numbers
    assertNotNull(vector.seqNumbers);
    assertEquals(5, vector.seqNumbers.length);
    assertEquals(1, vector.seqNumbers[0]);
    assertEquals(2, vector.seqNumbers[1]);
    assertEquals(3, vector.seqNumbers[2]);
    assertEquals(4, vector.seqNumbers[3]);
    assertEquals(5, vector.seqNumbers[4]);
  }

  @Test
  public void testDLatchExecution() throws Exception {
    // Create the D-Latch circuit XML
    String circuitXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <project source="4.0.0dev" version="1.0">
        <lib desc="#Wiring" name="0"/>
        <lib desc="#Gates" name="1"/>
        <lib desc="#Plexers" name="2"/>
        <lib desc="#Arithmetic" name="3"/>
        <lib desc="#Memory" name="4"/>
        <lib desc="#I/O" name="5"/>
        <lib desc="#TTL" name="6"/>
        <lib desc="#TCL" name="7"/>
        <lib desc="#Base" name="8"/>
        <main name="DLatch"/>
        <circuit name="DLatch">
          <a name="appearance" val="logisim_evolution"/>
          <a name="circuit" val="DLatch"/>
          <a name="circuitnamedboxfixedsize" val="true"/>
          <a name="simulationFrequency" val="1.0"/>
          <comp lib="0" loc="(210,150)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="label" val="data"/>
          </comp>
          <comp lib="0" loc="(210,250)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="label" val="write"/>
          </comp>
          <comp lib="0" loc="(670,170)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="west"/>
            <a name="label" val="Q"/>
            <a name="type" val="output"/>
          </comp>
          <comp lib="0" loc="(670,210)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="west"/>
            <a name="label" val="NQ"/>
            <a name="type" val="output"/>
          </comp>
          <comp lib="1" loc="(290,210)" name="NOT Gate"/>
          <comp lib="1" loc="(390,170)" name="AND Gate"/>
          <comp lib="1" loc="(390,230)" name="AND Gate"/>
          <comp lib="1" loc="(480,200)" name="OR Gate"/>
          <comp lib="4" loc="(550,160)" name="S-R Flip-Flop">
            <a name="appearance" val="logisim_evolution"/>
          </comp>
          <wire from="(210,150)" to="(220,150)"/>
          <wire from="(210,190)" to="(210,250)"/>
          <wire from="(210,190)" to="(340,190)"/>
          <wire from="(210,250)" to="(340,250)"/>
          <wire from="(220,150)" to="(220,210)"/>
          <wire from="(220,150)" to="(340,150)"/>
          <wire from="(220,210)" to="(260,210)"/>
          <wire from="(290,210)" to="(340,210)"/>
          <wire from="(390,170)" to="(410,170)"/>
          <wire from="(390,230)" to="(410,230)"/>
          <wire from="(410,170)" to="(410,180)"/>
          <wire from="(410,170)" to="(540,170)"/>
          <wire from="(410,180)" to="(430,180)"/>
          <wire from="(410,220)" to="(410,230)"/>
          <wire from="(410,220)" to="(430,220)"/>
          <wire from="(410,230)" to="(520,230)"/>
          <wire from="(480,200)" to="(530,200)"/>
          <wire from="(520,190)" to="(520,230)"/>
          <wire from="(520,190)" to="(540,190)"/>
          <wire from="(530,200)" to="(530,210)"/>
          <wire from="(530,210)" to="(540,210)"/>
          <wire from="(600,170)" to="(670,170)"/>
          <wire from="(600,210)" to="(670,210)"/>
        </circuit>
        </project>
        """;

    // Create test vector file
    File testFile = new File(tempDir, "dlatch_exec_test.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("# Simple test for D-Latch\n");
      writer.write("<seq> data write Q NQ\n");
      writer.write("1     0    0     0 1\n");
      writer.write("2     1    0     0 1\n");
      writer.write("3     1    1     1 0\n");
      writer.write("4     0    0     1 0\n");
      writer.write("5     0    1     0 1\n");
    }

    // Load circuit from XML
    Loader loader = new Loader(null);
    ByteArrayInputStream xmlStream = new ByteArrayInputStream(
        circuitXml.getBytes(StandardCharsets.UTF_8));
    LogisimFile logisimFile = LogisimFile.load(xmlStream, loader);
    assertNotNull(logisimFile, "Circuit should load successfully");

    Circuit circuit = logisimFile.getCircuit("DLatch");
    assertNotNull(circuit, "DLatch circuit should exist");

    // Create project
    Project project = new Project(logisimFile);
    project.setCurrentCircuit(circuit);

    // Execute test vector using the same method CLI uses
    // This will actually run the tests and throw exceptions on failure
    TestVector vector = new TestVector(testFile.getAbsolutePath());

    CircuitState tempState = CircuitState.createRootState(project, circuit, Thread.currentThread());

    TestVectorEvaluator evaluator;
    try {
      evaluator = new TestVectorEvaluator(tempState, vector);
    } catch (TestException e) {
      throw new AssertionError("Failed to construct evaluator: " + e);
    }

    evaluator.evaluate((row, report) -> {
      if (report == null || report.isEmpty()) {
        // All good.
      } else {
        // Test failed - collect error details
        StringBuilder errorDetails = new StringBuilder();
        errorDetails.append(String.format("Test %d failed:\n", row + 1));
        for (final var fail : report) {
          errorDetails.append(String.format("  %s\n", fail.toString()));
        }
        throw new AssertionError(
            String.format("Test %d failed unexpectedly with the corrected circuit:\n", row + 1));
      }
    });
  }
}
