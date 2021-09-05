/*
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

package com.cburch.logisim.fpga.file;

import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.settings.VendorSoftware;
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
import org.w3c.dom.Comment;
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
      factory = DocumentBuilderFactory.newInstance();
      // Get the DocumentBuilder
      parser = factory.newDocumentBuilder();
      // Create blank DOM Document
      boardInfo = parser.newDocument();

      final var root = boardInfo.createElement(BoardInfo.getBoardName());
      boardInfo.appendChild(root);
      final var fpgainfo = boardInfo.createElement(BOARD_INFORMATION_SECTION_STRING);
      root.appendChild(fpgainfo);
      final var comment = boardInfo.createComment("This section decribes the FPGA and its clock");
      fpgainfo.appendChild(comment);
      final var clkinfo = boardInfo.createElement(CLOCK_INFORMATION_SECTION_STRING);
      clkinfo.setAttribute(CLOCK_SECTION_STRINGS[0], Long.toString(BoardInfo.fpga.getClockFrequency()));
      final var pin = boardInfo.createAttribute(CLOCK_SECTION_STRINGS[1]);
      pin.setValue(BoardInfo.fpga.getClockPinLocation().toUpperCase());
      clkinfo.setAttributeNode(pin);
      final var pull = boardInfo.createAttribute(CLOCK_SECTION_STRINGS[2]);
      pull.setValue(PullBehaviors.BEHAVIOR_STRINGS[BoardInfo.fpga.getClockPull()]);
      clkinfo.setAttributeNode(pull);
      final var IOS = boardInfo.createAttribute(CLOCK_SECTION_STRINGS[3]);
      IOS.setValue(IoStandards.Behavior_strings[BoardInfo.fpga.getClockStandard()]);
      clkinfo.setAttributeNode(IOS);
      fpgainfo.appendChild(clkinfo);
      final var FPGA = boardInfo.createElement(FPGA_INFORMATION_SECTION_STRING);
      FPGA.setAttribute(FPGA_SECTION_STRINGS[0], VendorSoftware.VENDORS[BoardInfo.fpga.getVendor()].toUpperCase());
      final var part = boardInfo.createAttribute(FPGA_SECTION_STRINGS[1]);
      part.setValue(BoardInfo.fpga.getPart());
      FPGA.setAttributeNode(part);
      final var tech = boardInfo.createAttribute(FPGA_SECTION_STRINGS[2]);
      tech.setValue(BoardInfo.fpga.getTechnology());
      FPGA.setAttributeNode(tech);
      final var box = boardInfo.createAttribute(FPGA_SECTION_STRINGS[3]);
      box.setValue(BoardInfo.fpga.getPackage());
      FPGA.setAttributeNode(box);
      final var speed = boardInfo.createAttribute(FPGA_SECTION_STRINGS[4]);
      speed.setValue(BoardInfo.fpga.getSpeedGrade());
      FPGA.setAttributeNode(speed);
      final var usbtmc = boardInfo.createAttribute(FPGA_SECTION_STRINGS[5]);
      usbtmc.setValue(BoardInfo.fpga.USBTMCDownloadRequired().toString());
      FPGA.setAttributeNode(usbtmc);
      final var jtagPos = boardInfo.createAttribute(FPGA_SECTION_STRINGS[6]);
      jtagPos.setValue(String.valueOf(BoardInfo.fpga.getFpgaJTAGChainPosition()));
      FPGA.setAttributeNode(jtagPos);
      final var flashName = boardInfo.createAttribute(FPGA_SECTION_STRINGS[7]);
      flashName.setValue(String.valueOf(BoardInfo.fpga.getFlashName()));
      FPGA.setAttributeNode(flashName);
      final var flashJtagPos = boardInfo.createAttribute(FPGA_SECTION_STRINGS[8]);
      flashJtagPos.setValue(String.valueOf(BoardInfo.fpga.getFlashJTAGChainPosition()));
      FPGA.setAttributeNode(flashJtagPos);
      final var UnusedPins = boardInfo.createElement(UNUSED_PINS_STRING);
      fpgainfo.appendChild(FPGA);
      UnusedPins.setAttribute("PullBehavior", PullBehaviors.BEHAVIOR_STRINGS[BoardInfo.fpga.getUnusedPinsBehavior()]);
      fpgainfo.appendChild(UnusedPins);
      final var Components = boardInfo.createElement(COMPONENTS_SECTION_STRING);
      root.appendChild(Components);
      Comment Compcmd = boardInfo.createComment("This section describes all Components present on the boards");
      Components.appendChild(Compcmd);
      for (var comp : BoardInfo.GetAllComponents()) {
        Components.appendChild(comp.GetDocumentElement(boardInfo));
      }
      final var writer = new ImageXmlFactory();
      writer.CreateStream(BoardImage);
      final var BoardPicture = boardInfo.createElement(IMAGE_INFORMATION_STRING);
      root.appendChild(BoardPicture);
      final var Pictcmd = boardInfo.createComment("This section hold the board picture");
      BoardPicture.appendChild(Pictcmd);
      final var pictsize = boardInfo.createElement("PictureDimension");
      pictsize.setAttribute("Width", Integer.toString(BoardImage.getWidth(null)));
      final var height = boardInfo.createAttribute("Height");
      height.setValue(Integer.toString(BoardImage.getHeight(null)));
      pictsize.setAttributeNode(height);
      BoardPicture.appendChild(pictsize);
      final var CodeTable = boardInfo.createElement("CompressionCodeTable");
      BoardPicture.appendChild(CodeTable);
      CodeTable.setAttribute("TableData", writer.GetCodeTable());
      final var PixelData = boardInfo.createElement("PixelData");
      BoardPicture.appendChild(PixelData);
      PixelData.setAttribute("PixelRGB", writer.GetCompressedString());
    } catch (Exception e) {
      /* TODO: handle exceptions */
      logger.error(
          "Exceptions not handled yet in BoardWriterClass(), but got an exception: {}",
          e.getMessage());
    }
  }

  public void PrintXml() {
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

  public void PrintXml(String filename) {
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
