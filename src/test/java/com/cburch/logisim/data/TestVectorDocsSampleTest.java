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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for D-Latch test vector execution.
 *
 * This test verifies that sequential test vectors work correctly for
 * stateful circuits like a D-Latch, where the output depends on previous
 * state as well as current inputs.
 */
public class TestVectorDocsSampleTest {

  @TempDir File tempDir;

  @Test
  public void actualtestDocsSampleExecution() throws IOException {
    // Create test vector file matching the D-Latch test case
    // Note: This test vector has no <seq> or <set> column
    // All tests default to set=0, and seq == 0, they should be combinational





    File testFile = new File(tempDir, "DocsSample_test.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B O_Nor O_Nand O_Xor O_Or O_And O_AB[2]\n");
      writer.write("0 0 1 1 0 0 0 00\n");
      writer.write("0 1 0 1 1 1 0 01\n");
      writer.write("1 0 0 1 1 1 0 10\n");
      writer.write("1 1 0 0 0 1 1 11\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);

    // Verify parsing
    // Column indices: 0=A, 1=B, 2=O_Nor, 3=O_Nand, 4=O_Xor, 5=O_Or, 6=O_And, 7=O_AB[2]
    assertEquals(8, vector.columnName.length);
    assertEquals("A", vector.columnName[0]);
    assertEquals("B", vector.columnName[1]);
    assertEquals("O_Nor", vector.columnName[2]);
    assertEquals("O_Nand", vector.columnName[3]);
    assertEquals("O_Xor", vector.columnName[4]);
    assertEquals("O_Or", vector.columnName[5]);
    assertEquals("O_And", vector.columnName[6]);
    assertEquals("O_AB", vector.columnName[7]);

    // Verify we have 4 test rows
    assertEquals(4, vector.data.size());

    // Verify sequence numbers (should be empty or default to 0 since no <seq> column)
    // When there's no <seq> column, seqNumbers should be empty array
    if (vector.seqNumbers != null && vector.seqNumbers.length > 0) {
      // If seqNumbers exists, all should be 0 (default for combinational tests)
      for (int i = 0; i < vector.seqNumbers.length; i++) {
        assertEquals(0, vector.seqNumbers[i], "Row " + i + " should default to seq=0");
      }
    }

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
    // Row 0: A=0, B=0, O_Nor=1, O_Nand=1, O_Xor=0, O_Or=0, O_And=0, O_AB=00
    assertEquals(0L, vector.data.get(0)[0].toLongValue()); // A
    assertEquals(0L, vector.data.get(0)[1].toLongValue()); // B
    assertEquals(1L, vector.data.get(0)[2].toLongValue()); // O_Nor
    assertEquals(1L, vector.data.get(0)[3].toLongValue()); // O_Nand
    assertEquals(0L, vector.data.get(0)[4].toLongValue()); // O_Xor
    assertEquals(0L, vector.data.get(0)[5].toLongValue()); // O_Or
    assertEquals(0L, vector.data.get(0)[6].toLongValue()); // O_And
    assertEquals(0L, vector.data.get(0)[7].toLongValue()); // O_AB

    // Row 1: A=0, B=1, O_Nor=0, O_Nand=1, O_Xor=1, O_Or=1, O_And=0, O_AB=01
    assertEquals(0L, vector.data.get(1)[0].toLongValue()); // A
    assertEquals(1L, vector.data.get(1)[1].toLongValue()); // B
    assertEquals(0L, vector.data.get(1)[2].toLongValue()); // O_Nor
    assertEquals(1L, vector.data.get(1)[3].toLongValue()); // O_Nand
    assertEquals(1L, vector.data.get(1)[4].toLongValue()); // O_Xor
    assertEquals(1L, vector.data.get(1)[5].toLongValue()); // O_Or
    assertEquals(0L, vector.data.get(1)[6].toLongValue()); // O_And
    assertEquals(1L, vector.data.get(1)[7].toLongValue()); // O_AB

    // Row 2: A=1, B=0, O_Nor=0, O_Nand=1, O_Xor=1, O_Or=1, O_And=0, O_AB=10
    assertEquals(1L, vector.data.get(2)[0].toLongValue()); // A
    assertEquals(0L, vector.data.get(2)[1].toLongValue()); // B
    assertEquals(0L, vector.data.get(2)[2].toLongValue()); // O_Nor
    assertEquals(1L, vector.data.get(2)[3].toLongValue()); // O_Nand
    assertEquals(1L, vector.data.get(2)[4].toLongValue()); // O_Xor
    assertEquals(1L, vector.data.get(2)[5].toLongValue()); // O_Or
    assertEquals(0L, vector.data.get(2)[6].toLongValue()); // O_And
    assertEquals(2L, vector.data.get(2)[7].toLongValue()); // O_AB (binary 10 = decimal 2)

    // Row 3: A=1, B=1, O_Nor=0, O_Nand=0, O_Xor=0, O_Or=1, O_And=1, O_AB=11
    assertEquals(1L, vector.data.get(3)[0].toLongValue()); // A
    assertEquals(1L, vector.data.get(3)[1].toLongValue()); // B
    assertEquals(0L, vector.data.get(3)[2].toLongValue()); // O_Nor
    assertEquals(0L, vector.data.get(3)[3].toLongValue()); // O_Nand
    assertEquals(0L, vector.data.get(3)[4].toLongValue()); // O_Xor
    assertEquals(1L, vector.data.get(3)[5].toLongValue()); // O_Or
    assertEquals(1L, vector.data.get(3)[6].toLongValue()); // O_And
    assertEquals(3L, vector.data.get(3)[7].toLongValue()); // O_AB (binary 11 = decimal 3)
  }

  @Test
  public void testDocsSampleWithSetColumn() throws IOException {
    // Create test vector file with explicit <set> column
    // This should make the sequential behavior more explicit
    File testFile = new File(tempDir, "DocsSample_with_set.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B O_Nor O_Nand O_Xor O_Or O_And O_AB[2]\n");
      writer.write("0 0 1 1 0 0 0 00\n");
      writer.write("0 1 0 1 1 1 0 01\n");
      writer.write("1 0 0 1 1 1 0 10\n");
      writer.write("1 1 0 0 0 1 1 11\n");
    }

    TestVector vector = new TestVector(testFile);
    assertNotNull(vector);

    // Verify set numbers
    assertNotNull(vector.setNumbers);
    assertEquals(4, vector.setNumbers.length);
    for (int i = 0; i < 4; i++) {
      assertEquals(0, vector.setNumbers[i], "Row " + i + " should have set=1");
    }

    // Verify sequence numbers
    assertNotNull(vector.seqNumbers);
    assertEquals(4, vector.seqNumbers.length);
    assertEquals(0, vector.seqNumbers[0]);
    assertEquals(0, vector.seqNumbers[1]);
    assertEquals(0, vector.seqNumbers[2]);
    assertEquals(0, vector.seqNumbers[3]);
  }

  @Test
  public void testDocsSampleActualExecution() throws Exception {
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
        <main name="docsSample"/>
          <circuit name="testVectorSample">
            <a name="appearance" val="logisim_evolution"/>
            <a name="circuit" val="testVectorSample"/>
            <a name="circuitnamedboxfixedsize" val="true"/>
            <a name="simulationFrequency" val="1.0"/>
            <comp lib="0" loc="(110,130)" name="Pin">
              <a name="appearance" val="NewPins"/>
              <a name="facing" val="south"/>
              <a name="label" val="A"/>
            </comp>
            <comp lib="0" loc="(170,130)" name="Pin">
              <a name="appearance" val="NewPins"/>
              <a name="facing" val="south"/>
              <a name="label" val="B"/>
            </comp>
            <comp lib="0" loc="(270,430)" name="Splitter">
              <a name="appear" val="right"/>
              <a name="facing" val="west"/>
            </comp>
            <comp lib="0" loc="(410,220)" name="Pin">
              <a name="appearance" val="NewPins"/>
              <a name="facing" val="west"/>
              <a name="label" val="O_Nor"/>
              <a name="type" val="output"/>
            </comp>
            <comp lib="0" loc="(410,250)" name="Pin">
              <a name="appearance" val="NewPins"/>
              <a name="facing" val="west"/>
              <a name="label" val="O_Nand"/>
              <a name="type" val="output"/>
            </comp>
            <comp lib="0" loc="(410,290)" name="Pin">
              <a name="appearance" val="NewPins"/>
              <a name="facing" val="west"/>
              <a name="label" val="O_Xor"/>
              <a name="type" val="output"/>
            </comp>
            <comp lib="0" loc="(410,330)" name="Pin">
              <a name="appearance" val="NewPins"/>
              <a name="facing" val="west"/>
              <a name="label" val="O_Or"/>
              <a name="type" val="output"/>
            </comp>
            <comp lib="0" loc="(410,360)" name="Pin">
              <a name="appearance" val="NewPins"/>
              <a name="facing" val="west"/>
              <a name="label" val="O_And"/>
              <a name="type" val="output"/>
            </comp>
            <comp lib="0" loc="(410,430)" name="Pin">
              <a name="appearance" val="NewPins"/>
              <a name="facing" val="west"/>
              <a name="label" val="O_AB"/>
              <a name="type" val="output"/>
              <a name="width" val="2"/>
            </comp>
            <comp lib="1" loc="(130,190)" name="NOT Gate">
              <a name="facing" val="south"/>
            </comp>
            <comp lib="1" loc="(190,190)" name="NOT Gate">
              <a name="facing" val="south"/>
            </comp>
            <comp lib="1" loc="(250,220)" name="AND Gate">
              <a name="size" val="30"/>
            </comp>
            <comp lib="1" loc="(250,270)" name="AND Gate">
              <a name="size" val="30"/>
            </comp>
            <comp lib="1" loc="(250,310)" name="AND Gate">
              <a name="size" val="30"/>
            </comp>
            <comp lib="1" loc="(260,360)" name="NAND Gate">
              <a name="size" val="30"/>
            </comp>
            <comp lib="1" loc="(330,290)" name="OR Gate">
              <a name="size" val="30"/>
            </comp>
            <comp lib="1" loc="(380,250)" name="OR Gate">
              <a name="size" val="30"/>
            </comp>
            <comp lib="1" loc="(380,330)" name="OR Gate">
              <a name="size" val="30"/>
            </comp>
            <wire from="(110,130)" to="(110,150)"/>
            <wire from="(110,150)" to="(110,320)"/>
            <wire from="(110,150)" to="(130,150)"/>
            <wire from="(110,320)" to="(110,370)"/>
            <wire from="(110,320)" to="(220,320)"/>
            <wire from="(110,370)" to="(110,420)"/>
            <wire from="(110,370)" to="(220,370)"/>
            <wire from="(110,420)" to="(250,420)"/>
            <wire from="(130,150)" to="(130,160)"/>
            <wire from="(130,190)" to="(130,230)"/>
            <wire from="(130,230)" to="(130,280)"/>
            <wire from="(130,230)" to="(220,230)"/>
            <wire from="(130,280)" to="(220,280)"/>
            <wire from="(170,130)" to="(170,150)"/>
            <wire from="(170,150)" to="(170,260)"/>
            <wire from="(170,150)" to="(190,150)"/>
            <wire from="(170,260)" to="(170,350)"/>
            <wire from="(170,260)" to="(220,260)"/>
            <wire from="(170,350)" to="(170,410)"/>
            <wire from="(170,350)" to="(220,350)"/>
            <wire from="(170,410)" to="(250,410)"/>
            <wire from="(190,150)" to="(190,160)"/>
            <wire from="(190,190)" to="(190,210)"/>
            <wire from="(190,210)" to="(190,300)"/>
            <wire from="(190,210)" to="(220,210)"/>
            <wire from="(190,300)" to="(220,300)"/>
            <wire from="(250,220)" to="(340,220)"/>
            <wire from="(250,270)" to="(270,270)"/>
            <wire from="(250,310)" to="(270,310)"/>
            <wire from="(260,360)" to="(340,360)"/>
            <wire from="(270,270)" to="(270,280)"/>
            <wire from="(270,280)" to="(300,280)"/>
            <wire from="(270,300)" to="(270,310)"/>
            <wire from="(270,300)" to="(300,300)"/>
            <wire from="(270,430)" to="(410,430)"/>
            <wire from="(330,290)" to="(340,290)"/>
            <wire from="(340,220)" to="(340,240)"/>
            <wire from="(340,220)" to="(410,220)"/>
            <wire from="(340,240)" to="(350,240)"/>
            <wire from="(340,260)" to="(340,290)"/>
            <wire from="(340,260)" to="(350,260)"/>
            <wire from="(340,290)" to="(340,320)"/>
            <wire from="(340,290)" to="(410,290)"/>
            <wire from="(340,320)" to="(350,320)"/>
            <wire from="(340,340)" to="(340,360)"/>
            <wire from="(340,340)" to="(350,340)"/>
            <wire from="(340,360)" to="(410,360)"/>
            <wire from="(380,250)" to="(410,250)"/>
            <wire from="(380,330)" to="(410,330)"/>
          </circuit>
        </project>
        """;

    // Create test vector file - no <seq> column
    File testFile = new File(tempDir, "docsSample_exec_test.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B O_Nor O_Nand O_Xor O_Or O_And O_AB[2]\n");
      writer.write("0 0 1 1 0 0 0 00\n");
      writer.write("0 1 0 1 1 1 0 01\n");
      writer.write("1 0 0 1 1 1 0 10\n");
      writer.write("1 1 0 0 0 1 1 11\n");
    }

    // Load circuit from XML
    Loader loader = new Loader(null);
    ByteArrayInputStream xmlStream = new ByteArrayInputStream(
        circuitXml.getBytes(StandardCharsets.UTF_8));
    LogisimFile logisimFile = LogisimFile.load(xmlStream, loader);
    assertNotNull(logisimFile, "Circuit should load successfully");

    Circuit circuit = logisimFile.getCircuit("testVectorSample");
    assertNotNull(circuit, "testVectorSample circuit should exist");

    // Create project
    Project project = new Project(logisimFile);
    project.setCurrentCircuit(circuit);

    // Execute test vector using the same method CLI uses
    // This will actually run the tests and throw exceptions on failure
    TestVector vector = new TestVector(testFile.getAbsolutePath());

    CircuitState tempState = CircuitState.createRootState(project, circuit, Thread.currentThread());

    // Execute tests and collect expected errors based on the circuit's actual behavior
    List<Set<String>> expectedErrors = new ArrayList<>();

    // Test 1: A=0, B=0 - expects O_Or=0, O_And=0, but gets O_Or=1, O_And=1
    expectedErrors.add(Set.of("O_Or = 1 (expected 0)", "O_And = 1 (expected 0)"));

    // Test 2: A=0, B=1 - expects O_And=0, but gets O_And=1
    expectedErrors.add(Set.of("O_And = 1 (expected 0)"));

    // Test 3: A=1, B=0 - expects O_And=0, but gets O_And=1
    expectedErrors.add(Set.of("O_And = 1 (expected 0)"));

    // Test 4: A=1, B=1 - expects O_Or=1, O_And=1, but gets O_Or=0, O_And=0
    expectedErrors.add(Set.of("O_Or = 0 (expected 1)", "O_And = 0 (expected 1)"));

    int numPass = 0;
    int numFail = 0;

    TestVectorEvaluator evaluator;
    try {
      evaluator = new TestVectorEvaluator(tempState, vector);
    } catch (TestException e) {
      throw new AssertionError("Failed to construct evaluator: " + e);
    }

    final var passFail = evaluator.evaluate((row, report) -> {
      if (report == null || report.isEmpty()) {
      } else {
        // Verify errors match expected
        Set<String> expected = expectedErrors.get(row);
        Set<String> actualErrors = new HashSet<>();
        for (final var line : report) {
          actualErrors.add(line.toString());
        }
        if (!actualErrors.equals(expected)) {
          throw new AssertionError(String.format(
              "Test %d errors don't match expected.\nExpected: %s\nActual: %s",
              row + 1, expected, actualErrors));
        }
      }
    });

    // Verify all tests failed as expected
    assertEquals(0, passFail[0], "Expected 0 tests to pass");
    assertEquals(4, passFail[1], "Expected 4 tests to fail");
  }

  @Test
  public void testDocsSampleCorrectedExecution() throws Exception {
    // Create the corrected testVectorSample circuit XML (NAND changed to AND, location fixed)
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
        <main name="testVectorSampleCorrected"/>
        <circuit name="testVectorSampleCorrected">
          <a name="appearance" val="logisim_evolution"/>
          <a name="circuit" val="testVectorSampleCorrected"/>
          <a name="circuitnamedboxfixedsize" val="true"/>
          <a name="simulationFrequency" val="1.0"/>
          <comp lib="0" loc="(110,130)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="south"/>
            <a name="label" val="A"/>
          </comp>
          <comp lib="0" loc="(170,130)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="south"/>
            <a name="label" val="B"/>
          </comp>
          <comp lib="0" loc="(270,430)" name="Splitter">
            <a name="appear" val="right"/>
            <a name="facing" val="west"/>
          </comp>
          <comp lib="0" loc="(410,220)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="west"/>
            <a name="label" val="O_Nor"/>
            <a name="type" val="output"/>
          </comp>
          <comp lib="0" loc="(410,250)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="west"/>
            <a name="label" val="O_Nand"/>
            <a name="type" val="output"/>
          </comp>
          <comp lib="0" loc="(410,290)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="west"/>
            <a name="label" val="O_Xor"/>
            <a name="type" val="output"/>
          </comp>
          <comp lib="0" loc="(410,330)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="west"/>
            <a name="label" val="O_Or"/>
            <a name="type" val="output"/>
          </comp>
          <comp lib="0" loc="(410,360)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="west"/>
            <a name="label" val="O_And"/>
            <a name="type" val="output"/>
          </comp>
          <comp lib="0" loc="(410,430)" name="Pin">
            <a name="appearance" val="NewPins"/>
            <a name="facing" val="west"/>
            <a name="label" val="O_AB"/>
            <a name="type" val="output"/>
            <a name="width" val="2"/>
          </comp>
          <comp lib="1" loc="(130,190)" name="NOT Gate">
            <a name="facing" val="south"/>
          </comp>
          <comp lib="1" loc="(190,190)" name="NOT Gate">
            <a name="facing" val="south"/>
          </comp>
          <comp lib="1" loc="(250,220)" name="AND Gate">
            <a name="size" val="30"/>
          </comp>
          <comp lib="1" loc="(250,270)" name="AND Gate">
            <a name="size" val="30"/>
          </comp>
          <comp lib="1" loc="(250,310)" name="AND Gate">
            <a name="size" val="30"/>
          </comp>
          <comp lib="1" loc="(250,360)" name="AND Gate">
            <a name="size" val="30"/>
          </comp>
          <comp lib="1" loc="(330,290)" name="OR Gate">
            <a name="size" val="30"/>
          </comp>
          <comp lib="1" loc="(380,250)" name="OR Gate">
            <a name="size" val="30"/>
          </comp>
          <comp lib="1" loc="(380,330)" name="OR Gate">
            <a name="size" val="30"/>
          </comp>
          <wire from="(110,130)" to="(110,150)"/>
          <wire from="(110,150)" to="(110,320)"/>
          <wire from="(110,150)" to="(130,150)"/>
          <wire from="(110,320)" to="(110,370)"/>
          <wire from="(110,320)" to="(220,320)"/>
          <wire from="(110,370)" to="(110,420)"/>
          <wire from="(110,370)" to="(220,370)"/>
          <wire from="(110,420)" to="(250,420)"/>
          <wire from="(130,150)" to="(130,160)"/>
          <wire from="(130,190)" to="(130,230)"/>
          <wire from="(130,230)" to="(130,280)"/>
          <wire from="(130,230)" to="(220,230)"/>
          <wire from="(130,280)" to="(220,280)"/>
          <wire from="(170,130)" to="(170,150)"/>
          <wire from="(170,150)" to="(170,260)"/>
          <wire from="(170,150)" to="(190,150)"/>
          <wire from="(170,260)" to="(170,350)"/>
          <wire from="(170,260)" to="(220,260)"/>
          <wire from="(170,350)" to="(170,410)"/>
          <wire from="(170,350)" to="(220,350)"/>
          <wire from="(170,410)" to="(250,410)"/>
          <wire from="(190,150)" to="(190,160)"/>
          <wire from="(190,190)" to="(190,210)"/>
          <wire from="(190,210)" to="(190,300)"/>
          <wire from="(190,210)" to="(220,210)"/>
          <wire from="(190,300)" to="(220,300)"/>
          <wire from="(250,220)" to="(340,220)"/>
          <wire from="(250,270)" to="(270,270)"/>
          <wire from="(250,310)" to="(270,310)"/>
          <wire from="(250,360)" to="(340,360)"/>
          <wire from="(270,270)" to="(270,280)"/>
          <wire from="(270,280)" to="(300,280)"/>
          <wire from="(270,300)" to="(270,310)"/>
          <wire from="(270,300)" to="(300,300)"/>
          <wire from="(270,430)" to="(410,430)"/>
          <wire from="(330,290)" to="(340,290)"/>
          <wire from="(340,220)" to="(340,240)"/>
          <wire from="(340,220)" to="(410,220)"/>
          <wire from="(340,240)" to="(350,240)"/>
          <wire from="(340,260)" to="(340,290)"/>
          <wire from="(340,260)" to="(350,260)"/>
          <wire from="(340,290)" to="(340,320)"/>
          <wire from="(340,290)" to="(410,290)"/>
          <wire from="(340,320)" to="(350,320)"/>
          <wire from="(340,340)" to="(340,360)"/>
          <wire from="(340,340)" to="(350,340)"/>
          <wire from="(340,360)" to="(410,360)"/>
          <wire from="(380,250)" to="(410,250)"/>
          <wire from="(380,330)" to="(410,330)"/>
        </circuit>
        </project>
        """;

    // Create test vector file (same as before)
    File testFile = new File(tempDir, "docsSample_corrected_test.txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("A B O_Nor O_Nand O_Xor O_Or O_And O_AB[2]\n");
      writer.write("0 0 1 1 0 0 0 00\n");
      writer.write("0 1 0 1 1 1 0 01\n");
      writer.write("1 0 0 1 1 1 0 10\n");
      writer.write("1 1 0 0 0 1 1 11\n");
    }

    // Load circuit from XML
    Loader loader = new Loader(null);
    ByteArrayInputStream xmlStream = new ByteArrayInputStream(
        circuitXml.getBytes(StandardCharsets.UTF_8));
    LogisimFile logisimFile = LogisimFile.load(xmlStream, loader);
    assertNotNull(logisimFile, "Circuit should load successfully");

    Circuit circuit = logisimFile.getCircuit("testVectorSampleCorrected");
    assertNotNull(circuit, "testVectorSampleCorrected circuit should exist");

    // Create project
    Project project = new Project(logisimFile);
    project.setCurrentCircuit(circuit);

    // Execute test vector using the same method CLI uses
    TestVector vector = new TestVector(testFile.getAbsolutePath());

    CircuitState tempState = CircuitState.createRootState(project, circuit, Thread.currentThread());

    // Execute tests - with the corrected circuit, all tests should pass

    ArrayList<Integer> steps = new ArrayList<>();
    for (int i = 0; i < vector.data.size(); i++) {
      steps.add(i);
    }

    TestVectorEvaluator evaluator;
    try {
      evaluator = new TestVectorEvaluator(tempState, vector, steps);
    } catch (TestException e) {
      throw new AssertionError("Failed to construct evaluator: " + e);
    }

    final var passFail = evaluator.evaluate((row, report) -> {
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
            String.format("Test %d failed unexpectedly with the corrected circuit:\n",
                row + 1, errorDetails.toString()));
      }
    });

    // Verify all tests passed as expected
    assertEquals(4, passFail[0], "Expected 4 tests to pass with the corrected circuit");
    assertEquals(0, passFail[1], "Expected 0 tests to fail with the corrected circuit");
  }

}
