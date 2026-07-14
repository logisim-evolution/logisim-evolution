package com.cburch.logisim.util;

import com.cburch.logisim.prefs.AppPreferences;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SoftwaresTest {

  @Test
  public void questaValidationCommandDoesNotForceVhdl1993(@TempDir Path tmpDir)
      throws Exception {
    final var command =
        Softwares.buildQuestaValidationCommand("questa", tmpDir.resolve("tmp.vhd").toFile());

    assertFalse(command.contains("-93"));
    assertTrue(command.contains("-reportprogress"));
    assertTrue(command.contains("-work"));
    assertTrue(command.contains("work"));
  }

  @Test
  public void questaValidationCopiesBundledModelsimConfig(@TempDir Path tmpDir)
      throws Exception {
    Softwares.copyQuestaValidationConfig(tmpDir.toFile());

    final var configFile = tmpDir.resolve("modelsim.ini");
    assertTrue(Files.exists(configFile));
    assertTrue(Files.readString(configFile).contains("VHDL93 = 2002"));
  }

  @Test
  public void questaValidationCopiesConfiguredVhdlStandard(@TempDir Path tmpDir)
      throws Exception {
    final var previousStandard = AppPreferences.VHDL_STANDARD.get();
    try {
      AppPreferences.VHDL_STANDARD.set(AppPreferences.VHDL_STANDARD_2008);

      Softwares.copyQuestaValidationConfig(tmpDir.toFile());

      final var config = Files.readString(tmpDir.resolve("modelsim.ini"));
      assertTrue(config.contains("VHDL93 = 2008"));
      assertFalse(config.contains("VHDL93 = 2002"));
    } finally {
      AppPreferences.VHDL_STANDARD.set(previousStandard);
    }
  }
}
