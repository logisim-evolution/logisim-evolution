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
import com.cburch.logisim.fpga.data.DriveStrength;
import com.cburch.logisim.fpga.data.FpgaIoInformationContainer;
import com.cburch.logisim.fpga.data.IoComponentTypes;
import com.cburch.logisim.fpga.data.IoComponentsInformation;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.PinActivity;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.data.SevenSegmentScanningDriving;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScanningConstraintTest {

  @Test
  void vivadoScanningConstraintsUsePinLocationsAndIoStandards() throws Exception {
    final var board = createBoard();
    final var netlist = createNetlistWithoutUserClock();
    final var downloader =
        new VivadoDownload("build/test-scanning-constraints", netlist, board, List.of(), List.of());
    downloader.setMappableResources(createScanningResources());

    final var constraints = getPinLocStrings(downloader);

    assertTrue(
        constraints.contains("set_property PACKAGE_PIN T5 [get_ports {Displ0_Segment_A}]"),
        () -> String.join("\n", constraints));
    assertTrue(
        constraints.contains(
            "    set_property IOSTANDARD LVCMOS33 [get_ports {Displ0_Segment_A}]"));
    assertTrue(constraints.contains("set_property PACKAGE_PIN P9 [get_ports {Displ0Select[0]}]"));
    assertTrue(constraints.contains("set_property PACKAGE_PIN N14 [get_ports {fpgaGlobalClock}]"));
    assertTrue(
        constraints.contains("    set_property IOSTANDARD LVCMOS33 [get_ports {fpgaGlobalClock}]"));
    assertFalse(constraints.contains("set_property PACKAGE_PIN Displ0_Segment_A [get_ports {T5}]"));
  }

  @Test
  void xilinxScanningConstraintsUsePinLocationsAndIoStandards() throws Exception {
    final var board = createBoard();
    final var netlist = createNetlistWithoutUserClock();
    final var downloader =
        new XilinxDownload(
            "build/test-scanning-constraints",
            netlist,
            board,
            List.of(),
            List.of(),
            HdlGeneratorFactory.VHDL,
            false);
    downloader.setMappableResources(createScanningResources());

    final var constraints = getPinLocStrings(downloader);

    assertTrue(
        constraints.stream()
            .anyMatch(
                line ->
                    line.startsWith("NET \"Displ0_Segment_A\" LOC = \"T5\"")
                        && line.contains("PULLUP")
                        && line.contains("DRIVE = 4")
                        && line.contains("IOSTANDARD = LVCMOS33")),
        () -> String.join("\n", constraints));
    assertTrue(
        constraints.stream()
            .anyMatch(
                line ->
                    line.startsWith("NET \"Displ0Select[0]\" LOC = \"P9\"")
                        && line.contains("PULLUP")
                        && line.contains("DRIVE = 4")
                        && line.contains("IOSTANDARD = LVCMOS33")));
    assertTrue(
        constraints.contains("NET \"fpgaGlobalClock\" LOC = \"N14\" | IOSTANDARD = LVCMOS33 ;"));
    assertFalse(constraints.contains("NET \"T5\" LOC=\"Displ0_Segment_A\";"));
  }

  private static BoardInformation createBoard() {
    final var board = new BoardInformation();
    board.fpga.set(
        100_000_000,
        "N14",
        "Float",
        "LVCMOS33",
        "Artix-7",
        "xc7a35t",
        "ftg256",
        "-1",
        "VIVADO",
        "Float",
        false,
        "1",
        "",
        "2");
    return board;
  }

  private static Netlist createNetlistWithoutUserClock() {
    final var netlist = mock(Netlist.class);
    when(netlist.numberOfClockTrees()).thenReturn(0);
    when(netlist.requiresGlobalClockConnection()).thenReturn(false);
    return netlist;
  }

  private static MappableResourcesContainer createScanningResources() {
    final var segment = mock(FpgaIoInformationContainer.class);
    when(segment.getType()).thenReturn(IoComponentTypes.SevenSegmentScanning);
    when(segment.hasMap()).thenReturn(true);
    when(segment.getArrayDriveMode()).thenReturn(SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_LOW);
    when(segment.getNrOfRows()).thenReturn(4);
    when(segment.getNrOfColumns()).thenReturn(-1);
    when(segment.getArrayId()).thenReturn(0);
    when(segment.getExternalPinCount()).thenReturn(12);
    final var pins = List.of("T5", "R5", "T9", "R6", "R7", "T7", "T8", "T10", "P9", "N9", "R8", "P8");
    for (var index = 0; index < pins.size(); index++) {
      when(segment.getPinLocation(index)).thenReturn(pins.get(index));
    }
    when(segment.getIoStandard()).thenReturn(IoStandards.LVCMOS33);
    when(segment.getPullBehavior()).thenReturn(PullBehaviors.PULL_UP);
    when(segment.getDrive()).thenReturn(DriveStrength.DRIVE_4);
    when(segment.getActivityLevel()).thenReturn(PinActivity.ACTIVE_LOW);

    final var ioComponents = mock(IoComponentsInformation.class);
    when(ioComponents.getComponents()).thenReturn(List.of(segment));

    final var resources = mock(MappableResourcesContainer.class);
    when(resources.getMappableResources()).thenReturn(Collections.emptyMap());
    when(resources.getIoComponentInformation()).thenReturn(ioComponents);
    return resources;
  }

  @SuppressWarnings("unchecked")
  private static List<String> getPinLocStrings(Object downloader)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    final Method method = downloader.getClass().getDeclaredMethod("getPinLocStrings");
    method.setAccessible(true);
    return (List<String>) method.invoke(downloader);
  }
}
