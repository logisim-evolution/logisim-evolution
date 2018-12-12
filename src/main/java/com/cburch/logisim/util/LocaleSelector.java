/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.util;

import java.util.Locale;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.cburch.logisim.prefs.AppPreferences;

@SuppressWarnings("rawtypes")
class LocaleSelector extends JList implements LocaleListener,
		ListSelectionListener {
	private static class LocaleOption implements Runnable {
		private Locale locale;
		private String text;

		LocaleOption(Locale locale) {
			this.locale = locale;
			update(locale);
		}

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
			if (current != null && current.equals(locale)) {
				text = locale.getDisplayName(locale);
			} else {
				text = locale.getDisplayName(locale) + " / "
						+ locale.getDisplayName(current);
			}
		}
	}

	private static final long serialVersionUID = 1L;

	private LocaleOption[] items;

	@SuppressWarnings("unchecked")
	LocaleSelector(Locale[] locales) {
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		DefaultListModel<LocaleOption> model = new DefaultListModel<>();
		items = new LocaleOption[locales.length];
		for (int i = 0; i < locales.length; i++) {
			items[i] = new LocaleOption(locales[i]);
			model.addElement(items[i]);
		}
		setModel(model);
		setVisibleRowCount(Math.min(items.length, 8));
		LocaleManager.addLocaleListener(this);
		localeChanged();
		addListSelectionListener(this);
	}

	public void localeChanged() {
		Locale current = LocaleManager.getLocale();
		LocaleOption sel = null;
		for (int i = 0; i < items.length; i++) {
			items[i].update(current);
			if (current.equals(items[i].locale))
				sel = items[i];
		}
		if (sel != null) {
			setSelectedValue(sel, true);
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		LocaleOption opt = (LocaleOption) getSelectedValue();
		if (opt != null) {
			SwingUtilities.invokeLater(opt);
		}
	}
}
