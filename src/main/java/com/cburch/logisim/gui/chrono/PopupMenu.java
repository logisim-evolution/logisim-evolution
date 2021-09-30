/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
        for (var i = 1; i < signals.size(); i++)
          if (signals.get(i).info.getRadix() != radix) {
            radix = null;
            break;
          }
      }
      final var g = new ButtonGroup();
      for (final var r : RadixOption.OPTIONS) {
        final var m = new JRadioButtonMenuItem(r.toDisplayString());
        add(m);
        m.setEnabled(signals.size() > 0);
        g.add(m);
        if (r == radix) m.setSelected(true);
        m.addActionListener(e -> {
          for (final var s : signals)
            s.info.setRadix(r);
        });
      }
      addSeparator();
      var m = new JMenuItem(S.get("editClearItem"));
      add(m);
      m.setEnabled(signals.size() > 0);
      m.addActionListener(e -> {
        final var items = new SignalInfo.List();
        for (final var s : signals)
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
    final var menu = new PopupContents();
    menu.show(e.getComponent(), e.getX(), e.getY());
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (e.isPopupTrigger()) doPop(e);
  }

}
