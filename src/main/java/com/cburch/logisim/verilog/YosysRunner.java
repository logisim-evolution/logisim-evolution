/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import static com.cburch.logisim.verilog.YosysJson.checkCancelled;
import static com.cburch.logisim.verilog.YosysJson.encode;
import static com.cburch.logisim.verilog.YosysJson.map;
import static com.cburch.logisim.verilog.YosysJson.object;
import static com.cburch.logisim.verilog.YosysJson.read;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Invokes Yosys without a shell. All temporary files belong to one import operation. */
public final class YosysRunner {
  public enum Memories { AUTO, NATIVE, LOGIC }

  public record Options(String executable, List<Path> sources, String top, List<Path> includes,
      List<String> defines, Path workingDirectory, boolean systemVerilog, String undefined,
      Memories memories) {
    public Options {
      if (workingDirectory == null || !Files.isDirectory(workingDirectory)) {
        throw new ImportException("Working directory does not exist");
      }
      workingDirectory = workingDirectory.toAbsolutePath().normalize();
      final var base = workingDirectory;
      sources = sources.stream().map(base::resolve).map(Path::normalize).toList();
      includes = includes.stream().map(base::resolve).map(Path::normalize).toList();
      sources = List.copyOf(sources);
      includes = List.copyOf(includes);
      defines = List.copyOf(defines);
      if (executable == null || executable.isBlank()) {
        throw new ImportException("Choose a Yosys executable");
      }
      if (sources.isEmpty()) {
        throw new ImportException("Choose at least one Verilog source");
      }
      if (top != null && !top.isBlank() && !top.matches("[A-Za-z_][A-Za-z0-9_$]*")) {
        throw new ImportException("Top must be a Verilog identifier");
      }
      if (!List.of("error", "0", "1").contains(undefined)) {
        throw new ImportException("Invalid undefined-value policy");
      }
      for (var source : sources) {
        if (!Files.isRegularFile(source)) {
          throw new ImportException("Missing source: " + source);
        }
      }
      for (var include : includes) {
        if (!Files.isDirectory(include)) {
          throw new ImportException("Missing include directory: " + include);
        }
      }
      for (var define : defines) {
        if (!define.matches("[A-Za-z_][A-Za-z0-9_]*(=[A-Za-z0-9_+'().-]+)?")) {
          throw new ImportException("Unsupported macro definition: " + define);
        }
      }
      if (memories == null) {
        throw new ImportException("Missing memory policy");
      }
    }
  }

  private YosysRunner() {}

  static String quoted(Path path) {
    var value = path.toAbsolutePath().normalize().toString().replace('\\', '/');
    if (value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
      throw new ImportException("Unsupported quote/newline in path: " + value);
    }
    return '"' + value + '"';
  }

  // Tcl forwards a list of arguments directly to Yosys, preserving spaces in -I values.
  private static String tcl(String value) {
    return "\""
        + value.replace("\\", "\\\\")
              .replace("\"", "\\\"")
              .replace("$", "\\$")
              .replace("[", "\\[")
              .replace("]", "\\]")
              .replace("\n", "\\n")
              .replace("\r", "\\r")
        + "\"";
  }

  private static String tcl(Path path) {
    return tcl(path.toAbsolutePath().normalize().toString().replace('\\', '/'));
  }

  static YosysDesign synthesize(Options options, Consumer<String> messages) throws IOException {
    Path directory = Files.createTempDirectory("logisim-verilog-");
    try {
      var raw = directory.resolve("raw.json");
      var source = directory.resolve("source.json");
      var mapped = directory.resolve("mapped.json");
      var script = new StringBuilder("yosys read_verilog");
      if (options.systemVerilog()) {
        script.append(" -sv");
      }
      for (var include : options.includes()) {
        script.append(" -I ").append(tcl(include));
      }
      for (var define : options.defines()) {
        script.append(" -D ").append(tcl(define));
      }
      for (var file : options.sources()) {
        script.append(' ').append(tcl(file));
      }
      script.append("\nyosys hierarchy -check ");
      script.append(options.top() == null || options.top().isBlank()
              ? "-auto-top"
              : "-top " + tcl(options.top()));
      // Preserve outputless modules before opt/clean can erase their internal logic.
      script.append("\nyosys proc\nyosys write_json ").append(tcl(raw)).append('\n');
      messages.accept("Reading and elaborating Verilog...");
      execute(options, directory, script.toString(), messages, true);
      var design = new YosysDesign(read(raw), options.top());
      design.preserveOutputlessModules();
      Files.writeString(source, encode(design.json));
      messages.accept("Mapping registers and memories...");
      execute(options, directory,
          "read_json " + quoted(source)
              + "\ntribuf\nopt\nmemory -nomap -nordff\nopt_clean\ncheck\nwrite_json "
              + quoted(mapped) + "\n",
          messages);
      design = new YosysDesign(read(mapped), design.top);
      boolean fallback = false;
      for (var name : design.order) {
        for (var entry : map(design.module(name), "cells").entrySet()) {
          var cell = object(entry.getValue());
          if (!cell.get("type").toString().startsWith("$mem")) {
            continue;
          }
          var reason = NativeMemory.reason(cell);
          if (options.memories() == Memories.LOGIC) {
            reason = "Logic memory mapping selected";
          }
          if (reason != null) {
            if (options.memories() == Memories.NATIVE) {
              throw new ImportException(name + "." + entry.getKey() + ": " + reason);
            }
            messages.accept(
                name + "." + entry.getKey() + ": expanding memory to logic (" + reason + ")");
            cell.computeIfAbsent("attributes", key -> new LinkedHashMap<String, Object>());
            map(cell, "attributes").put("logisim_memory_fallback", 1);
            fallback = true;
          }
        }
      }
      if (fallback) {
        Files.writeString(source, encode(design.json));
        execute(options, directory,
            "read_json " + quoted(source)
                + "\nmemory_map a:logisim_memory_fallback\nopt\nclean\ncheck\nwrite_json "
                + quoted(mapped) + "\n",
            messages);
        design = new YosysDesign(read(mapped), design.top);
      }
      return design;
    } finally {
      cleanup(directory, messages);
    }
  }

  private static void cleanup(Path directory, Consumer<String> messages) {
    boolean interrupted = Thread.interrupted();
    try {
      // Windows may release the redirected log handle just after process termination.
      for (int attempt = 0; attempt < 6; attempt++) {
        try (var paths = Files.walk(directory)) {
          for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(path);
          }
          return;
        } catch (IOException exception) {
          if (attempt == 5) {
            messages.accept("Could not remove synthesis temporary directory " + directory + ": "
                + exception.getMessage());
            return;
          }
          try {
            Thread.sleep(25L << attempt);
          } catch (InterruptedException cancellation) {
            interrupted = true;
          }
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static void execute(Options options, Path directory, String script,
      Consumer<String> messages) throws IOException {
    execute(options, directory, script, messages, false);
  }

  private static void execute(Options options, Path directory, String script,
      Consumer<String> messages, boolean tcl) throws IOException {
    checkCancelled();
    var scriptPath = directory.resolve(tcl ? "synthesis.tcl" : "synthesis.ys");
    var log = directory.resolve("yosys.log");
    Files.writeString(scriptPath, script);
    var process = new ProcessBuilder(
        options.executable(), "-Q", "-T", tcl ? "-c" : "-s", scriptPath.toString())
                      .directory(options.workingDirectory().toFile())
                      .redirectErrorStream(true)
                      .redirectOutput(log.toFile())
                      .start();
    try {
      while (!process.waitFor(100, TimeUnit.MILLISECONDS)) {
        checkCancelled();
      }
      var output = tail(log);
      messages.accept(output);
      if (process.exitValue() != 0) {
        throw new ImportException("Yosys failed (exit " + process.exitValue() + "):\n" + output);
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new CancellationException();
    } finally {
      if (process.isAlive()) {
        var descendants = process.descendants().toList();
        descendants.reversed().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        boolean interrupted = Thread.interrupted();
        try {
          process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
          interrupted = true;
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  }

  private static String tail(Path path) throws IOException {
    try (var channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
      long start = Math.max(0, channel.size() - 32768);
      channel.position(start);
      var buffer = ByteBuffer.allocate((int) (channel.size() - start));
      while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
      }
      buffer.flip();
      return (start == 0 ? "" : "[Earlier synthesis log omitted]\n")
          + StandardCharsets.UTF_8.decode(buffer);
    }
  }
}
