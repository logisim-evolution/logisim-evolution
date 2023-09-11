/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.vga;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.tools.MenuExtender;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class VgaMenu implements ActionListener, MenuExtender {

  private final Instance instance;
  private Frame frame;
  private JMenuItem exportC;

  public VgaMenu(Instance inst) {
    instance = inst;
  }

  @Override
  public void configureMenu(JPopupMenu menu, Project proj) {
    this.frame = proj.getFrame();
    exportC = SocSupport.createItem(this, S.get("ExportC"));
    menu.addSeparator();
    menu.add(exportC);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src == exportC) exportC();
  }

  public void exportC() {
    JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int result = fc.showDialog(frame, S.get("SelectDirectoryToStoreC"));
    if (result == JFileChooser.APPROVE_OPTION) {
      VgaState myState = instance.getAttributeValue(VgaAttributes.VGA_STATE);
      if (myState == null) throw new NullPointerException("BUG in VgaMenu.java");
      String compName =
          myState.getName().replace(" ", "_").replace("@", "_").replace(",", "_").toUpperCase();
      String headerFileName =
          fc.getSelectedFile().getAbsolutePath() + File.separator + compName + ".h";
      String cFileName = fc.getSelectedFile().getAbsolutePath() + File.separator + compName + ".c";
      FileWriter headerFile = null;
      FileWriter cFile = null;
      try {
        headerFile = new FileWriter(headerFileName, false);
      } catch (IOException e) {
        headerFile = null;
      }
      try {
        cFile = new FileWriter(cFileName, false);
      } catch (IOException e) {
        cFile = null;
      }
      if (headerFile == null || cFile == null) {
        OptionPane.showMessageDialog(
            frame,
            S.get("ErrorCreatingHeaderAndOrCFile"),
            S.get("ExportC"),
            OptionPane.ERROR_MESSAGE);
        return;
      }
      PrintWriter headerWriter = new PrintWriter(headerFile);
      PrintWriter cWriter = new PrintWriter(cFile);
      headerWriter.println("/* Logisim automatically generated file for a VGA-component */\n");
      cWriter.println("/* Logisim automatically generated file for a VGA-component */\n");
      headerWriter.println("#ifndef __" + compName + "_H__");
      headerWriter.println("#define __" + compName + "_H__");
      headerWriter.println();
      headerWriter.println("#define SOFT_MODE_160X120_MASK " + VgaAttributes.MODE_160_120_MASK);
      headerWriter.println("#define SOFT_MODE_320X240_MASK " + VgaAttributes.MODE_320_240_MASK);
      headerWriter.println("#define SOFT_MODE_640X480_MASK " + VgaAttributes.MODE_640_480_MASK);
      headerWriter.println("#define SOFT_MODE_800X600_MASK " + VgaAttributes.MODE_800_600_MASK);
      headerWriter.println("#define SOFT_MODE_1024X768_MASK " + VgaAttributes.MODE_1024_768_MASK);
      headerWriter.println();
      int base = myState.getStartAddress();
      headerWriter.println(S.get("VgaMenuModeSelectFunctions"));
      SocSupport.addAllFunctions(headerWriter, cWriter, compName, "VgaMode", base, 0);
      headerWriter.println("#endif");
      headerWriter.close();
      cWriter.close();
      OptionPane.showMessageDialog(
          frame, S.get("SuccesCreatingHeaderAndCFile", headerFileName, cFileName));
    }
  }
}
