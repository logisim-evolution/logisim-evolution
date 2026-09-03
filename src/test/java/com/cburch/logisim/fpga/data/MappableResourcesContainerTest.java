/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.file.BoardWriterClass;
import com.cburch.logisim.util.XmlUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class MappableResourcesContainerTest {

  @Test
  void boardMappingsAreReboundWhenIoComponentsAreRefreshed() throws Exception {
    final var board = new BoardInformation();
    board.setBoardName("test-board");
    board.addComponent(createOutputComponent(2));

    final var key = new ArrayList<>(List.of("test-board", "output"));
    final var container = createContainer(board, key, 2);
    final var oldIo = container.getIoComponentInformation().getComponents().getFirst();
    final var oldMap = container.getMappableResources().get(key);
    assertTrue(oldMap.tryMap(1, oldIo, 1));

    container.destroyIOComponentInformation();
    container.updateIoComponents(board);
    container.updateMappableComponents();

    final var newIo = container.getIoComponentInformation().getComponents().getFirst();
    final var newMap = container.getMappableResources().get(key);

    assertNotSame(oldIo, newIo);
    assertSame(newIo, newMap.getFpgaInfo(1));
    assertEquals("PIN_1", newMap.getPinLocation(1));
    assertTrue(newIo.isPinMapped(1));
    assertSame(newMap, newIo.getPinMap(1));
    assertEquals(1, newIo.getMapPin(1));
  }

  @Test
  void completeMappingsKeepTheirSharedBoardPinWhenIoComponentsAreRefreshed()
      throws Exception {
    final var board = new BoardInformation();
    board.setBoardName("test-board");
    board.addComponent(createOutputComponent(4));

    final var key = new ArrayList<>(List.of("test-board", "rgb-output"));
    final var container = createContainer(board, key, 3);
    final var oldIo = container.getIoComponentInformation().getComponents().getFirst();
    final var oldMap = container.getMappableResources().get(key);
    assertTrue(oldMap.tryCompleteMap(oldIo, 2));

    container.destroyIOComponentInformation();
    container.updateIoComponents(board);
    container.updateMappableComponents();

    final var newIo = container.getIoComponentInformation().getComponents().getFirst();
    final var newMap = container.getMappableResources().get(key);

    for (var pin = 0; pin < 3; pin++) {
      assertSame(newIo, newMap.getFpgaInfo(pin));
      assertEquals("PIN_2", newMap.getPinLocation(pin));
    }
    assertTrue(newMap.isCompleteMap(false));
    assertTrue(newIo.isPinMapped(2));
    assertSame(newMap, newIo.getPinMap(2));
    assertEquals(0, newIo.getMapPin(2));
  }

  private static MappableResourcesContainer createContainer(
      BoardInformation board, ArrayList<String> key, int outputCount) {
    final var resources = new HashMap<ArrayList<String>, netlistComponent>();
    resources.put(key, createNetlistComponent(outputCount));

    final var circuit = mock(Circuit.class);
    final var netlist = mock(Netlist.class);
    when(circuit.getNetList()).thenReturn(netlist);
    when(netlist.getMappableResources(anyList(), eq(true))).thenReturn(resources);
    return new MappableResourcesContainer(board, circuit);
  }

  private static FpgaIoInformationContainer createOutputComponent(int pinCount) throws Exception {
    final var document =
        XmlUtil.getHardenedBuilderFactory().newDocumentBuilder().newDocument();
    final var element = document.createElement(IoComponentTypes.SevenSegment.toString());
    element.setAttribute(BoardWriterClass.RECT_SET_STRING, "10,20,30,40");
    final var locations = new ArrayList<String>();
    for (var pin = 0; pin < pinCount; pin++) {
      locations.add("PIN_" + pin);
    }
    element.setAttribute(BoardWriterClass.OUTPUT_SET_STRING, String.join(",", locations));
    element.setAttribute(BoardWriterClass.LABEL_STRING, "output");
    return new FpgaIoInformationContainer(element);
  }

  private static netlistComponent createNetlistComponent(int outputCount) {
    final var component = mock(Component.class);
    when(component.getFactory()).thenReturn(mock(ComponentFactory.class));
    when(component.getAttributeSet()).thenReturn(mock(AttributeSet.class));

    final var netlistComponent = mock(netlistComponent.class);
    when(netlistComponent.getComponent()).thenReturn(component);
    when(netlistComponent.getMapInformationContainer())
        .thenReturn(new ComponentMapInformationContainer(0, outputCount, 0));
    return netlistComponent;
  }
}
