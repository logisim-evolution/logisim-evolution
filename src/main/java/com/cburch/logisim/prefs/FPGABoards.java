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
import java.util.Iterator;
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

public class FPGABoards implements ActionListener {

  private class ExternalBoardModel implements ListModel<String> {

    private SortedArrayList ExternalBoards = new SortedArrayList();
    private ArrayList<ListDataListener> MyListeners = new ArrayList<ListDataListener>();

    public boolean contains(String entry) {
      return ExternalBoards.contains(entry);
    }

    public void insert(String entry) {
      ExternalBoards.insertSorted(entry);
      FireChange(new ListDataEvent(this, 0, ExternalBoards.size(), ListDataEvent.INTERVAL_ADDED));
    }

    public int indexOf(String entry) {
      return ExternalBoards.indexOf(entry);
    }

    public void remove(String entry) {
      ExternalBoards.remove(entry);
      FireChange(new ListDataEvent(this, 0, ExternalBoards.size(), ListDataEvent.INTERVAL_REMOVED));
    }

    public int NrOfExternalBoards() {
      Iterator<String> iter = ExternalBoards.iterator();
      boolean removed = false;
      while (iter.hasNext()) {
        String file = iter.next();
        File f = new File(file);
        if (!f.exists() || f.isDirectory()) {
          BuildInBoards.RemoveExternalBoard(file);
          RemoveFromPrefs(file);
          iter.remove();
          removed = true;
        }
      }
      if (removed)
        FireChange(
            new ListDataEvent(this, 0, ExternalBoards.size(), ListDataEvent.INTERVAL_REMOVED));
      return ExternalBoards.size();
    }

    public String get(int index) {
      return ExternalBoards.get(index);
    }

    @Override
    public int getSize() {
      int size = NrOfExternalBoards();
      return size;
    }

    @Override
    public String getElementAt(int index) {
      int size = NrOfExternalBoards();
      return (index < size) ? BoardList.getBoardName(ExternalBoards.get(index)) : null;
    }

    @Override
    public void addListDataListener(ListDataListener l) {
      MyListeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
      MyListeners.remove(l);
    }

    private void FireChange(ListDataEvent e) {
      for (ListDataListener listener : MyListeners) {
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
  private class SortedArrayList extends ArrayList<String> {

    public void insertSorted(String value) {
      add(value);
      Comparable<String> cmp = (Comparable<String>) BoardList.getBoardName(value);
      for (int i = size() - 1; i > 0 && cmp.compareTo(BoardList.getBoardName(get(i - 1))) < 0; i--)
        Collections.swap(this, i, i - 1);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource().equals(AddButton)) {
      AddBoard(false);
    } else if (e.getSource().equals(RemoveButton)) {
      String Board = BoardNamesList.getSelectedValue();
      if (RemoveBoard(Board)) {
      	if (AppPreferences.SelectedBoard.get().equals(Board)) {
      		if (BoardSelector != null && BoardSelector.getItemCount() >= 2)
              AppPreferences.SelectedBoard.set(BoardSelector.getItemAt(1));
      		else {
      	      BoardSelector = new JComboBox<String>();
      	      RebuildBoardSelector(false, null);
              AppPreferences.SelectedBoard.set(BoardSelector.getItemAt(1));
      		}
        }
        if ((BoardNamesList.getSelectedIndex() >= BoardNamesList.getModel().getSize())
            && (BoardNamesList.getModel().getSize() > 0)) {
          BoardNamesList.setSelectedIndex(BoardNamesList.getModel().getSize() - 1);
        }
        UpdateButtons();
        BoardNamesList.repaint();
        RebuildBoardSelector(false, null);
      }
    } else if (e.getSource().equals(BoardSelector)) {
      if (BoardSelector.getSelectedItem() == null) return;
      if (BoardSelector.getSelectedItem().equals("Other")) {
        if (!AddBoard(true)) {
          RebuildBoardSelector(false, null);
        }
      } else {
        AppPreferences.SelectedBoard.set(BoardSelector.getSelectedItem().toString());
      }
    }
  }

  private static final String ExtBoard = "ExtBoardDescr";
  private static final int MaxBoards = 20;
  private BoardList BuildInBoards = new BoardList();
  private JScrollPane BoardPane;
  private JList<String> BoardNamesList;
  private JButton AddButton;
  private JButton RemoveButton;
  private JComboBox<String> BoardSelector;
  private ExternalBoardModel ExtBoardModel = new ExternalBoardModel();

  FPGABoards() {
    Preferences prefs = AppPreferences.getPrefs();
    for (int i = 0; i < MaxBoards; i++) {
      String encoding = prefs.get(ExtBoard + i, null);
      if (encoding != null) {
        AddExternalBoard(encoding, i, prefs);
      }
    }
    String SelectedBoard = AppPreferences.SelectedBoard.get();
    if (!BuildInBoards.GetBoardNames().contains(SelectedBoard)) {
      AppPreferences.SelectedBoard.set(BuildInBoards.GetBoardNames().get(0));
    }
  }

  private boolean AddExternalBoard(String filename, int oldindex, Preferences prefs) {
    /* first we check if the file exists */
    File f = new File(filename);
    if (!f.exists() || f.isDirectory()) {
      if (prefs != null) {
        prefs.remove(ExtBoard + oldindex);
      }
      return false;
    }
    /* we check if the list is full and clean-up */
    if (ExtBoardModel.NrOfExternalBoards() == MaxBoards) return false;
    /* then we check if the file is already in the list */
    if (ExtBoardModel.contains(filename)) {
      if (prefs != null) prefs.remove(ExtBoard + oldindex);
      return false;
    }
    ExtBoardModel.insert(filename);
    BuildInBoards.AddExternalBoard(filename);
    int index = ExtBoardModel.indexOf(filename);
    if ((index != oldindex) && (oldindex != MaxBoards)) {
      prefs.remove(ExtBoard + oldindex);
      prefs.put(ExtBoard + index, filename);
    } else if (oldindex == MaxBoards) {
      RebuildPrefsTree();
    }
    return true;
  }

  public boolean AddExternalBoard(String filename) {
    Preferences prefs = AppPreferences.getPrefs();
    return AddExternalBoard(filename, MaxBoards, prefs);
  }

  public String GetBoardFilePath(String BoardName) {
    return BuildInBoards.GetBoardFilePath(BoardName);
  }

  public ArrayList<String> GetBoardNames() {
    return BuildInBoards.GetBoardNames();
  }

  public String GetSelectedBoardFileName() {
    String SelectedBoardName = AppPreferences.SelectedBoard.get();
    return BuildInBoards.GetBoardFilePath(SelectedBoardName);
  }

  public JComboBox<String> BoardSelector() {
    if (BoardSelector == null) {
      BoardSelector = new JComboBox<String>();
      RebuildBoardSelector(false, null);
    }
    return BoardSelector;
  }

  public JPanel AddRemovePanel() {
    JPanel panel = new JPanel();
    int NrBoards = ExtBoardModel.NrOfExternalBoards();
    GridBagLayout thisLayout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
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
    BoardNamesList = new JList<String>(ExtBoardModel);
    BoardNamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    if (BoardNamesList.getModel().getSize() != 0) BoardNamesList.setSelectedIndex(0);
    BoardPane = new JScrollPane(BoardNamesList);
    panel.add(BoardPane, c);
    c.gridheight = 1;
    c.gridwidth = 1;
    c.gridy = 2;
    c.gridx = 1;
    AddButton = new JButton();
    AddButton.setText(S.get("AddBoard"));
    AddButton.setEnabled(NrBoards < MaxBoards);
    AddButton.addActionListener(this);
    panel.add(AddButton, c);
    c.gridy = 3;
    RemoveButton = new JButton();
    RemoveButton.setText(S.get("RemoveBoard"));
    RemoveButton.setEnabled(NrBoards > 0);
    RemoveButton.addActionListener(this);
    panel.add(RemoveButton, c);

    return panel;
  }

  private void UpdateButtons() {
    int size = ExtBoardModel.NrOfExternalBoards();
    if (AddButton != null) AddButton.setEnabled(size < MaxBoards);
    if (RemoveButton != null) RemoveButton.setEnabled(size > 0);
  }

  private void RebuildBoardSelector(boolean update, String Board) {
    if (BoardSelector == null) return;
    BoardSelector.removeActionListener(this);
    BoardSelector.removeAllItems();
    BoardSelector.addItem("Other");
    int index = 1;
    boolean found = false;
    ;
    if (update) AppPreferences.SelectedBoard.set(Board);
    for (String item : BuildInBoards.GetBoardNames()) {
      BoardSelector.addItem(item);
      if (item.equals(AppPreferences.SelectedBoard.get())) {
        BoardSelector.setSelectedIndex(index);
        found = true;
      }
      index++;
    }
    if (!found) {
      AppPreferences.SelectedBoard.set(BoardSelector.getItemAt(1));
      BoardSelector.setSelectedIndex(1);
    }
    BoardSelector.repaint();
    BoardSelector.addActionListener(this);
  }

  private void RemoveFromPrefs(String fname) {
    Preferences prefs = AppPreferences.getPrefs();
    for (int i = 0; i < MaxBoards; i++) {
      String name = prefs.get(ExtBoard + i, null);
      if ((name != null) && (name.equals(fname))) prefs.remove(ExtBoard + i);
    }
  }

  private boolean RemoveBoard(String name) {
    if (name == null) return false;
    String qualifier = BuildInBoards.GetBoardFilePath(name);
    if (ExtBoardModel.contains(qualifier)) {
      ExtBoardModel.remove(qualifier);
    } else return false;
    if (!BuildInBoards.RemoveExternalBoard(qualifier)) return false;
    RemoveFromPrefs(qualifier);
    return true;
  }

  private void RebuildPrefsTree() {
    Preferences prefs = AppPreferences.getPrefs();
    for (int i = 0; i < ExtBoardModel.getSize(); i++) {
      prefs.put(ExtBoard + i, ExtBoardModel.get(i));
    }
    for (int i = ExtBoardModel.getSize(); i < MaxBoards; i++) {
      prefs.remove(ExtBoard + i);
    }
  }

  private boolean AddBoard(boolean UpdateSelection) {
    if (ExtBoardModel.getSize() >= MaxBoards) {
      OptionPane.showMessageDialog(
          null, S.get("MaxBoardsReached"), S.get("AddExternalBoards"), OptionPane.ERROR_MESSAGE);
      return false;
    }
    String BoardFileName = GetBoardFile();
    if (BoardFileName == null) return false;
    BoardReaderClass test = new BoardReaderClass(BoardFileName);
    if (test.GetBoardInformation() == null) {
      OptionPane.showMessageDialog(
          null, S.get("InvalidFileFormat"), S.get("AddExternalBoards"), OptionPane.ERROR_MESSAGE);
      return false;
    }
    if (BuildInBoards.GetBoardNames().contains(BoardList.getBoardName(BoardFileName))) {
      OptionPane.showMessageDialog(
          null,
          S.get("BoardPreset") + "\"" + BoardList.getBoardName(BoardFileName) + "\"",
          S.get("AddExternalBoards"),
          OptionPane.ERROR_MESSAGE);
      return false;
    }
    if (!AddExternalBoard(BoardFileName)) return false;
    UpdateButtons();
    if (BoardNamesList != null) {
      BoardNamesList.setSelectedIndex(ExtBoardModel.indexOf(BoardFileName));
    }
    RebuildBoardSelector(UpdateSelection, BoardList.getBoardName(BoardFileName));
    return true;
  }

  private String GetBoardFile() {
    JFileChooser fc = new JFileChooser(AppPreferences.FPGA_Workspace.get());
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Board files", "xml", "xml");
    fc.setFileFilter(filter);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    File test = new File(AppPreferences.FPGA_Workspace.get());
    if (test.exists()) {
      fc.setSelectedFile(test);
    }
    fc.setDialogTitle(S.get("BoardSelection"));
    int retval = fc.showOpenDialog(null);
    if (retval == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      return file.getPath();
    } else return null;
  }
}
