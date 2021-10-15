/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.file;

import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.FpgaClass;
import com.cburch.logisim.fpga.data.FpgaIoInformationContainer;
import com.cburch.logisim.fpga.gui.DialogNotification;
import com.cburch.logisim.util.XmlUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BoardReaderClass {

  static final Logger logger = LoggerFactory.getLogger(BoardReaderClass.class);

  private final String myfilename;
  private DocumentBuilderFactory factory;
  private DocumentBuilder parser;
  private Document BoardDoc;

  public BoardReaderClass(String filename) {
    myfilename = filename;
  }

  private BufferedImage createImage(int width, int height, String[] CodeTable, String PixelData) {
    ImageXmlFactory reader = new ImageXmlFactory();
    reader.setCodeTable(CodeTable);
    reader.setCompressedString(PixelData);
    return reader.getPicture(width, height);
  }

  public BoardInformation getBoardInformation() {
    try {
      // Create instance of DocumentBuilderFactory
      factory = XmlUtil.getHardenedBuilderFactory();
      // Get the DocumentBuilder
      parser = factory.newDocumentBuilder();
      // Create blank DOM Document
      if (myfilename.startsWith("url:")) {
        InputStream xml =
            getClass().getResourceAsStream("/" + myfilename.substring("url:".length()));
        BoardDoc = parser.parse(xml);
      } else if (myfilename.startsWith("file:")) {
        File xml = new File(myfilename.substring("file:".length()));
        BoardDoc = parser.parse(xml);
      } else {
        File xml = new File(myfilename);
        BoardDoc = parser.parse(xml);
      }

      NodeList ImageList = BoardDoc.getElementsByTagName(BoardWriterClass.IMAGE_INFORMATION_STRING);
      if (ImageList.getLength() != 1) return null;
      Node ThisImage = ImageList.item(0);
      NodeList ImageParameters = ThisImage.getChildNodes();

      String[] CodeTable = null;
      String PixelData = null;
      int PictureWidth = 0;
      int PictureHeight = 0;
      for (int i = 0; i < ImageParameters.getLength(); i++) {
        if (ImageParameters.item(i).getNodeName().equals("CompressionCodeTable")) {
          NamedNodeMap TableAttrs = ImageParameters.item(i).getAttributes();
          for (int j = 0; j < TableAttrs.getLength(); j++) {
            if (TableAttrs.item(j).getNodeName().equals("TableData")) {
              String CodeTableStr = TableAttrs.item(j).getNodeValue();
              CodeTable = CodeTableStr.split(" ");
            }
          }
        }
        if (ImageParameters.item(i).getNodeName().equals("PictureDimension")) {
          NamedNodeMap SizeAttrs = ImageParameters.item(i).getAttributes();
          for (int j = 0; j < SizeAttrs.getLength(); j++) {
            if (SizeAttrs.item(j).getNodeName().equals("Width"))
              PictureWidth = Integer.parseInt(SizeAttrs.item(j).getNodeValue());
            if (SizeAttrs.item(j).getNodeName().equals("Height"))
              PictureHeight = Integer.parseInt(SizeAttrs.item(j).getNodeValue());
          }
        }
        if (ImageParameters.item(i).getNodeName().equals("PixelData")) {
          NamedNodeMap PixelAttrs = ImageParameters.item(i).getAttributes();
          for (int j = 0; j < PixelAttrs.getLength(); j++)
            if (PixelAttrs.item(j).getNodeName().equals("PixelRGB"))
              PixelData = PixelAttrs.item(j).getNodeValue();
        }
      }
      if (CodeTable == null) {
        DialogNotification.showDialogNotification(
            // FIXME: hardcoded string
            null, "Error", "The selected XML file does not contain a compression code table");
        return null;
      }
      if ((PictureWidth == 0) || (PictureHeight == 0)) {
        DialogNotification.showDialogNotification(
            // FIXME: hardcoded string
            null, "Error", "The selected XML file does not contain the picture dimensions");
        return null;
      }
      if (PixelData == null) {
        DialogNotification.showDialogNotification(
            // FIXME: hardcoded string
            null, "Error", "The selected XML file does not contain the picture data");
        return null;
      }

      BoardInformation result = new BoardInformation();
      result.setBoardName(BoardDoc.getDocumentElement().getNodeName());
      BufferedImage Picture = createImage(PictureWidth, PictureHeight, CodeTable, PixelData);
      if (Picture == null) return null;
      result.setImage(Picture);
      FpgaClass FPGA = getFpgaInfo();
      if (FPGA == null) return null;
      result.fpga = FPGA;
      NodeList CompList = BoardDoc.getElementsByTagName("PinsInformation"); // for backward
      // compatibility
      processComponentList(CompList, result);
      CompList = BoardDoc.getElementsByTagName("ButtonsInformation"); // for
      // backward
      // compatibility
      processComponentList(CompList, result);
      CompList = BoardDoc.getElementsByTagName("LEDsInformation"); // for
      // backward
      // compatibility
      processComponentList(CompList, result);
      CompList = BoardDoc.getElementsByTagName(BoardWriterClass.COMPONENTS_SECTION_STRING); // new
      // format
      processComponentList(CompList, result);
      return result;
    } catch (Exception e) {
      logger.error(
          "Exceptions not handled yet in GetBoardInformation(), but got an exception: {}",
          e.getMessage());
      /* TODO: handle exceptions */
      return null;
    }
  }

  private FpgaClass getFpgaInfo() {
    var fpgaList =
        BoardDoc.getElementsByTagName(BoardWriterClass.BOARD_INFORMATION_SECTION_STRING);
    var frequency = -1L;
    String clockPin = null;
    String clockPull = null;
    String clockStand = null;
    String unusedPull = null;
    String vendor = null;
    String part = null;
    String family = null;
    String Package = null;
    String speed = null;
    String usbTmc = null;
    String jtagPos = null;
    String flashName = null;
    String flashPos = null;
    if (fpgaList.getLength() != 1) return null;
    final var thisFpga = fpgaList.item(0);
    final var fpgaParams = thisFpga.getChildNodes();
    for (int i = 0; i < fpgaParams.getLength(); i++) {
      if (fpgaParams.item(i)
          .getNodeName()
          .equals(BoardWriterClass.CLOCK_INFORMATION_SECTION_STRING)) {
        final var clockAttrs = fpgaParams.item(i).getAttributes();
        for (int j = 0; j < clockAttrs.getLength(); j++) {
          if (clockAttrs.item(j).getNodeName().equals(BoardWriterClass.CLOCK_SECTION_STRINGS[0]))
            frequency = Long.parseLong(clockAttrs.item(j).getNodeValue());
          if (clockAttrs.item(j).getNodeName().equals(BoardWriterClass.CLOCK_SECTION_STRINGS[1]))
            clockPin = clockAttrs.item(j).getNodeValue();
          if (clockAttrs.item(j).getNodeName().equals(BoardWriterClass.CLOCK_SECTION_STRINGS[2]))
            clockPull = clockAttrs.item(j).getNodeValue();
          if (clockAttrs.item(j).getNodeName().equals(BoardWriterClass.CLOCK_SECTION_STRINGS[3]))
            clockStand = clockAttrs.item(j).getNodeValue();
        }
      }
      if (fpgaParams.item(i).getNodeName().equals(BoardWriterClass.UNUSED_PINS_STRING)) {
        final var unusedAttrs = fpgaParams.item(i).getAttributes();
        for (int j = 0; j < unusedAttrs.getLength(); j++)
          if (unusedAttrs.item(j).getNodeName().equals("PullBehavior"))
            unusedPull = unusedAttrs.item(j).getNodeValue();
      }
      if (fpgaParams.item(i)
          .getNodeName()
          .equals(BoardWriterClass.FPGA_INFORMATION_SECTION_STRING)) {
        final var fpgaAttrs = fpgaParams.item(i).getAttributes();
        for (int j = 0; j < fpgaAttrs.getLength(); j++) {
          if (fpgaAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[0]))
            vendor = fpgaAttrs.item(j).getNodeValue();
          if (fpgaAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[1]))
            part = fpgaAttrs.item(j).getNodeValue();
          if (fpgaAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[2]))
            family = fpgaAttrs.item(j).getNodeValue();
          if (fpgaAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[3]))
            Package = fpgaAttrs.item(j).getNodeValue();
          if (fpgaAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[4]))
            speed = fpgaAttrs.item(j).getNodeValue();
          if (fpgaAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[5]))
            usbTmc = fpgaAttrs.item(j).getNodeValue();
          if (fpgaAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[6]))
            jtagPos = fpgaAttrs.item(j).getNodeValue();
          if (fpgaAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[7]))
            flashName = fpgaAttrs.item(j).getNodeValue();
          if (fpgaAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[8]))
            flashPos = fpgaAttrs.item(j).getNodeValue();
        }
      }
    }
    if ((frequency < 0)
        || (clockPin == null)
        || (clockPull == null)
        || (clockStand == null)
        || (unusedPull == null)
        || (vendor == null)
        || (part == null)
        || (family == null)
        || (Package == null)
        || (speed == null)) {
      // FIXME: hardcoded string
      DialogNotification.showDialogNotification(null, "Error", "The selected xml file does not contain the required FPGA parameters");
      return null;
    }
    if (usbTmc == null) usbTmc = Boolean.toString(false);
    if (jtagPos == null) jtagPos = "1";
    if (flashPos == null) flashPos = "2";
    FpgaClass result = new FpgaClass();
    result.set(
        frequency,
        clockPin,
        clockPull,
        clockStand,
        family,
        part,
        Package,
        speed,
        vendor,
        unusedPull,
        usbTmc.equals(Boolean.toString(true)),
        jtagPos,
        flashName,
        flashPos);
    return result;
  }

  private void processComponentList(NodeList compList, BoardInformation board) {
    Node tempNode = null;
    if (compList.getLength() == 1) {
      tempNode = compList.item(0);
      compList = tempNode.getChildNodes();
      for (var i = 0; i < compList.getLength(); i++) {
        final var newComp = new FpgaIoInformationContainer(compList.item(i));
        if (newComp.isKnownComponent()) {
          board.addComponent(newComp);
        }
      }
    }
  }
}
