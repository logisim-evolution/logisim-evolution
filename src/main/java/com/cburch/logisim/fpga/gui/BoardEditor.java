/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.contracts.BaseComponentListenerContract;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.BoardManipulatorListener;
import com.cburch.logisim.fpga.data.IoComponentsInformation;
import com.cburch.logisim.fpga.file.BoardReaderClass;
import com.cburch.logisim.fpga.file.BoardWriterClass;
import com.cburch.logisim.fpga.file.XMLFileFilter;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BoardEditor implements ActionListener, BaseComponentListenerContract, LocaleListener, BoardManipulatorListener {

  private final JFrame panel;
  private BoardInformation TheBoard = new BoardInformation();
  private final JTextField BoardNameInput;
  private final JButton saveButton = new JButton();
  private final JButton loadButton = new JButton();
  private final JButton importButton = new JButton();
  private final JButton cancelButton = new JButton();
  private final JButton fpgaButton = new JButton();
  private final JLabel LocText = new JLabel();
  private final BoardManipulator picturepanel;
  private static final String CancelStr = "cancel";
  private static final String FPGAStr = "fpgainfo";

  public BoardEditor() {
    GridBagConstraints gbc = new GridBagConstraints();

    panel = new JFrame();
    panel.setResizable(false);
    panel.addComponentListener(this);
    panel.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    GridBagLayout thisLayout = new GridBagLayout();
    panel.setLayout(thisLayout);

    // Set an empty board picture
    picturepanel = new BoardManipulator(panel);
    picturepanel.addComponentListener(this);
    picturepanel.addBoardManipulatorListener(this);

    JPanel ButtonPanel = new JPanel();
    GridBagLayout ButtonLayout = new GridBagLayout();
    ButtonPanel.setLayout(ButtonLayout);

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    ButtonPanel.add(LocText, gbc);

    BoardNameInput = new JTextField(22);
    BoardNameInput.setEnabled(false);
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    ButtonPanel.add(BoardNameInput, gbc);

    gbc.gridx = 4;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    cancelButton.setActionCommand(CancelStr);
    cancelButton.addActionListener(this);
    ButtonPanel.add(cancelButton, gbc);

    gbc.gridx = 1;
    gbc.gridy = 1;
    fpgaButton.setActionCommand(FPGAStr);
    fpgaButton.addActionListener(this);
    fpgaButton.setEnabled(false);
    ButtonPanel.add(fpgaButton, gbc);

    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    ButtonPanel.add(picturepanel.getZoomSlider(), gbc);

    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loadButton.setActionCommand("load");
    loadButton.addActionListener(this);
    loadButton.setEnabled(true);
    ButtonPanel.add(loadButton, gbc);

    gbc.gridx = 4;
    importButton.setActionCommand("internal");
    importButton.addActionListener(this);
    ButtonPanel.add(importButton, gbc);

    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    saveButton.setActionCommand("save");
    saveButton.addActionListener(this);
    saveButton.setEnabled(false);
    ButtonPanel.add(saveButton, gbc);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(ButtonPanel, gbc);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(picturepanel, gbc);

    panel.setLocationRelativeTo(null);
    panel.setVisible(true);
    int ScreenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    int ScreenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
    int ImageWidth = picturepanel.getWidth();
    int ImageHeight = picturepanel.getHeight();
    int ImageXBorder = panel.getWidth() - ImageWidth;
    int ImageYBorder = panel.getHeight() - ImageHeight;
    ScreenWidth -= ImageXBorder;
    ScreenHeight -= (ImageYBorder + (ImageYBorder >> 1));
    int zoomX = (ScreenWidth * 100) / ImageWidth;
    int zoomY = (ScreenHeight * 100) / ImageHeight;
    picturepanel.setMaxZoom(Math.min(zoomX, zoomY));
    localeChanged();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    switch (e.getActionCommand()) {
      case CancelStr:
        this.clear();
        break;
      case "save":
        panel.setVisible(false);
        TheBoard.setBoardName(BoardNameInput.getText());
        String filename = getDirName("", S.get("FpgaBoardSaveDir"));
        filename += TheBoard.getBoardName() + ".xml";
        TheBoard.setComponents(picturepanel.getIoComponents());
        BoardWriterClass xmlwriter = new BoardWriterClass(TheBoard, picturepanel.getImage());
        xmlwriter.PrintXml(filename);
        this.clear();
        break;
      case "load":
        JFileChooser fc = new JFileChooser(S.get("FpgaBoardLoadFile"));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setFileFilter(XMLFileFilter.XML_FILTER);
        fc.setAcceptAllFileFilterUsed(false);
        int retval = fc.showOpenDialog(null);
        if (retval == JFileChooser.APPROVE_OPTION) {
          File file = fc.getSelectedFile();
          String FileName = file.getPath();
          BoardReaderClass reader = new BoardReaderClass(FileName);
          UpdateInfo(reader);
        }
        break;
      case FPGAStr:
        FpgaIoInformationSettingsDialog.getFpgaInformation(panel, TheBoard);
        if (picturepanel.hasIOComponents() && TheBoard.fpga.FpgaInfoPresent())
          saveButton.setEnabled(true);
        break;
      case "internal":
        String Board = getInternalBoardName();
        if (Board != null) {
          BoardReaderClass reader = new BoardReaderClass(
              AppPreferences.Boards.getBoardFilePath(Board));
          UpdateInfo(reader);
        }
        break;
    }
  }

  private void UpdateInfo(BoardReaderClass reader) {
    TheBoard = reader.GetBoardInformation();
    picturepanel.setBoard(TheBoard);
    picturepanel.repaint();
  }

  private String getInternalBoardName() {
    List<String> boards = AppPreferences.Boards.getBoardNames();
    return (String)
        OptionPane.showInputDialog(
            panel,
            S.get("FpgaBoardSelect"),
            S.get("FpgaBoardLoadInternal"),
            OptionPane.PLAIN_MESSAGE,
            null,
            boards.toArray(),
            boards.get(0));
  }

  private String checkIfEndsWithSlash(String path) {
    if (!path.endsWith("/")) {
      path += "/";
    }
    return (path);
  }

  public void clear() {
    if (panel.isVisible()) panel.setVisible(false);
    picturepanel.clear();
    TheBoard.clear();
    BoardNameInput.setText("");
    saveButton.setEnabled(false);
    fpgaButton.setEnabled(false);
    loadButton.setEnabled(true);
    importButton.setEnabled(true);
  }

  @Override
  public void componentResized(ComponentEvent e) {
    panel.pack();
  }

  private String getDirName(String old, String window_name) {
    JFileChooser fc = new JFileChooser(old);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fc.setDialogTitle(window_name);
    int retval = fc.showOpenDialog(null);
    if (retval == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      old = checkIfEndsWithSlash(file.getPath());
    }
    return old;
  }

  public JFrame GetPanel() {
    return panel;
  }

  public boolean isActive() {
    return panel.isVisible();
  }

  public void setActive() {
    this.clear();
    panel.setVisible(true);
  }

  @Override
  public void localeChanged() {
    panel.setTitle(S.get("FPGABoardEditor"));
    LocText.setText(S.get("FpgaBoardName"));
    cancelButton.setText(S.get("FpgaBoardCancel"));
    loadButton.setText(S.get("FpgaBoardLoadExternal"));
    importButton.setText(S.get("FpgaBoardLoadInternal"));
    saveButton.setText(S.get("FpgaBoardSave"));
    fpgaButton.setText(S.get("FpgaBoardFpgaParam"));
    panel.pack();
  }

  @Override
  public void boardNameChanged(String newBoardName) {
    BoardNameInput.setEnabled(true);
    BoardNameInput.setText(newBoardName);
    TheBoard.setBoardName(newBoardName);
    loadButton.setEnabled(false);
    importButton.setEnabled(false);
    fpgaButton.setEnabled(true);
    saveButton.setEnabled(picturepanel.hasIOComponents() && TheBoard.fpga.FpgaInfoPresent());
  }

  @Override
  public void componentsChanged(IoComponentsInformation IOcomps) {
    saveButton.setEnabled(IOcomps.hasComponents() && TheBoard.fpga.FpgaInfoPresent());
  }
}
