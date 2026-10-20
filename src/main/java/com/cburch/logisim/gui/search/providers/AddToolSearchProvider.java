/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search.providers;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.gui.search.IndexedSearchProvider;
import com.cburch.logisim.gui.search.SearchCandidate;
import com.cburch.logisim.gui.search.SearchContext;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;

/** Offers the components in the current project's visible, open library hierarchy. */
public class AddToolSearchProvider extends IndexedSearchProvider {

  @Override
  public String getDisplayName() {
    return S.get("searchProviderAdd");
  }

  @Override
  public boolean isAvailable(SearchContext context) {
    return context.project() != null;
  }

  @Override
  protected List<SearchCandidate> buildCandidates(SearchContext context) {
    final var project = context.project();
    if (project == null || project.getLogisimFile() == null) return List.of();

    final var candidates = new ArrayList<SearchCandidate>();
    final Set<Library> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
    collectFrom(
        project.getLogisimFile(),
        getDisplayName(),
        project,
        candidates,
        ancestors,
        true);
    return candidates;
  }

  private static void collectFrom(
      Library library,
      String parentPath,
      Project project,
      List<SearchCandidate> candidates,
      Set<Library> ancestors,
      boolean projectRoot) {
    if ((!projectRoot && library.isHidden()) || !ancestors.add(library)) return;
    try {
      final var path = appendPath(parentPath, displayNameOf(library));
      for (final var tool : library.getTools()) {
        if (tool instanceof AddTool addTool && isPlaceable(addTool, project)) {
          candidates.add(
              new SearchCandidate(
                  addTool.getDisplayName(),
                  path,
                  new ToolIcon(addTool),
                  "",
                  true,
                  () -> project.setTool(addTool)));
        }
      }
      for (final var child : library.getLibraries()) {
        collectFrom(child, path, project, candidates, ancestors, false);
      }
    } finally {
      ancestors.remove(library);
    }
  }

  private static boolean isPlaceable(AddTool tool, Project project) {
    return !(tool.getFactory(false) instanceof SubcircuitFactory subcircuit
        && subcircuit.getSubcircuit() == project.getCurrentCircuit());
  }

  private static String displayNameOf(Library library) {
    final var displayName = library.getDisplayName();
    if (displayName != null && !displayName.isBlank()) return displayName.trim();
    final var name = library.getName();
    return name == null ? "" : name.trim();
  }

  private static String appendPath(String parent, String child) {
    if (child.isEmpty()) return parent;
    return parent.isEmpty() ? child : parent + SearchCandidate.CONTEXT_SEPARATOR + child;
  }

  /** Paints an AddTool using the same scaled component icon geometry as the toolbox. */
  private static class ToolIcon implements Icon {
    private final AddTool tool;

    ToolIcon(AddTool tool) {
      this.tool = tool;
    }

    @Override
    public int getIconHeight() {
      return AppPreferences.getScaled(AppPreferences.BOX_SIZE);
    }

    @Override
    public int getIconWidth() {
      return AppPreferences.getScaled(AppPreferences.BOX_SIZE);
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
      final var baseGraphics = graphics.create();
      baseGraphics.setColor(new Color(AppPreferences.COMPONENT_ICON_COLOR.get()));
      final var iconGraphics = baseGraphics.create();
      try {
        final var context =
            new ComponentDrawContext(component, null, null, baseGraphics, iconGraphics);
        final var border = AppPreferences.getScaled(AppPreferences.ICON_BORDER);
        tool.paintIcon(context, x + border, y + border);
      } finally {
        iconGraphics.dispose();
        baseGraphics.dispose();
      }
    }
  }
}
