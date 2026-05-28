/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Graphics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@ExtendWith(MockitoExtension.class)
class DynamicElementTest {

  @Test
  void getInstanceStateUsesCurrentStateForSingleElementPath() {
    final var state = mock(CircuitState.class);
    final var leaf = mock(InstanceComponent.class);
    final var expected = mock(InstanceState.class);
    final var element = new TestDynamicElement(new DynamicElement.Path(new InstanceComponent[] {leaf}));

    when(state.getInstanceState(leaf)).thenReturn(expected);

    assertSame(expected, element.resolveInstanceState(state));
  }

  @Test
  void getInstanceStateTraversesNestedCircuitStatePath() {
    final var outerState = mock(CircuitState.class);
    final var innerState = mock(CircuitState.class);
    final var subcircuit = mock(InstanceComponent.class);
    final var leaf = mock(InstanceComponent.class);
    final var expected = mock(InstanceState.class);
    final var element =
        new TestDynamicElement(new DynamicElement.Path(new InstanceComponent[] {subcircuit, leaf}));

    when(outerState.getData(subcircuit)).thenReturn(innerState);
    when(innerState.getInstanceState(leaf)).thenReturn(expected);

    assertSame(expected, element.resolveInstanceState(outerState));
  }

  @Test
  void getInstanceStateReturnsNullWhenNestedStateIsUnavailable() {
    final var outerState = mock(CircuitState.class);
    final var subcircuit = mock(InstanceComponent.class);
    final var leaf = mock(InstanceComponent.class);
    final var element =
        new TestDynamicElement(new DynamicElement.Path(new InstanceComponent[] {subcircuit, leaf}));

    when(outerState.getData(subcircuit)).thenReturn(null);

    assertNull(element.resolveInstanceState(outerState));
  }

  @Test
  void getInstanceStateRejectsUnexpectedIntermediateData() {
    final var outerState = mock(CircuitState.class);
    final var subcircuit = mock(InstanceComponent.class);
    final var leaf = mock(InstanceComponent.class);
    final var element =
        new TestDynamicElement(new DynamicElement.Path(new InstanceComponent[] {subcircuit, leaf}));

    when(outerState.getData(subcircuit)).thenReturn(new Object());

    assertThrows(IllegalStateException.class, () -> element.resolveInstanceState(outerState));
  }

  private static class TestDynamicElement extends DynamicElement {
    TestDynamicElement(DynamicElement.Path path) {
      super(path, Bounds.create(0, 0, 1, 1));
    }

    InstanceState resolveInstanceState(CircuitState state) {
      return getInstanceState(state);
    }

    @Override
    public String getDisplayName() {
      return "test dynamic element";
    }

    @Override
    public void paintDynamic(Graphics g, CircuitState state) {
      // Not used in these tests.
    }

    @Override
    public Element toSvgElement(Document document) {
      return document.createElement("test-dynamic-element");
    }
  }
}
