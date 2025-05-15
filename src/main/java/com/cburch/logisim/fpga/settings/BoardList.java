/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class BoardList {
  public static String getBoardName(String boardIdentifier) {
    final var parts =
        boardIdentifier.contains("url:")
            ? boardIdentifier.split("/")
            : boardIdentifier.split(Pattern.quote(File.separator));
    return parts[parts.length - 1].replace(".xml", "");
  }

  private static Collection<String> getBoards(Pattern pattern, String match, String element) {
    final var ret = new ArrayList<String>();
    final var file = new File(element);
    if (file.isDirectory()) {
      ret.addAll(getBoardsfromDirectory(pattern, match, file));
    } else {
      ret.addAll(getBoardsfromJar(pattern, match, file));
    }
    return ret;
  }

  private static Collection<String> getBoardsfromDirectory(
      Pattern pattern, String match, File dir) {
    final var ret = new ArrayList<String>();
    final var fileList = dir.listFiles();
    for (final var file : fileList) {
      if (file.isDirectory()) {
        ret.addAll(getBoardsfromDirectory(pattern, match, file));
      } else {
        try {
          final var fileName = file.getCanonicalPath();
          final var accept = pattern.matcher(fileName).matches() && fileName.contains(match);
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

  private static Collection<String> getBoardsfromJar(Pattern pattern, String match, File dir) {
    // All path separators are defined with File.Separator, but when
    // browsing the .jar, java uses slash even in Windows
    match = match.replaceAll("\\\\", "/");
    final var ret = new ArrayList<String>();
    ZipFile zf;
    try {
      zf = new ZipFile(dir);
    } catch (IOException e) {
      throw new Error(e);
    }
    final var entries = zf.entries();
    while (entries.hasMoreElements()) {
      final var ze = entries.nextElement();
      final var fileName = ze.getName();
      final var accept = pattern.matcher(fileName).matches() && fileName.contains(match);
      if (accept) {
        try {
          // Check that fileName doesn't stray outside target directory: Zip Slip security test.
          // if getCanonicalFile throws IOException, then assume the worst.
          if ((new File(dir, fileName)).getCanonicalFile().toPath().startsWith(dir.getCanonicalFile().toPath())) {
            ret.add("url:" + fileName);
            continue;
          }

          zf.close();
        } catch (IOException e1) {
          // Do nothing since we are about to throw an Error anyway.
        }
        throw new Error("Bad entry: " + fileName + " in " + dir);
      }
    }
    try {
      zf.close();
    } catch (IOException e1) {
      throw new Error(e1);
    }
    return ret;
  }

  private static final String boardResourcePath =
      "resources" + File.separator + "logisim" + File.separator + "boards";

  private final ArrayList<String> definedBoards = new ArrayList<>();

  public BoardList() {
    final var classPath = System.getProperty("java.class.path", File.pathSeparator);
    final var classPathElements = classPath.split(Pattern.quote(File.pathSeparator));
    final var pattern = Pattern.compile(".*.xml");
    for (final var element : classPathElements) {
      definedBoards.addAll(getBoards(pattern, boardResourcePath, element));
    }
  }

  public boolean addExternalBoard(String fileName) {
    if (!definedBoards.contains(fileName)) {
      definedBoards.add(fileName);
      return true;
    }
    return false;
  }

  public boolean removeExternalBoard(String fileName) {
    if (definedBoards.contains(fileName)) {
      definedBoards.remove(fileName);
      return true;
    }
    return false;
  }

  public String getBoardFilePath(String boardName) {
    if (boardName == null) return null;
    for (final var board : definedBoards) {
      if (getBoardName(board).equals(boardName)) {
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
      for (int i = size() - 1; i > 0 && cmp.compareTo(get(i - 1)) < 0; i--) {
        Collections.swap(this, i, i - 1);
      }
    }
  }

  public List<String> getBoardNames() {
    final var ret = new SortedArrayList<String>();
    for (final var board : definedBoards) {
      ret.insertSorted(getBoardName(board));
    }
    return ret;
  }
}
