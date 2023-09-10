/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import static com.cburch.logisim.proj.Strings.S;

import com.cburch.logisim.fpga.file.BoardReaderClass;
import com.cburch.logisim.fpga.settings.BoardList;
import com.cburch.logisim.gui.generic.OptionPane;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FpgaBoards implements ActionListener {

  private class ExternalBoardModel implements ListModel<String> {

    private final SortedArrayList externalBoards = new SortedArrayList();
    private final ArrayList<ListDataListener> myListeners = new ArrayList<>();

    public boolean contains(String entry) {
      return externalBoards.contains(entry);
    }

    public void insert(String entry) {
      externalBoards.insertSorted(entry);
      fireChange(
          new ListDataEvent(
              this,
              ListDataEvent.CONTENTS_CHANGED,
              externalBoards.size(),
              ListDataEvent.INTERVAL_ADDED));
    }

    public int indexOf(String entry) {
      return externalBoards.indexOf(entry);
    }

    public void remove(String entry) {
      externalBoards.remove(entry);
      fireChange(
          new ListDataEvent(
              this,
              ListDataEvent.CONTENTS_CHANGED,
              externalBoards.size(),
              ListDataEvent.INTERVAL_REMOVED));
    }

    public int nrOfExternalBoards() {
      final var iter = externalBoards.iterator();
      var removed = false;
      while (iter.hasNext()) {
        final var file = iter.next();
        final var f = new File(file);
        if (!f.exists() || f.isDirectory()) {
          buildInBoards.removeExternalBoard(file);
          removeFromPrefs(file);
          iter.remove();
          removed = true;
        }
      }
      if (removed)
        fireChange(
            new ListDataEvent(
                this,
                ListDataEvent.CONTENTS_CHANGED,
                externalBoards.size(),
                ListDataEvent.INTERVAL_REMOVED));
      return externalBoards.size();
    }

    public String get(int index) {
      return externalBoards.get(index);
    }

    @Override
    public int getSize() {
      return nrOfExternalBoards();
    }

    @Override
    public String getElementAt(int index) {
      final var size = nrOfExternalBoards();
      return (index < size) ? BoardList.getBoardName(externalBoards.get(index)) : null;
    }

    @Override
    public void addListDataListener(ListDataListener l) {
      myListeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
      myListeners.remove(l);
    }

    private void fireChange(ListDataEvent e) {
      for (final var listener : myListeners) {
        switch (e.getType()) {
          case ListDataEvent.CONTENTS_CHANGED -> listener.contentsChanged(e);
          case ListDataEvent.INTERVAL_ADDED -> listener.intervalAdded(e);
          default -> listener.intervalRemoved(e);
        }
      }
    }
  }

  @SuppressWarnings("serial")
  private static class SortedArrayList extends ArrayList<String> {

    public void insertSorted(String value) {
      add(value);
      final var cmp = BoardList.getBoardName(value);
      for (var i = size() - 1; i > 0 && cmp.compareTo(BoardList.getBoardName(get(i - 1))) < 0; i--)
        Collections.swap(this, i, i - 1);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource().equals(addButton)) {
      addBoard(false);
    } else if (e.getSource().equals(removeButton)) {
      final var board = boardNamesList.getSelectedValue();
      if (removeBoard(board)) {
        if (AppPreferences.SelectedBoard.get().equals(board)) {
          if (boardSelector != null && boardSelector.getItemCount() >= 2)
            AppPreferences.SelectedBoard.set(boardSelector.getItemAt(1));
          else {
            boardSelector = new JComboBox<>();
            rebuildBoardSelector(false, null);
            AppPreferences.SelectedBoard.set(boardSelector.getItemAt(1));
          }
        }
        if ((boardNamesList.getSelectedIndex() >= boardNamesList.getModel().getSize())
            && (boardNamesList.getModel().getSize() > 0)) {
          boardNamesList.setSelectedIndex(boardNamesList.getModel().getSize() - 1);
        }
        updateButtons();
        boardNamesList.repaint();
        rebuildBoardSelector(false, null);
      }
    } else if (e.getSource().equals(boardSelector)) {
      if (boardSelector.getSelectedItem() == null) return;
      if (boardSelector.getSelectedItem().equals("Other")) {
        if (!addBoard(true)) rebuildBoardSelector(false, null);
      } else {
        AppPreferences.SelectedBoard.set(boardSelector.getSelectedItem().toString());
      }
    }
  }

  private static final String ExtBoard = "ExtBoardDescr";
  private static final int MaxBoards = 20;
  private final BoardList buildInBoards = new BoardList();
  private JScrollPane boardPane;
  private JList<String> boardNamesList;
  private JButton addButton;
  private JLabel extBoardPanelTitl;
  private JButton removeButton;
  private JComboBox<String> boardSelector;
  private final ExternalBoardModel extBoardModel = new ExternalBoardModel();

  public FpgaBoards() {
    final var prefs = AppPreferences.getPrefs();
    for (var i = 0; i < MaxBoards; i++) {
      final var encoding = prefs.get(ExtBoard + i, null);
      if (encoding != null) addExternalBoard(encoding, i, prefs);
    }
    final var selectedBoard = AppPreferences.SelectedBoard.get();
    if (!buildInBoards.getBoardNames().contains(selectedBoard)) {
      AppPreferences.SelectedBoard.set(buildInBoards.getBoardNames().get(0));
    }
  }

  private boolean addExternalBoard(String filename, int oldindex, Preferences prefs) {
    /* first we check if the file exists */
    final var f = new File(filename);
    if (!f.exists() || f.isDirectory()) {
      if (prefs != null) prefs.remove(ExtBoard + oldindex);
      return false;
    }
    /* we check if the list is full and clean-up */
    if (extBoardModel.nrOfExternalBoards() == MaxBoards) return false;
    /* then we check if the file is already in the list */
    if (extBoardModel.contains(filename)) {
      if (prefs != null) prefs.remove(ExtBoard + oldindex);
      return false;
    }
    extBoardModel.insert(filename);
    buildInBoards.addExternalBoard(filename);
    final var index = extBoardModel.indexOf(filename);
    if ((index != oldindex) && (oldindex != MaxBoards)) {
      prefs.remove(ExtBoard + oldindex);
      prefs.put(ExtBoard + index, filename);
    } else if (oldindex == MaxBoards) {
      rebuildPrefsTree();
    }
    return true;
  }

  public boolean addExternalBoard(String filename) {
    final var prefs = AppPreferences.getPrefs();
    return addExternalBoard(filename, MaxBoards, prefs);
  }

  public String getBoardFilePath(String boardName) {
    return buildInBoards.getBoardFilePath(boardName);
  }

  public List<String> getBoardNames() {
    return buildInBoards.getBoardNames();
  }

  public String getSelectedBoardFileName() {
    return buildInBoards.getBoardFilePath(AppPreferences.SelectedBoard.get());
  }

  public JComboBox<String> boardSelector() {
    if (boardSelector == null) {
      boardSelector = new JComboBox<>();
      rebuildBoardSelector(false, null);
    }
    return boardSelector;
  }

  public JPanel addRemovePanel() {
    final var panel = new JPanel();
    final int nrBoards = extBoardModel.nrOfExternalBoards();
    final var gbc = new GridBagConstraints();
    panel.setLayout(new GridBagLayout());
    panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.CENTER;
    extBoardPanelTitl = new JLabel(S.get("ExternalBoards"));
    panel.add(extBoardPanelTitl, gbc);
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(new JSeparator(), gbc);
    gbc.gridheight = 10;
    gbc.gridwidth = 1;
    gbc.gridy = 2;
    boardNamesList = new JList<>(extBoardModel);
    boardNamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    if (boardNamesList.getModel().getSize() != 0) boardNamesList.setSelectedIndex(0);
    boardPane = new JScrollPane(boardNamesList);
    panel.add(boardPane, gbc);
    gbc.gridheight = 1;
    gbc.gridwidth = 1;
    gbc.gridy = 2;
    gbc.gridx = 1;
    addButton = new JButton();
    addButton.setText(S.get("AddBoard"));
    addButton.setEnabled(nrBoards < MaxBoards);
    addButton.addActionListener(this);
    panel.add(addButton, gbc);
    gbc.gridy = 3;
    removeButton = new JButton();
    removeButton.setText(S.get("RemoveBoard"));
    removeButton.setEnabled(nrBoards > 0);
    removeButton.addActionListener(this);
    panel.add(removeButton, gbc);

    return panel;
  }
  
  public void localeChanged() {
    addButton.setText(S.get("AddBoard"));
    extBoardPanelTitl.setText(S.get("ExternalBoards"));
    removeButton.setText(S.get("RemoveBoard"));
  }
  
  private void updateButtons() {
    final var size = extBoardModel.nrOfExternalBoards();
    if (addButton != null) addButton.setEnabled(size < MaxBoards);
    if (removeButton != null) removeButton.setEnabled(size > 0);
  }

  private void rebuildBoardSelector(boolean update, String board) {
    if (boardSelector == null) return;
    boardSelector.removeActionListener(this);
    boardSelector.removeAllItems();
    boardSelector.addItem("Other");
    var index = 1;
    var found = false;
    if (update) AppPreferences.SelectedBoard.set(board);
    for (String item : buildInBoards.getBoardNames()) {
      boardSelector.addItem(item);
      if (item.equals(AppPreferences.SelectedBoard.get())) {
        boardSelector.setSelectedIndex(index);
        found = true;
      }
      index++;
    }
    if (!found) {
      AppPreferences.SelectedBoard.set(boardSelector.getItemAt(1));
      boardSelector.setSelectedIndex(1);
    }
    boardSelector.repaint();
    boardSelector.addActionListener(this);
  }

  private void removeFromPrefs(String fname) {
    final var prefs = AppPreferences.getPrefs();
    for (var i = 0; i < MaxBoards; i++) {
      final var name = prefs.get(ExtBoard + i, null);
      if ((name != null) && (name.equals(fname))) prefs.remove(ExtBoard + i);
    }
  }

  private boolean removeBoard(String name) {
    if (name == null) return false;
    final var qualifier = buildInBoards.getBoardFilePath(name);
    if (extBoardModel.contains(qualifier)) {
      extBoardModel.remove(qualifier);
    } else return false;
    if (!buildInBoards.removeExternalBoard(qualifier)) return false;
    removeFromPrefs(qualifier);
    return true;
  }

  private void rebuildPrefsTree() {
    final var prefs = AppPreferences.getPrefs();
    for (var i = 0; i < extBoardModel.getSize(); i++) {
      prefs.put(ExtBoard + i, extBoardModel.get(i));
    }
    for (int i = extBoardModel.getSize(); i < MaxBoards; i++) {
      prefs.remove(ExtBoard + i);
    }
  }

  private boolean addBoard(boolean updateSelection) {
    if (extBoardModel.getSize() >= MaxBoards) {
      OptionPane.showMessageDialog(
          null, S.get("MaxBoardsReached"), S.get("AddExternalBoards"), OptionPane.ERROR_MESSAGE);
      return false;
    }
    var boardFileName = getBoardFile();
    if (boardFileName == null) return false;
    var test = new BoardReaderClass(boardFileName);
    if (test.getBoardInformation() == null) {
      OptionPane.showMessageDialog(
          null, S.get("InvalidFileFormat"), S.get("AddExternalBoards"), OptionPane.ERROR_MESSAGE);
      return false;
    }
    if (buildInBoards.getBoardNames().contains(BoardList.getBoardName(boardFileName))) {
      OptionPane.showMessageDialog(
          null,
          S.get("BoardPreset") + "\"" + BoardList.getBoardName(boardFileName) + "\"",
          S.get("AddExternalBoards"),
          OptionPane.ERROR_MESSAGE);
      return false;
    }
    if (!addExternalBoard(boardFileName)) return false;
    updateButtons();
    if (boardNamesList != null) {
      boardNamesList.setSelectedIndex(extBoardModel.indexOf(boardFileName));
    }
    rebuildBoardSelector(updateSelection, BoardList.getBoardName(boardFileName));
    return true;
  }

  private String getBoardFile() {
    final var fc = new JFileChooser(AppPreferences.FPGA_Workspace.get());
    final var filter = new FileNameExtensionFilter("Board files", "xml", "xml");
    fc.setFileFilter(filter);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    final var test = new File(AppPreferences.FPGA_Workspace.get());
    if (test.exists()) {
      fc.setSelectedFile(test);
    }
    fc.setDialogTitle(S.get("BoardSelection"));
    final var retval = fc.showOpenDialog(null);
    if (retval == JFileChooser.APPROVE_OPTION) {
      final var file = fc.getSelectedFile();
      return file.getPath();
    } else return null;
  }
}
