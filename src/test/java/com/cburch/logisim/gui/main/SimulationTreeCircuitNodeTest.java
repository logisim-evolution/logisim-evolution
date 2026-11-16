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
import static org.junit.jupiter.api.Assertions.assertSame;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import org.junit.jupiter.api.Test;

class SimulationTreeCircuitNodeTest {

  /** {@code main} holds two {@code mid} instances, each {@code mid} holds three {@code leaf}s. */
  private static final class Fixture {
    private final Circuit main;
    private final Circuit leaf;
    private final List<Component> mids = new ArrayList<>();
    private final CircuitState rootState;
    private final SimulationTreeModel model;

    private Fixture() {
      final var file = LogisimFile.createNew(new Loader(null), null);
      final var project = new Project(file);
      main = file.getMainCircuit();
      main.setProject(project);
      leaf = new Circuit("leaf", file, project);
      file.addCircuit(leaf);
      final var mid = new Circuit("mid", file, project);
      file.addCircuit(mid);
      for (var i = 0; i < 3; i++) add(mid, instance(leaf, 100 + 100 * i));
      for (var i = 0; i < 2; i++) mids.add(add(main, instance(mid, 100 + 100 * i)));
      project.setCurrentCircuit(main);
      rootState = project.getCircuitState();
      model = new SimulationTreeModel(List.of(rootState));
    }

    /** Renames {@code leaf} and returns how many tree nodes reported the change. */
    private int leafNodesNotified(String newName) {
      final var notified = new int[1];
      final TreeModelListener listener =
          new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
              if (e.getTreePath().getLastPathComponent() instanceof SimulationTreeCircuitNode node
                  && node.getCircuitState().getCircuit() == leaf) {
                notified[0]++;
              }
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {}

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {}

            @Override
            public void treeStructureChanged(TreeModelEvent e) {}
          };
      model.addTreeModelListener(listener);
      leaf.getStaticAttributes().setValue(CircuitAttributes.NAME_ATTR, newName);
      model.removeTreeModelListener(listener);
      return notified[0];
    }

    private SimulationTreeCircuitNode rootNode() {
      return (SimulationTreeCircuitNode) ((SimulationTreeNode) model.getRoot()).getChildAt(0);
    }
  }

  @Test
  void treeHasOneNodePerSubcircuitInstance() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final var fixture = new Fixture();
          assertEquals(6, fixture.leafNodesNotified("leaf2"));
        });
  }

  @Test
  void nodesOfRemovedSubcircuitStopListening() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final var fixture = new Fixture();
          remove(fixture.main, fixture.mids.get(0));
          assertEquals(1, fixture.rootNode().getChildCount());
          assertEquals(3, fixture.leafNodesNotified("leaf2"));
        });
  }

  @Test
  void movedSubcircuitKeepsItsStateAndNodes() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final var fixture = new Fixture();
          final var old = fixture.mids.get(1);
          final var moved =
              old.getFactory().createComponent(Location.create(400, 400, true), old.getAttributeSet());
          final var mutation = new CircuitMutation(fixture.main);
          mutation.replace(old, moved);
          mutation.execute();
          final var root = fixture.rootNode();
          assertEquals(2, root.getChildCount());
          var found = false;
          for (var i = 0; i < root.getChildCount(); i++) {
            final var child = (SimulationTreeCircuitNode) root.getChildAt(i);
            if (child.getCircuitState() == fixture.rootState.getData(moved)) found = true;
            assertSame(fixture.rootState, child.getCircuitState().getParentState());
          }
          assertEquals(true, found);
          assertEquals(6, fixture.leafNodesNotified("leaf2"));
        });
  }

  private static Component instance(Circuit circuit, int x) {
    final var factory = circuit.getSubcircuitFactory();
    return factory.createComponent(Location.create(x, 100, true), factory.createAttributeSet());
  }

  private static Component add(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
    return component;
  }

  private static void remove(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.remove(component);
    mutation.execute();
  }
}
