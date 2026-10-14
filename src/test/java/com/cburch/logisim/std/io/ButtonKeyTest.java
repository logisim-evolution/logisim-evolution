/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.proj.Project;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class ButtonKeyTest {
  @Test
  void activeButtonGoesHighWhileKeyIsHeldAndLowOnRelease() {
    final var fixture = new Fixture();
    final var button = fixture.add("A", 0, false);
    final var held = new HashSet<Component>();

    assertTrue(fixture.press(KeyEvent.VK_A, 'a', held));
    assertSame(Value.TRUE, fixture.level(button));
    assertTrue(fixture.press(KeyEvent.VK_A, 'a', held));
    assertSame(Value.TRUE, fixture.level(button));

    assertTrue(fixture.release(KeyEvent.VK_A, 'a', held));
    assertSame(Value.FALSE, fixture.level(button));
    assertTrue(held.isEmpty());
  }

  @Test
  void passiveButtonGoesLowWhileKeyIsHeld() {
    final var fixture = new Fixture();
    final var button = fixture.add("A", 0, true);

    assertSame(Value.TRUE, fixture.level(button));
    assertTrue(fixture.press(KeyEvent.VK_A, 'A', new HashSet<>()));
    assertSame(Value.FALSE, fixture.level(button));
  }

  @Test
  void twoKeysStayPressedIndependently() {
    final var fixture = new Fixture();
    final var first = fixture.add("A", 0, false);
    final var second = fixture.add("B", 40, false);
    final var held = new HashSet<Component>();

    assertTrue(fixture.press(KeyEvent.VK_A, 'a', held));
    assertTrue(fixture.press(KeyEvent.VK_B, 'b', held));
    assertSame(Value.TRUE, fixture.level(first));
    assertSame(Value.TRUE, fixture.level(second));

    assertTrue(fixture.release(KeyEvent.VK_A, 'a', held));
    assertSame(Value.FALSE, fixture.level(first));
    assertSame(Value.TRUE, fixture.level(second));
  }

  @Test
  void numericKeypadUsesTheSameDigit() {
    final var fixture = new Fixture();
    final var button = fixture.add("1", 0, false);

    assertTrue(fixture.press(KeyEvent.VK_NUMPAD1, '1', new HashSet<>()));
    assertSame(Value.TRUE, fixture.level(button));
  }

  @Test
  void controlChordIsNotConsumed() {
    final var fixture = new Fixture();
    final var button = fixture.add("A", 0, false);
    final var event =
        keyEvent(KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_A, 'a');

    assertFalse(
        ButtonKeyDispatcher.dispatch(
            fixture.circuit, fixture.state, event, true, new HashSet<>()));
    assertFalse(event.isConsumed());
    assertSame(Value.FALSE, fixture.level(button));
  }

  @Test
  void unmappedKeyIsNotConsumed() {
    final var fixture = new Fixture();
    fixture.add("A", 0, false);
    final var event = keyEvent(KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_Z, 'z');

    assertFalse(
        ButtonKeyDispatcher.dispatch(
            fixture.circuit, fixture.state, event, true, new HashSet<>()));
    assertFalse(event.isConsumed());
  }

  @Test
  void typedCharacterOfMappedKeyIsConsumed() {
    final var fixture = new Fixture();
    fixture.add("A", 0, false);
    final var event = keyEvent(KeyEvent.KEY_TYPED, 0, KeyEvent.VK_UNDEFINED, 'a');

    assertTrue(ButtonKeyDispatcher.consumeTyped(fixture.circuit, event));
    assertTrue(event.isConsumed());
  }

  @Test
  void addedButtonLosesKeyThatAnotherButtonAlreadyUses() {
    final var fixture = new Fixture();
    final var first = fixture.add("A", 0, false);
    final var second = fixture.add("A", 40, false);

    assertSame(Button.keyOption("A"), first.getAttributeSet().getValue(Button.ATTR_KEY));
    assertSame(Button.KEY_NONE, second.getAttributeSet().getValue(Button.ATTR_KEY));
  }

  @Test
  void sameKeyIsAllowedInAnotherCircuit() {
    final var fixture = new Fixture();
    final var mainButton = fixture.add("A", 0, false);
    final var other = new Circuit("other", fixture.file, fixture.project);
    fixture.file.addCircuit(other);
    final var otherButton = fixture.button("A", 0, false);
    add(other, otherButton);

    assertSame(Button.keyOption("A"), mainButton.getAttributeSet().getValue(Button.ATTR_KEY));
    assertSame(Button.keyOption("A"), otherButton.getAttributeSet().getValue(Button.ATTR_KEY));
  }

  @Test
  void keyReleaseDoesNotLiftAButtonHeldOnlyByTheMouse() {
    final var fixture = new Fixture();
    final var button = fixture.add("A", 0, false);
    Button.setPressed(fixture.state.getInstanceState(button), true);
    final var event = keyEvent(KeyEvent.KEY_RELEASED, 0, KeyEvent.VK_A, 'a');

    assertTrue(
        ButtonKeyDispatcher.dispatch(
            fixture.circuit, fixture.state, event, false, new HashSet<>()));
    assertSame(Value.TRUE, fixture.level(button));
  }

  @Test
  void releasingKeyboardHoldsLeavesAMousePressedButtonDown() {
    final var fixture = new Fixture();
    final var keyed = fixture.add("A", 0, false);
    final var mouseOnly = fixture.add(null, 40, false);
    final var held = new HashSet<Component>();

    assertTrue(fixture.press(KeyEvent.VK_A, 'a', held));
    Button.setPressed(fixture.state.getInstanceState(mouseOnly), true);
    ButtonKeyDispatcher.releaseHeld(fixture.state, held);

    assertSame(Value.FALSE, fixture.level(keyed));
    assertSame(Value.TRUE, fixture.level(mouseOnly));
    assertTrue(held.isEmpty());
  }

  @Test
  void duplicateMappingPressesNeitherButton() {
    final var fixture = new Fixture();
    final var first = fixture.add("A", 0, false);
    final var second = fixture.add("B", 40, false);
    second.getAttributeSet().setValue(Button.ATTR_KEY, Button.keyOption("A"));
    final var event = keyEvent(KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_A, 'a');

    assertFalse(
        ButtonKeyDispatcher.dispatch(
            fixture.circuit, fixture.state, event, true, new HashSet<>()));
    assertSame(Value.FALSE, fixture.level(first));
    assertSame(Value.FALSE, fixture.level(second));
  }

  private static void add(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
  }

  private static KeyEvent keyEvent(int id, int modifiers, int keyCode, char keyChar) {
    return new KeyEvent(new JPanel(), id, System.currentTimeMillis(), modifiers, keyCode, keyChar);
  }

  private static final class Fixture {
    private final LogisimFile file;
    private final Project project;
    private final Circuit circuit;
    private final CircuitState state;
    private final Button factory = new Button();

    private Fixture() {
      file = LogisimFile.createNew(new Loader(null), null);
      project = new Project(file);
      circuit = file.getMainCircuit();
      circuit.setProject(project);
      project.setCurrentCircuit(circuit);
      state = project.getCircuitState();
    }

    private Component add(String key, int x, boolean passive) {
      final var component = button(key, x, passive);
      ButtonKeyTest.add(circuit, component);
      return component;
    }

    private Component button(String key, int x, boolean passive) {
      final var attrs = factory.createAttributeSet();
      if (passive) attrs.setValue(Button.ATTR_PRESS, Button.BUTTON_PRESS_PASSIVE);
      if (key != null) attrs.setValue(Button.ATTR_KEY, Button.keyOption(key));
      return factory.createComponent(Location.create(x, 0, true), attrs);
    }

    private boolean press(int keyCode, char keyChar, Set<Component> held) {
      final var event = keyEvent(KeyEvent.KEY_PRESSED, 0, keyCode, keyChar);
      return ButtonKeyDispatcher.dispatch(circuit, state, event, true, held);
    }

    private boolean release(int keyCode, char keyChar, Set<Component> held) {
      final var event = keyEvent(KeyEvent.KEY_RELEASED, 0, keyCode, keyChar);
      return ButtonKeyDispatcher.dispatch(circuit, state, event, false, held);
    }

    private Value level(Component button) {
      final var data = (InstanceDataSingleton) state.getData(button);
      if (data == null) return Button.level(state.getInstanceState(button), false);
      return (Value) data.getValue();
    }
  }
}
