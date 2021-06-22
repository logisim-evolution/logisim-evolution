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

package com.cburch.logisim.gui.log;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.StringGetter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class ClockSource extends JDialogOk {

  private static final long serialVersionUID = 1L;
  private final ComponentSelector selector;
  private final JLabel msgLabel = new JLabel();
  private final StringGetter msg;
  SignalInfo item;

  public ClockSource(StringGetter msg, Circuit circ, boolean requireDriveable) {
    super("Clock Source Selection", true);
    this.msg = msg;

    selector =
        new ComponentSelector(
            circ,
            requireDriveable
                ? ComponentSelector.DRIVEABLE_CLOCKS
                : ComponentSelector.OBSERVEABLE_CLOCKS);

    JScrollPane explorerPane =
        new JScrollPane(
            selector,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    explorerPane.setPreferredSize(new Dimension(120, 200));

    msgLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    explorerPane.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10), explorerPane.getBorder()));
    getContentPane().add(msgLabel, BorderLayout.NORTH);
    getContentPane().add(explorerPane, BorderLayout.CENTER);

    localeChanged();

    setMinimumSize(new Dimension(200, 300));
    setPreferredSize(new Dimension(300, 400));
    pack();
  }

  public void localeChanged() {
    selector.localeChanged();
    msgLabel.setText("<html>" + msg.toString() + "</html>"); // for line breaking
  }

  public void okClicked() {
    SignalInfo.List list = selector.getSelectedItems();
    if (list == null || list.size() != 1) return;
    item = list.get(0);
  }

  public static Component doClockDriverDialog(Circuit circ) {
    ClockSource dialog = new ClockSource(S.getter("selectClockDriverMessage"), circ, true);
    dialog.setVisible(true);
    return dialog.item == null ? null : dialog.item.getComponent(); // always top-level
  }

  public static SignalInfo doClockMissingObserverDialog(Circuit circ) {
    ClockSource dialog = new ClockSource(S.getter("selectClockMissingMessage"), circ, false);
    dialog.setVisible(true);
    return dialog.item;
  }

  public static SignalInfo doClockMultipleObserverDialog(Circuit circ) {
    ClockSource dialog = new ClockSource(S.getter("selectClockMultipleMessage"), circ, false);
    dialog.setVisible(true);
    return dialog.item;
  }

  public static SignalInfo doClockObserverDialog(Circuit circ) {
    ClockSource dialog = new ClockSource(S.getter("selectClockObserverMessage"), circ, false);
    dialog.setVisible(true);
    return dialog.item;
  }

  public static class CycleInfo {
    public final int hi;
    public final int lo;
    public final int phase;
    public final int ticks;

    public CycleInfo(int h, int l, int p) {
      hi = h;
      lo = l;
      phase = p;
      ticks = hi + lo;
    }
  }

  public static final CycleInfo DEFAULT_CYCLE_INFO = new CycleInfo(1, 1, 0);

  public static CycleInfo getCycleInfo(SignalInfo clockSource) {
    Component clk = clockSource.getComponent();
    if (clk.getFactory() instanceof Clock) {
      int hi = clk.getAttributeSet().getValue(Clock.ATTR_HIGH);
      int lo = clk.getAttributeSet().getValue(Clock.ATTR_LOW);
      int phase = clk.getAttributeSet().getValue(Clock.ATTR_PHASE);
      return new CycleInfo(hi, lo, phase);
    }
    return DEFAULT_CYCLE_INFO;
  }
}
