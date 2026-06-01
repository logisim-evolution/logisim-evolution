/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.download;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.IoComponentsInformation;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.ToplevelHdlGeneratorFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XilinxDownloadTest {
  @TempDir Path tempDir;

  @Test
  void createDownloadScriptsKeepsJtagPlaceholdersAvailable() throws IOException {
    final var board = createXilinxBoard();
    final var rootNetList = mock(Netlist.class);
    final var resources = mock(MappableResourcesContainer.class);
    final var ioComponents = mock(IoComponentsInformation.class);

    when(rootNetList.numberOfClockTrees()).thenReturn(0);
    when(rootNetList.requiresGlobalClockConnection()).thenReturn(false);
    when(resources.getMappableResources()).thenReturn(Map.of());
    when(resources.getIoComponentInformation()).thenReturn(ioComponents);
    when(ioComponents.getComponents()).thenReturn(List.of());

    final var download =
        new XilinxDownload(
            tempDir.toString(),
            rootNetList,
            board,
            List.of("gate_entity.vhd"),
            List.of("gate_behavior.vhd"),
            HdlGeneratorFactory.VHDL,
            false);
    download.setMapableResources(resources);

    assertTrue(download.createDownloadScripts());

    final var script =
        Files.readString(
            tempDir.resolve("scripts").resolve("XilinxDownload"));
    assertTrue(
        script.contains(
            "assignFile -p 1 -file "
                + ToplevelHdlGeneratorFactory.FPGA_TOP_LEVEL_NAME
                + ".bit"));
    assertTrue(script.contains("program -p 1 -onlyFpga"));
    assertFalse(script.contains("{{JTAGPos}}"));
    assertFalse(script.contains("{{fileBaseName}}"));
  }

  private static BoardInformation createXilinxBoard() {
    final var board = new BoardInformation();
    board.setBoardName("Xilinx test board");
    board.fpga.set(
        50_000_000L,
        "P123",
        "Float",
        "Default",
        "Spartan6",
        "XC6SLX16",
        "CSG324",
        "2",
        "Xilinx",
        "Float",
        false,
        "1",
        null,
        "2");
    return board;
  }
}
