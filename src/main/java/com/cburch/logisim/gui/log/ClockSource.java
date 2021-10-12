/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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

  @Override
  public void okClicked() {
    final var list = selector.getSelectedItems();
    if (list == null || list.size() != 1) return;
    item = list.get(0);
  }

  public static Component doClockDriverDialog(Circuit circ) {
    final var dialog = new ClockSource(S.getter("selectClockDriverMessage"), circ, true);
    dialog.setVisible(true);
    return dialog.item == null ? null : dialog.item.getComponent(); // always top-level
  }

  public static SignalInfo doClockMissingObserverDialog(Circuit circ) {
    final var dialog = new ClockSource(S.getter("selectClockMissingMessage"), circ, false);
    dialog.setVisible(true);
    return dialog.item;
  }

  public static SignalInfo doClockMultipleObserverDialog(Circuit circ) {
    final var dialog = new ClockSource(S.getter("selectClockMultipleMessage"), circ, false);
    dialog.setVisible(true);
    return dialog.item;
  }

  public static SignalInfo doClockObserverDialog(Circuit circ) {
    final var dialog = new ClockSource(S.getter("selectClockObserverMessage"), circ, false);
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
    final var clk = clockSource.getComponent();
    if (clk.getFactory() instanceof Clock) {
      final var hi = clk.getAttributeSet().getValue(Clock.ATTR_HIGH);
      final var lo = clk.getAttributeSet().getValue(Clock.ATTR_LOW);
      final var phase = clk.getAttributeSet().getValue(Clock.ATTR_PHASE);
      return new CycleInfo(hi, lo, phase);
    }
    return DEFAULT_CYCLE_INFO;
  }
}
