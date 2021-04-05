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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.NumberFormatter;

import com.cburch.logisim.comp.ComponentEvent;

class OptionsPanel extends LogPanel implements ActionListener, ChangeListener {

  // [ ] Stop-motion mode [ time_scale: 5ms ]
  //   [ ] Coarse-grained summary
  //     Data is captured only when all signals have stabilized, hiding any
  //     transient signal fluctuations that may happen during propagation.  Each
  //     captured value is recorded as if it were held stable for {5ms}.
  //   [ ] Fine-grained details  [ gate_delay: 10ns ]
  //     Data is captured whenever any signal value changes, including any
  //     transient signal fluctuations that may happen during propagation.
  //     Transient fluctuations are recorded as if the gate delay were {10ns},
  //     and stable signals are recorded as if they were held stable for {5ms}.
  // [ ] Real-time mode aka continuous [ time_scale: 5ms per second ]
  //   [ ] Coarse-grained summary
  //     Data is captured continuously, whenever the simulator is enabled.
  //     Transient signal fluctuations are ignored. Each second of real time is
  //     recorded as {5ms} of simulated circuit time.
  //   [ ] Fine-grained details
  //     Data is captured continuously, whenever the simulator is enabled.
  //     Transient signal fluctuations are captured. Each second of real time is
  //     recorded as {5ms} of simulated circuit time.
  // [ ] Clocked mode [ time_scale: 5ms per tick ] [ clock_cycle_length: 2 ticks ]
  //   [ ] Coarse-grained summary 
  //     Data is captured at the end of each clock cycle (2 ticks), hiding any
  //     transient fluctuations that happen during the clock cycle.
  //     Each clock cycle is recorded as if it took {10ms} per cycle ({5ms} per tick).
  //   [ ] Fine-grained details  [ gate_delay: 10ns ]
  //     Data is captured whenever any signal value changes, including any
  //     transient signal fluctuations that may happen during propagation. 
  //     Transient fluctuations are recorded as if the gate delay were {10ns},
  //     and each clock cycle is recorded as if it took {10ms} per cycle ({5ms}
  //     per tick).
  private static final long serialVersionUID = 1L;
  JButton selectionButton;

  JRadioButton stepTime = new JRadioButton();
  JRadioButton realTime = new JRadioButton();
  JRadioButton clockTime = new JRadioButton();

  // todo: save defaults with project and/or compute from circuit clocks
  JCheckBox stepFine = new JCheckBox();
  JCheckBox realFine = new JCheckBox();
  JCheckBox clockFine = new JCheckBox();

  TimeSelector stepScale = new TimeSelector("timeScale", 5000);
  TimeSelector stepGate = new TimeSelector("gateDelay", 10);
  TimeSelector realScale = new TimeSelector("timeScale", 5000, "perSecond");
  TimeSelector clockScale = new TimeSelector("timeScale", 5000, "perTick");
  TickSelector clockTicks = new TickSelector("cycleLength", 2);
  TimeSelector clockGate = new TimeSelector("gateDelay", 10);
  
  JCheckBox unlimited = new JCheckBox();
  JSpinner limit = new JSpinner();
  JLabel limitLabel = new JLabel();

  JLabel description = new JLabel();

  JPanel selectionPanel = new JPanel();
  Box modePanel = new Box(BoxLayout.Y_AXIS);
  JPanel optionsPanel = new JPanel(new CardLayout());
  Box stepOptionsPanel = new Box(BoxLayout.Y_AXIS);
  Box realOptionsPanel = new Box(BoxLayout.Y_AXIS);
  Box clockOptionsPanel = new Box(BoxLayout.Y_AXIS);
  Box historyPanel = new Box(BoxLayout.Y_AXIS);

  JScrollPane pane;

  // todo: tooltips?
  OptionsPanel(LogFrame frame) {
    super(frame);

    selectionButton = frame.makeSelectionButton();
    selectionPanel.add(selectionButton);

    ButtonGroup g = new ButtonGroup();
    g.add(stepTime);
    g.add(realTime);
    g.add(clockTime);
    modePanel.add(stepTime);
    modePanel.add(realTime);
    modePanel.add(clockTime);
    
    // middle has timing options for each mode
    stepFine.setAlignmentX(0.0f); 
    stepOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    stepOptionsPanel.add(stepFine);
    stepOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    stepOptionsPanel.add(stepScale.getPanel());
    stepOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    stepOptionsPanel.add(stepGate.getPanel());
    stepOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));

    realFine.setAlignmentX(0.0f); 
    realOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    realOptionsPanel.add(realFine);
    realOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    realOptionsPanel.add(realScale.getPanel());
    realOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));

    clockFine.setAlignmentX(0.0f); 
    clockOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    clockOptionsPanel.add(clockFine);
    clockOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    clockOptionsPanel.add(clockScale.getPanel());
    clockOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    clockOptionsPanel.add(clockTicks.getPanel());
    clockOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    clockOptionsPanel.add(clockGate.getPanel());
    clockOptionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));

    optionsPanel.add(stepOptionsPanel, "stepTime");
    optionsPanel.add(realOptionsPanel, "realTime");
    optionsPanel.add(clockOptionsPanel, "clockTime");

    // right side has history options
    limit.setModel(new SpinnerNumberModel(400, 10, Integer.MAX_VALUE, 100));
    limit.setEditor(new JSpinner.NumberEditor(limit, "####"));
    JFormattedTextField txt = ((JSpinner.NumberEditor)limit.getEditor()).getTextField();
    ((NumberFormatter)txt.getFormatter()).setAllowsInvalid(false);
    unlimited.setAlignmentX(0.0f);
    Box limitBox = new Box(BoxLayout.X_AXIS);
    limitBox.add(limitLabel);
    limitBox.add(Box.createRigidArea(new Dimension(6, 0)));
    limitBox.add(limit);
    limitBox.setAlignmentX(0.0f);
    historyPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    historyPanel.add(unlimited);
    historyPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    historyPanel.add(limitBox);
    historyPanel.add(Box.createVerticalGlue());

    JPanel inner = new ScrollablePanel();

    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    inner.setLayout(gb);

    gc.fill = GridBagConstraints.BOTH;
    gc.insets = new Insets(5, 5, 5, 5);
    gc.weightx = gc.weighty = 0.0f;
    gc.gridx = gc.gridy = 0;
    gb.setConstraints(selectionPanel, gc);
    inner.add(selectionPanel);

    gc.gridy = 1;

    gb.setConstraints(modePanel, gc);
    inner.add(modePanel);

    gc.gridx = 1;
    gc.gridy = 0;
    gc.gridheight = 2;
    gb.setConstraints(optionsPanel, gc);
    inner.add(optionsPanel);

    gc.gridx = 2;
    gb.setConstraints(historyPanel, gc);
    inner.add(historyPanel);

    Component fill = Box.createGlue();
    gc.gridx = 3;
    gb.setConstraints(fill, gc);
    inner.add(fill);

    gc.weightx = gc.weighty = 1.0f;
    gc.gridx = 0;
    gc.gridy = 2;
    gc.gridheight = 1;
    gc.gridwidth = 4;
    gb.setConstraints(description, gc);
    inner.add(description);
    description.setFont(description.getFont().deriveFont(Font.PLAIN));

    pane = new JScrollPane(inner,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    setLayout(new BorderLayout());
    add(pane, BorderLayout.CENTER);

    unlimited.addActionListener(this);
    limit.addChangeListener(this);
    stepTime.addActionListener(this);
    realTime.addActionListener(this);
    clockTime.addActionListener(this);
    stepFine.addActionListener(this);
    realFine.addActionListener(this);
    clockFine.addActionListener(this);
    stepScale.addActionListener(this);
    stepGate.addActionListener(this);
    realScale.addActionListener(this);
    clockScale.addActionListener(this);
    clockTicks.addActionListener(this);
    clockGate.addActionListener(this);

    stepTime.setSelected(false);
    realTime.setSelected(false);
    clockTime.setSelected(false);
    modelChanged(null, getModel());    
    localeChanged();

    addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        pane.getVerticalScrollBar().setValue(0);
        pane.getHorizontalScrollBar().setValue(0);
      }
    });

  }

  static class ScrollablePanel extends JPanel implements Scrollable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 10;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return ((orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width) - 10;
    }

    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    public boolean getScrollableTracksViewportHeight() {
      return false;
    }
  }

  @Override
  public void stateChanged(ChangeEvent e) {              
    Model m = getLogFrame().getModel();
    limit.setEnabled(!unlimited.isSelected());
    if (unlimited.isSelected()) {
      m.setHistoryLimit(0);
    } else {
      try {
        limit.commitEdit();
      } catch (ParseException ex) {
        // revert to last valid value
        JComponent editor = limit.getEditor();
        ((JSpinner.NumberEditor)editor).getTextField().setValue(limit.getValue());
      }
      m.setHistoryLimit((Integer)limit.getValue());
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {              
    Model m = getLogFrame().getModel();
    if (e.getSource() == unlimited) {
      stateChanged(null);
    } else {
      if (stepTime.isSelected())
        m.setStepMode(stepScale.getValue(), stepFine.isSelected() ? stepGate.getValue() : 0);
      else if (realTime.isSelected())
        m.setRealMode(realScale.getValue(), realFine.isSelected());
      else
        m.setClockMode(clockScale.getValue(), clockTicks.getValue(),
            clockFine.isSelected() ? clockGate.getValue() : 0);
      updateDescription();
    }
  }

  static class TimeEditor extends BasicComboBoxEditor {
    String suffix;
    TimeVerifier v;
    public TimeEditor(String s) { suffix = s; v = new TimeVerifier(suffix); }
    protected JTextField createEditorComponent() {
      JTextField f = super.createEditorComponent();
      f.setInputVerifier(v);
      return f;
    }
    public Object getItem() {
      return v.matched ? v.value * v.scale : null;
    }
  }

  static class TimeVerifier extends InputVerifier {
    String suffix;
    public TimeVerifier(String s) { suffix = s; }
    long scale, value;
    boolean matched;
    String text;

    void trySuffix(String suffix, long v) {
      suffix = suffix.trim();
      if (!text.endsWith(suffix))
        return;
      text = text.substring(0, text.length() - suffix.length());
      scale = v;
      matched = true;
    }

    @Override
    public boolean verify(JComponent input) {
      text = ((JTextField) input).getText().trim();
      String s = S.get(suffix);
      if (text.endsWith(s))
        text = text.substring(0, text.length() - s.length()).trim();
      scale = 1;
      matched = false;
      if (!matched) trySuffix("ns", 1);
      if (!matched) trySuffix("nsec", 1);
      if (!matched) trySuffix(S.fmt("nsFormat",""), 1);
      if (!matched) trySuffix("us", 1000);
      if (!matched) trySuffix("usec", 1000);
      if (!matched) trySuffix(S.fmt("usFormat",""), 1000);
      if (!matched) trySuffix("ms", 1000000);
      if (!matched) trySuffix("msec", 1000000);
      if (!matched) trySuffix(S.fmt("msFormat",""), 1000000);
      if (!matched) trySuffix("s", 1000000000);
      if (!matched) trySuffix("sec", 1000000000);
      if (!matched) trySuffix(S.fmt("sFormat",""), 1000000000);
      if (!matched) scale = 1000000;
      try {
        value = Long.parseLong(text);
        return true;
      } catch (NumberFormatException e) {
        return false;
      }
    }
  }

  // todo: listen to locale here, but need to make LocaleManager hold weak ref
  static class JLabeledComboBox<E> extends JComboBox<E> {
    private static final long serialVersionUID = 1L;
    String labelKey;
    JLabel label = new JLabel();
    JPanel panel = new JPanel();
    JLabeledComboBox(String labelKey, E[] items) {
      super(items);
      setRenderer(new Renderer());
      this.labelKey = labelKey;
      label.setText(S.get(labelKey));
      label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
      panel.add(label);
      panel.add(this);
      panel.setMaximumSize(panel.getPreferredSize());
      panel.setAlignmentX(0.0f);
    }
    void localeChanged() {
      label.setText(S.get(labelKey));
      repaint();
    }
    JPanel getPanel() {
      return panel;
    }
    E getValue() {
      return (E)getSelectedItem();
    }
    String renderAsText(E v) {
      return v.toString();
    }
    String getText() {
      return renderAsText(getValue());
    }
    class Renderer extends DefaultListCellRenderer {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      @Override
      public Component getListCellRendererComponent(JList<?> list,
          Object w, int index, boolean isSelected, boolean cellHasFocus) {
        String s = renderAsText((E)w);
        return super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
      }
    }
  }

  static class TimeSelector extends JLabeledComboBox<Long> {
    private static final long serialVersionUID = 1L;
    String suffix;
    static final Long[] defaultVals = new Long[] {
      1L, 5L, 10L, 50L, 100L, 500L,  // ns
          1000L, 5000L, 10000L, 50000L, 100000L, 500000L, // us
          1000000L, 5000000L, 10000000L, 50000000L, 100000000L, 500000000L, // ms
          1000000000L // s
    };
    TimeSelector(String labelKey, long nsDefault, String suffix) {
      super(labelKey, defaultVals);
      setSelectedItem(nsDefault);
      // setEditor(new TimeEditor(suffix));
      // setEditable(true);
    }
    TimeSelector(String labelKey, long nsDefault) {
      this(labelKey, nsDefault, null);
    }
    @Override
    String renderAsText(Long v) {
      String s = Model.formatDuration(v);
      if (suffix != null)
        s = s + " " + S.get(suffix);
      return s;
    }
  }

  static class TickSelector extends JLabeledComboBox<Integer> {
    private static final long serialVersionUID = 1L;
    static final Integer[] defaultVals = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    TickSelector(String labelKey, int tickDefault) {
      super(labelKey, defaultVals);
      setSelectedItem(tickDefault);
      setEditable(false);
    }
    @Override
    String renderAsText(Integer v) {
      return S.fmt("tickFormat", v);
    }
  }

  @Override
  public String getHelpText() {
    return S.get("optionsHelp");
  }

  @Override
  public String getTitle() {
    return S.get("optionsTab");
  }

  @Override
  public void localeChanged() {
    selectionPanel.setBorder(BorderFactory.createTitledBorder(S.get("selectionLabel")));
    modePanel.setBorder(BorderFactory.createTitledBorder(S.get("modeLabel")));
    historyPanel.setBorder(BorderFactory.createTitledBorder(S.get("historyLabel")));
    stepTime.setText(S.get("stepTime"));

    realTime.setText(S.get("realTime"));
    clockTime.setText(S.get("clockTime"));

    stepFine.setText(S.get("fineDetail"));
    realFine.setText(S.get("fineDetail"));
    clockFine.setText(S.get("fineDetail"));

    unlimited.setText(S.get("historyUnlimited"));
    limitLabel.setText(S.get("historyLimit"));

    stepScale.localeChanged();
    stepGate.localeChanged();
    realScale.localeChanged();
    clockScale.localeChanged();
    clockTicks.localeChanged();
    clockGate.localeChanged();

    updateDescription();
  }

  void updateDescription() {
    limit.setEnabled(!unlimited.isSelected());
    String mode;
    String d;
    if (stepTime.isSelected()) {
      mode = "stepTime";
      boolean fine = stepFine.isSelected();
      stepGate.setEnabled(fine);
      if (fine)
        d = S.fmt("stepFineDescription", stepGate.getText(), stepScale.getText());
      else
        d = S.fmt("stepCoarseDescription", stepScale.getText());

      realFine.setSelected(fine);
      clockFine.setSelected(fine);
      realScale.setSelectedItem(stepScale.getValue());
      clockScale.setSelectedItem(stepScale.getValue());
      clockGate.setSelectedItem(stepGate.getValue());
    } else if (realTime.isSelected()) {
      mode = "realTime";
      boolean fine = realFine.isSelected();
      if (fine)
        d = S.fmt("realFineDescription", realScale.getText());
      else
        d = S.fmt("realCoarseDescription", realScale.getText());
      stepFine.setSelected(fine);
      clockFine.setSelected(fine);
      stepScale.setSelectedItem(realScale.getValue());
      clockScale.setSelectedItem(realScale.getValue());
    } else {
      mode = "clockTime";
      boolean fine = clockFine.isSelected();
      clockGate.setEnabled(fine);
      if (fine)
        d = S.fmt("clockFineDescription", clockGate.getText(),
          clockScale.renderAsText(clockScale.getValue() * clockTicks.getValue()),
          clockScale.getText());
      else
        d = S.fmt("clockCoarseDescription", clockTicks.getText(),
          clockScale.renderAsText(clockScale.getValue() * clockTicks.getValue()),
          clockScale.getText());
      stepFine.setSelected(fine);
      realFine.setSelected(fine);
      stepScale.setSelectedItem(clockScale.getValue());
      realScale.setSelectedItem(clockScale.getValue());
      stepGate.setSelectedItem(clockGate.getValue());
    }
    ((CardLayout)optionsPanel.getLayout()).show(optionsPanel, mode);
    optionsPanel.setBorder(BorderFactory.createTitledBorder(
          S.get("timingLabel") + ": " + S.get(mode)));
    description.setText("<html>"+d+"</html>"); // html to enable line wrapping
  }

  @Override
  public void modelChanged(Model oldModel, Model newModel) {
    stepScale.setSelectedItem(newModel.getTimeScale());
    realScale.setSelectedItem(newModel.getTimeScale());
    clockScale.setSelectedItem(newModel.getTimeScale());
    stepGate.setSelectedItem(newModel.getTimeScale());
    clockGate.setSelectedItem(newModel.getTimeScale());
    clockTicks.setSelectedItem(newModel.getCycleLength());
    stepFine.setSelected(newModel.isFine());
    realFine.setSelected(newModel.isFine());
    clockFine.setSelected(newModel.isFine());
    if (newModel.isStepMode())
      stepTime.setSelected(true);
    else if (newModel.isRealMode())
      realTime.setSelected(true);
    else
      clockTime.setSelected(true);
    updateDescription();
    int n = newModel.getHistoryLimit();
    unlimited.setSelected(n == 0);
    if (n > 0)
      limit.setValue(n);
  }

}
