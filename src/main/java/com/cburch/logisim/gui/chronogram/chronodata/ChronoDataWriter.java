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

package com.cburch.logisim.gui.chronogram.chronodata;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ChronoDataWriter {
  /** Export all chronogram data into a specified file */
  public static void export(String filePath, TimelineParam timeLineParam, ChronoData chronoData) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

      // header
      for (String name : chronoData.getSignalOrder()) {
        if (!name.contains("__s__")) writer.write(name + "\t");
      }
      writer.newLine();

      // freqency
      if (timeLineParam != null) writer.write(timeLineParam.toString());
      else writer.write("noclk");
      writer.newLine();

      // content
      for (int row = 0; row < chronoData.get("sysclk").getSignalValues().size(); ++row) {
        for (String signalName : chronoData.getSignalOrder()) {
          if (!signalName.contains("__s__")) {
            writer.write(chronoData.get(signalName).getSignalValues().get(row));
            writer.write("\t");
          }
        }
        writer.newLine();
      }

      writer.flush();
      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
