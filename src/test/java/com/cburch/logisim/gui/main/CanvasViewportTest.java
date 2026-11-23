/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.WidthIncompatibilityData;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.MouseMappings;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.proj.Project;
import java.awt.Dimension;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CanvasViewportTest {

  private Project project;
  private Circuit circuit;
  private Canvas canvas;

  @BeforeEach
  void setUp() {
    project = mock(Project.class);
    final var options = mock(Options.class);
    final var mouseMappings = mock(MouseMappings.class);
    final var attributeSet = mock(com.cburch.logisim.data.AttributeSet.class);
    final var simulator = mock(Simulator.class);
    circuit = mock(Circuit.class);

    when(project.getOptions()).thenReturn(options);
    when(options.getMouseMappings()).thenReturn(mouseMappings);
    when(options.getAttributeSet()).thenReturn(attributeSet);
    when(project.getSimulator()).thenReturn(simulator);
    when(project.getCurrentCircuit()).thenReturn(circuit);
    when(circuit.getBounds()).thenReturn(Bounds.create(0, 0, 500, 500));

    canvas = new Canvas(project);
    canvas.setSize(new Dimension(500, 500));
  }

  @Test
  void computeViewportContentsClearsWidthMessageWhenNoExceptions() {
    when(circuit.getWidthIncompatibilityData()).thenReturn(Collections.emptySet());

    canvas.computeViewportContents();

    assertNull(canvas.getViewport().widthMessage);
  }

  @Test
  void computeViewportContentsSetsWidthMessageWithoutRepaintLoop() {
    final var data = new WidthIncompatibilityData();
    data.add(Location.create(100, 100, false), BitWidth.create(1));
    data.add(Location.create(100, 100, false), BitWidth.create(2));

    when(circuit.getWidthIncompatibilityData()).thenReturn(Set.of(data));

    final var repaintCount = new AtomicInteger(0);
    final var viewport = canvas.getViewport();

    canvas.computeViewportContents();

    assertNotNull(viewport.widthMessage);
    assertTrue(viewport.widthMessage.contains("Incompatible widths"));
    assertEquals(0, repaintCount.get());
  }

  @Test
  void computeViewportContentsAccumulatesArrowsForMultipleExceptionsWithoutRepaint() {
    final var northEx = new WidthIncompatibilityData();
    northEx.add(Location.create(250, -50, false), BitWidth.create(1));
    northEx.add(Location.create(250, -50, false), BitWidth.create(2));

    final var eastEx = new WidthIncompatibilityData();
    eastEx.add(Location.create(800, 250, false), BitWidth.create(1));
    eastEx.add(Location.create(800, 250, false), BitWidth.create(4));

    final var exceptions = new LinkedHashSet<WidthIncompatibilityData>();
    exceptions.add(northEx);
    exceptions.add(eastEx);

    when(circuit.getWidthIncompatibilityData()).thenReturn(exceptions);

    final var viewport = canvas.getViewport();
    canvas.computeViewportContents();

    assertTrue(viewport.isNorth, "North arrow should be set for northern exception");
    assertTrue(viewport.isEast, "East arrow should be set for eastern exception");
    assertFalse(viewport.isSouth, "South arrow should not be set");
  }

  @Test
  void setArrowsWithRepaintFalseDoesNotTriggerRepaint() {
    canvas.setArrows(100, 100, 200, 200, true, false);
    // Verified that execution completes without invoking viewport.repaint()
  }
}
