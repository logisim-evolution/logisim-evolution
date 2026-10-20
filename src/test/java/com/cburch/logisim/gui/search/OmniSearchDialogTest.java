/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javax.swing.DefaultListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.junit.jupiter.api.Test;

class OmniSearchDialogTest {

  @Test
  void replacesLargeResultSetsInOneBatch() {
    final var model = new DefaultListModel<SearchResult>();
    model.addElement(result("old"));
    final var additions = new AtomicInteger();
    final var removals = new AtomicInteger();
    final var changes = new AtomicInteger();
    model.addListDataListener(
        new ListDataListener() {
          @Override
          public void intervalAdded(ListDataEvent event) {
            additions.incrementAndGet();
          }

          @Override
          public void intervalRemoved(ListDataEvent event) {
            removals.incrementAndGet();
          }

          @Override
          public void contentsChanged(ListDataEvent event) {
            changes.incrementAndGet();
          }
        });
    final var results = IntStream.range(0, 600).mapToObj(i -> result("result " + i)).toList();

    OmniSearchDialog.replaceResults(model, results);

    assertEquals(600, model.size());
    assertEquals(1, additions.get());
    assertEquals(1, removals.get());
    assertEquals(0, changes.get());
  }

  private static SearchResult result(String title) {
    return SearchResult.of(SearchCandidate.of(title, "", true, () -> {}), 0);
  }
}
