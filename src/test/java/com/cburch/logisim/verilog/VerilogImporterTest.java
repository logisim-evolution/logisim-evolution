/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import com.cburch.logisim.Main;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

class VerilogImporterTest {
  @TempDir Path temporary;

  @Test
  void importsReferenceDesignsAndSimulatesAfterSaveReload() throws Exception {
    boolean wasHeadless = Main.headless;
    Main.headless = true;
    try {
      copy("cases.json");
      var manifest = temporary.resolve("cases.json");
      for (var value : YosysJson.array(YosysJson.read(manifest).get("cases"))) {
        var name = YosysJson.object(value).get("name").toString();
        copy(name + ".json");
        copy(name + ".txt");
      }
      ImportChecks.batch(manifest);
    } finally {
      Main.headless = wasHeadless;
    }
  }

  @Test
  void resolvesTriStateDriversAndBidirectionalPorts() throws Exception {
    boolean wasHeadless = Main.headless;
    Main.headless = true;
    try {
      TristateChecks.json(temporary);
    } finally {
      Main.headless = wasHeadless;
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "YOSYS", matches = ".+")
  void synthesizesVerilogWithExternalYosys() throws Exception {
    boolean wasHeadless = Main.headless;
    Main.headless = true;
    try {
      SynthesisChecks.run(temporary, System.getenv("YOSYS"));
      TristateChecks.synthesis(temporary, System.getenv("YOSYS"));
    } finally {
      Main.headless = wasHeadless;
    }
  }

  private void copy(String name) throws IOException {
    try (var input = getClass().getResourceAsStream("/verilog/" + name)) {
      if (input == null) {
        throw new IOException("Missing fixture " + name);
      }
      Files.copy(input, temporary.resolve(name));
    }
  }
}
