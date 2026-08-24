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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Propagator;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TtyInterfaceTest {

  @TempDir File tempDir;

  @Test
  public void testVectorHeaderIncludesBitWidth() {
    assertEquals("a", TtyInterface.formatTestVectorHeader("a", 1));
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

  @Test
  void runReturnsFailureWhenInputCannotBeOpened() {
    final var args = startupFor(new File(tempDir, "missing.circ"), TtyInterface.FORMAT_TABLE);

    assertEquals(TtyInterface.EXIT_FAILURE, TtyInterface.run(args));
  }

  @ParameterizedTest
  @ValueSource(ints = {TtyInterface.FORMAT_STATISTICS, TtyInterface.FORMAT_TABLE})
  void analysisOnlyModesReturnSuccess(int format) {
    final var args = startupFor(saveCircuit("analysis.circ"), format);

    assertEquals(TtyInterface.EXIT_SUCCESS, TtyInterface.run(args));
  }

  @Test
  void missingMemoryTargetReturnsFailure() {
    final var args = startupFor(saveCircuit("load.circ"), TtyInterface.FORMAT_HALT);
    final var loads = new HashMap<String, File>();
    loads.put("missing", new File(tempDir, "memory.hex"));
    when(args.getMemoryLoadFiles()).thenReturn(loads);

    assertEquals(TtyInterface.EXIT_FAILURE, TtyInterface.run(args));
  }

  @Test
  void missingTtyComponentReturnsFailure() {
    final var args = startupFor(saveCircuit("tty.circ"), TtyInterface.FORMAT_TTY);

    assertEquals(TtyInterface.EXIT_FAILURE, TtyInterface.run(args));
  }

  @Test
  void failedFpgaDownloadReturnsFailure() {
    final var args = startupFor(saveCircuit("fpga.circ"), TtyInterface.FORMAT_HALT);
    when(args.isFpgaDownload()).thenReturn(true);
    when(args.fpgaDownload(any(Project.class))).thenReturn(false);

    assertEquals(TtyInterface.EXIT_FAILURE, TtyInterface.run(args));
  }

  @Test
  void missingRamForSaveOverridesSuccessfulSimulationStatus() {
    final var state = mock(CircuitState.class);
    final var circuit = mock(Circuit.class);
    when(state.getCircuit()).thenReturn(circuit);
    when(circuit.getNonWires()).thenReturn(Set.of());
    when(state.getSubstates()).thenReturn(Set.of());

    assertEquals(
        TtyInterface.EXIT_FAILURE,
        TtyInterface.finishSimulation(
            state, new File(tempDir, "memory.hex"), TtyInterface.EXIT_SUCCESS));
  }

  @Test
  void simulationStatusIsReturnedWhenNoSaveWasRequested() {
    assertEquals(
        TtyInterface.EXIT_SUCCESS,
        TtyInterface.finishSimulation(null, null, TtyInterface.EXIT_SUCCESS));
    assertEquals(
        TtyInterface.EXIT_OSCILLATION,
        TtyInterface.finishSimulation(null, null, TtyInterface.EXIT_OSCILLATION));
    assertEquals(
        TtyInterface.EXIT_FAILURE,
        TtyInterface.finishSimulation(null, null, TtyInterface.EXIT_FAILURE));
  }

  @Test
  void oscillationReturnsItsExistingStatus() {
    final var state = mock(CircuitState.class);
    final var propagator = mock(Propagator.class);
    when(state.getPropagator()).thenReturn(propagator);
    when(propagator.isOscillating()).thenReturn(true);

    assertEquals(
        TtyInterface.EXIT_OSCILLATION,
        TtyInterface.runSimulation(
            state, new ArrayList<>(), null, TtyInterface.FORMAT_HALT));
  }

  private Startup startupFor(File circuitFile, int format) {
    final var args = mock(Startup.class);
    when(args.getFilesToOpen()).thenReturn(List.of(circuitFile));
    when(args.getSubstitutions()).thenReturn(Map.of());
    when(args.getMemoryLoadFiles()).thenReturn(new HashMap<>());
    when(args.getTtyFormat()).thenReturn(format);
    return args;
  }

  private File saveCircuit(String name) {
    final var loader = new RecordingLoader();
    final var file = LogisimFile.createNew(loader, null);
    final var path = new File(tempDir, name);
    assertTrue(loader.save(file, path), loader.errors());
    return path;
  }

  private static class RecordingLoader extends Loader {
    private final List<String> errors = new ArrayList<>();

    private RecordingLoader() {
      super(null);
    }

    private String errors() {
      return String.join("\n", errors);
    }

    @Override
    public void showError(String description) {
      errors.add(description);
    }
  }
}
