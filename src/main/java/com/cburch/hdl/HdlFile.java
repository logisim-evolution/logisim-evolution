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

package com.cburch.hdl;

import static com.cburch.hdl.Strings.S;

import com.cburch.logisim.std.hdl.HdlContentEditor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class HdlFile {

  public static void open(File file, HdlContentEditor editor) throws IOException {
    BufferedReader in = null;

    try {
      in = new BufferedReader(new FileReader(file));

      StringBuilder content = new StringBuilder();
      String l;

      while ((l = in.readLine()) != null) {
        content.append(l);
        content.append(System.getProperty("line.separator"));
      }
      editor.setText(content.toString());
    } catch (IOException ex) {
      throw new IOException(S.get("hdlFileReaderError"));
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  public static void save(File file, HdlContentEditor editor) throws IOException {
    BufferedWriter out = null;

    try {
      out = new BufferedWriter(new FileWriter(file));
      String data = editor.getText();
      out.write(data, 0, data.length());
    } catch (IOException ex) {
      throw new IOException(S.get("hdlFileWriterError"));
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}
