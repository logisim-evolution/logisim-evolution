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

package com.cburch.logisim.fpga.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BoardList {
  public static String getBoardName(String BoardIdentifier) {
    String[] parts;
    if (BoardIdentifier.contains("url:")) parts = BoardIdentifier.split("/");
    else parts = BoardIdentifier.split(Pattern.quote(File.separator));
    return parts[parts.length - 1].replace(".xml", "");
  }

  private static Collection<String> getBoards(Pattern p, String Match, String Element) {
    ArrayList<String> ret = new ArrayList<>();
    File file = new File(Element);
    if (file.isDirectory()) {
      ret.addAll(getBoardsfromDirectory(p, Match, file));
    } else {
      ret.addAll(getBoardsfromJar(p, Match, file));
    }
    return ret;
  }

  private static Collection<String> getBoardsfromDirectory(Pattern p, String Match, File Dir) {
    ArrayList<String> ret = new ArrayList<>();
    File[] fileList = Dir.listFiles();
    for (File file : fileList) {
      if (file.isDirectory()) {
        ret.addAll(getBoardsfromDirectory(p, Match, file));
      } else {
        try {
          String fileName = file.getCanonicalPath();
          boolean accept = p.matcher(fileName).matches() && fileName.contains(Match);
          if (accept) {
            ret.add("file:" + fileName);
          }
        } catch (IOException e) {
          throw new Error(e);
        }
      }
    }
    return ret;
  }

  private static Collection<String> getBoardsfromJar(Pattern p, String Match, File Dir) {
    // All path separators are defined with File.Separator, but when
    // browsing the .jar, java uses slash even in Windows
    Match = Match.replaceAll("\\\\", "/");
    ArrayList<String> ret = new ArrayList<>();
    ZipFile zf;
    try {
      zf = new ZipFile(Dir);
    } catch (IOException e) {
      throw new Error(e);
    }
    Enumeration<? extends ZipEntry> e = zf.entries();
    while (e.hasMoreElements()) {
      ZipEntry ze = e.nextElement();
      String fileName = ze.getName();
      boolean accept = p.matcher(fileName).matches() && fileName.contains(Match);
      if (accept) {
        ret.add("url:" + fileName);
      }
    }
    try {
      zf.close();
    } catch (IOException e1) {
      throw new Error(e1);
    }
    return ret;
  }

  private static final String BoardResourcePath =
      "resources" + File.separator + "logisim" + File.separator + "boards";

  private final ArrayList<String> DefinedBoards = new ArrayList<>();

  public BoardList() {
    String classPath = System.getProperty("java.class.path", File.pathSeparator);
    String[] classPathElements = classPath.split(File.pathSeparator);
    Pattern p = Pattern.compile(".*.xml");
    for (String element : classPathElements) {
      DefinedBoards.addAll(getBoards(p, BoardResourcePath, element));
    }
  }

  public boolean AddExternalBoard(String Filename) {
    if (!DefinedBoards.contains(Filename)) {
      DefinedBoards.add(Filename);
      return true;
    }
    return false;
  }

  public boolean RemoveExternalBoard(String Filename) {
    if (DefinedBoards.contains(Filename)) {
      DefinedBoards.remove(Filename);
      return true;
    }
    return false;
  }

  public String GetBoardFilePath(String BoardName) {
    if (BoardName == null) return null;
    for (String board : DefinedBoards) {
      if (getBoardName(board).equals(BoardName)) {
        return board;
      }
    }
    return null;
  }

  @SuppressWarnings("serial")
  private static class SortedArrayList<T> extends ArrayList<T> {

    @SuppressWarnings("unchecked")
    public void insertSorted(T value) {
      add(value);
      Comparable<T> cmp = (Comparable<T>) value;
      for (int i = size() - 1; i > 0 && cmp.compareTo(get(i - 1)) < 0; i--)
        Collections.swap(this, i, i - 1);
    }
  }

  public ArrayList<String> GetBoardNames() {
    SortedArrayList<String> ret = new SortedArrayList<>();
    for (String board : DefinedBoards) {
      ret.insertSorted(getBoardName(board));
    }
    return ret;
  }
}
