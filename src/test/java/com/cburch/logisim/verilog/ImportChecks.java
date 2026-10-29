/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CancellationException;

/** Standalone entry point for the reference-design checks also exercised by JUnit. */
public final class ImportChecks {
  private ImportChecks() {}

  public static void main(String[] args) {
    try {
      Main.headless = true;
      if (args.length == 0) {
        codec();
        System.out.println("Passed JSON codec checks");
      } else if (args[0].equals("batch")) {
        batch(Path.of(args[1]));
      } else {
        var file = LogisimFile.createNew(new Loader(null), null);
        VerilogImporter.Result result;
        if (args[0].equals("json")) {
          result = VerilogImporter.readJson(Path.of(args[1]), args[4].equals("-") ? null : args[4],
              args[3], file, VerilogImporter.reservedNames(file), System.out::println);
        } else {
          var source = Path.of(args[1]).toAbsolutePath();
          var options = new YosysRunner.Options(args[5], List.of(source), args[4], List.of(),
              List.of(), source.getParent(), false, args[3], YosysRunner.Memories.AUTO);
          result = VerilogImporter.synthesize(
              options, file, VerilogImporter.reservedNames(file), System.out::println);
        }
        commitAndSave(result, Path.of(args[2]));
      }
      System.exit(0);
    } catch (Throwable exception) {
      exception.printStackTrace();
      System.exit(1);
    }
  }

  static void require(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  static void codec() throws Exception {
    String text = "{\"bits\":[2,\"0\",\"x\"],\"name\":\"a\\n\\u03bb\",\"unused\":null}";
    var parsed = YosysJson.parse(new StringReader(text));
    require(parsed.equals(YosysJson.parse(new StringReader(YosysJson.encode(parsed)))),
        "JSON round trip");
    for (String bad : List.of("{\"a\":1,\"a\":2}", "[1,]", "01", "{", "\"\\q\"", "true false")) {
      try {
        YosysJson.parse(new StringReader(bad));
        throw new AssertionError("Accepted malformed JSON: " + bad);
      } catch (ImportException expected) {
      }
    }
    Thread.currentThread().interrupt();
    try {
      YosysJson.parse(new StringReader(text));
      throw new AssertionError("Ignored cancellation");
    } catch (CancellationException expected) {
      Thread.interrupted();
    }
  }

  static void commitAndSave(VerilogImporter.Result result, Path path) throws Exception {
    var file = result.destination();
    int before = file.getCircuitCount();
    require(before == 1, "Conversion modified destination before commit");
    var project = new Project(file);
    try {
      var original = project.getCurrentCircuit();
      var action = result.action("Import Verilog");
      project.doAction(action);
      require(
          file.getCircuitCount() == before + result.circuits().size(), "Missing imported circuits");
      require(project.getCurrentCircuit() == result.top(), "Top was not selected");
      project.undoAction();
      require(file.getCircuitCount() == before, "Undo left circuits behind");
      require(project.getCurrentCircuit() == original, "Undo lost previous selection");
      project.redoAction();
      require(file.getCircuitCount() == before + result.circuits().size(), "Redo failed");
      file.setMainCircuit(result.top());
      try (var stream = Files.newOutputStream(path)) {
        file.write(stream, file.getLoader());
      }
      var loaded = new Loader(null).openLogisimFile(path.toFile());
      require(loaded.getCircuitCount() == file.getCircuitCount(), "Save/reload lost hierarchy");
      for (var circuit : loaded.getCircuits()) {
        var conflicts = circuit.getWidthIncompatibilityData();
        require(conflicts == null || conflicts.isEmpty(), "Save/reload width conflict");
      }
      System.out.println(
          "Passed: import, undo/redo, save/reload; " + result.componentCount() + " components");
    } finally {
      project.getSimulator().shutDown();
    }
  }

  static void batch(Path manifest) throws Exception {
    codec();
    failures(manifest.toAbsolutePath().getParent());
    var cases = YosysJson.array(YosysJson.read(manifest).get("cases"));
    int vectors = 0;
    for (var value : cases) {
      var entry = YosysJson.object(value);
      var name = entry.get("name").toString();
      var root = manifest.toAbsolutePath().getParent();
      var file = LogisimFile.createNew(new Loader(null), null);
      var result = VerilogImporter.readJson(root.resolve(name + ".json"),
          entry.get("top").toString(), entry.get("undefined").toString(), file,
          VerilogImporter.reservedNames(file), s -> {});
      var path = root.resolve(name + ".circ");
      commitAndSave(result, path);
      var loaded = new Loader(null).openLogisimFile(path.toFile());
      vectors += simulate(loaded, result.top().getName(), root.resolve(name + ".txt"));
      System.out.println("Passed " + name);
    }
    System.out.println(
        "Passed " + cases.size() + " reference designs and " + vectors + " simulation vectors");
  }

  static void failures(Path directory) throws Exception {
    var file = LogisimFile.createNew(new Loader(null), null);
    var input = directory.resolve("validation-input.json");
    String module = "\"attributes\":{},\"ports\":{\"a\":{\"direction\":\"input\",\"bits\":[2]},"
        + "\"y\":{\"direction\":\"output\",\"bits\":[2]}},\"cells\":{},\"netnames\":{}";
    String valid = "{\"modules\":{\"top\":{" + module + "}}}";
    for (var bad :
        List.of(valid.replace("\"cells\":{}", "\"cells\":{\"bad\":{\"type\":\"$unsupported\"}}"),
            valid.replace("\"input\"", "\"invalid-direction\""),
            valid.replace("\"bits\":[2]", "\"bits\":[\"x\"]"),
            valid.replace("\"bits\":[2]",
                "\"bits\":["
                    + "2,".repeat(64) + "2]"),
            valid.replace("\"cells\":{}", "\"cells\":{\"recursive\":{\"type\":\"top\"}}"),
            valid.replace("\"attributes\":{}", "\"attributes\":{\"blackbox\":1}"),
            valid.replace(
                "\"netnames\":{}", "\"netnames\":{\"q\":{\"attributes\":{\"init\":\"1\"}}}"))) {
      Files.writeString(input, bad);
      try {
        VerilogImporter.readJson(
            input, "top", "error", file, VerilogImporter.reservedNames(file), s -> {});
        throw new AssertionError("Accepted invalid design: " + bad);
      } catch (ImportException expected) {
        require(file.getCircuitCount() == 1, "Failed conversion changed destination");
      }
    }
    Files.writeString(input, valid);
    var result = VerilogImporter.readJson(
        input, "top", "error", file, VerilogImporter.reservedNames(file), s -> {});
    var project = new Project(file);
    try {
      var stale = VerilogImporter.readJson(
          input, "top", "error", file, VerilogImporter.reservedNames(file), s -> {});
      project.doAction(result.action("Import"));
      int undoCount = project.getUndoActions().size();
      try {
        project.doAction(stale.action("Stale import"));
        throw new AssertionError("Accepted stale circuit names");
      } catch (ImportException expected) {
        require(project.getUndoActions().size() == undoCount, "Failed import changed undo history");
      }
      var again = VerilogImporter.readJson(
          input, "top", "error", file, VerilogImporter.reservedNames(file), s -> {});
      require(!again.top().getName().equals(result.top().getName()), "Repeated import reused name");
      project.doAction(again.action("Import again"));
      require(file.getCircuitCount() == 3, "Repeated import replaced a circuit");
      project.undoAction();
      require(file.getCircuitCount() == 2, "Undo removed earlier import");
      Files.delete(input);
      System.out.println("Passed invalid-design atomicity and repeated imports");
    } finally {
      project.getSimulator().shutDown();
    }
  }

  static int simulate(LogisimFile file, String top, Path vectors) throws Exception {
    var project = new Project(file);
    try {
      var circuit = file.getCircuit(top);
      var state = CircuitState.createRootState(project, circuit, Thread.currentThread());
      var propagator = state.getPropagator();
      var pins = new HashMap<String, Component>();
      for (var component : circuit.getNonWires()) {
        if (component.getFactory() instanceof Pin) {
          pins.put(component.getAttributeSet().getValue(StdAttr.LABEL), component);
        }
      }
      var lines = Files.readAllLines(vectors);
      var header = lines.getFirst().trim().split("\\s+");
      for (var label : header) {
        require(pins.containsKey(label), "Missing pin " + label);
      }
      propagator.propagate();
      for (int row = 1; row < lines.size(); row++) {
        var values = lines.get(row).trim().split("\\s+");
        for (int i = 0; i < header.length; i++) {
          var component = pins.get(header[i]);
          var instance = Instance.getInstanceFor(component);
          if (Pin.FACTORY.isInputPin(instance)) {
            var pinState = state.getInstanceState(component);
            Pin.FACTORY.driveInputPin(
                pinState, vectorValue(values[i], Pin.FACTORY.getWidth(instance)));
            pinState.fireInvalidated();
          }
        }
        propagator.propagate();
        require(!propagator.isOscillating(), "Oscillation at " + vectors + ":" + row);
        for (int i = 0; i < header.length; i++) {
          var component = pins.get(header[i]);
          var instance = Instance.getInstanceFor(component);
          if (!Pin.FACTORY.isInputPin(instance)) {
            var actual = Pin.FACTORY.getValue(state.getInstanceState(component));
            var expected = vectorValue(values[i], Pin.FACTORY.getWidth(instance));
            require(actual.equals(expected),
                vectors + " row " + row + " " + header[i] + ": " + actual + " expected "
                    + expected);
          }
        }
      }
      return lines.size() - 1;
    } finally {
      project.getSimulator().shutDown();
    }
  }

  private static Value vectorValue(String text, BitWidth width) {
    if (!text.startsWith("b")) {
      return Value.createKnown(width, Long.parseUnsignedLong(text));
    }
    require(text.length() == width.getWidth() + 1, "Vector width mismatch: " + text);
    var bits = new Value[width.getWidth()];
    for (int i = 0; i < bits.length; i++) {
      bits[i] = switch (Character.toLowerCase(text.charAt(text.length() - 1 - i))) {
        case '0' -> Value.FALSE;
        case '1' -> Value.TRUE;
        case 'z', 'x' -> Value.UNKNOWN;
        case 'e' -> Value.ERROR;
        default -> throw new AssertionError("Invalid vector digit: " + text);
      };
    }
    return Value.create(bits);
  }
}
