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

package com.cburch.logisim.prefs;

import static com.cburch.logisim.proj.Strings.S;

import com.cburch.logisim.Main;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.val;

public class Template {

  public static Template create(InputStream in) {
    val reader = new InputStreamReader(in);
    val buf = new char[4096];
    val dest = new StringBuilder();
    while (true) {
      try {
        val nrOfBytes = reader.read(buf);
        if (nrOfBytes < 0) break;
        dest.append(buf, 0, nrOfBytes);
      } catch (IOException e) {
        break;
      }
    }
    return new Template(dest.toString());
  }

  public static Template createEmpty() {
    val circName = S.get("newCircuitName");
    val buf = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<project source=\"" + Main.VERSION
        + "\" version=\"1.0\">"
        + " <circuit name=\"" + circName + "\" />"
        + "</project>";
    return new Template(buf);
  }

  private final String contents;

  private Template(String contents) {
    this.contents = contents;
  }

  public InputStream createStream() {
    return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
  }
}
