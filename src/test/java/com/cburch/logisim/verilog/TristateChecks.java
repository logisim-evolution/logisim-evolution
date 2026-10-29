/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import com.cburch.logisim.Main;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Floating, shared-driver and bidirectional hierarchy regressions. */
public final class TristateChecks {
  private TristateChecks() {}

  public static void main(String[] args) {
    try {
      Main.headless = true;
      var out = Path.of(args[0]).toAbsolutePath();
      Files.createDirectories(out);
      json(out);
      if (args.length > 1) {
        synthesis(out, args[1]);
      }
      System.exit(0);
    } catch (Throwable exception) {
      exception.printStackTrace();
      System.exit(1);
    }
  }

  static void json(Path out) throws Exception {
    var input = out.resolve("tristate.json");
    Files.writeString(input, """
        {"modules":{"shared":{"ports":{
          "a":{"direction":"input","bits":[2,3,4,5]},
          "b":{"direction":"input","bits":[6,7,8,9]},
          "ea":{"direction":"input","bits":[10]},
          "eb":{"direction":"input","bits":[11]},
          "io":{"direction":"inout","bits":[12,13,14,15]},
          "seen":{"direction":"output","bits":[12,13,14,15]}},
          "cells":{
            "a_driver":{"type":"$tribuf","connections":{"A":[2,3,4,5],"EN":[10],"Y":[12,13,14,15]}},
            "b_driver":{"type":"$tribuf","connections":{"A":[6,7,8,9],"EN":[11],"Y":[12,13,14,15]}}}}}}
        """);
    String rows = """
        a b ea eb io_in io_out seen
        5 10 0 0 bzzzz bzzzz bzzzz
        5 10 1 0 bzzzz 5 5
        5 10 0 0 bzzzz bzzzz bzzzz
        5 10 0 0 9 bzzzz 9
        5 10 1 0 9 5 bEE01
        5 10 1 1 bzzzz bEEEE bEEEE
        5 5 1 1 bzzzz 5 5
        5 5 0 0 bzzzz bzzzz bzzzz
        5 10 1 0 bzz10 5 b01EE
        5 10 0 0 bzzzz bzzzz bzzzz
        """;
    for (var policy : List.of("error", "0", "1")) {
      var file = LogisimFile.createNew(new Loader(null), null);
      var result = VerilogImporter.readJson(
          input, "shared", policy, file, VerilogImporter.reservedNames(file), s -> {});
      check(result, out.resolve("shared_" + policy), rows);
    }
    System.out.println("Passed shared tri-state drivers, external drive, contention and release "
        + "under all undefined policies");
    var fine = YosysJson.read(input);
    var module = YosysJson.object(YosysJson.map(fine, "modules").get("shared"));
    for (var port : YosysJson.map(module, "ports").values()) {
      var definition = YosysJson.object(port);
      definition.put("bits", List.of(YosysJson.array(definition.get("bits")).getFirst()));
    }
    for (var value : YosysJson.map(module, "cells").values()) {
      var cell = YosysJson.object(value);
      cell.put("type", "$_TBUF_");
      var cn = YosysJson.map(cell, "connections");
      cn.put("E", cn.remove("EN"));
      cn.replaceAll((key, bits) -> List.of(YosysJson.array(bits).getFirst()));
    }
    Files.writeString(input, YosysJson.encode(fine));
    var file = LogisimFile.createNew(new Loader(null), null);
    check(VerilogImporter.readJson(
              input, "shared", "error", file, VerilogImporter.reservedNames(file), s -> {}),
        out.resolve("fine"), """
        a b ea eb io_in io_out seen
        1 0 0 0 bz bz bz
        1 0 1 0 bz 1 1
        1 0 1 1 bz bE bE
        1 0 0 0 bz bz bz
        """);
    for (var value : YosysJson.map(module, "cells").values()) {
      var cell = YosysJson.object(value);
      cell.put("type", "$mux");
      var cn = YosysJson.map(cell, "connections");
      cn.put("B", cn.get("A"));
      cn.put("A", List.of("z"));
      cn.put("S", cn.remove("E"));
    }
    Files.writeString(input, YosysJson.encode(fine));
    file = LogisimFile.createNew(new Loader(null), null);
    check(VerilogImporter.readJson(
              input, "shared", "error", file, VerilogImporter.reservedNames(file), s -> {}),
        out.resolve("raw_mux"), """
        a b ea eb io_in io_out seen
        1 0 0 0 bz bz bz
        1 0 1 0 bz 1 1
        1 0 1 1 bz bE bE
        1 0 0 0 bz bz bz
        """);
    var ports = YosysJson.map(module, "ports");
    ports.put("overlap", ports.get("io"));
    Files.writeString(input, YosysJson.encode(fine));
    file = LogisimFile.createNew(new Loader(null), null);
    try {
      VerilogImporter.readJson(
          input, "shared", "error", file, VerilogImporter.reservedNames(file), s -> {});
      throw new AssertionError("Accepted aliased bidirectional ports");
    } catch (ImportException expected) {
      ImportChecks.require(
          expected.getMessage().contains("Aliased inout ports"), expected.getMessage());
      ImportChecks.require(file.getCircuitCount() == 1, "Rejected inout mutated destination");
    }
  }

  static void synthesis(Path out, String executable) throws Exception {
    var source = out.resolve("tristate.v");
    Files.writeString(source, """
        module leaf(input [3:0] d, input en, inout [3:0] io, output [3:0] seen);
          assign io = en ? d : 4'bzzzz;
          assign seen = io;
        endmodule
        module middle(input [3:0] d, input en, inout [3:0] io, output [3:0] seen);
          leaf u(d,en,io,seen);
        endmodule
        module nested(input [3:0] a,b, input ea,eb, inout [3:0] io,
                      output [3:0] seen,sa,sb,isolated,mixed);
          middle u(a,ea,io,sa);
          middle v(b,eb,io,sb);
          leaf disconnected(.d(4'b0),.en(1'b0),.seen(isolated));
          assign seen=io;
          assign mixed=4'b10zz;
        endmodule
        module out_driver(input [3:0] d,input en,output [3:0] y);
          assign y=en?d:4'bzzzz;
        endmodule
        module outputs_shared(input [3:0] a,b,input ea,eb,output [3:0] y);
          out_driver u(a,ea,y),v(b,eb,y);
        endmodule
        module sliced(input [3:0] a,b,input ea,eb,inout [5:0] io,output [7:0] permuted);
          assign io[3:0]=ea?a:4'bzzzz;
          assign io[5:2]=eb?b:4'bzzzz;
          assign permuted={io[0],io[5],io[0],io[2],io[3],io[4],io[1],io[5]};
        endmodule
        module wide(input [63:0] d,input disable,inout [63:0] io,output [63:0] seen);
          assign io=disable?64'bz:d;
          assign seen=io;
        endmodule
        module read_only(inout [3:0] io, output [3:0] seen);
          assign seen=io;
        endmodule
        module push_pull(input [3:0] d, inout [3:0] io, output [3:0] seen);
          assign io=d;
          assign seen=io;
        endmodule
        module open_drain(input pull_low, inout io, output seen);
          assign io=pull_low?1'b0:1'bz;
          assign seen=io;
        endmodule
        """);
    var options = new YosysRunner.Options(executable, List.of(source), "nested", List.of(),
        List.of(), out, false, "error", YosysRunner.Memories.AUTO);
    var file = LogisimFile.createNew(new Loader(null), null);
    var result =
        VerilogImporter.synthesize(options, file, VerilogImporter.reservedNames(file), s -> {});
    check(result, out.resolve("nested"), """
        a b ea eb io_in io_out seen sa sb isolated mixed
        5 10 0 0 bzzzz bzzzz bzzzz bzzzz bzzzz bzzzz b10zz
        5 10 1 0 bzzzz 5 5 5 5 bzzzz b10zz
        5 10 0 0 bzzzz bzzzz bzzzz bzzzz bzzzz bzzzz b10zz
        5 10 0 0 9 bzzzz 9 9 9 bzzzz b10zz
        5 10 1 0 9 5 bEE01 bEE01 bEE01 bzzzz b10zz
        5 10 1 1 bzzzz bEEEE bEEEE bEEEE bEEEE bzzzz b10zz
        5 5 1 1 bzzzz 5 5 5 5 bzzzz b10zz
        5 5 0 0 bzzzz bzzzz bzzzz bzzzz bzzzz bzzzz b10zz
        """);
    System.out.println("Passed synthesized nested inout hierarchy, disconnected ports and mixed "
        + "high-impedance constants");
    check(synthesize(source, "outputs_shared", executable), out.resolve("outputs_shared"), """
        a b ea eb y
        5 10 0 0 bzzzz
        5 10 1 0 5
        5 10 1 1 bEEEE
        5 10 0 0 bzzzz
        """);
    check(synthesize(source, "sliced", executable), out.resolve("sliced"), """
        a b ea eb io_in io_out permuted
        5 10 0 0 bzzzzzz bzzzzzz bzzzzzzzz
        5 10 1 0 bzzzzzz bzz0101 b1z110z0z
        5 10 1 1 bzzzzzz b10EE01 b111EE001
        5 10 0 0 bzzzzzz bzzzzzz bzzzzzzzz
        5 10 0 0 42 bzzzzzz 75
        """);
    var released = "b"
        + "z".repeat(64);
    check(synthesize(source, "wide", executable), out.resolve("wide"),
        "d disable io_in io_out seen\n"
            + "9223372036854775808 0 " + released + " 9223372036854775808 9223372036854775808\n"
            + "9223372036854775808 1 " + released + " " + released + " " + released + "\n"
            + "18446744073709551615 0 " + released
            + " 18446744073709551615 18446744073709551615\n"
            + "0 1 " + released + " " + released + " " + released + "\n");
    check(synthesize(source, "read_only", executable), out.resolve("read_only"), """
        io_in io_out seen
        bzzzz bzzzz bzzzz
        9 bzzzz 9
        bzzzz bzzzz bzzzz
        """);
    check(synthesize(source, "push_pull", executable), out.resolve("push_pull"), """
        d io_in io_out seen
        5 bzzzz 5 5
        5 9 5 bEE01
        9 bzzzz 9 9
        """);
    check(synthesize(source, "open_drain", executable), out.resolve("open_drain"), """
        pull_low io_in io_out seen
        0 bz bz bz
        1 bz 0 0
        0 bz bz bz
        0 1 bz 1
        """);
    System.out.println("Passed output-only driver hierarchy, overlapping slices, repeated/permuted "
        + "lanes, 64-bit active-low enable and read-only inout");
  }

  private static VerilogImporter.Result synthesize(Path source, String top, String executable)
      throws Exception {
    var options = new YosysRunner.Options(executable, List.of(source), top, List.of(), List.of(),
        source.getParent(), false, "error", YosysRunner.Memories.AUTO);
    var file = LogisimFile.createNew(new Loader(null), null);
    return VerilogImporter.synthesize(options, file, VerilogImporter.reservedNames(file), s -> {});
  }

  private static void check(VerilogImporter.Result result, Path base, String rows)
      throws Exception {
    var circuit = Path.of(base + ".circ");
    var vectors = Path.of(base + ".txt");
    Files.writeString(vectors, rows);
    ImportChecks.commitAndSave(result, circuit);
    var loaded = new Loader(null).openLogisimFile(circuit.toFile());
    ImportChecks.simulate(loaded, result.top().getName(), vectors);
  }
}
