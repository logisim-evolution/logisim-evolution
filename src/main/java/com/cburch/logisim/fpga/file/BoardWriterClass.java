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
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.util.XmlUtil;
import java.awt.Image;
import java.io.File;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class BoardWriterClass {

  static final Logger logger = LoggerFactory.getLogger(BoardWriterClass.class);

  public static final String BOARD_INFORMATION_SECTION_STRING = "BoardInformation";
  public static final String CLOCK_INFORMATION_SECTION_STRING = "ClockInformation";
  public static final String INPUT_SET_STRING = "InputPinSet";
  public static final String OUTPUT_SET_STRING = "OutputPinSet";
  public static final String IO_SET_STRING = "BiDirPinSet";
  public static final String RECT_SET_STRING = "Rect_x_y_w_h";
  public static final String LED_ARRAY_INFO_STRING = "LedArrayInfo";
  public static final String SCANNING_SEVEN_SEGMENT_INFO_STRING = "ScanningSevenSegInfo";
  public static final String MAP_ROTATION = "rotation";
  public static final String[] CLOCK_SECTION_STRINGS = {
    "Frequency", "FPGApin", "PullBehavior", "IOStandard"
  };
  public static final String FPGA_INFORMATION_SECTION_STRING = "FPGAInformation";
  public static final String[] FPGA_SECTION_STRINGS = {
    "Vendor",
    "Part",
    "Family",
    "Package",
    "Speedgrade",
    "USBTMC",
    "JTAGPos",
    "FlashName",
    "FlashPos"
  };
  public static final String UNUSED_PINS_STRING = "UnusedPins";
  public static final String COMPONENTS_SECTION_STRING = "IOComponents";
  public static final String LOCATION_X_STRING = "LocationX";
  public static final String LOCATION_Y_STRING = "LocationY";
  public static final String WIDTH_STRING = "Width";
  public static final String HEIGHT_STRING = "Height";
  public static final String PIN_LOCATION_STRING = "FPGAPinName";
  public static final String IMAGE_INFORMATION_STRING = "BoardPicture";
  public static final String MULTI_PIN_INFORMATION_STRING = "NrOfPins";
  public static final String MULTI_PIN_PREFIX_STRING = "FPGAPin_";
  public static final String LABEL_STRING = "Label";
  private DocumentBuilderFactory factory;
  private DocumentBuilder parser;
  private Document boardInfo;

  public BoardWriterClass(BoardInformation BoardInfo, Image BoardImage) {
    try {
      // Create instance of DocumentBuilderFactory
      factory = XmlUtil.getHardenedBuilderFactory();
      // Get the DocumentBuilder
      parser = factory.newDocumentBuilder();
      // Create blank DOM Document
      boardInfo = parser.newDocument();

      final var root = boardInfo.createElement(BoardInfo.getBoardName());
      boardInfo.appendChild(root);
      final var fpgaInfo = boardInfo.createElement(BOARD_INFORMATION_SECTION_STRING);
      root.appendChild(fpgaInfo);
      final var comment = boardInfo.createComment("This section decribes the FPGA and its clock");
      fpgaInfo.appendChild(comment);
      final var clkInfo = boardInfo.createElement(CLOCK_INFORMATION_SECTION_STRING);
      clkInfo.setAttribute(
          CLOCK_SECTION_STRINGS[0], Long.toString(BoardInfo.fpga.getClockFrequency()));
      final var pin = boardInfo.createAttribute(CLOCK_SECTION_STRINGS[1]);
      pin.setValue(BoardInfo.fpga.getClockPinLocation().toUpperCase());
      clkInfo.setAttributeNode(pin);
      final var pull = boardInfo.createAttribute(CLOCK_SECTION_STRINGS[2]);
      pull.setValue(PullBehaviors.BEHAVIOR_STRINGS[BoardInfo.fpga.getClockPull()]);
      clkInfo.setAttributeNode(pull);
      final var ios = boardInfo.createAttribute(CLOCK_SECTION_STRINGS[3]);
      ios.setValue(IoStandards.BEHAVIOR_STRINGS[BoardInfo.fpga.getClockStandard()]);
      clkInfo.setAttributeNode(ios);
      fpgaInfo.appendChild(clkInfo);
      final var fpga = boardInfo.createElement(FPGA_INFORMATION_SECTION_STRING);
      fpga.setAttribute(
          FPGA_SECTION_STRINGS[0],
          VendorSoftware.VENDORS[BoardInfo.fpga.getVendor()].toUpperCase());
      final var part = boardInfo.createAttribute(FPGA_SECTION_STRINGS[1]);
      part.setValue(BoardInfo.fpga.getPart());
      fpga.setAttributeNode(part);
      final var tech = boardInfo.createAttribute(FPGA_SECTION_STRINGS[2]);
      tech.setValue(BoardInfo.fpga.getTechnology());
      fpga.setAttributeNode(tech);
      final var box = boardInfo.createAttribute(FPGA_SECTION_STRINGS[3]);
      box.setValue(BoardInfo.fpga.getPackage());
      fpga.setAttributeNode(box);
      final var speed = boardInfo.createAttribute(FPGA_SECTION_STRINGS[4]);
      speed.setValue(BoardInfo.fpga.getSpeedGrade());
      fpga.setAttributeNode(speed);
      final var usbTmc = boardInfo.createAttribute(FPGA_SECTION_STRINGS[5]);
      usbTmc.setValue(BoardInfo.fpga.isUsbTmcDownloadRequired().toString());
      fpga.setAttributeNode(usbTmc);
      final var jtagPos = boardInfo.createAttribute(FPGA_SECTION_STRINGS[6]);
      jtagPos.setValue(String.valueOf(BoardInfo.fpga.getFpgaJTAGChainPosition()));
      fpga.setAttributeNode(jtagPos);
      final var flashName = boardInfo.createAttribute(FPGA_SECTION_STRINGS[7]);
      flashName.setValue(String.valueOf(BoardInfo.fpga.getFlashName()));
      fpga.setAttributeNode(flashName);
      final var flashJtagPos = boardInfo.createAttribute(FPGA_SECTION_STRINGS[8]);
      flashJtagPos.setValue(String.valueOf(BoardInfo.fpga.getFlashJTAGChainPosition()));
      fpga.setAttributeNode(flashJtagPos);
      final var unusedPins = boardInfo.createElement(UNUSED_PINS_STRING);
      fpgaInfo.appendChild(fpga);
      unusedPins.setAttribute(
          "PullBehavior", PullBehaviors.BEHAVIOR_STRINGS[BoardInfo.fpga.getUnusedPinsBehavior()]);
      fpgaInfo.appendChild(unusedPins);
      final var components = boardInfo.createElement(COMPONENTS_SECTION_STRING);
      root.appendChild(components);
      final var compCmd =
          boardInfo.createComment("This section describes all Components present on the boards");
      components.appendChild(compCmd);
      for (final var comp : BoardInfo.getAllComponents()) {
        components.appendChild(comp.getDocumentElement(boardInfo));
      }
      final var writer = new ImageXmlFactory();
      writer.createStream(BoardImage);
      final var boardPicture = boardInfo.createElement(IMAGE_INFORMATION_STRING);
      root.appendChild(boardPicture);
      final var pictCmd = boardInfo.createComment("This section hold the board picture");
      boardPicture.appendChild(pictCmd);
      final var pictsize = boardInfo.createElement("PictureDimension");
      pictsize.setAttribute("Width", Integer.toString(BoardImage.getWidth(null)));
      final var height = boardInfo.createAttribute("Height");
      height.setValue(Integer.toString(BoardImage.getHeight(null)));
      pictsize.setAttributeNode(height);
      boardPicture.appendChild(pictsize);
      final var codeTable = boardInfo.createElement("CompressionCodeTable");
      boardPicture.appendChild(codeTable);
      codeTable.setAttribute("TableData", writer.getCodeTable());
      final var pixelData = boardInfo.createElement("PixelData");
      boardPicture.appendChild(pixelData);
      pixelData.setAttribute("PixelRGB", writer.getCompressedString());
    } catch (Exception e) {
      /* TODO: handle exceptions */
      logger.error(
          "Exceptions not handled yet in BoardWriterClass(), but got an exception: {}",
          e.getMessage());
    }
  }

  public void printXml() {
    try {
      final var tranFactory = TransformerFactory.newInstance();
      final var aTransformer = tranFactory.newTransformer();
      aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
      final var src = new DOMSource(boardInfo);
      final var dest = new StreamResult(new StringWriter());
      aTransformer.transform(src, dest);
      logger.info(dest.getWriter().toString());
    } catch (Exception e) {
      /* TODO: handle exceptions */
      logger.error(
          "Exceptions not handled yet in PrintXml(), but got an exception: {}", e.getMessage());
    }
  }

  public void printXml(String filename) {
    try {
      final var tranFactory = TransformerFactory.newInstance();
      tranFactory.setAttribute("indent-number", 3);
      final var aTransformer = tranFactory.newTransformer();
      aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
      final var src = new DOMSource(boardInfo);
      final var file = new File(filename);
      final var dest = new StreamResult(file);
      aTransformer.transform(src, dest);
    } catch (Exception e) {
      logger.error(
          "Exceptions not handled yet in PrintXml(), but got an exception: {}", e.getMessage());
    }
  }
}
