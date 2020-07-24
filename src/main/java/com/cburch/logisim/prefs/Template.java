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

package com.cburch.logisim.prefs;

import static com.cburch.logisim.proj.Strings.S;

import com.cburch.logisim.Main;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Template {

  public static Template create(InputStream in) {
    InputStreamReader reader = new InputStreamReader(in);
    char[] buf = new char[4096];
    StringBuilder dest = new StringBuilder();
    while (true) {
      try {
        int nbytes = reader.read(buf);
        if (nbytes < 0) break;
        dest.append(buf, 0, nbytes);
      } catch (IOException e) {
        break;
      }
    }
    return new Template(dest.toString());
  }

  public static Template createEmpty() {
    String circName = S.get("newCircuitName");
    StringBuilder buf = new StringBuilder();
    buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    buf.append("<project source=\"" + Main.VERSION.mainVersion() + "\" version=\"1.0\">");
    buf.append(" <circuit name=\"" + circName + "\" />");
    buf.append("</project>");
    return new Template(buf.toString());
  }

  static final Logger logger = LoggerFactory.getLogger(Template.class);

  private String contents;

  private Template(String contents) {
    this.contents = contents;
  }

  public InputStream createStream() {
    try {
      return new ByteArrayInputStream(contents.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      logger.warn("UTF-8 is not supported");
      return new ByteArrayInputStream(contents.getBytes());
    }
  }
}
