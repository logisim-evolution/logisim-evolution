/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.pio;

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

public class PioMenu implements ActionListener, MenuExtender {

  private final Instance instance;
  private Frame frame;
  private JMenuItem exportC;

  public PioMenu(Instance inst) {
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

  private void exportC() {
    JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int result = fc.showDialog(frame, S.get("SelectDirectoryToStoreC"));
    if (result == JFileChooser.APPROVE_OPTION) {
      PioState myState = instance.getAttributeValue(PioAttributes.PIO_STATE);
      if (myState == null) throw new NullPointerException("BUG in PioMenu.java");
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
      headerWriter.println("/* Logisim automatically generated file for a PIO-component */\n");
      cWriter.println("/* Logisim automatically generated file for a PIO-component */\n");
      headerWriter.println("#ifndef __" + compName + "_H__");
      headerWriter.println("#define __" + compName + "_H__");
      headerWriter.println();
      int base = myState.getStartAddress();
      int nrBits = myState.getNrOfIOs().getWidth();
      String functName;
      if (myState.getPortDirection() != PioAttributes.PORT_INPUT) {
        functName = "OutputValue";
        headerWriter.println(S.get("PioMenuOutputDataFunctionRemark", Integer.toString(nrBits)));
        SocSupport.addSetterFunction(headerWriter, compName, functName, base, 0, true);
        headerWriter.println();
        SocSupport.addSetterFunction(cWriter, compName, functName, base, 0, false);
      }
      if (myState.getPortDirection() != PioAttributes.PORT_OUTPUT) {
        functName = "InputValue";
        headerWriter.println(S.get("PioMenuInputDataFunctionRemark", Integer.toString(nrBits)));
        SocSupport.addGetterFunction(headerWriter, compName, functName, base, 0, true);
        headerWriter.println();
        SocSupport.addGetterFunction(cWriter, compName, functName, base, 0, false);
        if (myState.getPortDirection() == PioAttributes.PORT_BIDIR) {
          functName = "DirectionReg";
          headerWriter.println(S.get("PioMenuBidirFunctionsRemark", Integer.toString(nrBits)));
          SocSupport.addAllFunctions(headerWriter, cWriter, compName, functName, base, 2);
        }
        if (myState.inputGeneratesIrq()) {
          functName = "IrqMaskReg";
          String reactName =
              myState.getIrqType() == PioAttributes.IRQ_EDGE
                  ? S.get("PioMenuIrqEdge")
                  : S.get("PioMenuIrqLevel");
          headerWriter.println(
              S.get("PioMenuMaskFunctionsRemark", reactName, Integer.toString(nrBits)));
          SocSupport.addAllFunctions(headerWriter, cWriter, compName, functName, base, 2);
        }
        if (myState.inputIsCapturedSynchronisely()) {
          functName = "EdgeCapturReg";
          String EdgeName = S.get("PioMenuCaptureAny");
          if (myState.getInputCaptureEdge() == PioAttributes.CAPT_RISING)
            EdgeName = S.get("PioMenuCaptureRising");
          if (myState.getInputCaptureEdge() == PioAttributes.CAPT_FALLING)
            EdgeName = S.get("PioMenuCaptureFalling");
          String ClearName =
              myState.inputCaptureSupportsBitClearing()
                  ? S.get("PioMenuCaptureBit")
                  : S.get("PioMenuCaptureAll");
          headerWriter.println(
              S.get("PioMenuEdgeCaptureRemark", EdgeName, ClearName, Integer.toString(nrBits)));
          SocSupport.addAllFunctions(headerWriter, cWriter, compName, functName, base, 3);
        }
        if (myState.outputSupportsBitManipulations()) {
          functName = "OutsetReg";
          headerWriter.println(S.get("PioMenuOutSetRemark", Integer.toString(nrBits)));
          SocSupport.addSetterFunction(headerWriter, compName, functName, base, 4, true);
          headerWriter.println();
          SocSupport.addSetterFunction(cWriter, compName, functName, base, 4, false);
          functName = "OutclearReg";
          headerWriter.println(S.get("PioMenuOutClearRemark", Integer.toString(nrBits)));
          SocSupport.addSetterFunction(headerWriter, compName, functName, base, 5, true);
          headerWriter.println();
          SocSupport.addSetterFunction(cWriter, compName, functName, base, 5, false);
        }
      }
      headerWriter.println("#endif");
      headerWriter.close();
      cWriter.close();
      OptionPane.showMessageDialog(
          frame, S.get("SuccesCreatingHeaderAndCFile", headerFileName, cFileName));
    }
  }
}
