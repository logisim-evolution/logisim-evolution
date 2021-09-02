/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import static com.cburch.logisim.proj.Strings.S;

import com.cburch.logisim.Main;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Template {

  public static Template create(InputStream in) {
    final var reader = new InputStreamReader(in);
    final var buf = new char[4096];
    final var dest = new StringBuilder();
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
    final var circName = S.get("newCircuitName");
    final var buf = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<project source=\"" + Main.VERSION
        + "\" version=\"1.0\">"
        + " <circuit name=\"" + circName + "\" />"
        + "</project>";
    return new Template(buf);
  }

  static final Logger logger = LoggerFactory.getLogger(Template.class);

  private final String contents;

  private Template(String contents) {
    this.contents = contents;
  }

  public InputStream createStream() {
    return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
  }
}
