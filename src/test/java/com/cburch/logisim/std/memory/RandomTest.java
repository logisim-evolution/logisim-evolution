/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RandomTest {

  private static final long FIRST_VALUE = 0xCFC5D07F6F03C29BL;
  private static final long SECOND_VALUE = 0xBF424132963FE08DL;

  @Test
  void explicitSeedProducesFullWidthValuesAtAllSupportedWidths() {
    for (final var width : new int[] {1, 8, 32, 64}) {
      final var test = new TestRandom(width, 1);

      test.propagate();
      assertEquals(Value.createKnown(BitWidth.create(width), FIRST_VALUE), test.output());

      test.setClock(Value.TRUE);
      test.propagate();
      assertEquals(Value.createKnown(BitWidth.create(width), SECOND_VALUE), test.output());
    }
  }

  @Test
  void lowEnableBlocksClockAndDisconnectedEnableAllowsIt() {
    final var test = new TestRandom(64, 1);
    test.setEnable(Value.FALSE);
    test.propagate();

    test.setClock(Value.TRUE);
    test.propagate();
    assertEquals(Value.createKnown(64, FIRST_VALUE), test.output());

    test.setClock(Value.FALSE);
    test.setEnable(Value.UNKNOWN);
    test.propagate();
    test.setClock(Value.TRUE);
    test.propagate();
    assertEquals(Value.createKnown(64, SECOND_VALUE), test.output());
  }

  @Test
  void resetRestoresSequenceAndBlocksClockWhileHigh() {
    final var test = new TestRandom(64, 1);
    test.propagate();
    test.setClock(Value.TRUE);
    test.propagate();
    assertEquals(Value.createKnown(64, SECOND_VALUE), test.output());

    test.setReset(Value.TRUE);
    test.propagate();
    assertEquals(Value.createKnown(64, FIRST_VALUE), test.output());

    test.setClock(Value.FALSE);
    test.propagate();
    test.setClock(Value.TRUE);
    test.propagate();
    assertEquals(Value.createKnown(64, FIRST_VALUE), test.output());

    test.setReset(Value.FALSE);
    test.setClock(Value.FALSE);
    test.propagate();
    test.setClock(Value.TRUE);
    test.propagate();
    assertEquals(Value.createKnown(64, SECOND_VALUE), test.output());
  }

  @Test
  void seedAndWidthRoundTripThroughProjectFile() throws Exception {
    final var loader = new RecordingLoader();
    final var file = LogisimFile.createNew(loader, null);
    final var memoryLibrary = loader.getBuiltin().getLibrary(MemoryLibrary._ID);
    file.addLibrary(memoryLibrary);
    final var random =
        assertInstanceOf(
            Random.class,
            assertInstanceOf(AddTool.class, memoryLibrary.getTool(Random._ID)).getFactory());
    final var attrs = random.createAttributeSet();
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(64));
    attrs.setValue(Random.ATTR_SEED, 1);

    final var mutation = new CircuitMutation(file.getMainCircuit());
    mutation.add(random.createComponent(Location.create(100, 100, true), attrs));
    mutation.execute();

    final var output = new ByteArrayOutputStream();
    file.write(output, loader);
    loader.assertNoErrors();
    final var reloadLoader = new RecordingLoader();
    final var loaded =
        LogisimFile.load(new ByteArrayInputStream(output.toByteArray()), reloadLoader);
    reloadLoader.assertNoErrors();
    final var loadedComponents = loaded.getMainCircuit().getNonWires();

    assertEquals(1, loadedComponents.size());
    final var loadedRandom = loadedComponents.iterator().next();
    assertInstanceOf(Random.class, loadedRandom.getFactory());
    assertEquals(BitWidth.create(64), loadedRandom.getAttributeSet().getValue(StdAttr.WIDTH));
    assertEquals(1, loadedRandom.getAttributeSet().getValue(Random.ATTR_SEED));
  }

  private static final class RecordingLoader extends Loader {
    private final StringBuilder errors = new StringBuilder();

    private RecordingLoader() {
      super(null);
    }

    @Override
    public void showError(String description) {
      if (!errors.isEmpty()) errors.append('\n');
      errors.append(description);
    }

    private void assertNoErrors() {
      assertEquals("", errors.toString());
    }
  }

  private static final class TestRandom {
    private final Random random = new Random();
    private final TestInstanceState state;

    private TestRandom(int width, int seed) {
      final var attrs = random.createAttributeSet();
      attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
      attrs.setValue(Random.ATTR_SEED, seed);
      state = new TestInstanceState(random, attrs);
      state.setPortValue(Random.CK, Value.FALSE);
      state.setPortValue(Random.NXT, Value.TRUE);
      state.setPortValue(Random.RST, Value.FALSE);
    }

    private void propagate() {
      random.propagate(state);
    }

    private Value output() {
      return state.getPortValue(Random.OUT);
    }

    private void setClock(Value value) {
      state.setPortValue(Random.CK, value);
    }

    private void setEnable(Value value) {
      state.setPortValue(Random.NXT, value);
    }

    private void setReset(Value value) {
      state.setPortValue(Random.RST, value);
    }
  }

  private static final class TestInstanceState implements InstanceState {
    private final AttributeSet attrs;
    private final Instance instance;
    private final Map<Integer, Value> portValues = new HashMap<>();
    private InstanceData data;

    private TestInstanceState(InstanceFactory factory, AttributeSet attrs) {
      this.attrs = attrs;
      final var component = factory.createComponent(Location.create(0, 0, false), attrs);
      instance = Instance.getInstanceFor(component);
    }

    @Override
    public void fireInvalidated() {}

    @Override
    public AttributeSet getAttributeSet() {
      return attrs;
    }

    @Override
    public <E> E getAttributeValue(Attribute<E> attr) {
      return attrs.getValue(attr);
    }

    @Override
    public InstanceData getData() {
      return data;
    }

    @Override
    public InstanceFactory getFactory() {
      return instance.getFactory();
    }

    @Override
    public Instance getInstance() {
      return instance;
    }

    @Override
    public int getPortIndex(Port port) {
      return instance.getPorts().indexOf(port);
    }

    @Override
    public Value getPortValue(int portIndex) {
      return portValues.getOrDefault(portIndex, Value.UNKNOWN);
    }

    @Override
    public Project getProject() {
      return null;
    }

    @Override
    public int getTickCount() {
      return 0;
    }

    @Override
    public boolean isCircuitRoot() {
      return true;
    }

    @Override
    public boolean isPortConnected(int portIndex) {
      return portValues.containsKey(portIndex);
    }

    @Override
    public CircuitState createCircuitSubstateFor(Circuit circ) {
      return null;
    }

    @Override
    public void setData(InstanceData value) {
      data = value;
    }

    @Override
    public void setPort(int portIndex, Value value, int delay) {
      portValues.put(portIndex, value);
    }

    private void setPortValue(int portIndex, Value value) {
      portValues.put(portIndex, value);
    }
  }
}
