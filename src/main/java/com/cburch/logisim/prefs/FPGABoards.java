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
import lombok.val;

public class FPGABoards implements ActionListener {

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
      val iter = externalBoards.iterator();
      var removed = false;
      while (iter.hasNext()) {
        val file = iter.next();
        val f = new File(file);
        if (!f.exists() || f.isDirectory()) {
          buildInBoards.RemoveExternalBoard(file);
          removeFromPrefs(file);
          iter.remove();
          removed = true;
        }
      }
      if (removed) {
        fireChange(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, externalBoards.size(), ListDataEvent.INTERVAL_REMOVED));
      }
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
      return (index < getSize()) ? BoardList.getBoardName(externalBoards.get(index)) : null;
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
      for (val listener : myListeners) {
        switch (e.getType()) {
          case ListDataEvent.CONTENTS_CHANGED:
            listener.contentsChanged(e);
            break;
          case ListDataEvent.INTERVAL_ADDED:
            listener.intervalAdded(e);
            break;
          default:
            listener.intervalRemoved(e);
        }
      }
    }
  }

  @SuppressWarnings("serial")
  private static class SortedArrayList extends ArrayList<String> {

    public void insertSorted(String value) {
      add(value);
      Comparable<String> cmp = BoardList.getBoardName(value);
      for (var i = size() - 1; i > 0 && cmp.compareTo(BoardList.getBoardName(get(i - 1))) < 0; i--)
        Collections.swap(this, i, i - 1);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource().equals(addButton)) {
      addBoard(false);
    } else if (e.getSource().equals(removeButton)) {
      val board = boardNamesList.getSelectedValue();
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

  private static final String extBoard = "ExtBoardDescr";
  private static final int maxBoards = 20;
  private final BoardList buildInBoards = new BoardList();
  private JScrollPane boardPane;
  private JList<String> boardNamesList;
  private JButton addButton;
  private JButton removeButton;
  private JComboBox<String> boardSelector;
  private final ExternalBoardModel extBoardModel = new ExternalBoardModel();

  FPGABoards() {
    val prefs = AppPreferences.getPrefs();
    for (var i = 0; i < maxBoards; i++) {
      val encoding = prefs.get(extBoard + i, null);
      if (encoding != null) {
        addExternalBoard(encoding, i, prefs);
      }
    }
    val selectedBoard = AppPreferences.SelectedBoard.get();
    if (!buildInBoards.GetBoardNames().contains(selectedBoard)) {
      AppPreferences.SelectedBoard.set(buildInBoards.GetBoardNames().get(0));
    }
  }

  private boolean addExternalBoard(String filename, int oldindex, Preferences prefs) {
    /* first we check if the file exists */
    val f = new File(filename);
    if (!f.exists() || f.isDirectory()) {
      if (prefs != null) prefs.remove(extBoard + oldindex);
      return false;
    }
    /* we check if the list is full and clean-up */
    if (extBoardModel.nrOfExternalBoards() == maxBoards) return false;
    /* then we check if the file is already in the list */
    if (extBoardModel.contains(filename)) {
      if (prefs != null) prefs.remove(extBoard + oldindex);
      return false;
    }
    extBoardModel.insert(filename);
    buildInBoards.AddExternalBoard(filename);
    val index = extBoardModel.indexOf(filename);
    if ((index != oldindex) && (oldindex != maxBoards)) {
      prefs.remove(extBoard + oldindex);
      prefs.put(extBoard + index, filename);
    } else if (oldindex == maxBoards) {
      rebuildPrefsTree();
    }
    return true;
  }

  public boolean addExternalBoard(String filename) {
    val prefs = AppPreferences.getPrefs();
    return addExternalBoard(filename, maxBoards, prefs);
  }

  public String getBoardFilePath(String boardName) {
    return buildInBoards.GetBoardFilePath(boardName);
  }

  public ArrayList<String> getBoardNames() {
    return buildInBoards.GetBoardNames();
  }

  public String getSelectedBoardFileName() {
    return buildInBoards.GetBoardFilePath(AppPreferences.SelectedBoard.get());
  }

  public JComboBox<String> boardSelector() {
    if (boardSelector == null) {
      boardSelector = new JComboBox<>();
      rebuildBoardSelector(false, null);
    }
    return boardSelector;
  }

  public JPanel addRemovePanel() {
    val panel = new JPanel();
    val nrBoards = extBoardModel.nrOfExternalBoards();
    val thisLayout = new GridBagLayout();
    val c = new GridBagConstraints();
    panel.setLayout(thisLayout);
    panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
    c.gridwidth = 2;
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.CENTER;
    panel.add(new JLabel(S.get("ExternalBoards")), c);
    c.gridy = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    panel.add(new JSeparator(), c);
    c.gridheight = 10;
    c.gridwidth = 1;
    c.gridy = 2;
    boardNamesList = new JList<>(extBoardModel);
    boardNamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    if (boardNamesList.getModel().getSize() != 0) boardNamesList.setSelectedIndex(0);
    boardPane = new JScrollPane(boardNamesList);
    panel.add(boardPane, c);
    c.gridheight = 1;
    c.gridwidth = 1;
    c.gridy = 2;
    c.gridx = 1;
    addButton = new JButton();
    addButton.setText(S.get("AddBoard"));
    addButton.setEnabled(nrBoards < maxBoards);
    addButton.addActionListener(this);
    panel.add(addButton, c);
    c.gridy = 3;
    removeButton = new JButton();
    removeButton.setText(S.get("RemoveBoard"));
    removeButton.setEnabled(nrBoards > 0);
    removeButton.addActionListener(this);
    panel.add(removeButton, c);

    return panel;
  }

  private void updateButtons() {
    val size = extBoardModel.nrOfExternalBoards();
    if (addButton != null) addButton.setEnabled(size < maxBoards);
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
    for (val item : buildInBoards.GetBoardNames()) {
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
    val prefs = AppPreferences.getPrefs();
    for (var i = 0; i < maxBoards; i++) {
      val name = prefs.get(extBoard + i, null);
      if ((name != null) && (name.equals(fname))) prefs.remove(extBoard + i);
    }
  }

  private boolean removeBoard(String name) {
    if (name == null) return false;
    val qualifier = buildInBoards.GetBoardFilePath(name);
    if (extBoardModel.contains(qualifier)) {
      extBoardModel.remove(qualifier);
    } else return false;
    if (!buildInBoards.RemoveExternalBoard(qualifier)) return false;
    removeFromPrefs(qualifier);
    return true;
  }

  private void rebuildPrefsTree() {
    val prefs = AppPreferences.getPrefs();
    for (var i = 0; i < extBoardModel.getSize(); i++) {
      prefs.put(extBoard + i, extBoardModel.get(i));
    }
    for (int i = extBoardModel.getSize(); i < maxBoards; i++) {
      prefs.remove(extBoard + i);
    }
  }

  private boolean addBoard(boolean updateSelection) {
    if (extBoardModel.getSize() >= maxBoards) {
      OptionPane.showMessageDialog(null, S.get("MaxBoardsReached"), S.get("AddExternalBoards"), OptionPane.ERROR_MESSAGE);
      return false;
    }
    var boardFileName = getBoardFile();
    if (boardFileName == null) return false;
    var test = new BoardReaderClass(boardFileName);
    if (test.GetBoardInformation() == null) {
      OptionPane.showMessageDialog(null, S.get("InvalidFileFormat"), S.get("AddExternalBoards"), OptionPane.ERROR_MESSAGE);
      return false;
    }
    if (buildInBoards.GetBoardNames().contains(BoardList.getBoardName(boardFileName))) {
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
    val fc = new JFileChooser(AppPreferences.FPGA_Workspace.get());
    val filter = new FileNameExtensionFilter("Board files", "xml", "xml");
    fc.setFileFilter(filter);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    val test = new File(AppPreferences.FPGA_Workspace.get());
    if (test.exists()) {
      fc.setSelectedFile(test);
    }
    fc.setDialogTitle(S.get("BoardSelection"));
    val retval = fc.showOpenDialog(null);
    if (retval == JFileChooser.APPROVE_OPTION) {
      return fc.getSelectedFile().getPath();
    }
    return null;
  }
}
