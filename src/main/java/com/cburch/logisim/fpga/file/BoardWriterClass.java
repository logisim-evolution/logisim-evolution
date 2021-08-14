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

  public static final String BoardInformationSectionString = "BoardInformation";
  public static final String ClockInformationSectionString = "ClockInformation";
  public static final String InputSetString = "InputPinSet";
  public static final String OutputSetString = "OutputPinSet";
  public static final String IOSetString = "BiDirPinSet";
  public static final String RectSetString = "Rect_x_y_w_h";
  public static final String LedArrayInfoString = "LedArrayInfo";
  public static final String[] ClockSectionStrings = {
    "Frequency", "FPGApin", "PullBehavior", "IOStandard"
  };
  public static final String FPGAInformationSectionString = "FPGAInformation";
  public static final String[] FPGASectionStrings = {
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
  public static final String UnusedPinsString = "UnusedPins";
  public static final String ComponentsSectionString = "IOComponents";
  public static final String LocationXString = "LocationX";
  public static final String LocationYString = "LocationY";
  public static final String WidthString = "Width";
  public static final String HeightString = "Height";
  public static final String PinLocationString = "FPGAPinName";
  public static final String ImageInformationString = "BoardPicture";
  public static final String MultiPinInformationString = "NrOfPins";
  public static final String MultiPinPrefixString = "FPGAPin_";
  public static final String LabelString = "Label";
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
      final var fpgainfo = boardInfo.createElement(BoardInformationSectionString);
      root.appendChild(fpgainfo);
      final var comment = boardInfo.createComment("This section decribes the FPGA and its clock");
      fpgainfo.appendChild(comment);
      final var clkinfo = boardInfo.createElement(ClockInformationSectionString);
      clkinfo.setAttribute(ClockSectionStrings[0], Long.toString(BoardInfo.fpga.getClockFrequency()));
      final var pin = boardInfo.createAttribute(ClockSectionStrings[1]);
      pin.setValue(BoardInfo.fpga.getClockPinLocation().toUpperCase());
      clkinfo.setAttributeNode(pin);
      final var pull = boardInfo.createAttribute(ClockSectionStrings[2]);
      pull.setValue(PullBehaviors.Behavior_strings[BoardInfo.fpga.getClockPull()]);
      clkinfo.setAttributeNode(pull);
      final var IOS = boardInfo.createAttribute(ClockSectionStrings[3]);
      IOS.setValue(IoStandards.Behavior_strings[BoardInfo.fpga.getClockStandard()]);
      clkinfo.setAttributeNode(IOS);
      fpgainfo.appendChild(clkinfo);
      final var FPGA = boardInfo.createElement(FPGAInformationSectionString);
      FPGA.setAttribute(FPGASectionStrings[0], VendorSoftware.Vendors[BoardInfo.fpga.getVendor()].toUpperCase());
      final var part = boardInfo.createAttribute(FPGASectionStrings[1]);
      part.setValue(BoardInfo.fpga.getPart());
      FPGA.setAttributeNode(part);
      final var tech = boardInfo.createAttribute(FPGASectionStrings[2]);
      tech.setValue(BoardInfo.fpga.getTechnology());
      FPGA.setAttributeNode(tech);
      final var box = boardInfo.createAttribute(FPGASectionStrings[3]);
      box.setValue(BoardInfo.fpga.getPackage());
      FPGA.setAttributeNode(box);
      final var speed = boardInfo.createAttribute(FPGASectionStrings[4]);
      speed.setValue(BoardInfo.fpga.getSpeedGrade());
      FPGA.setAttributeNode(speed);
      final var usbtmc = boardInfo.createAttribute(FPGASectionStrings[5]);
      usbtmc.setValue(BoardInfo.fpga.USBTMCDownloadRequired().toString());
      FPGA.setAttributeNode(usbtmc);
      final var jtagPos = boardInfo.createAttribute(FPGASectionStrings[6]);
      jtagPos.setValue(String.valueOf(BoardInfo.fpga.getFpgaJTAGChainPosition()));
      FPGA.setAttributeNode(jtagPos);
      final var flashName = boardInfo.createAttribute(FPGASectionStrings[7]);
      flashName.setValue(String.valueOf(BoardInfo.fpga.getFlashName()));
      FPGA.setAttributeNode(flashName);
      final var flashJtagPos = boardInfo.createAttribute(FPGASectionStrings[8]);
      flashJtagPos.setValue(String.valueOf(BoardInfo.fpga.getFlashJTAGChainPosition()));
      FPGA.setAttributeNode(flashJtagPos);
      final var UnusedPins = boardInfo.createElement(UnusedPinsString);
      fpgainfo.appendChild(FPGA);
      UnusedPins.setAttribute("PullBehavior", PullBehaviors.Behavior_strings[BoardInfo.fpga.getUnusedPinsBehavior()]);
      fpgainfo.appendChild(UnusedPins);
      final var Components = boardInfo.createElement(ComponentsSectionString);
      root.appendChild(Components);
      Comment Compcmd = boardInfo.createComment("This section describes all Components present on the boards");
      Components.appendChild(Compcmd);
      for (var comp : BoardInfo.GetAllComponents()) {
        Components.appendChild(comp.GetDocumentElement(boardInfo));
      }
      final var writer = new ImageXmlFactory();
      writer.CreateStream(BoardImage);
      final var BoardPicture = boardInfo.createElement(ImageInformationString);
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
