/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.chrono;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.gui.log.SelectionPanel;
import com.cburch.logisim.gui.log.Signal;
import com.cburch.logisim.gui.log.SignalInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

public class PopupMenu extends MouseAdapter {

  private class PopupContents extends JPopupMenu {
    private static final long serialVersionUID = 1L;

    public PopupContents() {
      super("Options");
      RadixOption radix = null;
      if (signals.size() > 0) {
        radix = signals.get(0).info.getRadix();
        for (int i = 1; i < signals.size(); i++)
          if (signals.get(i).info.getRadix() != radix) {
            radix = null;
            break;
          }
      }
      ButtonGroup g = new ButtonGroup();
      for (RadixOption r : RadixOption.OPTIONS) {
        JRadioButtonMenuItem m = new JRadioButtonMenuItem(r.toDisplayString());
        add(m);
        m.setEnabled(signals.size() > 0);
        g.add(m);
        if (r == radix)
          m.setSelected(true);
        m.addActionListener(e -> {
          for (Signal s : signals)
            s.info.setRadix(r);
        });
      }
      JMenuItem m;
      addSeparator();
      m = new JMenuItem(S.get("editClearItem"));
      add(m);
      m.setEnabled(signals.size() > 0);
      m.addActionListener(e -> {
        SignalInfo.List items = new SignalInfo.List();
        for (Signal s : signals)
          items.add(s.info);
        chronoPanel.getModel().remove(items);
      });
      addSeparator();
      m = new JMenuItem(S.get("addRemoveSignals"));
      add(m);
      m.addActionListener(e -> SelectionPanel.doDialog(chronoPanel.getLogFrame()));

    }
  }

  private final List<Signal> signals;
  private final ChronoPanel chronoPanel;

  public PopupMenu(ChronoPanel p, List<Signal> s) {
    chronoPanel = p;
    signals = s;
  }

  public void doPop(MouseEvent e) {
    PopupContents menu = new PopupContents();
    menu.show(e.getComponent(), e.getX(), e.getY());
  }

  public void mousePressed(MouseEvent e) {
    if (e.isPopupTrigger()) doPop(e);
  }

}
