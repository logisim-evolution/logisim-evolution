/**
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

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

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

import com.cburch.logisim.gui.menu.MenuSimulate;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.LocaleListener;

public class FPGAClockPanel extends JPanel implements ActionListener,LocaleListener {

  private static final long serialVersionUID = 1L;
  private Project MyProject;
  private JLabel freqLabel = new JLabel();
  private JLabel divLabel = new JLabel();
  private JComboBox<String> frequenciesList = new JComboBox<>();
  private JTextField divider = new JTextField();
  private double FPGAClockFrequency;

  public FPGAClockPanel(Project proj) {
    super();
    MyProject = proj;
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createStrokeBorder(new BasicStroke(2)), S.get("FpgaFreqTitle")));
    updateFrequencyList();
    JPanel pan1 = new JPanel();
    pan1.setLayout(new BorderLayout());
    pan1.add(freqLabel, BorderLayout.NORTH);
    pan1.add(frequenciesList, BorderLayout.SOUTH);
    frequenciesList.addActionListener(this);
    frequenciesList.setActionCommand("frequency");
    frequenciesList.setEditable(true);
    add(pan1,BorderLayout.WEST);
    pan1 = new JPanel();
    pan1.setLayout(new BorderLayout());
    pan1.add(divLabel, BorderLayout.NORTH);
    divider.addActionListener(this);
    divider.setActionCommand("divider");
    pan1.add(divider, BorderLayout.SOUTH);
    add(pan1, BorderLayout.CENTER);
    localeChanged();
  }
  
  public void setEnabled( boolean enabled ) {
    freqLabel.setEnabled(enabled);
    divLabel.setEnabled(enabled);
    frequenciesList.setEnabled(enabled);
    divider.setEnabled(enabled);
  }
  
  private void updateFrequencyList() {
    frequenciesList.removeAllItems();
    for (String freq : MenuSimulate.getTickFrequencyStrings()) {
      frequenciesList.addItem(freq);
    }
    frequenciesList.setSelectedIndex(0);
    for (int i = 0; i < MenuSimulate.SupportedTickFrequencies.length; i++) {
      if (MenuSimulate.SupportedTickFrequencies[i].equals(
          MyProject.getSimulator().getTickFrequency())) {
        frequenciesList.setSelectedIndex(i);
        recalculateFrequency();
      }
    }
  }
  
  public void setSelectedFrequency() {
    for (int i = 0; i < MenuSimulate.SupportedTickFrequencies.length; i++) {
      if (MenuSimulate.SupportedTickFrequencies[i].equals(
          MyProject.getSimulator().getTickFrequency())) {
        if (i != frequenciesList.getSelectedIndex()) {
          frequenciesList.setSelectedIndex(i);
          recalculateFrequency();
        }
        break;
      }
    }
  }
  
  public void setFpgaClockFrequency(long frequency) {
    FPGAClockFrequency = frequency;
    recalculateFrequency();
  }
  
  public double GetTickfrequency() {
    double ret = 0.0;
    String TickIndex = frequenciesList.getSelectedItem().toString().trim().toUpperCase();
    int i = 0;
    /* first pass, find the number */
    StringBuffer number = new StringBuffer();
    while (i<TickIndex.length() && 
       (TickIndex.charAt(i)=='.' || Character.isDigit(TickIndex.charAt(i)))) 
      number.append(TickIndex.charAt(i++));
    /*second pass, get the Hz, etc */
    char extention = 0;
    while (i < TickIndex.length()) {
      if (TickIndex.charAt(i) == 'K' || TickIndex.charAt(i) == 'M')
        extention = TickIndex.charAt(i);
      i++;
    }
    ret = Double.valueOf(number.toString());
    switch (extention) {
      case 'K' : ret *= 1000;
                 break;
      case 'M' : ret *= 1000000;
                 break;
    }
    return ret;
  }
  
  private void setSelectedFrequency(double freq) {
     for (int i = 0; i < MenuSimulate.SupportedTickFrequencies.length; i++) {
       if (MenuSimulate.SupportedTickFrequencies[i] == freq) {
         frequenciesList.setSelectedIndex(i);
         return;
       }
     }
     StringBuffer extention = new StringBuffer();
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
     String tick = df.format(work)+extention.toString();
     frequenciesList.setSelectedItem(tick);
  }
  
  private void recalculateFrequency() {
    double freq = GetTickfrequency();
    double divider = FPGAClockFrequency/freq;
    long longDivider = (long) divider;
    if (longDivider <= 1) longDivider = 2;
    if ((longDivider&1) != 0) longDivider++;
    double corfreq = FPGAClockFrequency/longDivider;
    this.divider.setText(Long.toString((longDivider)>>1));
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
    double corfreq = FPGAClockFrequency/divider;
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
