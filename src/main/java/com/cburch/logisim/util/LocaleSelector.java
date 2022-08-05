/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.logisim.prefs.AppPreferences;
import java.util.Locale;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@SuppressWarnings("rawtypes")
class LocaleSelector extends JList implements LocaleListener, ListSelectionListener {
  private static class LocaleOption implements Runnable {
    private final Locale locale;
    private String text;

    LocaleOption(Locale locale) {
      this.locale = locale;
      update(locale);
    }

    @Override
    public void run() {
      if (!LocaleManager.getLocale().equals(locale)) {
        LocaleManager.setLocale(locale);
        AppPreferences.LOCALE.set(locale.getLanguage());
      }
    }

    @Override
    public String toString() {
      return text;
    }

    void update(Locale current) {
      text =
          (current != null && current.equals(locale))
              ? locale.getDisplayName(locale)
              : locale.getDisplayName(locale) + " / " + locale.getDisplayName(current);
    }
  }

  private static final long serialVersionUID = 1L;

  private final LocaleOption[] items;

  @SuppressWarnings("unchecked")
  LocaleSelector(Locale[] locales) {
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final var model = new DefaultListModel<LocaleOption>();
    items = new LocaleOption[locales.length];
    for (var i = 0; i < locales.length; i++) {
      items[i] = new LocaleOption(locales[i]);
      model.addElement(items[i]);
    }
    setModel(model);
    setVisibleRowCount(Math.min(items.length, 8));
    LocaleManager.addLocaleListener(this);
    localeChanged();
    addListSelectionListener(this);
  }

  @Override
  public void localeChanged() {
    final var current = LocaleManager.getLocale();
    LocaleOption sel = null;
    for (final var item : items) {
      item.update(current);
      if (current.equals(item.locale)) sel = item;
    }
    if (sel != null) {
      setSelectedValue(sel, true);
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    final var opt = (LocaleOption) getSelectedValue();
    if (opt != null) {
      SwingUtilities.invokeLater(opt);
    }
  }
}
