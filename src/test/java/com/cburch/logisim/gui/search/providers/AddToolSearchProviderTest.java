/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search.providers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.search.SearchCandidate;
import com.cburch.logisim.gui.search.SearchContext;
import com.cburch.logisim.gui.search.SearchProviders;
import com.cburch.logisim.gui.search.SearchQuery;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Probe;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class AddToolSearchProviderTest {

  @Test
  void isAvailableOnlyForProjectWindowsAndIsRegistered() {
    final var provider = new AddToolSearchProvider();

    assertFalse(provider.isAvailable(new SearchContext(null, null, null)));
    assertTrue(provider.isAvailable(contextFor(mock(Project.class))));
    assertTrue(
        SearchProviders.getAll().stream().anyMatch(AddToolSearchProvider.class::isInstance));
  }

  @Test
  void indexesVisibleNestedAddToolsWithTheirLibraryPathsAndIcons() {
    final var rootTool = new AddTool(Pin.FACTORY);
    final var leafTool = new AddTool(Probe.FACTORY);
    final var ordinaryTool = mock(Tool.class);
    final var leafLibrary = new TestLibrary("Leaf", List.of(leafTool, ordinaryTool), List.of());
    final var parentLibrary = new TestLibrary("Parent", List.of(), List.of(leafLibrary));
    final var hiddenTool = new AddTool(Pin.FACTORY);
    final var hiddenLibrary = new TestLibrary("Hidden", List.of(hiddenTool), List.of());
    hiddenLibrary.setHidden();

    final var file = mock(LogisimFile.class);
    when(file.getDisplayName()).thenReturn("Demo");
    when(file.getTools()).thenReturn(List.of(rootTool));
    when(file.getLibraries()).thenReturn(List.of(parentLibrary, hiddenLibrary));
    final var project = mock(Project.class);
    when(project.getLogisimFile()).thenReturn(file);

    final var candidates = candidates(new AddToolSearchProvider(), project);

    assertEquals(2, candidates.size());
    assertCandidate(candidates.get(0), rootTool, "Add › Demo");
    assertCandidate(candidates.get(1), leafTool, "Add › Demo › Parent › Leaf");
  }

  @Test
  void activatingResultSelectsExistingToolThroughProject() {
    final var tool = new AddTool(Pin.FACTORY);
    final var file = mock(LogisimFile.class);
    when(file.getDisplayName()).thenReturn("Demo");
    when(file.getTools()).thenReturn(List.of(tool));
    when(file.getLibraries()).thenReturn(List.of());
    final var project = mock(Project.class);
    when(project.getLogisimFile()).thenReturn(file);

    final var candidate = candidates(new AddToolSearchProvider(), project).get(0);
    candidate.action().run();

    verify(project).setTool(tool);
  }

  @Test
  void excludesCurrentCircuitBecauseItCannotContainItself() {
    final var currentCircuit = mock(Circuit.class);
    final var currentFactory = mock(SubcircuitFactory.class);
    when(currentFactory.getSubcircuit()).thenReturn(currentCircuit);
    final var currentTool = mock(AddTool.class);
    when(currentTool.getFactory(false)).thenReturn(currentFactory);
    final var otherTool = new AddTool(Pin.FACTORY);
    final var file = mock(LogisimFile.class);
    when(file.getDisplayName()).thenReturn("Demo");
    when(file.getTools()).thenReturn(List.of(currentTool, otherTool));
    when(file.getLibraries()).thenReturn(List.of());
    final var project = mock(Project.class);
    when(project.getLogisimFile()).thenReturn(file);
    when(project.getCurrentCircuit()).thenReturn(currentCircuit);

    final var candidates = candidates(new AddToolSearchProvider(), project);

    assertEquals(1, candidates.size());
    assertEquals(otherTool.getDisplayName(), candidates.get(0).title());
  }

  @Test
  void rebuildsSnapshotEachTimeDialogPreparesProvider() {
    final var first = new AddTool(Pin.FACTORY);
    final var second = new AddTool(Probe.FACTORY);
    final var currentTools = new AtomicReference<>(List.of(first));
    final var file = mock(LogisimFile.class);
    when(file.getDisplayName()).thenReturn("Demo");
    when(file.getTools()).thenAnswer(invocation -> currentTools.get());
    when(file.getLibraries()).thenReturn(List.of());
    final var project = mock(Project.class);
    when(project.getLogisimFile()).thenReturn(file);
    final var provider = new AddToolSearchProvider();

    candidates(provider, project).get(0).action().run();
    verify(project).setTool(first);

    currentTools.set(List.of(second));
    candidates(provider, project).get(0).action().run();
    verify(project).setTool(second);
  }

  private static List<SearchCandidate> candidates(
      AddToolSearchProvider provider, Project project) {
    provider.prepare(contextFor(project));
    return provider.search(new SearchQuery("")).stream().map(result -> result.candidate()).toList();
  }

  private static SearchContext contextFor(Project project) {
    return new SearchContext(null, null, project);
  }

  private static void assertCandidate(SearchCandidate candidate, AddTool tool, String context) {
    assertEquals(tool.getDisplayName(), candidate.title());
    assertEquals(context, candidate.context());
    assertNotNull(candidate.icon());
    assertTrue(candidate.enabled());
    final var image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
    final var graphics = image.createGraphics();
    try {
      assertDoesNotThrow(() -> candidate.icon().paintIcon(new JPanel(), graphics, 0, 0));
    } finally {
      graphics.dispose();
    }
  }

  private static class TestLibrary extends Library {
    private final String name;
    private final List<? extends Tool> tools;
    private final List<Library> libraries;

    TestLibrary(String name, List<? extends Tool> tools, List<Library> libraries) {
      this.name = name;
      this.tools = tools;
      this.libraries = libraries;
    }

    @Override
    public String getDisplayName() {
      return name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public List<Library> getLibraries() {
      return libraries;
    }

    @Override
    public List<? extends Tool> getTools() {
      return tools;
    }
  }
}
