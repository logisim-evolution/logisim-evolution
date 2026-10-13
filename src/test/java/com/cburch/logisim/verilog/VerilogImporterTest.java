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
import org.junit.jupiter.api.io.TempDir;

class VerilogImporterTest {
  @TempDir Path temporary;

  @Test
  void importsReferenceDesignsAndSimulatesAfterSaveReload() throws Exception {
    Main.headless = true;
    copy("cases.json");
    var manifest = temporary.resolve("cases.json");
    for (var value : YosysJson.array(YosysJson.read(manifest).get("cases"))) {
      var name = YosysJson.object(value).get("name").toString();
      copy(name + ".json");
      copy(name + ".txt");
    }
    ImportChecks.batch(manifest);
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
