/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.file;

import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.FPGAClass;
import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.gui.DialogNotification;
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

  private BufferedImage CreateImage(int width, int height, String[] CodeTable, String PixelData) {
    ImageXmlFactory reader = new ImageXmlFactory();
    reader.SetCodeTable(CodeTable);
    reader.SetCompressedString(PixelData);
    return reader.GetPicture(width, height);
  }

  public BoardInformation GetBoardInformation() {
    try {
      // Create instance of DocumentBuilderFactory
      factory = DocumentBuilderFactory.newInstance();
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
            null, "Error", "The selected xml file does not contain a compression code table");
        return null;
      }
      if ((PictureWidth == 0) || (PictureHeight == 0)) {
        DialogNotification.showDialogNotification(
            null, "Error", "The selected xml file does not contain the picture dimensions");
        return null;
      }
      if (PixelData == null) {
        DialogNotification.showDialogNotification(
            null, "Error", "The selected xml file does not contain the picture data");
        return null;
      }

      BoardInformation result = new BoardInformation();
      result.setBoardName(BoardDoc.getDocumentElement().getNodeName());
      BufferedImage Picture = CreateImage(PictureWidth, PictureHeight, CodeTable, PixelData);
      if (Picture == null) return null;
      result.SetImage(Picture);
      FPGAClass FPGA = GetFPGAInfo();
      if (FPGA == null) return null;
      result.fpga = FPGA;
      NodeList CompList = BoardDoc.getElementsByTagName("PinsInformation"); // for backward
      // compatibility
      ProcessComponentList(CompList, result);
      CompList = BoardDoc.getElementsByTagName("ButtonsInformation"); // for
      // backward
      // compatibility
      ProcessComponentList(CompList, result);
      CompList = BoardDoc.getElementsByTagName("LEDsInformation"); // for
      // backward
      // compatibility
      ProcessComponentList(CompList, result);
      CompList = BoardDoc.getElementsByTagName(BoardWriterClass.COMPONENTS_SECTION_STRING); // new
      // format
      ProcessComponentList(CompList, result);
      return result;
    } catch (Exception e) {
      logger.error(
          "Exceptions not handled yet in GetBoardInformation(), but got an exception: {}",
          e.getMessage());
      /* TODO: handle exceptions */
      return null;
    }
  }

  private FPGAClass GetFPGAInfo() {
    NodeList FPGAList =
        BoardDoc.getElementsByTagName(BoardWriterClass.BOARD_INFORMATION_SECTION_STRING);
    long frequency = -1;
    String clockpin = null;
    String clockpull = null;
    String clockstand = null;
    String Unusedpull = null;
    String vendor = null;
    String Part = null;
    String family = null;
    String Package = null;
    String Speed = null;
    String UsbTmc = null;
    String JTAGPos = null;
    String FlashName = null;
    String FlashPos = null;
    if (FPGAList.getLength() != 1) return null;
    Node ThisFPGA = FPGAList.item(0);
    NodeList FPGAParameters = ThisFPGA.getChildNodes();
    for (int i = 0; i < FPGAParameters.getLength(); i++) {
      if (FPGAParameters.item(i)
          .getNodeName()
          .equals(BoardWriterClass.CLOCK_INFORMATION_SECTION_STRING)) {
        NamedNodeMap ClockAttrs = FPGAParameters.item(i).getAttributes();
        for (int j = 0; j < ClockAttrs.getLength(); j++) {
          if (ClockAttrs.item(j).getNodeName().equals(BoardWriterClass.CLOCK_SECTION_STRINGS[0]))
            frequency = Long.parseLong(ClockAttrs.item(j).getNodeValue());
          if (ClockAttrs.item(j).getNodeName().equals(BoardWriterClass.CLOCK_SECTION_STRINGS[1]))
            clockpin = ClockAttrs.item(j).getNodeValue();
          if (ClockAttrs.item(j).getNodeName().equals(BoardWriterClass.CLOCK_SECTION_STRINGS[2]))
            clockpull = ClockAttrs.item(j).getNodeValue();
          if (ClockAttrs.item(j).getNodeName().equals(BoardWriterClass.CLOCK_SECTION_STRINGS[3]))
            clockstand = ClockAttrs.item(j).getNodeValue();
        }
      }
      if (FPGAParameters.item(i).getNodeName().equals(BoardWriterClass.UNUSED_PINS_STRING)) {
        NamedNodeMap UnusedAttrs = FPGAParameters.item(i).getAttributes();
        for (int j = 0; j < UnusedAttrs.getLength(); j++)
          if (UnusedAttrs.item(j).getNodeName().equals("PullBehavior"))
            Unusedpull = UnusedAttrs.item(j).getNodeValue();
      }
      if (FPGAParameters.item(i)
          .getNodeName()
          .equals(BoardWriterClass.FPGA_INFORMATION_SECTION_STRING)) {
        NamedNodeMap FPGAAttrs = FPGAParameters.item(i).getAttributes();
        for (int j = 0; j < FPGAAttrs.getLength(); j++) {
          if (FPGAAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[0]))
            vendor = FPGAAttrs.item(j).getNodeValue();
          if (FPGAAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[1]))
            Part = FPGAAttrs.item(j).getNodeValue();
          if (FPGAAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[2]))
            family = FPGAAttrs.item(j).getNodeValue();
          if (FPGAAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[3]))
            Package = FPGAAttrs.item(j).getNodeValue();
          if (FPGAAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[4]))
            Speed = FPGAAttrs.item(j).getNodeValue();
          if (FPGAAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[5]))
            UsbTmc = FPGAAttrs.item(j).getNodeValue();
          if (FPGAAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[6]))
            JTAGPos = FPGAAttrs.item(j).getNodeValue();
          if (FPGAAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[7]))
            FlashName = FPGAAttrs.item(j).getNodeValue();
          if (FPGAAttrs.item(j).getNodeName().equals(BoardWriterClass.FPGA_SECTION_STRINGS[8]))
            FlashPos = FPGAAttrs.item(j).getNodeValue();
        }
      }
    }
    if ((frequency < 0)
        || (clockpin == null)
        || (clockpull == null)
        || (clockstand == null)
        || (Unusedpull == null)
        || (vendor == null)
        || (Part == null)
        || (family == null)
        || (Package == null)
        || (Speed == null)) {
      DialogNotification.showDialogNotification(
          null, "Error", "The selected xml file does not contain the required FPGA parameters");
      return null;
    }
    if (UsbTmc == null) UsbTmc = Boolean.toString(false);
    if (JTAGPos == null) JTAGPos = "1";
    if (FlashPos == null) FlashPos = "2";
    FPGAClass result = new FPGAClass();
    result.Set(
        frequency,
        clockpin,
        clockpull,
        clockstand,
        family,
        Part,
        Package,
        Speed,
        vendor,
        Unusedpull,
        UsbTmc.equals(Boolean.toString(true)),
        JTAGPos,
        FlashName,
        FlashPos);
    return result;
  }

  private void ProcessComponentList(NodeList CompList, BoardInformation board) {
    Node tempNode = null;
    if (CompList.getLength() == 1) {
      tempNode = CompList.item(0);
      CompList = tempNode.getChildNodes();
      for (int i = 0; i < CompList.getLength(); i++) {
        FPGAIOInformationContainer NewComp = new FPGAIOInformationContainer(CompList.item(i));
        if (NewComp.IsKnownComponent()) {
          board.AddComponent(NewComp);
        }
      }
    }
  }
}
