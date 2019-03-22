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

package com.cburch.logisim.fpga.fpgagui;

import com.cburch.logisim.fpga.download.Download;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardReaderClass;
import com.cburch.logisim.proj.Project;

public class FPGACommanderTests extends FPGACommanderBase {

  private String circuitTestName = null;
  private String circuitPathMap = null;
  private boolean writeToFlash = false;
  private double TickFrequency;

  public FPGACommanderTests(
      Project project, String pathMap, String circuit, String boardName, double frequency) {
    MyReporter = new FPGAReportNoGui();
    MyProject = project;
    circuitTestName = circuit;
    circuitPathMap = pathMap;
    TickFrequency = frequency;
    MyBoardInformation =
        new BoardReaderClass("url:resources/logisim/boards/" + boardName + ".xml")
            .GetBoardInformation();
    MyBoardInformation.setBoardName(boardName);
  }

  public boolean StartTests() {
    Download Downloader =
        new Download(
            MyProject,
            circuitTestName,
            TickFrequency,
            MyReporter,
            MyBoardInformation,
            circuitPathMap,
            writeToFlash,
            false,
            false);
    return Downloader.runtty();
  }
}
