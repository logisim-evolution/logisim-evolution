/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.gui.generic.LFrame;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live "omni-search" over everything the registered {@link SearchProvider}s offer
 * <p>
 * The dialog is only a hub and it delegates all the work to SearchProviders
 */
public class OmniSearchDialog extends JDialog {

  private static final long serialVersionUID = 1L;

  static final Logger logger = LoggerFactory.getLogger(OmniSearchDialog.class);

  private static final int DIALOG_WIDTH = 680;
  private static final int DIALOG_HEIGHT = 440;

  /**
   * Pause after typing before asynchronous providers are consulted, in
   * milliseconds.
   */
  private static final int ASYNC_DEBOUNCE_MS = 250;

  /**
   * Asynchronous providers are left alone until the query is at least this long.
   */
  private static final int ASYNC_MIN_QUERY_LENGTH = 2;

  /**
   * Shared worker for asynchronous providers; daemon so it never holds the
   * application open.
   */
  private static final ExecutorService EXECUTOR =
      Executors.newCachedThreadPool(
          runnable -> {
            final var thread = new Thread(runnable, "omni-search");
            thread.setDaemon(true);
            return thread;
          });

  private final transient List<SearchProvider> providers;
  private final transient Map<SearchProvider, List<SearchResult>> resultsByProvider =
      new LinkedHashMap<>();

  private final JTextField searchField = new JTextField();
  private final DefaultListModel<SearchResult> resultModel = new DefaultListModel<>();
  private final JList<SearchResult> resultList = new JList<>(resultModel);
  private final JLabel statusLabel = new JLabel();
  private final Timer asyncDebounce;

  /**
   * Bumped on every keystroke, so results from a query the user has moved on from
   * are dropped.
   */
  private int generation;

  /**
   * Guards against a second dialog being stacked on the first, e.g. by a shortcut
   * that fires again before the first one is dismissed.
   */
  private static boolean showing;

  /**
   * Opens the omni-search for {@code context} and blocks until it is dismissed.
   *
   * @param context the window the search was opened from
   */
  public static void showDialog(SearchContext context) {
    if (showing) return;
    showing = true;
    try {
      final var dialog = new OmniSearchDialog(context);
      dialog.setVisible(true);
    } finally {
      showing = false;
    }
  }

  /**
   * Opens the omni-search for {@code window}, doing nothing when it is not a Logisim
   * frame carrying a menu bar - a preferences or file dialog has nothing to search.
   */
  public static void showForWindow(Window window) {
    if (!(window instanceof LFrame frame)) return;
    final var menuBar = frame.getLogisimMenuBar();
    if (menuBar == null) return;
    showDialog(new SearchContext(frame, menuBar, frame.getProject()));
  }

  private OmniSearchDialog(SearchContext context) {
    super(context.owner(), S.get("searchTitle"), Dialog.ModalityType.APPLICATION_MODAL);
    this.providers = SearchProviders.getAvailable(context);
    for (final var provider : providers) {
      try {
        provider.prepare(context);
      } catch (Exception e) {
        logger.warn("Search provider {} failed to prepare", provider.getDisplayName(), e);
      }
    }

    asyncDebounce = new Timer(ASYNC_DEBOUNCE_MS, event -> queryAsyncProviders());
    asyncDebounce.setRepeats(false);

    buildUi();
    refresh();
    setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
    setLocationRelativeTo(context.owner());
  }

  private void buildUi() {
    searchField.putClientProperty("JTextField.placeholderText", S.get("searchPlaceholder"));
    searchField.setBorder(
        BorderFactory.createCompoundBorder(
            searchField.getBorder(), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
    searchField
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent event) {
                refresh();
              }

              @Override
              public void removeUpdate(DocumentEvent event) {
                refresh();
              }

              @Override
              public void changedUpdate(DocumentEvent event) {
                refresh();
              }
            });
    searchField.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent event) {
            handleNavigationKey(event);
          }
        });

    resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    resultList.setCellRenderer(new ResultRenderer());
    // Focus stays in the search field; the list is driven from there.
    resultList.setFocusable(false);
    resultList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent event) {
            final var row = resultList.locationToIndex(event.getPoint());
            if (row < 0) return;
            resultList.setSelectedIndex(row);
            if (event.getClickCount() >= 2) activateSelection();
          }
        });

    statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
    statusLabel.setForeground(secondaryColor());
    statusLabel.setToolTipText(describeProviders());

    final var footer = new JLabel(S.get("searchFooter"));
    footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
    footer.setForeground(secondaryColor());

    final var bottom = new JPanel();
    bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
    bottom.add(statusLabel);
    bottom.add(footer);

    final var content = new JPanel(new BorderLayout(0, 6));
    content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    content.add(searchField, BorderLayout.NORTH);
    content.add(new JScrollPane(resultList), BorderLayout.CENTER);
    content.add(bottom, BorderLayout.SOUTH);
    setContentPane(content);

    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeOmniSearch");
    getRootPane()
        .getActionMap()
        .put(
            "closeOmniSearch",
            new AbstractAction() {
              private static final long serialVersionUID = 1L;

              @Override
              public void actionPerformed(ActionEvent event) {
                dispose();
              }
            });

    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowOpened(WindowEvent event) {
            searchField.requestFocusInWindow();
          }

          @Override
          public void windowClosed(WindowEvent event) {
            asyncDebounce.stop();
          }
        });
  }

  private SearchQuery currentQuery() {
    return new SearchQuery(searchField.getText().trim());
  }

  /**
   * Re-queries the synchronous providers immediately and schedules the
   * asynchronous ones.
   */
  private void refresh() {
    final var query = currentQuery();
    generation++;

    for (final var provider : providers) {
      if (provider.isAsynchronous()) {
        // Cleared now so stale remote results never linger under a newer query.
        resultsByProvider.remove(provider);
      } else {
        resultsByProvider.put(provider, safeSearch(provider, query));
      }
    }
    rebuildModel();

    asyncDebounce.stop();
    if (query.text().length() >= ASYNC_MIN_QUERY_LENGTH && hasAsyncProvider()) {
      asyncDebounce.start();
    }
  }

  private boolean hasAsyncProvider() {
    return providers.stream().anyMatch(SearchProvider::isAsynchronous);
  }

  /**
   * Fires the asynchronous providers for the current query, merging results as
   * they land.
   */
  private void queryAsyncProviders() {
    final var query = currentQuery();
    final var issued = generation;
    for (final var provider : providers) {
      if (!provider.isAsynchronous()) continue;
      EXECUTOR.execute(
          () -> {
            final var results = safeSearch(provider, query);
            SwingUtilities.invokeLater(
                () -> {
                  // The user has typed on since; these results are for a question no longer asked.
                  if (issued != generation) return;
                  resultsByProvider.put(provider, results);
                  rebuildModel();
                });
          });
    }
  }

  private List<SearchResult> safeSearch(SearchProvider provider, SearchQuery query) {
    try {
      final var results = provider.search(query);
      return results == null ? List.of() : results;
    } catch (Exception e) {
      // A misbehaving provider must not take the whole search down with it.
      logger.warn("Search provider {} failed", provider.getDisplayName(), e);
      return List.of();
    }
  }

  /**
   * Merges every provider's current results into one ranked list.
   */
  private void rebuildModel() {
    final var merged = new ArrayList<SearchResult>();
    // Keyed by identity: SearchResult carries an int[], for which record equality is reference
    // equality anyway, and two providers may well offer equal-looking results.
    final var priorities = new IdentityHashMap<SearchResult, Integer>();
    for (final var provider : providers) {
      final var results = resultsByProvider.get(provider);
      if (results == null) continue;
      for (final var result : results) {
        merged.add(result);
        priorities.put(result, provider.getPriority());
      }
    }
    // Stable sort, so results that score alike keep provider order and, within a provider, the
    // order it produced them in - which for the menus is menu order.
    merged.sort(
        Comparator.<SearchResult>comparingInt(SearchResult::score)
            .thenComparingInt(result -> priorities.getOrDefault(result, 0))
            .reversed());

    final var previous = resultList.getSelectedValue();
    replaceResults(resultModel, merged);

    var selection = previous == null ? -1 : resultModel.indexOf(previous);
    if (selection < 0 && !resultModel.isEmpty()) selection = 0;
    if (selection >= 0) {
      resultList.setSelectedIndex(selection);
      resultList.ensureIndexIsVisible(selection);
    }

    final var providerCount = String.valueOf(providers.size());
    statusLabel.setText(
        resultModel.isEmpty()
            ? S.get("searchNoResults", providerCount)
            : S.get(
                "searchResultCount",
                String.valueOf(resultModel.size()),
                providerCount));
  }

  /** Replaces the visible results using one removal and one addition event. */
  static void replaceResults(
      DefaultListModel<SearchResult> model, List<SearchResult> results) {
    model.clear();
    model.addAll(results);
  }

  /**
   * Names the providers behind the count in the status line, so "3 sources" can be
   * read as something concrete.
   */
  private String describeProviders() {
    if (providers.isEmpty()) return S.get("searchNoProviders");
    final var names = new StringBuilder();
    for (final var provider : providers) {
      if (!names.isEmpty()) names.append(", ");
      names.append(provider.getDisplayName());
    }
    return S.get("searchProviderList", names.toString());
  }

  /**
   * Handles the keys that drive the result list while focus stays in the search
   * field.
   */
  private void handleNavigationKey(KeyEvent event) {
    switch (event.getKeyCode()) {
      case KeyEvent.VK_DOWN -> {
        moveSelection(1);
        event.consume();
      }
      case KeyEvent.VK_UP -> {
        moveSelection(-1);
        event.consume();
      }
      case KeyEvent.VK_PAGE_DOWN -> {
        moveSelection(10);
        event.consume();
      }
      case KeyEvent.VK_PAGE_UP -> {
        moveSelection(-10);
        event.consume();
      }
      case KeyEvent.VK_ENTER -> {
        activateSelection();
        event.consume();
      }
      default -> {
        // Anything else is ordinary typing and belongs to the search field.
      }
    }
  }

  private void moveSelection(int delta) {
    final var size = resultModel.size();
    if (size == 0) return;
    // Wrap around, so pressing Up at the top lands on the last result rather than sticking.
    final var next = ((resultList.getSelectedIndex() + delta) % size + size) % size;
    resultList.setSelectedIndex(next);
    resultList.ensureIndexIsVisible(next);
  }

  /**
   * Runs the selected candidate, if it is available, and closes the dialog.
   */
  private void activateSelection() {
    final var result = resultList.getSelectedValue();
    if (result == null) return;
    final var candidate = result.candidate();
    if (!candidate.enabled()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    // Close first: the action may open a dialog of its own, which must not end up parented to a
    // window that is about to disappear.
    dispose();
    SwingUtilities.invokeLater(
        () -> {
          try {
            candidate.action().run();
          } catch (Exception e) {
            logger.error("Search result \"{}\" failed to run", candidate.displayText(), e);
          }
        });
  }

  /**
   * Muted colour for the dialog's own captions, derived by fading the label colour
   * towards the panel behind it. Deriving it beats reading
   * {@code Label.disabledForeground}, which some themes leave all but identical to
   * the ordinary foreground.
   */
  private static Color secondaryColor() {
    final var foreground = UIManager.getColor("Label.foreground");
    final var background = UIManager.getColor("Panel.background");
    if (foreground == null || background == null) {
      final var fallback = UIManager.getColor("Label.disabledForeground");
      return fallback != null ? fallback : Color.GRAY;
    }
    return blend(foreground, background, 0.40);
  }

  /**
   * Mixes {@code from} towards {@code towards}, where {@code amount} of 0 keeps
   * {@code from} unchanged and 1 yields {@code towards}.
   */
  private static Color blend(Color from, Color towards, double amount) {
    final var keep = 1.0 - amount;
    return new Color(
        (int) Math.round(from.getRed() * keep + towards.getRed() * amount),
        (int) Math.round(from.getGreen() * keep + towards.getGreen() * amount),
        (int) Math.round(from.getBlue() * keep + towards.getBlue() * amount));
  }


  /**
   * Renders one result: context in a muted colour, matched runs picked out with a
   * highlighter chip.
   *
   * <p>Every colour is derived from the row it is drawn on rather than read from the
   * look-and-feel, because the selected row and the ordinary rows have different
   * backgrounds and a fixed palette is invisible on one or the other. Only the chip
   * itself is a fixed hue - amber reads clearly against white, against a dark grey,
   * and against the blue that most themes select rows with.
   */
  private static class ResultRenderer extends JPanel implements ListCellRenderer<SearchResult> {

    private static final long serialVersionUID = 1L;

    /**
     * Highlighter chip behind matched characters. One amber serves every theme:
     * measured against the list backgrounds of all six shipped look-and-feels,
     * selected rows included, it never drops below 2:1 against the row and reaches
     * 7:1 on the darkest, which beats picking a per-theme shade.
     */
    private static final Color CHIP_BACKGROUND = new Color(0xFF, 0xA0, 0x00);

    /** Text drawn on the chip; 8.5:1 against {@link #CHIP_BACKGROUND}. */
    private static final Color CHIP_FOREGROUND = new Color(0x1A, 0x1A, 0x1A);

    /** How far the menu path is faded towards the row background. */
    private static final double CONTEXT_FADE = 0.30;

    /** How far an unavailable row's text is faded towards the row background. */
    private static final double DISABLED_FADE = 0.35;

    private final JLabel textLabel = new JLabel();
    private final JLabel hintLabel = new JLabel();

    ResultRenderer() {
      super(new BorderLayout(8, 0));
      setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
      add(textLabel, BorderLayout.CENTER);
      add(hintLabel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends SearchResult> list,
        SearchResult value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {
      final var candidate = value.candidate();
      final var enabled = candidate.enabled();

      // Focus lives in the search field by design, so the look-and-feel would otherwise
      // paint the cursor row in its washed-out "inactive selection" colour. Ask for the
      // active colours directly, so the row under the cursor always reads as selected.
      final var rowBackground =
          isSelected ? activeSelectionBackground(list) : list.getBackground();
      final var rowForeground =
          isSelected ? activeSelectionForeground(list) : list.getForeground();
      // Unavailable rows are faded towards their own background, which keeps them legible
      // whichever way round the theme runs.
      final var titleColor =
          enabled ? rowForeground : blend(rowForeground, rowBackground, DISABLED_FADE);
      // Faded from the row's own foreground rather than from titleColor: fading a second
      // time on top of a disabled row compounds into something barely legible.
      final var contextColor =
          blend(
              rowForeground,
              rowBackground,
              Math.max(CONTEXT_FADE, enabled ? 0.0 : DISABLED_FADE));

      setOpaque(true);
      setBackground(rowBackground);
      textLabel.setForeground(titleColor);
      hintLabel.setForeground(contextColor);

      textLabel.setIcon(enabled ? candidate.icon() : null);
      textLabel.setText(toHtml(candidate, value.highlights(), contextColor));
      hintLabel.setText(candidate.hint());
      return this;
    }

    private static Color activeSelectionBackground(JList<?> list) {
      final var color = UIManager.getColor("List.selectionBackground");
      return color != null ? color : list.getSelectionBackground();
    }

    private static Color activeSelectionForeground(JList<?> list) {
      final var color = UIManager.getColor("List.selectionForeground");
      return color != null ? color : list.getSelectionForeground();
    }

    /**
     * Builds the row's markup: the menu path faded, and each run of matched
     * characters wrapped in a coloured chip so it stands out whatever the row
     * behind it looks like.
     */
    private static String toHtml(
        SearchCandidate candidate, int[] highlights, Color contextColor) {
      final var text = candidate.displayText();
      final var length = text.length();
      final var titleOffset = candidate.titleOffset();
      final var matched = new boolean[length];
      for (final var position : highlights) {
        if (position >= 0 && position < length) matched[position] = true;
      }

      final var chipOpen =
          "<span style=\"background-color:"
              + toHexColor(CHIP_BACKGROUND)
              + ";color:"
              + toHexColor(CHIP_FOREGROUND)
              + "\"><b>";
      final var html = new StringBuilder("<html><nobr>");
      var tinted = titleOffset > 0;
      if (tinted) {
        html.append("<font color=\"").append(toHexColor(contextColor)).append("\">");
      }

      var i = 0;
      while (i < length) {
        if (tinted && i == titleOffset) {
          html.append("</font>");
          tinted = false;
        }
        // Runs stop at the path/title boundary so the chip and the fade never
        // interleave into invalid markup.
        final var limit = tinted ? Math.min(length, titleOffset) : length;
        final var highlighted = matched[i];
        var end = i;
        while (end < limit && matched[end] == highlighted) end++;

        if (highlighted) html.append(chipOpen);
        for (var j = i; j < end; j++) {
          appendEscaped(html, text.charAt(j));
        }
        if (highlighted) html.append("</b></span>");
        i = end;
      }
      if (tinted) html.append("</font>");
      return html.append("</nobr></html>").toString();
    }

    private static void appendEscaped(StringBuilder out, char ch) {
      switch (ch) {
        case '&' -> out.append("&amp;");
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        default -> out.append(ch);
      }
    }

    private static String toHexColor(Color color) {
      return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
  }
}
