/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.gui.menu.MenuSimulate;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.LocaleListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class FpgaClockPanel extends JPanel implements ActionListener, LocaleListener {

  private static final long serialVersionUID = 1L;
  private final Project myProject;
  private Circuit rootSheet;
  private final JLabel freqLabel = new JLabel();
  private final JLabel divLabel = new JLabel();
  private final JComboBox<String> frequenciesList = new JComboBox<>();
  private final JTextField divider = new JTextField();
  private double FPGAClockFrequency;

  public FpgaClockPanel(Project proj) {
    super();
    myProject = proj;
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createStrokeBorder(new BasicStroke(2)), S.get("FpgaFreqTitle")));
    JPanel pan1 = new JPanel();
    pan1.setLayout(new BorderLayout());
    pan1.add(freqLabel, BorderLayout.NORTH);
    pan1.add(frequenciesList, BorderLayout.SOUTH);
    frequenciesList.addActionListener(this);
    frequenciesList.setActionCommand("frequency");
    frequenciesList.setEditable(true);
    for (var freq : MenuSimulate.getTickFrequencyStrings())
      frequenciesList.addItem(freq);
    frequenciesList.setSelectedIndex(0);
    add(pan1, BorderLayout.WEST);
    pan1 = new JPanel();
    pan1.setLayout(new BorderLayout());
    pan1.add(divLabel, BorderLayout.NORTH);
    divider.addActionListener(this);
    divider.setActionCommand("divider");
    pan1.add(divider, BorderLayout.SOUTH);
    add(pan1, BorderLayout.CENTER);
    localeChanged();
  }

  @Override
  public void setEnabled(boolean enabled) {
    freqLabel.setEnabled(enabled);
    divLabel.setEnabled(enabled);
    frequenciesList.setEnabled(enabled);
    divider.setEnabled(enabled);
  }

  public void updateFrequencyList(String circuitName) {
    final var newTopCircuit = myProject.getLogisimFile().getCircuit(circuitName);
    if ((newTopCircuit == null) || (newTopCircuit == rootSheet)) return;
    rootSheet = newTopCircuit;
    final var savedDownloadFrequency = rootSheet.getDownloadFrequency();
    final var savedSimFrequency = rootSheet.getTickFrequency();
    final var selectedFrequency = (savedDownloadFrequency > 0) ? savedDownloadFrequency : savedSimFrequency;
    for (var i = 0; i < MenuSimulate.SUPPORTED_TICK_FREQUENCIES.length; i++) {
      if (MenuSimulate.SUPPORTED_TICK_FREQUENCIES[i].equals(selectedFrequency)) {
        frequenciesList.setSelectedIndex(i);
        recalculateFrequency();
        return;
      }
    }
    setSelectedFrequency(selectedFrequency);
  }

  private void setSelectedFrequency(double freq) {
    if (freq <= 0) return;
    if (rootSheet != null) {
      final var savedDownloadFrequency = rootSheet.getDownloadFrequency();
      final var savedSimFrequency = rootSheet.getTickFrequency();
      if (((savedDownloadFrequency > 0) && (freq != savedDownloadFrequency))
          || ((freq != savedSimFrequency) && (freq != savedDownloadFrequency)))
        rootSheet.setDownloadFrequency(freq);
    }
    for (int i = 0; i < MenuSimulate.SUPPORTED_TICK_FREQUENCIES.length; i++) {
      if (MenuSimulate.SUPPORTED_TICK_FREQUENCIES[i] == freq) {
        frequenciesList.setSelectedIndex(i);
        return;
      }
    }
    StringBuilder extention = new StringBuilder();
    extention.append(" ");
    double work = freq;
    if (work > 1000000.0) {
      extention.append("M");
      work /= 1000000.0;
    }
    if (work > 1000.0) {
      extention.append("k");
      work /= 1000.0;
    }
    extention.append("Hz");
    DecimalFormat df = new DecimalFormat("#.#####");
    df.setRoundingMode(RoundingMode.HALF_UP);
    String tick = df.format(work) + extention;
    frequenciesList.setSelectedItem(tick);
  }

  public void setFpgaClockFrequency(long frequency) {
    FPGAClockFrequency = frequency;
    recalculateFrequency();
  }

  public double getTickFrequency() {
    String TickIndex = frequenciesList.getSelectedItem().toString().trim().toUpperCase();
    int i = 0;
    /* first pass, find the number */
    StringBuilder number = new StringBuilder();
    while (i < TickIndex.length()
        && (TickIndex.charAt(i) == '.' || Character.isDigit(TickIndex.charAt(i))))
      number.append(TickIndex.charAt(i++));
    /*second pass, get the Hz, etc */
    char extention = 0;
    while (i < TickIndex.length()) {
      if (TickIndex.charAt(i) == 'K' || TickIndex.charAt(i) == 'M')
        extention = TickIndex.charAt(i);
      i++;
    }
    return Double.parseDouble(number.toString()) * switch (extention) {
      case 'K' -> 1000d;
      case 'M' -> 1000000d;
      default -> 1d;
    };
  }

  private void recalculateFrequency() {
    double freq = getTickFrequency();
    double divider = FPGAClockFrequency / freq;
    long longDivider = (long) divider;
    if (longDivider <= 1) longDivider = 2;
    if ((longDivider & 1) != 0) longDivider++;
    double corfreq = FPGAClockFrequency / longDivider;
    this.divider.setText(Long.toString((longDivider) >> 1));
    setSelectedFrequency(corfreq);
  }

  private void recalculateDivider() {
    long divider = 0;
    try {
      divider = Long.parseUnsignedLong(this.divider.getText());
    } catch (NumberFormatException e) {
      recalculateFrequency();
      return;
    }
    divider <<= 1;
    if (divider <= 1) divider = 2;
    double corfreq = FPGAClockFrequency / divider;
    if (corfreq < 0.00001) {
      recalculateFrequency();
      return;
    }
    setSelectedFrequency(corfreq);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("frequency")) {
      recalculateFrequency();
    } else if (e.getActionCommand().equals("divider")) {
      recalculateDivider();
    }
  }

  @Override
  public void localeChanged() {
    freqLabel.setText(S.get("FpgaFreqFrequency"));
    divLabel.setText(S.get("FpgaFreqDivider"));
  }
}
