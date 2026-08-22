/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AlteraDownloadTest {
  private static final String FIRST_CABLE = "USB-Blaster [1-1]";
  private static final String SECOND_CABLE = "USB-Blaster II [3-2]";
  private static final String MISSING_CABLE = "Intel FPGA Download Cable [9-9]";

  @Test
  void parsesCompleteQuartusCableNames() {
    assertEquals(
        List.of(FIRST_CABLE, SECOND_CABLE),
        AlteraDownload.getDevices(
            List.of(
                "Info: Quartus Prime Programmer",
                "1) " + FIRST_CABLE,
                "2) " + SECOND_CABLE,
                "Info: Command completed")));
  }

  @Test
  void singleCableRemainsAutomatic() {
    final var selection =
        AlteraDownload.selectHeadlessCable(List.of(FIRST_CABLE), "a different cable");

    assertEquals(FIRST_CABLE, selection.cableName());
    assertEquals(AlteraDownload.CableSelectionFailure.NONE, selection.failure());
  }

  @Test
  void multipleCablesRequireExactRequestedName() {
    final var devices = List.of(FIRST_CABLE, SECOND_CABLE);

    final var missing = AlteraDownload.selectHeadlessCable(devices, null);
    assertNull(missing.cableName());
    assertEquals(AlteraDownload.CableSelectionFailure.REQUIRED, missing.failure());
    final var missingMessage =
        AlteraDownload.getCableSelectionError(missing, devices, null);
    assertTrue(missingMessage.contains(FIRST_CABLE));
    assertTrue(missingMessage.contains(SECOND_CABLE));

    final var mismatch = AlteraDownload.selectHeadlessCable(devices, MISSING_CABLE);
    assertNull(mismatch.cableName());
    assertEquals(AlteraDownload.CableSelectionFailure.NOT_FOUND, mismatch.failure());
    final var mismatchMessage =
        AlteraDownload.getCableSelectionError(mismatch, devices, MISSING_CABLE);
    assertTrue(mismatchMessage.contains(MISSING_CABLE));
    assertTrue(mismatchMessage.contains(FIRST_CABLE));
    assertTrue(mismatchMessage.contains(SECOND_CABLE));

    final var exact = AlteraDownload.selectHeadlessCable(devices, SECOND_CABLE);
    assertEquals(SECOND_CABLE, exact.cableName());
    assertEquals(AlteraDownload.CableSelectionFailure.NONE, exact.failure());
  }

  @Test
  void programmerCommandPassesCableNameAsSingleArgument() {
    assertEquals(
        List.of(
            "quartus_pgm",
            "-c",
            SECOND_CABLE,
            "-m",
            "jtag",
            "-o",
            "P;LogisimToplevelShell.sof@2"),
        AlteraDownload.buildProgrammerCommand(
            "quartus_pgm", SECOND_CABLE, "P;LogisimToplevelShell.sof@2"));
  }
}
