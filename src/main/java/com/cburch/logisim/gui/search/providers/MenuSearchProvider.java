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

import com.cburch.logisim.gui.search.IndexedSearchProvider;
import com.cburch.logisim.gui.search.SearchCandidate;
import com.cburch.logisim.gui.search.SearchContext;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * Offers the menu items of the window the search was opened from.
 * <p>
 * The index is built by walking the live menu bar. Submenus that populate lazily on
 * are indexed with whatever they hold when the dialog opens. In practice that only
 * affects the Edit menu's undo and redo history, whose entries are per-project
 * history steps rather than actions worth searching for.
 */
public class MenuSearchProvider extends IndexedSearchProvider {

  /**
   * Client property marking a menu item that must stay out of the index. Set it
   * to {@link Boolean#TRUE} on any item that would be noise in the results - the
   * entry point into the search dialog being the obvious example.
   */
  public static final String EXCLUDE_PROPERTY = "logisim.search.exclude";

  /**
   * Guards against pathological nesting; real menus never come close.
   */
  private static final int MAX_DEPTH = 8;

  @Override
  public String getDisplayName() {
    return S.get("searchProviderMenus");
  }

  @Override
  public boolean isAvailable(SearchContext context) {
    return context.menuBar() != null;
  }

  @Override
  protected List<SearchCandidate> buildCandidates(SearchContext context) {
    final var candidates = new ArrayList<SearchCandidate>();
    final var menuBar = context.menuBar();
    if (menuBar == null) return candidates;
    for (var i = 0; i < menuBar.getMenuCount(); i++) {
      final var menu = menuBar.getMenu(i);
      // getMenu() returns null for anything in the bar that is not a JMenu, e.g. a separator.
      if (menu != null) collectFrom(menu, "", candidates, 0);
    }
    return candidates;
  }

  private void collectFrom(
      JMenu menu, String parentPath, List<SearchCandidate> candidates, int depth) {
    if (depth > MAX_DEPTH || isExcluded(menu)) return;
    final var menuText = textOf(menu);
    final var path =
        parentPath.isEmpty()
            ? menuText
            : parentPath + SearchCandidate.CONTEXT_SEPARATOR + menuText;

    for (final var child : menu.getMenuComponents()) {
      // JMenu extends JMenuItem, so submenus must be tested for first. Separators are plain
      // JSeparators and match neither branch.
      if (child instanceof JMenu subMenu) {
        collectFrom(subMenu, path, candidates, depth + 1);
      } else if (child instanceof JMenuItem item) {
        final var text = textOf(item);
        if (!isExcluded(item) && !text.isEmpty()) {
          candidates.add(toCandidate(item, text, path));
        }
      }
    }
  }

  private static SearchCandidate toCandidate(JMenuItem item, String text, String path) {
    return new SearchCandidate(
        text,
        path,
        item.getIcon(),
        acceleratorText(item.getAccelerator()),
        item.isEnabled(),
        item::doClick);
  }

  private static boolean isExcluded(JMenuItem item) {
    return Boolean.TRUE.equals(item.getClientProperty(EXCLUDE_PROPERTY));
  }

  private static String textOf(JMenuItem item) {
    final var text = item.getText();
    return text == null ? "" : text.trim();
  }

  /**
   * Formats an accelerator the same way the hotkey preferences page does.
   */
  private static String acceleratorText(KeyStroke stroke) {
    if (stroke == null) return "";
    final var modifiers = InputEvent.getModifiersExText(stroke.getModifiers());
    final var key =
        stroke.getKeyCode() == KeyEvent.VK_UNDEFINED
            ? String.valueOf(stroke.getKeyChar())
            : KeyEvent.getKeyText(stroke.getKeyCode());
    return modifiers.isEmpty() ? key : modifiers + "+" + key;
  }
}
