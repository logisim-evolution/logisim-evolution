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
import com.cburch.logisim.fpga.file.XmlFileFilter;
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
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BoardEditor implements ActionListener, BaseComponentListenerContract, LocaleListener, BoardManipulatorListener {

  private final JFrame panel;
  private BoardInformation theBoard = new BoardInformation();
  private final JTextField boardNameInput;
  private final JButton saveButton = new JButton();
  private final JButton loadButton = new JButton();
  private final JButton importButton = new JButton();
  private final JButton cancelButton = new JButton();
  private final JButton fpgaButton = new JButton();
  private final JLabel locTextLabel = new JLabel();
  private final BoardManipulator picturepanel;
  // FIXME: hardcoded string (??)
  private static final String cancelStr = "cancel";
  private static final String fpgaStr = "fpgainfo";

  public BoardEditor() {
    final var gbc = new GridBagConstraints();

    panel = new JFrame();
    panel.setResizable(false);
    panel.addComponentListener(this);
    panel.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    final var thisLayout = new GridBagLayout();
    panel.setLayout(thisLayout);

    // Set an empty board picture
    picturepanel = new BoardManipulator(panel);
    picturepanel.addComponentListener(this);
    picturepanel.addBoardManipulatorListener(this);

    final var buttonPanel = new JPanel();
    final var buttonLayout = new GridBagLayout();
    buttonPanel.setLayout(buttonLayout);

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    buttonPanel.add(locTextLabel, gbc);

    boardNameInput = new JTextField(22);
    boardNameInput.setEnabled(false);
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    buttonPanel.add(boardNameInput, gbc);

    gbc.gridx = 4;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    cancelButton.setActionCommand(cancelStr);
    cancelButton.addActionListener(this);
    buttonPanel.add(cancelButton, gbc);

    gbc.gridx = 1;
    gbc.gridy = 1;
    fpgaButton.setActionCommand(fpgaStr);
    fpgaButton.addActionListener(this);
    fpgaButton.setEnabled(false);
    buttonPanel.add(fpgaButton, gbc);

    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    buttonPanel.add(picturepanel.getZoomSlider(), gbc);

    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loadButton.setActionCommand("load");
    loadButton.addActionListener(this);
    loadButton.setEnabled(true);
    buttonPanel.add(loadButton, gbc);

    gbc.gridx = 4;
    importButton.setActionCommand("internal");
    importButton.addActionListener(this);
    buttonPanel.add(importButton, gbc);

    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    saveButton.setActionCommand("save");
    saveButton.addActionListener(this);
    saveButton.setEnabled(false);
    buttonPanel.add(saveButton, gbc);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(buttonPanel, gbc);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(picturepanel, gbc);

    panel.setLocationRelativeTo(null);
    panel.setVisible(true);
    var screenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    var screenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
    final var imageWidth = picturepanel.getWidth();
    final var imageHeight = picturepanel.getHeight();
    final var imageBorderX = panel.getWidth() - imageWidth;
    final var imageBorderY = panel.getHeight() - imageHeight;
    screenWidth -= imageBorderX;
    screenHeight -= (imageBorderY + (imageBorderY >> 1));
    final var zoomX = (screenWidth * 100) / imageWidth;
    final var zoomY = (screenHeight * 100) / imageHeight;
    picturepanel.setMaxZoom(Math.min(zoomX, zoomY));
    localeChanged();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    switch (e.getActionCommand()) {
      case cancelStr -> this.clear();
      case "save" -> {
        panel.setVisible(false);
        theBoard.setBoardName(boardNameInput.getText());
        String filename = getDirName("", S.get("FpgaBoardSaveDir"));
        filename += theBoard.getBoardName() + ".xml";
        theBoard.setComponents(picturepanel.getIoComponents());
        BoardWriterClass xmlwriter = new BoardWriterClass(theBoard, picturepanel.getImage());
        xmlwriter.printXml(filename);
        this.clear();
      }
      case "load" -> {
        JFileChooser fc = new JFileChooser(S.get("FpgaBoardLoadFile"));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setFileFilter(XmlFileFilter.XML_FILTER);
        fc.setAcceptAllFileFilterUsed(false);
        int retval = fc.showOpenDialog(null);
        if (retval == JFileChooser.APPROVE_OPTION) {
          File file = fc.getSelectedFile();
          String FileName = file.getPath();
          BoardReaderClass reader = new BoardReaderClass(FileName);
          updateInfo(reader);
        }
      }
      case fpgaStr -> {
        FpgaIoInformationSettingsDialog.getFpgaInformation(panel, theBoard);
        if (picturepanel.hasIOComponents() && theBoard.fpga.isFpgaInfoPresent())
          saveButton.setEnabled(true);
      }
      case "internal" -> {
        String Board = getInternalBoardName();
        if (Board != null) {
          BoardReaderClass reader = new BoardReaderClass(
              AppPreferences.Boards.getBoardFilePath(Board));
          updateInfo(reader);
        }
      }
    }
  }

  private void updateInfo(BoardReaderClass reader) {
    theBoard = reader.getBoardInformation();
    picturepanel.setBoard(theBoard);
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
    theBoard.clear();
    boardNameInput.setText("");
    saveButton.setEnabled(false);
    fpgaButton.setEnabled(false);
    loadButton.setEnabled(true);
    importButton.setEnabled(true);
  }

  @Override
  public void componentResized(ComponentEvent event) {
    panel.pack();
  }

  private String getDirName(String old, String windowName) {
    JFileChooser fc = new JFileChooser(old);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fc.setDialogTitle(windowName);
    int retval = fc.showOpenDialog(null);
    if (retval == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      old = checkIfEndsWithSlash(file.getPath());
    }
    return old;
  }

  public JFrame getPanel() {
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
    locTextLabel.setText(S.get("FpgaBoardName"));
    cancelButton.setText(S.get("FpgaBoardCancel"));
    loadButton.setText(S.get("FpgaBoardLoadExternal"));
    importButton.setText(S.get("FpgaBoardLoadInternal"));
    saveButton.setText(S.get("FpgaBoardSave"));
    fpgaButton.setText(S.get("FpgaBoardFpgaParam"));
    panel.pack();
  }

  @Override
  public void boardNameChanged(String newBoardName) {
    boardNameInput.setEnabled(true);
    boardNameInput.setText(newBoardName);
    theBoard.setBoardName(newBoardName);
    loadButton.setEnabled(false);
    importButton.setEnabled(false);
    fpgaButton.setEnabled(true);
    saveButton.setEnabled(picturepanel.hasIOComponents() && theBoard.fpga.isFpgaInfoPresent());
  }

  @Override
  public void componentsChanged(IoComponentsInformation ioComps) {
    saveButton.setEnabled(ioComps.hasComponents() && theBoard.fpga.isFpgaInfoPresent());
  }
}
