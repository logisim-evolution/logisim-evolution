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

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.analyze.model.VariableListEvent;
import com.cburch.logisim.analyze.model.VariableListListener;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.util.StringUtil;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class VariableTab extends AnalyzerTab implements TabInterface {
  private class MyListener implements ActionListener, DocumentListener, ListSelectionListener {
    public void actionPerformed(ActionEvent event) {
      String value = field.getText().trim();
      int end = (value.contains("[")) ? value.indexOf('[') : value.length();
      String name = value.substring(0, end);
      int NrOfBits = getSpecifiedNrOfBits();
      int w = NrOfBits > 0 ? NrOfBits : (Integer) width.getSelectedItem();
      Var newVar = new Var(name, w);
      Var oldVar = list.getSelectedValue();
      Object src = event.getSource();
      if ((src == add || src == field) && add.isEnabled()) {
        if (!name.equals("")) {
          data.add(newVar);
          list.setSelectedValue(newVar, true);
          width.setSelectedItem(1);
          field.setText("");
          field.grabFocus();
        }
      } else if (src == rename && rename.isEnabled()) {
        if (oldVar != null && !name.equals("")) {
          if (contains(name) && !oldVar.name.toLowerCase().equals(name.toLowerCase())) {
            for (Var item : data.vars) {
              if (item.name.toLowerCase().equals(name.toLowerCase())) oldVar = item;
            }
          }
          data.replace(oldVar, newVar);
          list.setSelectedValue(newVar, true);
          width.setSelectedItem(1);
          field.setText("");
          field.grabFocus();
        }
      } else if (src == remove && oldVar != null) {
        data.remove(oldVar);
      } else if (src == moveUp && oldVar != null) {
        data.move(oldVar, -1);
        list.setSelectedValue(oldVar, true);
      } else if (src == moveDown && oldVar != null) {
        data.move(oldVar, 1);
        list.setSelectedValue(oldVar, true);
      } else if (src == width) {
        computeEnabled();
      }
    }

    public void changedUpdate(DocumentEvent event) {
      computeEnabled();
    }

    public void insertUpdate(DocumentEvent event) {
      computeEnabled();
    }

    public void removeUpdate(DocumentEvent event) {
      computeEnabled();
    }

    public void valueChanged(ListSelectionEvent event) {
      Var var = list.getSelectedValue();
      if (var != null) {
        String index = (var.width > 1) ? "[" + (var.width - 1) + "..0]" : "";
        field.setText(var.name + index);
        width.setSelectedItem(var.width);
      }
      computeEnabled();
    }
  }

  @SuppressWarnings("rawtypes")
  private static class VariableListModel extends AbstractListModel implements VariableListListener {
    private static final long serialVersionUID = 1L;
    private VariableList list;
    private Var[] listCopy;

    public VariableListModel(VariableList list) {
      this.list = list;
      updateCopy();
      list.addVariableListListener(this);
    }

    public Object getElementAt(int i) {
      return i >= 0 && i < listCopy.length ? listCopy[i] : null;
    }

    public int getSize() {
      return listCopy.length;
    }

    public void listChanged(VariableListEvent event) {
      int oldSize = listCopy.length;
      updateCopy();
      Integer idx = event.getIndex();
      switch (event.getType()) {
        case VariableListEvent.ALL_REPLACED:
          fireContentsChanged(this, 0, oldSize);
          return;
        case VariableListEvent.ADD:
          fireIntervalAdded(this, idx, idx);
          return;
        case VariableListEvent.REMOVE:
          fireIntervalRemoved(this, idx, idx);
          return;
        case VariableListEvent.MOVE:
          fireContentsChanged(this, 0, getSize());
          return;
        case VariableListEvent.REPLACE:
          fireContentsChanged(this, idx, idx);
          return;
      }
    }

    private void update() {
      int oldSize = listCopy.length;
      updateCopy();
      fireContentsChanged(this, 0, oldSize);
    }

    private void updateCopy() {
      listCopy = list.vars.toArray(new Var[list.vars.size()]);
    }
  }

  private static final long serialVersionUID = 1L;

  private VariableList data;
  private MyListener myListener = new MyListener();
  private VariableTab Othertab = null;
  private String OtherId;

  private JList<Var> list = new JList<Var>();
  private JTextField field = new JTextField();
  private JComboBox<Integer> width;
  private JButton remove = new JButton();
  private JButton moveUp = new JButton();
  private JButton moveDown = new JButton();
  private JButton add = new JButton();
  private JButton rename = new JButton();
  private JLabel error = new JLabel(" ");

  @SuppressWarnings("unchecked")
  VariableTab(VariableList data, int maxwidth) {
    this.data = data;

    Integer widths[] = new Integer[maxwidth > 32 ? 32 : maxwidth];
    for (int i = 0; i < widths.length; i++) widths[i] = i + 1;
    width = new JComboBox<Integer>(widths);
    list.setModel(new VariableListModel(data));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(myListener);
    remove.addActionListener(myListener);
    moveUp.addActionListener(myListener);
    moveDown.addActionListener(myListener);
    add.addActionListener(myListener);
    rename.addActionListener(myListener);
    field.addActionListener(myListener);
    field.getDocument().addDocumentListener(myListener);
    width.addActionListener(myListener);

    JScrollPane listPane =
        new JScrollPane(
            list,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    listPane.setPreferredSize(new Dimension(100, 100));

    JPanel topPanel = new JPanel(new GridLayout(3, 1));
    topPanel.add(remove);
    topPanel.add(moveUp);
    topPanel.add(moveDown);

    JPanel fieldPanel = new JPanel();
    fieldPanel.add(rename);
    fieldPanel.add(add);

    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    setLayout(gb);
    Insets oldInsets = gc.insets;

    gc.insets = new Insets(10, 10, 0, 0);
    gc.fill = GridBagConstraints.BOTH;
    gc.weightx = 1.0;
    gb.setConstraints(listPane, gc);
    add(listPane);

    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.PAGE_START;
    gc.weightx = 0.0;
    gb.setConstraints(topPanel, gc);
    add(topPanel);

    gc.insets = new Insets(10, 10, 0, 10);
    gc.weightx = 1.0;
    gc.gridx = 0;
    gc.gridy = GridBagConstraints.RELATIVE;
    gc.fill = GridBagConstraints.BOTH;
    gb.setConstraints(field, gc);
    add(field);

    JPanel wPanel = new JPanel();
    wPanel.add(new JLabel("width: "));
    wPanel.add(width);
    gc.insets = new Insets(10, 10, 0, 10);
    gc.weightx = 0.0;
    gc.gridx = 1;
    gc.fill = GridBagConstraints.NONE;
    gb.setConstraints(wPanel, gc);
    add(wPanel);

    gc.insets = oldInsets;
    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.LINE_END;
    gb.setConstraints(fieldPanel, gc);
    add(fieldPanel);

    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.gridx = 0;
    gc.gridwidth = 2;
    gb.setConstraints(error, gc);
    add(error);

    if (!data.vars.isEmpty()) list.setSelectedValue(data.vars.get(0), true);
    computeEnabled();
  }

  public void SetCompanion(VariableTab tab, String id) {
    Othertab = tab;
    OtherId = id;
  }

  private void computeEnabled() {
    int index = list.getSelectedIndex();
    int max = list.getModel().getSize();
    boolean selected = index >= 0 && index < max;
    remove.setEnabled(selected);
    moveUp.setEnabled(selected && index > 0);
    moveDown.setEnabled(selected && index < max);

    int err = validateInput();
    int specbit = getSpecifiedNrOfBits();
    if (specbit == 0 || specbit > 32) specbit = -1;
    int w = (specbit < 0) ? (Integer) width.getSelectedItem() : specbit;
    add.setEnabled(err == OK && data.bits.size() + w <= data.getMaximumSize());
    rename.setEnabled((err == OK || err == RESIZED) && selected);
  }

  public void copy() {
    field.requestFocus();
    field.copy();
  }

  public void delete() {
    field.requestFocus();
    field.replaceSelection("");
  }

  @Override
  void localeChanged() {
    remove.setText(S.get("variableRemoveButton"));
    moveUp.setText(S.get("variableMoveUpButton"));
    moveDown.setText(S.get("variableMoveDownButton"));
    add.setText(S.get("variableAddButton"));
    rename.setText(S.get("variableRenameButton"));
    validateInput();
  }

  public void paste() {
    field.requestFocus();
    field.paste();
  }

  void registerDefaultButtons(DefaultRegistry registry) {
    registry.registerDefaultButton(field, add);
  }

  public void selectAll() {
    field.requestFocus();
    field.selectAll();
  }

  @Override
  void updateTab() {
    VariableListModel model = (VariableListModel) list.getModel();
    model.update();
  }

  public boolean contains(String label) {
    for (int i = 0, n = data.vars.size(); i < n; i++) {
      String other = data.vars.get(i).name;
      if (label.toLowerCase().equals(other.toLowerCase())) return true;
    }
    return false;
  }

  public static final int OK = 0;
  public static final int EMPTY = 1;
  public static final int UNCHANGED = 2;
  public static final int RESIZED = 3;
  public static final int BAD_NAME = 4;
  public static final int DUP_NAME = 5;
  public static final int NO_START_PAR = -1;
  public static final int NO_VALID_MSB_INDEX = -2;
  public static final int NO_VALID_INDEX_SEP = -3;
  public static final int NO_VALID_LSB_INDEX = -4;
  public static final int LSB_BIGGER_MSB = -5;
  public static final int NO_FINAL_PAR = -6;
  public static final int INVALID_CHARS = -7;

  private int getSpecifiedNrOfBits() {
    String text = field.getText().trim();
    String bits;
    if (text.contains("[") && text.endsWith("]")) {
      int idx = text.indexOf('[');
      bits = text.substring(idx, text.length());
    } else return -1;
    return checkindex(bits);
  }

  public static int checkindex(String index) {
    int length = index.length();
    int pos = 0;
    if (length < 2) return 0;
    if (index.charAt(pos++) != '[') return NO_START_PAR;
    while ((pos < length) && ("0123456789".indexOf(index.charAt(pos)) >= 0)) pos++;
    if (pos == 1) return NO_VALID_MSB_INDEX;
    int MSBIndex = Integer.parseInt(index.substring(1, pos));
    if (pos >= length) return 0;
    if (index.charAt(pos) == ']') {
      pos++;
      if (pos != length) return INVALID_CHARS;
      else return MSBIndex;
    }
    if (pos >= length - 2) return 0;
    if (!index.substring(pos, pos + 2).equals("..")) return NO_VALID_INDEX_SEP;
    pos += 2;
    int curpos = pos;
    while ((pos < length) && ("0123456789".indexOf(index.charAt(pos)) >= 0)) pos++;
    if (pos == curpos) return NO_VALID_LSB_INDEX;
    int LSBIndex = Integer.parseInt(index.substring(curpos, pos));
    if (LSBIndex > MSBIndex) return LSB_BIGGER_MSB;
    if (pos >= length) return 0;
    if (index.charAt(pos++) != ']') return NO_FINAL_PAR;
    if (pos != length) return INVALID_CHARS;
    return MSBIndex - LSBIndex + 1;
  }

  private int validateInput() {
    Var oldVar = list.getSelectedValue();
    String text = field.getText().trim();
    String name = "";
    String index = "";
    if (text.contains("[")) {
      int idx = text.indexOf('[');
      name = text.substring(0, idx);
      index = text.substring(idx, text.length());
    } else name = text;

    int w = (Integer) width.getSelectedItem();
    int err = OK;
    if (text.length() == 0) {
      err = EMPTY;
    } else if (!Character.isJavaIdentifierStart(name.charAt(0))) {
      error.setText(S.get("variableStartError"));
      err = BAD_NAME;
    } else {
      for (int i = 1; i < name.length() && err == OK; i++) {
        char c = text.charAt(i);
        if (!Character.isJavaIdentifierPart(c)) {
          error.setText(StringUtil.format(S.get("variablePartError"), "" + c));
          err = BAD_NAME;
        }
      }
    }
    if (err == OK) {
      if (!CorrectLabel.IsCorrectLabel(name)) {
        err = BAD_NAME;
        if (CorrectLabel.IsKeyword(name, false)) {
          error.setText(S.get("HdlKeyword"));
        } else {
          String wrong = CorrectLabel.FirstInvalidCharacter(name);
          error.setText(StringUtil.format(S.get("InvalidCharacter"), wrong));
        }
      }
    }
    if (err == OK) {
      if (contains(name)) err = RESIZED;
    }
    if ((err == OK) && (Othertab != null)) {
      if (Othertab.contains(name)) {
        error.setText(StringUtil.format(S.get("variableDuplicateError1"), OtherId));
        err = DUP_NAME;
      }
    }
    int NrOfBits = checkindex(index);
    if (NrOfBits < 0) {
      String ErrorText = null;
      if (NrOfBits == NO_START_PAR) ErrorText = S.get("variableRangeStartPar");
      else if (NrOfBits == NO_VALID_MSB_INDEX) ErrorText = S.get("variableRangeMSBWrong");
      else if (NrOfBits == NO_VALID_INDEX_SEP) ErrorText = S.get("variableRangeWrongSep");
      else if (NrOfBits == NO_VALID_LSB_INDEX) ErrorText = S.get("variableRangeWrongSep");
      else if (NrOfBits == LSB_BIGGER_MSB) ErrorText = S.get("variableRangeWrongLB");
      else if (NrOfBits == NO_FINAL_PAR) ErrorText = S.get("variableRangeFinalPar");
      else if (NrOfBits == INVALID_CHARS) ErrorText = S.get("variableRangeInvalChar");
      if (ErrorText != null) {
        error.setText(ErrorText);
        err = NrOfBits;
      }
    } else if (NrOfBits > 0 && NrOfBits != w) {
      w = NrOfBits;
    }

    if (err == OK && oldVar != null) {
      if (oldVar.name.equals(name) && oldVar.width == w) err = UNCHANGED;
      else if (oldVar.name.equals(name)) err = RESIZED;
    }
    if (err == OK) {
      for (int i = 0, n = data.vars.size(); i < n && err == OK; i++) {
        Var other = data.vars.get(i);
        if (other != oldVar && name.equals(other.name)) {
          error.setText(S.get("variableDuplicateError"));
          err = DUP_NAME;
        }
      }
    }
    if (err == OK || err == EMPTY) {
      if (data.bits.size() + w > data.getMaximumSize()) {
        error.setText(StringUtil.format(S.get("variableMaximumError"), "" + data.getMaximumSize()));
      } else {
        error.setText(" ");
      }
    }
    return err;
  }
}
