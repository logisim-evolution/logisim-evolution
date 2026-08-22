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

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.TestVectorEvaluator;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Mem;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.RamAppearance;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RamTestVectorTest {

  @TempDir
  File tempDir;

  @Test
  void testRamSequentialExecution() throws Exception {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();

    circuit.setProject(project);
    project.setCurrentCircuit(circuit);

    // Use a small RAM so the test vector remains easy to understand.
    final var ramFactory = new Ram();
    final var ramAttrs = ramFactory.createAttributeSet();
    ramAttrs.setValue(Mem.ADDR_ATTR, BitWidth.create(2));
    ramAttrs.setValue(Mem.DATA_ATTR, BitWidth.create(8));

    final var ram =
        ramFactory.createComponent(Location.create(200, 150, true), ramAttrs);
    add(circuit, ram);

    /*
    * RAM port order is defined by RamAppearance.configurePorts():
    * address, data input, data output, OE, WE, clock.
    */
    final var addressEnd = ram.getEnd(RamAppearance.getAddrIndex(0, ramAttrs));
    final var dataInEnd = ram.getEnd(RamAppearance.getDataInIndex(0, ramAttrs));
    final var dataOutEnd = ram.getEnd(RamAppearance.getDataOutIndex(0, ramAttrs));
    final var weEnd = ram.getEnd(RamAppearance.getWEIndex(0, ramAttrs));
    final var clkEnd = ram.getEnd(RamAppearance.getClkIndex(0, ramAttrs));

    final var address =
        inputPin("address", BitWidth.create(2), addressEnd.getLocation());
    final var data =
        inputPin("data", BitWidth.create(8), dataInEnd.getLocation());
    final var write =
        inputPin("write", BitWidth.ONE, weEnd.getLocation());
    final var output =
        outputPin("out", BitWidth.create(8), dataOutEnd.getLocation());

    final var clock =
        Clock.FACTORY.createComponent(
            clkEnd.getLocation(),
            Clock.FACTORY.createAttributeSet());

    add(circuit, address);
    add(circuit, data);
    add(circuit, write);
    add(circuit, output);
    add(circuit, clock);

    final var testFile = new File(tempDir, "ram_sequential_test.txt");
    try (var writer = new FileWriter(testFile)) {
      writer.write("# Sequential RAM write/read test\n");
      writer.write("<set> <seq> address[2] data[8] write out[8]\n");
      writer.write("1 1 00 01011010 1 <DC>\n");
      writer.write("1 2 00 00000000 0 01011010\n");
      writer.write("1 3 01 10100101 1 01011010\n");
      writer.write("1 4 01 00000000 0 10100101\n");
    }

    final var vector = new TestVector(testFile);
    final var state =
        CircuitState.createRootState(project, circuit, Thread.currentThread());
    final var evaluator = new TestVectorEvaluator(state, vector);

    final var failures = new ArrayList<String>();

    final var result =
        evaluator.evaluate(
            (row, report) -> {
              if (report != null && !report.isEmpty()) {
                failures.add("Row " + (row + 1) + ": " + report);
              }
            });

    assertEquals(4, result[0], "All RAM test-vector rows should pass: " + failures);
    assertEquals(0, result[1], "RAM test-vector should have no failures: " + failures);
  }

  private static Component inputPin(String label, BitWidth width, Location location) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, label);
    attrs.setValue(StdAttr.WIDTH, width);
    return Pin.FACTORY.createComponent(location, attrs);
  }

  private static Component outputPin(String label, BitWidth width, Location location) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, label);
    attrs.setValue(StdAttr.WIDTH, width);
    attrs.setValue(Pin.ATTR_TYPE, Pin.OUTPUT);
    return Pin.FACTORY.createComponent(location, attrs);
  }

  private static void add(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
  }
}
