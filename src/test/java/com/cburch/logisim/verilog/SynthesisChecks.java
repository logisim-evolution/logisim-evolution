/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import static com.cburch.logisim.verilog.ImportChecks.require;

import com.cburch.logisim.Main;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

/** Real Yosys checks, including process failure, memory fallback and paths containing spaces. */
public final class SynthesisChecks {
  private SynthesisChecks() {}

  public static void main(String[] args) {
    try {
      Main.headless = true;
      run(Path.of(args[0]).toAbsolutePath(), args[1]);
      System.exit(0);
    } catch (Throwable exception) {
      exception.printStackTrace();
      System.exit(1);
    }
  }

  static void run(Path workspace, String executable) throws Exception {
    var out = workspace.resolve("build/logisim-import/synthesis-checks");
    Files.createDirectories(out);
    for (String name : List.of("word_pipeline", "native_memory", "hierarchy", "memory",
             "multiplier", "add_sub", "buffer", "display_controller")) {
      var source = workspace.resolve("examples/" + name + "/" + name + ".v");
      var file = LogisimFile.createNew(new Loader(null), null);
      var options = options(executable, source, name, YosysRunner.Memories.AUTO);
      var result =
          VerilogImporter.synthesize(options, file, VerilogImporter.reservedNames(file), s -> {});
      ImportChecks.commitAndSave(result, out.resolve(name + ".circ"));
      System.out.println("Passed Verilog synthesis: " + name);
    }

    var directory = out.resolve("sources with spaces [literal] $money;");
    Files.createDirectories(directory);
    var inc = directory.resolve("include files");
    Files.createDirectories(inc);
    Files.writeString(inc.resolve("width.vh"), "`define WIDTH 8\n");
    var leaf = directory.resolve("leaf.v");
    Files.writeString(
        leaf, "module leaf(input [7:0] a, output [7:0] y); assign y=a+`INCREMENT; endmodule\n");
    var source = directory.resolve("top.v");
    Files.writeString(source,
        "`include \"width.vh\"\nmodule top(input [`WIDTH-1:0] a, output [`WIDTH-1:0] y); leaf "
        + "u(a,y); endmodule\n");
    var options = new YosysRunner.Options(executable, List.of(source, leaf), "top", List.of(inc),
        List.of("INCREMENT=3"), directory, false, "error", YosysRunner.Memories.AUTO);
    var file = LogisimFile.createNew(new Loader(null), null);
    var result =
        VerilogImporter.synthesize(options, file, VerilogImporter.reservedNames(file), s -> {});
    ImportChecks.commitAndSave(result, out.resolve("multi_file.circ"));
    var vectors = out.resolve("multi_file.txt");
    Files.writeString(vectors, "a y\n0 3\n1 4\n253 0\n255 2\n");
    ImportChecks.simulate(file, result.top().getName(), vectors);
    System.out.println("Passed source lists, include paths with spaces, defines and simulation");

    source = directory.resolve("wide.sv");
    Files.writeString(source,
        "module wide(input logic [63:0] a,b, output logic [63:0] y); assign y=a+b; endmodule\n");
    options = new YosysRunner.Options(executable, List.of(Path.of("wide.sv")), "", List.of(),
        List.of(), directory, true, "error", YosysRunner.Memories.AUTO);
    file = LogisimFile.createNew(new Loader(null), null);
    result =
        VerilogImporter.synthesize(options, file, VerilogImporter.reservedNames(file), s -> {});
    ImportChecks.commitAndSave(result, out.resolve("wide.circ"));
    vectors = out.resolve("wide.txt");
    Files.writeString(vectors,
        "a b y\n9223372036854775807 1 9223372036854775808\n"
            + "18446744073709551615 1 0\n9223372036854775808 0 9223372036854775808\n");
    ImportChecks.simulate(file, result.top().getName(), vectors);
    System.out.println("Passed relative paths, automatic top, SystemVerilog and 64-bit arithmetic");

    source = directory.resolve("mixed.v");
    Files.writeString(source, """
        module mixed(input c1,c2,w1,w2,input [1:0] a,b,input [7:0] d,e,output [7:0] q,r);
          reg [7:0] words[0:3];
          always @(posedge c1) if(w1) words[a]<=d;
          always @(posedge c1) if(w2) words[b]<=e;
          assign q=words[a];
          reg [7:0] rom[0:3];
          initial begin rom[0]=1; rom[1]=2; rom[2]=3; rom[3]=4; end
          assign r=rom[b];
        endmodule
        """);
    var messages = new ArrayList<String>();
    file = LogisimFile.createNew(new Loader(null), null);
    result =
        VerilogImporter.synthesize(options(executable, source, "mixed", YosysRunner.Memories.AUTO),
            file, VerilogImporter.reservedNames(file), messages::add);
    require(messages.stream().anyMatch(s -> s.contains("expanding memory")),
        "Missing fallback diagnostic");
    require(
        result.top().getNonWires().stream().anyMatch(c -> c.getFactory().getName().equals("ROM")),
        "Selective fallback also expanded the compatible ROM");
    ImportChecks.commitAndSave(result, out.resolve("mixed.circ"));
    vectors = out.resolve("mixed.txt");
    Files.writeString(vectors,
        "c1 c2 w1 w2 a b d e q r\n0 0 1 0 0 1 42 9 0 2\n"
            + "1 0 1 0 0 1 42 9 42 2\n0 0 0 1 0 1 0 71 42 2\n1 1 0 1 0 1 0 71 42 2\n"
            + "0 0 0 0 1 3 0 0 71 4\n");
    ImportChecks.simulate(file, result.top().getName(), vectors);
    final var mixedSource = source;
    expectFailure(
        ()
            -> YosysRunner.synthesize(
                options(executable, mixedSource, "mixed", YosysRunner.Memories.NATIVE), s -> {}),
        "Multiple write ports");
    var logic = YosysRunner.synthesize(
        options(executable, source, "mixed", YosysRunner.Memories.LOGIC), s -> {});
    for (var name : logic.order) {
      require(YosysJson.map(logic.module(name), "cells")
                  .values()
                  .stream()
                  .noneMatch(c -> YosysJson.object(c).get("type").toString().startsWith("$mem")),
          "Logic mode retained memory");
    }
    System.out.println("Passed selective memory fallback, native-only rejection and logic mode");

    source = directory.resolve("outputless.v");
    Files.writeString(source, """
        module worker(input clk); reg [3:0] q; always @(posedge clk) q <= q+1; endmodule
        module outputless(input clk); worker u(clk); endmodule
        """);
    file = LogisimFile.createNew(new Loader(null), null);
    result = VerilogImporter.synthesize(
        options(executable, source, "outputless", YosysRunner.Memories.AUTO), file,
        VerilogImporter.reservedNames(file), s -> {});
    require(result.circuits().size() == 2, "Outputless hierarchy was removed");
    require(result.circuits().getFirst().getNonWires().stream().anyMatch(
                c -> c.getFactory().getName().equals("Register")),
        "Outputless register was removed");
    System.out.println("Passed outputless-module preservation");

    source = directory.resolve("bad.v");
    Files.writeString(source, "module bad(this is not verilog\n");
    final var badSource = source;
    expectFailure(
        ()
            -> YosysRunner.synthesize(
                options(executable, badSource, "bad", YosysRunner.Memories.AUTO), s -> {}),
        "Yosys failed");
    expectFailure(
        ()
            -> YosysRunner.synthesize(options(directory.resolve("missing-yosys").toString(),
                                          badSource, "bad", YosysRunner.Memories.AUTO),
                s -> {}),
        "Cannot run program");
    expectFailure(
        ()
            -> options(executable, badSource, "bad; shell injected", YosysRunner.Memories.AUTO),
        "Top must");
    System.out.println("Passed synthesis failure, missing executable and identifier validation");

    // Interrupt an actual synthesis worker after launch and require prompt termination.
    source = directory.resolve("slow.v");
    Files.writeString(source,
        "module slow(input [19:0] a, output [7:0] q); reg [7:0] m[0:1048575]; integer i; "
            + "initial for(i=0;i<1048576;i=i+1) m[i]=i; assign q=m[a]; endmodule\n");
    var slow = options(executable, source, "slow", YosysRunner.Memories.AUTO);
    var failure = new AtomicReference<Throwable>();
    var task = new Thread(() -> {
      try {
        YosysRunner.synthesize(slow, s -> {});
        failure.set(new AssertionError("Synthesis unexpectedly completed before cancellation"));
      } catch (Throwable exception) {
        failure.set(exception);
      }
    });
    task.start();
    Thread.sleep(300);
    task.interrupt();
    task.join(10000);
    require(!task.isAlive(), "Cancellation did not terminate worker");
    require(failure.get() instanceof CancellationException,
        "Unexpected cancellation result: " + failure.get());
    System.out.println("Passed cancellation of running Yosys process");
  }

  private static YosysRunner.Options options(
      String executable, Path source, String top, YosysRunner.Memories memories) {
    return new YosysRunner.Options(executable, List.of(source), top, List.of(), List.of(),
        source.getParent(), false, "0", memories);
  }

  @FunctionalInterface
  interface Operation {
    void run() throws Exception;
  }

  private static void expectFailure(Operation operation, String message) throws Exception {
    try {
      operation.run();
      throw new AssertionError("Expected failure containing " + message);
    } catch (Exception exception) {
      require(exception.getMessage().contains(message), "Unexpected failure: " + exception);
    }
  }
}
